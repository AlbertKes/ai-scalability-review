package app.aiscalabilityreview.api.review;

import app.aiscalabilityreview.api.serviceconfig.embedded.AIModelView;
import core.framework.api.json.Property;
import core.framework.api.validate.NotBlank;
import core.framework.api.validate.NotNull;

public class TriggerReviewRequest {
    @NotNull
    @NotBlank
    @Property(name = "service_id")
    public String serviceId;

    @Property(name = "model")
    public AIModelView model;  // optional override, e.g. "claude-sonnet-4-6"

    @Property(name = "note")
    public String note;  // optional label for manual triggers
}