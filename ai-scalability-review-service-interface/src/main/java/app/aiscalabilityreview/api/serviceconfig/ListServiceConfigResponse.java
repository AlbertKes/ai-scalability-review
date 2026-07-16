package app.aiscalabilityreview.api.serviceconfig;

import core.framework.api.json.Property;

import java.util.List;

public class ListServiceConfigResponse {
    @Property(name = "items")
    public List<ServiceConfigView> items;

    @Property(name = "total")
    public Integer total;
}