# Collected Metrics — wonder-cart-service (prod)
**Collected At**: 2026-10-14 14:00 UTC
**Lookback**: 28 days (2026-09-16 to 2026-10-14, UTC day-aligned)

## 1. Traffic & Throughput
- Average RPS: 0.4625 [Source: Datadog MCP → query_metrics("sum:trace.undertow_http.request.hits{env:prod,service:wonder-cart-service}.as_count().rollup(sum, 86400)")]
- Peak RPS: 0.5969 [Source: Datadog MCP → query_metrics("sum:trace.undertow_http.request.hits{env:prod,service:wonder-cart-service}.as_count().rollup(sum, 3600)")] (at 2026-10-06 22:00 UTC, max hourly hits: 2149)
- Week-over-week average RPS growth: +0.81% [Source: Datadog MCP → query_metrics("sum:trace.undertow_http.request.hits{env:prod,service:wonder-cart-service}.as_count().rollup(sum, 86400)")]
- Weekly Volumes:
  - 2026-09-16 to 2026-09-22: 275,834 [Source: Datadog MCP → query_metrics("sum:trace.undertow_http.request.hits{env:prod,service:wonder-cart-service}.as_count().rollup(sum, 86400)")]
  - 2026-09-23 to 2026-09-29: 278,410 [Source: Datadog MCP → query_metrics("sum:trace.undertow_http.request.hits{env:prod,service:wonder-cart-service}.as_count().rollup(sum, 86400)")]
  - 2026-09-30 to 2026-10-06: 281,193 [Source: Datadog MCP → query_metrics("sum:trace.undertow_http.request.hits{env:prod,service:wonder-cart-service}.as_count().rollup(sum, 86400)")]
  - 2026-10-07 to 2026-10-13: 283,483 [Source: Datadog MCP → query_metrics("sum:trace.undertow_http.request.hits{env:prod,service:wonder-cart-service}.as_count().rollup(sum, 86400)")]

## 2. Latency Profile
- P50 latency average: 0.2381 ms [Source: Datadog MCP → query_metrics("p50:trace.undertow_http.request{env:prod,service:wonder-cart-service}.rollup(avg, 86400)")]
- P95 latency average: 1.4197 ms [Source: Datadog MCP → query_metrics("p95:trace.undertow_http.request{env:prod,service:wonder-cart-service}.rollup(avg, 86400)")]
- P99 latency average: 1.8635 ms [Source: Datadog MCP → query_metrics("p99:trace.undertow_http.request{env:prod,service:wonder-cart-service}.rollup(avg, 86400)")]
- Peak P99 latency: 2.7200 ms (at 2026-10-11 00:00 UTC) [Source: Datadog MCP → query_metrics("p99:trace.undertow_http.request{env:prod,service:wonder-cart-service}.rollup(max, 86400)")]

## 3. Error Rates & Stability
- HTTP error rate, 28-day average: 0.0% [Source: Datadog MCP → query_metrics("sum:trace.undertow_http.request.errors{env:prod,service:wonder-cart-service}.as_count().rollup(sum, 86400)")]
- HTTP error rate, worst single day: 0.0% (at 2026-09-16 00:00 UTC) [Source: Datadog MCP → query_metrics("sum:trace.undertow_http.request.errors{env:prod,service:wonder-cart-service}.as_count().rollup(sum, 86400)")]
- Container restarts: 0 [Source: Datadog MCP → query_metrics("sum:kubernetes.containers.restarts{env:prod,service:wonder-cart-service}.rollup(sum, 86400)")] (No OOMKill or restarts recorded; metric indicates 0 restarts across all days)

## 4. Resource Saturation
- CPU usage average: 0.009335 cores (raw: 9,335,014.2 nanocores) [Source: Datadog MCP → query_metrics("avg:kubernetes.cpu.usage.total{env:prod,service:wonder-cart-service}.rollup(avg, 86400)")]
- CPU usage peak: 0.250005 cores (raw: 250,005,029.2 nanocores, at 2026-09-22 00:00 UTC) [Source: Datadog MCP → query_metrics("max:kubernetes.cpu.usage.total{env:prod,service:wonder-cart-service}.rollup(max, 86400)")]
- Memory working set average: 388.95 MiB (raw: 407,819,307 bytes) [Source: Datadog MCP → query_metrics("avg:kubernetes.memory.working_set{env:prod,service:wonder-cart-service}.rollup(avg, 86400)")]
- Memory working set peak: 419.19 MiB (raw: 439,554,048 bytes, at 2026-09-21 00:00 UTC) [Source: Datadog MCP → query_metrics("max:kubernetes.memory.working_set{env:prod,service:wonder-cart-service}.rollup(max, 86400)")]
- Replicas average: 2.0 [Source: Datadog MCP → query_metrics("avg:kubernetes_state.deployment.replicas{env:prod,kube_deployment:wonder-cart-service}.rollup(avg, 86400)")]
- Replicas max: 3.0 [Source: Datadog MCP → query_metrics("max:kubernetes_state.deployment.replicas{env:prod,kube_deployment:wonder-cart-service}.rollup(max, 86400)")]

## 5. Persistence Layer
- Active connections average: 928.37 [Source: Datadog MCP → query_metrics("avg:azure.dbformysql_flexibleservers.active_connections{name:rfprodv2-flexible-wonder-db}.rollup(avg, 86400)")]
- Active connections peak: 1169 (at 2026-10-11 00:00 UTC) [Source: Datadog MCP → query_metrics("max:azure.dbformysql_flexibleservers.active_connections{name:rfprodv2-flexible-wonder-db}.rollup(max, 86400)")]
- Slow queries daily average: 695.79 [Source: Datadog MCP → query_metrics("sum:azure.dbformysql_flexibleservers.slow_queries{name:rfprodv2-flexible-wonder-db}.as_count().rollup(sum, 86400)")]
- Slow queries worst single day: 880 (at 2026-10-13 00:00 UTC) [Source: Datadog MCP → query_metrics("sum:azure.dbformysql_flexibleservers.slow_queries{name:rfprodv2-flexible-wonder-db}.as_count().rollup(sum, 86400)")]
- Replication lag average: 340.50 seconds [Source: Datadog MCP → query_metrics("max:azure.dbformysql_flexibleservers.replication_lag{name:rfprodv2-flexible-wonder-db-*}.rollup(max, 86400)")]
- Replication lag peak: 397 seconds (at 2026-09-21 00:00 UTC) [Source: Datadog MCP → query_metrics("max:azure.dbformysql_flexibleservers.replication_lag{name:rfprodv2-flexible-wonder-db-*}.rollup(max, 86400)")]
- Storage used (last bucket): 1583.59 GB (raw: 1,700,365,949,199 bytes, at 2026-10-13 00:00 UTC) [Source: Datadog MCP → query_metrics("max:azure.dbformysql_flexibleservers.storage_used{name:rfprodv2-flexible-wonder-db}.rollup(max, 86400)")]
- Storage 28-day growth: 72.52 GB [Source: Datadog MCP → query_metrics("max:azure.dbformysql_flexibleservers.storage_used{name:rfprodv2-flexible-wonder-db}.rollup(max, 86400)")]

## 6. Kafka Consumer Lag
- Topic `courier-supply-health-trigger-message`:
  - 28-day average lag: 20.75 messages [Source: Datadog MCP → query_metrics("max:kafka.consumer_lag{env:prod,consumer_group:wonder-cart-service} by {topic}.rollup(max, 86400)")]
  - Peak lag: 232 messages (at 2026-09-25 00:00 UTC) [Source: Datadog MCP → query_metrics("max:kafka.consumer_lag{env:prod,consumer_group:wonder-cart-service} by {topic}.rollup(max, 86400)")]
- Topic `courier-supply-health-trigger-periodically-message`:
  - 28-day average lag: 0 messages [Source: Datadog MCP → query_metrics("max:kafka.consumer_lag{env:prod,consumer_group:wonder-cart-service} by {topic}.rollup(max, 86400)")]
  - Peak lag: 0 messages [Source: Datadog MCP → query_metrics("max:kafka.consumer_lag{env:prod,consumer_group:wonder-cart-service} by {topic}.rollup(max, 86400)")]
- Topic `wonder-configuration-updated`:
  - 28-day average lag: 0 messages [Source: Datadog MCP → query_metrics("max:kafka.consumer_lag{env:prod,consumer_group:wonder-cart-service} by {topic}.rollup(max, 86400)")]
  - Peak lag: 0 messages [Source: Datadog MCP → query_metrics("max:kafka.consumer_lag{env:prod,consumer_group:wonder-cart-service} by {topic}.rollup(max, 86400)")]
- Topic `consumer-clean-data`:
  - 28-day average lag: 0 messages [Source: Datadog MCP → query_metrics("max:kafka.consumer_lag{env:prod,consumer_group:wonder-cart-service} by {topic}.rollup(max, 86400)")]
  - Peak lag: 0 messages [Source: Datadog MCP → query_metrics("max:kafka.consumer_lag{env:prod,consumer_group:wonder-cart-service} by {topic}.rollup(max, 86400)")]
- Topic `corporate-order-client-changed`:
  - 28-day average lag: 0 messages [Source: Datadog MCP → query_metrics("max:kafka.consumer_lag{env:prod,consumer_group:wonder-cart-service} by {topic}.rollup(max, 86400)")]
  - Peak lag: 0 messages [Source: Datadog MCP → query_metrics("max:kafka.consumer_lag{env:prod,consumer_group:wonder-cart-service} by {topic}.rollup(max, 86400)")]

## 7. SLO & Monitors
- Monitors: None in ALERT or WARN [Source: Datadog MCP → get_monitors(groupStates=["alert", "warn"], tags=["service:wonder-cart-service"])]
- SLOs: NOT_COLLECTED: the configured Datadog MCP server exposes no SLO tool

## 8. Infrastructure Live State (Azure MCP)
- MySQL Flexible Server:
  - Server Name: rfprodv2-flexible-wonder-db [Source: Azure MCP → mysql_server_config_get]
  - Location: eastus2 [Source: Azure MCP → mysql_server_config_get]
  - Version: 8.4 [Source: Azure MCP → mysql_server_config_get]
  - SKU: Standard_E16ds_v4 [Source: Azure MCP → mysql_server_config_get]
  - Provisioned Storage Size (GB): 1671 [Source: Azure MCP → mysql_server_config_get]
  - Backup Retention Days: 7 [Source: Azure MCP → mysql_server_config_get]
  - Geo-Redundant Backup: Enabled [Source: Azure MCP → mysql_server_config_get]
  - Max Connections: 2731 [Source: Azure MCP → mysql_server_param_get]
  - Slow Query Log: ON [Source: Azure MCP → mysql_server_param_get]
  - Long Query Time: 5s [Source: Azure MCP → mysql_server_param_get]
  - Storage Used (absolute): 1583.59 GB [Source: Datadog MCP → query_metrics("max:azure.dbformysql_flexibleservers.storage_used{name:rfprodv2-flexible-wonder-db}.rollup(max, 86400)")]
- AKS Node Pools:
  - Cluster Name: RFProdV2AKSCluster [Source: Azure MCP → aks_cluster_get]
  - Application Node Pool (largest maxCount User pool): `apppoolv5` (VM Size: Standard_D16s_v4, minCount: 18, maxCount: 42, count: 37) [Source: Azure MCP → aks_cluster_get]

## 9. MySQL Table Storage (Top 20)
| Table | Rows | Data (MB) | Index (MB) | Total (MB) |
| :--- | ---: | ---: | ---: | ---: |
| cart_menu_item_options | 159009 | 175.02 | 106.48 | 281.50 |
| web_cart_menu_item_options | 123440 | 100.02 | 93.41 | 193.42 |
| cart_items | 61927 | 121.25 | 44.23 | 165.48 |
| web_cart_items | 48973 | 55.02 | 40.19 | 95.20 |
| cart_bundle_item_choice_options | 34363 | 33.09 | 23.14 | 56.23 |
| cart_bundle_item_choices | 29787 | 33.08 | 23.14 | 56.22 |
| web_cart_bundle_item_choice_options | 26252 | 20.08 | 19.11 | 39.19 |
| carts | 39090 | 28.06 | 10.05 | 38.11 |
| web_cart_bundle_item_choices | 25639 | 18.06 | 18.05 | 36.11 |
| web_carts | 26466 | 16.06 | 12.05 | 28.11 |
| cart_carry_over_histories | 28765 | 21.02 | 0.00 | 21.02 |
| reorder_cart_created_records | 36129 | 8.06 | 2.02 | 10.08 |
| pop_cart_items | 167 | 0.13 | 0.06 | 0.19 |
| pop_cart_menu_item_options | 197 | 0.11 | 0.03 | 0.14 |
| pop_cart_bundle_item_choices | 54 | 0.05 | 0.03 | 0.08 |
| pop_carts | 108 | 0.05 | 0.02 | 0.06 |
| pop_cart_bundle_item_choice_options | 11 | 0.02 | 0.03 | 0.05 |
| flyway_schema_history | 111 | 0.02 | 0.02 | 0.03 |

- **Largest table**: `cart_menu_item_options` at 0.27 GB (159009 rows)
- **Tables above 10 GB**: 0
- **Total of the top 18 tables**: 1.00 GB

[Source: MySQL (rfprodv2-flexible-wonder-db/wonder_cart) → SELECT table_name, table_rows, ROUND(data_length/1024/1024,2) AS data_mb, ROUND(index_length/1024/1024,2) AS index_mb, ROUND((data_length+index_length)/1024/1024,2) AS total_mb FROM information_schema.tables WHERE table_schema='wonder_cart' ORDER BY (data_length+index_length) DESC LIMIT 20]

## 10. Collection Gaps
- SLOs: NOT_COLLECTED: the configured Datadog MCP server exposes no SLO tool

## 11. Metrics Snapshot (JSON)
```json
{
  "avg_rps": 0.4625,
  "peak_rps": 0.5969,
  "wow_rps_growth_pct": 0.81,
  "p50_latency_ms": 0.2381,
  "p95_latency_ms": 1.4197,
  "p99_latency_ms": 1.8635,
  "p99_latency_peak_ms": 2.7200,
  "error_rate_pct": 0.0,
  "container_restarts": 0,
  "cpu_avg_cores": 0.009335,
  "cpu_peak_cores": 0.250005,
  "mem_avg_mib": 388.95,
  "mem_peak_mib": 419.19,
  "replicas_avg": 2.0,
  "replicas_max": 3.0,
  "kafka_lag_avg": 20.75,
  "kafka_lag_peak": 232,
  "mysql_connection_avg": 928.37,
  "mysql_connection_peak": 1169,
  "mysql_max_connections": 2731,
  "mysql_slow_queries_per_day": 695.79,
  "mysql_replication_lag_avg_s": 340.50,
  "mysql_storage_used_gb": 1583.59,
  "mongo_read_latency_ms": null,
  "mongo_read_latency_peak_ms": null,
  "mongo_write_latency_ms": null,
  "mongo_connections_peak": null,
  "mongo_targeting_ratio_avg": null,
  "mongo_targeting_ratio_peak": null,
  "mongo_data_size_gb": null,
  "mongo_index_size_gb": null,
  "mongo_collections": null,
  "mongo_documents": null,
  "mongo_data_growth_pct": null,
  "largest_table_gb": 0.27
}
```
