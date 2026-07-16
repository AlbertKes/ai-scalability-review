package app.aiscalabilityreview.api;

import app.aiscalabilityreview.api.serviceconfig.ListServiceConfigResponse;
import app.aiscalabilityreview.api.serviceconfig.UpsertServiceConfigRequest;
import app.aiscalabilityreview.api.serviceconfig.UpsertServiceConfigResponse;
import core.framework.api.web.service.GET;
import core.framework.api.web.service.PUT;
import core.framework.api.web.service.Path;

public interface ServiceConfigWebService {
    @PUT
    @Path("/service-config/upsert")
    UpsertServiceConfigResponse upsert(UpsertServiceConfigRequest request);

    @GET
    @Path("/service-config")
    ListServiceConfigResponse list();
}
