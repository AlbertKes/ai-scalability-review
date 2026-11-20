package app.aiscalabilityreview.service;

import app.aiscalabilityreview.service.DatadogMetricsCollector.Run;

import java.util.List;
import java.util.Locale;

import static app.aiscalabilityreview.service.DatadogMetricsCollector.DAY;
import static app.aiscalabilityreview.service.DatadogMetricsCollector.DAYS;
import static app.aiscalabilityreview.service.DatadogMetricsCollector.WEEK;
import static app.aiscalabilityreview.service.MetricsDocument.num;

/** Sections 1–3: traffic, latency, errors and restarts. */
final class TrafficMetrics {
    private static final double SECONDS_TO_MS = 1000;
    private static final int HOUR = 3600;
    private static final int WEEKS = 4;
    private static final List<String> LEVELS = List.of("p50", "p95", "p99");

    private final Run run;
    private final MetricsDocument doc;

    TrafficMetrics(Run run) {
        this.run = run;
        this.doc = run.doc;
    }

    void collect(String tags) {
        String hitsQuery = "sum:trace.undertow_http.request.hits{" + tags + "}.as_count().rollup(sum, " + DAY + ")";
        doc.heading("1. Traffic & Throughput");
        MetricSeries hits = run.single("Average RPS", hitsQuery);
        if (hits != null) traffic(hits, hitsQuery);
        peakRps(tags);
        doc.heading("2. Latency Profile");
        latency(tags);
        doc.heading("3. Error Rates & Stability");
        errors(hits, tags);
    }

    private void traffic(MetricSeries hits, String hitsQuery) {
        double averageRps = hits.sum() / ((double) DAYS * DAY);
        doc.bullet("Average RPS", num(averageRps, 4) + " RPS", hitsQuery);
        doc.put("avg_rps", averageRps, 4);
        if (hits.size() >= DAYS) {
            double w3 = hits.sum(2 * 7, 3 * 7);
            double w4 = hits.sum(3 * 7, 4 * 7);
            double growth = w3 == 0 ? Double.NaN : (w4 - w3) / w3 * 100;
            doc.bullet("Week-over-Week Average RPS Growth", Double.isNaN(growth) ? "n/a (no traffic in week 3)"
                : String.format(Locale.ROOT, "%+.2f%%", growth) + " (week 3: " + Math.round(w3) + " requests; week 4: " + Math.round(w4) + " requests)", hitsQuery);
            doc.put("wow_rps_growth_pct", growth, 2);
            doc.line("- **Weekly Volumes** (4 rows, oldest first):");
            for (int week = 0; week < WEEKS; week++) {
                doc.line("  - " + hits.dayRange(week * 7, (week + 1) * 7) + ": " + Math.round(hits.sum(week * 7, (week + 1) * 7)) + " requests");
            }
            doc.line("  " + MetricsDocument.source(hitsQuery).strip());
        }
    }

    /** Four weekly calls: 672 hourly points do not fit in one response. */
    private void peakRps(String tags) {
        String query = "sum:trace.undertow_http.request.hits{" + tags + "}.as_count().rollup(sum, " + HOUR + ")";
        double best = Double.NaN;
        String when = null;
        for (int week = 0; week < WEEKS; week++) {
            long from = run.from + (long) week * WEEK;
            List<MetricSeries> series = MetricSeries.of(run.datadog.queryRange(query, from, from + WEEK));
            if (series == null || series.isEmpty()) {
                doc.gap("Peak RPS", query, "the hourly query for week " + (week + 1) + " returned no data");
                doc.put("peak_rps", Double.NaN, 4);
                return;
            }
            MetricSeries weekly = series.get(0);
            int index = weekly.peakIndex();
            if (index >= 0 && (Double.isNaN(best) || weekly.value(index) > best)) {
                best = weekly.value(index);
                when = weekly.hourAt(index);
            }
        }
        doc.bullet("Peak RPS", num(best / HOUR, 4) + " RPS (" + Math.round(best) + " requests in the hour starting " + when + ")", query);
        doc.put("peak_rps", best / HOUR, 4);
    }

    private void latency(String tags) {
        for (String level : LEVELS) {
            String name = level.toUpperCase(Locale.ROOT);
            String query = level + ":trace.undertow_http.request{" + tags + "}.rollup(avg, " + DAY + ")";
            MetricSeries series = run.single(name + " Average", query);
            if (series == null) continue;
            double ms = series.mean() * SECONDS_TO_MS;
            doc.bullet(name + " Average", num(ms, 2) + " ms", query);
            doc.put(level + "_latency_ms", ms, 2);
        }
        String peakQuery = "p99:trace.undertow_http.request{" + tags + "}.rollup(max, " + DAY + ")";
        MetricSeries peak = run.single("Peak P99", peakQuery);
        if (peak != null) {
            double ms = peak.peak() * SECONDS_TO_MS;
            doc.bullet("Peak P99", num(ms, 2) + " ms (" + peak.dayAt(peak.peakIndex()) + ")", peakQuery);
            doc.put("p99_latency_peak_ms", ms, 2);
        }
    }

    private void errors(MetricSeries hits, String tags) {
        String errorQuery = "sum:trace.undertow_http.request.errors{" + tags + "}.as_count().rollup(sum, " + DAY + ")";
        List<MetricSeries> errorSeries = run.series(errorQuery);
        if (errorSeries == null || hits == null) {
            doc.gap("HTTP Error Rate", errorQuery, "the Datadog query failed");
        } else {
            // Datadog returns no series at all when there were no errors, which is a zero and not a gap.
            MetricSeries errors = errorSeries.isEmpty() ? null : errorSeries.get(0);
            double totalErrors = errors == null ? 0 : errors.sum();
            double rate = hits.sum() == 0 ? 0 : totalErrors / hits.sum() * 100;
            double worst = 0;
            int worstDay = 0;
            for (int i = 0; errors != null && i < hits.size() && i < errors.size(); i++) {
                double day = hits.value(i) > 0 && !Double.isNaN(errors.value(i)) ? errors.value(i) / hits.value(i) * 100 : 0;
                if (day > worst) {
                    worst = day;
                    worstDay = i;
                }
            }
            doc.bullet("HTTP Error Rate, 28-Day Average", num(rate, 4) + "% (" + Math.round(totalErrors) + " errors across " + Math.round(hits.sum()) + " hits)", errorQuery);
            doc.bullet("HTTP Error Rate, Worst Single Day", num(worst, 4) + "% (" + hits.dayAt(worstDay) + ")", errorQuery);
            doc.put("error_rate_pct", rate, 4);
        }
        restarts(tags);
    }

    private void restarts(String tags) {
        String query = "sum:kubernetes.containers.restarts{" + tags + "}.rollup(sum, " + DAY + ")";
        List<MetricSeries> series = run.series(query);
        if (series == null) {
            doc.gap("Container Restarts", query, "the Datadog query failed");
        } else {
            double total = series.isEmpty() ? 0 : series.get(0).sum();
            doc.bullet("Container Restarts", Math.round(total) + " over the 28 days" + (series.isEmpty() ? " (the metric has no series, which Datadog reports for zero restarts)" : ""), query);
            doc.put("container_restarts", total, 0);
        }
        String oomQuery = "sum:kubernetes.containers.last_state.terminated{" + tags + ",reason:oomkilled}.rollup(sum, " + DAY + ")";
        List<MetricSeries> oom = run.series(oomQuery);
        if (oom == null) {
            doc.gap("OOMKill Events", oomQuery, "the Datadog query failed");
        } else {
            doc.bullet("OOMKill Events", Math.round(oom.isEmpty() ? 0 : oom.get(0).sum()) + " over the 28 days", oomQuery);
        }
    }
}
