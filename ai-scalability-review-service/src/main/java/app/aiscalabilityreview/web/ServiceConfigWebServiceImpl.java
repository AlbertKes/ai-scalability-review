package app.aiscalabilityreview.web;

import app.aiscalabilityreview.api.ServiceConfigWebService;
import app.aiscalabilityreview.api.serviceconfig.ListServiceConfigResponse;
import app.aiscalabilityreview.api.serviceconfig.ServiceConfigView;
import app.aiscalabilityreview.api.serviceconfig.UpsertServiceConfigRequest;
import app.aiscalabilityreview.api.serviceconfig.UpsertServiceConfigResponse;
import app.aiscalabilityreview.domain.ServiceConfig;
import app.aiscalabilityreview.service.ReviewService;
import core.framework.inject.Inject;
import core.framework.log.ActionLogContext;

import java.util.List;

public class ServiceConfigWebServiceImpl implements ServiceConfigWebService {
    @Inject
    ReviewService reviewService;

    @Override
    public UpsertServiceConfigResponse upsert(UpsertServiceConfigRequest request) {
        if (request.id != null) {
            ActionLogContext.put("service_id", request.id);
        }
        return reviewService.upsertServiceConfig(request.id, request);
    }

    @Override
    public ListServiceConfigResponse list() {
        List<ServiceConfig> configs = reviewService.listServiceConfigs();
        List<ServiceConfigView> views = configs.stream().map(this::toView).toList();
        var response = new ListServiceConfigResponse();
        response.items = views;
        response.total = views.size();
        return response;
    }

    private ServiceConfigView toView(ServiceConfig config) {
        ServiceConfigView view = new ServiceConfigView();
        view.serviceId = config.serviceId;
        view.displayName = config.displayName;
        view.team = config.team;
        view.tier = config.tier;
        view.enabled = config.enabled;
        view.reviewSchedule = config.reviewSchedule;
        view.aiModel = config.aiModel;
        view.environment = config.environment;
        view.createdAt = config.createdAt;
        view.updatedAt = config.updatedAt;
        return view;
    }
}
