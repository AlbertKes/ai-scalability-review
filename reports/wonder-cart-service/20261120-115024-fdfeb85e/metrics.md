# Collected Metrics — wonder-cart-service (prod)
**Collected At**: 2026-11-20 03:51 UTC
**Lookback**: 28 days (2026-10-23 to 2026-11-20, UTC day-aligned)

## 1. Traffic & Throughput
- **Average RPS**: 0.4610 RPS [Source: Datadog API → query-metrics("sum:trace.undertow_http.request.hits{env:prod,service:wonder-cart-service}.as_count().rollup(sum, 86400)")]
- **Week-over-Week Average RPS Growth**: -0.44% (week 3: 278685 requests; week 4: 277463 requests) [Source: Datadog API → query-metrics("sum:trace.undertow_http.request.hits{env:prod,service:wonder-cart-service}.as_count().rollup(sum, 86400)")]
- **Weekly Volumes** (4 rows, oldest first):
  - 2026-10-23 to 2026-10-29: 280438 requests
  - 2026-10-30 to 2026-11-05: 278616 requests
  - 2026-11-06 to 2026-11-12: 278685 requests
  - 2026-11-13 to 2026-11-19: 277463 requests
  [Source: Datadog API → query-metrics("sum:trace.undertow_http.request.hits{env:prod,service:wonder-cart-service}.as_count().rollup(sum, 86400)")]
- **Peak RPS**: 0.6025 RPS (2169 requests in the hour starting 2026-10-28 19:00 UTC) [Source: Datadog API → query-metrics("sum:trace.undertow_http.request.hits{env:prod,service:wonder-cart-service}.as_count().rollup(sum, 3600)")]

## 2. Latency Profile
- **P50 Average**: 0.25 ms [Source: Datadog API → query-metrics("p50:trace.undertow_http.request{env:prod,service:wonder-cart-service}.rollup(avg, 86400)")]
- **P95 Average**: 1.44 ms [Source: Datadog API → query-metrics("p95:trace.undertow_http.request{env:prod,service:wonder-cart-service}.rollup(avg, 86400)")]
- **P99 Average**: 1.97 ms [Source: Datadog API → query-metrics("p99:trace.undertow_http.request{env:prod,service:wonder-cart-service}.rollup(avg, 86400)")]
- **Peak P99**: 3.18 ms (2026-11-19) [Source: Datadog API → query-metrics("p99:trace.undertow_http.request{env:prod,service:wonder-cart-service}.rollup(max, 86400)")]

## 3. Error Rates & Stability
- **HTTP Error Rate, 28-Day Average**: 0.0000% (0 errors across 1115202 hits) [Source: Datadog API → query-metrics("sum:trace.undertow_http.request.errors{env:prod,service:wonder-cart-service}.as_count().rollup(sum, 86400)")]
- **HTTP Error Rate, Worst Single Day**: 0.0000% (2026-10-23) [Source: Datadog API → query-metrics("sum:trace.undertow_http.request.errors{env:prod,service:wonder-cart-service}.as_count().rollup(sum, 86400)")]
- **Container Restarts**: 0 over the 28 days [Source: Datadog API → query-metrics("sum:kubernetes.containers.restarts{env:prod,service:wonder-cart-service}.rollup(sum, 86400)")]
- **OOMKill Events**: 0 over the 28 days [Source: Datadog API → query-metrics("sum:kubernetes.containers.last_state.terminated{env:prod,service:wonder-cart-service,reason:oomkilled}.rollup(sum, 86400)")]

## 4. Resource Saturation
- **CPU Usage - Average**: 0.0111 cores (11079271 nanocores, per pod) [Source: Datadog API → query-metrics("avg:kubernetes.cpu.usage.total{env:prod,service:wonder-cart-service}.rollup(avg, 86400)")]
- **CPU Usage - Peak**: 1.1916 cores (1191649295 nanocores, per pod) on 2026-11-02 [Source: Datadog API → query-metrics("max:kubernetes.cpu.usage.total{env:prod,service:wonder-cart-service}.rollup(max, 86400)")]
- **Memory Working Set - Average**: 376.51 MiB (394800081 bytes) [Source: Datadog API → query-metrics("avg:kubernetes.memory.working_set{env:prod,service:wonder-cart-service}.rollup(avg, 86400)")]
- **Memory Working Set - Peak**: 412.05 MiB (432062464 bytes) on 2026-11-19 [Source: Datadog API → query-metrics("max:kubernetes.memory.working_set{env:prod,service:wonder-cart-service}.rollup(max, 86400)")]
- **Replicas - Average**: 2.00 replicas [Source: Datadog API → query-metrics("avg:kubernetes_state.deployment.replicas{env:prod,kube_deployment:wonder-cart-service}.rollup(avg, 86400)")]
- **Replicas - Max**: 3 replicas (on 2026-11-02) [Source: Datadog API → query-metrics("max:kubernetes_state.deployment.replicas{env:prod,kube_deployment:wonder-cart-service}.rollup(max, 86400)")]

## 5. Persistence Layer
- **MySQL Active Connections - Average**: 944.47 [Source: Datadog API → query-metrics("avg:azure.dbformysql_flexibleservers.active_connections{name:rfprodv2-flexible-wonder-db}.rollup(avg, 86400)")]
- **MySQL Active Connections - Peak**: 1535 (2026-11-09) [Source: Datadog API → query-metrics("max:azure.dbformysql_flexibleservers.active_connections{name:rfprodv2-flexible-wonder-db}.rollup(max, 86400)")]
- **MySQL Slow Queries**: 942.21 per day on average (total 26382 over 28 days); worst single day 1159 (2026-11-18) [Source: Datadog API → query-metrics("sum:azure.dbformysql_flexibleservers.slow_queries{name:rfprodv2-flexible-wonder-db}.as_count().rollup(sum, 86400)")]
- **MySQL Replication Lag - Average**: 2.53 s [Source: Datadog API → query-metrics("avg:azure.dbformysql_flexibleservers.replication_lag{name:rfprodv2-flexible-wonder-db-*}.rollup(avg, 86400)")]
- **MySQL Replication Lag - Peak**: 2198.00 s (2026-11-02) [Source: Datadog API → query-metrics("max:azure.dbformysql_flexibleservers.replication_lag{name:rfprodv2-flexible-wonder-db-*}.rollup(max, 86400)")]
- **MySQL Storage Used**: 1769.09 GB at the last bucket (2026-11-19); 28-day growth +154.49 GB [Source: Datadog API → query-metrics("max:azure.dbformysql_flexibleservers.storage_used{name:rfprodv2-flexible-wonder-db}.rollup(max, 86400)")]

## 6. Kafka Consumer Lag
- **Consumer group `wonder-cart-service`** (messages; top 8 topics) [Source: Datadog API → query-metrics("top(avg:kafka.consumer_lag{env:prod,consumer_group:wonder-cart-service} by {topic}.rollup(avg, 86400), 8, 'mean', 'desc')") and query-metrics("top(max:kafka.consumer_lag{env:prod,consumer_group:wonder-cart-service} by {topic}.rollup(max, 86400), 8, 'max', 'desc')")]:
  - `courier-supply-health-trigger-message`: 28-day average 0.03; peak 36 (2026-11-19)
  - `consumer-clean-data`: 28-day average 0.00; peak 0 (2026-10-23)
  - `corporate-order-client-changed`: 28-day average 0.00; peak 0 (2026-10-23)
  - `courier-supply-health-trigger-periodically-message`: 28-day average 0.00; peak 0 (2026-10-23)
  - `wonder-configuration-updated`: 28-day average 0.00; peak 0 (2026-10-23)
- Snapshot values are the worst topic's 28-day average and the highest peak across the groups above.

## 7. SLO & Monitors
- **Monitors in ALERT or WARN**: None in ALERT or WARN [Source: Datadog API → GET /api/v1/monitor?group_states=alert,warn&tags=service:wonder-cart-service]
- **SLOs** (attainment over the 28-day window; error budget = (attainment − target) ÷ (100 − target)):
  1. **Latency SLO for wonder-cart-service service** (`035131df1dcb5e16b7e74a48691195fa`)
     - Type: metric
     - Target: 95% (7d timeframe)
     - 28-Day Attainment: 99.9956%
     - Error Budget Remaining: 99.9121%
     - Status: compliant
     [Source: Datadog API → GET /api/v1/slo/035131df1dcb5e16b7e74a48691195fa/history]
  2. **Availability SLO for wonder-cart-service service** (`b4e50da728195fb2941462245967ed01`)
     - Type: metric
     - Target: 99.9% (7d timeframe)
     - 28-Day Attainment: 100.0000%
     - Error Budget Remaining: 100.0000%
     - Status: compliant
     [Source: Datadog API → GET /api/v1/slo/b4e50da728195fb2941462245967ed01/history]

## 8. Infrastructure Live State (Azure MCP)
### MySQL Flexible Server (`rfprodv2-flexible-wonder-db`)
- **Server Name**: `rfprodv2-flexible-wonder-db` [Source: Azure MCP → mysql_server_config_get(server=rfprodv2-flexible-wonder-db, resource-group=RFProdV2, subscription=Wonder-Platform-Prod)]
- **Location**: `eastus2` [Source: Azure MCP → mysql_server_config_get(server=rfprodv2-flexible-wonder-db, resource-group=RFProdV2, subscription=Wonder-Platform-Prod)]
- **Version**: `8.4` [Source: Azure MCP → mysql_server_config_get(server=rfprodv2-flexible-wonder-db, resource-group=RFProdV2, subscription=Wonder-Platform-Prod)]
- **SKU**: `Standard_E16ds_v4` [Source: Azure MCP → mysql_server_config_get(server=rfprodv2-flexible-wonder-db, resource-group=RFProdV2, subscription=Wonder-Platform-Prod)]
- **StorageSizeGB**: `1841` [Source: Azure MCP → mysql_server_config_get(server=rfprodv2-flexible-wonder-db, resource-group=RFProdV2, subscription=Wonder-Platform-Prod)]
- **BackupRetentionDays**: `7` [Source: Azure MCP → mysql_server_config_get(server=rfprodv2-flexible-wonder-db, resource-group=RFProdV2, subscription=Wonder-Platform-Prod)]
- **GeoRedundantBackup**: `Enabled` [Source: Azure MCP → mysql_server_config_get(server=rfprodv2-flexible-wonder-db, resource-group=RFProdV2, subscription=Wonder-Platform-Prod)]
- **max_connections**: `2731` [Source: Azure MCP → mysql_server_param_get(param=max_connections, server=rfprodv2-flexible-wonder-db, resource-group=RFProdV2, subscription=Wonder-Platform-Prod)]
- **slow_query_log**: `ON` [Source: Azure MCP → mysql_server_param_get(param=slow_query_log, server=rfprodv2-flexible-wonder-db, resource-group=RFProdV2, subscription=Wonder-Platform-Prod)]
- **long_query_time**: `5` [Source: Azure MCP → mysql_server_param_get(param=long_query_time, server=rfprodv2-flexible-wonder-db, resource-group=RFProdV2, subscription=Wonder-Platform-Prod)]
- **Replicas / Servers List**: `rfprodv2-flexible-wonder-db-replica-v4` (and other servers in group) [Source: Azure MCP → mysql_list(resource-group=RFProdV2, subscription=Wonder-Platform-Prod)]

### AKS Cluster & Node Pool
- **Cluster Name**: `RFProdV2AKSCluster` [Source: Azure MCP → aks_cluster_get(resource-group=RFProdV2, subscription=Wonder-Platform-Prod)]
- **Kubernetes Version**: `1.34.8` [Source: Azure MCP → aks_cluster_get(resource-group=RFProdV2, subscription=Wonder-Platform-Prod)]
- **Selected Node Pool (`apppoolv5`)**:
  - Name: `apppoolv5` [Source: Azure MCP → aks_nodepool_get(cluster=RFProdV2AKSCluster, resource-group=RFProdV2, subscription=Wonder-Platform-Prod)]
  - vmSize: `Standard_D16s_v4` [Source: Azure MCP → aks_nodepool_get(cluster=RFProdV2AKSCluster, resource-group=RFProdV2, subscription=Wonder-Platform-Prod)]
  - minCount: `18` [Source: Azure MCP → aks_nodepool_get(cluster=RFProdV2AKSCluster, resource-group=RFProdV2, subscription=Wonder-Platform-Prod)]
  - maxCount: `42` [Source: Azure MCP → aks_nodepool_get(cluster=RFProdV2AKSCluster, resource-group=RFProdV2, subscription=Wonder-Platform-Prod)]
  - count: `37` [Source: Azure MCP → aks_nodepool_get(cluster=RFProdV2AKSCluster, resource-group=RFProdV2, subscription=Wonder-Platform-Prod)]
  - enableAutoScaling: `true` [Source: Azure MCP → aks_nodepool_get(cluster=RFProdV2AKSCluster, resource-group=RFProdV2, subscription=Wonder-Platform-Prod)]
  - mode: `User` [Source: Azure MCP → aks_nodepool_get(cluster=RFProdV2AKSCluster, resource-group=RFProdV2, subscription=Wonder-Platform-Prod)]

## 9. MySQL Table Storage (Top 20)
| Table | Rows | Data (MB) | Index (MB) | Total (MB) |
| :--- | ---: | ---: | ---: | ---: |
| cart_menu_item_options | 202293 | 175.02 | 109.64 | 284.66 |
| web_cart_menu_item_options | 171032 | 104.02 | 95.55 | 199.56 |
| cart_items | 81624 | 121.28 | 44.25 | 165.53 |
| web_cart_items | 62280 | 55.02 | 39.25 | 94.27 |
| cart_bundle_item_choice_options | 36508 | 33.11 | 22.14 | 55.25 |
| cart_bundle_item_choices | 30143 | 30.09 | 22.14 | 52.23 |
| web_cart_bundle_item_choice_options | 29824 | 20.08 | 19.14 | 39.22 |
| carts | 43383 | 28.09 | 10.06 | 38.16 |
| web_cart_bundle_item_choices | 24716 | 16.06 | 16.11 | 32.17 |
| web_carts | 32900 | 16.08 | 11.05 | 27.13 |
| cart_carry_over_histories | 38180 | 27.02 | 0.00 | 27.02 |
| reorder_cart_created_records | 43655 | 9.06 | 3.02 | 12.08 |
| pop_cart_menu_item_options | 205 | 0.11 | 0.09 | 0.20 |
| pop_cart_items | 169 | 0.13 | 0.06 | 0.19 |
| pop_cart_bundle_item_choices | 52 | 0.05 | 0.03 | 0.08 |
| pop_carts | 107 | 0.05 | 0.02 | 0.06 |
| pop_cart_bundle_item_choice_options | 27 | 0.02 | 0.03 | 0.05 |
| flyway_schema_history | 113 | 0.02 | 0.02 | 0.03 |

- **Largest table**: `cart_menu_item_options` at 0.28 GB (202293 rows)
- **Tables above 10 GB**: 0
- **Total of the top 18 tables**: 1.00 GB

[Source: MySQL (rfprodv2-flexible-wonder-db/wonder_cart) → SELECT table_name, table_rows, ROUND(data_length/1024/1024,2) AS data_mb, ROUND(index_length/1024/1024,2) AS index_mb, ROUND((data_length+index_length)/1024/1024,2) AS total_mb FROM information_schema.tables WHERE table_schema='wonder_cart' ORDER BY (data_length+index_length) DESC LIMIT 20]

## 10. Collection Gaps
None

## 11. Metrics Snapshot (JSON)
```json
{
  "avg_rps": 0.461,
  "peak_rps": 0.6025,
  "wow_rps_growth_pct": -0.44,
  "p50_latency_ms": 0.25,
  "p95_latency_ms": 1.44,
  "p99_latency_ms": 1.97,
  "p99_latency_peak_ms": 3.18,
  "error_rate_pct": 0,
  "container_restarts": 0,
  "cpu_avg_cores": 0.0111,
  "cpu_peak_cores": 1.1916,
  "mem_avg_mib": 376.51,
  "mem_peak_mib": 412.05,
  "replicas_avg": 2,
  "replicas_max": 3,
  "kafka_lag_avg": 0.03,
  "kafka_lag_peak": 36,
  "mysql_connection_avg": 944.47,
  "mysql_connection_peak": 1535,
  "mysql_max_connections": 2731,
  "mysql_slow_queries_per_day": 942.21,
  "mysql_replication_lag_avg_s": 2.53,
  "mysql_storage_used_gb": 1769.09,
  "mongo_read_latency_ms": null,
  "mongo_read_latency_peak_ms": null,
  "mongo_write_latency_ms": null,
  "mongo_connections_peak": null,
  "mongo_targeting_ratio_avg": null,
  "mongo_targeting_ratio_peak": null,
  "mongo_database": null,
  "mongo_db_storage_gb": null,
  "mongo_db_storage_growth_pct": null,
  "mongo_db_data_size_gb": null,
  "mongo_db_index_size_gb": null,
  "mongo_collections": null,
  "mongo_documents": null,
  "mongo_disk_used_gb": null,
  "mongo_disk_used_pct": null,
  "largest_table_gb": 0.28,
  "slo_count": 2,
  "slo_breached_count": 0,
  "slo_min_error_budget_remaining_pct": 99.9121
}
```
