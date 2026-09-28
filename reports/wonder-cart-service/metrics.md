# Collected Metrics — wonder-cart-service (uat)
**Collected At**: 2026-09-28 00:00 UTC
**Lookback**: 28 days (2026-08-31 to 2026-09-28, UTC day-aligned)

## 1. Traffic & Throughput
- **Average RPS**: 0.4003
- **Peak RPS**: 0.4078 (2026-09-17 07:00 UTC)
- **Week-over-week average RPS growth**: -0.0388%
- **Weekly volumes**:
  - 2026-08-31 to 2026-09-06: 242100
  - 2026-09-07 to 2026-09-13: 242061
  - 2026-09-14 to 2026-09-20: 242185
  - 2026-09-21 to 2026-09-27: 242091

## 2. Latency Profile
- **P50 average**: 0.2725 ms
- **P95 average**: 0.5516 ms
- **P99 average**: 0.8844 ms
- **Peak P99**: 1.7350 ms (2026-09-16 00:00 UTC)

## 3. Error Rates & Stability
- **HTTP error rate, 28-day average**: 0.0%
- **HTTP error rate, worst single day**: 0.0% (2026-08-31)
- **Container restarts**: 0 (Worst day: N/A)

## 4. Resource Saturation
- **CPU average**: 0.0131 cores
- **CPU peak**: 1.0439 cores (2026-09-05 00:00 UTC)
- **Memory average**: 508.35 MiB
- **Memory peak**: 634.15 MiB (2026-09-03 00:00 UTC)
- **Replicas average**: 2.00
- **Replicas peak**: 3 (2026-09-06 00:00 UTC)

## 5. Persistence Layer
- **Active connections average**: 795.39
- **Active connections peak**: 1372 (2026-09-23 00:00 UTC)
- **Slow queries per day**: 15.39
- **Replication lag average**: 327.93 s
- **Replication lag peak**: 1735 s (2026-09-08 00:00 UTC)

## 6. Kafka Consumer Lag
- **wonder-cart-service (courier-supply-health-trigger-periodically-message)**: Avg lag 0.25, Peak lag 1 (2026-08-31)
- **wonder-cart-service (wonder-configuration-updated)**: Avg lag 0.04, Peak lag 1 (2026-09-06)
- **wonder-cart-service (courier-supply-health-trigger-message)**: Avg lag 1.57, Peak lag 10 (2026-09-17)
- **wonder-cart-service (consumer-clean-data)**: Avg lag 0.0, Peak lag 0
- **wonder-cart-service (corporate-order-client-changed)**: Avg lag 0.07, Peak lag 1 (2026-09-06)

## 7. SLO & Monitors
None in ALERT or WARN

## 8. Infrastructure Live State (Azure MCP)
- **MySQL SKU**: Standard_D8ds_v4
- **MySQL Version**: 8.4
- **MySQL Provisioned Storage**: 526 GB
- **MySQL Used Storage**: 490.99 GB
- **AKS Cluster Name**: FTIUatAKSClusterV2
- **AKS Application Node Pool**: apppoolv5 (User-mode, Standard_D16s_v4, Max: 24)

## 9. MySQL Table Storage (Top 20)
| Table | Rows | Data (MB) | Index (MB) | Total (MB) |
| :--- | ---: | ---: | ---: | ---: |
| pop_cart_items | 8265 | 5.02 | 4.09 | 9.11 |
| cart_items | 4944 | 5.02 | 4.03 | 9.05 |
| cart_menu_item_options | 4946 | 4.02 | 3.27 | 7.28 |
| pop_cart_menu_item_options | 7115 | 4.02 | 2.17 | 6.19 |
| web_cart_menu_item_options | 1673 | 3.02 | 2.23 | 5.25 |
| web_cart_items | 1411 | 2.02 | 2.03 | 4.05 |
| carts | 3158 | 3.02 | 1.02 | 4.03 |
| pop_carts | 5964 | 2.03 | 1.36 | 3.39 |
| cart_bundle_item_choices | 1024 | 1.02 | 1.27 | 2.28 |
| cart_bundle_item_choice_options | 526 | 1.02 | 0.23 | 1.25 |
| web_carts | 1307 | 1.02 | 0.14 | 1.16 |
| cart_carry_over_histories | 300 | 1.02 | 0.00 | 1.02 |
| web_cart_bundle_item_choice_options | 238 | 0.16 | 0.16 | 0.31 |
| web_cart_bundle_item_choices | 159 | 0.08 | 0.11 | 0.19 |
| pop_cart_bundle_item_choices | 0 | 0.02 | 0.03 | 0.05 |
| pop_cart_bundle_item_choice_options | 0 | 0.02 | 0.03 | 0.05 |
| flyway_schema_history | 110 | 0.02 | 0.02 | 0.03 |
| reorder_cart_created_records | 4 | 0.02 | 0.02 | 0.03 |

- **Largest table**: `pop_cart_items` at 0.01 GB (8265 rows)
- **Tables above 10 GB**: 0
- **Total of the top 18 tables**: 0.05 GB

[Source: MySQL (ftiuat-flexible-consumer-db/wonder_cart) → SELECT table_name, table_rows, ROUND(data_length/1024/1024,2) AS data_mb, ROUND(index_length/1024/1024,2) AS index_mb, ROUND((data_length+index_length)/1024/1024,2) AS total_mb FROM information_schema.tables WHERE table_schema='wonder_cart' ORDER BY (data_length+index_length) DESC LIMIT 20]

## 10. Collection Gaps
None

## 11. Metrics Snapshot (JSON)
```json
{
  "avg_rps": 0.4003,
  "peak_rps": 0.4078,
  "wow_rps_growth_pct": -0.0388,
  "p50_latency_ms": 0.2725,
  "p95_latency_ms": 0.5516,
  "p99_latency_ms": 0.8844,
  "p99_latency_peak_ms": 1.7350,
  "error_rate_pct": 0.0,
  "container_restarts": 0,
  "cpu_avg_cores": 0.0131,
  "cpu_peak_cores": 1.0439,
  "mem_avg_mib": 508.35,
  "mem_peak_mib": 634.15,
  "replicas_avg": 2.0,
  "replicas_max": 3.0,
  "kafka_lag_avg": 0.372,
  "kafka_lag_peak": 10.0,
  "mysql_connection_avg": 795.39,
  "mysql_connection_peak": 1372.0,
  "mysql_max_connections": 2731.0,
  "mysql_slow_queries_per_day": 15.39,
  "mysql_replication_lag_avg_s": 327.93,
  "mysql_storage_used_gb": 490.99,
  "mongo_read_latency_ms": null,
  "mongo_write_latency_ms": null,
  "largest_table_gb": 0.01
}
```
