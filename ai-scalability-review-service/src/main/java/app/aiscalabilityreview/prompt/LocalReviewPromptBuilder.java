package app.aiscalabilityreview.prompt;

import core.framework.util.Strings;

import java.util.Locale;

/**
 * Builds Gemini CLI prompts for the three local review stages.
 * Each prompt uses @file syntax so the Gemini CLI reads local files directly
 * and calls Datadog MCP, Azure MCP, and MySQL MCP tools as instructed.
 */
public class LocalReviewPromptBuilder {

    /**
     * Stage 0 — Code analysis prompt.
     */
    public static String buildCodeAnalysisPrompt(String serviceId, String localAppRepoPath) {
        return CodeAnalysisTaskPrompt.CONTENT.replace("{{SERVICE}}", serviceId)
            + "\n\n## Source Code\n\nThe following files are from the **"
            + serviceId + "** application repository. Analyze them per the instructions above.\n\n@"
            + localAppRepoPath + '\n';
    }

    /**
     * Stage 1 — Scalability review prompt with @file refs to local infra repo.
     */
    public static String buildReviewPrompt(ReviewPromptParams p) {
        ResolvedReviewParams r = new ResolvedReviewParams(p);
        StringBuilder sb = new StringBuilder(8192);
        appendReviewHeader(sb, r);
        appendInfraStep(sb, r);
        appendAzureStep(sb, r);
        appendDatadogStep(sb, r);
        appendMySQLStep(sb, r.mysqlHost, r.mysqlDb);
        appendScoringOutput(sb);
        return sb.toString();
    }

    /**
     * Stage 2 — Validation prompt referencing the review report file.
     */
    public static String buildValidationPrompt(ValidationPromptParams p) {
        String envLower = p.env.toLowerCase(Locale.US);
        return ValidateReportTaskPrompt.CONTENT
            .replace("{{REPORT_FILE}}", p.reviewReportPath)
            .replace("{{SERVICE}}", p.serviceId)
            .replace("{{ENV}}", envLower)
            .replace("{{NAMESPACE}}", p.namespace)
            .replace("{{MYSQL_HOST}}", nvl(p.mysqlHost, "N/A"))
            .replace("{{MYSQL_DB}}", nvl(p.mysqlDb, "N/A"))
            .replace("{{ATLAS_CLUSTER}}", nvl(p.atlasCluster, "N/A"))
            .replace("{{KAFKA_CONSUMER_GROUPS}}", nvl(p.kafkaConsumerGroups, "N/A"))
            + "\n\n## Report to Validate\n\n@" + p.reviewReportPath
            + "\n\n## Validation Format Reference\n\n" + ValidationFormatPrompt.CONTENT
            + "\n\nNow produce the complete validation report.\n";
    }

    private static void appendReviewHeader(StringBuilder sb, ResolvedReviewParams r) {
        sb.append("# Scalability Review Task\n\n"
                + "Perform a complete scalability review for:\n\n"
                + "- **Service**: ").append(r.serviceId)
            .append("\n- **Environment**: ").append(r.envLower)
            .append("\n- **Namespace**: ").append(r.namespace)
            .append("\n- **Lookback window**: 28 days\n- **MySQL host/cluster**: ").append(r.mysqlHost)
            .append("\n- **MySQL database**: ").append(r.mysqlDb)
            .append("\n- **Atlas MongoDB cluster**: ").append(r.atlasCluster)
            .append("\n- **HPA type**: ").append(r.hpaType)
            .append("\n- **Kafka consumer groups**: ").append(r.kafkaGroups)
            .append("\n\nAnnotate every value with its source: `[Source: code → <path>]`, "
                + "`[Source: Azure MCP → <tool>]`, `[Source: Datadog MCP → query_metrics(\"...\")]`, "
                + "`[Source: MySQL MCP → <SQL>]`.\n"
                + "If unavailable: `NOT_COLLECTED: <reason>`. No approximations.\n\n---\n\n");
    }

    private static void appendInfraStep(StringBuilder sb, ResolvedReviewParams r) {
        sb.append("### Step 1 — Load Kubernetes and Infrastructure Configuration\n\n**K8s manifests**:\n@")
            .append(r.localInfraRepoPath).append('/').append(r.envLower).append("/app/").append(r.domain)
            .append("/kube/resource/\n\n**AKS node pools (Terraform)**:\n@")
            .append(r.localInfraRepoPath).append('/').append(r.envLower).append("/app/infra/env/\n\n");
        if (!"N/A".equals(r.mysqlHost)) {
            sb.append("**MySQL Terraform**:\n@").append(r.localInfraRepoPath)
                .append('/').append(r.envLower).append("/app/infra/mysql/\n\n");
        }
        if (!"N/A".equals(r.atlasCluster)) {
            sb.append("**Atlas Terraform**:\n@").append(r.localInfraRepoPath)
                .append('/').append(r.envLower).append("/atlas/\n\n");
        }
        sb.append("**Business context**:\n@").append(r.codeContextPath).append("\n\n---\n\n");
    }

    private static void appendAzureStep(StringBuilder sb, ResolvedReviewParams r) {
        sb.append("### Step 3 — Query Azure MCP\n\n");
        if (!"N/A".equals(r.mysqlHost)) {
            sb.append("- `get_mysql_flexible_server(name=").append(r.mysqlHost).append(")`\n"
                    + "- `list_mysql_flexible_server_configurations(server_name=").append(r.mysqlHost)
                .append(")`\n- `list_mysql_flexible_server_replicas(server_name=").append(r.mysqlHost)
                .append(")`\n- `get_metric(resource=").append(r.mysqlHost)
                .append(", metric=storage_used)` (if auto-grow ON: collect only, skip utilisation %)\n"
                    + "- `get_metric(resource=").append(r.mysqlHost)
                .append(", metric=cpu_percent)`\n- `get_metric(resource=").append(r.mysqlHost)
                .append(", metric=active_connections)`\n\n");
        }
        sb.append("- `get_aks_node_pools` for namespace ").append(r.namespace)
            .append(". Cross-check against Terraform. Flag drift.\n\n---\n\n"
                + "### Step 4 — Collect Datadog Metrics (env=").append(r.envLower)
            .append(", namespace=").append(r.namespace).append(")\n\n");
    }

    private static void appendDatadogStep(StringBuilder sb, ResolvedReviewParams r) {
        appendDatadogCoreMetrics(sb, r);
        if (!"N/A".equals(r.mysqlHost)) {
            sb.append("**MySQL connections**: `avg/max:azure.dbformysql_flexibleservers.active_connections{name:")
                .append(r.mysqlHost).append("}`\n"
                    + "**MySQL slow queries**: `sum:azure.dbformysql_flexibleservers.slow_queries{name:")
                .append(r.mysqlHost).append("}.as_count()`\n"
                    + "**MySQL replication lag**: `max:azure.dbformysql_flexibleservers.replication_lag{name:")
                .append(r.mysqlHost).append("-*}`\n");
        }
        if (!"N/A".equals(r.atlasCluster)) {
            sb.append("**Atlas latency**: `avg:mongodb.atlas.oplatencies.reads.avg / writes.avg{clustername:")
                .append(r.atlasCluster).append("}`\n"
                    + "**Atlas connections**: `max:mongodb.atlas.connections.current{clustername:")
                .append(r.atlasCluster).append("}`\n"
                    + "**Atlas targeting**: `sum:mongodb.atlas.metrics.queryexecutor.scannedperreturned{clustername:")
                .append(r.atlasCluster).append("}`\n");
        }
        sb.append("**Monitors**: `list_monitors(service:").append(r.serviceId)
            .append(")`\n\nMark unavailable metrics `NOT_COLLECTED: <reason>`. Do not invent values.\n\n---\n\n");
    }

    private static void appendDatadogCoreMetrics(StringBuilder sb, ResolvedReviewParams r) {
        sb.append("**Traffic**: `sum:trace.undertow_http.request.hits{env:").append(r.envLower)
            .append(",service:").append(r.serviceId).append("}.as_count()`\n");
        if (!"N/A".equals(r.kafkaGroups)) {
            sb.append("**Kafka lag**: `max:kafka.consumer_lag{env:").append(r.envLower)
                .append(",consumer_group:").append(r.kafkaGroups).append("} by {topic}`\n");
        }
        sb.append("**Latency P50/P95/P99**: `p50/p95/p99:trace.undertow_http.request{env:").append(r.envLower)
            .append(",service:").append(r.serviceId).append("}`\n"
                + "**Error rate**: `sum:trace.undertow_http.request.errors{env:").append(r.envLower)
            .append(",service:").append(r.serviceId).append("}.as_count() / hits.as_count()`\n"
                + "**Restarts**: `sum:kubernetes.containers.restarts{env:").append(r.envLower)
            .append(",service:").append(r.serviceId).append("}`\n"
                + "**CPU avg/peak**: `avg/max:kubernetes.cpu.usage.total{env:").append(r.envLower)
            .append(",service:").append(r.serviceId).append("}`\n"
                + "**Memory avg/peak**: `avg/max:kubernetes.memory.working_set{env:").append(r.envLower)
            .append(",service:").append(r.serviceId).append("}`\n"
                + "**Replicas avg/max**: `avg/max:kubernetes_state.deployment.replicas{env:").append(r.envLower)
            .append(",kube_deployment:").append(r.serviceId).append("}`\n");
    }

    private static void appendMySQLStep(StringBuilder sb, String mysqlHost, String mysqlDb) {
        if ("N/A".equals(mysqlHost) || "N/A".equals(mysqlDb)) return;
        sb.append("### Step 5 — MySQL Table Sizes via MySQL MCP `").append(mysqlHost)
            .append('-').append(mysqlDb).append("`\n\n"
                + "```sql\nSELECT table_name, table_rows,\n"
                + "  ROUND(data_length/1024/1024,2) AS data_mb,\n"
                + "  ROUND(index_length/1024/1024,2) AS index_mb,\n"
                + "  ROUND((data_length+index_length)/1024/1024,2) AS total_mb\n"
                + "FROM information_schema.tables WHERE table_schema='").append(mysqlDb)
            .append("'\nORDER BY (data_length+index_length) DESC LIMIT 20;\n"
                + "```\n\nFlag tables with total_mb > 10240 as notable storage consumers.\n\n---\n\n");
    }

    private static void appendScoringOutput(StringBuilder sb) {
        sb.append("### Steps 6–9 — Cross-Reference, Score, Projections, Output\n\n"
                + "- Does HPA maxReplicas leave headroom at +1Q traffic?\n"
                + "  Compute `effective_trigger_pct_of_limit = averageUtilization x (cpu_request/cpu_limit)`\n"
                + "- Are CPU/memory limits within 20% of peak (throttling/OOM risk)?\n"
                + "- If MySQL connections approach max_connections (Terraform + Azure MCP), flag RED.\n"
                + "- If Atlas query targeting ratio > 100, flag YELLOW.\n"
                + "- Generate +1Q/+2Q/+4Q capacity projections from the 28-day growth rate.\n\n"
                + "## Metric Scoring Reference\n\n").append(MetricScorePrompt.CONTENT)
            .append("\n\n## Report Format Reference\n\n").append(ReportFormatPrompt.CONTENT)
            .append("\n\nNow produce the complete scalability review report.\n");
    }

    private static String nvl(String value, String defaultValue) {
        return Strings.isBlank(value) ? defaultValue : value;
    }

    public static class ReviewPromptParams {
        public String serviceId;
        public String localInfraRepoPath;
        public String codeContextPath;
        public String env;
        public String namespace;
        public String domain;
        public String mysqlHost;
        public String mysqlDb;
        public String atlasCluster;
        public String hpaType;
        public String kafkaConsumerGroups;
    }

    public static class ValidationPromptParams {
        public String serviceId;
        public String reviewReportPath;
        public String env;
        public String namespace;
        public String mysqlHost;
        public String mysqlDb;
        public String atlasCluster;
        public String kafkaConsumerGroups;
    }

    private static class ResolvedReviewParams {
        final String serviceId;
        final String envLower;
        final String namespace;
        final String domain;
        final String localInfraRepoPath;
        final String codeContextPath;
        final String mysqlHost;
        final String mysqlDb;
        final String atlasCluster;
        final String hpaType;
        final String kafkaGroups;

        ResolvedReviewParams(ReviewPromptParams p) {
            this.serviceId = p.serviceId;
            this.envLower = p.env.toLowerCase(Locale.US);
            this.namespace = p.namespace;
            this.domain = p.domain;
            this.localInfraRepoPath = p.localInfraRepoPath;
            this.codeContextPath = p.codeContextPath;
            this.mysqlHost = nvl(p.mysqlHost, "N/A");
            this.mysqlDb = nvl(p.mysqlDb, "N/A");
            this.atlasCluster = nvl(p.atlasCluster, "N/A");
            this.hpaType = nvl(p.hpaType, "none");
            this.kafkaGroups = nvl(p.kafkaConsumerGroups, "N/A");
        }
    }
}
