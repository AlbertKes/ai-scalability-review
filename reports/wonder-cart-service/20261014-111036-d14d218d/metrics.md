# Collected Metrics — wonder-cart-service (prod)
**Collected At**: 2026-10-14 11:10 UTC
**Lookback**: 28 days (2026-09-16 to 2026-10-14, UTC day-aligned)

## 1. Traffic & Throughput
- **Average RPS**: 0.46 [Source: Datadog MCP → query_metrics("sum:trace.undertow_http.request.hits{env:prod,service:wonder-cart-service}.as_count().rollup(sum, 86400)")]
- **Peak RPS**: 0.60 at 2026-09-29 21:00 UTC [Source: Datadog MCP → query_metrics("sum:trace.undertow_http.request.hits{env:prod,service:wonder-cart-service}.as_count().rollup(sum, 3600)")]
- **Week-over-Week Average RPS Growth**: +0.81% [Source: Datadog MCP → query_metrics("sum:trace.undertow_http.request.hits{env:prod,service:wonder-cart-service}.as_count().rollup(sum, 86400)")]

### Weekly Volumes
| Date Range | Total Hits |
| :--- | ---: |
| 2026-09-16 to 2026-09-22 | 275,834 |
| 2026-09-23 to 2026-09-29 | 278,410 |
| 2026-09-30 to 2026-10-06 | 281,193 |
| 2026-10-07 to 2026-10-13 | 283,483 |
[Source: Datadog MCP → query_metrics("sum:trace.undertow_http.request.hits{env:prod,service:wonder-cart-service}.as_count().rollup(sum, 86400)")]

## 2. Latency Profile
- **P50 Average**: 0.24 ms [Source: Datadog MCP → query_metrics("p50:trace.undertow_http.request{env:prod,service:wonder-cart-service}.rollup(avg, 86400)")]
- **P95 Average**: 1.42 ms [Source: Datadog MCP → query_metrics("p95:trace.undertow_http.request{env:prod,service:wonder-cart-service}.rollup(avg, 86400)")]
- **P99 Average**: 1.86 ms [Source: Datadog MCP → query_metrics("p99:trace.undertow_http.request{env:prod,service:wonder-cart-service}.rollup(avg, 86400)")]
- **Peak P99**: 2.72 ms at 2026-10-12 00:00 UTC [Source: Datadog MCP → query_metrics("p99:trace.undertow_http.request{env:prod,service:wonder-cart-service}.rollup(max, 86400)")]

## 3. Error Rates & Stability
- **HTTP Error Rate (28-day average)**: 0.0% [Source: Datadog MCP → query_metrics("sum:trace.undertow_http.request.errors{env:prod,service:wonder-cart-service}.as_count().rollup(sum, 86400)")]
- **HTTP Error Rate (worst single day)**: 0.0% on 2026-09-16 [Source: Datadog MCP → query_metrics("sum:trace.undertow_http.request.errors{env:prod,service:wonder-cart-service}.as_count().rollup(sum, 86400)")]
- **Container Restarts**: 0 (Worst day bucket: 0 on 2026-09-16; no OOMKills reported) [Source: Datadog MCP → query_metrics("sum:kubernetes.containers.restarts{env:prod,service:wonder-cart-service}.rollup(sum, 86400)")]

## 4. Resource Saturation
- **CPU Usage Average**: 0.009335 cores (9,335,266.09 nanocores) [Source: Datadog MCP → query_metrics("avg:kubernetes.cpu.usage.total{env:prod,service:wonder-cart-service}.rollup(avg, 86400)")]
- **CPU Usage Peak**: 0.250005 cores (250,005,029.23 nanocores) at 2026-09-21 00:00 UTC [Source: Datadog MCP → query_metrics("max:kubernetes.cpu.usage.total{env:prod,service:wonder-cart-service}.rollup(max, 86400)")]
- **Memory Working Set Average**: 388.95 MiB (407,841,353.62 bytes) [Source: Datadog MCP → query_metrics("avg:kubernetes.memory.working_set{env:prod,service:wonder-cart-service}.rollup(avg, 86400)")]
- **Memory Working Set Peak**: 419.19 MiB (439,554,048.00 bytes) at 2026-09-21 00:00 UTC [Source: Datadog MCP → query_metrics("max:kubernetes.memory.working_set{env:prod,service:wonder-cart-service}.rollup(max, 86400)")]
- **Replicas Average**: 2.00 [Source: Datadog MCP → query_metrics("avg:kubernetes_state.deployment.replicas{env:prod,kube_deployment:wonder-cart-service}.rollup(avg, 86400)")]
- **Replicas Max**: 3 [Source: Datadog MCP → query_metrics("max:kubernetes_state.deployment.replicas{env:prod,kube_deployment:wonder-cart-service}.rollup(max, 86400)")]

## 5. Persistence Layer
- **MySQL Active Connections Average**: 928.37 [Source: Datadog MCP → query_metrics("avg:azure.dbformysql_flexibleservers.active_connections{name:rfprodv2-flexible-wonder-db}.rollup(avg, 86400)")]
- **MySQL Active Connections Peak**: 1169 [Source: Datadog MCP → query_metrics("max:azure.dbformysql_flexibleservers.active_connections{name:rfprodv2-flexible-wonder-db}.rollup(max, 86400)")]
- **MySQL Slow Queries Daily Average**: 695.79 queries/day [Source: Datadog MCP → query_metrics("sum:azure.dbformysql_flexibleservers.slow_queries{name:rfprodv2-flexible-wonder-db}.as_count().rollup(sum, 86400)")]
- **MySQL Replication Lag Average**: 340.50 s [Source: Datadog MCP → query_metrics("max:azure.dbformysql_flexibleservers.replication_lag{name:rfprodv2-flexible-wonder-db-*}.rollup(max, 86400)")]
- **MySQL Replication Lag Peak**: 397 s [Source: Datadog MCP → query_metrics("max:azure.dbformysql_flexibleservers.replication_lag{name:rfprodv2-flexible-wonder-db-*}.rollup(max, 86400)")]
- **MySQL Storage Used**: 1583.59 GB (Current/Last bucket) [Source: Datadog MCP → query_metrics("max:azure.dbformysql_flexibleservers.storage_used{name:rfprodv2-flexible-wonder-db}.rollup(max, 86400)")]

## 6. Kafka Consumer Lag
- **Topic**: `courier-supply-health-trigger-message`
  - 28-day Average Lag: 43.61 messages [Source: Datadog MCP → query_metrics("max:kafka.consumer_lag{env:prod,consumer_group:wonder-cart-service} by {topic}.rollup(max, 86400)")]
  - Peak Lag: 232 messages at 2026-10-01 00:00 UTC [Source: Datadog MCP → query_metrics("max:kafka.consumer_lag{env:prod,consumer_group:wonder-cart-service} by {topic}.rollup(max, 86400)")]
- **Topic**: `courier-supply-health-trigger-periodically-message` — Average Lag: 0.00 messages, Peak Lag: 0 messages [Source: Datadog MCP → query_metrics("max:kafka.consumer_lag{env:prod,consumer_group:wonder-cart-service} by {topic}.rollup(max, 86400)")]
- **Topic**: `wonder-configuration-updated` — Average Lag: 0.00 messages, Peak Lag: 0 messages [Source: Datadog MCP → query_metrics("max:kafka.consumer_lag{env:prod,consumer_group:wonder-cart-service} by {topic}.rollup(max, 86400)")]
- **Topic**: `consumer-clean-data` — Average Lag: 0.00 messages, Peak Lag: 0 messages [Source: Datadog MCP → query_metrics("max:kafka.consumer_lag{env:prod,consumer_group:wonder-cart-service} by {topic}.rollup(max, 86400)")]
- **Topic**: `corporate-order-client-changed` — Average Lag: 0.00 messages, Peak Lag: 0 messages [Source: Datadog MCP → query_metrics("max:kafka.consumer_lag{env:prod,consumer_group:wonder-cart-service} by {topic}.rollup(max, 86400)")]

## 7. SLO & Monitors
- **Monitors**: None in ALERT or WARN [Source: Datadog MCP → get_monitors(groupStates=["alert", "warn"], tags=["service:wonder-cart-service"]) ]
- **SLOs**: NOT_COLLECTED: the configured Datadog MCP server exposes no SLO tool [Source: Datadog MCP]

## 8. Infrastructure Live State (Azure MCP)
- **MySQL Flexible Server**:
  - Server Name: `rfprodv2-flexible-wonder-db`
  - Location: `eastus2`
  - Version: `8.4`
  - SKU: `Standard_E16ds_v4`
  - Provisioned Storage: `1671 GB`
  - Max Connections: `2731` [Source: Azure MCP → mysql(mysql_server_config_get / mysql_server_param_get)]
- **AKS Node Pool**:
  - Cluster Name: `RFProdV2AKSCluster`
  - Active Pool: `apppoolv5` (VM Size: `Standard_D16s_v4`, minCount: 18, maxCount: 42, count: 37, enableAutoScaling: true) [Source: Azure MCP → aks(aks_cluster_get)]

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
- None (All expected metrics successfully collected).

## 11. Metrics Snapshot (JSON)
```json
{
  "avg_rps": 0.4625,
  "peak_rps": 0.5969,
  "wow_rps_growth_pct": 0.8144,
  "p50_latency_ms": 0.2381,
  "p95_latency_ms": 1.4197,
  "p99_latency_ms": 1.8635,
  "p99_latency_peak_ms": 2.7200,
  "error_rate_pct": 0.0,
  "container_restarts": 0,
  "cpu_avg_cores": 0.009335,
  "cpu_peak_cores": 0.250005,
  "mem_avg_mib": 388.9478,
  "mem_peak_mib": 419.1914,
  "replicas_avg": 2.00,
  "replicas_max": 3,
  "kafka_lag_avg": 43.61,
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
