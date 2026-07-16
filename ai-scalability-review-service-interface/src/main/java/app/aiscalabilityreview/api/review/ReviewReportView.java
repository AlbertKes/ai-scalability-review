package app.aiscalabilityreview.api.review;

import core.framework.api.json.Property;

import java.time.ZonedDateTime;

public class ReviewReportView {
    @Property(name = "report_id")
    public String reportId;

    @Property(name = "job_id")
    public String jobId;

    @Property(name = "service_id")
    public String serviceId;

    @Property(name = "period_label")
    public String periodLabel;

    @Property(name = "ai_model")
    public String aiModel;

    @Property(name = "report_markdown")
    public String reportMarkdown;

    @Property(name = "confluence_page_url")
    public String confluencePageUrl;

    @Property(name = "scores")
    public ScoresView scores;

    @Property(name = "generated_at")
    public ZonedDateTime generatedAt;

    public static class ScoresView {
        @Property(name = "traffic")
        public String traffic;

        @Property(name = "latency")
        public String latency;

        @Property(name = "errors")
        public String errors;

        @Property(name = "resources")
        public String resources;

        @Property(name = "persistence")
        public String persistence;

        @Property(name = "overall")
        public String overall;
    }
}