# Collected Metrics — wonder-cart-service (uat)
**Collected At**: 2026-09-23 07:15 UTC
**Lookback**: 28 days

## 1. Traffic & Throughput
- Average RPS: 5.75 [Source: Datadog MCP → query_metrics("sum:trace.undertow_http.request.hits{env:uat,service:wonder-cart-service}.as_count()")]
- Peak RPS: 5.86 at 2026-09-21 16:00 [Source: Datadog MCP → query_metrics("sum:trace.undertow_http.request.hits{env:uat,service:wonder-cart-service}.as_count()")]
- Last 4 complete weeks: NOT_COLLECTED: Complex data derivation required beyond current capabilities.

## 2. Latency Profile
- P50 Average: 0.00028s [Source: Datadog MCP → query_metrics("p50:trace.undertow_http.request{env:uat,service:wonder-cart-service}")]
- P95 Average: 0.00062s [Source: Datadog MCP → query_metrics("p95:trace.undertow_http.request{env:uat,service:wonder-cart-service}")]
- P99 Average: 0.00086s [Source: Datadog MCP → query_metrics("p99:trace.undertow_http.request{env:uat,service:wonder-cart-service}")]
- Peak P99: 0.0032s [Source: Datadog MCP → query_metrics("p99:trace.undertow_http.request{env:uat,service:wonder-cart-service}")]

## 3. Error Rates & Stability
- HTTP error rate (28-day avg): 0.0 [Source: Datadog MCP → query_metrics("sum:trace.undertow_http.request.errors{env:uat,service:wonder-cart-service}.as_count()")]
- Total container restarts: 0 [Source: Datadog MCP → query_metrics("sum:kubernetes.containers.restarts{env:uat,service:wonder-cart-service}")]

## 4. Resource Saturation
- CPU (avg): 12.3 nanocores (12.3e-9 cores) [Source: Datadog MCP → query_metrics("avg:kubernetes.cpu.usage.total{env:uat,service:wonder-cart-service}")]
- CPU (peak): 42.9 nanocores (42.9e-9 cores) [Source: Datadog MCP → query_metrics("max:kubernetes.cpu.usage.total{env:uat,service:wonder-cart-service}")]
- Memory (avg): 505.7 MiB (530260480 bytes) [Source: Datadog MCP → query_metrics("avg:kubernetes.memory.working_set{env:uat,service:wonder-cart-service}")]
- Memory (peak): 534.1 MiB (560000000 bytes) [Source: Datadog MCP → query_metrics("max:kubernetes.memory.working_set{env:uat,service:wonder-cart-service}")]
- Replicas (avg): 2 [Source: Datadog MCP → query_metrics("avg:kubernetes_state.deployment.replicas{env:uat,kube_deployment:wonder-cart-service}")]
- Replicas (max): 2 [Source: Datadog MCP → query_metrics("max:kubernetes_state.deployment.replicas{env:uat,kube_deployment:wonder-cart-service}")]

## 5. Persistence Layer
- MySQL Active Connections (avg): 705.5 [Source: Datadog MCP → query_metrics("avg:azure.dbformysql_flexibleservers.active_connections{name:ftiuat-flexible-consumer-db}")]
- MySQL Active Connections (peak): 994.2 [Source: Datadog MCP → query_metrics("max:azure.dbformysql_flexibleservers.active_connections{name:ftiuat-flexible-consumer-db}")]
- Slow queries daily average: 3.5 [Source: Datadog MCP → query_metrics("sum:azure.dbformysql_flexibleservers.slow_queries{name:ftiuat-flexible-consumer-db}.as_count()")]
- MySQL Replication Lag (avg): 0.32s [Source: Datadog MCP → query_metrics("max:azure.dbformysql_flexibleservers.replication_lag{name:ftiuat-flexible-consumer-db-*}")]

## 6. Kafka Consumer Lag
- Topic: corporate-order-client-changed - Avg Lag: 0 messages, Peak Lag: 0.004 messages [Source: Datadog MCP → query_metrics("max:kafka.consumer_lag{env:uat,consumer_group:wonder-cart-service} by {topic}")]
- Topic: courier-supply-health-trigger-message - Avg Lag: 0.04 messages, Peak Lag: 6.47 messages [Source: Datadog MCP → query_metrics("max:kafka.consumer_lag{env:uat,consumer_group:wonder-cart-service} by {topic}")]
- Topic: courier-supply-health-trigger-periodically-message - Avg Lag: 0.01 messages, Peak Lag: 0.22 messages [Source: Datadog MCP → query_metrics("max:kafka.consumer_lag{env:uat,consumer_group:wonder-cart-service} by {topic}")]
- Topic: wonder-configuration-updated - Avg Lag: 0 messages, Peak Lag: 0.002 messages [Source: Datadog MCP → query_metrics("max:kafka.consumer_lag{env:uat,consumer_group:wonder-cart-service} by {topic}")]
- Topic: consumer-clean-data - Avg Lag: 0 messages, Peak Lag: 0 messages [Source: Datadog MCP → query_metrics("max:kafka.consumer_lag{env:uat,consumer_group:wonder-cart-service} by {topic}")]

## 7. SLO & Monitors
None in ALERT or WARN

## 8. Infrastructure Live State (Azure MCP)
NOT_COLLECTED: Azure MCP error — Authentication failed due to expired/invalid credentials.

## 9. MySQL Table Storage (Top 20)
NOT_COLLECTED: MySQL MCP error — Invalid response format from query tool.

## 10. Collection Gaps
- Last 4 complete weeks: Complex data derivation required beyond current capabilities.
- Infrastructure Live State (Azure MCP): Azure MCP error — Authentication failed due to expired/invalid credentials.
- MySQL Table Storage (Top 20): MySQL MCP error — Invalid response format from query tool.

## 11. Metrics Snapshot (JSON)
```json
{
  "avg_rps": 5.75,
  "peak_rps": 5.86,
  "wow_rps_growth_pct": null,
  "p50_latency_ms": 0.28,
  "p95_latency_ms": 0.62,
  "p99_latency_ms": 0.86,
  "p99_latency_peak_ms": 3.2,
  "error_rate_pct": 0.0,
  "container_restarts": 0,
  "cpu_avg_cores": 0.0000000123,
  "cpu_peak_cores": 0.0000000429,
  "mem_avg_mib": 505.7,
  "mem_peak_mib": 534.1,
  "replicas_avg": 2,
  "replicas_max": 2,
  "kafka_lag_avg": null,
  "kafka_lag_peak": null,
  "mysql_connection_avg": 705.5,
  "mysql_connection_peak": 994.2,
  "mysql_max_connections": null,
  "mysql_slow_queries_per_day": 3.5,
  "mysql_replication_lag_avg_s": 0.32,
  "mysql_storage_used_gb": null,
  "mongo_read_latency_ms": null,
  "mongo_write_latency_ms": null,
  "largest_table_gb": null
}
```
