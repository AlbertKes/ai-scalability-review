package app.aiscalabilityreview.service;

import app.aiscalabilityreview.service.DatadogMetricProbe.Monitor;
import app.aiscalabilityreview.service.DatadogMetricProbe.QueryResponse;
import app.aiscalabilityreview.service.DatadogMetricProbe.Series;
import app.aiscalabilityreview.service.DatadogMetricProbe.Slo;
import app.aiscalabilityreview.service.DatadogMetricProbe.SloThreshold;
import app.aiscalabilityreview.service.DatadogMetricsCollector.Collected;
import app.aiscalabilityreview.service.DatadogMetricsCollector.Params;
import org.junit.jupiter.api.Test;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DatadogMetricsCollectorTest {
    private static final long TO = ZonedDateTime.of(2026, 11, 19, 0, 0, 0, 0, ZoneOffset.UTC).toEpochSecond();
    private static final long FROM = TO - 28L * 86400;
    private static final Params SERVICE_ONLY = new Params("svc", "prod", null, null, null, null);

    /**
     * Callers stringify absent Atlas fields as the four characters {@code null}. That is not blank, and
     * used to make this collector query {@code clustername:null} and then report eight MongoDB gaps for
     * a MySQL-only service — the same "8 metric(s) were not collected" the report then flagged.
     */
    @Test
    void theLiteralNullStringDoesNotCollectMongoMetricsOrRecordGaps() {
        Collected collected = collector(new FakeProbe()).collect(
            new Params("svc", "prod", null, "null", "null", null), TO);

        assertThat(collected.markdown()).doesNotContain("MongoDB").doesNotContain("clustername:null");
        assertThat(collected.gaps()).noneMatch(gap -> gap.contains("Mongo"));
    }

    @Test
    void averagesAndPeaksAreComputedFromThePointsWithTheirDates() {
        FakeProbe probe = new FakeProbe();
        probe.answer("sum:trace.undertow_http.request.hits{env:prod,service:svc}.as_count().rollup(sum, 86400)", daily(FROM, 86400.0, 28));
        probe.answer("max:kubernetes.cpu.usage.total{env:prod,service:svc}.rollup(max, 86400)", dailyWithPeak(FROM, 1_000_000, 1_190_000_000, 5));

        Collected collected = collector(probe).collect(SERVICE_ONLY, TO);

        // 28 days x 86400 hits over 2419200 s is exactly 1 RPS
        assertThat(collected.markdown())
            .contains("**Average RPS**: 1.0000 RPS [Source: Datadog API → query-metrics(\"sum:trace.undertow_http.request.hits{env:prod,service:svc}.as_count().rollup(sum, 86400)\")]")
            .contains("**CPU Usage - Peak**: 1.1900 cores (1190000000 nanocores, per pod) on 2026-10-27");
        assertThat(collected.snapshot()).containsEntry("avg_rps", 1.0).containsEntry("cpu_peak_cores", 1.19);
    }

    @Test
    void peakRpsIsTheLargestHourOfFourWeeklyQueriesAndItsTimeIsInsideTheWindow() {
        FakeProbe probe = new FakeProbe();
        String hourly = "sum:trace.undertow_http.request.hits{env:prod,service:svc}.as_count().rollup(sum, 3600)";
        probe.answerRange(hourly, FROM + 2 * 604800L, hourlySeries(FROM + 2 * 604800L, 3600, 7200));

        Collected collected = collector(probe).collect(SERVICE_ONLY, TO);

        assertThat(collected.snapshot()).containsEntry("peak_rps", 2.0);
        assertThat(collected.markdown()).containsPattern("Peak RPS\\*\\*: 2\\.0000 RPS \\(7200 requests in the hour starting 2026-11-0\\d \\d\\d:00 UTC\\)");
    }

    @Test
    void missingSeriesBecomeGapsAndNullSnapshotValuesNeverInventedOnes() {
        Collected collected = collector(new FakeProbe()).collect(SERVICE_ONLY, TO);

        assertThat(collected.gaps()).isNotEmpty().anyMatch(gap -> gap.startsWith("Average RPS"));
        assertThat(collected.snapshot()).doesNotContainKey("avg_rps");
        assertThat(collected.markdown()).contains("NOT_COLLECTED: the query returned no data");
    }

    @Test
    void anEmptyErrorSeriesIsZeroErrorsNotAGap() {
        FakeProbe probe = new FakeProbe();
        probe.answer("sum:trace.undertow_http.request.hits{env:prod,service:svc}.as_count().rollup(sum, 86400)", daily(FROM, 100.0, 28));
        probe.answer("sum:trace.undertow_http.request.errors{env:prod,service:svc}.as_count().rollup(sum, 86400)", new QueryResponse());

        Collected collected = collector(probe).collect(SERVICE_ONLY, TO);

        assertThat(collected.snapshot()).containsEntry("error_rate_pct", 0.0);
        assertThat(collected.gaps()).noneMatch(gap -> gap.contains("Error Rate"));
    }

    @Test
    void kafkaReportsAnAverageFromTheAverageRollupAndThePeakFromTheMaxRollup() {
        FakeProbe probe = new FakeProbe();
        Series avg = series("topic:t1", FROM, 0.03);
        Series max = dailyWithPeakSeries("topic:t1", FROM, 0, 1, 3);
        probe.answer("top(avg:kafka.consumer_lag{env:prod,consumer_group:g} by {topic}.rollup(avg, 86400), 8, 'mean', 'desc')", response(avg));
        probe.answer("top(max:kafka.consumer_lag{env:prod,consumer_group:g} by {topic}.rollup(max, 86400), 8, 'max', 'desc')", response(max));

        Collected collected = collector(probe).collect(new Params("svc", "prod", null, null, null, "g"), TO);

        assertThat(collected.snapshot()).containsEntry("kafka_lag_avg", 0.03).containsEntry("kafka_lag_peak", 1.0);
        assertThat(collected.markdown()).contains("`t1`: 28-day average 0.03; peak 1 (2026-10-25)");
    }

    @Test
    void mysqlStorageIsTheLastBucketAndItsGrowthNotAnAverage() {
        FakeProbe probe = new FakeProbe();
        double gib = 1024.0 * 1024 * 1024;
        List<List<Double>> points = new ArrayList<>(28);
        for (int day = 0; day < 28; day++) {
            points.add(List.of((double) (FROM + day * 86400L) * 1000, (1000 + day) * gib));
        }
        probe.answer("max:azure.dbformysql_flexibleservers.storage_used{name:db}.rollup(max, 86400)", response(seriesOf("", points)));

        Collected collected = collector(probe).collect(new Params("svc", "prod", "db", null, null, null), TO);

        assertThat(collected.snapshot()).containsEntry("mysql_storage_used_gb", 1027.0);
        assertThat(collected.markdown()).contains("1027.00 GB at the last bucket (2026-11-18); 28-day growth +27.00 GB");
    }

    @Test
    void sloAttainmentAndRemainingErrorBudgetAreDerivedFromTheHistory() {
        FakeProbe probe = new FakeProbe();
        Slo slo = new Slo();
        slo.id = "abc";
        slo.name = "Availability";
        slo.type = "metric";
        SloThreshold threshold = new SloThreshold();
        threshold.target = 99.0;
        threshold.timeframe = "7d";
        slo.thresholds = List.of(threshold);
        slo.sliValue = 99.9;
        probe.slos = List.of(slo);
        Monitor monitor = new Monitor();
        monitor.id = 1L;
        monitor.name = "High latency";
        monitor.overallState = "Warn";
        probe.monitors = List.of(monitor);

        Collected collected = collector(probe).collect(SERVICE_ONLY, TO);

        assertThat(collected.snapshot()).containsEntry("slo_count", 1).containsEntry("slo_breached_count", 0)
            .containsEntry("slo_min_error_budget_remaining_pct", 90.0);
        assertThat(collected.markdown()).contains("Error Budget Remaining: 90.0000%").contains("Status: compliant").contains("High latency — Warn");
    }

    @Test
    void anUnreachableMonitorApiIsAGapRatherThanAnEmptyList() {
        FakeProbe probe = new FakeProbe();
        probe.monitors = null;

        Collected collected = collector(probe).collect(SERVICE_ONLY, TO);

        assertThat(collected.gaps()).anyMatch(gap -> gap.startsWith("Monitors"));
    }

    @Test
    void theAssembledFileCarriesEverySnapshotKeyAndTheGaps() {
        FakeProbe probe = new FakeProbe();
        probe.answer("sum:trace.undertow_http.request.hits{env:prod,service:svc}.as_count().rollup(sum, 86400)", daily(FROM, 86400.0, 28));
        Collected collected = collector(probe).collect(SERVICE_ONLY, TO);

        String file = MetricsAssembler.assemble("svc", "prod", collected,
            "- **MySQL max_connections**: 5000\n```json\n{ \"mysql_max_connections\": 5000 }\n```\n",
            "Largest table**: `t` at 0.28 GB (1 rows)", collected.windowEnd());

        assertThat(file).contains("# Collected Metrics — svc (prod)")
            .contains("**Lookback**: 28 days (2026-10-22 to 2026-11-19, UTC day-aligned)")
            .contains("## 8. Infrastructure Live State (Azure MCP)")
            .contains("## 10. Collection Gaps")
            .contains("\"avg_rps\": 1")
            .contains("\"mysql_max_connections\": 5000")
            .contains("\"largest_table_gb\": 0.28")
            .contains("\"p50_latency_ms\": null");
        MetricsAssembler.SNAPSHOT_KEYS.forEach(key -> assertThat(file).contains("\"" + key + "\":"));
    }

    private DatadogMetricsCollector collector(FakeProbe probe) {
        DatadogMetricsCollector collector = new DatadogMetricsCollector();
        collector.datadog = probe;
        return collector;
    }

    private QueryResponse daily(long from, double value, int days) {
        List<List<Double>> points = new ArrayList<>(days);
        for (int day = 0; day < days; day++) {
            points.add(List.of((double) (from + day * 86400L) * 1000, value));
        }
        return response(seriesOf("", points));
    }

    private QueryResponse dailyWithPeak(long from, double base, double peak, int peakDay) {
        return response(dailyWithPeakSeries("", from, base, peak, peakDay));
    }

    private Series dailyWithPeakSeries(String scope, long from, double base, double peak, int peakDay) {
        List<List<Double>> points = new ArrayList<>(28);
        for (int day = 0; day < 28; day++) {
            points.add(List.of((double) (from + day * 86400L) * 1000, day == peakDay ? peak : base));
        }
        return seriesOf(scope, points);
    }

    private Series series(String scope, long from, double value) {
        List<List<Double>> points = new ArrayList<>(28);
        for (int day = 0; day < 28; day++) {
            points.add(List.of((double) (from + day * 86400L) * 1000, value));
        }
        return seriesOf(scope, points);
    }

    private QueryResponse hourlySeries(long from, double base, double peak) {
        List<List<Double>> points = new ArrayList<>(168);
        for (int hour = 0; hour < 168; hour++) {
            points.add(List.of((double) (from + hour * 3600L) * 1000, hour == 30 ? peak : base));
        }
        return response(seriesOf("", points));
    }

    private Series seriesOf(String scope, List<List<Double>> points) {
        Series series = new Series();
        series.scope = scope;
        series.pointlist = points;
        return series;
    }

    private QueryResponse response(Series... series) {
        QueryResponse response = new QueryResponse();
        response.series = List.of(series);
        return response;
    }

    /** Answers by query text (and, for the weekly peak queries, by the start of the week asked for). */
    private static final class FakeProbe extends DatadogMetricProbe {
        private final java.util.Map<String, QueryResponse> answers = new java.util.HashMap<>();
        private List<Monitor> monitors = List.of();
        private List<Slo> slos = List.of();

        void answer(String query, QueryResponse response) {
            answers.put(query, response);
        }

        void answerRange(String query, long from, QueryResponse response) {
            answers.put(query + "@" + from, response);
        }

        @Override
        public QueryResponse queryRange(String query, long fromSeconds, long toSeconds) {
            QueryResponse ranged = answers.get(query + "@" + fromSeconds);
            if (ranged != null) return ranged;
            if (query.contains("rollup(sum, 3600)") && !answers.isEmpty()) return hourlyFallback(fromSeconds);
            return answers.getOrDefault(query, new QueryResponse());
        }

        private QueryResponse hourlyFallback(long from) {
            List<List<Double>> points = new ArrayList<>(168);
            for (int hour = 0; hour < 168; hour++) {
                points.add(List.of((double) (from + hour * 3600L) * 1000, 100.0));
            }
            Series series = new Series();
            series.scope = "";
            series.pointlist = points;
            QueryResponse response = new QueryResponse();
            response.series = List.of(series);
            return response;
        }

        @Override
        public List<Monitor> monitors(String service) {
            return monitors;
        }

        @Override
        public List<Slo> slos(String service, long fromSeconds, long toSeconds) {
            return slos;
        }
    }
}
