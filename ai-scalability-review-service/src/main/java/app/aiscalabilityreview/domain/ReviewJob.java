package app.aiscalabilityreview.domain;

import app.aiscalabilityreview.domain.embedded.AIModel;
import core.framework.mongo.Collection;
import core.framework.mongo.Field;
import core.framework.mongo.Id;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

@Collection(name = "review_jobs")
public class ReviewJob {
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
    public String status;  // PENDING | RUNNING | COMPLETED | FAILED

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

    public static class StageStatus {
        @Field(name = "status")
        public String status;  // PENDING | RUNNING | COMPLETED | FAILED | SKIPPED

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
    }

    public static class DeploymentConfig {
        @Field(name = "name")
        public String name;

        @Field(name = "type")
        public String type;
    }
}