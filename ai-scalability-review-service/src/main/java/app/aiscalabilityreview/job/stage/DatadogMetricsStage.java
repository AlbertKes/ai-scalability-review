package app.aiscalabilityreview.job.stage;

import app.aiscalabilityreview.domain.ServiceConfig;
import app.aiscalabilityreview.service.AuditLogService;
import app.aiscalabilityreview.service.DatadogService;
import core.framework.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Stage 4: Query Datadog metrics for the service over a 28-day lookback window.
 * <p>
 * Produces context.datadogMetricsData with annotated query results.
 */
public class DatadogMetricsStage {
    private static final long SECONDS_28_DAYS = 28L * 24 * 3600;

    private final Logger logger = LoggerFactory.getLogger(DatadogMetricsStage.class);

    @Inject
    DatadogService datadogService;

    @Inject
    AuditLogService auditLogService;

    public void execute(ReviewContext context) throws Exception {
        ServiceConfig config = context.config;
        String env = config.environment;
        String svc = config.serviceId;
        String namespace = config.namespace;
        long now = Instant.now().getEpochSecond();
        long from = now - SECONDS_28_DAYS;
        StringBuilder metricsText = new StringBuilder();
        metricsText.append("## Datadog Metrics (28-day lookback)\n\n");
        List<String[]> queries = buildQueries(config, env, svc, namespace);
        for (String[] qDef : queries) {
            String label = qDef[0];
            String query = qDef[1];
            metricsText.append("### ").append(label).append("\n");
            metricsText.append("`[Source: Datadog MCP → query_metrics(\"").append(query).append("\")]`\n\n");
            queryMetrics(context, query, from, now, metricsText, svc);
        }

        try {
            String monitors = datadogService.listMonitors(svc);
            metricsText.append("### SLO & Monitors\n");
            metricsText.append("`[Source: Datadog MCP → list_monitors(service:").append(svc).append(")]`\n\n");
            metricsText.append(monitors).append("\n\n");
        } catch (Exception e) {
            metricsText.append("### SLO & Monitors\n");
            metricsText.append("NOT_COLLECTED: Datadog monitors query failed — ").append(e.getMessage()).append("\n\n");
        }

        context.datadogMetricsData = metricsText.toString();
        logger.info("Datadog metrics stage complete for service {}: {} chars", svc, metricsText.length());
    }

    private void queryMetrics(ReviewContext context, String query, long from, long now, StringBuilder metricsText, String svc) {
        long startMs = System.currentTimeMillis();
        boolean success = false;
        String resultSummary;

        try {
            DatadogService.MetricQueryResult result = datadogService.queryMetrics(query, from, now);
            long durationMs = System.currentTimeMillis() - startMs;
            success = true;

            resultSummary = formatResult(result);
            metricsText.append(resultSummary).append("\n\n");

            auditLogService.log(new AuditLogService.AuditLogParam(context.job.jobId, svc,
                "DATADOG_METRICS_QUERY", "DatadogMetricsStage", query,
                "Query: " + query, null,
                200, durationMs, resultSummary, null, true));

        } catch (Exception e) {
            long durationMs = System.currentTimeMillis() - startMs;
            resultSummary = "NOT_COLLECTED: Datadog query failed — " + e.getMessage();
            metricsText.append(resultSummary).append("\n\n");

            auditLogService.log(new AuditLogService.AuditLogParam(context.job.jobId, svc,
                "DATADOG_METRICS_QUERY", "DatadogMetricsStage", query,
                "Query: " + query, null,
                500, durationMs, null, e.getMessage(), false));

            logger.warn("Datadog query failed for {}: {}", query, e.getMessage());
        }
    }

    private List<String[]> buildQueries(ServiceConfig config, String env, String svc, String ns) {
        List<String[]> queries = new ArrayList<>();
        // Traffic
        queries.add(new String[]{"Request Rate (RPS)",
            "sum:trace.undertow_http.request.hits{env:" + env + ",service:" + svc + "}.as_count()"});
        // Latency
        queries.add(new String[]{"P50 Latency",
            "p50:trace.undertow_http.request{env:" + env + ",service:" + svc + "}"});
        queries.add(new String[]{"P95 Latency",
            "p95:trace.undertow_http.request{env:" + env + ",service:" + svc + "}"});
        queries.add(new String[]{"P99 Latency",
            "p99:trace.undertow_http.request{env:" + env + ",service:" + svc + "}"});
        // Errors
        queries.add(new String[]{"HTTP Error Rate",
            "sum:trace.undertow_http.request.errors{env:" + env + ",service:" + svc + "}.as_count() / "
                + "sum:trace.undertow_http.request.hits{env:" + env + ",service:" + svc + "}.as_count()"});
        queries.add(new String[]{"Container Restarts",
            "sum:kubernetes.containers.restarts{env:" + env + ",service:" + svc + "}"});
        // Resource saturation
        queries.add(new String[]{"CPU Avg",
            "avg:kubernetes.cpu.usage.total{env:" + env + ",service:" + svc + "}"});
        queries.add(new String[]{"CPU Max",
            "max:kubernetes.cpu.usage.total{env:" + env + ",service:" + svc + "}"});
        queries.add(new String[]{"Memory Avg",
            "avg:kubernetes.memory.working_set{env:" + env + ",service:" + svc + "}"});
        queries.add(new String[]{"Memory Max",
            "max:kubernetes.memory.working_set{env:" + env + ",service:" + svc + "}"});
        queries.add(new String[]{"Replica Count Avg",
            "avg:kubernetes_state.deployment.replicas{env:" + env + ",kube_deployment:" + svc + "}"});
        queries.add(new String[]{"Replica Count Max",
            "max:kubernetes_state.deployment.replicas{env:" + env + ",kube_deployment:" + svc + "}"});
        // Kafka
        if (config.kafkaConsumerGroups != null && !config.kafkaConsumerGroups.isEmpty()) {
            String groups = String.join(",", config.kafkaConsumerGroups);
            queries.add(new String[]{"Kafka Consumer Lag",
                "max:kafka.consumer_lag{env:" + env + ",consumer_group:" + groups + "} by {topic}"});
        }
        buildMySQLQueries(config, queries);
        buildAtlasQueries(config, queries);
        return queries;
    }

    private void buildMySQLQueries(ServiceConfig config, List<String[]> queries) {
        // MySQL
        if (config.mysqlHost != null && !config.mysqlHost.isBlank()) {
            queries.add(new String[]{"MySQL Active Connections Avg",
                "avg:azure.dbformysql_flexibleservers.active_connections{name:" + config.mysqlHost + "}"});
            queries.add(new String[]{"MySQL Active Connections Max",
                "max:azure.dbformysql_flexibleservers.active_connections{name:" + config.mysqlHost + "}"});
            queries.add(new String[]{"MySQL Slow Queries",
                "sum:azure.dbformysql_flexibleservers.slow_queries{name:" + config.mysqlHost + "}.as_count()"});
            queries.add(new String[]{"MySQL Replication Lag",
                "max:azure.dbformysql_flexibleservers.replication_lag{name:" + config.mysqlHost + "-*}"});
        }
    }

    private void buildAtlasQueries(ServiceConfig config, List<String[]> queries) {
        // MongoDB Atlas
        if (config.atlasCluster != null && !config.atlasCluster.isBlank()) {
            String cluster = config.atlasCluster;
            queries.add(new String[]{"MongoDB Read Latency",
                "avg:mongodb.atlas.oplatencies.reads.avg{clustername:" + cluster + "}"});
            queries.add(new String[]{"MongoDB Write Latency",
                "avg:mongodb.atlas.oplatencies.writes.avg{clustername:" + cluster + "}"});
            queries.add(new String[]{"MongoDB Connections",
                "max:mongodb.atlas.connections.current{clustername:" + cluster + "}"});
            queries.add(new String[]{"MongoDB Query Targeting Ratio",
                "sum:mongodb.atlas.metrics.queryexecutor.scannedperreturned{clustername:" + cluster + "}"});
        }
    }

    private String formatResult(DatadogService.MetricQueryResult result) {
        if (result.pointList == null || result.pointList.isEmpty()) {
            return "NOT_COLLECTED: no data points returned";
        }
        return String.format("- Count: %d data points | Avg: %.4f | Max: %.4f | Min: %.4f | Sum: %.4f",
            result.count, result.avg, result.max, result.min, result.sum);
    }
}