package app.aiscalabilityreview.api.preset;

import app.aiscalabilityreview.api.localreview.LocalReviewRequestView;
import core.framework.api.json.Property;

import java.time.ZonedDateTime;

public class PresetView {
    @Property(name = "preset_id")
    public String presetId;

    @Property(name = "service_id")
    public String serviceId;

    @Property(name = "env")
    public String env;

    @Property(name = "name")
    public String name;

    @Property(name = "request")
    public LocalReviewRequestView request;

    @Property(name = "is_default")
    public Boolean isDefault;

    @Property(name = "created_by")
    public String createdBy;

    @Property(name = "created_at")
    public ZonedDateTime createdAt;

    @Property(name = "updated_at")
    public ZonedDateTime updatedAt;
}
