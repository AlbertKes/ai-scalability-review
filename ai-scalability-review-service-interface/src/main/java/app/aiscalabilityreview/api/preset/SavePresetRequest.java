package app.aiscalabilityreview.api.preset;

import app.aiscalabilityreview.api.localreview.LocalReviewRequestView;
import core.framework.api.json.Property;
import core.framework.api.validate.NotBlank;
import core.framework.api.validate.NotNull;

/**
 * Creates or replaces a preset. The payload is the form as it stands, so a preset can be saved
 * before it is complete; the rules in FR-1.13 run when it is used, not when it is stored.
 */
public class SavePresetRequest {
    /** Null creates a preset, non-null replaces that one. */
    @Property(name = "preset_id")
    public String presetId;

    @NotNull
    @NotBlank
    @Property(name = "service_id")
    public String serviceId;

    @Property(name = "env")
    public String env;

    @NotNull
    @NotBlank
    @Property(name = "name")
    public String name;

    @NotNull
    @Property(name = "request")
    public LocalReviewRequestView request;

    @Property(name = "is_default")
    public Boolean isDefault;

    @NotNull
    @NotBlank
    @Property(name = "created_by")
    public String createdBy;
}
