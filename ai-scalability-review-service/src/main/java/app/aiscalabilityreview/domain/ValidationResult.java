package app.aiscalabilityreview.domain;

import core.framework.mongo.Collection;
import core.framework.mongo.Field;
import core.framework.mongo.Id;

import java.time.ZonedDateTime;
import java.util.List;

@Collection(name = "validation_results")
public class ValidationResult {
    @Id
    public String validationId;

    @Field(name = "report_id")
    public String reportId;

    @Field(name = "job_id")
    public String jobId;

    @Field(name = "service_id")
    public String serviceId;

    @Field(name = "ai_model")
    public String aiModel;

    @Field(name = "status")
    public String status;  // PENDING | RUNNING | COMPLETED | FAILED

    @Field(name = "verdict")
    public String verdict;  // PASS | CONDITIONAL_PASS | FAIL

    @Field(name = "check_results")
    public List<CheckResult> checkResults;

    @Field(name = "validation_report_markdown")
    public String validationReportMarkdown;

    @Field(name = "confluence_page_id")
    public String confluencePageId;

    @Field(name = "confluence_page_url")
    public String confluencePageUrl;

    @Field(name = "input_tokens")
    public Integer inputTokens;

    @Field(name = "output_tokens")
    public Integer outputTokens;

    @Field(name = "estimated_cost_usd")
    public Double estimatedCostUsd;

    @Field(name = "triggered_by")
    public String triggeredBy;

    @Field(name = "generated_at")
    public ZonedDateTime generatedAt;

    @Field(name = "report_original_date")
    public ZonedDateTime reportOriginalDate;

    public static class CheckResult {
        @Field(name = "check_number")
        public Integer checkNumber;

        @Field(name = "check_name")
        public String checkName;

        @Field(name = "status")
        public String status;  // PASS | FAIL | WARNING | SKIPPED

        @Field(name = "issue_count")
        public Integer issueCount;
    }
}