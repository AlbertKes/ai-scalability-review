package app.aiscalabilityreview.api.review;

import core.framework.api.json.Property;

import java.time.ZonedDateTime;

public class GetReviewReportValidationResponse {
    @Property(name = "validation_id")
    public String validationId;

    @Property(name = "report_id")
    public String reportId;

    @Property(name = "service_id")
    public String serviceId;

    @Property(name = "ai_model")
    public String aiModel;

    @Property(name = "status")
    public String status;

    @Property(name = "verdict")
    public String verdict;

    @Property(name = "input_tokens")
    public Integer inputTokens;

    @Property(name = "output_tokens")
    public Integer outputTokens;

    @Property(name = "estimated_cost_usd")
    public Double estimatedCostUsd;

    @Property(name = "confluence_page_url")
    public String confluencePageUrl;

    @Property(name = "generated_at")
    public ZonedDateTime generatedAt;

    @Property(name = "validation_report_markdown")
    public String validationReportMarkdown;
}
