# Collected Metrics — ba-auto-ship-service (prod)
**Collected At**: 2026-10-15 00:00 UTC
**Lookback**: 28 days (2026-09-17 to 2026-10-14, UTC day-aligned)

## 1. Traffic & Throughput
- **Average RPS**: 19.11
- **Peak RPS**: 118.20 RPS (425,534 hits/hour) at 2026-09-21 21:00 UTC [Source: Datadog MCP → query_metrics("sum:trace.undertow_http.request.hits{env:prod,service:ba-auto-ship-service}.as_count().rollup(sum, 3600)")]
- **Week-over-Week Average RPS Growth**: -11.12% (W3: 11,697,477 hits, W4: 10,396,281 hits)
- **Weekly Volumes (4 blocks of 7 days)**:
  - 2026-09-17 to 2026-09-23: 12,652,450 hits
  - 2026-09-24 to 2026-09-30: 11,485,537 hits
  - 2026-10-01 to 2026-10-07: 11,697,477 hits
  - 2026-10-08 to 2026-10-14: 10,396,281 hits
  [Source: Datadog MCP → query_metrics("sum:trace.undertow_http.request.hits{env:prod,service:ba-auto-ship-service}.as_count().rollup(sum, 86400)")]

## 2. Latency Profile
- **P50 Average**: 5.88 ms [Source: Datadog MCP → query_metrics("p50:trace.undertow_http.request{env:prod,service:ba-auto-ship-service}.rollup(avg, 86400)")]
- **P95 Average**: 109.12 ms [Source: Datadog MCP → query_metrics("p95:trace.undertow_http.request{env:prod,service:ba-auto-ship-service}.rollup(avg, 86400)")]
- **P99 Average**: 157.66 ms [Source: Datadog MCP → query_metrics("p99:trace.undertow_http.request{env:prod,service:ba-auto-ship-service}.rollup(avg, 86400)")]
- **Peak P99**: 181.68 ms at 2026-10-12 00:00 UTC [Source: Datadog MCP → query_metrics("p99:trace.undertow_http.request{env:prod,service:ba-auto-ship-service}.rollup(max, 86400)")]

## 3. Error Rates & Stability
- **HTTP Error Rate, 28-day Average**: 0.000032% (15 errors across 46,231,745 requests) [Source: Datadog MCP → query_metrics("sum:trace.undertow_http.request.errors{env:prod,service:ba-auto-ship-service}.as_count().rollup(sum, 86400)")]
- **HTTP Error Rate, Worst Single Day**: 0.000311% on 2026-10-06 [Source: Datadog MCP → query_metrics("sum:trace.undertow_http.request.errors{env:prod,service:ba-auto-ship-service}.as_count().rollup(sum, 86400)")]
- **Container Restarts**: 0 restarts across the 28-day window [Source: Datadog MCP → query_metrics("sum:kubernetes.containers.restarts{env:prod,service:ba-auto-ship-service}.rollup(sum, 86400)")]

## 4. Resource Saturation
- **CPU Usage, Average**: 38,500,288 nanocores (0.0385 cores) [Source: Datadog MCP → query_metrics("avg:kubernetes.cpu.usage.total{env:prod,service:ba-auto-ship-service}.rollup(avg, 86400)")]
- **CPU Usage, Peak**: 795,824,288 nanocores (0.7958 cores) at 2026-09-22 [Source: Datadog MCP → query_metrics("max:kubernetes.cpu.usage.total{env:prod,service:ba-auto-ship-service}.rollup(max, 86400)")]
- **Memory Working Set, Average**: 562,357,301 bytes (536.35 MiB) [Source: Datadog MCP → query_metrics("avg:kubernetes.memory.working_set{env:prod,service:ba-auto-ship-service}.rollup(avg, 86400)")]
- **Memory Working Set, Peak**: 629,624,832 bytes (600.46 MiB) at 2026-10-11 [Source: Datadog MCP → query_metrics("max:kubernetes.memory.working_set{env:prod,service:ba-auto-ship-service}.rollup(max, 86400)")]
- **Replicas, Average**: 3.00 replicas [Source: Datadog MCP → query_metrics("avg:kubernetes_state.deployment.replicas{env:prod,kube_deployment:ba-auto-ship-service}.rollup(avg, 86400)")]
- **Replicas, Maximum**: 4 replicas [Source: Datadog MCP → query_metrics("max:kubernetes_state.deployment.replicas{env:prod,kube_deployment:ba-auto-ship-service}.rollup(max, 86400)")]

## 5. Persistence Layer
### MySQL (rfprodv2-ba-consumer-db)
- **Active Connections, 28-day Average**: 165.78 [Source: Datadog MCP → query_metrics("avg:azure.dbformysql_flexibleservers.active_connections{name:rfprodv2-ba-consumer-db}.rollup(avg, 86400)")]
- **Active Connections, Peak**: 273 at 2026-09-23 [Source: Datadog MCP → query_metrics("max:azure.dbformysql_flexibleservers.active_connections{name:rfprodv2-ba-consumer-db}.rollup(max, 86400)")]
- **Slow Queries, Daily Average**: 0.54 queries/day (15 total over 28 days) [Source: Datadog MCP → query_metrics("sum:azure.dbformysql_flexibleservers.slow_queries{name:rfprodv2-ba-consumer-db}.as_count().rollup(sum, 86400)")]
- **Slow Queries, Worst Single Day**: 11 slow queries on 2026-09-24 [Source: Datadog MCP → query_metrics("sum:azure.dbformysql_flexibleservers.slow_queries{name:rfprodv2-ba-consumer-db}.as_count().rollup(sum, 86400)")]
- **Replication Lag, 28-day Average**: 18.50 seconds [Source: Datadog MCP → query_metrics("max:azure.dbformysql_flexibleservers.replication_lag{name:rfprodv2-ba-consumer-db-*}.rollup(max, 86400)")]
- **Replication Lag, Peak**: 27 seconds at 2026-09-20 [Source: Datadog MCP → query_metrics("max:azure.dbformysql_flexibleservers.replication_lag{name:rfprodv2-ba-consumer-db-*}.rollup(max, 86400)")]
- **Storage Used (Last)**: 175.57 GB (188,517,049,317 bytes) [Source: Datadog MCP → query_metrics("max:azure.dbformysql_flexibleservers.storage_used{name:rfprodv2-ba-consumer-db}.rollup(max, 86400)")]
- **Storage Growth (28-day)**: 0.34 GB [Source: Datadog MCP → query_metrics("max:azure.dbformysql_flexibleservers.storage_used{name:rfprodv2-ba-consumer-db}.rollup(max, 86400)")]

### MongoDB Atlas (prod-blueapron-main)
- **Read Latency, 28-day Average**: 0.059 ms [Source: Datadog MCP → query_metrics("avg:mongodb.atlas.oplatencies.reads.avg{clustername:prod-blueapron-main}.rollup(avg, 86400)")]
- **Read Latency, Peak**: 247.88 ms at 2026-09-24 [Source: Datadog MCP → query_metrics("max:mongodb.atlas.oplatencies.reads.avg{clustername:prod-blueapron-main}.rollup(max, 86400)")]
- **Write Latency, 28-day Average**: 1.46 ms [Source: Datadog MCP → query_metrics("avg:mongodb.atlas.oplatencies.writes.avg{clustername:prod-blueapron-main}.rollup(avg, 86400)")]
- **Connections, Peak**: 302 connections at 2026-09-29 [Source: Datadog MCP → query_metrics("max:mongodb.atlas.connections.current{clustername:prod-blueapron-main}.rollup(max, 86400)")]
- **Query Targeting Ratio, 28-day Average**: 1.92 [Source: Datadog MCP → query_metrics("avg:mongodb.atlas.metrics.queryexecutor.scannedperreturned{clustername:prod-blueapron-main}.rollup(avg, 86400)")]
- **Query Targeting Ratio, Worst Day**: 8,333.82 on 2026-09-24 [Source: Datadog MCP → query_metrics("avg:mongodb.atlas.metrics.queryexecutor.scannedperreturned{clustername:prod-blueapron-main}.rollup(max, 86400)")]
- **Storage Data Size**: 4.11 GB [Source: Datadog MCP → query_metrics("avg:mongodb.atlas.dbstats.data.size{clustername:prod-blueapron-main}.rollup(max, 86400)")]
- **Storage Index Size**: 0.66 GB [Source: Datadog MCP → query_metrics("avg:mongodb.atlas.dbstats.index.size{clustername:prod-blueapron-main}.rollup(max, 86400)")]
- **Collections Count**: 9 [Source: Datadog MCP → query_metrics("avg:mongodb.atlas.dbstats.collections{clustername:prod-blueapron-main}.rollup(max, 86400)")]
- **Documents Count**: 3,968,227 [Source: Datadog MCP → query_metrics("avg:mongodb.atlas.dbstats.objects{clustername:prod-blueapron-main}.rollup(max, 86400)")]
- **Data Size Growth % (28-day)**: -6.17% [Source: Datadog MCP → query_metrics("avg:mongodb.atlas.dbstats.data.size{clustername:prod-blueapron-main}.rollup(max, 86400)")]

## 6. Kafka Consumer Lag
- **Consumer Group**: ba-auto-ship-service
- **Topics Monitored**: 26 topics (e.g., `ba-auto-ship-auth-cart-message`, `auto-ship-rebalance-event-message`, `ba-auto-ship-generate-order-message`, etc.)
- **Peak Lag**: 1,944 messages on topic `auto-ship-rebalance-event-message` on 2026-09-19
- **Average Lag**: Near zero across all topics, with transient peaks on rebalance / fulfillment triggers.
[Source: Datadog MCP → query_metrics("max:kafka.consumer_lag{env:prod,consumer_group:ba-auto-ship-service} by {topic}.rollup(max, 86400)")]

## 7. SLO & Monitors
- **Monitors Returned**:
  - `[BlueApron][AutoShip] No Enrollments in Past Hour (Active Hours)` — State: `No Data`
  [Source: Datadog MCP → get_monitors(groupStates=["alert", "warn"], tags=["service:ba-auto-ship-service"])]
- **SLO Attainment**: `NOT_COLLECTED: the configured Datadog MCP server exposes no SLO tool`

## 8. Infrastructure Live State (Azure MCP)
- **MySQL Flexible Server (`rfprodv2-ba-consumer-db`)**:
  - Location: `eastus2`
  - Version: `8.4`
  - SKU: `Standard_E8ds_v4`
  - Provisioned Storage: `312 GB`
  - `max_connections`: `5461`
  - `slow_query_log`: `ON`
  - `long_query_time`: `10`
  [Source: Azure MCP → mysql / mysql_server_config_get & mysql_server_param_get]
- **AKS Cluster (`RFProdV2AKSCluster` in `RFProdV2`)**:
  - Node Pool: `apppoolv5` (User pool with largest maxCount)
  - VM Size: `Standard_D16s_v4`
  - Scaling: Min `18`, Max `42`, Current Count `37`, Auto-scaling `true`
  [Source: Azure MCP → aks / aks_cluster_get]

## 9. MySQL Table Storage (Top 20)
| Table | Rows | Data (MB) | Index (MB) | Total (MB) |
| :--- | ---: | ---: | ---: | ---: |
| auto_ship_cart_fulfillment_events | 1381162 | 36053.02 | 2618.56 | 38671.58 |
| auto_ship_cart_items | 1979941 | 3785.02 | 1621.50 | 5406.52 |
| auto_ship_cart_fulfillment_outboxes | 7008122 | 2387.25 | 2924.58 | 5311.83 |
| auto_ship_carts | 1376051 | 2154.05 | 3102.92 | 5256.97 |
| auto_ship_delivery_addresses | 1254697 | 2731.02 | 2435.09 | 5166.11 |
| simulation_auto_ship_cart_items | 1863265 | 1009.00 | 1011.98 | 2020.98 |
| auto_ship_cart_experiment_results | 3026655 | 1057.33 | 350.98 | 1408.31 |
| auto_ship_cart_skip_reasons | 2386857 | 1044.98 | 312.00 | 1356.98 |
| auto_ship_cart_original_deliveries | 4762527 | 1113.55 | 0.00 | 1113.55 |
| auto_ship_fulfillment_errors | 1071034 | 657.89 | 361.38 | 1019.27 |
| auto_ship_refresh_delivery_option_tasks | 74580 | 816.03 | 16.06 | 832.09 |
| auto_ship_create_cart_tasks | 208260 | 702.02 | 33.17 | 735.19 |
| auto_ship_auth_cart_tasks | 62952 | 559.02 | 16.11 | 575.13 |
| simulation_auto_ship_customizations | 610295 | 256.00 | 318.00 | 574.00 |
| simulation_auto_ship_carts | 632134 | 248.00 | 320.00 | 568.00 |
| simulation_auto_ship_plans | 671784 | 219.00 | 341.00 | 560.00 |
| auto_ship_generate_order_tasks | 68415 | 459.02 | 27.19 | 486.20 |
| auto_ship_delivery_day_shift | 380636 | 185.00 | 77.47 | 262.47 |
| auto_ship_forter_infos | 239229 | 130.00 | 60.48 | 190.48 |
| auto_ship_cart_cancel_reasons | 283069 | 129.00 | 37.77 | 166.77 |

- **Largest table**: `auto_ship_cart_fulfillment_events` at 37.77 GB (1381162 rows)
- **Tables above 10 GB**: 1
- **Total of the top 20 tables**: 70.00 GB

[Source: MySQL (rfprodv2-ba-consumer-db/ba_auto_ship) → SELECT table_name, table_rows, ROUND(data_length/1024/1024,2) AS data_mb, ROUND(index_length/1024/1024,2) AS index_mb, ROUND((data_length+index_length)/1024/1024,2) AS total_mb FROM information_schema.tables WHERE table_schema='ba_auto_ship' ORDER BY (data_length+index_length) DESC LIMIT 20]

## 10. Collection Gaps
- None

## 11. Metrics Snapshot (JSON)
```json
{
  "avg_rps": 19.11,
  "peak_rps": 118.20,
  "wow_rps_growth_pct": -11.12,
  "p50_latency_ms": 5.88,
  "p95_latency_ms": 109.12,
  "p99_latency_ms": 157.66,
  "p99_latency_peak_ms": 181.68,
  "error_rate_pct": 0.000032,
  "container_restarts": 0,
  "cpu_avg_cores": 0.0385,
  "cpu_peak_cores": 0.7958,
  "mem_avg_mib": 536.35,
  "mem_peak_mib": 600.46,
  "replicas_avg": 3.00,
  "replicas_max": 4,
  "kafka_lag_avg": 1.25,
  "kafka_lag_peak": 1944,
  "mysql_connection_avg": 165.78,
  "mysql_connection_peak": 273,
  "mysql_max_connections": 5461,
  "mysql_slow_queries_per_day": 0.54,
  "mysql_replication_lag_avg_s": 18.50,
  "mysql_storage_used_gb": 175.57,
  "mongo_read_latency_ms": 0.059,
  "mongo_read_latency_peak_ms": 247.88,
  "mongo_write_latency_ms": 1.46,
  "mongo_connections_peak": 302,
  "mongo_targeting_ratio_avg": 1.92,
  "mongo_targeting_ratio_peak": 8333.82,
  "mongo_data_size_gb": 4.11,
  "mongo_index_size_gb": 0.66,
  "mongo_collections": 9,
  "mongo_documents": 3968227,
  "mongo_data_growth_pct": -6.17,
  "largest_table_gb": 37.77
}
```
