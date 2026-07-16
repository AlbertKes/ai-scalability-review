package app.aiscalabilityreview.api.review;

import core.framework.api.json.Property;

import java.time.ZonedDateTime;
import java.util.List;

public class CompareReviewResponse {
    @Property(name = "comparison_id")
    public String comparisonId;

    @Property(name = "overall_trajectory")
    public String overallTrajectory;

    @Property(name = "summary_text")
    public String summaryText;

    @Property(name = "dimension_diffs")
    public List<DimensionDiff> dimensionDiffs;

    @Property(name = "generated_at")
    public ZonedDateTime generatedAt;

    public static class DimensionDiff {
        @Property(name = "dimension")
        public String dimension;

        @Property(name = "score_a")
        public String scoreA;

        @Property(name = "score_b")
        public String scoreB;

        @Property(name = "change")
        public String change;

        @Property(name = "key_changes")
        public List<String> keyChanges;
    }
}