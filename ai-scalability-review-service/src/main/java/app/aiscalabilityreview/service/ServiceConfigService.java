package app.aiscalabilityreview.service;

import app.aiscalabilityreview.api.serviceconfig.ListServiceConfigResponse;
import app.aiscalabilityreview.api.serviceconfig.UpsertServiceConfigRequest;
import app.aiscalabilityreview.api.serviceconfig.UpsertServiceConfigResponse;
import app.aiscalabilityreview.api.serviceconfig.embedded.AIModelView;
import app.aiscalabilityreview.api.serviceconfig.embedded.EnvironmentView;
import app.aiscalabilityreview.api.serviceconfig.embedded.ServiceConfigView;
import app.aiscalabilityreview.api.serviceconfig.embedded.ServiceTierView;
import app.aiscalabilityreview.domain.ServiceConfig;
import app.aiscalabilityreview.domain.embedded.AIModel;
import app.aiscalabilityreview.domain.embedded.Environment;
import app.aiscalabilityreview.domain.embedded.HPAType;
import app.aiscalabilityreview.domain.embedded.Repositories;
import app.aiscalabilityreview.domain.embedded.ReviewConfig;
import app.aiscalabilityreview.domain.embedded.Runtime;
import app.aiscalabilityreview.domain.embedded.ServiceTier;
import com.mongodb.ReadPreference;
import com.mongodb.client.model.Filters;
import core.framework.inject.Inject;
import core.framework.mongo.MongoCollection;
import core.framework.mongo.Query;
import core.framework.util.Strings;
import core.framework.web.exception.NotFoundException;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

public class ServiceConfigService {
    @Inject
    MongoCollection<ServiceConfig> serviceConfigCollection;

    public ServiceConfig getServiceConfig(String serviceId) {
        return serviceConfigCollection.get(serviceId).orElseThrow(
            () -> new NotFoundException(Strings.format("service config not found, id = {}", serviceId)));
    }

    public ListServiceConfigResponse list() {
        var query = new Query();
        query.readPreference = ReadPreference.secondaryPreferred();
        List<ServiceConfig> configs = serviceConfigCollection.find(query);
        List<ServiceConfigView> views = configs.stream().map(this::toView).toList();
        var response = new ListServiceConfigResponse();
        response.items = views;
        response.total = views.size();
        return response;
    }

    public List<ServiceConfig> listEnabledServiceConfigs() {
        Query query = new Query();
        query.filter = Filters.eq("enabled", Boolean.TRUE);
        return serviceConfigCollection.find(query);
    }

    public UpsertServiceConfigResponse upsertServiceConfig(String serviceId, UpsertServiceConfigRequest request) {
        ServiceConfig config = serviceId == null ? new ServiceConfig() : getServiceConfig(serviceId);
        config.displayName = request.displayName;
        config.team = request.team;
        config.tier = ServiceTier.valueOf(request.tier.name());
        config.enabled = request.enabled == null || request.enabled;
        buildRepositoriesAndReview(request, config);
        buildRuntime(request, config);
        var now = ZonedDateTime.now();
        config.updatedBy = request.operator;
        config.updatedTime = now;
        if (serviceId == null) {
            config.serviceId = UUID.randomUUID().toString();
            config.createdBy = request.operator;
            config.createdTime = now;
            serviceConfigCollection.insert(config);
        } else {
            serviceConfigCollection.replace(config);
        }
        var response = new UpsertServiceConfigResponse();
        response.id = config.serviceId;
        return response;
    }

    private void buildRepositoriesAndReview(UpsertServiceConfigRequest request, ServiceConfig config) {
        config.repositories = new Repositories();
        config.repositories.app = new Repositories.RepoConfig();
        config.repositories.app.url = request.repositories.app.url;
        config.repositories.app.branch = request.repositories.app.branch;
        config.repositories.app.servicePath = request.repositories.app.servicePath;
        config.repositories.app.includePaths = request.repositories.app.includePaths;
        config.repositories.app.excludePaths = request.repositories.app.excludePaths;
        if (request.repositories.infra != null) {
            config.repositories.infra = new Repositories.RepoConfig();
            config.repositories.infra.url = request.repositories.infra.url;
            config.repositories.infra.branch = request.repositories.infra.branch;
            config.repositories.infra.servicePath = request.repositories.infra.servicePath;
        }
        if (request.repositories.k8sGitops != null) {
            config.repositories.k8sGitops = new Repositories.RepoConfig();
            config.repositories.k8sGitops.url = request.repositories.k8sGitops.url;
            config.repositories.k8sGitops.branch = request.repositories.k8sGitops.branch;
            config.repositories.k8sGitops.servicePath = request.repositories.k8sGitops.servicePath;
        }
        config.reviewConfig = new ReviewConfig();
        config.reviewConfig.schedule = request.reviewConfig.schedule;
        config.reviewConfig.aiModel = AIModel.valueOf(request.reviewConfig.aiModel.name());
        config.reviewConfig.metricLookbackDays = request.reviewConfig.metricLookbackDays;
        config.reviewConfig.confluenceSpaceKey = request.reviewConfig.confluenceSpaceKey;
        config.reviewConfig.confluenceParentPageTitle = request.reviewConfig.confluenceParentPageTitle;
    }

    private void buildRuntime(UpsertServiceConfigRequest request, ServiceConfig config) {
        config.runtime = new Runtime();
        config.runtime.environment = Environment.valueOf(request.runtime.environment.name());
        config.runtime.namespace = request.runtime.namespace;
        config.runtime.deployments = request.runtime.deployments.stream().map(d -> {
            var dc = new Runtime.DeploymentConfig();
            dc.name = d.name;
            dc.type = d.type;
            return dc;
        }).toList();
        config.runtime.hpaType = HPAType.valueOf(request.runtime.hpaType.name());
        config.runtime.kafkaConsumerGroups = request.runtime.kafkaConsumerGroups;
        config.runtime.mysqlHost = request.runtime.mysqlHost;
        config.runtime.mysqlDB = request.runtime.mysqlDB;
        config.runtime.atlasCluster = request.runtime.atlasCluster;
        config.runtime.redisEnabled = request.runtime.redisEnabled;
        config.runtime.domain = request.runtime.domain;
    }

    private ServiceConfigView toView(ServiceConfig config) {
        ServiceConfigView view = new ServiceConfigView();
        view.serviceId = config.serviceId;
        view.displayName = config.displayName;
        view.team = config.team;
        view.tier = ServiceTierView.valueOf(config.tier.name());
        view.enabled = config.enabled;
        view.reviewSchedule = config.reviewConfig.schedule;
        view.aiModel = AIModelView.valueOf(config.reviewConfig.aiModel.name());
        view.environment = EnvironmentView.valueOf(config.runtime.environment.name());
        view.createdAt = config.createdTime;
        view.updatedAt = config.updatedTime;
        return view;
    }
}
