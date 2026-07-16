package app.aiscalabilityreview.job.stage;

import app.aiscalabilityreview.domain.ServiceConfig;
import app.aiscalabilityreview.service.AuditLogService;
import app.aiscalabilityreview.service.AzureService;
import core.framework.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Stage 3: Fetch live Azure resource configuration and Azure Monitor metrics.
 *
 * Runs only when at least one Azure resource is configured (MySQL host or AKS).
 * Appends results with [Source: Azure MCP → ...] annotations to context.infraSnapshot.
 */
public class AzureMCPStage {
    private final Logger logger = LoggerFactory.getLogger(AzureMCPStage.class);

    @Inject
    AzureService azureService;

    @Inject
    AuditLogService auditLogService;

    public void execute(ReviewContext context) throws Exception {
        ServiceConfig config = context.config;
        String subscriptionId = System.getenv("AZURE_SUBSCRIPTION_ID");
        String resourceGroup = System.getenv("AZURE_RESOURCE_GROUP");
        if (subscriptionId == null || resourceGroup == null) {
            logger.warn("AZURE_SUBSCRIPTION_ID or AZURE_RESOURCE_GROUP not set; skipping Azure stage");
            appendToSnapshot(context, "## Azure MCP: NOT_COLLECTED (AZURE_SUBSCRIPTION_ID or AZURE_RESOURCE_GROUP not configured)\n\n");
            return;
        }
        StringBuilder azureData = new StringBuilder(150);
        azureData.append("\n\n## Azure Resource Configuration\n\n");
        appendAzureData(context, config, subscriptionId, resourceGroup, azureData);
        String aksClusterName = System.getenv("AKS_CLUSTER_NAME");
        if (aksClusterName != null && !aksClusterName.isBlank()) {
            logger.info("Fetching AKS node pools for cluster {}", aksClusterName);
            long startMs = System.currentTimeMillis();
            String nodePools = azureService.getAKSNodePools(subscriptionId, resourceGroup, aksClusterName);
            logAudit(context, "AZURE_AKS_NODE_POOLS", aksClusterName, startMs, nodePools);
            azureData.append("### [Source: Azure MCP → get_aks_node_pools(cluster=").append(aksClusterName)
                    .append(", resource_group=").append(resourceGroup).append(")]\n").append(nodePools).append("\n\n");
        }
        context.azureConfigData = azureData.toString();
        appendToSnapshot(context, context.azureConfigData);
        logger.info("Azure stage complete for service {}", config.serviceId);
    }

    private void appendAzureData(ReviewContext context, ServiceConfig config, String subscriptionId, String resourceGroup, StringBuilder azureData) {
        if (config.mysqlHost != null && !config.mysqlHost.isBlank()) {
            logger.info("Fetching MySQL Flexible Server config for {}", config.mysqlHost);

            long startMs = System.currentTimeMillis();
            String serverProps = azureService.getMySQLFlexibleServer(subscriptionId, resourceGroup, config.mysqlHost);
            logAudit(context, "AZURE_MYSQL_SERVER_PROPS", config.mysqlHost, startMs, serverProps);
            azureData.append("### [Source: Azure MCP → get_mysql_flexible_server(name=").append(config.mysqlHost)
                .append(")]\n").append(serverProps).append("\n\n");

            startMs = System.currentTimeMillis();
            String serverConf = azureService.listMySQLServerConfigurations(subscriptionId, resourceGroup, config.mysqlHost);
            logAudit(context, "AZURE_MYSQL_SERVER_CONF", config.mysqlHost, startMs, serverConf);
            azureData.append("### [Source: Azure MCP → list_mysql_flexible_server_configurations(server_name=")
                .append(config.mysqlHost).append(")]\n").append(serverConf).append("\n\n");

            startMs = System.currentTimeMillis();
            String replicas = azureService.listMySQLServerReplicas(subscriptionId, resourceGroup, config.mysqlHost);
            logAudit(context, "AZURE_MYSQL_REPLICAS", config.mysqlHost, startMs, replicas);
            azureData.append("### [Source: Azure MCP → list_mysql_flexible_server_replicas(server_name=")
                .append(config.mysqlHost).append(")]\n").append(replicas).append("\n\n");

            String toIso = ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            String fromIso = ZonedDateTime.now().minusDays(28).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

            for (String[] metricDef : new String[][]{
                    {"storage_used", "Average"},
                    {"cpu_percent", "Average"},
                    {"active_connections", "Average"},
                    {"network_bytes_ingress", "Total"},
                    {"network_bytes_egress", "Total"}
            }) {
                startMs = System.currentTimeMillis();
                String metricData = azureService.getAzureMonitorMetric(new AzureService.AzureMonitorMetricParam(
                    subscriptionId, resourceGroup,
                        "Microsoft.DBforMySQL/flexibleServers", config.mysqlHost,
                        metricDef[0], metricDef[1], fromIso, toIso));
                logAudit(context, "AZURE_MONITOR_METRIC_" + metricDef[0].toUpperCase(), config.mysqlHost, startMs, metricData);
                azureData.append("### [Source: Azure MCP → get_metric(resource=").append(config.mysqlHost)
                        .append(", metric=").append(metricDef[0])
                        .append(", aggregation=").append(metricDef[1]).append(")]\n");
                azureData.append(metricData).append("\n\n");
            }
        }
    }

    private void appendToSnapshot(ReviewContext context, String text) {
        if (context.infraSnapshot == null) {
            context.infraSnapshot = text;
        } else {
            context.infraSnapshot = context.infraSnapshot + text;
        }
    }

    private void logAudit(ReviewContext context, String eventType, String target, long startMs, String result) {
        long durationMs = System.currentTimeMillis() - startMs;
        boolean success = result != null && !result.startsWith("NOT_COLLECTED");
        auditLogService.log(new AuditLogService.AuditLogParam(context.job.jobId, context.config.serviceId,
                eventType, "AzureMcpStage", target,
                "Azure API call", null,
                success ? 200 : 500, durationMs,
                success ? "OK (" + result.length() + " chars)" : result,
                success ? null : result, success));
    }
}