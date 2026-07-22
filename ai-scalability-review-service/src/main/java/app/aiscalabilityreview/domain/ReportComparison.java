package app.aiscalabilityreview.domain;

import app.aiscalabilityreview.domain.embedded.AIModel;
import core.framework.mongo.Collection;
import core.framework.mongo.Field;
import core.framework.mongo.Id;

import java.time.ZonedDateTime;
import java.util.List;

@Collection(name = "report_comparisons")
public class ReportComparison {
    @Id
    public String comparisonId;

    @Field(name = "service_id")
    public String serviceId;

    @Field(name = "report_id_a")
    public String reportIdA;

    @Field(name = "report_id_b")
    public String reportIdB;

    @Field(name = "period_a")
    public String periodA;

    @Field(name = "period_b")
    public String periodB;

    @Field(name = "ai_model")
    public AIModel aiModel;

    @Field(name = "overall_trajectory")
    public String overallTrajectory;

    @Field(name = "dimension_diffs")
    public List<DimensionDiff> dimensionDiffs;

    @Field(name = "summary_text")
    public String summaryText;

    @Field(name = "confluence_page_id")
    public String confluencePageId;

    @Field(name = "requested_by")
    public String requestedBy;

    @Field(name = "generated_at")
    public ZonedDateTime generatedAt;

    public static class DimensionDiff {
        @Field(name = "dimension")
        public String dimension;

        @Field(name = "score_a")
        public String scoreA;

        @Field(name = "score_b")
        public String scoreB;

        @Field(name = "change")
        public String change;  // IMPROVED | DEGRADED | UNCHANGED

        @Field(name = "key_changes")
        public List<String> keyChanges;

        @Field(name = "new_issues")
        public List<String> newIssues;

        @Field(name = "resolved_issues")
        public List<String> resolvedIssues;
    }
}