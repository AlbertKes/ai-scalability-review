package app.aiscalabilityreview.domain;

import core.framework.mongo.Collection;
import core.framework.mongo.Field;
import core.framework.mongo.Id;

import java.time.ZonedDateTime;

@Collection(name = "audit_logs")
public class AuditLog {
    @Id
    public String logId;

    @Field(name = "job_id")
    public String jobId;

    @Field(name = "service_id")
    public String serviceId;

    @Field(name = "event_type")
    public String eventType;

    @Field(name = "actor")
    public String actor;

    @Field(name = "target")
    public String target;

    @Field(name = "request_summary")
    public String requestSummary;

    @Field(name = "request_token_count")
    public Integer requestTokenCount;

    @Field(name = "response_status_code")
    public Integer responseStatusCode;

    @Field(name = "response_duration_ms")
    public Long responseDurationMs;

    @Field(name = "response_summary")
    public String responseSummary;

    @Field(name = "response_error_message")
    public String responseErrorMessage;

    @Field(name = "success")
    public Boolean success;

    @Field(name = "timestamp")
    public ZonedDateTime timestamp;
}