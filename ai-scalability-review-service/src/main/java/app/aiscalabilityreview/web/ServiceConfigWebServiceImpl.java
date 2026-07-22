package app.aiscalabilityreview.web;

import app.aiscalabilityreview.api.ServiceConfigWebService;
import app.aiscalabilityreview.api.serviceconfig.ListServiceConfigResponse;
import app.aiscalabilityreview.api.serviceconfig.UpsertServiceConfigRequest;
import app.aiscalabilityreview.api.serviceconfig.UpsertServiceConfigResponse;
import app.aiscalabilityreview.service.ServiceConfigService;
import core.framework.inject.Inject;
import core.framework.log.ActionLogContext;

public class ServiceConfigWebServiceImpl implements ServiceConfigWebService {
    @Inject
    ServiceConfigService serviceConfigService;

    @Override
    public UpsertServiceConfigResponse upsert(UpsertServiceConfigRequest request) {
        if (request.id != null) {
            ActionLogContext.put("service_id", request.id);
        }
        return serviceConfigService.upsertServiceConfig(request.id, request);
    }

    @Override
    public ListServiceConfigResponse list() {
        return serviceConfigService.list();
    }
}
