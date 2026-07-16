package app.aiscalabilityreview.api.serviceconfig;

import core.framework.api.json.Property;
import core.framework.api.validate.NotBlank;
import core.framework.api.validate.NotNull;

public class UpsertServiceConfigResponse {
    @NotNull
    @NotBlank
    @Property(name = "id")
    public String id;
}