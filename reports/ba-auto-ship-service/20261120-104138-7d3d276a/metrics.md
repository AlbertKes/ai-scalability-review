# Collected Metrics — ba-auto-ship-service (prod)
**Collected At**: 2026-11-20 03:31 UTC
**Lookback**: 28 days (2026-10-23 to 2026-11-20, UTC day-aligned)

## 1. Traffic & Throughput
- **Average RPS**: 21.1647 RPS [Source: Datadog API → query-metrics("sum:trace.undertow_http.request.hits{env:prod,service:ba-auto-ship-service}.as_count().rollup(sum, 86400)")]
- **Week-over-Week Average RPS Growth**: -6.66% (week 3: 13690776 requests; week 4: 12778938 requests) [Source: Datadog API → query-metrics("sum:trace.undertow_http.request.hits{env:prod,service:ba-auto-ship-service}.as_count().rollup(sum, 86400)")]
- **Weekly Volumes** (4 rows, oldest first):
  - 2026-10-23 to 2026-10-29: 12837529 requests
  - 2026-10-30 to 2026-11-05: 11894396 requests
  - 2026-11-06 to 2026-11-12: 13690776 requests
  - 2026-11-13 to 2026-11-19: 12778938 requests
  [Source: Datadog API → query-metrics("sum:trace.undertow_http.request.hits{env:prod,service:ba-auto-ship-service}.as_count().rollup(sum, 86400)")]
- **Peak RPS**: 106.3983 RPS (383034 requests in the hour starting 2026-11-16 15:00 UTC) [Source: Datadog API → query-metrics("sum:trace.undertow_http.request.hits{env:prod,service:ba-auto-ship-service}.as_count().rollup(sum, 3600)")]

## 2. Latency Profile
- **P50 Average**: 4.99 ms [Source: Datadog API → query-metrics("p50:trace.undertow_http.request{env:prod,service:ba-auto-ship-service}.rollup(avg, 86400)")]
- **P95 Average**: 109.61 ms [Source: Datadog API → query-metrics("p95:trace.undertow_http.request{env:prod,service:ba-auto-ship-service}.rollup(avg, 86400)")]
- **P99 Average**: 167.72 ms [Source: Datadog API → query-metrics("p99:trace.undertow_http.request{env:prod,service:ba-auto-ship-service}.rollup(avg, 86400)")]
- **Peak P99**: 184.52 ms (2026-10-26) [Source: Datadog API → query-metrics("p99:trace.undertow_http.request{env:prod,service:ba-auto-ship-service}.rollup(max, 86400)")]

## 3. Error Rates & Stability
- **HTTP Error Rate, 28-Day Average**: 0.0002% (80 errors across 51201639 hits) [Source: Datadog API → query-metrics("sum:trace.undertow_http.request.errors{env:prod,service:ba-auto-ship-service}.as_count().rollup(sum, 86400)")]
- **HTTP Error Rate, Worst Single Day**: 0.0023% (2026-10-30) [Source: Datadog API → query-metrics("sum:trace.undertow_http.request.errors{env:prod,service:ba-auto-ship-service}.as_count().rollup(sum, 86400)")]
- **Container Restarts**: 0 over the 28 days [Source: Datadog API → query-metrics("sum:kubernetes.containers.restarts{env:prod,service:ba-auto-ship-service}.rollup(sum, 86400)")]
- **OOMKill Events**: 0 over the 28 days [Source: Datadog API → query-metrics("sum:kubernetes.containers.last_state.terminated{env:prod,service:ba-auto-ship-service,reason:oomkilled}.rollup(sum, 86400)")]

## 4. Resource Saturation
- **CPU Usage - Average**: 0.0378 cores (37758859 nanocores, per pod) [Source: Datadog API → query-metrics("avg:kubernetes.cpu.usage.total{env:prod,service:ba-auto-ship-service}.rollup(avg, 86400)")]
- **CPU Usage - Peak**: 1.7326 cores (1732598196 nanocores, per pod) on 2026-11-16 [Source: Datadog API → query-metrics("max:kubernetes.cpu.usage.total{env:prod,service:ba-auto-ship-service}.rollup(max, 86400)")]
- **Memory Working Set - Average**: 543.76 MiB (570169463 bytes) [Source: Datadog API → query-metrics("avg:kubernetes.memory.working_set{env:prod,service:ba-auto-ship-service}.rollup(avg, 86400)")]
- **Memory Working Set - Peak**: 638.50 MiB (669515776 bytes) on 2026-11-02 [Source: Datadog API → query-metrics("max:kubernetes.memory.working_set{env:prod,service:ba-auto-ship-service}.rollup(max, 86400)")]
- **Replicas - Average**: 3.00 replicas [Source: Datadog API → query-metrics("avg:kubernetes_state.deployment.replicas{env:prod,kube_deployment:ba-auto-ship-service}.rollup(avg, 86400)")]
- **Replicas - Max**: 4 replicas (on 2026-10-26) [Source: Datadog API → query-metrics("max:kubernetes_state.deployment.replicas{env:prod,kube_deployment:ba-auto-ship-service}.rollup(max, 86400)")]

## 5. Persistence Layer
- **MySQL Active Connections - Average**: 163.60 [Source: Datadog API → query-metrics("avg:azure.dbformysql_flexibleservers.active_connections{name:rfprodv2-ba-consumer-db}.rollup(avg, 86400)")]
- **MySQL Active Connections - Peak**: 216 (2026-11-06) [Source: Datadog API → query-metrics("max:azure.dbformysql_flexibleservers.active_connections{name:rfprodv2-ba-consumer-db}.rollup(max, 86400)")]
- **MySQL Slow Queries**: 0.07 per day on average (total 2 over 28 days); worst single day 1 (2026-10-26) [Source: Datadog API → query-metrics("sum:azure.dbformysql_flexibleservers.slow_queries{name:rfprodv2-ba-consumer-db}.as_count().rollup(sum, 86400)")]
- **MySQL Replication Lag - Average**: 0.77 s [Source: Datadog API → query-metrics("avg:azure.dbformysql_flexibleservers.replication_lag{name:rfprodv2-ba-consumer-db-*}.rollup(avg, 86400)")]
- **MySQL Replication Lag - Peak**: 29.00 s (2026-10-24) [Source: Datadog API → query-metrics("max:azure.dbformysql_flexibleservers.replication_lag{name:rfprodv2-ba-consumer-db-*}.rollup(max, 86400)")]
- **MySQL Storage Used**: 190.87 GB at the last bucket (2026-11-19); 28-day growth +14.99 GB [Source: Datadog API → query-metrics("max:azure.dbformysql_flexibleservers.storage_used{name:rfprodv2-ba-consumer-db}.rollup(max, 86400)")]
- **MongoDB Read Latency - Average**: 0.11 ms [Source: Datadog API → query-metrics("avg:mongodb.atlas.oplatencies.reads.avg{clustername:prod-blueapron-main}.rollup(avg, 86400)")]
- **MongoDB Read Latency - Peak**: 128.87 ms [Source: Datadog API → query-metrics("max:mongodb.atlas.oplatencies.reads.avg{clustername:prod-blueapron-main}.rollup(max, 86400)")]
- **MongoDB Write Latency - Average**: 1.46 ms [Source: Datadog API → query-metrics("avg:mongodb.atlas.oplatencies.writes.avg{clustername:prod-blueapron-main}.rollup(avg, 86400)")]
- **MongoDB Connections - Peak**: 317 (2026-11-16) [Source: Datadog API → query-metrics("max:mongodb.atlas.connections.current{clustername:prod-blueapron-main}.rollup(max, 86400)")]
- **MongoDB Query Targeting Ratio (documents scanned per returned)**: 28-day average 1.72; worst day 2090.72 (2026-11-01) [Source: Datadog API → query-metrics("avg:mongodb.atlas.metrics.queryexecutor.scannedperreturned{clustername:prod-blueapron-main}.rollup(avg, 86400)")]
- **MongoDB Database Storage (`ba-auto-ship`, on disk)**: 26.35 GB at the last bucket; 28-day growth -0.04% [Source: Datadog API → query-metrics("avg:mongodb.atlas.dbstats.storage.size{clustername:prod-blueapron-main,databasename:ba-auto-ship} by {databasename}.rollup(max, 86400)")]
- **MongoDB Database Data Size (uncompressed)**: 41.25 GB [Source: Datadog API → query-metrics("avg:mongodb.atlas.dbstats.data.size{clustername:prod-blueapron-main,databasename:ba-auto-ship} by {databasename}.rollup(max, 86400)")]
- **MongoDB Database Index Size**: 8.50 GB [Source: Datadog API → query-metrics("avg:mongodb.atlas.dbstats.index.size{clustername:prod-blueapron-main,databasename:ba-auto-ship} by {databasename}.rollup(max, 86400)")]
- **MongoDB Collections**: 16 [Source: Datadog API → query-metrics("avg:mongodb.atlas.dbstats.collections{clustername:prod-blueapron-main,databasename:ba-auto-ship} by {databasename}.rollup(max, 86400)")]
- **MongoDB Documents**: 44536174 [Source: Datadog API → query-metrics("avg:mongodb.atlas.dbstats.objects{clustername:prod-blueapron-main,databasename:ba-auto-ship} by {databasename}.rollup(max, 86400)")]
- **MongoDB Disk (cluster-wide, shared; fullest member `prod-blueapron-main-shard-00-02.e9cxtw.mongodb.net`)**: 68.88 GB used of 122.90 GB (56.0%) [Source: Datadog API → query-metrics("max:mongodb.atlas.system.disk.space.used{clustername:prod-blueapron-main} by {host}.rollup(max, 86400)")]

## 6. Kafka Consumer Lag
- **Consumer group `ba-auto-ship-service`** (messages; top 8 topics) [Source: Datadog API → query-metrics("top(avg:kafka.consumer_lag{env:prod,consumer_group:ba-auto-ship-service} by {topic}.rollup(avg, 86400), 8, 'mean', 'desc')") and query-metrics("top(max:kafka.consumer_lag{env:prod,consumer_group:ba-auto-ship-service} by {topic}.rollup(max, 86400), 8, 'max', 'desc')")]:
  - `auto-ship-rebalance-event-message`: 28-day average 25.16; peak 1756 (2026-10-27)
  - `ba-auto-ship-auto-assign-cart-message`: 28-day average 0.35; peak 611 (2026-11-19)
  - `ba-auto-ship-generate-order-message`: 28-day average 4.24; peak 305 (2026-10-30)
  - `ba-auto-ship-auth-cart-message`: 28-day average 6.96; peak 304 (2026-10-29)
  - `ba-auto-ship-refresh-delivery-option-message`: 28-day average 0.10; peak 256 (2026-11-17)
  - `auto-ship-fulfillment-event-message`: 28-day average 0.26; peak 116 (2026-11-02)
  - `auto-ship-fulfillment-trigger-message`: 28-day average 0.52; peak 69 (2026-11-19)
  - `customer-updated`: 28-day average < 0.37; peak 60 (2026-11-09)
  - `oms-order-event`: 28-day average 0.29; peak < 60.00
- Snapshot values are the worst topic's 28-day average and the highest peak across the groups above.

## 7. SLO & Monitors
- **Monitors in ALERT or WARN** [Source: Datadog API → GET /api/v1/monitor?group_states=alert,warn&tags=service:ba-auto-ship-service]:
  - [BlueApron][AutoShip] No Enrollments in Past Hour (Active Hours) — No Data
  - [PROD] Alert BA Auto Ship Service - NOT_FOUND Warn — OK
- **SLOs** (attainment over the 28-day window; error budget = (attainment − target) ÷ (100 − target)):
  1. **ba-auto-ship-service Latency SLO** (`0dad129479265fb4ab565ff335d01c77`)
     - Type: metric
     - Target: 98% (30d timeframe)
     - 28-Day Attainment: 99.9912%
     - Error Budget Remaining: 99.5594%
     - Status: compliant
     [Source: Datadog API → GET /api/v1/slo/0dad129479265fb4ab565ff335d01c77/history]
  2. **ba-auto-ship-service Availability SLO** (`86d7ebca02385e7c8760e432b4caaf74`)
     - Type: metric
     - Target: 99.95% (30d timeframe)
     - 28-Day Attainment: 99.9998%
     - Error Budget Remaining: 99.6875%
     - Status: compliant
     [Source: Datadog API → GET /api/v1/slo/86d7ebca02385e7c8760e432b4caaf74/history]

## 8. Infrastructure Live State (Azure MCP)
### MySQL Flexible Server
- **Server Name**: rfprodv2-ba-consumer-db [Source: Azure MCP → mysql_server_config_get(server: rfprodv2-ba-consumer-db, resource-group: RFProdV2, subscription: Wonder-Platform-Prod)]
- **Location**: eastus2 [Source: Azure MCP → mysql_server_config_get(server: rfprodv2-ba-consumer-db, resource-group: RFProdV2, subscription: Wonder-Platform-Prod)]
- **Version**: 8.4 [Source: Azure MCP → mysql_server_config_get(server: rfprodv2-ba-consumer-db, resource-group: RFProdV2, subscription: Wonder-Platform-Prod)]
- **SKU**: Standard_E8ds_v4 [Source: Azure MCP → mysql_server_config_get(server: rfprodv2-ba-consumer-db, resource-group: RFProdV2, subscription: Wonder-Platform-Prod)]
- **StorageSizeGB**: 312 [Source: Azure MCP → mysql_server_config_get(server: rfprodv2-ba-consumer-db, resource-group: RFProdV2, subscription: Wonder-Platform-Prod)]
- **BackupRetentionDays**: 15 [Source: Azure MCP → mysql_server_config_get(server: rfprodv2-ba-consumer-db, resource-group: RFProdV2, subscription: Wonder-Platform-Prod)]
- **GeoRedundantBackup**: Enabled [Source: Azure MCP → mysql_server_config_get(server: rfprodv2-ba-consumer-db, resource-group: RFProdV2, subscription: Wonder-Platform-Prod)]
- **max_connections**: 5461 [Source: Azure MCP → mysql_server_param_get(param: max_connections, server: rfprodv2-ba-consumer-db, resource-group: RFProdV2, subscription: Wonder-Platform-Prod)]
- **slow_query_log**: ON [Source: Azure MCP → mysql_server_param_get(param: slow_query_log, server: rfprodv2-ba-consumer-db, resource-group: RFProdV2, subscription: Wonder-Platform-Prod)]
- **long_query_time**: 10 [Source: Azure MCP → mysql_server_param_get(param: long_query_time, server: rfprodv2-ba-consumer-db, resource-group: Wonder-Platform-Prod)]
- **Replicas**: rfprodv2-ba-consumer-db-replica [Source: Azure MCP → mysql_list(resource-group: RFProdV2, subscription: Wonder-Platform-Prod)]

### AKS Cluster & Node Pool
- **Cluster Name**: RFProdV2AKSCluster [Source: Azure MCP → aks_cluster_get(resource-group: RFProdV2, subscription: Wonder-Platform-Prod)]
- **Node Pool Name**: apppoolv5 [Source: Azure MCP → aks_cluster_get(resource-group: RFProdV2, subscription: Wonder-Platform-Prod)]
- **VM Size**: Standard_D16s_v4 [Source: Azure MCP → aks_cluster_get(resource-group: RFProdV2, subscription: Wonder-Platform-Prod)]
- **Min Count**: 18 [Source: Azure MCP → aks_cluster_get(resource-group: RFProdV2, subscription: Wonder-Platform-Prod)]
- **Max Count**: 42 [Source: Azure MCP → aks_cluster_get(resource-group: RFProdV2, subscription: Wonder-Platform-Prod)]
- **Current Count**: 37 [Source: Azure MCP → aks_cluster_get(resource-group: RFProdV2, subscription: Wonder-Platform-Prod)]
- **Enable Auto Scaling**: true [Source: Azure MCP → aks_cluster_get(resource-group: RFProdV2, subscription: Wonder-Platform-Prod)]
- **Mode**: User [Source: Azure MCP → aks_cluster_get(resource-group: RFProdV2, subscription: Wonder-Platform-Prod)]

## 9. MySQL Table Storage (Top 20)
| Table | Rows | Data (MB) | Index (MB) | Total (MB) |
| :--- | ---: | ---: | ---: | ---: |
| auto_ship_cart_fulfillment_events | 264791 | 30067.02 | 2052.08 | 32119.09 |
| auto_ship_cart_item_ingredients | 8215839 | 3808.11 | 3358.33 | 7166.44 |
| auto_ship_cart_fulfillment_outboxes | 6515318 | 2499.25 | 3050.58 | 5549.83 |
| auto_ship_cart_items | 1851924 | 3769.02 | 1622.47 | 5391.48 |
| auto_ship_carts | 1187793 | 2146.05 | 3070.84 | 5216.89 |
| auto_ship_delivery_addresses | 1243516 | 2724.02 | 2424.67 | 5148.69 |
| simulation_auto_ship_cart_items | 1863265 | 1009.00 | 1011.98 | 2020.98 |
| auto_ship_cart_experiment_results | 3026655 | 1057.33 | 350.98 | 1408.31 |
| auto_ship_cart_skip_reasons | 2356945 | 1044.98 | 312.00 | 1356.98 |
| auto_ship_cart_original_deliveries | 4637429 | 1113.55 | 0.00 | 1113.55 |
| auto_ship_fulfillment_errors | 1070737 | 657.89 | 361.38 | 1019.27 |
| auto_ship_refresh_delivery_option_tasks | 78121 | 747.02 | 18.06 | 765.08 |
| auto_ship_create_cart_tasks | 184509 | 620.02 | 33.16 | 653.17 |
| simulation_auto_ship_customizations | 610295 | 256.00 | 318.00 | 574.00 |
| simulation_auto_ship_carts | 632134 | 248.00 | 320.00 | 568.00 |
| simulation_auto_ship_plans | 671784 | 219.00 | 341.00 | 560.00 |
| auto_ship_auth_cart_tasks | 78197 | 520.02 | 20.14 | 540.16 |
| auto_ship_generate_order_tasks | 77366 | 443.02 | 28.20 | 471.22 |
| auto_ship_delivery_day_shift | 380193 | 185.00 | 77.47 | 262.47 |
| auto_ship_forter_infos | 301958 | 158.00 | 71.52 | 229.52 |

- **Largest table**: `auto_ship_cart_fulfillment_events` at 31.37 GB (264791 rows)
- **Tables above 10 GB**: 1
- **Total of the top 20 tables**: 70.44 GB

[Source: MySQL (rfprodv2-ba-consumer-db/ba_auto_ship) → SELECT table_name, table_rows, ROUND(data_length/1024/1024,2) AS data_mb, ROUND(index_length/1024/1024,2) AS index_mb, ROUND((data_length+index_length)/1024/1024,2) AS total_mb FROM information_schema.tables WHERE table_schema='ba_auto_ship' ORDER BY (data_length+index_length) DESC LIMIT 20]

## 10. Collection Gaps
None

## 11. Metrics Snapshot (JSON)
```json
{
  "avg_rps": 21.1647,
  "peak_rps": 106.3983,
  "wow_rps_growth_pct": -6.66,
  "p50_latency_ms": 4.99,
  "p95_latency_ms": 109.61,
  "p99_latency_ms": 167.72,
  "p99_latency_peak_ms": 184.52,
  "error_rate_pct": 2.0E-4,
  "container_restarts": 0,
  "cpu_avg_cores": 0.0378,
  "cpu_peak_cores": 1.7326,
  "mem_avg_mib": 543.76,
  "mem_peak_mib": 638.5,
  "replicas_avg": 3,
  "replicas_max": 4,
  "kafka_lag_avg": 25.16,
  "kafka_lag_peak": 1756,
  "mysql_connection_avg": 163.6,
  "mysql_connection_peak": 216,
  "mysql_max_connections": 5461,
  "mysql_slow_queries_per_day": 0.07,
  "mysql_replication_lag_avg_s": 0.77,
  "mysql_storage_used_gb": 190.87,
  "mongo_read_latency_ms": 0.11,
  "mongo_read_latency_peak_ms": 128.87,
  "mongo_write_latency_ms": 1.46,
  "mongo_connections_peak": 317,
  "mongo_targeting_ratio_avg": 1.72,
  "mongo_targeting_ratio_peak": 2090.72,
  "mongo_database": "ba-auto-ship",
  "mongo_db_storage_gb": 26.35,
  "mongo_db_storage_growth_pct": -0.04,
  "mongo_db_data_size_gb": 41.25,
  "mongo_db_index_size_gb": 8.5,
  "mongo_collections": 16,
  "mongo_documents": 44536174,
  "mongo_disk_used_gb": 68.88,
  "mongo_disk_used_pct": 56,
  "largest_table_gb": 31.37,
  "slo_count": 2,
  "slo_breached_count": 0,
  "slo_min_error_budget_remaining_pct": 99.5594
}
```
