package app.aiscalabilityreview.service;

import app.aiscalabilityreview.service.DatadogMetricProbe.Monitor;
import app.aiscalabilityreview.service.DatadogMetricProbe.Slo;
import core.framework.inject.Inject;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads the 28 days of Datadog metrics the review scores, and does the arithmetic on them.
 *
 * <p>A model used to do both through the Datadog MCP server, and what it wrote down was wrong often
 * enough that validation failed the report: peak CPU of 0.0245 cores against a real 1.19, a slow-query
 * average of 0 against 974 a day, MySQL storage 482 GB against 1,763 GB, a peak-RPS timestamp in the
 * year 58794. The cheaper the model, the more of it was invented. Nothing here needs judgement — a
 * fixed list of queries, a mean, a maximum — so it is code, and takes seconds instead of minutes.
 *
 * <p>The window is the same day-aligned 28 days for every query, ending at today 00:00 UTC, so the
 * daily buckets are complete and the four weekly totals are four whole weeks.
 */
public class DatadogMetricsCollector {
    static final int DAY = 86400;
    static final int WEEK = 7 * DAY;
    static final int DAYS = 28;
    private static final double SLO_AT_RISK_BUDGET_PCT = 25;

    @Inject
    DatadogMetricProbe datadog;

    public Collected collect(Params params) {
        long to = Instant.now().atZone(ZoneOffset.UTC).toLocalDate().atStartOfDay(ZoneOffset.UTC).toEpochSecond();
        return collect(params, to);
    }

    Collected collect(Params params, long to) {
        MetricsDocument doc = new MetricsDocument();
        Run run = new Run(datadog, doc, to - (long) DAYS * DAY, to);
        String tags = "env:" + params.env() + ",service:" + params.service();
        new TrafficMetrics(run).collect(tags);
        new ResourceMetrics(run).collect(tags, params.service(), params.env());
        DatabaseMetrics databases = new DatabaseMetrics(run);
        if (RequestValues.isPresent(params.mysqlHost())) databases.mysql(params.mysqlHost());
        if (RequestValues.isPresent(params.atlasCluster())) databases.atlas(params.atlasCluster(), params.atlasDatabase());
        if (RequestValues.isPresent(params.kafkaConsumerGroups())) new KafkaMetrics(run).collect(params.env(), params.kafkaConsumerGroups());
        sloAndMonitors(run, params.service());
        return new Collected(doc.text(), doc.snapshot, doc.gaps, to);
    }

    private void sloAndMonitors(Run run, String service) {
        MetricsDocument doc = run.doc;
        doc.heading("7. SLO & Monitors");
        List<Monitor> monitors = run.datadog.monitors(service);
        if (monitors == null) {
            doc.line("- **Monitors in ALERT or WARN**: NOT_COLLECTED: the Datadog monitor API could not be reached [Source: Datadog API → GET /api/v1/monitor]");
            doc.gaps.add("Monitors — the Datadog monitor API could not be reached");
        } else if (monitors.isEmpty()) {
            doc.line("- **Monitors in ALERT or WARN**: None in ALERT or WARN [Source: Datadog API → GET /api/v1/monitor?group_states=alert,warn&tags=service:" + service + "]");
        } else {
            doc.line("- **Monitors in ALERT or WARN** [Source: Datadog API → GET /api/v1/monitor?group_states=alert,warn&tags=service:" + service + "]:");
            for (Monitor monitor : monitors) {
                doc.line("  - " + monitor.name + " — " + monitor.overallState);
            }
        }
        slos(run, service);
    }

    private void slos(Run run, String service) {
        MetricsDocument doc = run.doc;
        List<Slo> slos = run.datadog.slos(service, run.from, run.to);
        if (slos == null) {
            doc.line("- **SLOs**: NOT_COLLECTED: the Datadog SLO API could not be reached [Source: Datadog API → GET /api/v1/slo]");
            doc.gaps.add("SLOs — the Datadog SLO API could not be reached");
            return;
        }
        if (slos.isEmpty()) {
            doc.line("- **SLOs**: None defined for service:" + service + " [Source: Datadog API → GET /api/v1/slo?tags_query=service:" + service + "]");
            doc.put("slo_count", 0);
            return;
        }
        doc.line("- **SLOs** (attainment over the 28-day window; error budget = (attainment − target) ÷ (100 − target)):");
        int breached = 0;
        double minBudget = Double.NaN;
        int number = 1;
        for (Slo slo : slos) {
            double target = slo.thresholds == null || slo.thresholds.isEmpty() ? Double.NaN : slo.thresholds.get(0).target;
            String timeframe = slo.thresholds == null || slo.thresholds.isEmpty() ? "?" : slo.thresholds.get(0).timeframe;
            if (slo.sliValue == null) {
                doc.line("  " + number++ + ". **" + slo.name + "** (`" + slo.id + "`): NOT_COLLECTED: no history returned [Source: Datadog API → GET /api/v1/slo/" + slo.id + "/history]");
                doc.gaps.add("SLO " + slo.name + " — no history returned");
                continue;
            }
            double budget = Math.max(0, (slo.sliValue - target) / (100 - target) * 100);
            String status = slo.sliValue < target ? "breached" : budget < SLO_AT_RISK_BUDGET_PCT ? "at-risk" : "compliant";
            if ("breached".equals(status)) breached++;
            minBudget = Double.isNaN(minBudget) ? budget : Math.min(minBudget, budget);
            doc.line("  " + number++ + ". **" + slo.name + "** (`" + slo.id + "`)\n     - Type: " + slo.type + "\n     - Target: "
                + trim(target) + "% (" + timeframe + " timeframe)\n     - 28-Day Attainment: " + MetricsDocument.num(slo.sliValue, 4)
                + "%\n     - Error Budget Remaining: " + MetricsDocument.num(budget, 4) + "%\n     - Status: " + status
                + "\n     [Source: Datadog API → GET /api/v1/slo/" + slo.id + "/history]");
        }
        doc.put("slo_count", slos.size());
        doc.put("slo_breached_count", breached);
        doc.put("slo_min_error_budget_remaining_pct", minBudget, 4);
    }

    private String trim(double value) {
        return Double.compare(value, Math.rint(value)) == 0 ? String.valueOf((long) value) : String.valueOf(value);
    }

    /** What one collection needs to know about the service. */
    public record Params(String service, String env, String mysqlHost, String atlasCluster, String atlasDatabase, String kafkaConsumerGroups) {
    }

    /**
     * @param markdown sections 1–7 of the metrics file, ready to be assembled
     * @param snapshot the numeric keys of the metrics snapshot, in order, null where not collected
     * @param gaps     one line per metric that could not be collected, for the "Collection Gaps" section
     * @param windowEnd the end of the window in epoch seconds, today 00:00 UTC
     */
    public record Collected(String markdown, Map<String, Object> snapshot, List<String> gaps, long windowEnd) {
        public Collected {
            snapshot = new LinkedHashMap<>(snapshot);
            gaps = new ArrayList<>(gaps);
        }
    }

    /** The window and the sinks one collection shares. */
    static final class Run {
        final DatadogMetricProbe datadog;
        final MetricsDocument doc;
        final long from;
        final long to;

        Run(DatadogMetricProbe datadog, MetricsDocument doc, long from, long to) {
            this.datadog = datadog;
            this.doc = doc;
            this.from = from;
            this.to = to;
        }

        /** One query over the whole window; null when Datadog could not be asked, empty when it holds no data. */
        List<MetricSeries> series(String query) {
            return MetricSeries.of(datadog.queryRange(query, from, to));
        }

        /** The single series of a query, or null (and a recorded gap) when there is none. */
        MetricSeries single(String label, String query) {
            List<MetricSeries> series = series(query);
            if (series == null) {
                doc.gap(label, query, "the Datadog query failed");
                return null;
            }
            if (series.isEmpty()) {
                doc.gap(label, query, "the query returned no data");
                return null;
            }
            return series.get(0);
        }
    }
}
