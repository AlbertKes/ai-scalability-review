package app.aiscalabilityreview.api.localreview;

import core.framework.api.json.Property;

import java.util.List;

/**
 * The trigger parameters a run was started with, as stored.
 *
 * <p>Deliberately separate from {@link GenerateLocalReviewRequest}: that class carries the
 * {@code @NotNull} contract of an incoming request, and a historical run — or a half-filled preset —
 * must still be returnable without tripping response validation.
 */
public class LocalReviewRequestView {
    @Property(name = "service")
    public String service;

    @Property(name = "local_app_repo_path")
    public String localAppRepoPath;

    @Property(name = "local_infra_repo_path")
    public String localInfraRepoPath;

    @Property(name = "env")
    public String env;

    @Property(name = "namespace")
    public String namespace;

    @Property(name = "domain")
    public String domain;

    @Property(name = "mysql_host")
    public String mysqlHost;

    @Property(name = "mysql_db")
    public String mysqlDB;

    @Property(name = "atlas_cluster")
    public String atlasCluster;

    @Property(name = "azure_resource_group")
    public String azureResourceGroup;

    @Property(name = "azure_subscription_id")
    public String azureSubscriptionId;

    @Property(name = "hpa_type")
    public String hpaType;

    @Property(name = "kafka_consumer_groups")
    public String kafkaConsumerGroups;

    @Property(name = "skip_code_analysis")
    public Boolean skipCodeAnalysis;

    @Property(name = "skip_validation")
    public Boolean skipValidation;

    @Property(name = "app_code_paths")
    public List<String> appCodePaths;

    @Property(name = "code_analysis_model")
    public String codeAnalysisModel;

    @Property(name = "metric_collection_model")
    public String metricCollectionModel;

    @Property(name = "synthesis_model")
    public String synthesisModel;

    @Property(name = "validation_model")
    public String validationModel;

    @Property(name = "output_base_dir")
    public String outputBaseDir;

    @Property(name = "operator")
    public String operator;
}
