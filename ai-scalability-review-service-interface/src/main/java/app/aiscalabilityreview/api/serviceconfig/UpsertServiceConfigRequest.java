package app.aiscalabilityreview.api.serviceconfig;

import app.aiscalabilityreview.api.serviceconfig.embedded.AIModelView;
import app.aiscalabilityreview.api.serviceconfig.embedded.EnvironmentView;
import app.aiscalabilityreview.api.serviceconfig.embedded.HPATypeView;
import app.aiscalabilityreview.api.serviceconfig.embedded.ServiceTierView;
import core.framework.api.json.Property;
import core.framework.api.validate.NotBlank;
import core.framework.api.validate.NotNull;

import java.util.List;

public class UpsertServiceConfigRequest {
    @NotBlank
    @Property(name = "id")
    public String id;

    @NotNull
    @NotBlank
    @Property(name = "display_name")
    public String displayName;

    @NotNull
    @NotBlank
    @Property(name = "team")
    public String team;

    @NotNull
    @Property(name = "tier")
    public ServiceTierView tier;

    @NotNull
    @Property(name = "repositories")
    public Repositories repositories;

    @NotNull
    @Property(name = "review_config")
    public ReviewConfig reviewConfig;

    @NotNull
    @Property(name = "runtime")
    public Runtime runtime;

    @NotNull
    @Property(name = "enabled")
    public Boolean enabled = Boolean.TRUE;

    @NotNull
    @NotBlank
    @Property(name = "operator")
    public String operator;

    public static class RepoConfig {
        @NotNull
        @NotBlank
        @Property(name = "url")
        public String url;

        @NotNull
        @NotBlank
        @Property(name = "branch")
        public String branch = "main";

        @Property(name = "service_path")
        public String servicePath;

        @Property(name = "include_paths")
        public List<String> includePaths;

        @Property(name = "exclude_paths")
        public List<String> excludePaths;
    }

    public static class DeploymentConfig {
        @NotNull
        @NotBlank
        @Property(name = "name")
        public String name;

        @Property(name = "type")
        public String type;
    }

    public static class Repositories {
        @NotNull
        @Property(name = "app")
        public RepoConfig app;

        @Property(name = "infra")
        public RepoConfig infra;

        @Property(name = "k8s_gitops")
        public RepoConfig k8sGitops;
    }

    public static class ReviewConfig {
        @NotNull
        @NotBlank
        @Property(name = "schedule")
        public String schedule;

        @NotNull
        @Property(name = "ai_model")
        public AIModelView aiModel = AIModelView.GEMINI_2_5_PRO;

        @NotNull
        @Property(name = "metric_lookback_days")
        public Integer metricLookbackDays = 30;

        @Property(name = "confluence_space_key")
        public String confluenceSpaceKey;

        @Property(name = "confluence_parent_page_title")
        public String confluenceParentPageTitle;
    }

    public static class Runtime {
        @NotNull
        @Property(name = "environment")
        public EnvironmentView environment = EnvironmentView.UAT;

        @NotNull
        @NotBlank
        @Property(name = "namespace")
        public String namespace;

        @NotNull
        @Property(name = "deployments")
        public List<DeploymentConfig> deployments;

        @NotNull
        @Property(name = "hpa_type")
        public HPATypeView hpaType = HPATypeView.NONE;

        @Property(name = "kafka_consumer_groups")
        public List<String> kafkaConsumerGroups;

        @Property(name = "mysql_host")
        public String mysqlHost;

        @Property(name = "mysql_db")
        public String mysqlDB;

        @Property(name = "atlas_cluster")
        public String atlasCluster;

        @Property(name = "redis_enabled")
        public Boolean redisEnabled;

        @NotNull
        @Property(name = "domain")
        public String domain;
    }
}
