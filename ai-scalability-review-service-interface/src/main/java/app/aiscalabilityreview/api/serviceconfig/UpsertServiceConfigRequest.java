package app.aiscalabilityreview.api.serviceconfig;

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
    @NotBlank
    @Property(name = "tier")
    public String tier;

    @NotNull
    @Property(name = "repositories")
    public Repositories repositories;

    @NotNull
    @Property(name = "review")
    public ReviewConfig review;

    @NotNull
    @Property(name = "runtime")
    public Runtime runtime;

    @Property(name = "enabled")
    public Boolean enabled;

    public static class Repositories {
        @NotNull
        @Property(name = "app")
        public RepoConfig app;

        @Property(name = "infra")
        public RepoConfig infra;

        @Property(name = "k8s_gitops")
        public RepoConfig k8sGitops;

        public static class RepoConfig {
            @NotNull
            @NotBlank
            @Property(name = "url")
            public String url;

            @NotNull
            @NotBlank
            @Property(name = "branch")
            public String branch;

            @Property(name = "service_path")
            public String servicePath;

            @Property(name = "include_paths")
            public List<String> includePaths;

            @Property(name = "exclude_paths")
            public List<String> excludePaths;
        }
    }

    public static class ReviewConfig {
        @NotNull
        @NotBlank
        @Property(name = "schedule")
        public String schedule;

        @NotNull
        @NotBlank
        @Property(name = "ai_model")
        public String aiModel;

        @Property(name = "metric_lookback_days")
        public Integer metricLookbackDays;

        @Property(name = "confluence_space_key")
        public String confluenceSpaceKey;

        @Property(name = "confluence_parent_page_title")
        public String confluenceParentPageTitle;
    }

    public static class Runtime {
        @NotNull
        @NotBlank
        @Property(name = "environment")
        public String environment;

        @NotNull
        @NotBlank
        @Property(name = "namespace")
        public String namespace;

        @Property(name = "deployments")
        public List<DeploymentConfig> deployments;

        @Property(name = "hpa_type")
        public String hpaType;

        @Property(name = "kafka_consumer_groups")
        public List<String> kafkaConsumerGroups;

        @Property(name = "mysql_host")
        public String mysqlHost;

        @Property(name = "mysql_db")
        public String mysqlDb;

        @Property(name = "atlas_cluster")
        public String atlasCluster;

        @Property(name = "redis_enabled")
        public Boolean redisEnabled;

        @Property(name = "datadog_domain")
        public String datadogDomain;

        public static class DeploymentConfig {
            @NotNull
            @NotBlank
            @Property(name = "name")
            public String name;

            @NotNull
            @NotBlank
            @Property(name = "type")
            public String type;
        }
    }
}