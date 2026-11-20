# Validation Report: wonder-cart-service (prod)
**Original Report**: `/Users/albertke/IdeaProjects/03Wonder/ai-scalability-review/reports/wonder-cart-service/20261120-100857-492f1620/2026-11-20-review.md`
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
| 8 | Recommendation Soundness | PASS | 0 issue(s) |

---

## Check 1 — Config Accuracy: PASS

### Findings

**[PASS]** 33 items verified: wonder-cart-service K8s CPU requests/limits, wonder-cart-service K8s memory requests/limits, wonder-cart-service HPA min/max/target, consumer-wonder-cart-service K8s CPU requests/limits, consumer-wonder-cart-service K8s memory requests/limits, consumer-wonder-cart-service HPA min/max/targets, wonder-cart-service-kafka-consumer K8s CPU requests/limits, wonder-cart-service-kafka-consumer K8s memory requests/limits, wonder-cart-service-kafka-consumer static replica count, AKS cluster node pool SKU, AKS cluster node pool min/max/current scaling, MySQL host name, MySQL host SKU/vCPU/RAM, MySQL version, MySQL storage size and flags, MySQL read replica host
  - Reported: Infrastructure configuration values across all three deployments (wonder-cart-service, consumer-wonder-cart-service, wonder-cart-service-kafka-consumer)
  - Actual: Infrastructure configuration values across all three deployments [Source: azure-live.md; infra/prod/app/consumer/kube/resource/22-wonder-cart-service.yml; infra/prod/app/consumer/kube/resource/consumer-service/22-consumer-wonder-cart-service.yml; infra/prod/app/consumer/kube/resource/kafka-consumer/22-wonder-cart-service-kafka-consumer.yml; infra/prod/app/consumer/kube/resource/190-hpa.yml; infra/prod/app/consumer/kube/resource/200-consumer-hybrid-hpa.yml; infra/prod/app/infra/env/12-aks.tf; infra/prod/app/infra/env/16-mysql-flexible-wonder-db.tf]
  - Dimension: General
  - Detail: all matched

### Summary
All 33 infrastructure configuration parameters across Kubernetes deployments, HPAs, AKS node pool sizing, and Azure MySQL Flexible Server match the manifests and live Azure state exactly.

---

## Check 2 — Metric Numerical Fidelity: PASS

### Findings

**[PASS]** 30 items verified: average RPS, peak RPS and peak timestamp, peak/average RPS ratio, week-over-week RPS growth, 4-week request volumes, latency P50/P95/P99 28-day averages, peak P99 latency and date, HTTP error rate 28-day average and worst single day, container restarts, OOMKill events, CPU usage average and peak (with limit percentages), memory working set average and peak (with limit percentages), replica count average and peak, HPA effective trigger calculation, MySQL active connections average and peak (with max_connections percentages), MySQL max_connections, MySQL slow queries daily average and worst day, MySQL largest table size and tables > 10GB count, MySQL replication lag average and peak, MySQL storage used, Kafka consumer lag averages and peaks across 5 topics, Latency SLO attainment and error budget, Availability SLO attainment and error budget, Datadog monitor alert/warn states, MySQL table storage top 18 tables breakdown
  - Reported: Performance metrics in Section 3 and table storage breakdown in Section 2.1
  - Actual: Performance metrics in Section 1–7, 9 and JSON snapshot [Source: metrics.md]
  - Dimension: General
  - Detail: all matched

### Summary
All numerical metrics, unit conversions, derived ratios/percentages, peak timestamps, and SLO attainments match the collected metrics in metrics.md within tolerance.

---

## Check 3 — Cross-Source Metric Consistency: PASS

### Findings

**[PASS]** 11 metric consistency points verified: Avg RPS, Peak RPS, P99 latency, HTTP error rate, CPU % of limit, MySQL connection %, Capacity baseline RPS, Capacity baseline requests/week, Overall Score, Slow queries, Replication lag
  - Reported: Metric values in Executive Summary, Section 3 (Performance Metrics), Section 4 (Assessment & Scoring), and Section 5 (Capacity Projections)
  - Actual: Identical values across all corresponding sections [Source: /Users/albertke/IdeaProjects/03Wonder/ai-scalability-review/reports/wonder-cart-service/20261120-100857-492f1620/2026-11-20-review.md]
  - Dimension: General
  - Detail: all matched

### Summary
All metrics cited across multiple sections of the report are mutually consistent. Baseline values, latency profiles, error rates, resource utilization, persistence figures, and overall scores align without discrepancy.

---

## Check 4 — Scoring Accuracy: PASS

### Findings

**[PASS]** 6 scoring evaluations verified: Dimension 1 (Traffic & Throughput: GREEN), Dimension 2 (Latency & Thread Concurrency: GREEN), Dimension 3 (Error Rates & Stability: GREEN), Dimension 4 (Resource Saturation: GREEN), Dimension 5 (Persistence Layer: YELLOW), Overall Score (GREEN)
  - Reported: Dimension 1 GREEN, Dimension 2 GREEN, Dimension 3 GREEN, Dimension 4 GREEN, Dimension 5 YELLOW, Overall Score GREEN
  - Actual: Dimension 1 GREEN, Dimension 2 GREEN, Dimension 3 GREEN, Dimension 4 GREEN, Dimension 5 YELLOW, Overall Score GREEN [Source: Metric Scoring Reference]
  - Dimension: General
  - Detail: all matched

### Summary
All 5 scoring dimensions and the overall score accurately follow the Metric Scoring Reference thresholds and aggregation rules based on the cited metric values.

---

## Check 5 — Capacity Projection Math: WARNING

### Findings

**[PASS]** 5 projection math checks verified: Baseline weekly hits, Growth factor math (+1Q, +2Q, +4Q), Projected RPS (+1Q, +2Q, +4Q), Projected weekly hits (+1Q, +2Q, +4Q), Factor internal consistency
  - Reported: Baseline weekly hits `278813`, Growth factors (`1.21x`, `1.47x`, `2.17x`), Projected RPS (`0.5594`, `0.6789`, `0.9998`), Projected hits (`338354`, `410610`, `604709`)
  - Actual: Baseline weekly hits `278812.8` (0.4610 × 604800), Factors `1.21x`, `1.47x`, `2.17x` (compound 1.5%/week), Projected RPS `0.5594`, `0.6789`, `0.9998`, Projected hits `338354`, `410610`, `604709` [Source: /Users/albertke/IdeaProjects/03Wonder/ai-scalability-review/reports/wonder-cart-service/20261120-100857-492f1620/2026-11-20-review.md:L174-L183]
  - Dimension: General
  - Detail: all matched

**[WARNING]** Infrastructure Status color for Current (Base)
  - Reported: `GREEN`
  - Actual: `YELLOW` according to Check 5 Verification 6 ("If the service currently scores YELLOW on any dimension, the 'Current (Base)' row must not be labeled GREEN"), but `GREEN` according to Report Format Reference Section 6 ("The 'Current (Base)' status must equal the Overall Score from Section 5") [Source: /Users/albertke/IdeaProjects/03Wonder/ai-scalability-review/reports/wonder-cart-service/20261120-100857-492f1620/2026-11-20-review.md:L178]
  - Dimension: General
  - Detail: Dimension 5 (Persistence Layer) scored YELLOW due to shared MySQL slow queries. The report labeled Current (Base) status as GREEN in accordance with the Report Format Reference rule tying it to the Overall Score (which is GREEN under multi-dimension aggregation). However, Check 5 Verification 6 specifies that Current (Base) must not be labeled GREEN if any dimension scores YELLOW. Flagged as a specification ambiguity for reviewer attention.

### Summary
All capacity projection formulas, compound growth factors, projected RPS, and weekly hit volumes are mathematically accurate within ±1% tolerance. A minor specification ambiguity exists regarding whether Current (Base) infrastructure status should reflect the overall score (GREEN) or the presence of a YELLOW persistence dimension.

---

## Check 6 — NOT_COLLECTED Authenticity: PASS

### Findings

**[SKIPPED]** MongoDB metrics
  - Reported: `N/A`
  - Actual: `N/A` [Source: prompt configuration (Atlas MongoDB cluster: null)]
  - Dimension: Persistence
  - Detail: wonder-cart-service does not use MongoDB; omission of MongoDB metrics is expected and legitimate.

**[PASS]** 4 items verified: collection gaps alignment, MySQL metrics completeness, Kafka consumer lag metrics completeness, core service telemetry completeness
  - Reported: Zero NOT_COLLECTED declarations; complete coverage of applicable MySQL, Kafka, traffic, latency, error, and resource metrics
  - Actual: Section 10 lists "None" collection gaps; all applicable telemetry present [Source: metrics.md: Section 10]
  - Dimension: General
  - Detail: all matched

### Summary
No collection gaps exist in metrics.md, no unwarranted NOT_COLLECTED assertions were present in the report, and all applicable metrics for the service and its datastores were completely represented.

---

## Check 7 — Format & Attribution Completeness: PASS

### Findings

**[PASS]** 10 format checks verified: 7a (Section presence and order), 7b (Infrastructure table schema), 7c (Source attribution completeness), 7d (MySQL sub-table presence and match), 7e (Weekly volume 4-row count), 7f (Kafka consumer lag section presence), 7g (5 scoring dimension structures and headings), 7h (Capacity projection 4-row count), 7i (Recommendation structure, priority markers, and sort order), 7j (No invented metrics and source coverage)
  - Reported: Structure, tables, dimension headings, attribution tags, and recommendations in 2026-11-20-review.md
  - Actual: Matches all required schemas, counts, heading shapes, and attribution constraints [Source: Report Format Reference; /Users/albertke/IdeaProjects/03Wonder/ai-scalability-review/reports/wonder-cart-service/20261120-100857-492f1620/mysql-tables.md]
  - Dimension: General
  - Detail: all matched

### Summary
The report strictly complies with the required 8-section template structure, dimension heading formats, table row counts, recommendation priority tagging and sorting, and full metric source attribution with zero invented metrics.

---

## Check 8 — Recommendation Soundness: PASS

### Findings

**[PASS]** 6 items verified: Recommendation 1 / Exec Summary 1 (updated_time index for cart cleanup handlers), Recommendation 2 / Exec Summary 2 (slow query warning suppression removal and cart search optimization), Recommendation 3 / Exec Summary 3 (bulk retrieval in courier supply health fetcher), Recommendation 4 (securing bundle item choice migration endpoint), Exec Summary 4 (shared MySQL slow query volume context), Exec Summary 5 (shared MySQL replication lag context)
  - Reported: Recommendations 1–4 in Section 6 and Critical Findings 1–5 in Section 1 (Executive Summary)
  - Actual: Code implementations in CleanDataMessageHandler.java, CartService.java, WebCartService.java, CartServiceHelper.java, WebCartServiceHelper.java, CourierSupplyHealthDataFetcher.java, MigrateCartBundleItemChoiceController.java, CartModule.java, KafkaConsumerModule.java; Flyway migration DDL in R__carts.sql, R__web_carts.sql, R__cart_carry_over_histories.sql, R__cart_bundle_item_choices.sql; table statistics in mysql-tables.md; and K8s manifests in infra/prod [Source: code → backend/wonder-cart-service; backend/wonder-cart-service-db-migration; infra/prod/app/consumer]
  - Dimension: General
  - Detail: all matched

### Summary
All four recommendations and five executive summary critical findings were verified against primary application code, database migration schemas, table statistics, and deployment manifests. All premises, actions, numeric parameters, core-ng framework conventions, and priority classifications are technically sound and fully traceable.

---

## Overall Validation Verdict: PASS

### Dimension Results

**0 of 5 dimensions failed** — 3+ is FAIL, 2 is CONDITIONAL PASS (yellow), 0–1 is PASS (green).

| Dimension | Status | FAIL | WARNING |
|---|---|---|---|
| Traffic & Throughput | PASS | 0 | 0 |
| Latency & Thread Concurrency | PASS | 0 | 0 |
| Error Rates & Stability | PASS | 0 | 0 |
| Resource Saturation | PASS | 0 | 0 |
| Persistence Layer | PASS | 0 | 0 |

### Warnings

- **[Check 5]** Infrastructure Status color for Current (Base)


---
*Validation performed by three independent sessions — one comparing the report with the collected
Datadog and Azure data and the infra repository, one re-checking the report against itself, one re-verifying each
recommendation against the application source — against the claims in the original report.*
*Metric values are compared with the figures the pipeline collected over the same fixed window: equal after rounding.*
*Config tolerances: exact match for static config.*
*The summary table and the verdict above are computed from the findings, not asserted beside them:
three or more failed dimensions FAIL, two are a yellow, one or none is green.*
