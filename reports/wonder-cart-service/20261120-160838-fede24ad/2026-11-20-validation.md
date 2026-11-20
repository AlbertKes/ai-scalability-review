# Validation Report: wonder-cart-service (prod)
**Original Report**: `/Users/albertke/IdeaProjects/03Wonder/ai-scalability-review/reports/wonder-cart-service/20261120-160838-fede24ad/2026-11-20-review.md`
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
| 5 | Capacity Projection Math | WARNING | 1 issue(s) |
| 6 | NOT_COLLECTED Authenticity | PASS | 0 issue(s) |
| 7 | Format & Attribution Completeness | PASS | 0 issue(s) |
| 8 | Recommendation Soundness | FAIL | 3 issue(s) |

---

## Check 1 — Config Accuracy: PASS

### Findings

**[PASS]** 11 items verified: AKS Node Pool, AKS Scaling, K8s Pod CPU Requests, K8s Pod CPU Limits, K8s Pod Memory Requests, K8s Pod Memory Limits, HPA Settings, MySQL Host, MySQL Version, Storage, Read Replica
  - Reported: `All infrastructure configuration rows match live Azure state and infra repository manifests`
  - Actual: `All infrastructure configuration rows match live Azure state and infra repository manifests` [Source: azure-live.md; infra/prod/app/consumer/kube/resource/22-wonder-cart-service.yml; infra/prod/app/consumer/kube/resource/190-hpa.yml; infra/prod/app/infra/env/16-mysql-flexible-wonder-db.tf]
  - Dimension: General
  - Detail: all matched.

### Summary
All 11 infrastructure configuration parameters from the review report were verified against the infra repository Kubernetes and Terraform manifests and live Azure MCP state with exact fidelity.

---

## Check 2 — Metric Numerical Fidelity: PASS

### Findings

**[PASS]** 28 items verified: Average RPS, Peak RPS, Peak / Average RPS ratio, Week-over-week average RPS growth, Weekly Volumes (4 weeks), P50 latency average, P95 latency average, P99 latency average, Peak P99 latency, HTTP error rate 28-day average, HTTP error rate worst day, Container restarts, OOMKill events, CPU usage average, CPU usage peak, Memory working set average, Memory working set peak, Replica count (average/max), HPA effective trigger, MySQL active connections (average/peak), MySQL slow queries (average/worst day), MySQL largest table, MySQL tables above 10GB, MySQL replication lag (average/peak), MySQL storage used, Kafka consumer lag (5 topics), SLO compliance (Latency & Availability SLOs), Monitors in ALERT/WARN
  - Reported: `All performance metrics in Section 3 match collected Datadog and Azure metrics within tolerance`
  - Actual: `All performance metrics in Section 3 match collected Datadog and Azure metrics within tolerance` [Source: metrics.md → Sections 1-7, 9, 11]
  - Dimension: General
  - Detail: all matched.

### Summary
All 28 numerical metrics and conversions in the performance metrics section match the collected ground-truth Datadog API and Azure data within rounding tolerances, with correct lookback window timestamps and peak dates.

---

## Check 3 — Cross-Source Metric Consistency: PASS

### Findings

**[PASS]** 9 items verified: Average RPS, Peak RPS, P99 latency (avg and peak), HTTP error rate (avg and worst-day), CPU % of limit and request, MySQL connection pool avg and peak %, Capacity baseline RPS, Capacity baseline requests/week, Overall Score aggregation
  - Reported: Metric values and statuses consistent across Executive Summary, Infrastructure Configuration, Performance Metrics, Scoring Rationales, and Capacity Projections
  - Actual: All metric values match identically or adhere to mathematical consistency formulas across cited sections [Source: 2026-11-20-review.md: Sections 1, 2, 3, 4, 5]
  - Dimension: General
  - Detail: all matched

### Summary
All 9 cross-source metric consistency verification points across the report match identically or maintain strict mathematical consistency between metric listings, scoring rationales, and capacity baseline projections.

---

## Check 4 — Scoring Accuracy: PASS

### Findings

**[PASS]** 6 items verified: Dimension 1 (Traffic & Throughput), Dimension 2 (Latency & Thread Concurrency), Dimension 3 (Error Rates & Stability), Dimension 4 (Resource Saturation), Dimension 5 (Persistence Layer), Overall Score Aggregation
  - Reported: Dimension 1 GREEN, Dimension 2 GREEN, Dimension 3 GREEN, Dimension 4 GREEN, Dimension 5 YELLOW, Overall Score GREEN
  - Actual: Dimension 1 GREEN, Dimension 2 GREEN, Dimension 3 GREEN, Dimension 4 GREEN, Dimension 5 YELLOW, Overall Score GREEN [Source: prompt.md → Metric Scoring Reference; 2026-11-20-review.md: Section 4]
  - Dimension: General
  - Detail: all matched

### Summary
All 5 scoring dimensions and the overall score aggregation are mathematically and procedurally accurate against the Metric Scoring Reference thresholds based on the metric values cited in each rationale.

---

## Check 5 — Capacity Projection Math: WARNING

### Findings

**[PASS]** 5 items verified: Baseline weekly hits, Growth factor math (+1Q/+2Q/+4Q), Projected RPS, Projected weekly hits, Factor internal consistency
  - Reported: Baseline weekly hits `278813`, Growth factors (`1.00x`, `1.21x`, `1.47x`, `2.17x`), Projected RPS (`0.4610`, `0.5594`, `0.6789`, `1.0000`), Projected weekly hits (`278813`, `338355`, `410610`, `604710`), Factor internal consistency (1.21² ≈ 1.47, 1.47² ≈ 2.17)
  - Actual: All values match compound model calculations `(1 + 0.015)^n` and baseline formula `Baseline Avg RPS × 604,800` within ±0.42% (well within ±1% to ±3% tolerances) [Source: 2026-11-20-review.md: Section 5; prompt.md → Check 5]
  - Dimension: General
  - Detail: all matched

**[WARNING]** Current (Base) infrastructure status color
  - Reported: `GREEN`
  - Actual: `YELLOW` (per Check 5 rule 6) / `GREEN` (per Report Format Reference Section 6 & Check 3) [Source: prompt.md → Check 5 Rule 6 vs. Report Format Reference Section 6 & Check 3]
  - Dimension: General
  - Detail: Dimension 5 (Persistence Layer) scored YELLOW due to shared MySQL server slow queries. Under Check 5 rule 6, if any dimension scores YELLOW, the Current (Base) row must not be labeled GREEN. However, Report Format Reference Section 6 and Check 3 mandate that Current (Base) status must equal the Section 4 Overall Score, which is GREEN (0 RED, 1 YELLOW). The report correctly adhered to the Section 6 format mandate and Check 3; this warning documents the specification ambiguity between Check 5 rule 6 and the Report Format Reference.

### Summary
All mathematical calculations for baseline volume, compound growth factors, projected RPS, and projected weekly hits are strictly accurate within established tolerances. A minor specification conflict exists between Check 5 rule 6 and Report Format Reference Section 6 regarding whether Current (Base) reflects individual dimension scores (YELLOW) or the Overall Score (GREEN).

---

## Check 6 — NOT_COLLECTED Authenticity: PASS

### Findings

**[PASS]** 4 items verified: No unwarranted NOT_COLLECTED assertions, Applicable MySQL metrics coverage, Applicable Kafka metrics coverage, Collection gaps alignment
  - Reported: `Complete metric coverage without unwarranted NOT_COLLECTED entries`
  - Actual: `Complete metric coverage without unwarranted NOT_COLLECTED entries` [Source: metrics.md → Section 10 Collection Gaps; metrics.md → Sections 1-7, 9]
  - Dimension: General
  - Detail: all matched.

**[SKIPPED]** MongoDB metrics
  - Reported: `N/A`
  - Actual: `N/A` [Source: metrics.md → Section 11 Metrics Snapshot (JSON)]
  - Dimension: Persistence
  - Detail: Service does not use MongoDB (Atlas MongoDB cluster is null). Omission of MongoDB metrics from the review report is legitimate.

### Summary
No collection gaps exist in the primary metrics source and no unwarranted NOT_COLLECTED entries were introduced in the report. All applicable MySQL and Kafka metrics are present, and MongoDB metrics are legitimately omitted as the service does not utilize MongoDB.

---

## Check 7 — Format & Attribution Completeness: PASS

### Findings

**[PASS]** 10 items verified: Section presence and order (7a), Infrastructure table schema (7b), Source attribution completeness (7c), MySQL sub-table presence (7d), Weekly volume count (7e), Kafka section presence (7f), Scoring dimension structure (7g), Capacity table row count (7h), Recommendation structure and sort order (7i), No invented metrics (7j)
  - Reported: Full structural, attribution, schema, and syntax compliance across all report sections
  - Actual: All 8 sections present in required order, all config and metric values attributed with `[Source: ...]`, MySQL sub-table matches `mysql-tables.md`, exactly 4 weekly volumes, Kafka section present, 5 scoring dimensions properly formatted, exactly 4 projection rows, exactly 3 recommendations sorted High-to-Low with required fields, and zero invented metrics [Source: 2026-11-20-review.md; mysql-tables.md; prompt.md → Report Format Reference]
  - Dimension: General
  - Detail: all matched

### Summary
The review report adheres completely to all format, structural, schema, and attribution standards defined in the Report Format Reference, with zero missing sections, zero attribution gaps, and perfect recommendation formatting.

---

## Check 8 — Recommendation Soundness: FAIL

### Findings

**[FAIL]** Recommendation 1 — Replace JVM-Local Semaphore with Redis-Backed Concurrency Limiter
  - Reported: `In SemaphoreExecutorUtil.java, concurrent execution of courier-supply-health-trigger-message tasks is restricted by a static JVM-local Semaphore initialized with app.semaphore.permits = 5 from conf/prod/resources/app.properties (read in src/main/java/app/wondercart/KafkaConsumerModule.java). When HorizontalPodAutoscaler scales the deployment from 2 to 4 pods (infra/prod/app/consumer/kube/resource/190-hpa.yml), aggregate cluster-wide concurrency to downstream delivery supply services expands up to 20 concurrent requests, bypassing intended downstream protection.`
  - Actual: `courier-supply-health-trigger-message is consumed exclusively by deployment wonder-cart-service-kafka-consumer (infra/prod/app/consumer/kube/resource/kafka-consumer/22-wonder-cart-service-kafka-consumer.yml), which defines APP_ENABLEKAFKACONSUMER="true", has fixed replicas: 2, and has no HPA configured. Deployment wonder-cart-service, targeted by HPA in 190-hpa.yml (min 2, max 4), defaults app.enableKafkaConsumer to false (conf/prod/resources/app.properties:L3) and terminates KafkaConsumerModule early (L30), never subscribing to Kafka topics. Concurrency across Kafka consumer pods is statically bounded at 2 pods * 5 permits = 10 permits, cannot expand to 20, and does not autoscale under load.` [Source: code → backend/wonder-cart-service/src/main/java/app/wondercart/KafkaConsumerModule.java:L26-L30, backend/wonder-cart-service/conf/prod/resources/app.properties:L3,L7, infra/prod/app/consumer/kube/resource/kafka-consumer/22-wonder-cart-service-kafka-consumer.yml:L18,L64-L65, infra/prod/app/consumer/kube/resource/190-hpa.yml:L766-L778]
  - Dimension: Latency
  - Detail: Premise contradicted by primary sources (8a, 8h). The report conflates the HTTP API deployment (wonder-cart-service, scaled 2–4 by HPA) with the Kafka consumer deployment (wonder-cart-service-kafka-consumer, static 2 replicas, no HPA). Because consumer pods do not autoscale, aggregate concurrency cannot expand to 20 requests under load. The proposed action (Redis-backed concurrency limiter or dynamic permit adjustment) is based on a non-existent autoscaling risk, and its High Priority rating is ungrounded. Additionally, Redis is initialized in CartServiceApp.java:L37 via SystemModule, not configured in CartModule.java.

**[FAIL]** Executive Summary Finding 2 — In-Process Semaphore Concurrency Expansion
  - Reported: `The JVM-local semaphore governing delivery supply health checks multiplies allowed concurrent calls as pod replicas scale out, risking uncoordinated traffic bursts against downstream supply services during peak load.`
  - Actual: `The Kafka consumer deployment executing delivery supply health checks (wonder-cart-service-kafka-consumer) has a static replica count of 2 with no HPA configured. The deployment configured with HPA (wonder-cart-service, min 2 / max 4) has Kafka message consumption disabled (app.enableKafkaConsumer=false). Pod replicas for message processing do not scale out during peak load.` [Source: code → backend/wonder-cart-service/src/main/java/app/wondercart/KafkaConsumerModule.java:L30, infra/prod/app/consumer/kube/resource/kafka-consumer/22-wonder-cart-service-kafka-consumer.yml:L18,L64-L65, infra/prod/app/consumer/kube/resource/190-hpa.yml:L766-L778]
  - Dimension: Latency
  - Detail: Premise contradicted by primary sources (8a, 8h). Flagged in both Executive Summary and Recommendations per Check 8 guidelines. Message consumer replicas do not scale out with traffic load, so concurrency remains statically bounded at 10 (2 pods * 5 permits) rather than expanding dynamically up to 20.

**[WARNING]** Executive Summary Finding 1 — Shared MySQL Server Slow Query Contention
  - Reported: `The underlying shared database server rfprodv2-flexible-wonder-db recorded an elevated daily slow query volume and a peak single-day surge, presenting external contention risk even though wonder-cart-service queries execute swiftly.`
  - Actual: `Slow queries on shared MySQL server rfprodv2-flexible-wonder-db averaged 942.21/day with a single-day peak of 1159 [Source: Datadog API → query-metrics("sum:azure.dbformysql_flexibleservers.slow_queries{name:rfprodv2-flexible-wonder-db}.as_count().rollup(sum, 86400)")]. wonder-cart-service P99 latency is 1.97 ms and query execution is fast [Source: Datadog API → query-metrics("p99:trace.undertow_http.request{env:prod,service:wonder-cart-service}.rollup(avg, 86400)")].`
  - Dimension: Persistence
  - Detail: Shared-resource metric cited as service-level critical finding (8f). rfprodv2-flexible-wonder-db is a shared database instance supporting multiple services across the consumer domain. The slow queries reflect global server activity across all tenant services rather than wonder-cart-service queries. While the report acknowledges the external nature of the contention and avoids proposing unjustified changes to wonder-cart-service, highlighting a multi-tenant infrastructure metric as a service critical finding blurs the boundary between shared platform maintenance and service-specific scalability risks.

**[SKIPPED]** Outbound HTTP client timeouts and distributed locking for periodic Kafka handlers
  - Reported: `N/A`
  - Actual: `N/A` [Source: N/A]
  - Dimension: General
  - Detail: Out of scope for now per validation instructions. (No recommendations regarding explicit outbound HTTP client timeouts or distributed locking for periodic background Kafka message handlers were included in the report).

**[PASS]** 4 items verified: Recommendation 2 (Add Secondary Indexes on updated_time for Cleanup Queries), Recommendation 3 (Implement Retention Purge for Kiosk POP Cart Tables), Executive Summary Finding 3 (Full Table Scans in Periodic Cleanup), Executive Summary Finding 4 (Unbounded Storage Accumulation in POP Kiosk Carts)
  - Reported: `Recommendation 2 & Finding 3: CleanDataMessageHandler queries carts (43,383 rows), web_carts (32,900 rows), and cart_carry_over_histories (38,180 rows) by updated_time <= ? LIMIT 100 without supporting secondary indexes (Medium Priority). Recommendation 3 & Finding 4: CleanDataMessageHandler lacks retention purge for kiosk POP cart tables (pop_carts: 107 rows, 0.06 MB), causing indefinite accumulation (Low Priority).`
  - Actual: `Recommendation 2 & Finding 3: CleanDataMessageHandler.java (lines 37-58) executes batch cleanup queries filtering by updated_time <= ? LIMIT 100 against carts, web_carts, and cart_carry_over_histories; table schemas in Flyway migrations (R__carts.sql, R__web_carts.sql, R__cart_carry_over_histories.sql) lack indexes on updated_time; row counts match mysql-tables.md; Flyway DDL migration is idiomatic. Recommendation 3 & Finding 4: CleanDataMessageHandler.java (lines 27-35) defines cleanup handlers for app, web, carry-over, and reorder carts but lacks any handler for pop_carts and child tables; pop_carts has 107 rows (0.06 MB) in mysql-tables.md; extending CleanDataMessageHandler with 1-month purge window is idiomatic and traceable.` [Source: code → backend/wonder-cart-service/src/main/java/app/kafka/CleanDataMessageHandler.java:L27-L58, backend/wonder-cart-service-db-migration/src/main/resources/db/migration/R__carts.sql, mysql-tables.md]
  - Dimension: Persistence
  - Detail: all matched. Verified premises, primary source schemas, metrics, implementation absence, direction, scope, framework idioms, and priority proportionality across all 4 items.

### Summary
Check 8 produced 1 WARNING (shared MySQL server slow query contention highlighted as an executive critical finding) and 2 FAIL findings (Recommendation 1 and Executive Summary Finding 2, whose premise that HPA autoscaling scales consumer pods to multiply concurrency up to 20 is contradicted by Kubernetes manifests and core-ng configuration, as Kafka consumers run under a static 2-replica deployment without HPA). The remaining recommendations and executive findings regarding cleanup secondary indexes and POP cart retention purging are verified as sound.

---

## Overall Validation Verdict: PASS

### Dimension Results

**1 of 5 dimensions failed** — 3+ is FAIL, 2 is CONDITIONAL PASS (yellow), 0–1 is PASS (green).

| Dimension | Status | FAIL | WARNING |
|---|---|---|---|
| Traffic & Throughput | PASS | 0 | 0 |
| Latency & Thread Concurrency | FAIL | 2 | 0 |
| Error Rates & Stability | PASS | 0 | 0 |
| Resource Saturation | PASS | 0 | 0 |
| Persistence Layer | WARNING | 0 | 1 |

### Critical Issues

- **[Check 8]** Recommendation 1 — Replace JVM-Local Semaphore with Redis-Backed Concurrency Limiter
- **[Check 8]** Executive Summary Finding 2 — In-Process Semaphore Concurrency Expansion

### Warnings

- **[Check 5]** Current (Base) infrastructure status color
- **[Check 8]** Executive Summary Finding 1 — Shared MySQL Server Slow Query Contention


---
*Validation performed by three independent sessions — one comparing the report with the collected
Datadog and Azure data and the infra repository, one re-checking the report against itself, one re-verifying each
recommendation against the application source — against the claims in the original report.*
*Metric values are compared with the figures the pipeline collected over the same fixed window: equal after rounding.*
*Config tolerances: exact match for static config.*
*The summary table and the verdict above are computed from the findings, not asserted beside them:
three or more failed dimensions FAIL, two are a yellow, one or none is green.*
