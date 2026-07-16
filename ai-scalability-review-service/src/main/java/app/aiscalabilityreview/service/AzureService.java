package app.aiscalabilityreview.service;

import core.framework.http.HTTPClient;
import core.framework.http.HTTPMethod;
import core.framework.http.HTTPRequest;
import core.framework.http.HTTPResponse;
import core.framework.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

/**
 * Queries Azure Resource Manager REST API for infrastructure configuration
 * and Azure Monitor metrics.
 * <p>
 * Authentication uses a pre-obtained access token from env var AZURE_ACCESS_TOKEN.
 */
public class AzureService {
    private static final String ARM_BASE = "https://management.azure.com";
    private static final String ARM_API_MYSQL = "2023-06-30";
    private static final String ARM_API_AKS = "2024-01-01";
    private static final String ARM_API_MONITOR = "2019-07-01";

    private final Logger logger = LoggerFactory.getLogger(AzureService.class);

    @Inject
    HTTPClient httpClient;

    /**
     * Get MySQL Flexible Server properties.
     *
     * @param subscriptionId Azure subscription ID
     * @param resourceGroup  resource group name
     * @param serverName     MySQL server name
     * @return JSON string of server properties, or NOT_COLLECTED message
     */
    public String getMySQLFlexibleServer(String subscriptionId, String resourceGroup, String serverName) {
        String url = ARM_BASE + "/subscriptions/" + subscriptionId
            + "/resourceGroups/" + resourceGroup
            + "/providers/Microsoft.DBforMySQL/flexibleServers/" + serverName
            + "?api-version=" + ARM_API_MYSQL;
        return get(url, "getMySQLFlexibleServer(" + serverName + ")");
    }

    /**
     * List MySQL Flexible Server configuration parameters.
     *
     * @param subscriptionId Azure subscription ID
     * @param resourceGroup  resource group name
     * @param serverName     MySQL server name
     * @return JSON string of configuration list, or NOT_COLLECTED message
     */
    public String listMySQLServerConfigurations(String subscriptionId, String resourceGroup, String serverName) {
        String url = ARM_BASE + "/subscriptions/" + subscriptionId
            + "/resourceGroups/" + resourceGroup
            + "/providers/Microsoft.DBforMySQL/flexibleServers/" + serverName
            + "/configurations?api-version=" + ARM_API_MYSQL;
        return get(url, "listMySQLServerConfigurations(" + serverName + ")");
    }

    /**
     * List MySQL Flexible Server read replicas.
     *
     * @param subscriptionId Azure subscription ID
     * @param resourceGroup  resource group name
     * @param serverName     MySQL server name
     * @return JSON string of replicas list, or NOT_COLLECTED message
     */
    public String listMySQLServerReplicas(String subscriptionId, String resourceGroup, String serverName) {
        String url = ARM_BASE + "/subscriptions/" + subscriptionId
            + "/resourceGroups/" + resourceGroup
            + "/providers/Microsoft.DBforMySQL/flexibleServers/" + serverName
            + "/replicas?api-version=" + ARM_API_MYSQL;
        return get(url, "listMySQLServerReplicas(" + serverName + ")");
    }

    /**
     * Get AKS cluster node pool configurations.
     *
     * @param subscriptionId Azure subscription ID
     * @param resourceGroup  resource group name
     * @param clusterName    AKS cluster name
     * @return JSON string of agent pool list, or NOT_COLLECTED message
     */
    public String getAKSNodePools(String subscriptionId, String resourceGroup, String clusterName) {
        String url = ARM_BASE + "/subscriptions/" + subscriptionId
            + "/resourceGroups/" + resourceGroup
            + "/providers/Microsoft.ContainerService/managedClusters/" + clusterName
            + "/agentPools?api-version=" + ARM_API_AKS;
        return get(url, "getAKSNodePools(cluster=" + clusterName + ")");
    }

    /**
     * Query an Azure Monitor metric for a resource.
     *
     * @return JSON string of metric values, or NOT_COLLECTED message
     */
    public String getAzureMonitorMetric(AzureMonitorMetricParam param) {
        String resourceId = "/subscriptions/" + param.subscriptionId
            + "/resourceGroups/" + param.resourceGroup
            + "/providers/" + param.resourceProvider + "/" + param.resourceName;
        String url = ARM_BASE + resourceId
            + "/providers/microsoft.insights/metrics"
            + "?api-version=" + ARM_API_MONITOR
            + "&metricnames=" + param.metric
            + "&aggregation=" + param.aggregation
            + "&starttime=" + param.fromIso
            + "&endtime=" + param.toIso;
        return get(url, "getAzureMonitorMetric(resource=" + param.resourceName + ", metric=" + param.metric + ")");
    }

    private String get(String url, String operationName) {
        String token = System.getenv("AZURE_ACCESS_TOKEN");
        if (token == null || token.isBlank()) {
            return "NOT_COLLECTED: AZURE_ACCESS_TOKEN environment variable is not set";
        }

        HTTPRequest request = new HTTPRequest(HTTPMethod.GET, url);
        request.headers.put("Authorization", "Bearer " + token);
        request.headers.put("Accept", "application/json");

        try {
            HTTPResponse response = httpClient.execute(request);
            if (response.statusCode == 404) {
                return "NOT_COLLECTED: Azure resource not found — " + operationName;
            }
            if (response.statusCode != 200) {
                String body = response.body == null ? "(empty)" : new String(response.body, StandardCharsets.UTF_8);
                logger.warn("Azure API error for {}: status={}, body={}", operationName, response.statusCode, body);
                return "NOT_COLLECTED: Azure MCP error — HTTP " + response.statusCode + " for " + operationName;
            }
            return new String(response.body, StandardCharsets.UTF_8);
        } catch (Exception e) {
            logger.warn("Azure API call failed for {}: {}", operationName, e.getMessage());
            return "NOT_COLLECTED: Azure MCP error — " + e.getMessage();
        }
    }

    /**
     * @param subscriptionId   Azure subscription ID
     * @param resourceGroup    resource group name
     * @param resourceProvider resource provider, e.g. "Microsoft.DBforMySQL/flexibleServers"
     * @param resourceName     resource name
     * @param metric           metric name, e.g. "storage_used"
     * @param aggregation      aggregation type: Average, Maximum, Minimum, Total
     * @param fromIso          ISO-8601 start time
     * @param toIso            ISO-8601 end time
     */
    public record AzureMonitorMetricParam(String subscriptionId,
                                          String resourceGroup,
                                          String resourceProvider,
                                          String resourceName,
                                          String metric,
                                          String aggregation,
                                          String fromIso,
                                          String toIso) {
    }
}