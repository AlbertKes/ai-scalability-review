package app.aiscalabilityreview.api.preset;

import core.framework.api.web.service.QueryParam;

public class ListPresetRequest {
    @QueryParam(name = "service_id")
    public String serviceId;

    @QueryParam(name = "env")
    public String env;
}
