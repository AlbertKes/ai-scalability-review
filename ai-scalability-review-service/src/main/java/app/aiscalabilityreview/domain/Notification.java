package app.aiscalabilityreview.domain;

import core.framework.mongo.Collection;
import core.framework.mongo.Field;
import core.framework.mongo.Id;

import java.time.ZonedDateTime;

/**
 * One console notification. Persisted rather than held in the page so the unread badge survives a
 * reload — a review takes minutes, and the tab that started it is usually not the tab that reads the
 * result.
 */
@Collection(name = "notifications")
public class Notification {
    @Id
    public String notificationId;

    @Field(name = "job_id")
    public String jobId;

    @Field(name = "service_id")
    public String serviceId;

    /** RUN_COMPLETED | RUN_FAILED | RUN_CANCELLED | STAGE_FAILED | OVER_BUDGET | VALIDATION_FAILED | RED_SCORE | RUN_STALE */
    @Field(name = "type")
    public String type;

    /** SUCCESS | WARNING | ERROR */
    @Field(name = "severity")
    public String severity;

    @Field(name = "title")
    public String title;

    @Field(name = "body")
    public String body;

    @Field(name = "created_at")
    public ZonedDateTime createdAt;

    @Field(name = "read_at")
    public ZonedDateTime readAt;
}
