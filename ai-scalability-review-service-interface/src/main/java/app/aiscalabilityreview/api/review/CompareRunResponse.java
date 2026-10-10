package app.aiscalabilityreview.api.review;

import core.framework.api.json.Property;

import java.util.List;

/**
 * Deterministic diff of two runs (FR-5.3).
 *
 * <p>Computed from the stored numbers rather than written by a model, so it is available for every
 * pair of runs and says the same thing every time it is asked.
 */
public class CompareRunResponse {
    @Property(name = "service_id")
    public String serviceId;

    @Property(name = "run_a")
    public RunRefView runA;

    @Property(name = "run_b")
    public RunRefView runB;

    @Property(name = "score_diffs")
    public List<ScoreDiffView> scoreDiffs;

    @Property(name = "metric_diffs")
    public List<MetricDiffView> metricDiffs;

    @Property(name = "recommendation_diffs")
    public List<MetricDiffView> recommendationDiffs;

    @Property(name = "cost_diffs")
    public List<MetricDiffView> costDiffs;

    /** Infra configuration lines that differ between the two runs' infra-context artifacts. */
    @Property(name = "config_drift")
    public List<ConfigDriftView> configDrift;

    public static class RunRefView {
        @Property(name = "job_id")
        public String jobId;

        @Property(name = "report_id")
        public String reportId;

        @Property(name = "label")
        public String label;

        @Property(name = "env")
        public String env;

        @Property(name = "overall_score")
        public String overallScore;

        @Property(name = "validation_verdict")
        public String validationVerdict;
    }

    public static class ScoreDiffView {
        @Property(name = "dimension")
        public String dimension;

        @Property(name = "score_a")
        public String scoreA;

        @Property(name = "score_b")
        public String scoreB;

        @Property(name = "change")
        public String change;  // IMPROVED | DEGRADED | UNCHANGED | UNKNOWN
    }

    public static class MetricDiffView {
        @Property(name = "metric")
        public String metric;

        @Property(name = "unit")
        public String unit;

        @Property(name = "value_a")
        public Double valueA;

        @Property(name = "value_b")
        public Double valueB;

        @Property(name = "delta")
        public Double delta;

        @Property(name = "delta_pct")
        public Double deltaPct;

        /** Whether a rise is good or bad for this metric, so the UI can colour it correctly. */
        @Property(name = "higher_is_worse")
        public Boolean higherIsWorse;
    }

    public static class ConfigDriftView {
        @Property(name = "key")
        public String key;

        @Property(name = "value_a")
        public String valueA;

        @Property(name = "value_b")
        public String valueB;
    }
}
