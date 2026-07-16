package app.aiscalabilityreview.api.serviceconfig;

import core.framework.api.json.Property;

import java.time.ZonedDateTime;

public class ServiceConfigView {
    @Property(name = "service_id")
    public String serviceId;

    @Property(name = "display_name")
    public String displayName;

    @Property(name = "team")
    public String team;

    @Property(name = "tier")
    public String tier;

    @Property(name = "enabled")
    public Boolean enabled;

    @Property(name = "review_schedule")
    public String reviewSchedule;

    @Property(name = "ai_model")
    public String aiModel;

    @Property(name = "environment")
    public String environment;

    @Property(name = "created_at")
    public ZonedDateTime createdAt;

    @Property(name = "updated_at")
    public ZonedDateTime updatedAt;
}