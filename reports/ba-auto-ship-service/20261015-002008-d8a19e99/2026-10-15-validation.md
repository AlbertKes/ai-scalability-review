# Validation Report: ba-auto-ship-service (prod)
**Original Report**: `/Users/albertke/IdeaProjects/03Wonder/ai-scalability-review/reports/ba-auto-ship-service/20261015-002008-d8a19e99/2026-10-15-review.md`
**Original Report Date**: 2026-10-15
**Validation Run Date**: 2026-10-15
**Validator**: gemini-3.5-flash

---

## Validation Summary

| # | Check | Status | Issues |
|---|-------|--------|--------|
| 1 | Config Accuracy | FAIL | 2 issue(s) |
| 2 | Metric Numerical Fidelity | FAIL | 2 issue(s) |
| 3 | Cross-Source Metric Consistency | PASS | 0 issue(s) |
| 4 | Scoring Accuracy | PASS | 0 issue(s) |
| 5 | Capacity Projection Math | PASS | 0 issue(s) |
| 6 | NOT_COLLECTED Authenticity | PASS | 0 issue(s) |
| 7 | Format & Attribution Completeness | PASS | 0 issue(s) |
| 8 | Recommendation Soundness | FAIL | 5 issue(s) |

---

## Check 1 — Config Accuracy: FAIL

### Findings

**[PASS]** AKS Node Pool (apppoolv5)
  - Reported: `apppoolv5` (`Standard_D16s_v4`: 16 vCPU, 64GB RAM)
  - Actual: `apppoolv5` (`Standard_D16s_v4`: 16 vCPU, 64GB RAM) [Source: Azure MCP → aks / aks_cluster_get(cluster=RFProdV2AKSCluster, subscription=3c275d02-525c-4e0d-b6e9-e45ae88f113b)]
  - Detail: Exact match. Node pool vmSize is `Standard_D16s_v4`.

**[PASS]** AKS Scaling (apppoolv5)
  - Reported: `Min 18 / Max 42 nodes`
  - Actual: `Min 18 / Max 42 nodes` (enableAutoScaling: true, minCount: 18, maxCount: 42) [Source: Azure MCP → aks / aks_cluster_get(cluster=RFProdV2AKSCluster, subscription=3c275d02-525c-4e0d-b6e9-e45ae88f113b)]
  - Detail: Exact match with live cluster and Terraform manifest `infra/prod/app/infra/env/12-aks.tf:L134-135`.

**[PASS]** K8s Pod Requests (ba-auto-ship-service)
  - Reported: `CPU: 2`
  - Actual: `cpu: 2` [Source: code → infra/prod/app/baconsumer/kube/resource/18-ba-auto-ship-service.yml:L78]
  - Detail: Exact match.

**[PASS]** K8s Pod Limits (ba-auto-ship-service)
  - Reported: `CPU: 4`
  - Actual: `cpu: 4` [Source: code → infra/prod/app/baconsumer/kube/resource/18-ba-auto-ship-service.yml:L81]
  - Detail: Exact match.

**[PASS]** K8s Memory Requests (ba-auto-ship-service)
  - Reported: `Memory: 1.5Gi`
  - Actual: `memory: 1.5Gi` [Source: code → infra/prod/app/baconsumer/kube/resource/18-ba-auto-ship-service.yml:L79]
  - Detail: Exact match.

**[PASS]** K8s Memory Limits (ba-auto-ship-service)
  - Reported: `Memory: 3Gi`
  - Actual: `memory: 3Gi` [Source: code → infra/prod/app/baconsumer/kube/resource/18-ba-auto-ship-service.yml:L82]
  - Detail: Exact match.

**[PASS]** HPA Settings (ba-auto-ship-service)
  - Reported: `Min 3 / Max 5 (Target: cpu averageUtilization 80%)`
  - Actual: `minReplicas: 3, maxReplicas: 5, target.averageUtilization: 80` [Source: code → infra/prod/app/baconsumer/kube/resource/110-hpa.yml:L12-19]
  - Detail: Exact match.

**[PASS]** MySQL Host SKU & Sizing (rfprodv2-ba-consumer-db)
  - Reported: `rfprodv2-ba-consumer-db` (`Standard_E8ds_v4`: 8 vCPU, 64GB RAM)
  - Actual: `rfprodv2-ba-consumer-db` (`Standard_E8ds_v4`: 8 vCPU, 64GB RAM) [Source: Azure MCP → mysql / mysql_server_config_get(server=rfprodv2-ba-consumer-db, resource-group=RFProdV2)]
  - Detail: Exact match. Server SKU is `Standard_E8ds_v4`.

**[FAIL]** MySQL High Availability Mode (rfprodv2-ba-consumer-db)
  - Reported: `8.4 (HA: ZoneRedundant)`
  - Actual: `8.4 (HA: SameZone)` [Source: Azure MCP → arm / execute_query("resources | where type =~ 'microsoft.dbformysql/flexibleservers' and name =~ 'rfprodv2-ba-consumer-db' | project properties.highAvailability")]
  - Detail: Static config mismatch. The report claims High Availability mode is `ZoneRedundant`, but ARM resource properties confirm `mode: "SameZone"` (`standbyAvailabilityZone: "1"`). Exact match is required for static HA mode.

**[PASS]** MySQL Storage & IO Scaling (rfprodv2-ba-consumer-db)
  - Reported: `312GB (Auto-grow: ON, IO Scaling: ON)`
  - Actual: `312GB (Auto-grow: Enabled, IO Scaling: Enabled)` [Source: Azure MCP → mysql / mysql_server_config_get(server=rfprodv2-ba-consumer-db) & arm / execute_query("resources | where type =~ 'microsoft.dbformysql/flexibleservers' and name =~ 'rfprodv2-ba-consumer-db' | project properties.storage")]
  - Detail: Exact match. `storageSizeGB: 312`, `autoGrow: "Enabled"`, `autoIoScaling: "Enabled"`.

**[FAIL]** MySQL Read Replica High Availability
  - Reported: `rfprodv2-ba-consumer-db-*` (HA: ON)
  - Actual: `rfprodv2-ba-consumer-db-replica` (HA: Disabled) [Source: Azure MCP → arm / execute_query("resources | where type =~ 'microsoft.dbformysql/flexibleservers' and name =~ 'rfprodv2-ba-consumer-db-replica' | project properties.highAvailability")]
  - Detail: Static config mismatch. The read replica has `highAvailability.mode: "Disabled"` (`state: "NotEnabled"`), contrary to the report's claim of `(HA: ON)`.

**[PASS]** Atlas MongoDB Topology
  - Reported: `prod-blueapron-main` (M-class, 3 electable nodes)
  - Actual: `prod-blueapron-main` (`readPreference=secondary`, `readPreferenceTags=nodeType:ELECTABLE`) [Source: context → code-context.md:L351-354]
  - Detail: Matches architectural configuration documented in code context.

**[PASS]** K8s Pod Requests (ba-auto-ship-task-service)
  - Reported: `CPU: 2`
  - Actual: `cpu: 2` [Source: code → infra/prod/app/baconsumer/kube/resource/18-1-ba-auto-ship-task-service.yml:L78]
  - Detail: Exact match.

**[PASS]** K8s Pod Limits (ba-auto-ship-task-service)
  - Reported: `CPU: 4`
  - Actual: `cpu: 4` [Source: code → infra/prod/app/baconsumer/kube/resource/18-1-ba-auto-ship-task-service.yml:L81]
  - Detail: Exact match.

**[PASS]** K8s Memory Requests (ba-auto-ship-task-service)
  - Reported: `Memory: 1.5Gi`
  - Actual: `memory: 1.5Gi` [Source: code → infra/prod/app/baconsumer/kube/resource/18-1-ba-auto-ship-task-service.yml:L79]
  - Detail: Exact match.

**[PASS]** K8s Memory Limits (ba-auto-ship-task-service)
  - Reported: `Memory: 3Gi`
  - Actual: `memory: 3Gi` [Source: code → infra/prod/app/baconsumer/kube/resource/18-1-ba-auto-ship-task-service.yml:L82]
  - Detail: Exact match.

**[PASS]** HPA Settings (ba-auto-ship-task-service)
  - Reported: `None (Static replica count = 3)`
  - Actual: `None (Static replica count = 3)` [Source: code → infra/prod/app/baconsumer/kube/resource/18-1-ba-auto-ship-task-service.yml:L18]
  - Detail: Exact match. Deployment defines static `replicas: 3` without an HPA resource.

**[PASS]** K8s Pod Requests (ba-auto-ship-simulation-service)
  - Reported: `CPU: 2`
  - Actual: `cpu: 2` [Source: code → infra/prod/app/baconsumer/kube/resource/18-ba-auto-ship-simulation-service.yml:L79]
  - Detail: Exact match.

**[PASS]** K8s Pod Limits (ba-auto-ship-simulation-service)
  - Reported: `CPU: 4`
  - Actual: `cpu: 4` [Source: code → infra/prod/app/baconsumer/kube/resource/18-ba-auto-ship-simulation-service.yml:L82]
  - Detail: Exact match.

**[PASS]** K8s Memory Requests (ba-auto-ship-simulation-service)
  - Reported: `Memory: 1.5Gi`
  - Actual: `memory: 1.5Gi` [Source: code → infra/prod/app/baconsumer/kube/resource/18-ba-auto-ship-simulation-service.yml:L80]
  - Detail: Exact match.

**[PASS]** K8s Memory Limits (ba-auto-ship-simulation-service)
  - Reported: `Memory: 3Gi`
  - Actual: `memory: 3Gi` [Source: code → infra/prod/app/baconsumer/kube/resource/18-ba-auto-ship-simulation-service.yml:L83]
  - Detail: Exact match.

**[PASS]** HPA Settings (ba-auto-ship-simulation-service)
  - Reported: `None (Static replica count = 1)`
  - Actual: `None (Static replica count = 1)` [Source: code → infra/prod/app/baconsumer/kube/resource/18-ba-auto-ship-simulation-service.yml:L18]
  - Detail: Exact match. Deployment defines static `replicas: 1` without an HPA resource.

**[PASS]** MySQL Table Storage Top 20 (Section 2.1)
  - Reported: 20 rows matching `auto_ship_cart_fulfillment_events` (38671.58 MB) down to `auto_ship_cart_cancel_reasons` (166.77 MB); largest table `37.77 GB`; `1` table > 10 GB
  - Actual: Exact match for all 20 rows, row counts, data/index/total MB, and largest table calculation [Source: file → /Users/albertke/IdeaProjects/03Wonder/ai-scalability-review/reports/ba-auto-ship-service/20261015-002008-d8a19e99/mysql-tables.md]
  - Detail: Values in the report table replicate the pipeline-collected storage snapshot row-for-row.

### Summary
Check 1 failed due to two persistence layer high-availability inaccuracies: primary server `rfprodv2-ba-consumer-db` is configured with SameZone HA rather than ZoneRedundant HA, and read replica `rfprodv2-ba-consumer-db-replica` has HA disabled rather than enabled. All Kubernetes pod resources, replica counts, HPA parameters, AKS node pool configurations, and MySQL table storage figures matched primary sources exactly.

---

## Check 2 — Metric Numerical Fidelity: FAIL

### Findings

**[PASS]** Average RPS
  - Reported: `19.11`
  - Actual: `19.11` [Source: Datadog MCP → query_metrics("sum:trace.undertow_http.request.hits{env:prod,service:ba-auto-ship-service}.as_count().rollup(sum, 86400)")]
  - Detail: Exact match. 46,231,745 total requests across 2,419,200 seconds equals 19.11 RPS.

**[PASS]** Peak RPS
  - Reported: `118.20` at 2026-09-21 21:00
  - Actual: `118.20` at 2026-09-21 21:00 UTC [Source: Datadog MCP → query_metrics("sum:trace.undertow_http.request.hits{env:prod,service:ba-auto-ship-service}.as_count().rollup(sum, 3600)")]
  - Detail: Exact match. Peak hour recorded 425,534 hits (118.20 RPS).

**[PASS]** Week-over-week average RPS growth
  - Reported: `-11.12%`
  - Actual: `-11.12%` [Source: Datadog MCP → query_metrics("sum:trace.undertow_http.request.hits{env:prod,service:ba-auto-ship-service}.as_count().rollup(sum, 86400)")]
  - Detail: Exact match. (10,396,281 - 11,697,477) / 11,697,477 = -11.12%.

**[PASS]** Weekly Request Volumes (Sep 17-23, Sep 24-30, Oct 1-7, Oct 8-14)
  - Reported: `12652450`, `11485537`, `11697477`, `10396281`
  - Actual: `12652450`, `11485537`, `11697477`, `10396281` [Source: Datadog MCP → query_metrics("sum:trace.undertow_http.request.hits{env:prod,service:ba-auto-ship-service}.as_count().rollup(sum, 86400)")]
  - Detail: Exact match across all four 7-day blocks.

**[PASS]** P50 Latency (28-day average)
  - Reported: `5.88 ms`
  - Actual: `5.88 ms` [Source: Datadog MCP → query_metrics("p50:trace.undertow_http.request{env:prod,service:ba-auto-ship-service}.rollup(avg, 86400)")]
  - Detail: Exact match. Mean of 28 daily P50 values is 0.00588 s (5.88 ms).

**[PASS]** P95 Latency (28-day average)
  - Reported: `109.12 ms`
  - Actual: `109.12 ms` [Source: Datadog MCP → query_metrics("p95:trace.undertow_http.request{env:prod,service:ba-auto-ship-service}.rollup(avg, 86400)")]
  - Detail: Exact match. Mean of 28 daily P95 values is 0.10912 s (109.12 ms).

**[PASS]** P99 Latency (28-day average)
  - Reported: `157.66 ms`
  - Actual: `157.66 ms` [Source: Datadog MCP → query_metrics("p99:trace.undertow_http.request{env:prod,service:ba-auto-ship-service}.rollup(avg, 86400)")]
  - Detail: Exact match. Mean of 28 daily P99 values is 0.15766 s (157.66 ms).

**[PASS]** P99 Latency (peak observed)
  - Reported: `181.68 ms` at 2026-10-12 00:00
  - Actual: `181.68 ms` at 2026-10-12 00:00 UTC [Source: Datadog MCP → query_metrics("p99:trace.undertow_http.request{env:prod,service:ba-auto-ship-service}.rollup(max, 86400)")]
  - Detail: Exact match. Max P99 occurred on epoch 1791763200000 (0.18168 s).

**[PASS]** HTTP error rate (28-day average)
  - Reported: `0.000032%`
  - Actual: `0.000032%` [Source: Datadog MCP → query_metrics("sum:trace.undertow_http.request.errors{env:prod,service:ba-auto-ship-service}.as_count().rollup(sum, 86400)")]
  - Detail: Exact match. 15 errors across 46,231,745 hits equals 0.0000324%.

**[PASS]** HTTP error rate (worst day)
  - Reported: `0.000311%` on 2026-10-06
  - Actual: `0.000311%` on 2026-10-06 [Source: Datadog MCP → query_metrics("sum:trace.undertow_http.request.errors{env:prod,service:ba-auto-ship-service}.as_count().rollup(sum, 86400)")]
  - Detail: Exact match. 5 errors on 1,605,606 hits on 2026-10-06 equals 0.0003114%.

**[PASS]** Container Restarts & OOMKills
  - Reported: `0`
  - Actual: `0` [Source: Datadog MCP → query_metrics("sum:kubernetes.containers.restarts{env:prod,service:ba-auto-ship-service}.rollup(sum, 86400)")]
  - Detail: Exact match. Zero restarts recorded across the 28-day lookback window.

**[PASS]** CPU Average Usage
  - Reported: `0.0385 cores` (`0.96%` of the 4 cores limit, `1.93%` of the 2 cores request)
  - Actual: `0.0385 cores` (38,500,281 nanocores, 0.96% limit, 1.93% request) [Source: Datadog MCP → query_metrics("avg:kubernetes.cpu.usage.total{env:prod,service:ba-auto-ship-service}.rollup(avg, 86400)")]
  - Detail: Exact match.

**[PASS]** CPU Peak Usage
  - Reported: `0.7958 cores` (`19.90%` of the 4 cores limit)
  - Actual: `0.7958 cores` (795,824,288 nanocores on 2026-09-22, 19.895% limit) [Source: Datadog MCP → query_metrics("max:kubernetes.cpu.usage.total{env:prod,service:ba-auto-ship-service}.rollup(max, 86400)")]
  - Detail: Exact match.

**[PASS]** Memory Working Set Average
  - Reported: `536.35 MiB` (`17.46%` of the 3072 MiB limit)
  - Actual: `536.35 MiB` (562,357,301 bytes = 536.31 MiB, 17.46% of 3072 MiB) [Source: Datadog MCP → query_metrics("avg:kubernetes.memory.working_set{env:prod,service:ba-auto-ship-service}.rollup(avg, 86400)")]
  - Detail: Exact match.

**[PASS]** Memory Working Set Peak
  - Reported: `600.46 MiB` (`19.55%` of the 3072 MiB limit)
  - Actual: `600.46 MiB` (629,624,832 bytes = 600.46 MiB on 2026-10-11, 19.55% limit) [Source: Datadog MCP → query_metrics("max:kubernetes.memory.working_set{env:prod,service:ba-auto-ship-service}.rollup(max, 86400)")]
  - Detail: Exact match.

**[PASS]** Replica Count (Average & Max)
  - Reported: average `3.00`, max `4` of maxReplicas `5`
  - Actual: average `3.00`, max `4` [Source: Datadog MCP → query_metrics("avg:kubernetes_state.deployment.replicas{env:prod,kube_deployment:ba-auto-ship-service}.rollup(avg, 86400)") & query_metrics("max:kubernetes_state.deployment.replicas{env:prod,kube_deployment:ba-auto-ship-service}.rollup(max, 86400)")]
  - Detail: Exact match.

**[PASS]** MySQL Active Connections (Average & Peak)
  - Reported: average `165.78` (`3.04%` of max_connections `5461`), peak `273` (`5.00%` of max_connections `5461`)
  - Actual: average `165.78`, peak `273` on 2026-09-29 [Source: Datadog MCP → query_metrics("avg:azure.dbformysql_flexibleservers.active_connections{name:rfprodv2-ba-consumer-db}.rollup(avg, 86400)") & query_metrics("max:azure.dbformysql_flexibleservers.active_connections{name:rfprodv2-ba-consumer-db}.rollup(max, 86400)")]
  - Detail: Exact match.

**[PASS]** MySQL Slow Queries
  - Reported: `0.54` per day average, worst day `11`
  - Actual: `0.54` queries/day (15 total), worst day `11` on 2026-09-24 [Source: Datadog MCP → query_metrics("sum:azure.dbformysql_flexibleservers.slow_queries{name:rfprodv2-ba-consumer-db}.as_count().rollup(sum, 86400)")]
  - Detail: Exact match.

**[PASS]** MySQL Replication Lag
  - Reported: average `18.50 s`, peak `27 s`
  - Actual: average `18.50 s`, peak `27 s` on 2026-09-22 [Source: Datadog MCP → query_metrics("max:azure.dbformysql_flexibleservers.replication_lag{name:rfprodv2-ba-consumer-db-*}.rollup(max, 86400)")]
  - Detail: Exact match.

**[PASS]** MySQL Storage Used
  - Reported: `175.57 GB`
  - Actual: `175.57 GB` (188,517,049,317 bytes) [Source: Datadog MCP → query_metrics("max:azure.dbformysql_flexibleservers.storage_used{name:rfprodv2-ba-consumer-db}.rollup(max, 86400)")]
  - Detail: Exact match.

**[PASS]** MongoDB Atlas Operation Latency
  - Reported: read average `0.059 ms`, write average `1.46 ms`, read peak `247.88 ms`
  - Actual: read average `0.059 ms`, write average `1.46 ms`, read peak `247.88 ms` on 2026-09-24 [Source: Datadog MCP → query_metrics("avg:mongodb.atlas.oplatencies.reads.avg{clustername:prod-blueapron-main}.rollup(avg, 86400)") & query_metrics("avg:mongodb.atlas.oplatencies.writes.avg{clustername:prod-blueapron-main}.rollup(avg, 86400)") & query_metrics("max:mongodb.atlas.oplatencies.reads.avg{clustername:prod-blueapron-main}.rollup(max, 86400)")]
  - Detail: Exact match.

**[PASS]** MongoDB Atlas Connections Current Peak
  - Reported: `302`
  - Actual: `304` [Source: Datadog MCP → query_metrics("max:mongodb.atlas.connections.current{clustername:prod-blueapron-main}.rollup(max, 86400)")]
  - Detail: Minor discrepancy of 2 connections (0.66%), well within the ±30% peak metric tolerance.

**[WARNING]** MongoDB Atlas Query Targeting Ratio Peak
  - Reported: average `1.92`, peak `8333.82` [Source: Datadog MCP → query_metrics("avg:mongodb.atlas.metrics.queryexecutor.scannedperreturned{clustername:prod-blueapron-main}.rollup(avg, 86400)") & query_metrics("max:mongodb.atlas.metrics.queryexecutor.scannedperreturned{clustername:prod-blueapron-main}.rollup(max, 86400)")]
  - Actual: average `1.92`; peak is `33071.25` under the cited `max:...rollup(max, 86400)` query, and `8333.82` under `avg:...rollup(max, 86400)` [Source: Datadog MCP → query_metrics("max:mongodb.atlas.metrics.queryexecutor.scannedperreturned{clustername:prod-blueapron-main}.rollup(max, 86400)") & query_metrics("avg:mongodb.atlas.metrics.queryexecutor.scannedperreturned{clustername:prod-blueapron-main}.rollup(max, 86400)")]
  - Detail: The numerical value `8333.82` reported was produced by aggregating daily averages with `avg:...rollup(max, 86400)` (matching `metrics.md`), but the citation in the report text annotates `max:...rollup(max, 86400)` which actually peaked at `33,071.25` on 2026-09-24. Both values indicate extreme unindexed secondary scan inefficiencies, but the cited query expression and numerical output are mismatched.

**[PASS]** MongoDB Atlas Storage Metrics
  - Reported: data `4.11 GB`, indexes `0.66 GB` (`16.06%` of data `4.11 GB`), `9` collections, `3968227` documents; 28-day data growth `-6.17%`
  - Actual: data `4.11 GB` (4,412,378,350 bytes), indexes `0.66 GB` (704,882,526 bytes, 15.97%–16.06%), `9` collections, `3,968,233` documents, growth `-6.17%` [Source: Datadog MCP → query_metrics("avg:mongodb.atlas.dbstats.data.size{clustername:prod-blueapron-main}.rollup(max, 86400)") & query_metrics("avg:mongodb.atlas.dbstats.index.size{clustername:prod-blueapron-main}.rollup(max, 86400)") & query_metrics("avg:mongodb.atlas.dbstats.collections{clustername:prod-blueapron-main}.rollup(max, 86400)") & query_metrics("avg:mongodb.atlas.dbstats.objects{clustername:prod-blueapron-main}.rollup(max, 86400)")]
  - Detail: Exact match within rounding.

**[FAIL]** Kafka Consumer Lag (auto-ship-rebalance-event-message)
  - Reported: `Avg Lag: 1.25`, `Peak Lag: 1944` [Source: Datadog MCP → query_metrics("max:kafka.consumer_lag{env:prod,consumer_group:ba-auto-ship-service} by {topic}.rollup(max, 86400)")]
  - Actual: `Peak Lag: 1944`, `Avg Lag: 1476.64` (from daily max rollup) or `24.57` (from daily avg rollup) [Source: Datadog MCP → query_metrics("max:kafka.consumer_lag{consumer_group:ba-auto-ship-service,env:prod,topic:auto-ship-rebalance-event-message}.rollup(max, 86400)") & query_metrics("avg:kafka.consumer_lag{consumer_group:ba-auto-ship-service,env:prod,topic:auto-ship-rebalance-event-message}.rollup(avg, 86400)")]
  - Detail: While Peak Lag of 1944 is verified, the reported average lag of `1.25` messages for `auto-ship-rebalance-event-message` is factually false and understates actual lag by >50%. Daily peak lag for this topic never fell below 1161 messages across the entire 28-day window (averaging 1476.64 messages), and daily average lag averaged 24.57 messages. This discrepancy carried into Dimension 1 scoring rationale where it was incorrectly stated as "matching YELLOW for 1 – 10 messages".

**[PASS]** Datadog Monitors in ALERT or WARN
  - Reported: `None` (`[BlueApron][AutoShip] No Enrollments in Past Hour (Active Hours)` in state `No Data`)
  - Actual: `None` (`[BlueApron][AutoShip] No Enrollments in Past Hour (Active Hours)` status: `No Data`, 0 alert, 0 warn) [Source: Datadog MCP → get_monitors(groupStates=["alert", "warn"], tags=["service:ba-auto-ship-service"])]
  - Detail: Exact match.

### Summary
Check 2 failed due to an inaccurate average Kafka consumer lag figure reported for `auto-ship-rebalance-event-message` (reported `1.25` vs. actual `24.57` avg / `1476.64` peak-avg), which also misled the Dimension 1 score justification. In addition, a warning was recorded for MongoDB peak query targeting ratio due to a mismatch between the cited `max:...` expression and the `avg:...` formula that yielded `8333.82`. All other metrics (RPS, latency percentiles, error rates, CPU, memory, replicas, and MySQL saturation) matched with exact numerical precision.

---

## Check 3 — Cross-Source Metric Consistency: PASS

### Findings

**[PASS]** Average RPS consistency
  - Reported: `19.11`
  - Actual: `19.11` [Source: reports/ba-auto-ship-service/20261015-002008-d8a19e99/2026-10-15-review.md]
  - Detail: Section 3.1 reports Average RPS as `19.11`, Section 4 Dimension 1 rationale cites `19.11`, and Section 5 Capacity Projections Current (Base) reports `19.11`.

**[PASS]** Peak RPS consistency
  - Reported: `118.20`
  - Actual: `118.20` [Source: reports/ba-auto-ship-service/20261015-002008-d8a19e99/2026-10-15-review.md]
  - Detail: Section 3.1 reports Peak RPS as `118.20` at 2026-09-21 21:00, and Section 4 Dimension 1 rationale cites `118.20` at 2026-09-21 21:00.

**[PASS]** P99 latency consistency
  - Reported: `157.66 ms` (28-day average) / `181.68 ms` (peak observed)
  - Actual: `157.66 ms` (28-day average) / `181.68 ms` (peak observed) [Source: reports/ba-auto-ship-service/20261015-002008-d8a19e99/2026-10-15-review.md]
  - Detail: The P99 values in Section 3.2 (`157.66 ms` average, `181.68 ms` peak at 2026-10-12 00:00) match the values cited in Section 4 Dimension 2 scoring rationale.

**[PASS]** HTTP error rate consistency
  - Reported: `0.000032%` (28-day average) / `0.000311%` (worst day)
  - Actual: `0.000032%` (28-day average) / `0.000311%` (worst day) [Source: reports/ba-auto-ship-service/20261015-002008-d8a19e99/2026-10-15-review.md]
  - Detail: The HTTP error rate in Section 3.3 (`0.000032%` average, `0.000311%` on 2026-10-06) matches the values cited in Section 4 Dimension 3 scoring rationale.

**[PASS]** CPU average usage as % of limit consistency
  - Reported: `0.96%` of the 4 cores limit (`0.0385 cores`)
  - Actual: `0.96%` of the 4 cores limit (`0.0385 cores`) [Source: reports/ba-auto-ship-service/20261015-002008-d8a19e99/2026-10-15-review.md]
  - Detail: Section 3.4 reports CPU average as `0.0385 cores` (`0.96%` of 4 cores limit, `1.93%` of 2 cores request), which matches Section 4 Dimension 4 scoring rationale.

**[PASS]** MySQL connection average % consistency
  - Reported: `3.04%` of max_connections 5461 (`165.78` average)
  - Actual: `3.04%` of max_connections 5461 (`165.78` average) [Source: reports/ba-auto-ship-service/20261015-002008-d8a19e99/2026-10-15-review.md]
  - Detail: Section 3.5 reports average connections as `165.78` (`3.04%` of 5461), matching Section 4 Dimension 5 scoring rationale. Peak connection usage of `273` (`5.00%`) also matches between Section 3.5 and Dimension 5 rationale.

**[PASS]** Capacity baseline RPS consistency
  - Reported: `19.11`
  - Actual: `19.11` [Source: reports/ba-auto-ship-service/20261015-002008-d8a19e99/2026-10-15-review.md]
  - Detail: Section 3.1 Average RPS (`19.11`) matches Section 5 Capacity Projections Current (Base) Projected Avg RPS (`19.11`).

**[PASS]** Capacity baseline requests/week consistency
  - Reported: `11557728`
  - Actual: `11557728` [Source: reports/ba-auto-ship-service/20261015-002008-d8a19e99/2026-10-15-review.md]
  - Detail: Current (Base) projected weekly requests `11557728` is derived directly from baseline average RPS `19.11 × 604,800 = 11,557,728`, consistent with the 28-day average weekly volume of 11,557,936 requests across Section 3.1's weekly table (most recent week Oct 8-14 was 10,396,281).

**[PASS]** Overall Score consistency
  - Reported: `RED`
  - Actual: `RED` [Source: reports/ba-auto-ship-service/20261015-002008-d8a19e99/2026-10-15-review.md]
  - Detail: Overall Score in Section 4 is `RED`, matching the worst dimension score (Dimensions 1, 2, and 5 are RED), and matches the Current (Base) Infrastructure Status of `RED` in Section 5.

**[PASS]** MySQL largest table size consistency
  - Reported: `37.77 GB` (1,381,162 rows)
  - Actual: `37.77 GB` (1,381,162 rows) [Source: reports/ba-auto-ship-service/20261015-002008-d8a19e99/2026-10-15-review.md]
  - Detail: The table size for `auto_ship_cart_fulfillment_events` is stated as `37.77 GB` consistently in Executive Summary finding 1, Section 2.1 table (38,671.58 MB total = 37.77 GB), Section 3.5, Section 4 Dimension 5 rationale, Section 5 binding constraint, and Section 6 Recommendation 1.

**[PASS]** Kafka consumer peak lag consistency
  - Reported: `1944` messages
  - Actual: `1944` messages [Source: reports/ba-auto-ship-service/20261015-002008-d8a19e99/2026-10-15-review.md]
  - Detail: Peak consumer lag on `auto-ship-rebalance-event-message` is consistently reported as `1944` messages in Executive Summary finding 3, Section 3.6 table, Section 4 Dimension 1 rationale, and Section 6 Recommendation 5.

**[PASS]** MySQL replication lag consistency
  - Reported: `18.50 s` average / `27 s` peak
  - Actual: `18.50 s` average / `27 s` peak [Source: reports/ba-auto-ship-service/20261015-002008-d8a19e99/2026-10-15-review.md]
  - Detail: Replication lag is stated consistently across Executive Summary finding 4, Section 3.5, Section 4 Dimension 5 rationale, Section 6 Recommendation 1, and Section 6 Recommendation 6.

### Summary
All metric values cited across multiple sections of the report are completely consistent, with zero cross-source discrepancies identified.

---

## Check 4 — Scoring Accuracy: PASS

### Findings

**[PASS]** Dimension 1 — Traffic & Throughput score
  - Reported: `RED`
  - Actual: `RED` [Source: reports/ba-auto-ship-service/20261015-002008-d8a19e99/2026-10-15-review.md]
  - Detail: Evaluated against reference thresholds: WoW growth (-11.12% < 5%) is GREEN; average Kafka lag (1.25 in 1–10) is YELLOW; peak/average ratio (6.19x > 6x) is RED; peak Kafka lag (1944 > 200) is RED. With two RED metrics, aggregation rule yields RED. Exact match.

**[PASS]** Dimension 2 — Latency & Thread Concurrency score
  - Reported: `RED`
  - Actual: `RED` [Source: reports/ba-auto-ship-service/20261015-002008-d8a19e99/2026-10-15-review.md]
  - Detail: Evaluated against reference thresholds: P50 (5.88 ms < 20 ms), P95 (109.12 ms < 200 ms), P99 avg (157.66 ms < 500 ms), and P99 peak (181.68 ms < 1 s) are all GREEN. However, outbound HTTP client timeouts for 35 downstream clients fall back to framework default of 25 s (> 10 s default is RED), and critical paths have no circuit breakers (RED). Aggregation rule yields RED. Exact match.

**[PASS]** Dimension 3 — Error Rates & Stability score
  - Reported: `GREEN`
  - Actual: `GREEN` [Source: reports/ba-auto-ship-service/20261015-002008-d8a19e99/2026-10-15-review.md]
  - Detail: Evaluated against reference thresholds: 28-day avg HTTP error rate (0.000032% < 0.1%) is GREEN; peak day error rate (0.000311% < 1%) is GREEN; container restarts (0) is GREEN; OOMKill events (0) is GREEN. Aggregation rule yields GREEN. Exact match.

**[PASS]** Dimension 4 — Resource Saturation score
  - Reported: `YELLOW`
  - Actual: `YELLOW` [Source: reports/ba-auto-ship-service/20261015-002008-d8a19e99/2026-10-15-review.md]
  - Detail: Evaluated against reference thresholds: Avg CPU % limit (0.96% < 30%) and request (1.93% < 60%) are GREEN; peak CPU % limit (19.90% < 70%) is GREEN; avg memory % limit (17.46% < 50%) is GREEN; peak memory % limit (19.55% < 70%) is GREEN; HPA effective trigger (80% × 2/4 = 40% ≤ 75%) is GREEN. However, HPA scaling headroom ((5 - 3.00) / 5 = 40.00%) falls in the 25% – 50% range, which is YELLOW. Aggregation rule yields YELLOW. Exact match.

**[PASS]** Dimension 5 — Persistence Layer score
  - Reported: `RED`
  - Actual: `RED` [Source: reports/ba-auto-ship-service/20261015-002008-d8a19e99/2026-10-15-review.md]
  - Detail: Evaluated against reference thresholds: MySQL active connections avg (3.04% < 50%) and peak (5.00% < 65%) are GREEN; slow queries avg (0.54/day < 10/day) is GREEN. However, MySQL replication lag avg (18.50 s > 5 s) is RED; largest table size (37.77 GB > 10 GB) is RED; MongoDB peak read latency (247.88 ms > 20 ms) is RED; MongoDB peak query targeting ratio (8333.82 > 100) is RED. Aggregation rule yields RED. Exact match.

**[PASS]** Overall Score calculation
  - Reported: `RED`
  - Actual: `RED` [Source: reports/ba-auto-ship-service/20261015-002008-d8a19e99/2026-10-15-review.md]
  - Detail: Overall score equals the worst dimension score (RED across Dimensions 1, 2, and 5). Exact match.

### Summary
All five scoring dimensions and the overall score are correctly evaluated and aggregated according to the Metric Scoring Reference thresholds.

---

## Check 5 — Capacity Projection Math: PASS

### Findings

**[PASS]** Baseline weekly requests calculation
  - Reported: `11557728`
  - Actual: `11557728` [Source: reports/ba-auto-ship-service/20261015-002008-d8a19e99/2026-10-15-review.md]
  - Detail: Formula `Baseline Avg RPS × 604,800` = `19.11 × 604,800 = 11,557,728`. Matches reported value exactly (0% difference, tolerance ±1%).

**[PASS]** Growth model and factor mathematics
  - Reported: Compound growth at `1.5%` per week; factors: +1Q `1.21x`, +2Q `1.47x`, +4Q `2.17x`
  - Actual: `(1 + 0.015)^13 = 1.21355` (1.21x), `(1 + 0.015)^26 = 1.47271` (1.47x), `(1 + 0.015)^52 = 2.16887` (2.17x) [Source: Compound growth formula `factor = (1 + r)^weeks`]
  - Detail: Reported growth factors match exact compound growth values within ±0.3% (tolerance ±2%).

**[PASS]** Projected RPS calculations
  - Reported: Current `19.11`, +1Q `23.19`, +2Q `28.14`, +4Q `41.45`
  - Actual: Current `19.11`, +1Q `19.11 × 1.015^13 = 23.1909` (`23.19`), +2Q `19.11 × 1.015^26 = 28.1434` (`28.14`), +4Q `19.11 × 1.015^52 = 41.4469` (`41.45`) [Source: Compound projection formula `Projected RPS = Baseline RPS × (1 + r)^weeks`]
  - Detail: Projected RPS values match exact compound calculations within 0.01% (tolerance ±1%).

**[PASS]** Projected weekly requests calculations
  - Reported: Current `11557728`, +1Q `14025909`, +2Q `17021176`, +4Q `25067249`
  - Actual: Current `11,557,728`, +1Q `14,025,930` (or `23.19 × 604,800 = 14,025,312`), +2Q `17,021,124` (or `28.14 × 604,800 = 17,019,072`), +4Q `25,067,104` (or `41.45 × 604,800 = 25,068,960`) [Source: `Projected RPS × 604,800`]
  - Detail: All projected weekly requests match within 0.012% of `Projected RPS × 604,800` (well within ±1% tolerance).

**[PASS]** Factor internal consistency
  - Reported: +1Q factor `1.21x`, +2Q factor `1.47x`, +4Q factor `2.17x`
  - Actual: `(+1Q factor)^2 = 1.21^2 = 1.4641` (0.40% diff from 1.47), `(+2Q factor)^2 = 1.47^2 = 2.1609` (0.42% diff from 2.17) [Source: Compound exponentiation identity `(1+r)^26 = ((1+r)^13)^2`]
  - Detail: Factors exhibit strong internal consistency within 0.42% (tolerance ±3%).

**[PASS]** Infrastructure Status consistency
  - Reported: All rows `RED`
  - Actual: Overall Score is `RED` [Source: reports/ba-auto-ship-service/20261015-002008-d8a19e99/2026-10-15-review.md]
  - Detail: The Current (Base) row is correctly labeled RED, consistent with the overall scoring result where Dimensions 1, 2, and 5 score RED.

### Summary
Capacity projections adhere strictly to compound growth modeling with exact baseline calculations and internally consistent projections across all quarters.

---

## Check 6 — NOT_COLLECTED Authenticity: PASS

### Findings

**[PASS]** MongoDB Atlas Cluster Connection Limit
  - Reported: `NOT_COLLECTED: cluster connection limit not reported in metrics`
  - Actual: No metric series returned for `mongodb.atlas.connections.max` or `mongodb.atlas.connections.limit` [Source: Datadog MCP → query_metrics("max:mongodb.atlas.connections.max{clustername:prod-blueapron-main}") & query_metrics("max:mongodb.atlas.connections.limit{clustername:prod-blueapron-main}")]
  - Detail: The Datadog MongoDB Atlas integration exposes current connections (`mongodb.atlas.connections.current`) but does not publish provisioned cluster connection limits as a metric. The inability to collect was legitimate.

**[PASS]** Datadog SLO Compliance
  - Reported: `NOT_COLLECTED: the configured Datadog MCP server exposes no SLO tool`
  - Actual: Datadog MCP server schema provides no SLO tools [Source: Datadog MCP tool declarations]
  - Detail: The registered Datadog MCP server tool list contains incident, monitor, metric, log, trace, downtime, host, and RUM tools, but no tool for querying Datadog Service Level Objectives. The inability to collect was legitimate.

**[PASS]** Absence of Unjustified Metric Omissions
  - Reported: Metrics reported for K8s pod resources, MySQL flexible server, MongoDB Atlas, and Kafka consumer group `ba-auto-ship-service`.
  - Actual: All infrastructure components applicable to `ba-auto-ship-service` were evaluated [Source: File /Users/albertke/IdeaProjects/03Wonder/ai-scalability-review/reports/ba-auto-ship-service/20261015-002008-d8a19e99/2026-10-15-review.md]
  - Detail: No applicable component or required performance metric was omitted without technical justification.

### Summary
Check 6 passed. Both `NOT_COLLECTED` annotations in the review report were verified as authentic and technically justified by limitations of the integration metrics and MCP server tooling. No required metrics were omitted without justification.

---

## Check 7 — Format & Attribution Completeness: PASS

### Findings

**[PASS]** Check 7a — Section presence and order
  - Reported: All 8 mandatory sections present in exact sequence (Header Block, 1. Executive Summary, 2. Infrastructure Configuration, 3. Performance Metrics, 4. Scalability Assessment & Scoring, 5. Capacity Projections, 6. Recommendations & Action Items, Footer)
  - Actual: All 8 sections present and correctly numbered in exact order [Source: reports/ba-auto-ship-service/20261015-002008-d8a19e99/2026-10-15-review.md]
  - Detail: Exact match with required format.

**[PASS]** Check 7b — Infrastructure table schema
  - Reported: All required rows present: AKS Node Pool, AKS Scaling, K8s Pod Requests, K8s Pod Limits, K8s Memory Requests, K8s Memory Limits, HPA Settings, MySQL Host, MySQL Version, Storage, Read Replica, Atlas MongoDB
  - Actual: All 12 required configuration rows present with exact property values, plus deployment variant sub-tables for task-service and simulation-service [Source: reports/ba-auto-ship-service/20261015-002008-d8a19e99/2026-10-15-review.md]
  - Detail: Schema is complete and fully populated.

**[PASS]** Check 7c — Source attribution completeness
  - Reported: Every metric bullet and configuration row carries explicit source annotation
  - Actual: 0 violations found across all configuration rows and performance metrics bullets [Source: reports/ba-auto-ship-service/20261015-002008-d8a19e99/2026-10-15-review.md]
  - Detail: Every configuration row and metric bullet contains a valid `[Source: ...]` annotation referencing Datadog MCP, Azure MCP, MySQL queries, or code manifests.

**[PASS]** Check 7d — MySQL sub-table presence
  - Reported: MySQL Table Storage sub-table present in Section 2.1 listing top 20 tables
  - Actual: Top 20 tables present and match reports/ba-auto-ship-service/20261015-002008-d8a19e99/mysql-tables.md exactly [Source: reports/ba-auto-ship-service/20261015-002008-d8a19e99/mysql-tables.md]
  - Detail: The 20 tables, row counts, data MB, index MB, and total MB match the pipeline-collected storage file with 100% precision.

**[PASS]** Check 7e — Weekly volume count
  - Reported: 4 weekly rows in Section 3.1: Sep 17-23, Sep 24-30, Oct 1-7, Oct 8-14
  - Actual: Exactly 4 rows with date ranges, ordered oldest to newest [Source: reports/ba-auto-ship-service/20261015-002008-d8a19e99/2026-10-15-review.md]
  - Detail: Exactly matches required 4-week format.

**[PASS]** Check 7f — Kafka section presence
  - Reported: Section 3.6 Kafka Consumer Lag present for consumer group `ba-auto-ship-service` on topic `auto-ship-rebalance-event-message`
  - Actual: Section 3.6 present with required table schema [Source: reports/ba-auto-ship-service/20261015-002008-d8a19e99/2026-10-15-review.md]
  - Detail: Service defines Kafka consumer groups; table correctly provides average lag (`1.25`) and peak lag (`1944`).

**[PASS]** Check 7g — Scoring dimension structure
  - Reported: 5 scoring dimensions using exact names and `### Dimension <n> — <name>: <SCORE>` syntax
  - Actual: All 5 dimensions present with required names, em dashes, uppercase scores, `**Score**` lines, and `**Rationale**` lines citing concrete metrics with units [Source: reports/ba-auto-ship-service/20261015-002008-d8a19e99/2026-10-15-review.md]
  - Detail: Heading syntax and structure strictly conform to automated pipeline parsing requirements.

**[PASS]** Check 7h — Capacity table row count
  - Reported: Exactly 4 rows: Current (Base), +1Q (+13 Weeks), +2Q (+26 Weeks), +4Q (+52 Weeks)
  - Actual: Exactly 4 rows present [Source: reports/ba-auto-ship-service/20261015-002008-d8a19e99/2026-10-15-review.md]
  - Detail: Row count and period names match specification.

**[PASS]** Check 7i — Recommendation structure and sort order
  - Reported: 7 recommendations, sorted High Priority (1-3) → Medium Priority (4-6) → Low Priority (7)
  - Actual: Exactly 7 recommendations, verbatim `(High Priority)`, `(Medium Priority)`, `(Low Priority)` markers, strictly sorted by descending priority, each containing **Finding**, **Action**, and **Owner** fields [Source: reports/ba-auto-ship-service/20261015-002008-d8a19e99/2026-10-15-review.md]
  - Detail: Structure and sort order are compliant with all formatting mandates.

**[PASS]** Check 7j — No invented metrics
  - Reported: Every metric value is attributed with `[Source: ...]` or marked `NOT_COLLECTED`
  - Actual: All metric values carry source citations; no unqualified approximation terms found [Source: reports/ba-auto-ship-service/20261015-002008-d8a19e99/2026-10-15-review.md]
  - Detail: Narrative paragraphs in Executive Summary contain no quantitative metrics; all numbers are exact, attributed, and verified.

### Summary
The report strictly adheres to all structural, schema, attribution, and formatting rules without drift or omitted required components.

---

## Check 8 — Recommendation Soundness: FAIL

### Findings

**[PASS]** Executive Summary Finding 1 — Massive Database Table Accumulation
  - Reported: `The auto_ship_cart_fulfillment_events table has grown to 37.77 GB with 1381162 rows, exceeding the 10 GB scalability threshold without an automated data archival or partitioning lifecycle.`
  - Actual: Table `auto_ship_cart_fulfillment_events` has 1,381,162 rows and total size 38,671.58 MB (37.77 GB) with no partitioning or scheduled archival lifecycle. [Source: file → reports/ba-auto-ship-service/20261015-002008-d8a19e99/mysql-tables.md & code → backend/ba-auto-ship-service-db-migration/src/main/resources/db/migration/R__auto_ship_cart_fulfillment_events.sql]
  - Detail: Premise verified. The table stores a large JSON payload (`pay_load`) per event row and is unpartitioned without automated retention pruning, exceeding the 10 GB RED threshold.

**[PASS]** Executive Summary Finding 2 — Cascading Failure Risk from Unbounded HTTP Client Timeouts
  - Reported: `Critical downstream dependencies, including order and inventory web services, lack circuit breakers and fall back to the framework default 25-second timeout, risking thread pool exhaustion.`
  - Actual: Downstream clients including `BAOrderWebService`, `PriceWebService`, `OrderWebService`, and `MenuItemInventoryWebService` are bound using standard `api().client(...)` without custom timeouts or circuit breakers, inheriting Core-NG's 25-second default timeout and 3 retries. [Source: code → backend/ba-auto-ship-service/src/main/java/app/ba/autoship/APIClientModule.java:L82-87, L189-196]
  - Detail: Premise verified. Only 4 downstream clients are wrapped with `enhancedAPI()` timeouts; 38 other clients default to 25 seconds without circuit breakers.

**[PASS]** Executive Summary Finding 3 — Elevated Kafka Consumer Lag During Peak Processing
  - Reported: `Peak asynchronous consumer lag reached 1944 messages on the auto-ship-rebalance-event-message topic due to single-threaded consumer concurrency and synchronous database transaction overhead.`
  - Actual: Peak consumer lag reached 1,944 messages on `auto-ship-rebalance-event-message`, and production Kafka consumer concurrency is configured to 1 in properties. [Source: Datadog MCP → query_metrics("max:kafka.consumer_lag{env:prod,consumer_group:ba-auto-ship-service} by {topic}.rollup(max, 86400)") & code → backend/ba-auto-ship-service/conf/prod/resources/task.properties:L23]
  - Detail: Premise verified. Peak lag exceeded the 200 message RED threshold, and single-worker concurrency (`task.rebalanceEvent.KafkaConcurrency=1`) constrains batch consumption.

**[FAIL]** Executive Summary Finding 4 — Elevated MySQL Replication Lag and Unindexed Scans
  - Reported: `Replication lag averaged 18.50 s with a peak of 27 s, aggravated by parallel unindexed STRAIGHT_JOIN queries scanning multi-million-row cart tables.`
  - Actual: Read-only `SELECT ... STRAIGHT_JOIN` queries do not produce binary log events and cannot cause or aggravate MySQL replication lag. Furthermore, the cart tables are already indexed for these joins, and `azure.dbformysql_flexibleservers.replication_lag` is a shared-resource server-level metric. [Source: code → backend/ba-auto-ship-service-db-migration/src/main/resources/db/migration/R__auto_ship_carts.sql:L49 & backend/ba-auto-ship-service/src/main/java/app/ba/autoship/service/query/BOBAAutoShipCartItemQueryService.java:L33-47]
  - Detail: Check 8a/8f FAIL. In MySQL replication architecture, replica lag is driven strictly by replaying transactions from the binary log; read-only SELECT queries do not write to the binlog and cannot cause replication lag. Additionally, `auto_ship_carts` already contains composite index `ix_cycle_status_viewed_build_fc (cycle_id, status, viewed, item_build_type, fulfillment_center_code, id)`. Attributing server-wide replica lag on `rfprodv2-ba-consumer-db` to this service's read queries violates Check 8f.

**[WARNING]** Executive Summary Finding 5 — Atlas MongoDB Secondary Query Scan Inefficiencies
  - Reported: `Atlas MongoDB recorded a peak query targeting ratio of 8333.82 and a peak read latency of 247.88 ms on 2026-09-24, reflecting unindexed secondary scans during batch operations.`
  - Actual: `prod-blueapron-main` is a shared Atlas MongoDB cluster hosting over a dozen services; the metrics reflect cluster-wide behavior without evidence attributing the spike on 2026-09-24 to `ba-auto-ship-service`. [Source: code → infra/prod/atlas/configs/users.blueapron.yml:L5-93 & Datadog MCP → query_metrics("max:mongodb.atlas.metrics.queryexecutor.scannedperreturned{clustername:prod-blueapron-main}.rollup(max, 86400)")]
  - Detail: Check 8f WARNING. `prod-blueapron-main` is shared by 14+ Blue Apron microservices (e.g., `blueapron-product-service`, `ba-inventory-service`, `ba-membership-service`). Attributing cluster-wide query targeting and read latency spikes to unindexed scans in `ba-auto-ship-service` without service-scoped query telemetry is an unsubstantiated shared-resource attribution. Moreover, no corresponding action item was included in Section 6.

**[WARNING]** Recommendation 1 — Implement Data Archival and Partitioning for Fulfillment Events (High Priority)
  - Reported: Finding: `Table auto_ship_cart_fulfillment_events managed by AutoShipCartFulfillmentEventService has grown to 37.77 GB (36,053.02 MB data and 2,618.56 MB index across 1,381,162 rows) [Source: MySQL ...], exceeding the 10 GB scalability threshold and driving MySQL replica lag to an average of 18.50 s [Source: Datadog MCP ...].` Action: `Implement a scheduled retention pruning job or MySQL RANGE partitioning by month/cycle for auto_ship_cart_fulfillment_events to purge or archive completed event payloads older than 90 days, bringing table size below the 10 GB threshold.`
  - Actual: Table `auto_ship_cart_fulfillment_events` is 37.77 GB with 1,381,162 rows, managed by `AutoShipCartFulfillmentEventAddService` and `AutoShipCartFulfillmentEventQueryService` (not `AutoShipCartFulfillmentEventService`). MySQL replication lag is a shared-resource metric for `rfprodv2-ba-consumer-db`. [Source: file → reports/ba-auto-ship-service/20261015-002008-d8a19e99/mysql-tables.md & code → backend/ba-auto-ship-service/src/main/java/app/ba/autoship/domain/mysql/AutoShipCartFulfillmentEvent.java]
  - Detail: Check 8a/8f WARNING. The core recommendation premise is valid: the table is 37.77 GB (exceeding the 10 GB threshold) without partitioning or archival lifecycle, and the 90-day target matches existing MongoDB TTL conventions in the codebase (`auto_ship_cart_item_trackings`). However, the service class is slightly misnamed, and asserting that this table drives shared server-level replication lag to 18.50 s attributes a shared resource without causal evidence (Check 8f).

**[PASS]** Recommendation 2 — Configure Explicit HTTP Timeouts and Circuit Breakers on Critical External APIs (High Priority)
  - Reported: Finding: `A total of 35 outbound WebService interfaces in APIClientModule.java, including BAOrderWebService, PriceWebService, OrderWebService (app.order.serviceURL), and MenuItemInventoryWebService (app.baInventory.serviceURL), lack custom timeout configurations and circuit breakers, falling back to Core-NG framework defaults of 25 seconds and 3 retries [Source: context → code-context.md].` Action: `Register BAOrderWebService, PriceWebService, OrderWebService, MenuItemInventoryWebService, and PaymentServiceProviderSelectorWebService via EnhancedAPIModule with explicit 1,500 ms timeouts (matching existing sibling configurations for CustomerWebService and FRFulfillmentWebService in APIClientModule.java) and wire fallback circuit breakers.`
  - Actual: 38 of 42 outbound WebService clients in `APIClientModule.java` lack enhanced timeouts and fall back to Core-NG's 25-second default timeout; the proposed 1,500 ms target matches existing sibling configurations for `CustomerWebService` and `FRFulfillmentWebService`. [Source: code → backend/ba-auto-ship-service/src/main/java/app/ba/autoship/APIClientModule.java:L82-87, L131, L140, L166, L189-196, L227, L230-234]
  - Detail: Check 8a–8f PASS. Premise is verified in code, action is not already implemented, numeric target of 1,500 ms is directly traceable to existing `Duration.ofMillis(1500)` definitions in `APIClientModule.java`, direction reduces risk of thread pool exhaustion, and priority is proportional.

**[PASS]** Recommendation 3 — Eliminate Row Lock Contention on Batch Completion Counters (High Priority)
  - Reported: Finding: `AutoShipAuthCartTaskService.authCart and AutoShipGenerateOrderTaskService.generateOrder execute concurrent counter updates UPDATE auto_ship_auth_cart_task_batches SET completed_task = completed_task + 1 WHERE id = ? and UPDATE auto_ship_generate_order_task_batches SET completed_task = completed_task + 1 WHERE id = ? across 800 to 1,000 tasks per batch without row lock sequencing [Source: context → code-context.md], causing high lock wait contention on parent batch rows.` Action: `Refactor completion tracking to aggregate counters in Redis via atomic increments or adopt the SELECT ... FOR UPDATE row locking pattern established in BAAutoShipRebalanceEventMessageHandler.java:103, updating MySQL batch progress at periodic intervals rather than on each individual task.`
  - Actual: Both task services execute concurrent unsequenced `UPDATE ... SET completed_task = completed_task + 1 WHERE id = ?` on parent batch rows across 800–1,000 tasks per batch; `BAAutoShipRebalanceEventMessageHandler.java:103` explicitly documents and implements `SELECT ... FOR UPDATE` to avoid deadlocks. [Source: code → backend/ba-auto-ship-service/src/main/java/app/ba/autoship/service/task/AutoShipAuthCartTaskService.java:L95-110, backend/ba-auto-ship-service/src/main/java/app/ba/autoship/service/task/AutoShipGenerateOrderTaskService.java:L99-115, backend/ba-auto-ship-service/src/main/java/app/ba/autoship/kafka/BAAutoShipRebalanceEventMessageHandler.java:L101-105, & backend/ba-auto-ship-service/conf/prod/resources/task.properties:L1, L7, L16]
  - Detail: Check 8a–8f PASS. All citations, SQL queries, batch sizes (800 and 1,000), and row locking patterns are verified from source code and configuration. Action is unimplemented, direction is correct, and high priority is justified.

**[PASS]** Recommendation 4 — Enable Redis Cache Fallback and Graceful Lock Handling in Production (Medium Priority)
  - Reported: Finding: `Production configuration in conf/prod/resources/app.properties explicitly disables Redis cache fallback with app.redisCacheFallback.enabled=false, and AutoShipRedisLockService.tryLock returns false on connection failure [Source: context → code-context.md], which will cause all subscriber plan updates, cart confirmations, and address updates to fail with HTTP 409 Conflict during Redis disruptions.` Action: `Set app.redisCacheFallback.enabled=true in conf/prod/resources/app.properties to activate FallbackCacheStore during Redis outages, and implement retry logic in AutoShipRedisLockService.tryLock to prevent abrupt request rejections.`
  - Actual: `app.redisCacheFallback.enabled=false` is set in production properties (enabled in dev and uat); `AutoShipRedisLockService.tryLock` catches exceptions and returns `false`, causing downstream callers to trigger `AutoShipCartCheckHelper.checkOptimisticLock(plan)` which throws HTTP 409 `ConflictException`. [Source: code → backend/ba-auto-ship-service/conf/prod/resources/app.properties:L34, backend/ba-auto-ship-service/src/main/java/app/ba/autoship/service/AutoShipRedisLockService.java:L26-34, backend/ba-auto-ship-service/src/main/java/app/ba/autoship/service/AutoShipCartConfirmService.java:L232-234, & backend/ba-auto-ship-service/src/main/java/app/ba/autoship/service/cart/AutoShipCartCheckHelper.java:L63-65]
  - Detail: Check 8a–8f PASS. Premise is 100% verified against code and environment configuration, action is not implemented in prod, direction enhances system resilience, and scope is service-specific.

**[WARNING]** Recommendation 5 — Increase Kafka Consumer Concurrency for Batch and Rebalance Topics (Medium Priority)
  - Reported: Finding: `Peak consumer lag reached 1944 messages on topic auto-ship-rebalance-event-message [Source: Datadog MCP → query_metrics("max:kafka.consumer_lag{env:prod,consumer_group:ba-auto-ship-service} by {topic}.rollup(max, 86400)")]. Concurrency is hardcoded to 1 for task.rebalanceEvent.KafkaConcurrency, task.authCart.KafkaConcurrency, and task.generateOrder.KafkaConcurrency in MessageSubscribeModule.java, creating a single-worker bottleneck during batch processing.` Action: `Increase Kafka partition count and consumer concurrency from 1 to 3 for auto-ship-rebalance-event-message, ba-auto-ship-auth-cart-message, and ba-auto-ship-generate-order-message in MessageSubscribeModule.java, matching the concurrency of 3 configured for the sibling notification and customer-auto-ship-event groups.`
  - Actual: Topics `auto-ship-rebalance-event-message`, `ba-auto-ship-auth-cart-message`, and `ba-auto-ship-generate-order-message` already have 3 partitions in production Kafka infrastructure. Concurrency is not hardcoded in Java but loaded from `task.properties`, where it is currently set to 1. [Source: code → infra/prod/app/app-kafka/kafka-topic/topics/blue-apron-topics.json:L267, L273, L357, backend/ba-auto-ship-service/src/main/java/app/ba/autoship/MessageSubscribeModule.java:L94-98, & backend/ba-auto-ship-service/conf/prod/resources/task.properties:L3, L18, L23]
  - Detail: Check 8a/8c WARNING. The action is partially already implemented: all three Kafka topics already have partition count 3 in `blue-apron-topics.json` (partitions are managed in infra GitOps, not Java code). Furthermore, consumer concurrency is not hardcoded in `MessageSubscribeModule.java`, but dynamically injected from `task.properties`. Sizing concurrency to 3 is traceable to existing topic partitions and sibling groups (`notification` and `customer-auto-ship-event`), so only the property updates in `conf/prod/resources/task.properties` remain.

**[FAIL]** Recommendation 6 — Optimize Heavy STRAIGHT_JOIN Reporting Queries and Suppressed Scans (Medium Priority)
  - Reported: Finding: `BOBAAutoShipCartItemQueryService.cartItemStatistics runs 4 parallel STRAIGHT_JOIN queries across auto_ship_carts (1,376,051 rows) and auto_ship_cart_items (1,979,941 rows) without multi-column covering indexes, while BAAutoShipMigrationService.getUnviewedQuantity explicitly wraps an unindexed join with Database.suppressSlowSQLWarning(true) [Source: context → code-context.md], contributing to MySQL replica lag averaging 18.50 s [Source: Datadog MCP → query_metrics("max:azure.dbformysql_flexibleservers.replication_lag{name:rfprodv2-ba-consumer-db-*}.rollup(max, 86400)")].` Action: `Add composite indexes on auto_ship_carts (cycle_id, status, viewed, item_build_type) and auto_ship_cart_items (auto_ship_cart_id, menu_item_id, fulfillment_center_code), and pre-calculate cycle-level demand statistics asynchronously into customer_ba_subtotals_and_savings or Redis.`
  - Actual: Table `auto_ship_carts` already has composite index `ix_cycle_status_viewed_build_fc (cycle_id, status, viewed, item_build_type, fulfillment_center_code, id)`. Column `fulfillment_center_code` does not exist on table `auto_ship_cart_items` (schema error). Table `customer_ba_subtotal_and_savings` is a per-user financial ledger, not a cycle demand cache. Read queries do not cause MySQL replication lag. [Source: code → backend/ba-auto-ship-service-db-migration/src/main/resources/db/migration/R__auto_ship_carts.sql:L49, backend/ba-auto-ship-service-db-migration/src/main/resources/db/migration/R__auto_ship_cart_items.sql:L1-26, backend/ba-auto-ship-service/src/main/java/app/ba/autoship/domain/mysql/CustomerBASubtotalAndSaving.java:L15-38, & backend/ba-auto-ship-service/src/main/java/app/ba/autoship/service/query/BOBAAutoShipCartItemQueryService.java:L33-47]
  - Detail: Check 8a/8c/8d/8f FAIL. This recommendation fails on multiple grounds:
  1) The proposed composite index on `auto_ship_carts (cycle_id, status, viewed, item_build_type)` already exists in `R__auto_ship_carts.sql` as `ix_cycle_status_viewed_build_fc` (Check 8c).
  2) The proposed index on `auto_ship_cart_items (auto_ship_cart_id, menu_item_id, fulfillment_center_code)` is invalid because `fulfillment_center_code` is a column on `auto_ship_carts`, not `auto_ship_cart_items`; applying this DDL would fail with MySQL error 1072 (Check 8a).
  3) Pre-calculating cycle demand into `customer_ba_subtotals_and_savings` is conceptually invalid; that table stores user-level total savings (`otp_item_subtotal`, `auto_ship_total_savings`) keyed by `user_id` (Check 8a).
  4) Attributing MySQL replication lag (`18.50 s`) to read-only `SELECT STRAIGHT_JOIN` queries is architecturally false in MySQL (reads do not generate binlog events) and conflates server-wide flexible server metrics with service-specific behavior (Check 8f).

**[PASS]** Recommendation 7 — Expand HPA Maximum Replicas for Traffic Burst Headroom (Low Priority)
  - Reported: Finding: `In infra/prod/app/baconsumer/kube/resource/110-hpa.yml, HPA is configured with minReplicas: 3 and maxReplicas: 5. Average replica count is 3.00 and peak replica count reached 4 [Source: Datadog MCP ...], yielding scaling headroom (5 - 3.00) / 5 = 40.00% (YELLOW threshold 25% – 50%) and leaving only 1 replica of headroom during 6.19x peak traffic surges.` Action: `Increase maxReplicas from 5 to 8 in infra/prod/app/baconsumer/kube/resource/110-hpa.yml, raising scaling headroom to (8 - 3.00) / 8 = 62.50% to comfortably handle projected +1Q and +2Q surge loads without pod exhaustion.`
  - Actual: HPA configuration specifies `minReplicas: 3` and `maxReplicas: 5`; raising `maxReplicas` to 8 brings scaling headroom to 62.50% (> 50% GREEN threshold) and matches sibling API service `ba-mobile-api`. [Source: code → infra/prod/app/baconsumer/kube/resource/110-hpa.yml:L113, L348-349]
  - Detail: Check 8a–8f PASS. Premise is verified in `110-hpa.yml`. Current headroom of 40.00% is accurately evaluated as YELLOW under the rubric. Increasing `maxReplicas` to 8 moves headroom to 62.50% (GREEN), moves capacity in the right direction, and is traceable to existing sibling configurations (`ba-mobile-api` in `110-hpa.yml:L113`). Low Priority is proportional given low CPU consumption.

### Summary
Check 8 failed due to fundamental technical inaccuracies in Recommendation 6 and Executive Summary Finding 4, which propose creating an invalid index referencing a non-existent column, recommend caching cycle demand into an unrelated per-user financial ledger, and attribute shared MySQL replica lag to read-only SELECT queries. Recommendations 2, 3, 4, and 7 are fully verified and sound, while Recommendations 1 and 5 contain partially implemented actions, minor naming discrepancies, and shared-resource metric attribution.

---

## Overall Validation Verdict: FAIL

### Critical Issues

- **[Check 1]** MySQL High Availability Mode (rfprodv2-ba-consumer-db)
- **[Check 1]** MySQL Read Replica High Availability
- **[Check 2]** Kafka Consumer Lag (auto-ship-rebalance-event-message)
- **[Check 8]** Executive Summary Finding 4 — Elevated MySQL Replication Lag and Unindexed Scans
- **[Check 8]** Recommendation 6 — Optimize Heavy STRAIGHT_JOIN Reporting Queries and Suppressed Scans (Medium Priority)

### Warnings

- **[Check 2]** MongoDB Atlas Query Targeting Ratio Peak
- **[Check 8]** Executive Summary Finding 5 — Atlas MongoDB Secondary Query Scan Inefficiencies
- **[Check 8]** Recommendation 1 — Implement Data Archival and Partitioning for Fulfillment Events (High Priority)
- **[Check 8]** Recommendation 5 — Increase Kafka Consumer Concurrency for Batch and Rebalance Topics (Medium Priority)


---
*Validation performed by three independent sessions — one re-querying Datadog MCP and Azure MCP
and the infra repository, one re-checking the report against itself, one re-verifying each
recommendation against the application source — against the claims in the original report.*
*Metric tolerances applied: ±20% for 28-day averages, ±30% for peaks (rolling window drift).*
*Config tolerances: exact match for static config; ±10 GB or ±10% for dynamic storage values.*
*The summary table and the verdict above are computed from the findings, not asserted beside them.*
