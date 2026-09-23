package app.aiscalabilityreview.api.localreview;

import core.framework.api.json.Property;
import core.framework.api.validate.NotBlank;
import core.framework.api.validate.NotNull;

import java.util.List;

public class GenerateLocalReviewRequest {
    @NotNull
    @NotBlank
    @Property(name = "service")
    public String service;

    @NotNull
    @NotBlank
    @Property(name = "local_app_repo_path")
    public String localAppRepoPath;  // absolute path to local app repo

    @NotNull
    @NotBlank
    @Property(name = "local_infra_repo_path")
    public String localInfraRepoPath;  // absolute path to local infra repo

    @NotNull
    @NotBlank
    @Property(name = "env")
    public String env;  // e.g. "prod", "uat" — used to locate K8s manifests and Terraform

    @NotNull
    @NotBlank
    @Property(name = "namespace")
    public String namespace;  // Kubernetes namespace

    @NotNull
    @NotBlank
    @Property(name = "domain")
    public String domain;  // service domain used in infra repo path, e.g. "consumer"

    @NotBlank
    @Property(name = "mysql_host")
    public String mysqlHost;  // Azure MySQL Flexible Server name, or null if not used

    @NotBlank
    @Property(name = "mysql_db")
    public String mysqlDB;  // MySQL database name, required when mysqlHost is set

    @NotBlank
    @Property(name = "atlas_cluster")
    public String atlasCluster;  // MongoDB Atlas cluster name, or null if not used

    @NotNull
    @NotBlank
    @Property(name = "hpa_type")
    public String hpaType = "none";  // "HPA" or "none" — defaults to "none"

    @NotBlank
    @Property(name = "kafka_consumer_groups")
    public String kafkaConsumerGroups;  // comma-separated group IDs, or null

    @NotNull
    @Property(name = "skip_code_analysis")
    public Boolean skipCodeAnalysis = Boolean.FALSE;  // skip Stage 0 code analysis

    @NotNull
    @Property(name = "skip_validation")
    public Boolean skipValidation = Boolean.FALSE;  // skip the final validation stage (halves the MCP round-trips of a run)

    @Property(name = "app_code_paths")
    public List<String> appCodePaths;  // repo-relative sub paths to analyze, e.g. ["backend/wonder-cart-service"]; whole repo when null

    // ---- per stage model overrides, see GeminiModels for the defaults (AD-6383) ----

    @Property(name = "code_analysis_model")
    public String codeAnalysisModel;

    @Property(name = "metric_collection_model")
    public String metricCollectionModel;

    @Property(name = "synthesis_model")
    public String synthesisModel;

    @Property(name = "validation_model")
    public String validationModel;

    @Property(name = "output_base_dir")
    public String outputBaseDir;  // base directory for output files; defaults to ./reports relative to working dir

    @NotNull
    @NotBlank
    @Property(name = "operator")
    public String operator;
}
