package app.aiscalabilityreview.api.serviceconfig;

import app.aiscalabilityreview.api.serviceconfig.embedded.ServiceConfigView;
import core.framework.api.json.Property;
import core.framework.api.validate.NotNull;

import java.util.List;

public class ListServiceConfigResponse {
    @NotNull
    @Property(name = "items")
    public List<ServiceConfigView> items = List.of();

    @NotNull
    @Property(name = "total")
    public Integer total;
}