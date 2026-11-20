# Validation Report: wonder-cart-service (prod)
**Original Report**: `/Users/albertke/IdeaProjects/03Wonder/ai-scalability-review/reports/wonder-cart-service/20261120-115024-fdfeb85e/2026-11-20-review.md`
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
| 8 | Recommendation Soundness | WARNING | 5 issue(s) |

---

## Check 1 — Config Accuracy: PASS

### Findings

**[PASS]** 33 items verified: wonder-cart-service AKS node pool SKU, wonder-cart-service AKS scaling, wonder-cart-service CPU requests, wonder-cart-service CPU limits, wonder-cart-service memory requests, wonder-cart-service memory limits, wonder-cart-service HPA min/max/target, wonder-cart-service MySQL host SKU, wonder-cart-service MySQL version/HA, wonder-cart-service MySQL storage, wonder-cart-service MySQL read replica, consumer-wonder-cart-service AKS node pool SKU, consumer-wonder-cart-service AKS scaling, consumer-wonder-cart-service CPU requests, consumer-wonder-cart-service CPU limits, consumer-wonder-cart-service memory requests, consumer-wonder-cart-service memory limits, consumer-wonder-cart-service hybrid HPA min/max/targets, consumer-wonder-cart-service MySQL host SKU, consumer-wonder-cart-service MySQL version/HA, consumer-wonder-cart-service MySQL storage, consumer-wonder-cart-service MySQL read replica, wonder-cart-service-kafka-consumer AKS node pool SKU, wonder-cart-service-kafka-consumer AKS scaling, wonder-cart-service-kafka-consumer CPU requests, wonder-cart-service-kafka-consumer CPU limits, wonder-cart-service-kafka-consumer memory requests, wonder-cart-service-kafka-consumer memory limits, wonder-cart-service-kafka-consumer replica count, wonder-cart-service-kafka-consumer MySQL host SKU, wonder-cart-service-kafka-consumer MySQL version/HA, wonder-cart-service-kafka-consumer MySQL storage, wonder-cart-service-kafka-consumer MySQL read replica
  - Reported: `All infrastructure configuration properties across wonder-cart-service, consumer-wonder-cart-service, and wonder-cart-service-kafka-consumer deployment variants`
  - Actual: `All matching configuration values in repository manifests and live Azure state` [Source: code → infra/prod/app/infra/env/12-aks.tf, infra/prod/app/infra/env/16-mysql-flexible-wonder-db.tf, infra/prod/app/consumer/kube/resource/22-wonder-cart-service.yml, infra/prod/app/consumer/kube/resource/190-hpa.yml, infra/prod/app/consumer/kube/resource/consumer-service/22-consumer-wonder-cart-service.yml, infra/prod/app/consumer/kube/resource/200-consumer-hybrid-hpa.yml, infra/prod/app/consumer/kube/resource/kafka-consumer/22-wonder-cart-service-kafka-consumer.yml, and azure-live.md]
  - Dimension: General
  - Detail: all matched

### Summary
All 33 infrastructure configuration parameters across the core service, consumer variant, and kafka-consumer variant perfectly match their respective Kubernetes manifests, Terraform definitions, and live Azure state.

---

## Check 2 — Metric Numerical Fidelity: PASS

### Findings

**[PASS]** 29 items verified: average RPS, peak RPS and timestamp, peak/average RPS ratio, week-over-week RPS growth, weekly request volumes, latency P50 average, latency P95 average, latency P99 average, peak P99 latency and timestamp, HTTP error rate 28-day average, worst day HTTP error rate and timestamp, container restarts, OOMKill events, CPU usage average, CPU usage peak, memory working set average, memory working set peak, replica count average and peak, HPA effective trigger, MySQL active connections average and peak, MySQL slow queries average and worst day, largest table size and tables above 10GB count, top MySQL table storage breakdown, MySQL replication lag average and peak, MySQL storage used, Kafka consumer lag by topic, Latency SLO compliance, Availability SLO compliance, Datadog monitors alert/warn count
  - Reported: `All numerical metric values, timestamps, and derived ratios in Section 3 and Section 2.1`
  - Actual: `All matching metric values in Datadog API collections and MySQL schema statistics` [Source: metrics.md:L1-L105, mysql-tables.md:L1-L24]
  - Dimension: General
  - Detail: all matched

### Summary
All 29 metric values, derived percentages, peak timestamps, weekly volume aggregates, and SLO attainments match the collected metrics exactly within tolerance.

---

## Check 3 — Cross-Source Metric Consistency: PASS

### Findings

**[PASS]** 9 consistency points verified: Avg RPS, Peak RPS, P99 latency, HTTP error rate, CPU avg % of limit, MySQL connection avg %, Capacity baseline RPS, Capacity baseline requests/week, Overall Score
  - Reported: Metric values in Executive Summary, Section 3 tables, Section 4 scoring rationales, and Section 5 capacity baseline
  - Actual: Values match consistently across all report sections [Source: report → 2026-11-20-review.md: Sections 1, 3, 4, 5]
  - Dimension: General
  - Detail: all matched.

### Summary
All metric values cited across the executive summary critical findings, detailed performance tables, scoring rationales, and capacity projection baselines are consistent.

---

## Check 4 — Scoring Accuracy: PASS

### Findings

**[PASS]** 5 scoring dimensions and overall score verified: Dimension 1 (Traffic & Throughput), Dimension 2 (Latency & Thread Concurrency), Dimension 3 (Error Rates & Stability), Dimension 4 (Resource Saturation), Dimension 5 (Persistence Layer), Overall Score
  - Reported: Dimension 1 = GREEN, Dimension 2 = GREEN, Dimension 3 = GREEN, Dimension 4 = GREEN, Dimension 5 = YELLOW, Overall Score = GREEN
  - Actual: Computed scores match reported scores exactly per Metric Scoring Reference thresholds and aggregation rules [Source: report → 2026-11-20-review.md: Section 4]
  - Dimension: General
  - Detail: all matched.

### Summary
All five dimension scores and the overall score accurately reflect the cited metric values and adhere strictly to the threshold definitions and aggregation rules in the Metric Scoring Reference.

---

## Check 5 — Capacity Projection Math: WARNING

### Findings

**[PASS]** 5 projection math items verified: Baseline weekly hits, Growth factor math, Projected RPS, Projected weekly hits, Factor internal consistency
  - Reported: Current (Base) `278813` hits / `0.4610` RPS; +1Q `1.21x` / `338355` hits / `0.5594` RPS; +2Q `1.47x` / `410611` hits / `0.6789` RPS; +4Q `2.17x` / `604708` hits / `0.9998` RPS
  - Actual: Compound growth model calculations (`1.015^weeks` and `RPS × 604,800`) match within ±1% tolerance [Source: report → 2026-11-20-review.md: Section 5]
  - Dimension: General
  - Detail: all matched.

**[WARNING]** Infrastructure Status color for Current (Base)
  - Reported: `GREEN`
  - Actual: `YELLOW` [Source: report → 2026-11-20-review.md: Section 4 (Dimension 5 — Persistence Layer: YELLOW)]
  - Dimension: General
  - Detail: Dimension 5 is scored YELLOW due to shared MySQL slow queries. Check 5 verification rule 6 states that if the service currently scores YELLOW on any dimension, the 'Current (Base)' row must not be labeled GREEN. However, the report labeled it GREEN to equal the Overall Score (GREEN per the aggregation rule), following the Report Format Reference ("The Current (Base) status must equal the Overall Score from Section 5"). This is a minor specification ambiguity between the format reference and Check 5 rule 6.

### Summary
All mathematical calculations for compound growth, factors, RPS projections, and weekly hit volumes are accurate within tolerance. A minor ambiguity exists regarding whether Current (Base) status should reflect the Overall Score (GREEN) or the presence of a YELLOW dimension per Check 5 rule 6.

---

## Check 6 — NOT_COLLECTED Authenticity: PASS

### Findings

**[PASS]** 1 items verified: Collection gaps and NOT_COLLECTED assertions
  - Reported: `No NOT_COLLECTED markers; all applicable metric categories collected and reported`
  - Actual: `Section 10 Collection Gaps: None; all applicable metrics present in collected metrics` [Source: metrics.md:L74]
  - Dimension: General
  - Detail: all matched

**[SKIPPED]** MongoDB metrics
  - Reported: `N/A`
  - Actual: `null` [Source: session configuration → Atlas MongoDB cluster: null, metrics.md:L86-L99]
  - Dimension: Persistence
  - Detail: wonder-cart-service does not use MongoDB; omission of MongoDB metrics is legitimate per session configuration.

### Summary
No false NOT_COLLECTED markers were introduced, all required metrics for active technologies (MySQL, Kafka, Undertow HTTP, Kubernetes) were fully collected and reported, and MongoDB metrics were legitimately omitted.

---

## Check 7 — Format & Attribution Completeness: PASS

### Findings

**[PASS]** 10 format and attribution checks verified: 7a (Section presence and order), 7b (Infrastructure table schema), 7c (Source attribution completeness), 7d (MySQL sub-table presence), 7e (Weekly volume count), 7f (Kafka section presence), 7g (Scoring dimension structure), 7h (Capacity table row count), 7i (Recommendation structure and sort order), 7j (No invented metrics)
  - Reported: Full structural and attribution compliance across all report sections
  - Actual: All 8 required sections present in order, all tables present with required schema, all metrics annotated with sources, and no invented metrics found [Source: report → 2026-11-20-review.md]
  - Dimension: General
  - Detail: all matched.

### Summary
The report strictly complies with all format, schema, source attribution, scoring heading, and recommendation structure requirements specified in the Report Format Reference.

---

## Check 8 — Recommendation Soundness: WARNING

### Findings

**[PASS]** 3 items verified: Executive Summary Bullet 2 (Unindexed Cart Cleanup Database Scans), Executive Summary Bullet 4 (Unbounded Cart Item Options Query), Executive Summary Bullet 5 (Unindexed Table Scan Suppression in Cart Search)
  - Reported: `CleanDataMessageHandler unindexed batch cleanup scans on carts, web_carts, and cart_carry_over_histories; CartContextService.cartItemOptionMap unbounded owner-scoped options retrieval; CartService.search and WebCartService.search unindexed count and join operations with Database.suppressSlowSQLWarning(true)`
  - Actual: `CleanDataMessageHandler executes batch cleanup queries on carts, web_carts, and cart_carry_over_histories lacking updated_time indexes; CartContextService.cartItemOptionMap filters cart_menu_item_options only by owner_id and filters cart items in-memory; CartService.search and WebCartService.search execute full table count() and join queries with Database.suppressSlowSQLWarning(true)` [Source: code → backend/wonder-cart-service/src/main/java/app/kafka/CleanDataMessageHandler.java:L40-L64, backend/wonder-cart-service/src/main/java/app/wondercart/service/CartContextService.java:L185-L191, backend/wonder-cart-service/src/main/java/app/wondercart/service/CartService.java:L80-L86, backend/wonder-cart-service/src/main/java/app/webcart/service/WebCartService.java:L86-L92]
  - Dimension: Persistence
  - Detail: all matched

**[WARNING]** Executive Summary Finding 1: Shared MySQL Server Slow Query Volume
  - Reported: `The shared database cluster rfprodv2-flexible-wonder-db recorded a 28-day daily average of 942.21 slow queries with a single-day spike of 1159, driven by database-wide activity and representing the primary persistence layer bottleneck.`
  - Actual: `Daily average 942.21 slow queries and peak 1159 on azure.dbformysql_flexibleservers.slow_queries{name:rfprodv2-flexible-wonder-db} reflect cluster-wide activity across all tenant services hosted on the shared server rfprodv2-flexible-wonder-db` [Source: Datadog API → query-metrics("sum:azure.dbformysql_flexibleservers.slow_queries{name:rfprodv2-flexible-wonder-db}.as_count().rollup(sum, 86400)")]
  - Dimension: Persistence
  - Detail: WARNING — shared-resource metric. The slow query metric is measured at the Azure MySQL Flexible Server level across all tenant databases and services sharing rfprodv2-flexible-wonder-db. Elevating this server-level metric to Critical Finding #1 of wonder-cart-service misattributes a shared-infrastructure bottleneck to this service alone.

**[WARNING]** Executive Summary Finding 3: Shared MySQL Server Replication Lag Spike
  - Reported: `The shared database replica rfprodv2-flexible-wonder-db-replica-v4 experienced a peak replication lag of 2198.00 s on 2026-11-02, creating read-staleness risks on the configured read-replica datasource during server-wide heavy write intervals.`
  - Actual: `Replication lag metric azure.dbformysql_flexibleservers.replication_lag{name:rfprodv2-flexible-wonder-db-*} (28-day average 2.53 s, peak 2198.00 s on 2026-11-02) measures server-wide replication delay on shared replica rfprodv2-flexible-wonder-db-replica-v4` [Source: Datadog API → query-metrics("avg:azure.dbformysql_flexibleservers.replication_lag{name:rfprodv2-flexible-wonder-db-*}.rollup(avg, 86400)")]
  - Dimension: Persistence
  - Detail: WARNING — shared-resource metric. Replication lag on rfprodv2-flexible-wonder-db-replica-v4 is a cluster-level metric driven by all tenants on the database. While the service does define replicaDB pointing to this replica in conf/prod/resources/sys.properties, the lag spike was server-wide and not isolated to wonder-cart-service.

**[WARNING]** Recommendation 1: Add Index on updated_time for Cart Tables (High Priority)
  - Reported: `In CleanDataMessageHandler (backend/wonder-cart-service/src/main/java/app/kafka/CleanDataMessageHandler.java), batch cleanup queries execute SELECT id FROM web_carts WHERE updated_time <= ? LIMIT 100, SELECT id FROM carts WHERE updated_time <= ? LIMIT 100, and SELECT id FROM cart_carry_over_histories WHERE updated_time <= ? LIMIT 100. None of these three tables have an index on updated_time ... Action: Add Flyway migration scripts creating single-column B-tree indexes on updated_time for tables carts, web_carts, and cart_carry_over_histories in backend/wonder-cart-service/src/main/resources/db/migration/`
  - Actual: `Premise verified in CleanDataMessageHandler.java:L40-L64 and migration schemas (R__carts.sql, R__web_carts.sql, R__cart_carry_over_histories.sql), but migration scripts reside in backend/wonder-cart-service-db-migration/src/main/resources/db/migration/, not backend/wonder-cart-service/src/main/resources/db/migration/` [Source: code → backend/wonder-cart-service-db-migration/src/main/resources/db/migration/R__carts.sql:L28, R__web_carts.sql:L28, R__cart_carry_over_histories.sql:L14]
  - Dimension: Persistence
  - Detail: WARNING — cited path not found. The path backend/wonder-cart-service/src/main/resources/db/migration/ does not exist; Flyway migrations are maintained in the sibling module backend/wonder-cart-service-db-migration. Additionally, High Priority is overstated given that the tables are small (all under 40 MB) and cleanup runs in the background via consumer-clean-data with 0 lag.

**[WARNING]** Recommendation 2: Scope Cart Menu Item Options Query by Cart ID (Medium Priority)
  - Reported: `In CartContextService.cartItemOptionMap (backend/wonder-cart-service/src/main/java/app/wondercart/CartContextService.java), option records are retrieved by executing cartMenuItemOptionRepository.select() filtered exclusively by owner_id = ? ... Action: Refactor CartContextService.cartItemOptionMap to filter cart_menu_item_options by both owner_id and the current cart_id ... utilizing the existing composite index cart_id on cart_menu_item_options.`
  - Actual: `Premise verified in CartContextService.java:L185-L191, but file path is backend/wonder-cart-service/src/main/java/app/wondercart/service/CartContextService.java; index ix_cart_menu_item_options_cart_id is a single-column B-tree index on (cart_id), not a composite index` [Source: code → backend/wonder-cart-service/src/main/java/app/wondercart/service/CartContextService.java:L185-L191, backend/wonder-cart-service-db-migration/src/main/resources/db/migration/R__cart_menu_item_options.sql:L19]
  - Dimension: Persistence
  - Detail: WARNING — cited path not found. The cited path backend/wonder-cart-service/src/main/java/app/wondercart/CartContextService.java omits the service subpackage. Furthermore, the action refers to an existing "composite index cart_id", whereas R__cart_menu_item_options.sql defines ix_cart_menu_item_options_cart_id as a single-column index on cart_id. The underlying premise and optimization advice are sound.

**[WARNING]** Recommendation 3: Eliminate Slow SQL Warning Suppression on Cart Search (Low Priority)
  - Reported: `In CartService.search and WebCartService.search (backend/wonder-cart-service/src/main/java/app/wondercart/CartService.java and WebCartService.java), cart search logic executes cartRepository.select().count() (full table scan COUNT(*)) and joins carts with cart items grouped by owner while invoking Database.suppressSlowSQLWarning(true) to silence framework slow query warnings`
  - Actual: `Premise verified in CartService.java:L83-L86 and WebCartService.java:L89-L92, but file paths are backend/wonder-cart-service/src/main/java/app/wondercart/service/CartService.java and backend/wonder-cart-service/src/main/java/app/webcart/service/WebCartService.java` [Source: code → backend/wonder-cart-service/src/main/java/app/wondercart/service/CartService.java:L80-L86, backend/wonder-cart-service/src/main/java/app/webcart/service/WebCartService.java:L86-L92]
  - Dimension: Latency
  - Detail: WARNING — cited path not found. Neither cited path exists at the root specified; CartService.java resides in app/wondercart/service/ and WebCartService.java resides in app/webcart/service/. The underlying finding is factually confirmed: both search endpoints wrap unindexed count() and join queries in Database.suppressSlowSQLWarning(true).

### Summary
All three recommendations and five executive summary critical findings were verified against primary source code, Flyway migrations, and infrastructure metrics. The underlying technical premises are sound and actionable, but warnings are issued for inaccurate source file paths across all recommendations, mislabeling a single-column index as composite in Recommendation 2, overstating priority on table cleanup in Recommendation 1, and elevating cluster-wide shared MySQL server metrics (slow queries and replica lag) to service-level critical findings.

---

## Overall Validation Verdict: PASS

### Dimension Results

**0 of 5 dimensions failed** — 3+ is FAIL, 2 is CONDITIONAL PASS (yellow), 0–1 is PASS (green).

| Dimension | Status | FAIL | WARNING |
|---|---|---|---|
| Traffic & Throughput | PASS | 0 | 0 |
| Latency & Thread Concurrency | WARNING | 0 | 1 |
| Error Rates & Stability | PASS | 0 | 0 |
| Resource Saturation | PASS | 0 | 0 |
| Persistence Layer | WARNING | 0 | 4 |

### Warnings

- **[Check 5]** Infrastructure Status color for Current (Base)
- **[Check 8]** Executive Summary Finding 1: Shared MySQL Server Slow Query Volume
- **[Check 8]** Executive Summary Finding 3: Shared MySQL Server Replication Lag Spike
- **[Check 8]** Recommendation 1: Add Index on updated_time for Cart Tables (High Priority)
- **[Check 8]** Recommendation 2: Scope Cart Menu Item Options Query by Cart ID (Medium Priority)
- **[Check 8]** Recommendation 3: Eliminate Slow SQL Warning Suppression on Cart Search (Low Priority)


---
*Validation performed by three independent sessions — one comparing the report with the collected
Datadog and Azure data and the infra repository, one re-checking the report against itself, one re-verifying each
recommendation against the application source — against the claims in the original report.*
*Metric values are compared with the figures the pipeline collected over the same fixed window: equal after rounding.*
*Config tolerances: exact match for static config.*
*The summary table and the verdict above are computed from the findings, not asserted beside them:
three or more failed dimensions FAIL, two are a yellow, one or none is green.*
