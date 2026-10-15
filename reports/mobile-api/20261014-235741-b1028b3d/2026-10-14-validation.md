# Validation Report: mobile-api (prod)
**Original Report**: `/Users/albertke/IdeaProjects/03Wonder/ai-scalability-review/reports/mobile-api/20261014-235741-b1028b3d/2026-10-14-review.md`
**Original Report Date**: 2026-10-14
**Validation Run Date**: 2026-10-15
**Validator**: gemini-3.5-flash

---

## Validation Summary

| # | Check | Status | Issues |
|---|-------|--------|--------|
| 1 | Config Accuracy | PASS | 0 issue(s) |
| 2 | Metric Numerical Fidelity | WARNING | 2 issue(s) |
| 3 | Cross-Source Metric Consistency | PASS | 0 issue(s) |
| 4 | Scoring Accuracy | PASS | 0 issue(s) |
| 5 | Capacity Projection Math | PASS | 0 issue(s) |
| 6 | NOT_COLLECTED Authenticity | PASS | 0 issue(s) |
| 7 | Format & Attribution Completeness | PASS | 0 issue(s) |
| 8 | Recommendation Soundness | FAIL | 9 issue(s) |

---

## Check 1 — Config Accuracy: PASS

### Findings

**[PASS]** AKS Node Pool (Primary Deployment)
  - Reported: `apppoolv5` (`Standard_D16s_v4`: 16 vCPU, 64GB RAM)
  - Actual: `apppoolv5` (`Standard_D16s_v4`: 16 vCPU, 64GB RAM) [Source: Azure MCP → aks_nodepool_get(cluster="RFProdV2AKSCluster", nodepool="apppoolv5", resource-group="RFProdV2"); code → infra/prod/app/infra/env/12-aks.tf:L93-96]
  - Detail: Exact match. Live AKS node pool returns vmSize Standard_D16s_v4 (16 vCPU, 64GB RAM), matching Terraform configuration.

**[PASS]** AKS Node Pool Scaling (Primary Deployment)
  - Reported: Min 18 / Max 42 nodes
  - Actual: Min 18 / Max 42 nodes (current count: 37, auto-scaling enabled) [Source: Azure MCP → aks_nodepool_get(cluster="RFProdV2AKSCluster", nodepool="apppoolv5", resource-group="RFProdV2"); code → infra/prod/app/infra/env/12-aks.tf:L97-98]
  - Detail: Exact match. Both live cluster query and Terraform manifest specify minCount 18 and maxCount 42.

**[PASS]** K8s Pod CPU Requests (Primary Deployment)
  - Reported: CPU: `2`
  - Actual: CPU: `2` [Source: code → infra/prod/app/consumer/kube/resource/55-mobile-api.yml:L99]
  - Detail: Exact match.

**[PASS]** K8s Pod CPU Limits (Primary Deployment)
  - Reported: CPU: `4`
  - Actual: CPU: `4` [Source: code → infra/prod/app/consumer/kube/resource/55-mobile-api.yml:L102]
  - Detail: Exact match.

**[PASS]** K8s Pod Memory Requests (Primary Deployment)
  - Reported: Memory: `4Gi`
  - Actual: Memory: `4Gi` [Source: code → infra/prod/app/consumer/kube/resource/55-mobile-api.yml:L100]
  - Detail: Exact match.

**[PASS]** K8s Pod Memory Limits (Primary Deployment)
  - Reported: Memory: `8Gi`
  - Actual: Memory: `8Gi` [Source: code → infra/prod/app/consumer/kube/resource/55-mobile-api.yml:L103]
  - Detail: Exact match.

**[PASS]** HPA Settings (Primary Deployment)
  - Reported: Min 5 / Max 15 (Target: external datadogmetric@prod-consumer:mobile-api-datadog-metric averageValue 60, resource cpu averageUtilization 50%)
  - Actual: Min 5 / Max 15 (Target: external datadogmetric@prod-consumer:mobile-api-datadog-metric averageValue 60, resource cpu averageUtilization 50%) [Source: code → infra/prod/app/consumer/kube/resource/200-consumer-hybrid-hpa.yml:L24-40]
  - Detail: Exact match for minReplicas (5), maxReplicas (15), external metric target (60), and CPU target (50%).

**[PASS]** MySQL Host & Compute (Primary Deployment)
  - Reported: `rfprodv2-flexible-wonder-db` (`Standard_E16ds_v4`: 16 vCPU, 128GB RAM)
  - Actual: `rfprodv2-flexible-wonder-db` (`Standard_E16ds_v4`: 16 vCPU, 128GB RAM) [Source: Azure MCP → mysql_server_config_get(server="rfprodv2-flexible-wonder-db", resource-group="RFProdV2"); code → infra/prod/app/infra/env/16-mysql-flexible-wonder-db.tf:L6-12]
  - Detail: Exact match. Live configuration confirms SKU Standard_E16ds_v4 (16 vCPU, 128GB RAM), matching Terraform resource declaration.

**[PASS]** MySQL Version & HA Mode (Primary Deployment)
  - Reported: 8.4 (HA: SameZone)
  - Actual: 8.4 (HA: SameZone) [Source: Azure MCP → mysql_server_config_get(server="rfprodv2-flexible-wonder-db", resource-group="RFProdV2"); code → infra/prod/app/infra/env/16-mysql-flexible-wonder-db.tf:L13-19]
  - Detail: Exact match. Version is 8.4 and high availability mode is SameZone.

**[PASS]** MySQL Storage & Features (Primary Deployment)
  - Reported: 1592GB (Auto-grow: ON, IO Scaling: ON)
  - Actual: 1671GB (Auto-grow: ON, IO Scaling: ON) [Source: Azure MCP → mysql_server_config_get(server="rfprodv2-flexible-wonder-db", resource-group="RFProdV2"); code → infra/prod/app/infra/env/16-mysql-flexible-wonder-db.tf:L28-33]
  - Detail: Report reflects provisioned storage at time of review generation (1592 GB). Current live storage size is 1671 GB (+79 GB / +4.96% dynamic growth), which is within the ±10% / ±10 GB tolerance for auto-growing storage. Auto-grow and IO scaling are enabled.

**[PASS]** MySQL Read Replica (Primary Deployment)
  - Reported: `rfprodv2-flexible-wonder-db-replica-v4` (HA: OFF)
  - Actual: `rfprodv2-flexible-wonder-db-replica-v4` (HA: OFF) [Source: Azure MCP → mysql_list(resource-group="RFProdV2"); code → infra/prod/app/infra/env/16-mysql-flexible-wonder-db.tf:L204-230]
  - Detail: Exact match. Replica exists in resource group RFProdV2 with high availability omitted (disabled).

**[PASS]** K8s Pod CPU Requests (Secondary Deployment: mobile-api-kafka-consumer)
  - Reported: CPU: `1`
  - Actual: CPU: `1` [Source: code → infra/prod/app/consumer/kube/resource/kafka-consumer/55-mobile-api-kafka-consumer.yml:L101]
  - Detail: Exact match.

**[PASS]** K8s Pod CPU Limits (Secondary Deployment: mobile-api-kafka-consumer)
  - Reported: CPU: `3`
  - Actual: CPU: `3` [Source: code → infra/prod/app/consumer/kube/resource/kafka-consumer/55-mobile-api-kafka-consumer.yml:L103]
  - Detail: Exact match.

**[PASS]** K8s Pod Memory Requests (Secondary Deployment: mobile-api-kafka-consumer)
  - Reported: Memory: `2Gi`
  - Actual: Memory: `2Gi` [Source: code → infra/prod/app/consumer/kube/resource/kafka-consumer/55-mobile-api-kafka-consumer.yml:L102]
  - Detail: Exact match.

**[PASS]** K8s Pod Memory Limits (Secondary Deployment: mobile-api-kafka-consumer)
  - Reported: Memory: `3Gi`
  - Actual: Memory: `3Gi` [Source: code → infra/prod/app/consumer/kube/resource/kafka-consumer/55-mobile-api-kafka-consumer.yml:L104]
  - Detail: Exact match.

**[PASS]** HPA Settings (Secondary Deployment: mobile-api-kafka-consumer)
  - Reported: None (Static replica count = 2)
  - Actual: None (Static replica count = 2) [Source: code → infra/prod/app/consumer/kube/resource/kafka-consumer/55-mobile-api-kafka-consumer.yml:L18]
  - Detail: Exact match. No HPA resource is defined for the deployment, and static replica count is set to 2.

### Summary
All 16 static and live infrastructure configuration properties across primary and secondary deployments match primary source manifests in the infra repository and live Azure resources within allowable tolerances.

---

## Check 2 — Metric Numerical Fidelity: WARNING

### Findings

**[PASS]** Average RPS (Traffic & Throughput)
  - Reported: `62.73`
  - Actual: `62.73` [Source: Datadog MCP → query_metrics("sum:trace.undertow_http.request.hits{env:prod,service:mobile-api}.as_count().rollup(sum, 86400)")]
  - Detail: Exact match. Total request volume across the 28-day window is 151,763,910 hits (5,420,139.64 hits/day / 86400s = 62.733 RPS).

**[WARNING]** Peak RPS (Traffic & Throughput)
  - Reported: `228.62` at 2026-09-21 16:00 UTC
  - Actual: `228.62` at 2026-09-18 22:00 UTC [Source: Datadog MCP → query_metrics("sum:trace.undertow_http.request.hits{env:prod,service:mobile-api}.as_count().rollup(sum, 3600)")]
  - Detail: The numerical peak rate of 228.62 RPS matches the maximum 1-hour rollup bucket (823,017 requests / 3600s = 228.616 RPS) exactly. However, this peak occurred at 2026-09-18 22:00 UTC (epoch timestamp 1789768800000), not at 2026-09-21 16:00 UTC (where volume was 332,611 requests = 92.39 RPS). The date 2026-09-21 corresponds to the worst-day error rate spike, which was incorrectly assigned to the throughput peak.

**[PASS]** Weekly Volumes & WoW Growth (Traffic & Throughput)
  - Reported: Sep 16 - Sep 22: `37,801,945`; Sep 23 - Sep 29: `37,892,620`; Sep 30 - Oct 06: `37,465,101`; Oct 07 - Oct 13: `38,604,244`; WoW growth: `3.04%`
  - Actual: W1: `37,801,945`; W2: `37,892,620`; W3: `37,465,101`; W4: `38,604,244`; WoW growth: `3.04%` [Source: Datadog MCP → query_metrics("sum:trace.undertow_http.request.hits{env:prod,service:mobile-api}.as_count().rollup(sum, 86400)")]
  - Detail: Exact match across all four weekly aggregation buckets and the week-over-week growth rate ((38,604,244 - 37,465,101) / 37,465,101 = +3.0405%).

**[PASS]** P50 Latency 28-day average (Latency Profile)
  - Reported: `44.93 ms`
  - Actual: `44.93 ms` [Source: Datadog MCP → query_metrics("p50:trace.undertow_http.request{env:prod,service:mobile-api}.rollup(avg, 86400)")]
  - Detail: Exact match. The arithmetic mean of the 28 daily P50 points is 0.04493 s.

**[PASS]** P95 Latency 28-day average (Latency Profile)
  - Reported: `622.57 ms`
  - Actual: `622.57 ms` [Source: Datadog MCP → query_metrics("p95:trace.undertow_http.request{env:prod,service:mobile-api}.rollup(avg, 86400)")]
  - Detail: Exact match. The arithmetic mean of the 28 daily P95 points is 0.62257 s.

**[PASS]** P99 Latency 28-day average (Latency Profile)
  - Reported: `1542.34 ms`
  - Actual: `1542.34 ms` [Source: Datadog MCP → query_metrics("p99:trace.undertow_http.request{env:prod,service:mobile-api}.rollup(avg, 86400)")]
  - Detail: Exact match. The arithmetic mean of the 28 daily P99 points is 1.54234 s.

**[PASS]** P99 Latency Peak Observed (Latency Profile)
  - Reported: `1774.69 ms` at 2026-10-04 00:00 UTC
  - Actual: `1774.69 ms` at 2026-10-04 00:00 UTC [Source: Datadog MCP → query_metrics("p99:trace.undertow_http.request{env:prod,service:mobile-api}.rollup(max, 86400)")]
  - Detail: Exact match. The maximum daily P99 point reached 1.77469 s on 2026-10-04 00:00 UTC (and tied on 2026-10-06 00:00 UTC).

**[PASS]** HTTP Error Rate 28-day average (Error Rates & Stability)
  - Reported: `0.0010%`
  - Actual: `0.0010%` [Source: Datadog MCP → query_metrics("sum:trace.undertow_http.request.errors{env:prod,service:mobile-api}.as_count().rollup(sum, 86400)")]
  - Detail: Exact match. Total errors across 28 days are 1,477 out of 151,763,910 hits (0.000973%, rounding to 0.0010%).

**[PASS]** HTTP Error Rate Worst Day (Error Rates & Stability)
  - Reported: `0.0103%` on 2026-09-21
  - Actual: `0.0103%` on 2026-09-21 [Source: Datadog MCP → query_metrics("sum:trace.undertow_http.request.errors{env:prod,service:mobile-api}.as_count().rollup(sum, 86400)")]
  - Detail: Exact match. On 2026-09-21, 488 errors occurred across 4,750,926 hits (0.01027%, rounding to 0.0103%).

**[PASS]** Container Restarts & OOMKills (Error Rates & Stability)
  - Reported: `0` restarts, `0` OOMKills
  - Actual: `0` restarts, `0` OOMKills [Source: Datadog MCP → query_metrics("sum:kubernetes.containers.restarts{env:prod,service:mobile-api}.rollup(sum, 86400)")]
  - Detail: Exact match. All 28 daily buckets recorded zero container restarts.

**[PASS]** CPU Average Usage (Resource Saturation)
  - Reported: `0.167 cores` (`4.18%` of limit, `8.35%` of request)
  - Actual: `0.167 cores` (`4.18%` of 4 cores limit, `8.35%` of 2 cores request) [Source: Datadog MCP → query_metrics("avg:kubernetes.cpu.usage.total{env:prod,service:mobile-api}.rollup(avg, 86400)")]
  - Detail: Exact match. 28-day average CPU usage is 167,061,902 nanocores = 0.1671 cores.

**[PASS]** CPU Peak Usage (Resource Saturation)
  - Reported: `2.856 cores` (`71.40%` of limit)
  - Actual: `2.856 cores` (`71.40%` of 4 cores limit) [Source: Datadog MCP → query_metrics("max:kubernetes.cpu.usage.total{env:prod,service:mobile-api}.rollup(max, 86400)")]
  - Detail: Exact match. Peak observed usage is 2,855,742,426 nanocores = 2.8557 cores on 2026-09-17 (71.39% of 4 cores limit).

**[PASS]** Memory Working Set Average (Resource Saturation)
  - Reported: `977.69 MiB` (`11.93%` of 8Gi limit)
  - Actual: `977.69 MiB` (`11.93%` of 8192 MiB limit) [Source: Datadog MCP → query_metrics("avg:kubernetes.memory.working_set{env:prod,service:mobile-api}.rollup(avg, 86400)")]
  - Detail: Exact match. 28-day average working set is 1,025,183,165 bytes = 977.69 MiB.

**[PASS]** Memory Working Set Peak (Resource Saturation)
  - Reported: `2034.12 MiB` (`24.83%` of 8Gi limit)
  - Actual: `2034.12 MiB` (`24.83%` of 8192 MiB limit) [Source: Datadog MCP → query_metrics("max:kubernetes.memory.working_set{env:prod,service:mobile-api}.rollup(max, 86400)")]
  - Detail: Exact match. Peak working set is 2,132,934,656 bytes = 2034.12 MiB observed on 2026-10-09.

**[PASS]** Replica Count (Resource Saturation)
  - Reported: average `5.01`, max `9` of maxReplicas `15`
  - Actual: average `5.01`, max `9` [Source: Datadog MCP → query_metrics("avg:kubernetes_state.deployment.replicas{env:prod,kube_deployment:mobile-api}.rollup(avg, 86400),max:kubernetes_state.deployment.replicas{env:prod,kube_deployment:mobile-api}.rollup(max, 86400)")]
  - Detail: Exact match. 28-day average replica count is 5.01 and maximum pod replica count reached 9 on 2026-09-17.

**[PASS]** MySQL Active Connections (Persistence Layer)
  - Reported: average `928.37` (`33.99%` of max_connections 2731), peak `1169` (`42.80%` of max_connections 2731)
  - Actual: average `928.37`, peak `1169` [Source: Datadog MCP → query_metrics("avg:azure.dbformysql_flexibleservers.active_connections{name:rfprodv2-flexible-wonder-db}.rollup(avg, 86400),max:azure.dbformysql_flexibleservers.active_connections{name:rfprodv2-flexible-wonder-db}.rollup(max, 86400)")]
  - Detail: Exact match. 28-day average active connections is 928.37, and peak connection count is 1169 against configured max_connections of 2731.

**[PASS]** MySQL Slow Queries (Persistence Layer)
  - Reported: `695.79` per day average, worst day `880`
  - Actual: `695.79` per day average, worst day `880` [Source: Datadog MCP → query_metrics("sum:azure.dbformysql_flexibleservers.slow_queries{name:rfprodv2-flexible-wonder-db}.as_count().rollup(sum, 86400)")]
  - Detail: Exact match. 28-day average slow queries is 695.79 per day, with the peak day recording 880 queries on 2026-10-13.

**[PASS]** MySQL Replication Lag (Persistence Layer)
  - Reported: average `340.50 s`, peak `397 s`
  - Actual: average `340.50 s`, peak `397 s` [Source: Datadog MCP → query_metrics("max:azure.dbformysql_flexibleservers.replication_lag{name:rfprodv2-flexible-wonder-db-*}.rollup(max, 86400)")]
  - Detail: Exact match. Mean of daily maximum replication lag is 340.50 s, with a peak of 397 s recorded on 2026-09-21.

**[PASS]** MySQL Storage Used (Persistence Layer)
  - Reported: `1583.59 GB`
  - Actual: `1583.59 GiB` (1,700,365,949,199 bytes) [Source: Datadog MCP → query_metrics("max:azure.dbformysql_flexibleservers.storage_used{name:rfprodv2-flexible-wonder-db}.rollup(max, 86400)")]
  - Detail: Exact numerical match in binary gigabytes (1,700,365,949,199 bytes / 1024^3 = 1583.59 GiB), though annotated with the unit GB in the report.

**[WARNING]** Kafka Consumer Lag (Traffic & Capacity)
  - Reported: Overall average `0.35 msgs`, peak `11 msgs` on topic `customer-updated`; 11 topics detailed in Section 3.6 table
  - Actual: Overall topic average `0.39 msgs`, peak `11 msgs` on topic `customer-updated` [Source: Datadog MCP → query_metrics("max:kafka.consumer_lag{env:prod,consumer_group:mobile-api} by {topic}.rollup(max, 86400)")]
  - Detail: The peak lag of 11 messages and individual topic peak lags match exactly across all 11 topics. The overall average across all 11 topics is 0.39 messages (10.0% variance from reported 0.35 messages, well within ±20% tolerance). However, on individual topic averages, `customer-profile-updated` shows an arithmetic discrepancy: reported `0.32 msgs` reflects the ratio of non-zero lag days (9/28 = 0.32) rather than the true 28-day pointlist mean of 0.54 messages (sum of 15 messages / 28 days).

**[SKIPPED]** MongoDB Operation Latency & Saturation
  - Reported: Omitted from report
  - Actual: N/A [Source: Prompt specification: Atlas MongoDB cluster is null]
  - Detail: The mobile-api service does not use MongoDB. Verification skipped per validation specification.

**[PASS]** Datadog Monitors Status (SLO & Monitors)
  - Reported: None in ALERT or WARN (`service:mobile-api`)
  - Actual: None in ALERT or WARN (2 monitors in OK status) [Source: Datadog MCP → get_monitors(groupStates=["alert", "warn"], tags=["service:mobile-api"])]
  - Detail: Exact match. No monitors targeting service:mobile-api are in ALERT or WARN status.

### Summary
All performance metrics exhibit exceptional numerical fidelity to the live telemetry data with zero discrepancies beyond acceptable tolerances; however, a timestamp attribution error on peak throughput (2026-09-18 22:00 UTC conflated with the 2026-09-21 error rate spike) and an arithmetic methodology variance on individual Kafka topic averages warrant WARNING status.

---

## Check 3 — Cross-Source Metric Consistency: PASS

### Findings

**[PASS]** Average RPS consistency across sections
  - Reported: `62.73`
  - Actual: `62.73` [Source: file → 2026-10-14-review.md: Section 3.1, Section 4 Dimension 1, Section 5]
  - Detail: Stated consistently as `62.73` across Section 3.1 Traffic & Throughput, Section 4 Dimension 1 rationale, and Section 5 Capacity Projections. It was omitted from Executive Summary critical findings in adherence to the top-5 severity constraint.

**[PASS]** Peak RPS consistency across sections
  - Reported: `228.62`
  - Actual: `228.62` [Source: file → 2026-10-14-review.md: Section 3.1, Section 4 Dimension 1]
  - Detail: Peak RPS is cited as `228.62` at 2026-09-21 16:00 UTC consistently in Section 3.1 and Section 4 Dimension 1 rationale, and corresponds to the `3.64x` peak-to-average ratio cited in Section 3.1, Section 4 Dimension 1, and Section 5 binding constraints.

**[PASS]** P99 latency consistency across sections
  - Reported: Average `1542.34 ms`, peak `1774.69 ms`
  - Actual: Average `1542.34 ms`, peak `1774.69 ms` [Source: file → 2026-10-14-review.md: Section 1, Section 3.2, Section 4 Dimension 2, Section 6 Rec 1]
  - Detail: Consistent across all occurrences. Average P99 latency of `1542.34 ms` appears in Executive Summary item 3, Section 3.2, Section 4 Dimension 2 rationale, and Recommendation 1 finding. Peak observed P99 of `1774.69 ms` appears in Section 3.2 and Section 4 Dimension 2 rationale.

**[PASS]** HTTP error rate consistency across sections
  - Reported: Average `0.0010%`, worst day `0.0103%`
  - Actual: Average `0.0010%`, worst day `0.0103%` [Source: file → 2026-10-14-review.md: Section 3.3, Section 4 Dimension 3]
  - Detail: The 28-day average of `0.0010%` and worst single-day error rate of `0.0103%` on 2026-09-21 match identically between Section 3.3 and Section 4 Dimension 3 rationale.

**[PASS]** CPU average percentage of limit consistency across sections
  - Reported: `4.18%` of limit (`0.167 cores`, `8.35%` of request)
  - Actual: `4.18%` of limit (`0.167 cores`, `8.35%` of request) [Source: file → 2026-10-14-review.md: Section 3.4, Section 4 Dimension 4]
  - Detail: Exact match between Section 3.4 and Section 4 Dimension 4 rationale for cores, percentage of limit, and percentage of request.

**[PASS]** MySQL active connection percentage consistency across sections
  - Reported: Average `33.99%` (`928.37`), peak `42.80%` (`1169`)
  - Actual: Average `33.99%` (`928.37`), peak `42.80%` (`1169`) [Source: file → 2026-10-14-review.md: Section 3.5, Section 4 Dimension 5]
  - Detail: Exact match between Section 3.5 and Section 4 Dimension 5 rationale for both average and peak active connection counts and percentages of max_connections (2731).

**[PASS]** Capacity baseline RPS consistency
  - Reported: `62.73`
  - Actual: `62.73` [Source: file → 2026-10-14-review.md: Section 3.1, Section 5]
  - Detail: Baseline RPS in Section 5 Capacity Projections Current (Base) matches Section 3.1 Average RPS identically.

**[PASS]** Capacity baseline weekly requests consistency
  - Reported: `37,939,104`
  - Actual: `37,939,104` [Source: file → 2026-10-14-review.md: Section 3.1, Section 5]
  - Detail: Current (Base) weekly requests of `37,939,104` matches `62.73 RPS × 604,800 s/week` exactly per report template specification, representing the 28-day baseline average weekly volume (4-week mean of 37,940,978 requests) and consistent within 1.7% of the most recent single week (`38,604,244` on Oct 07 - Oct 13).

**[PASS]** Overall score consistency with worst dimension
  - Reported: `RED`
  - Actual: `RED` [Source: file → 2026-10-14-review.md: Section 4, Section 5]
  - Detail: Overall Score in Section 4 is RED, matching the worst dimension scores (Dimensions 2 and 5 are RED), and Section 5 Current (Base) infrastructure status is consistently labeled RED.

### Summary
All metrics cited across multiple sections of the report are completely consistent, with exact alignment across executive summary highlights, performance metrics, scoring rationales, and capacity baseline projections.

---

## Check 4 — Scoring Accuracy: PASS

### Findings

**[PASS]** Dimension 1 — Traffic & Throughput scoring
  - Reported: `YELLOW`
  - Actual: `YELLOW` [Source: Metric Scoring Reference → Dimension 1]
  - Detail: WoW RPS growth of 3.04% is GREEN (< 5%). Kafka consumer lag average of 0.35 msgs is GREEN (≤ 1 msg) and peak lag of 11 msgs is GREEN (≤ 50 msgs). All traffic metrics were collected (GREEN). However, the Peak/Average ratio of 3.64x triggers the YELLOW threshold band (3x – 6x). Under the aggregation rules (no RED, ≥1 YELLOW), the dimension score is correctly evaluated as YELLOW.

**[PASS]** Dimension 2 — Latency & Thread Concurrency scoring
  - Reported: `RED`
  - Actual: `RED` [Source: Metric Scoring Reference → Dimension 2]
  - Detail: Average P95 latency of 622.57 ms exceeds the RED threshold (> 500 ms). Average P99 latency of 1542.34 ms exceeds the RED threshold (> 1.5 s). Outbound HTTP clients have no circuit breaker state machines on critical paths (RED). Multiple downstream clients have read timeouts > 10 s (OrderWebService 15000 ms, GrubhubRestaurantWebService 20000 ms, BODeliveryZoneWebService unconfigured default 20000 ms, Mapbox/Google Maps 25000 ms), matching the RED threshold. Under the aggregation rules, any RED metric establishes a RED dimension score.

**[PASS]** Dimension 3 — Error Rates & Stability scoring
  - Reported: `GREEN`
  - Actual: `GREEN` [Source: Metric Scoring Reference → Dimension 3]
  - Detail: The 28-day average HTTP error rate of 0.0010% is GREEN (< 0.1%). The worst single-day error rate of 0.0103% is GREEN (< 1%). Container restarts (0) and OOMKill events (0) are both GREEN (0). With all metrics in the GREEN band, the dimension score is correctly evaluated as GREEN.

**[PASS]** Dimension 4 — Resource Saturation scoring
  - Reported: `YELLOW`
  - Actual: `YELLOW` [Source: Metric Scoring Reference → Dimension 4]
  - Detail: Average CPU usage (4.18% of limit, 8.35% of request) is GREEN (< 30% limit, < 60% request). Average memory (11.93% of limit) and peak memory (24.83% of limit) are GREEN (< 50% avg, < 70% peak). The HPA effective trigger of 25.00% is GREEN (≤ 75%), and HPA headroom of 66.60% is GREEN (> 50%). However, peak CPU usage of 2.856 cores (71.40% of limit) falls in the YELLOW threshold band (70% – 90%). With one YELLOW metric and zero RED metrics, the dimension score is correctly evaluated as YELLOW.

**[PASS]** Dimension 5 — Persistence Layer scoring
  - Reported: `RED`
  - Actual: `RED` [Source: Metric Scoring Reference → Dimension 5]
  - Detail: Active connections average (33.99%) and peak (42.80%) are GREEN (< 50% avg, < 65% peak). Largest table (sessions: 2.60 GB) is GREEN (< 5 GB) with 0 tables > 10 GB (GREEN). However, average daily slow queries (695.79/day) and worst-day spike (880) exceed RED thresholds (> 50/day, > 200 spike). Replication lag average (340.50 s) and peak (397 s) exceed RED thresholds (> 5 s avg, > 60 s peak). DB storage used (1583.59 GB) exceeds the RED threshold (> 500 GB). Under the aggregation rules, the presence of multiple RED metrics establishes a RED dimension score.

**[PASS]** Overall Score aggregation
  - Reported: `RED`
  - Actual: `RED` [Source: Metric Scoring Reference → Scoring Principles]
  - Detail: Dimensions 2 and 5 score RED, which correctly establishes an Overall Score of RED according to the rule that the overall score equals the worst dimension score.

### Summary
All five scoring dimensions and the overall score strictly adhere to the Metric Scoring Reference thresholds and aggregation rules, with every dimension rationale citing concrete metrics with units and data sources.

---

## Check 5 — Capacity Projection Math: PASS

### Findings

**[PASS]** Baseline weekly requests calculation
  - Reported: `37,939,104`
  - Actual: `37,939,104` [Source: calculation: `62.73 RPS × 604,800 s`]
  - Detail: `62.73 × 604,800 = 37,939,104.00`. The reported baseline weekly volume is mathematically exact to 0.00% discrepancy (within ±1% tolerance).

**[PASS]** Quarterly growth factors (+1Q, +2Q, +4Q)
  - Reported: `+1Q: 1.48x`, `+2Q: 2.18x`, `+4Q: 4.75x`
  - Actual: `+1Q: 1.48x (1.4760)`, `+2Q: 2.18x (2.1785)`, `+4Q: 4.75x (4.7457)` [Source: calculation: `(1 + 0.0304)^weeks`]
  - Detail: Based on the compound weekly growth rate of 3.04%:
    - +1Q (13 weeks): `(1.0304)^13 = 1.4760` (rounded to `1.48x`, 0.27% difference from exact).
    - +2Q (26 weeks): `(1.0304)^26 = 2.1785` (rounded to `2.18x`, 0.07% difference from exact).
    - +4Q (52 weeks): `(1.0304)^52 = 4.7457` (rounded to `4.75x`, 0.09% difference from exact).
    All growth factors match within ±2% tolerance.

**[PASS]** Projected average RPS calculations (+1Q, +2Q, +4Q)
  - Reported: `+1Q: 92.59`, `+2Q: 136.66`, `+4Q: 297.70`
  - Actual: `+1Q: 92.59`, `+2Q: 136.66`, `+4Q: 297.70` [Source: calculation: `62.73 × Growth Factor`]
  - Detail:
    - +1Q: `62.73 × 1.47596 = 92.587` → rounds to `92.59` (exact match).
    - +2Q: `62.73 × 2.17846 = 136.656` → rounds to `136.66` (exact match).
    - +4Q: `62.73 × 4.74569 = 297.700` → rounds to `297.70` (exact match).
    All projected RPS figures match within ±1% tolerance.

**[PASS]** Projected weekly requests calculations (+1Q, +2Q, +4Q)
  - Reported: `+1Q: 55,996,788`, `+2Q: 82,649,298`, `+4Q: 180,049,231`
  - Actual: `+1Q: 55,996,788`, `+2Q: 82,649,298`, `+4Q: 180,049,231` [Source: calculation: `Projected RPS × 604,800 s`]
  - Detail:
    - +1Q: `92.5873 × 604,800 = 55,996,788.49` → rounds to `55,996,788` (exact match).
    - +2Q: `136.6556 × 604,800 = 82,649,298.25` → rounds to `82,649,298` (exact match).
    - +4Q: `297.7004 × 604,800 = 180,049,231.01` → rounds to `180,049,231` (exact match).
    All projected weekly hit counts match within ±1% tolerance.

**[PASS]** Compound growth factor internal consistency
  - Reported: `1.48x`, `2.18x`, `4.75x`
  - Actual: `(1.48)^2 = 2.1904` vs `2.18` (0.48% diff); `(2.18)^2 = 4.7524` vs `4.75` (0.05% diff) [Source: calculation: compound growth consistency]
  - Detail: The relations `(+1Q factor)^2 ≈ +2Q factor` and `(+2Q factor)^2 ≈ +4Q factor` hold within 0.48% and 0.05% respectively, well within the ±3% tolerance.

**[PASS]** Infrastructure status color alignment
  - Reported: `RED` across all rows (Current, +1Q, +2Q, +4Q)
  - Actual: `RED` [Source: file → 2026-10-14-review.md: Section 4 & Section 5]
  - Detail: Current (Base) infrastructure status correctly reflects the Section 4 Overall Score of RED, and all projected quarters maintain RED status given the binding constraints.

### Summary
The Capacity Projections table is mathematically rigorous and fully consistent, with exact adherence to the compound growth model formulas and internal consistency across all projected horizons.

---

## Check 6 — NOT_COLLECTED Authenticity: PASS

### Findings

**[PASS]** SLO Compliance NOT_COLLECTED Authenticity
  - Reported: `NOT_COLLECTED: the configured Datadog MCP server exposes no SLO tool`
  - Actual: Datadog MCP server does not provide SLO inspection tools [Source: Datadog MCP server tool declarations]
  - Detail: An inspection of the Datadog MCP tool catalog confirms that available commands cover incidents, metrics, logs, services, monitors, dashboards, traces, hosts, downtimes, and RUM, but no tool exists for querying Service Level Objectives. The inability to collect is authentic and legitimate.

**[PASS]** MySQL Metrics Collection Completeness
  - Reported: All MySQL metrics collected and analyzed in Section 3.5
  - Actual: Active connections, slow queries, replication lag, and storage used were successfully retrieved and reported for `rfprodv2-flexible-wonder-db` [Source: Datadog MCP & Azure MCP]
  - Detail: The database host rfprodv2-flexible-wonder-db is active, and all required performance and capacity metrics were collected without unjustified omission.

**[PASS]** Kafka Consumer Metrics Collection Completeness
  - Reported: Kafka consumer lag collected and analyzed in Section 3.6 for all 11 topics
  - Actual: Consumer lag metrics for consumer group `mobile-api` were successfully retrieved and reported [Source: Datadog MCP → query_metrics("max:kafka.consumer_lag{env:prod,consumer_group:mobile-api} by {topic}.rollup(max, 86400)")]
  - Detail: Consumer group mobile-api is active, and consumer lag was evaluated across all 11 registered message topics without unjustified omission.

**[SKIPPED]** MongoDB Metrics Omission Authenticity
  - Reported: Omitted from report
  - Actual: N/A [Source: Prompt specification: Atlas MongoDB cluster is null]
  - Detail: Legitimate omission. The mobile-api service does not use MongoDB.

### Summary
The single NOT_COLLECTED claim for Datadog SLO compliance was verified as authentic due to the absence of SLO query tools in the Datadog MCP server, and no applicable metrics were omitted without justification.

---

## Check 7 — Format & Attribution Completeness: PASS

### Findings

**[PASS]** 7a — Section presence and order
  - Reported: All 8 sections present in prescribed order
  - Actual: Header Block, 1. Executive Summary, 2. Infrastructure Configuration, 3. Performance Metrics (Last 28 Days), 4. Scalability Assessment & Scoring, 5. Capacity Projections, 6. Recommendations & Action Items, Footer [Source: file → 2026-10-14-review.md]
  - Detail: All eight mandatory sections appear in the exact required order and numbering.

**[PASS]** 7b — Infrastructure table schema
  - Reported: Primary deployment table with 11 standard rows; secondary deployment table with 5 rows; Atlas MongoDB omitted
  - Actual: All mandatory rows present [Source: file → 2026-10-14-review.md: Section 2]
  - Detail: All required components (AKS Node Pool, AKS Scaling, K8s Pod Requests, K8s Pod Limits, K8s Memory Requests, K8s Memory Limits, HPA Settings, MySQL Host, MySQL Version, Storage, Read Replica) are present. Atlas MongoDB is correctly omitted per format specification since MongoDB is not used. Secondary deployment table is appropriately separated.

**[PASS]** 7c — Source attribution completeness
  - Reported: Zero unattributed raw metric or configuration values
  - Actual: All 16 configuration rows in Section 2, all 23 raw observed metric bullets in Section 3, and the Kafka lag table carry explicit `[Source: ...]` tags; SLO compliance is explicitly marked `NOT_COLLECTED` [Source: file → 2026-10-14-review.md: Sections 2 & 3]
  - Detail: All raw retrieved metrics and infrastructure configuration properties carry primary source annotations. Derived ratio bullets (`Peak / Average ratio`, `Week-over-week average RPS growth`, `HPA effective trigger`) strictly conform to the report template format which specifies them without inline source placeholders.

**[PASS]** 7d — MySQL sub-table presence and accuracy
  - Reported: Sub-table `2.1 MySQL Table Storage (Top 20 by data + index)` with 17 rows
  - Actual: Sub-table present, matching `mysql-tables.md` [Source: file → mysql-tables.md]
  - Detail: The MySQL table storage sub-table is present immediately following the infrastructure table and matches `mysql-tables.md` row-for-row, including table names, row counts, data MB, index MB, and total MB.

**[PASS]** 7e — Weekly volume row count and format
  - Reported: Exactly 4 rows with weekly date ranges (Sep 16 - Sep 22, Sep 23 - Sep 29, Sep 30 - Oct 06, Oct 07 - Oct 13)
  - Actual: Exactly 4 rows with date ranges [Source: file → 2026-10-14-review.md: Section 3.1]
  - Detail: Table conforms exactly to the 4-row specification with date ranges and request counts.

**[PASS]** 7f — Kafka consumer lag section presence
  - Reported: Section 3.6 Kafka Consumer Lag table present
  - Actual: Present with 11 consumer topic rows [Source: file → 2026-10-14-review.md: Section 3.6]
  - Detail: Since consumer group `mobile-api` is configured, Section 3.6 is present and includes all consumer topics with average and peak lag metrics.

**[PASS]** 7g — Scoring dimension heading structure and content
  - Reported: 5 scoring dimensions matching exact headings `### Dimension <n> — <name>: <SCORE>`
  - Actual: 5 dimensions using required names, em dashes, `**Score**` lines, and `**Rationale**` lines citing concrete metric values with units [Source: file → 2026-10-14-review.md: Section 4]
  - Detail: Dimension headings, names, score lines, and rationale blocks adhere strictly to the format requirements without syntax alteration.

**[PASS]** 7h — Capacity projection table row count and periods
  - Reported: Exactly 4 rows: `Current (Base)`, `+1Q (+13 Weeks)`, `+2Q (+26 Weeks)`, `+4Q (+52 Weeks)`
  - Actual: Exactly 4 rows [Source: file → 2026-10-14-review.md: Section 5]
  - Detail: Exactly four rows are present with verbatim period labels and required column schema.

**[PASS]** 7i — Recommendation priority markers, ordering, and fields
  - Reported: 6 recommendations, sorted High (2) → Medium (3) → Low (1)
  - Actual: 6 recommendations with verbatim priority markers `(High Priority)`, `(Medium Priority)`, `(Low Priority)` and complete `Finding`, `Action`, and `Owner` fields [Source: file → 2026-10-14-review.md: Section 6]
  - Detail: Count is ≤ 7, priority markers are verbatim, ordering is strictly descending by priority, and every recommendation contains specific findings and actionable remediation citing source files and code symbols.

**[PASS]** 7j — Absence of invented or unattributed metrics
  - Reported: Zero invented metrics
  - Actual: 100% of numeric metric values are attributed to data sources, derived transparently from sourced values, or marked `NOT_COLLECTED` [Source: file → 2026-10-14-review.md]
  - Detail: No unverified or unsupported numbers appear in the report.

### Summary
The report structure and attribution fully satisfy all formatting, schema, source attribution, and drift-prevention mandates without format violations or invented metrics.

---

## Check 8 — Recommendation Soundness: FAIL

### Findings

**[WARNING]** Executive Summary Finding 1: Absence of Circuit Breakers and Excessive Dependency Timeouts
  - Reported: `The service lacks circuit breaker protection across all critical paths, allowing external client timeouts up to 20000 ms to exhaust Undertow worker threads during downstream degradation.`
  - Actual: Outbound HTTP clients configure timeouts up to 20000 ms on Grubhub services [Source: code → frontend/mobile-api/src/main/java/app/mobile/APIClientModule.java:197-198,210-212] and 15000 ms on OrderWebService [Source: code → frontend/mobile-api/src/main/java/app/mobile/APIClientModuleForOtherTeam.java:90,126], with no Resilience4j or Sentinel circuit breaker state machines. However, core cart services configure 5000 ms timeouts [Source: code → frontend/mobile-api/src/main/java/app/mobile/APIClientModule.java:195,281], and client-side exception fallback handlers with retries are already implemented via `CLBConfig` and `EnhancedAPIConfig` [Source: code → utility/client-side-load-balancing-library/src/main/java/lib/clb/CLBConfig.java; utility/enhanced-api-client-library/src/main/java/app/enhancedapiclient/EnhancedAPIConfig.java].
  - Detail: Premise is partially accurate regarding long timeouts and absence of formal circuit breakers, but overstates risk across "all critical paths" given existing client fallbacks and shorter timeouts (5000 ms) on cart services.

**[FAIL]** Executive Summary Finding 2: Persistence Layer Degradation with Severe Replication Lag
  - Reported: `The shared MySQL cluster exhibits high replication lag peaking at 397 seconds and substantial slow queries, creating severe read consistency risks.`
  - Actual: MySQL replication lag (average 340.50 s, peak 397 s) and slow queries (695.79/day) are server-wide metrics across all hosted schemas on shared database server `rfprodv2-flexible-wonder-db` [Source: Datadog MCP → query_metrics("max:azure.dbformysql_flexibleservers.replication_lag{name:rfprodv2-flexible-wonder-db-*}.rollup(max, 86400)"); query_metrics("sum:azure.dbformysql_flexibleservers.slow_queries{name:rfprodv2-flexible-wonder-db}.as_count().rollup(sum, 86400)")]. `mobile-api` connects exclusively to the primary writer instance and does not configure or read from the read replica `rfprodv2-flexible-wonder-db-replica-v4` [Source: code → frontend/mobile-api/conf/prod/resources/sys.properties; frontend/mobile-api/src/main/java/app/MobileAPIApp.java:186,236].
  - Detail: Factually incorrect assertion of operational risk. Because `mobile-api` reads directly from the primary database cluster and has no read replica routing configured, replication lag on the replica poses zero read consistency risk to `mobile-api`. Furthermore, attributing shared server-wide metrics to `mobile-api` alone violates metric scoping (Test 8f).

**[PASS]** Executive Summary Finding 3: Elevated Tail Latency on Core Endpoints
  - Reported: `Average P95 latency of 622.57 ms and average P99 latency of 1542.34 ms exceed acceptable interactive thresholds for a Tier-1 gateway.`
  - Actual: P95 average latency is `622.57 ms` and P99 average latency is `1542.34 ms` [Source: Datadog MCP → query_metrics("p95:trace.undertow_http.request{env:prod,service:mobile-api}.rollup(avg, 86400)"); query_metrics("p99:trace.undertow_http.request{env:prod,service:mobile-api}.rollup(avg, 86400)")].
  - Detail: Exact match with Datadog 28-day performance history; correctly identifies latency exceeding acceptable gateway threshold limits (> 500 ms P95, > 1.5 s P99).

**[FAIL]** Executive Summary Finding 4: Disabled Kafka Consumers Causing Stale Configuration
  - Reported: `The Kafka consumer module remains disabled across all deployment environments, preventing event-driven cache invalidation and forcing prolonged cache lifetimes.`
  - Actual: Environment variable `APP_ENABLEKAFKACONSUMER` is explicitly set to `"true"` on the dedicated consumer deployment manifest [Source: code → infra/prod/app/consumer/kube/resource/kafka-consumer/55-mobile-api-kafka-consumer.yml:74], and consumer lag metrics verify active processing across all 11 topics in consumer group `mobile-api` [Source: Datadog MCP / Report Section 3.6].
  - Detail: Factually false. The report confuses the local property fallback `app.enableKafkaConsumer=false` in `app.properties` with the production runtime deployment configuration, where it is overridden to `true` on dedicated worker pods. Event-driven cache invalidation is active in production.

**[PASS]** Executive Summary Finding 5: Peak CPU Surges Narrowing Container Headroom
  - Reported: `Peak CPU usage reached 2.856 cores (71.40% of the 4 cores limit), diminishing compute headroom during burst periods.`
  - Actual: Peak CPU usage reached `2.856 cores`, representing 71.40% of the 4 cores limit (requests: 2 cores, limits: 4 cores) [Source: Datadog MCP → query_metrics("max:kubernetes.cpu.usage.total{env:prod,service:mobile-api}.rollup(max, 86400)"); code → infra/prod/app/consumer/kube/resource/55-mobile-api.yml:100-104].
  - Detail: Exact match with Datadog resource metrics and K8s deployment manifests.

**[FAIL]** Recommendation 1: Implement Circuit Breakers and Reduce Downstream Client Timeouts (High Priority)
  - Reported: `In AppCartWebService, OrderWebService.placeOrderV2, GrubhubRestaurantWebService, and GrubhubOrderWebService [Source: code → frontend/mobile-api/src/main/java/com/wonder/mobile/order/OrderModule.java], HTTP clients configure read timeouts between 15000 ms and 20000 ms with 2 retries, without circuit breaker state machines [Source: context → code-context.md]... Action: Implement circuit breakers with fast-fail capability on OrderWebService and GrubhubOrderWebService interfaces, and reduce downstream read timeouts from 15000 ms / 20000 ms to 5000 ms, matching the standard timeout established for CustomerMembershipWebService.`
  - Actual: File path `com/wonder/mobile/order/OrderModule.java` does not exist (actual: `frontend/mobile-api/src/main/java/app/mobile/OrderModule.java`, which configures no HTTP clients) [Source: code → frontend/mobile-api/src/main/java/app/mobile/OrderModule.java]. `AppCartWebService` is configured with `Timeout.MILLISECOND_5000` (5,000 ms), not 15,000–20,000 ms [Source: code → frontend/mobile-api/src/main/java/app/mobile/APIClientModule.java:195,281]. Exception fallbacks are already configured on `OrderWebService` and `GrubhubOrderWebService` [Source: code → frontend/mobile-api/src/main/java/app/mobile/APIClientModule.java:212; frontend/mobile-api/src/main/java/app/mobile/APIClientModuleForOtherTeam.java:126].
  - Detail: Fails Test 8a (Premise). The citation path is incorrect, client configuration is misattributed to `OrderModule`, and the assertion that `AppCartWebService` has a 15,000–20,000 ms timeout is factually false. Furthermore, arbitrarily reducing `OrderWebService` timeout to 5,000 ms risks causing client-side aborts during synchronous multi-service order placement transactions.

**[FAIL]** Recommendation 2: Add Query Pagination and Optimize `sessions` Table Access (High Priority)
  - Reported: `In MigrateNonCityAddressService.java [Source: code → frontend/mobile-api/src/main/java/com/wonder/mobile/migration/service/MigrateNonCityAddressService.java], sessionRepository.select().where("address_id IS NULL").where("city IS NULL").where("state IS NOT NULL").fetch() performs an unbounded table scan against the sessions table... Action: Add explicit batch pagination with limit(300) (matching the batch size standard in MigrateLoginHistoryController.java) to MigrateNonCityAddressService.java, remove Database.suppressSlowSQLWarning(true) calls, and optimize the composite index ix_sessions_user_id_expired_time to prevent slow table scans.`
  - Actual: Cited path `com/wonder/mobile/...` does not exist (actual: `app/mobile/migration/service/MigrateNonCityAddressService.java`). `MigrateNonCityAddressService` is an administrative one-off migration endpoint exposed via `PUT /_app/migrate-non-city-address`, not runtime user traffic [Source: code → frontend/mobile-api/src/main/java/app/mobile/MigrateModule.java:25,34; frontend/mobile-api/src/main/java/app/mobile/migration/web/MigrateNonCityAddressController.java]. Composite index `ix_sessions_user_id_expired_time` already exists on `(user_id, expired_time)` [Source: code → frontend/mobile-api-db-migration/src/main/resources/db/migration/R__sessions.sql:39] and is completely unrelated to the migration query filtering on `address_id`, `city`, and `state`.
  - Detail: Fails Tests 8a, 8c, and proportionality. Proposing to optimize `ix_sessions_user_id_expired_time` to address an `(address_id, city, state)` table scan in an administrative migration script is technically invalid; that index already exists and covers different columns. Removing `Database.suppressSlowSQLWarning` is a cosmetic logging change. Marking an administrative script as High Priority is disproportionate.

**[FAIL]** Recommendation 3: Activate Dedicated Kafka Consumer Deployment for Event-Driven Invalidation (Medium Priority)
  - Reported: `In conf/prod/resources/app.properties [Source: context → code-context.md; infra/prod/app/consumer/kube/resource/kafka-consumer/55-mobile-api-kafka-consumer.yml], app.enableKafkaConsumer is set to false, disabling all 11 event handlers in KafkaConsumerModule... Action: Enable APP_ENABLEKAFKACONSUMER=true specifically on the isolated mobile-api-kafka-consumer deployment (where it is already declared in the container environment manifest) while maintaining consumers disabled on user-facing web pods...`
  - Actual: `APP_ENABLEKAFKACONSUMER="true"` is ALREADY declared and active on the dedicated consumer deployment manifest [Source: code → infra/prod/app/consumer/kube/resource/kafka-consumer/55-mobile-api-kafka-consumer.yml:74], and consumers actively process messages across all 11 topics in production [Source: Report Section 3.6 / Datadog MCP].
  - Detail: Fails Tests 8a and 8c. The proposed action is wholly implemented already. The report explicitly acknowledges that the setting is already declared in the deployment manifest. Recommending a change that is already in force creates noise and invalid advice.

**[FAIL]** Recommendation 4: Migrate Pod-Local Delivery Zone Cache to Distributed Redis (Medium Priority)
  - Reported: `In DeliveryZoneService.java [Source: code → frontend/mobile-api/src/main/java/com/wonder/mobile/zone/service/DeliveryZoneService.java], localCacheStore caches delivery zone names in JVM heap memory with a 30-minute eviction interval... Action: Migrate DeliveryZoneService.localCacheStore to the existing distributed Redis cache infrastructure using cache().add(...), applying a 30-minute TTL and reusing the established redis.timeout configuration.`
  - Actual: Distributed Redis cache for delivery zones is ALREADY implemented via `cache().add(DeliveryZoneCache.class, Duration.ofHours(12))` [Source: code → frontend/mobile-api/src/main/java/app/MobileAPIApp.java:391] and injected into `DeliveryZoneService` as `Cache<DeliveryZoneCache> deliveryZoneCache` [Source: code → frontend/mobile-api/src/main/java/app/mobile/service/address/DeliveryZoneService.java:46,115]. The cited path `com/wonder/mobile/...` does not exist.
  - Detail: Fails Tests 8a and 8c. The proposed action (`cache().add(...)` to Redis) is wholly present already in `MobileAPIApp.java:391`. The local cache is an intentional L1 JVM heap cache in front of L2 Redis to avoid network roundtrips for repeated zone name queries.

**[WARNING]** Recommendation 5: Expand HPA Scaling Headroom for Projected Growth (Medium Priority)
  - Reported: `The HPA resource CPU threshold is configured at averageUtilization: 50 on a request of 2 cores and limit of 4 cores... Peak CPU usage reached 2.856 cores (71.40% of the 4 cores limit)... Projected +2Q peak load reaches 497.44 RPS, which will challenge the maxReplicas: 15 ceiling. Action: Increase HPA maxReplicas from 15 to 25 to ensure adequate scaling headroom under compound quarterly growth while preserving the minReplicas: 5 baseline and the external Datadog throughput target of 60 hits/sec/pod.`
  - Actual: Current HPA configuration specifies `minReplicas: 5`, `maxReplicas: 15`, external throughput target `60 hits/s/pod`, and CPU utilization target `50%` on 2 cores request / 4 cores limit [Source: code → infra/prod/app/consumer/kube/resource/200-consumer-hybrid-hpa.yml:25-44; infra/prod/app/consumer/kube/resource/55-mobile-api.yml:100-104]. Sizing target of 25 is not an existing codebase constant or documented platform default [Source: code → infra/prod/app/consumer/kube/resource/200-consumer-hybrid-hpa.yml].
  - Detail: Premise and direction are sound (increasing maxReplicas provides headroom for projected +2Q peak load of ~20 pods). However, the specific target of 25 is not explicitly derived from a formula or platform convention in the report text (Test 8d).

**[WARNING]** Recommendation 6: Profile Shared MySQL Slow Queries and Optimize Replication Parameters (Low Priority)
  - Reported: `The shared database host rfprodv2-flexible-wonder-db recorded daily slow queries averaging 695.79 queries/day... and replication lag averaging 340.50 s with a peak of 397 s... Action: Partner with the DBA and Platform team to profile the top 10 slow queries across all hosted schemas on rfprodv2-flexible-wonder-db and evaluate replica sync parameters to reduce replication lag to targets below 5 s.`
  - Actual: Slow queries (695.79/day) and replication lag (340.50 s) are cluster-wide metrics across all hosted schemas on shared database host `rfprodv2-flexible-wonder-db` [Source: Datadog MCP → query_metrics("sum:azure.dbformysql_flexibleservers.slow_queries{name:rfprodv2-flexible-wonder-db}.as_count().rollup(sum, 86400)")]. `mobile-api` connects solely to the primary writer instance and does not configure or read from the read replica [Source: code → frontend/mobile-api/conf/prod/resources/sys.properties; frontend/mobile-api/src/main/java/app/MobileAPIApp.java:186,236].
  - Detail: Metric scope is server-wide across all tenants on the shared cluster, not scoped to `mobile-api` (Test 8f). Because `mobile-api` does not query the read replica, replication lag does not directly impact `mobile-api` latency or consistency. The action is appropriately assigned to Platform/DBA with Low Priority, but the premise relies on shared-resource attribution.

### Summary
Check 8 fails due to multiple invalid recommendations and executive summary findings: Recommendations 3 and 4 propose actions that are already implemented in the codebase and infrastructure manifests, Recommendation 1 cites non-existent file paths and misstates client timeouts, Recommendation 2 proposes irrelevant index optimizations for a one-off administrative script, and Executive Summary Finding 2 asserts read consistency risks from replication lag on a read replica that `mobile-api` does not query.

---

## Overall Validation Verdict: FAIL

### Critical Issues

- **[Check 8]** Executive Summary Finding 2: Persistence Layer Degradation with Severe Replication Lag
- **[Check 8]** Executive Summary Finding 4: Disabled Kafka Consumers Causing Stale Configuration
- **[Check 8]** Recommendation 1: Implement Circuit Breakers and Reduce Downstream Client Timeouts (High Priority)
- **[Check 8]** Recommendation 2: Add Query Pagination and Optimize `sessions` Table Access (High Priority)
- **[Check 8]** Recommendation 3: Activate Dedicated Kafka Consumer Deployment for Event-Driven Invalidation (Medium Priority)
- **[Check 8]** Recommendation 4: Migrate Pod-Local Delivery Zone Cache to Distributed Redis (Medium Priority)

### Warnings

- **[Check 2]** Peak RPS (Traffic & Throughput)
- **[Check 2]** Kafka Consumer Lag (Traffic & Capacity)
- **[Check 8]** Executive Summary Finding 1: Absence of Circuit Breakers and Excessive Dependency Timeouts
- **[Check 8]** Recommendation 5: Expand HPA Scaling Headroom for Projected Growth (Medium Priority)
- **[Check 8]** Recommendation 6: Profile Shared MySQL Slow Queries and Optimize Replication Parameters (Low Priority)


---
*Validation performed by three independent sessions — one re-querying Datadog MCP and Azure MCP
and the infra repository, one re-checking the report against itself, one re-verifying each
recommendation against the application source — against the claims in the original report.*
*Metric tolerances applied: ±20% for 28-day averages, ±30% for peaks (rolling window drift).*
*Config tolerances: exact match for static config; ±10 GB or ±10% for dynamic storage values.*
*The summary table and the verdict above are computed from the findings, not asserted beside them.*
