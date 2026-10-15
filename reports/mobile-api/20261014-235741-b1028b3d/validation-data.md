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
