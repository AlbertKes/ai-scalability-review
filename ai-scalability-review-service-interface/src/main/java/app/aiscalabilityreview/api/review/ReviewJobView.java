package app.aiscalabilityreview.api.review;

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
    public String status;  // PENDING | RUNNING | COMPLETED | FAILED

    @Property(name = "current_stage")
    public String currentStage;

    @Property(name = "ai_model")
    public AIModelView aiModel;

    @Property(name = "trigger_type")
    public String triggerType;

    @Property(name = "note")
    public String note;

    @Property(name = "started_at")
    public ZonedDateTime startedAt;

    @Property(name = "completed_at")
    public ZonedDateTime completedAt;

    @Property(name = "report_id")
    public String reportId;

    @Property(name = "error_message")
    public String errorMessage;

    @Property(name = "stages")
    public List<StageView> stages;

    public static class StageView {
        @Property(name = "name")
        public String name;

        @Property(name = "status")
        public String status;

        @Property(name = "duration_ms")
        public Long durationMs;

        @Property(name = "error")
        public String error;
    }
}