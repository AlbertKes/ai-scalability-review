package app.aiscalabilityreview.domain;

import app.aiscalabilityreview.domain.embedded.AIModel;
import app.aiscalabilityreview.domain.embedded.LocalReviewRequestSnapshot;
import core.framework.mongo.Collection;
import core.framework.mongo.Field;
import core.framework.mongo.Id;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

@Collection(name = "review_jobs")
public class ReviewJob {
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_QUEUED = "QUEUED";
    public static final String STATUS_RUNNING = "RUNNING";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_CANCELLED = "CANCELLED";
    /**
     * A run whose process no longer exists — the service restarted while it was in flight. Kept
     * distinct from FAILED because nothing is known about why it stopped, and from RUNNING because
     * a spinner that never resolves is worse than an honest dead end.
     */
    public static final String STATUS_STALE = "STALE";

    @Id
    public String jobId;

    @Field(name = "service_id")
    public String serviceId;

    @Field(name = "trigger_type")
    public String triggerType;  // MANUAL | SCHEDULED

    @Field(name = "triggered_by")
    public String triggeredBy;

    @Field(name = "ai_model")
    public AIModel aiModel;

    @Field(name = "status")
    public String status;  // see the STATUS_* constants

    @Field(name = "current_stage")
    public String currentStage;

    @Field(name = "stage_statuses")
    public Map<String, StageStatus> stageStatuses;

    @Field(name = "feedback_applied")
    public List<String> feedbackApplied;

    @Field(name = "note")
    public String note;

    @Field(name = "started_at")
    public ZonedDateTime startedAt;

    @Field(name = "completed_at")
    public ZonedDateTime completedAt;

    @Field(name = "report_id")
    public String reportId;

    @Field(name = "error_message")
    public String errorMessage;

    // ---- local review context, so the console can describe and repeat a run (FR-1.7, FR-2.2) ----

    @Field(name = "env")
    public String env;

    @Field(name = "namespace")
    public String namespace;

    @Field(name = "domain")
    public String domain;

    @Field(name = "request")
    public LocalReviewRequestSnapshot request;

    /** Absolute path of this run's own artifact directory; one directory per run, never shared. */
    @Field(name = "output_dir")
    public String outputDir;

    @Field(name = "acknowledged_warnings")
    public List<String> acknowledgedWarnings;

    @Field(name = "labels")
    public List<String> labels;

    @Field(name = "baseline")
    public Boolean baseline;

    @Field(name = "cancelled_at")
    public ZonedDateTime cancelledAt;

    @Field(name = "cancelled_by")
    public String cancelledBy;

    /** Set when this run was started as a retry of an earlier one, and from which stage. */
    @Field(name = "retry_of_job_id")
    public String retryOfJobId;

    @Field(name = "retry_from_stage")
    public String retryFromStage;

    // ---- run totals, denormalised so the list can filter and sort without re-aggregating ----

    @Field(name = "total_input_tokens")
    public Long totalInputTokens;

    @Field(name = "total_output_tokens")
    public Long totalOutputTokens;

    @Field(name = "total_cached_tokens")
    public Long totalCachedTokens;

    @Field(name = "total_tokens")
    public Long totalTokens;

    @Field(name = "total_duration_ms")
    public Long totalDurationMs;

    @Field(name = "total_mcp_tool_calls")
    public Integer totalMcpToolCalls;

    @Field(name = "estimated_cost_usd")
    public Double estimatedCostUsd;

    @Field(name = "within_budget")
    public Boolean withinBudget;

    @Field(name = "overall_score")
    public String overallScore;

    @Field(name = "validation_verdict")
    public String validationVerdict;  // PASS | CONDITIONAL_PASS | FAIL

    @Field(name = "validation_result_id")
    public String validationResultId;

    // ---- reproducibility fingerprint (FR-6.7) ----

    @Field(name = "cli_version")
    public String cliVersion;

    @Field(name = "prompt_version")
    public String promptVersion;

    @Field(name = "repo_sha_app")
    public String repoShaApp;

    @Field(name = "repo_sha_infra")
    public String repoShaInfra;

    @Field(name = "repo_dirty_app")
    public Boolean repoDirtyApp;

    @Field(name = "repo_dirty_infra")
    public Boolean repoDirtyInfra;

    @Field(name = "azure_resource_group")
    public String azureResourceGroup;

    public static class StageStatus {
        @Field(name = "status")
        public String status;  // PENDING | RUNNING | COMPLETED | FAILED | SKIPPED | CANCELLED

        @Field(name = "started_at")
        public ZonedDateTime startedAt;

        @Field(name = "completed_at")
        public ZonedDateTime completedAt;

        @Field(name = "duration_ms")
        public Long durationMs;

        @Field(name = "error_message")
        public String errorMessage;

        @Field(name = "output_summary")
        public String outputSummary;

        // ---- cost / latency instrumentation (AD-6383) ----

        @Field(name = "model")
        public String model;

        @Field(name = "input_tokens")
        public Long inputTokens;

        @Field(name = "output_tokens")
        public Long outputTokens;

        @Field(name = "cached_tokens")
        public Long cachedTokens;

        @Field(name = "total_tokens")
        public Long totalTokens;

        @Field(name = "api_requests")
        public Integer apiRequests;

        @Field(name = "tool_calls")
        public Integer toolCalls;

        @Field(name = "mcp_tool_calls")
        public Integer mcpToolCalls;

        @Field(name = "estimated_cost_usd")
        public Double estimatedCostUsd;

        // ---- provenance (FR-6.5, FR-6.6) ----

        /** Artifact file names this stage wrote, relative to the run's output directory. */
        @Field(name = "artifacts")
        public List<String> artifacts;

        /** Log file name, relative to the run's output directory, holding the CLI's stderr. */
        @Field(name = "log_file")
        public String logFile;

        @Field(name = "session_id")
        public String sessionId;

        /** The raw {@code stats} object of the Gemini CLI JSON envelope, kept verbatim. */
        @Field(name = "raw_stats_json")
        public String rawStatsJson;
    }

    public static class DeploymentConfig {
        @Field(name = "name")
        public String name;

        @Field(name = "type")
        public String type;
    }
}
