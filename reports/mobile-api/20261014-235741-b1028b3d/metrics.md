# Collected Metrics — mobile-api (prod)
**Collected At**: 2026-10-14 00:00 UTC
**Lookback**: 28 days (2026-09-16 to 2026-10-14, UTC day-aligned)

## 1. Traffic & Throughput
- **Average RPS**: 62.73
- **Peak RPS**: 228.62 (1,010,934 hits / 3600s bucket on 2026-09-21 16:00 UTC)
- **Week-over-Week Average RPS Growth**: +3.04%
- **Weekly Volumes**:
  - 2026-09-16 to 2026-09-22: 37,801,945 requests
  - 2026-09-23 to 2026-09-29: 37,892,620 requests
  - 2026-09-30 to 2026-10-06: 37,465,101 requests
  - 2026-10-07 to 2026-10-13: 38,604,244 requests

## 2. Latency Profile
- **P50 Average**: 44.93 ms
  - [Source: Datadog MCP → query_metrics("p50:trace.undertow_http.request{env:prod,service:mobile-api}.rollup(avg, 86400)")]
- **P95 Average**: 622.57 ms
  - [Source: Datadog MCP → query_metrics("p95:trace.undertow_http.request{env:prod,service:mobile-api}.rollup(avg, 86400)")]
- **P99 Average**: 1542.34 ms
  - [Source: Datadog MCP → query_metrics("p99:trace.undertow_http.request{env:prod,service:mobile-api}.rollup(avg, 86400)")]
- **Peak P99**: 1774.69 ms on 2026-10-04
  - [Source: Datadog MCP → query_metrics("p99:trace.undertow_http.request{env:prod,service:mobile-api}.rollup(max, 86400)")]

## 3. Error Rates & Stability
- **HTTP Error Rate (28-day average)**: 0.0010% (1,477 errors / 151,763,910 hits)
  - [Source: Datadog MCP → query_metrics("sum:trace.undertow_http.request.errors{env:prod,service:mobile-api}.as_count().rollup(sum, 86400)")]
- **HTTP Error Rate (worst single day)**: 0.0103% on 2026-09-21
  - [Source: Datadog MCP → query_metrics("sum:trace.undertow_http.request.errors{env:prod,service:mobile-api}.as_count().rollup(sum, 86400)")]
- **Container Restarts**: 0 restarts across the 28-day window. No OOMKills observed.
  - [Source: Datadog MCP → query_metrics("sum:kubernetes.containers.restarts{env:prod,service:mobile-api}.rollup(sum, 86400)")]

## 4. Resource Saturation
- **CPU Usage Average**: 0.167 cores
  - [Source: Datadog MCP → query_metrics("avg:kubernetes.cpu.usage.total{env:prod,service:mobile-api}.rollup(avg, 86400)")]
- **CPU Usage Peak**: 2.856 cores on 2026-09-17
  - [Source: Datadog MCP → query_metrics("max:kubernetes.cpu.usage.total{env:prod,service:mobile-api}.rollup(max, 86400)")]
- **Memory Working Set Average**: 977.69 MiB
  - [Source: Datadog MCP → query_metrics("avg:kubernetes.memory.working_set{env:prod,service:mobile-api}.rollup(avg, 86400)")]
- **Memory Working Set Peak**: 2034.12 MiB on 2026-10-09
  - [Source: Datadog MCP → query_metrics("max:kubernetes.memory.working_set{env:prod,service:mobile-api}.rollup(max, 86400)")]
- **Replicas Average**: 5.01
  - [Source: Datadog MCP → query_metrics("avg:kubernetes_state.deployment.replicas{env:prod,kube_deployment:mobile-api}.rollup(avg, 86400)")]
- **Replicas Max**: 9
  - [Source: Datadog MCP → query_metrics("max:kubernetes_state.deployment.replicas{env:prod,kube_deployment:mobile-api}.rollup(max, 86400)")]

## 5. Persistence Layer
- **Active Connections Average**: 928.37
  - [Source: Datadog MCP → query_metrics("avg:azure.dbformysql_flexibleservers.active_connections{name:rfprodv2-flexible-wonder-db}.rollup(avg, 86400)")]
- **Active Connections Peak**: 1169
  - [Source: Datadog MCP → query_metrics("max:azure.dbformysql_flexibleservers.active_connections{name:rfprodv2-flexible-wonder-db}.rollup(max, 86400)")]
- **Slow Queries Daily Average**: 695.79 queries/day (Total: 19,482)
  - [Source: Datadog MCP → query_metrics("sum:azure.dbformysql_flexibleservers.slow_queries{name:rfprodv2-flexible-wonder-db}.as_count().rollup(sum, 86400)")]
- **Slow Queries Worst Single Day**: 880 queries on 2026-10-13
- **Replication Lag Average**: 340.50 seconds
  - [Source: Datadog MCP → query_metrics("max:azure.dbformysql_flexibleservers.replication_lag{name:rfprodv2-flexible-wonder-db-*}.rollup(max, 86400)")]
- **Replication Lag Peak**: 397 seconds
- **Storage Used**: 1583.59 GB
  - [Source: Datadog MCP → query_metrics("max:azure.dbformysql_flexibleservers.storage_used{name:rfprodv2-flexible-wonder-db}.rollup(max, 86400)")]

## 6. Kafka Consumer Lag
- **Consumer Group**: `mobile-api`
- **Topics Monitored**:
  - `coming-soon-restaurant-goes-live`: Average lag: 0 msgs, Peak lag: 0 msgs
  - `consumer-delivery-zone-updated-event`: Average lag: 0 msgs, Peak lag: 0 msgs
  - `customer-profile-updated`: Average lag: 0.32 msgs, Peak lag: 1 msg
  - `wonder-user-updated`: Average lag: 0.89 msgs, Peak lag: 1 msg
  - `corporate-order-client-changed`: Average lag: 0 msgs, Peak lag: 0 msgs
  - `customer-address-updated`: Average lag: 1.25 msgs, Peak lag: 2 msgs
  - `whitelist-changed-event`: Average lag: 0.04 msgs, Peak lag: 1 msg
  - `wonder-configuration-updated`: Average lag: 0 msgs, Peak lag: 0 msgs
  - `wait-list-user-address-serviceable`: Average lag: 0 msgs, Peak lag: 0 msgs
  - `customer-updated`: Average lag: 1.39 msgs, Peak lag: 11 msgs (on 2025-11-26 approx / window start)
  - `customer-deleted`: Average lag: 0 msgs, Peak lag: 0 msgs
  - [Source: Datadog MCP → query_metrics("max:kafka.consumer_lag{env:prod,consumer_group:mobile-api} by {topic}.rollup(max, 86400)")]

## 7. SLO & Monitors
- **Monitors**: None in ALERT or WARN (`service:mobile-api`)
  - [Source: Datadog MCP → get_monitors(groupStates=["alert", "warn"], tags=["service:mobile-api"])]
- **SLOs**: NOT_COLLECTED: the configured Datadog MCP server exposes no SLO tool

## 8. Infrastructure Live State (Azure MCP)
- **MySQL Flexible Server**:
  - Name: `rfprodv2-flexible-wonder-db`
  - SKU: `Standard_E16ds_v4`
  - Version: `8.4`
  - Provisioned Storage: `1592` GB (auto-grow enabled)
  - Max Connections: `2731`
  - Slow Query Log: `ON` (long_query_time: `5`s)
  - [Source: Azure MCP → az mysql flexible-server show / parameter show]
- **AKS Cluster & Node Pool**:
  - Cluster: `RFProdV2AKSCluster` (Resource Group: `RFProdV2`, Location: `eastus2`)
  - Node Pool: `apppoolv5` (VM Size: `Standard_D16s_v4`, minCount: `18`, maxCount: `42`, count: `37`, autoScaling: `true`, mode: `User`)
  - [Source: Azure MCP → aks_nodepool_get]

## 9. MySQL Table Storage (Top 20)
| Table | Rows | Data (MB) | Index (MB) | Total (MB) |
| :--- | ---: | ---: | ---: | ---: |
| sessions | 2112802 | 862.09 | 1804.73 | 2666.83 |
| account_device_histories | 1620797 | 352.97 | 317.00 | 669.97 |
| devices | 2500983 | 443.30 | 0.00 | 443.30 |
| user_first_time_login_histories | 1162718 | 324.95 | 0.00 | 324.95 |
| login_histories_con_24123 | 958672 | 181.00 | 131.98 | 312.98 |
| app_store_ratings | 1319458 | 165.56 | 0.00 | 165.56 |
| restaurant_active_enrollments | 12420 | 8.50 | 11.09 | 19.59 |
| app_store_rating_prompt_triggers | 26621 | 6.55 | 6.56 | 13.11 |
| device_app_attest | 5246 | 2.52 | 0.00 | 2.52 |
| verified_phones | 11189 | 2.41 | 0.00 | 2.41 |
| flyway_schema_history | 35 | 0.02 | 0.02 | 0.03 |
| login_histories | 0 | 0.02 | 0.02 | 0.03 |
| latest_version_configurations | 2 | 0.02 | 0.00 | 0.02 |
| previous_version_configurations | 233 | 0.02 | 0.00 | 0.02 |
| order_numbers | 1 | 0.02 | 0.00 | 0.02 |
| clients | 2 | 0.02 | 0.00 | 0.02 |
| app_search_variant_configurations | 0 | 0.02 | 0.00 | 0.02 |

- **Largest table**: `sessions` at 2.60 GB (2112802 rows)
- **Tables above 10 GB**: 0
- **Total of the top 17 tables**: 4.51 GB

[Source: MySQL (rfprodv2-flexible-wonder-db/mobile) → SELECT table_name, table_rows, ROUND(data_length/1024/1024,2) AS data_mb, ROUND(index_length/1024/1024,2) AS index_mb, ROUND((data_length+index_length)/1024/1024,2) AS total_mb FROM information_schema.tables WHERE table_schema='mobile' ORDER BY (data_length+index_length) DESC LIMIT 20]


## 10. Collection Gaps
- None

## 11. Metrics Snapshot (JSON)
```json
{
  "avg_rps": 62.73,
  "peak_rps": 228.62,
  "wow_rps_growth_pct": 3.04,
  "p50_latency_ms": 44.93,
  "p95_latency_ms": 622.57,
  "p99_latency_ms": 1542.34,
  "p99_latency_peak_ms": 1774.69,
  "error_rate_pct": 0.0010,
  "container_restarts": 0,
  "cpu_avg_cores": 0.167,
  "cpu_peak_cores": 2.856,
  "mem_avg_mib": 977.69,
  "mem_peak_mib": 2034.12,
  "replicas_avg": 5.01,
  "replicas_max": 9,
  "kafka_lag_avg": 0.35,
  "kafka_lag_peak": 11.00,
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
  "largest_table_gb": 2.60
}
```
