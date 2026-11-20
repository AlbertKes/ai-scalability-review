# Validation Report: ba-auto-ship-service (prod)
**Original Report**: `/Users/albertke/IdeaProjects/03Wonder/ai-scalability-review/reports/ba-auto-ship-service/20261120-104138-7d3d276a/2026-11-20-review.md`
**Original Report Date**: 2026-11-20
**Validation Run Date**: 2026-11-20
**Validator**: gemini-3.5-flash

---

## Validation Summary

| # | Check | Status | Issues |
|---|-------|--------|--------|
| 1 | Config Accuracy | PASS | 0 issue(s) |
| 2 | Metric Numerical Fidelity | PASS | 0 issue(s) |
| 3 | Cross-Source Metric Consistency | PASS | 0 issue(s) |
| 4 | Scoring Accuracy | PASS | 0 issue(s) |
| 5 | Capacity Projection Math | FAIL | 1 issue(s) |
| 6 | NOT_COLLECTED Authenticity | WARNING | 1 issue(s) |
| 7 | Format & Attribution Completeness | PASS | 0 issue(s) |
| 8 | Recommendation Soundness | FAIL | 5 issue(s) |

---

## Check 1 — Config Accuracy: PASS

### Findings

**[PASS]** 22 items verified: ba-auto-ship-service K8s CPU requests, ba-auto-ship-service K8s CPU limits, ba-auto-ship-service K8s memory requests, ba-auto-ship-service K8s memory limits, ba-auto-ship-service HPA min/max replicas and CPU target, ba-auto-ship-task-service K8s CPU requests, ba-auto-ship-task-service K8s CPU limits, ba-auto-ship-task-service K8s memory requests, ba-auto-ship-task-service K8s memory limits, ba-auto-ship-task-service static replica count, ba-auto-ship-simulation-service K8s CPU requests, ba-auto-ship-simulation-service K8s CPU limits, ba-auto-ship-simulation-service K8s memory requests, ba-auto-ship-simulation-service K8s memory limits, ba-auto-ship-simulation-service static replica count, AKS apppoolv5 node pool VM size, AKS apppoolv5 autoscaling min/max node count, MySQL host SKU and hardware specs, MySQL engine version, MySQL storage provisioned, MySQL read replica configuration, Atlas MongoDB cluster topology
  - Reported: All infrastructure components and container configurations match specification
  - Actual: All values verified against Kubernetes manifests, Terraform configurations, and Azure live state [Source: code → infra/prod/app/baconsumer/kube/resource/18-ba-auto-ship-service.yml, 18-1-ba-auto-ship-task-service.yml, 18-ba-auto-ship-simulation-service.yml, 110-hpa.yml, infra/prod/app/infra/env/12-aks.tf; Azure MCP → azure-live.md]
  - Dimension: General
  - Detail: All matched.

### Summary
All infrastructure and deployment configuration parameters across the web API, task worker, and simulation profiles—including Kubernetes CPU/memory requests and limits, HPA autoscaling thresholds, Azure MySQL server SKU and storage, and AKS node pool sizing—match primary source manifests and Azure live state exactly.

---

## Check 2 — Metric Numerical Fidelity: PASS

### Findings

**[PASS]** 52 items verified: average RPS, peak RPS, peak RPS timestamp, peak/average RPS ratio, week-over-week RPS growth, 4-week request volumes, P50 latency average, P95 latency average, P99 latency average, peak P99 latency and date, HTTP error rate 28-day average, HTTP error rate worst day and date, container restarts, OOMKill events, CPU usage average and request/limit percentages, CPU usage peak and limit percentage, memory working set average and limit percentage, memory working set peak and limit percentage, deployment replicas average and peak, HPA effective trigger percentage, MySQL active connections average and limit percentage, MySQL active connections peak and limit percentage, MySQL slow queries per day and peak, MySQL largest table size and rows, MySQL tables above 10GB count, MongoDB read latency average, MongoDB read latency peak, MongoDB write latency average, MongoDB peak connections, MongoDB query targeting ratio average and peak, MongoDB database on-disk storage and growth percentage, MongoDB database uncompressed data size, MongoDB database index size and index/data ratio, MongoDB collections count, MongoDB documents count, MySQL replication lag average and peak, MySQL storage used, Atlas MongoDB cluster disk usage and percentage, Kafka consumer lag average and peak across 8 topics, Latency SLO attainment and error budget, Availability SLO attainment and error budget, active monitor alert/warn status
  - Reported: All performance and saturation metrics match telemetry data
  - Actual: All values verified against Datadog metrics and MySQL table statistics [Source: Datadog API & MySQL JDBC → metrics.md, mysql-tables.md]
  - Dimension: General
  - Detail: All matched.

### Summary
All reported numerical metrics across traffic, latency percentiles, error rates, container resources, database connections, MongoDB operations and storage, Kafka consumer lag, and Datadog SLO attainments match the collected telemetry ground truth within 1% tolerance (with exact matches across all rounded figures).

---

## Check 3 — Cross-Source Metric Consistency: PASS

### Findings

**[PASS]** 9 consistency points verified: Avg RPS, Peak RPS, P99 latency, HTTP error rate, CPU avg % of limit, MySQL connection avg %, Capacity baseline RPS, Capacity baseline requests/week, Overall Score
  - Reported: Consistent metric values across Executive Summary, Infrastructure Configuration, Performance Metrics, Scoring rationales, and Capacity Projections
  - Actual: All cited values match across cross-referenced sections [Source: 2026-11-20-review.md]
  - Dimension: General
  - Detail: All 9 cross-source consistency points verified with zero discrepancies. P99 latency (167.72 ms avg / 184.52 ms peak), HTTP error rate (0.0002% avg / 0.0023% worst day), CPU utilization (0.95% limit / 1.89% request), MySQL connections (3.00%), baseline RPS (21.16 vs 21.1647), weekly hits (12800411 = 21.1647 × 604800, consistent with 12778938 recent week), and overall score formula application all match.

### Summary
All metric values and capacity baselines are internally consistent across all sections of the report with zero discrepancies.

---

## Check 4 — Scoring Accuracy: PASS

### Findings

**[PASS]** 5 scoring dimensions verified: Dimension 1 (Traffic & Throughput), Dimension 2 (Latency & Thread Concurrency), Dimension 3 (Error Rates & Stability), Dimension 4 (Resource Saturation), Dimension 5 (Persistence Layer)
  - Reported: Dimension 1 = YELLOW, Dimension 2 = GREEN, Dimension 3 = GREEN, Dimension 4 = GREEN, Dimension 5 = RED; Overall Score = GREEN
  - Actual: Dimension 1 = YELLOW, Dimension 2 = GREEN, Dimension 3 = GREEN, Dimension 4 = GREEN, Dimension 5 = RED; Overall Score = GREEN [Source: Metric Scoring Reference]
  - Dimension: General
  - Detail: All dimension scores match the thresholds and aggregation rules in the Metric Scoring Reference. Dimension 1 is YELLOW due to 5.03x peak/avg ratio and 1756 peak Kafka lag. Dimensions 2, 3, and 4 are GREEN across all metrics. Dimension 5 is RED due to 31.37 GB largest table (> 10 GB), peak MongoDB read latency of 128.87 ms (> 20 ms), and peak query targeting ratio of 2090.72 (> 100). Overall Score is GREEN following the aggregation rule (1 RED, 1 YELLOW < 3 RED, < 2 RED, ≤ 2 YELLOW).

### Summary
All five scoring dimensions and the overall score strictly adhere to the Metric Scoring Reference thresholds and aggregation logic.

---

## Check 5 — Capacity Projection Math: FAIL

### Findings

**[PASS]** Capacity projection mathematical calculations (growth factors, projected RPS, projected weekly hits, factor consistency)
  - Reported: Model = Compound at 1.5% weekly rate; Growth factors: Current (1.00x), +1Q (1.21x), +2Q (1.47x), +4Q (2.17x); Projected Weekly Requests: 12800411, 15534006, 18851314, 27762558; Projected Avg RPS: 21.16, 25.68, 31.17, 45.90
  - Actual: Baseline weekly hits = 12800410.56 (matches 12800411 within ±1%); Factors = 1.00x, 1.2136x, 1.4727x, 2.1689x (all match within ±2%); Projected RPS = 21.16, 25.68, 31.17, 45.90 (all match within ±1%); Projected Hits = 12800411, 15533970, 18851287, 27762470 (all match within ±1%); Internal consistency (1.21² = 1.4641 vs 1.47, diff 0.40% ≤ 3%) [Source: 2026-11-20-review.md → Section 5 & compound growth formula]
  - Dimension: General
  - Detail: All mathematical projections, baseline multiplications, growth factors, and compound calculations are accurate within specified tolerances.

**[FAIL]** Infrastructure Status color in Capacity Projections
  - Reported: `GREEN` for Current (Base), `GREEN` for +1Q (+13 Weeks), `GREEN` for +2Q (+26 Weeks), `GREEN` for +4Q (+52 Weeks)
  - Actual: Must not be labeled `GREEN` when any dimension scores YELLOW or RED; actual infrastructure status is constrained by Dimension 1 (YELLOW) and Dimension 5 (RED) [Source: 2026-11-20-review.md → Section 4 & Section 5]
  - Dimension: General
  - Detail: Check 5 verification rule 6 explicitly specifies: "The status color in the projection table must be consistent with the actual scoring results. If the service currently scores YELLOW on any dimension, the 'Current (Base)' row must not be labeled GREEN." ba-auto-ship-service currently scores YELLOW on Dimension 1 and RED on Dimension 5 (due to a 31.37 GB table and unindexed MongoDB queries). Furthermore, the section's own binding constraint notes that MySQL storage and index bloat threatens disk capacity within +2Q to +4Q. Labeling Current (Base) and all projection intervals as GREEN directly contradicts rule 6 and the actual scoring results.

### Summary
Mathematical calculations for compound growth factors, projected requests, and baseline volumes are accurate within ±1% tolerance. However, the check fails because the Infrastructure Status is labeled GREEN across all projection rows despite the service scoring YELLOW on Traffic & Throughput and RED on Persistence Layer, violating Rule 6.

---

## Check 6 — NOT_COLLECTED Authenticity: WARNING

### Findings

**[WARNING]** MongoDB connections cluster limit ratio
  - Reported: `NOT_COLLECTED: cluster connection limit not provisioned in metrics`
  - Actual: `metrics.md` does not contain the MongoDB cluster connection limit, but Section 10 ("Collection Gaps") lists `None` [Source: Datadog API → metrics.md:Section 5, Section 10]
  - Dimension: Persistence
  - Detail: The MongoDB cluster connection limit was not collected or provisioned in `metrics.md`, so the report accurately avoided fabricating a percentage. However, Section 10 of `metrics.md` lists `None` rather than explicitly enumerating this metric as a collection gap.

**[PASS]** 3 items verified: MySQL telemetry coverage, MongoDB telemetry coverage, Kafka consumer lag coverage
  - Reported: All applicable datastores and message brokers have full metric coverage without unannounced omissions
  - Actual: All expected metrics for rfprodv2-ba-consumer-db, prod-blueapron-main, and ba-auto-ship-service are accounted for [Source: metrics.md]
  - Dimension: General
  - Detail: All matched.

### Summary
All applicable datastores (MySQL, Atlas MongoDB, Kafka) are comprehensively covered in the report without unacknowledged omissions. The single `NOT_COLLECTED` instance accurately reflects that the MongoDB cluster connection limit was absent from the collected metrics, though Section 10 of the metrics file recorded `None` under collection gaps.

---

## Check 7 — Format & Attribution Completeness: PASS

### Findings

**[PASS]** 10 format and attribution checks verified: section presence and order (7a), infrastructure table schema (7b), source attribution completeness (7c), MySQL sub-table presence (7d), weekly volume count (7e), Kafka section presence (7f), scoring dimension structure (7g), capacity table row count (7h), recommendation structure and sort order (7i), no invented metrics (7j)
  - Reported: Complete 8-section report structure conforming to template specifications, all metrics with sources or NOT_COLLECTED, exact table schemas, top 20 MySQL tables matching mysql-tables.md, 4-row weekly volume table, Kafka table present, 5 fixed dimension headings, 4-row capacity projection table, 5 prioritized recommendations with Finding and Action fields, and zero approximation qualifiers
  - Actual: All format requirements, schemas, row counts, heading conventions, attribution tags, and sorting rules match specification [Source: 2026-11-20-review.md & mysql-tables.md]
  - Dimension: General
  - Detail: All structural format, schema completeness, attribution, and drift-prevention checks pass with zero violations.

### Summary
The report strictly adheres to all formatting, attribution, section sequencing, table schemas, and drift-prevention requirements.

---

## Check 8 — Recommendation Soundness: FAIL

### Findings

**[PASS]** 5 items verified: Recommendation 1 (Implement Table Archiving and Purging for Fulfillment Events), Recommendation 5 (Right-Size Container Resource Requests to Reclaim Cluster Capacity), Executive Summary Finding 1 (Unbounded MySQL Fulfillment Event Table Growth), Executive Summary Finding 4 (Elevated Kafka Consumer Lag on Rebalance Events), Executive Summary Finding 5 (Over-Allocated Pod CPU and Memory Requests)
  - Reported: `Claims regarding fulfillment event table growth (>10 GB limit), Kafka consumer lag peaks (1756 msgs), and container CPU/memory request right-sizing`
  - Actual: `Verified against MySQL table telemetry (31.37 GB), Datadog Kafka lag metrics, and K8s deployment manifests (2 CPU / 1.5Gi request vs 0.0378 cores / 543.76 MiB usage)` [Source: mysql-tables.md, metrics.md, and code → infra/prod/app/baconsumer/kube/resource/18-ba-auto-ship-service.yml]
  - Dimension: Persistence
  - Detail: all matched.

**[FAIL]** Executive Summary Finding 2 (Severe MongoDB Query Targeting Spikes on Cleanup)
  - Reported: `Collection cleanup operations trigger extreme query targeting degradation with a peak of 2090.72 documents scanned per returned document due to a dropped timestamp index on the tracking collection.`
  - Actual: `Index on created_time is actively maintained with 90-day TTL; BLUE-6977 dropped the unexpired index specifically to recreate it with expiration in migration Main.java` [Source: code → backend/ba-auto-ship-service-mongo-migration/src/main/java/Main.java:L96 and backend/ba-auto-ship-service-mongo-migration/src/main/java/app/ba/autoship/script/AutoShipCartItemTrackingsScript.java:L13]
  - Dimension: Persistence
  - Detail: Premise contradicted by primary source (Rule 8a). The report claims cleanup operations run unindexed because AutoShipCartItemTrackingsScript.java dropped the timestamp index. However, AutoShipCartItemTrackingsScript only dropped the legacy index created_time_1 as part of ticket BLUE-6977 so Main.java could recreate it with a 90-day TTL (mongo.createIndex("auto_ship_cart_item_trackings", ascending("created_time"), new IndexOptions().expireAfter(90L, TimeUnit.DAYS))). The collection is not unindexed. Furthermore, query targeting ratio (2090.72) is a cluster-level shared metric for prod-blueapron-main, not scoped to this collection or service (Rule 8f).

**[FAIL]** Recommendation 2: Re-establish Missing Index on MongoDB Tracking Collection (High Priority)
  - Reported: `Dropping the created_time index in AutoShipCartItemTrackingsScript.java forces AutoShipCartItemTrackingCleanupService.java to execute unindexed collection scans with Filters.lt("created_time", cutoffTime) across 44536174 documents... Action: Add an explicit index on { created_time: 1 } for collection auto_ship_cart_item_trackings in BAMongoModule.java`
  - Actual: `TTL index on created_time already exists in mongo migration; BAMongoModule does not manage Mongo indexes in core-ng` [Source: code → backend/ba-auto-ship-service-mongo-migration/src/main/java/Main.java:L96 and backend/ba-auto-ship-service/src/main/java/app/ba/autoship/BAMongoModule.java:L35]
  - Dimension: Persistence
  - Detail: Fails 8a (premise contradicted by primary source), 8c (action already implemented), 8f (shared cluster metric), and 8g (wrong framework idiom). Main.java:L96 already creates ascending("created_time") with 90-day expiration; AutoShipCartItemTrackingsScript.java dropped the index only to recreate it with the TTL option. Additionally, 44,536,174 documents is the entire ba-auto-ship database object count, not this collection. In core-ng, Mongo indexes are managed in the companion migration module Main.java, not in BAMongoModule.java.

**[WARNING]** Executive Summary Finding 3 (Transient MongoDB Peak Read Latency Degradation)
  - Reported: `Cluster-level read operations experienced intermittent latency spikes reaching a peak of 128.87 ms during heavy query execution intervals.`
  - Actual: `Shared cluster metric max:mongodb.atlas.oplatencies.reads.avg{clustername:prod-blueapron-main} reflects entire multi-tenant Atlas cluster` [Source: Datadog API → query-metrics("max:mongodb.atlas.oplatencies.reads.avg{clustername:prod-blueapron-main}.rollup(max, 86400)")]
  - Dimension: Persistence
  - Detail: Fails 8f (shared-resource metric). The 128.87 ms read latency spike is a cluster-wide metric for prod-blueapron-main, which hosts multiple consumer domain services. It is presented as a critical finding for ba-auto-ship-service without service-level attribution.

**[WARNING]** Recommendation 3: Eliminate N+1 Entity Loading in Cart Fulfillment Handlers (Medium Priority)
  - Reported: `In BAAutoShipFulfillmentEventController.java, the fulfillment controller loops through autoShipCartIds executing 4 separate queries (cart, cartItems, familyMealChoiceValues, address) in sequential transactions per cart, and BAAutoShipGenerateOrderMessageHandler.java fetches cart items individually via cartItemQueryService.searchByCartId(cartId) rather than batch loading.`
  - Actual: `BAAutoShipFulfillmentEventController loops per cartId (valid N+1); BAAutoShipGenerateOrderMessageHandler is a single-record Kafka handler that does not call cartItemQueryService` [Source: code → backend/ba-auto-ship-service/src/main/java/app/ba/autoship/web/controller/BAAutoShipFulfillmentEventController.java:L43-L52 and backend/ba-auto-ship-service/src/main/java/app/ba/autoship/kafka/BAAutoShipGenerateOrderMessageHandler.java:L23-L28]
  - Dimension: Persistence
  - Detail: Fails 8a in part (partial premise contradiction). While BAAutoShipFulfillmentEventController.java genuinely executes sequential database queries in a loop per cart ID inside individual transactions, BAAutoShipGenerateOrderMessageHandler.java is a standard single-message Kafka consumer processing one order generation task per invocation. It does not loop over carts, nor does it inject or call cartItemQueryService.searchByCartId(cartId). The N+1 finding correctly identifies the issue in the controller, but incorrectly characterizes the Kafka message handler.

**[WARNING]** Recommendation 4: Optimize Correlated Subquery and Heavy Cart Item Aggregations (Medium Priority)
  - Reported: `Refactor searchPendingAuth in AutoShipCartQueryService.java to join against an indexed sub-selection or maintain a indexed status flag on auto_ship_carts, ensuring composite index (auto_ship_cart_id, status) is fully utilized.`
  - Actual: `No composite index (auto_ship_cart_id, status) exists; auto_ship_cart_items has no status column` [Source: code → backend/ba-auto-ship-service-db-migration/src/main/resources/db/migration/R__auto_ship_cart_items.sql:L1-L25 and backend/ba-auto-ship-service/src/main/java/app/ba/autoship/service/query/AutoShipCartQueryService.java:L174]
  - Dimension: Persistence
  - Detail: Discrepancy in proposed action (Rule 8a / Rule 8d). The finding correctly observes that searchPendingAuth uses an EXISTS subquery against auto_ship_cart_items and that aggregateByMenuItem performs a 5-dimension GROUP BY. However, the proposed action specifies 'ensuring composite index (auto_ship_cart_id, status) is fully utilized'. In the primary schema DDL, auto_ship_cart_items has no status column, and no composite index on (auto_ship_cart_id, status) exists on either table. auto_ship_cart_items already indexes auto_ship_cart_id as the leading prefix of ix_cart_id_menu_item_servings_qty.

**[SKIPPED]** Outbound HTTP client timeouts & distributed locking recommendations
  - Reported: `N/A`
  - Actual: `N/A` [Source: 2026-11-20-review.md]
  - Dimension: General
  - Detail: out of scope for now (no recommendations on explicit outbound HTTP client timeouts or distributed locking for periodic background Kafka handlers were included in the report).

### Summary
Check 8 resulted in FAIL due to invalid premises and recommendations regarding MongoDB index management. Recommendation 2 and Executive Summary Finding 2 falsely claimed that the created_time index was dropped leaving the collection unindexed, whereas ba-auto-ship-service-mongo-migration actively creates the TTL index; additionally, core-ng does not configure Mongo indexes in application modules. Recommendation 3 and Recommendation 4 contain valid performance observations (N+1 loading in fulfillment controller, correlated EXISTS subquery) but include inaccurate details (mischaracterizing a single-message Kafka consumer and citing a non-existent composite index).

---

## Overall Validation Verdict: PASS

### Dimension Results

**1 of 5 dimensions failed** — 3+ is FAIL, 2 is CONDITIONAL PASS (yellow), 0–1 is PASS (green).

| Dimension | Status | FAIL | WARNING |
|---|---|---|---|
| Traffic & Throughput | PASS | 0 | 0 |
| Latency & Thread Concurrency | PASS | 0 | 0 |
| Error Rates & Stability | PASS | 0 | 0 |
| Resource Saturation | PASS | 0 | 0 |
| Persistence Layer | FAIL | 2 | 4 |

1 FAIL finding(s) belong to no single dimension (format, scoring math, projection) and do not count toward the verdict; they are listed below.

### Critical Issues

- **[Check 5]** Infrastructure Status color in Capacity Projections
- **[Check 8]** Executive Summary Finding 2 (Severe MongoDB Query Targeting Spikes on Cleanup)
- **[Check 8]** Recommendation 2: Re-establish Missing Index on MongoDB Tracking Collection (High Priority)

### Warnings

- **[Check 6]** MongoDB connections cluster limit ratio
- **[Check 8]** Executive Summary Finding 3 (Transient MongoDB Peak Read Latency Degradation)
- **[Check 8]** Recommendation 3: Eliminate N+1 Entity Loading in Cart Fulfillment Handlers (Medium Priority)
- **[Check 8]** Recommendation 4: Optimize Correlated Subquery and Heavy Cart Item Aggregations (Medium Priority)


---
*Validation performed by three independent sessions — one comparing the report with the collected
Datadog and Azure data and the infra repository, one re-checking the report against itself, one re-verifying each
recommendation against the application source — against the claims in the original report.*
*Metric values are compared with the figures the pipeline collected over the same fixed window: equal after rounding.*
*Config tolerances: exact match for static config.*
*The summary table and the verdict above are computed from the findings, not asserted beside them:
three or more failed dimensions FAIL, two are a yellow, one or none is green.*
