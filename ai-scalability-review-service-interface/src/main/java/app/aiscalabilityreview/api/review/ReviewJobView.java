package app.aiscalabilityreview.api.review;

import app.aiscalabilityreview.api.localreview.LocalReviewRequestView;
import app.aiscalabilityreview.api.serviceconfig.embedded.AIModelView;
import core.framework.api.json.Property;

import java.time.ZonedDateTime;
import java.util.List;

public class ReviewJobView {
    @Property(name = "job_id")
    public String jobId;

    @Property(name = "service_id")
    public String serviceId;

    @Property(name = "status")
    public String status;  // PENDING | QUEUED | RUNNING | COMPLETED | FAILED | CANCELLED | STALE

    @Property(name = "current_stage")
    public String currentStage;

    @Property(name = "ai_model")
    public AIModelView aiModel;

    @Property(name = "trigger_type")
    public String triggerType;

    @Property(name = "triggered_by")
    public String triggeredBy;

    @Property(name = "note")
    public String note;

    @Property(name = "labels")
    public List<String> labels;

    @Property(name = "baseline")
    public Boolean baseline;

    @Property(name = "started_at")
    public ZonedDateTime startedAt;

    @Property(name = "completed_at")
    public ZonedDateTime completedAt;

    @Property(name = "cancelled_at")
    public ZonedDateTime cancelledAt;

    @Property(name = "cancelled_by")
    public String cancelledBy;

    @Property(name = "report_id")
    public String reportId;

    @Property(name = "error_message")
    public String errorMessage;

    @Property(name = "env")
    public String env;

    @Property(name = "namespace")
    public String namespace;

    @Property(name = "domain")
    public String domain;

    @Property(name = "output_dir")
    public String outputDir;

    @Property(name = "request")
    public LocalReviewRequestView request;

    @Property(name = "acknowledged_warnings")
    public List<String> acknowledgedWarnings;

    @Property(name = "totals")
    public RunTotalsView totals;

    @Property(name = "fingerprint")
    public FingerprintView fingerprint;

    @Property(name = "overall_score")
    public String overallScore;

    @Property(name = "validation_verdict")
    public String validationVerdict;

    @Property(name = "validation_result_id")
    public String validationResultId;

    @Property(name = "stages")
    public List<StageView> stages;

    public static class RunTotalsView {
        @Property(name = "input_tokens")
        public Long inputTokens;

        @Property(name = "output_tokens")
        public Long outputTokens;

        @Property(name = "cached_tokens")
        public Long cachedTokens;

        @Property(name = "total_tokens")
        public Long totalTokens;

        @Property(name = "duration_ms")
        public Long durationMs;

        @Property(name = "mcp_tool_calls")
        public Integer mcpToolCalls;

        @Property(name = "estimated_cost_usd")
        public Double estimatedCostUsd;

        @Property(name = "within_budget")
        public Boolean withinBudget;

        @Property(name = "token_budget")
        public Long tokenBudget;

        @Property(name = "duration_budget_ms")
        public Long durationBudgetMs;

        /** Share of input tokens served from cache, 0–1; priced differently, so shown separately. */
        @Property(name = "cache_hit_ratio")
        public Double cacheHitRatio;

        @Property(name = "price_table_version")
        public String priceTableVersion;
    }

    public static class FingerprintView {
        @Property(name = "cli_version")
        public String cliVersion;

        @Property(name = "prompt_version")
        public String promptVersion;

        @Property(name = "repo_sha_app")
        public String repoShaApp;

        @Property(name = "repo_sha_infra")
        public String repoShaInfra;

        @Property(name = "repo_dirty_app")
        public Boolean repoDirtyApp;

        @Property(name = "repo_dirty_infra")
        public Boolean repoDirtyInfra;

        @Property(name = "azure_resource_group")
        public String azureResourceGroup;
    }

    public static class StageView {
        @Property(name = "name")
        public String name;

        @Property(name = "status")
        public String status;

        @Property(name = "started_at")
        public ZonedDateTime startedAt;

        @Property(name = "completed_at")
        public ZonedDateTime completedAt;

        @Property(name = "duration_ms")
        public Long durationMs;

        @Property(name = "error")
        public String error;

        /** Known-cause hint for {@code error}, resolved server side (FR-3.7). */
        @Property(name = "error_hint")
        public String errorHint;

        @Property(name = "output_summary")
        public String outputSummary;

        @Property(name = "model")
        public String model;

        @Property(name = "input_tokens")
        public Long inputTokens;

        @Property(name = "output_tokens")
        public Long outputTokens;

        @Property(name = "cached_tokens")
        public Long cachedTokens;

        @Property(name = "total_tokens")
        public Long totalTokens;

        @Property(name = "api_requests")
        public Integer apiRequests;

        @Property(name = "tool_calls")
        public Integer toolCalls;

        @Property(name = "mcp_tool_calls")
        public Integer mcpToolCalls;

        @Property(name = "estimated_cost_usd")
        public Double estimatedCostUsd;

        @Property(name = "artifacts")
        public List<String> artifacts;

        @Property(name = "log_file")
        public String logFile;

        @Property(name = "session_id")
        public String sessionId;
    }
}
