package app.aiscalabilityreview.domain;

import core.framework.mongo.Collection;
import core.framework.mongo.Field;
import core.framework.mongo.Id;

import java.time.ZonedDateTime;
import java.util.List;

@Collection(name = "service_configs")
public class ServiceConfig {
    @Id
    public String serviceId;

    @Field(name = "display_name")
    public String displayName;

    @Field(name = "team")
    public String team;

    @Field(name = "tier")
    public String tier;

    // App repository
    @Field(name = "repo_app_url")
    public String repoAppUrl;

    @Field(name = "repo_app_branch")
    public String repoAppBranch;

    @Field(name = "repo_app_service_path")
    public String repoAppServicePath;

    @Field(name = "repo_app_include_paths")
    public List<String> repoAppIncludePaths;

    @Field(name = "repo_app_exclude_paths")
    public List<String> repoAppExcludePaths;

    // Infra repository
    @Field(name = "repo_infra_url")
    public String repoInfraUrl;

    @Field(name = "repo_infra_branch")
    public String repoInfraBranch;

    @Field(name = "repo_infra_service_path")
    public String repoInfraServicePath;

    // k8s-gitops repository
    @Field(name = "repo_k8s_gitops_url")
    public String repoK8sGitopsUrl;

    @Field(name = "repo_k8s_gitos_branch")
    public String repoK8sGitosBranch;

    @Field(name = "repo_k8s_gitops_service_path")
    public String repoK8sGitopsServicePath;

    // Review config
    @Field(name = "review_schedule")
    public String reviewSchedule;

    @Field(name = "ai_model")
    public String aiModel;

    @Field(name = "metric_lookback_days")
    public Integer metricLookbackDays;

    @Field(name = "confluence_space_key")
    public String confluenceSpaceKey;

    @Field(name = "confluence_parent_page_title")
    public String confluenceParentPageTitle;

    @Field(name = "confluence_parent_page_id")
    public String confluenceParentPageId;

    // Runtime config
    @Field(name = "environment")
    public String environment;

    @Field(name = "namespace")
    public String namespace;

    @Field(name = "deployments")
    public List<DeploymentConfig> deployments;

    @Field(name = "hpa_type")
    public String hpaType;

    @Field(name = "kafka_consumer_groups")
    public List<String> kafkaConsumerGroups;

    @Field(name = "mysql_host")
    public String mysqlHost;

    @Field(name = "mysql_db")
    public String mysqlDb;

    @Field(name = "atlas_cluster")
    public String atlasCluster;

    @Field(name = "redis_enabled")
    public Boolean redisEnabled;

    @Field(name = "datadog_domain")
    public String datadogDomain;

    @Field(name = "enabled")
    public Boolean enabled;

    @Field(name = "created_at")
    public ZonedDateTime createdAt;

    @Field(name = "updated_at")
    public ZonedDateTime updatedAt;

    @Field(name = "created_by")
    public String createdBy;

    public static class DeploymentConfig {
        @Field(name = "name")
        public String name;

        @Field(name = "type")
        public String type;
    }
}