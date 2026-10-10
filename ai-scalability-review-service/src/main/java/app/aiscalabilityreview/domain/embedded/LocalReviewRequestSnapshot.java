package app.aiscalabilityreview.domain.embedded;

import core.framework.mongo.Field;

import java.util.List;

/**
 * Verbatim copy of the trigger request a local review run was started with.
 *
 * <p>Persisting it is what makes a run describable after the fact: the console shows what the run
 * actually looked at, a failed run can be repeated without re-typing the form, and a service restart
 * can resume or honestly fail a job instead of stranding it in {@code PENDING} — the request used to
 * live only in an in-memory map that was consumed when the job started.
 *
 * <p>Stored verbatim rather than redacted because re-run has to reproduce it exactly; the request
 * carries no credentials (the MySQL password and the Datadog keys come from the environment).
 */
public class LocalReviewRequestSnapshot {
    @Field(name = "service")
    public String service;

    @Field(name = "local_app_repo_path")
    public String localAppRepoPath;

    @Field(name = "local_infra_repo_path")
    public String localInfraRepoPath;

    @Field(name = "env")
    public String env;

    @Field(name = "namespace")
    public String namespace;

    @Field(name = "domain")
    public String domain;

    @Field(name = "mysql_host")
    public String mysqlHost;

    @Field(name = "mysql_db")
    public String mysqlDB;

    @Field(name = "atlas_cluster")
    public String atlasCluster;

    @Field(name = "azure_resource_group")
    public String azureResourceGroup;

    @Field(name = "azure_subscription_id")
    public String azureSubscriptionId;

    @Field(name = "hpa_type")
    public String hpaType;

    @Field(name = "kafka_consumer_groups")
    public String kafkaConsumerGroups;

    @Field(name = "skip_code_analysis")
    public Boolean skipCodeAnalysis;

    @Field(name = "skip_validation")
    public Boolean skipValidation;

    @Field(name = "app_code_paths")
    public List<String> appCodePaths;

    @Field(name = "code_analysis_model")
    public String codeAnalysisModel;

    @Field(name = "metric_collection_model")
    public String metricCollectionModel;

    @Field(name = "synthesis_model")
    public String synthesisModel;

    @Field(name = "validation_model")
    public String validationModel;

    @Field(name = "output_base_dir")
    public String outputBaseDir;

    @Field(name = "operator")
    public String operator;
}
