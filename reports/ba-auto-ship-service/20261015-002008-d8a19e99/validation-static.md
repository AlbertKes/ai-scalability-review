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
