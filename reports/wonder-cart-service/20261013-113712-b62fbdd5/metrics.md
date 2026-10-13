# Collected Metrics — wonder-cart-service (uat)
**Collected At**: 2026-10-13 11:45 UTC
**Lookback**: 28 days (2026-09-15 00:00:00 to 2026-10-13 00:00:00, UTC day-aligned)

## 1. Traffic & Throughput
- **Average RPS**: 0.38
- **Peak RPS**: 0.42 [Source: Datadog MCP → query_metrics("sum:trace.undertow_http.request.hits{...}.rollup(sum, 3600)")] (2026-09-24 15:00 UTC)
- **Week-over-week average RPS growth**: -20.36%

| Date Range | Total Hits |
| :--- | :--- |
| 2026-09-15 to 2026-09-21 | 242100 |
| 2026-09-22 to 2026-09-28 | 242186 |
| 2026-09-29 to 2026-10-05 | 241990 |
| 2026-10-06 to 2026-10-12 | 192713 |

## 2. Latency Profile
- **P50 Average**: 0.25 ms
- **P95 Average**: 0.49 ms
- **P99 Average**: 0.77 ms
- **Peak P99**: 1.66 ms (2026-09-30 00:00 UTC)

## 3. Error Rates & Stability
- **HTTP error rate, 28-day average**: 0.0%
- **HTTP error rate, worst single day**: 0.0% (2026-09-15)
- **Container restarts**: 0

## 4. Resource Saturation
- **CPU average**: 0.013 cores
- **CPU peak**: 0.482 cores (2026-09-30 00:00 UTC)
- **Memory average**: 488.2 MiB
- **Memory peak**: 512.9 MiB (2026-09-15 00:00 UTC)
- **Replicas average**: 2.0
- **Replicas max**: 3.0

## 5. Persistence Layer
- **Active connections (avg)**: 699.2
- **Active connections (peak)**: 1089 (2026-09-30 00:00 UTC)
- **Slow queries (daily avg)**: 0.61
- **Slow queries (worst day)**: 40 (2026-09-20 00:00 UTC)
- **Replication lag (avg)**: 236.8 seconds
- **Replication lag (peak)**: 480 seconds (2026-10-06 00:00 UTC)
- **Storage used**: 485.7 GB

## 6. Kafka Consumer Lag
- **courier-supply-health-trigger-periodically-message**: avg lag 0.25, peak 1 (2026-09-20 00:00 UTC)
- **wonder-configuration-updated**: avg lag 0.04, peak 1 (2026-09-17 00:00 UTC)
- **consumer-clean-data**: avg lag 0, peak 0
- **corporate-order-client-changed**: avg lag 0, peak 0
- **courier-supply-health-trigger-message**: avg lag 24.36, peak 228 (2026-09-22 00:00 UTC)

## 7. SLO & Monitors
None in ALERT or WARN

## 8. Infrastructure Live State (Azure MCP)
- **MySQL SKU**: NOT_COLLECTED: Azure MCP tools not available
- **MySQL Version**: NOT_COLLECTED: Azure MCP tools not available
- **Provisioned Storage**: NOT_COLLECTED: Azure MCP tools not available
- **AKS Pool**: NOT_COLLECTED: Azure MCP tools not available

## 9. MySQL Table Storage (Top 20)
| Table | Rows | Data (MB) | Index (MB) | Total (MB) |
| :--- | ---: | ---: | ---: | ---: |
| pop_cart_items | 8431 | 5.02 | 4.09 | 9.11 |
| cart_items | 5290 | 5.02 | 3.03 | 8.05 |
| cart_menu_item_options | 5026 | 4.02 | 3.05 | 7.06 |
| pop_cart_menu_item_options | 7653 | 4.02 | 2.17 | 6.19 |
| web_cart_menu_item_options | 1121 | 2.02 | 2.17 | 4.19 |
| web_cart_items | 1220 | 2.02 | 2.03 | 4.05 |
| pop_carts | 6254 | 2.03 | 1.36 | 3.39 |
| carts | 3473 | 2.02 | 1.02 | 3.03 |
| cart_bundle_item_choices | 928 | 1.02 | 1.22 | 2.23 |
| cart_bundle_item_choice_options | 474 | 1.02 | 0.19 | 1.20 |
| web_carts | 1167 | 1.02 | 0.13 | 1.14 |
| cart_carry_over_histories | 666 | 1.02 | 0.00 | 1.02 |
| web_cart_bundle_item_choice_options | 233 | 0.14 | 0.14 | 0.28 |
| web_cart_bundle_item_choices | 124 | 0.09 | 0.05 | 0.14 |
| pop_cart_bundle_item_choices | 0 | 0.02 | 0.03 | 0.05 |
| pop_cart_bundle_item_choice_options | 0 | 0.02 | 0.03 | 0.05 |
| flyway_schema_history | 110 | 0.02 | 0.02 | 0.03 |
| reorder_cart_created_records | 4 | 0.02 | 0.02 | 0.03 |

- **Largest table**: `pop_cart_items` at 0.01 GB (8431 rows)
- **Tables above 10 GB**: 0
- **Total of the top 18 tables**: 0.05 GB

[Source: MySQL (ftiuat-flexible-consumer-db/wonder_cart) → SELECT table_name, table_rows, ROUND(data_length/1024/1024,2) AS data_mb, ROUND(index_length/1024/1024,2) AS index_mb, ROUND((data_length+index_length)/1024/1024,2) AS total_mb FROM information_schema.tables WHERE table_schema='wonder_cart' ORDER BY (data_length+index_length) DESC LIMIT 20]

## 10. Collection Gaps
- Azure MySQL configuration: NOT_COLLECTED: Azure MCP tools not available
- Azure AKS cluster configuration: NOT_COLLECTED: Azure MCP tools not available
- MySQL max connections: NOT_COLLECTED: Azure MCP tools not available

## 11. Metrics Snapshot (JSON)
```json
{
  "avg_rps": 0.38,
  "peak_rps": 0.42,
  "wow_rps_growth_pct": -20.36,
  "p50_latency_ms": 0.25,
  "p95_latency_ms": 0.49,
  "p99_latency_ms": 0.77,
  "p99_latency_peak_ms": 1.66,
  "error_rate_pct": 0.0,
  "container_restarts": 0,
  "cpu_avg_cores": 0.013,
  "cpu_peak_cores": 0.482,
  "mem_avg_mib": 488.2,
  "mem_peak_mib": 512.9,
  "replicas_avg": 2.0,
  "replicas_max": 3.0,
  "kafka_lag_avg": 24.36,
  "kafka_lag_peak": 228.0,
  "mysql_connection_avg": 699.2,
  "mysql_connection_peak": 1089.0,
  "mysql_max_connections": null,
  "mysql_slow_queries_per_day": 0.61,
  "mysql_replication_lag_avg_s": 236.8,
  "mysql_storage_used_gb": 485.7,
  "mongo_read_latency_ms": null,
  "mongo_write_latency_ms": null,
  "largest_table_gb": 0.01
}
```
