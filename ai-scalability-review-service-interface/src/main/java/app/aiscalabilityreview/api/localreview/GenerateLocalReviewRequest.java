package app.aiscalabilityreview.api.localreview;

import core.framework.api.json.Property;
import core.framework.api.validate.NotBlank;
import core.framework.api.validate.NotNull;

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

    @Property(name = "output_base_dir")
    public String outputBaseDir;  // base directory for output files; defaults to ./reports relative to working dir

    @NotNull
    @NotBlank
    @Property(name = "operator")
    public String operator;
}
