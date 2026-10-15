# Collected Metrics — order-service (prod)
**Collected At**: 2026-10-15 10:30 UTC
**Lookback**: 28 days (2025-09-17 to 2025-10-15, UTC day-aligned)

## 1. Traffic & Throughput
- **Average RPS**: 65.5542
- **Peak RPS**: 251.54 RPS (1,027,998 hits in peak hourly bucket starting at 2025-10-09 14:00 UTC) [Source: Datadog MCP → query_metrics("sum:trace.undertow_http.request.hits{env:prod,service:order-service}.as_count().rollup(sum, 3600)")]
- **Week-over-Week Average RPS Growth**: +4.21%
- **Weekly Volumes (4 blocks of 7 days)**:
  - 2025-09-17 to 2025-09-23: 40,845,417 hits [Source: Datadog MCP → query_metrics("sum:trace.undertow_http.request.hits{env:prod,service:order-service}.as_count().rollup(sum, 86400)")]
  - 2025-09-24 to 2025-09-30: 39,266,419 hits [Source: Datadog MCP → query_metrics("sum:trace.undertow_http.request.hits{env:prod,service:order-service}.as_count().rollup(sum, 86400)")]
  - 2025-10-01 to 2025-10-07: 38,421,123 hits [Source: Datadog MCP → query_metrics("sum:trace.undertow_http.request.hits{env:prod,service:order-service}.as_count().rollup(sum, 86400)")]
  - 2025-10-08 to 2025-10-14: 40,039,475 hits [Source: Datadog MCP → query_metrics("sum:trace.undertow_http.request.hits{env:prod,service:order-service}.as_count().rollup(sum, 86400)")]

## 2. Latency Profile
- **P50 Latency (28-day average)**: 2.01 ms [Source: Datadog MCP → query_metrics("p50:trace.undertow_http.request{env:prod,service:order-service}.rollup(avg, 86400)")]
- **P95 Latency (28-day average)**: 48.42 ms [Source: Datadog MCP → query_metrics("p95:trace.undertow_http.request{env:prod,service:order-service}.rollup(avg, 86400)")]
- **P99 Latency (28-day average)**: 151.78 ms [Source: Datadog MCP → query_metrics("p99:trace.undertow_http.request{env:prod,service:order-service}.rollup(avg, 86400)")]
- **Peak P99 Latency**: 193.31 ms on 2025-10-06 00:00 UTC [Source: Datadog MCP → query_metrics("p99:trace.undertow_http.request{env:prod,service:order-service}.rollup(max, 86400)")]

## 3. Error Rates & Stability
- **HTTP Error Rate (28-day average)**: 0.000030% (47 errors / 158,588,506 hits) [Source: Datadog MCP → query_metrics("sum:trace.undertow_http.request.errors{env:prod,service:order-service}.as_count().rollup(sum, 86400)")]
- **HTTP Error Rate (Worst single day)**: 0.000474% (26 errors on 2025-10-06) [Source: Datadog MCP → query_metrics("sum:trace.undertow_http.request.errors{env:prod,service:order-service}.as_count().rollup(sum, 86400)")]
- **Container Restarts**: 0 total restarts across the 28-day window [Source: Datadog MCP → query_metrics("sum:kubernetes.containers.restarts{env:prod,service:order-service}.rollup(sum, 86400)")]

## 4. Resource Saturation
- **CPU Usage (Average)**: 0.1055 cores (105,483,894 nanocores) [Source: Datadog MCP → query_metrics("avg:kubernetes.cpu.usage.total{env:prod,service:order-service}.rollup(avg, 86400)")]
- **CPU Usage (Peak)**: 2.3368 cores (2,336,802,553 nanocores) on 2025-10-07 00:00 UTC [Source: Datadog MCP → query_metrics("max:kubernetes.cpu.usage.total{env:prod,service:order-service}.rollup(max, 86400)")]
- **Memory Working Set (Average)**: 754.21 MiB (790,821,342 bytes) [Source: Datadog MCP → query_metrics("avg:kubernetes.memory.working_set{env:prod,service:order-service}.rollup(avg, 86400)")]
- **Memory Working Set (Peak)**: 911.69 MiB (955,981,824 bytes) on 2025-09-21 00:00 UTC [Source: Datadog MCP → query_metrics("max:kubernetes.memory.working_set{env:prod,service:order-service}.rollup(max, 86400)")]
- **Replicas (Average)**: 3.00 [Source: Datadog MCP → query_metrics("avg:kubernetes_state.deployment.replicas{env:prod,kube_deployment:order-service}.rollup(avg, 86400)")]
- **Replicas (Maximum)**: 4 [Source: Datadog MCP → query_metrics("max:kubernetes_state.deployment.replicas{env:prod,kube_deployment:order-service}.rollup(max, 86400)")]

## 5. Persistence Layer
### MySQL (`rfprodv2-flexible-wonder-db`)
- **Active Connections (28-day average)**: 928.91 [Source: Datadog MCP → query_metrics("avg:azure.dbformysql_flexibleservers.active_connections{name:rfprodv2-flexible-wonder-db}.rollup(avg, 86400)")]
- **Active Connections (Peak)**: 1,169 on 2025-10-08 00:00 UTC [Source: Datadog MCP → query_metrics("max:azure.dbformysql_flexibleservers.active_connections{name:rfprodv2-flexible-wonder-db}.rollup(max, 86400)")]
- **Max Connections**: NOT_COLLECTED: Azure MCP error — CredentialUnavailableException (refresh token expired)
- **Slow Queries (Daily average)**: 689.50 queries/day (19,306 total over 28 days) [Source: Datadog MCP → query_metrics("sum:azure.dbformysql_flexibleservers.slow_queries{name:rfprodv2-flexible-wonder-db}.as_count().rollup(sum, 86400)")]
- **Slow Queries (Worst single day)**: 880 queries on 2025-10-10 [Source: Datadog MCP → query_metrics("sum:azure.dbformysql_flexibleservers.slow_queries{name:rfprodv2-flexible-wonder-db}.as_count().rollup(sum, 86400)")]
- **Replication Lag (28-day average)**: 326.68 seconds [Source: Datadog MCP → query_metrics("max:azure.dbformysql_flexibleservers.replication_lag{name:rfprodv2-flexible-wonder-db-*}.rollup(max, 86400)")]
- **Replication Lag (Peak)**: 397 seconds on 2025-09-21 00:00 UTC [Source: Datadog MCP → query_metrics("max:azure.dbformysql_flexibleservers.replication_lag{name:rfprodv2-flexible-wonder-db-*}.rollup(max, 86400)")]
- **Storage Used**: NOT_COLLECTED: storage_used unit unexpected — raw value 1702900107209 (exceeds provisioned storage) [Source: Datadog MCP → query_metrics("max:azure.dbformysql_flexibleservers.storage_used{name:rfprodv2-flexible-wonder-db}.rollup(max, 86400)")]

### MongoDB Atlas (`prod-oms-main`)
- **Read Operation Latency (28-day average)**: 0.42 ms [Source: Datadog MCP → query_metrics("avg:mongodb.atlas.oplatencies.reads.avg{clustername:prod-oms-main}.rollup(avg, 86400)")]
- **Read Operation Latency (Peak)**: 254.11 ms on 2025-10-03 00:00 UTC [Source: Datadog MCP → query_metrics("max:mongodb.atlas.oplatencies.reads.avg{clustername:prod-oms-main}.rollup(max, 86400)")]
- **Write Operation Latency (28-day average)**: 2.28 ms [Source: Datadog MCP → query_metrics("avg:mongodb.atlas.oplatencies.writes.avg{clustername:prod-oms-main}.rollup(avg, 86400)")]
- **Connections (Peak)**: 172 connections [Source: Datadog MCP → query_metrics("max:mongodb.atlas.connections.current{clustername:prod-oms-main}.rollup(max, 86400)")]
- **Query Targeting Ratio (28-day average)**: 0.23 scanned/returned [Source: Datadog MCP → query_metrics("avg:mongodb.atlas.metrics.queryexecutor.scannedperreturned{clustername:prod-oms-main}.rollup(avg, 86400)")]
- **Query Targeting Ratio (Worst day)**: 60,975.63 scanned/returned on 2025-10-04 00:00 UTC [Source: Datadog MCP → query_metrics("max:mongodb.atlas.metrics.queryexecutor.scannedperreturned{clustername:prod-oms-main}.rollup(max, 86400)")]
- **Data Size**: 19.36 GB (last bucket) [Source: Datadog MCP → query_metrics("avg:mongodb.atlas.dbstats.data.size{clustername:prod-oms-main}.rollup(max, 86400)")]
- **Index Size**: 0.37 GB (last bucket) [Source: Datadog MCP → query_metrics("avg:mongodb.atlas.dbstats.index.size{clustername:prod-oms-main}.rollup(max, 86400)")]
- **Collections Count**: 5.64 [Source: Datadog MCP → query_metrics("avg:mongodb.atlas.dbstats.collections{clustername:prod-oms-main}.rollup(max, 86400)")]
- **Documents Count**: 3,047,446 [Source: Datadog MCP → query_metrics("avg:mongodb.atlas.dbstats.objects{clustername:prod-oms-main}.rollup(max, 86400)")]
- **Data Growth (28-day)**: +1.35% (last minus first) [Source: Datadog MCP → query_metrics("avg:mongodb.atlas.dbstats.data.size{clustername:prod-oms-main}.rollup(max, 86400)")]

## 6. Kafka Consumer Lag
Consumer Group: `order-service`
- **blue-apron-fulfillment-package-event**: Average 21.3 messages, Peak 62 messages on 2025-09-24 [Source: Datadog MCP → query_metrics("max:kafka.consumer_lag{env:prod,consumer_group:order-service} by {topic}.rollup(max, 86400)")]
- **vertex-report-event**: Average 15.6 messages, Peak 50 messages on 2025-09-20 [Source: Datadog MCP → query_metrics("max:kafka.consumer_lag{env:prod,consumer_group:order-service} by {topic}.rollup(max, 86400)")]
- **order-runner-scan-summary-event**: Average 2.3 messages, Peak 3 messages [Source: Datadog MCP → query_metrics("max:kafka.consumer_lag{env:prod,consumer_group:order-service} by {topic}.rollup(max, 86400)")]
- **hdr-fulfillment-status-updated**: Average 8.1 messages, Peak 19 messages on 2025-10-06 [Source: Datadog MCP → query_metrics("max:kafka.consumer_lag{env:prod,consumer_group:order-service} by {topic}.rollup(max, 86400)")]
- **hdr-fulfillment-eta-updated**: Average 7.5 messages, Peak 26 messages on 2025-10-06 [Source: Datadog MCP → query_metrics("max:kafka.consumer_lag{env:prod,consumer_group:order-service} by {topic}.rollup(max, 86400)")]
- **deli-delivery-task-v2-updated**: Average 5.0 messages, Peak 9 messages [Source: Datadog MCP → query_metrics("max:kafka.consumer_lag{env:prod,consumer_group:order-service} by {topic}.rollup(max, 86400)")]
- Other topics (`alias-user-converted-message`, `in-person-payment-charge-event`, `order-payment-retry-capture-task-job`, `save-blue-apron-order-rating-message`, `order-rating-submit`, `fr-fulfillment-request-retry-event`, `marked-component-as-oos-event`, `post-order-delayed-task`, `check-yesterday-non-final-status-orders`, `ba-sous-chef-recipe-combination-pdf-responses`, `ba-cycle-item-sku-swap-message`, `blue-apron-fulfillment-event`, `order-3d-secure-check-task`, `corporate-order-client-changed`, `grubhub-order-event-message`, `appliance-failure-impacted-orders-event`): Average < 1 message, Peak <= 5 messages.

## 7. SLO & Monitors
- **Datadog Monitors**: None in ALERT or WARN [Source: Datadog MCP → get_monitors()]
- **SLOs**: NOT_COLLECTED: the configured Datadog MCP server exposes no SLO tool [Source: Datadog MCP]

## 8. Infrastructure Live State (Azure MCP)
- **MySQL Server SKU / Version / Provisioned Storage**: NOT_COLLECTED: Azure MCP error — CredentialUnavailableException: refresh token expired [Source: Azure MCP → mysql]
- **AKS Node Pool**: NOT_COLLECTED: Azure MCP error — CredentialUnavailableException: refresh token expired [Source: Azure MCP → aks]

## 9. MySQL Table Storage (Top 20)
| Table | Rows | Data (MB) | Index (MB) | Total (MB) |
| :--- | ---: | ---: | ---: | ---: |
| order_timelines_v2 | 183877761 | 188788.58 | 70914.16 | 259702.73 |
| order_item_options | 70119270 | 63477.72 | 24840.89 | 88318.61 |
| order_items | 59370549 | 48512.52 | 18126.88 | 66639.39 |
| order_item_components | 75013591 | 29440.84 | 22898.95 | 52339.80 |
| order_status_tracking | 113593554 | 25121.69 | 25255.00 | 50376.69 |
| order_item_ingredients | 90852504 | 31384.91 | 18020.95 | 49405.86 |
| order_charge_items | 59043166 | 32157.83 | 5859.00 | 38016.83 |
| order_item_option_value_components | 51296258 | 18969.73 | 18967.88 | 37937.61 |
| orders | 18592406 | 11748.36 | 16069.69 | 27818.05 |
| order_restaurants | 21293723 | 15717.86 | 7458.91 | 23176.77 |
| order_configs | 19167237 | 17202.83 | 0.00 | 17202.83 |
| order_payments | 22571173 | 8062.13 | 7729.95 | 15792.08 |
| order_operation_version_tracks | 46218612 | 9134.00 | 4387.00 | 13521.00 |
| order_customers | 17554005 | 5968.67 | 4842.97 | 10811.64 |
| order_charge_item_taxable_extension | 38622723 | 10563.02 | 0.00 | 10563.02 |
| order_addresses | 8897561 | 5640.59 | 4806.86 | 10447.45 |
| order_payment_transactions | 21131917 | 7192.00 | 2608.94 | 9800.94 |
| order_charges | 18474062 | 8409.63 | 1087.00 | 9496.63 |
| order_hdr_addresses | 11636809 | 5228.95 | 2654.95 | 7883.91 |
| order_bundle_item_choices | 7641834 | 4284.84 | 2395.95 | 6680.80 |

- **Largest table**: `order_timelines_v2` at 253.62 GB (183877761 rows)
- **Tables above 10 GB**: 16
- **Total of the top 20 tables**: 787.04 GB

[Source: MySQL (rfprodv2-flexible-wonder-db/order) → SELECT table_name, table_rows, ROUND(data_length/1024/1024,2) AS data_mb, ROUND(index_length/1024/1024,2) AS index_mb, ROUND((data_length+index_length)/1024/1024,2) AS total_mb FROM information_schema.tables WHERE table_schema='order' ORDER BY (data_length+index_length) DESC LIMIT 20]

## 10. Collection Gaps
- `mysql_max_connections`: NOT_COLLECTED: Azure MCP error — CredentialUnavailableException: refresh token expired [Source: Azure MCP → mysql]
- `mysql_storage_used_gb`: NOT_COLLECTED: storage_used unit unexpected — raw value 1702900107209 (exceeds provisioned storage) [Source: Datadog MCP → query_metrics]
- `slo`: NOT_COLLECTED: the configured Datadog MCP server exposes no SLO tool [Source: Datadog MCP]
- `azure_infrastructure_live_state`: NOT_COLLECTED: Azure MCP error — CredentialUnavailableException: refresh token expired [Source: Azure MCP]

## 11. Metrics Snapshot (JSON)
```json
{
  "avg_rps": 65.5542,
  "peak_rps": 251.54,
  "wow_rps_growth_pct": 4.21,
  "p50_latency_ms": 2.01,
  "p95_latency_ms": 48.42,
  "p99_latency_ms": 151.78,
  "p99_latency_peak_ms": 193.31,
  "error_rate_pct": 0.000030,
  "container_restarts": 0,
  "cpu_avg_cores": 0.1055,
  "cpu_peak_cores": 2.3368,
  "mem_avg_mib": 754.21,
  "mem_peak_mib": 911.69,
  "replicas_avg": 3.00,
  "replicas_max": 4,
  "kafka_lag_avg": 21.3,
  "kafka_lag_peak": 62,
  "mysql_connection_avg": 928.91,
  "mysql_connection_peak": 1169,
  "mysql_max_connections": null,
  "mysql_slow_queries_per_day": 689.50,
  "mysql_replication_lag_avg_s": 326.68,
  "mysql_storage_used_gb": null,
  "mongo_read_latency_ms": 0.42,
  "mongo_read_latency_peak_ms": 254.11,
  "mongo_write_latency_ms": 2.28,
  "mongo_connections_peak": 172,
  "mongo_targeting_ratio_avg": 0.23,
  "mongo_targeting_ratio_peak": 60975.63,
  "mongo_data_size_gb": 19.36,
  "mongo_index_size_gb": 0.37,
  "mongo_collections": 5.64,
  "mongo_documents": 3047446,
  "mongo_data_growth_pct": 1.35,
  "largest_table_gb": 253.62
}
```
