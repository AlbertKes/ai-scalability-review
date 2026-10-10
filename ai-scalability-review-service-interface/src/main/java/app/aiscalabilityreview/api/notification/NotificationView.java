package app.aiscalabilityreview.api.notification;

import core.framework.api.json.Property;

import java.time.ZonedDateTime;

public class NotificationView {
    @Property(name = "notification_id")
    public String notificationId;

    @Property(name = "job_id")
    public String jobId;

    @Property(name = "service_id")
    public String serviceId;

    @Property(name = "type")
    public String type;

    @Property(name = "severity")
    public String severity;  // SUCCESS | WARNING | ERROR

    @Property(name = "title")
    public String title;

    @Property(name = "body")
    public String body;

    @Property(name = "created_at")
    public ZonedDateTime createdAt;

    @Property(name = "read_at")
    public ZonedDateTime readAt;
}
