package app.aiscalabilityreview.prompt;

public class ValidateReportTaskPrompt {
    /**
     * Validation task prompt.
     * Placeholders: {{REPORT_FILE}}, {{SERVICE}}, {{ENV}}, {{NAMESPACE}},
     * {{MYSQL_HOST}}, {{MYSQL_DB}}, {{ATLAS_CLUSTER}}, {{KAFKA_CONSUMER_GROUPS}}
     */
    public static final String CONTENT;

    static {
        CONTENT = """
        # Scalability Review Validation Task
        
        You are a quality-assurance engineer performing an **independent validation** of an existing
        scalability review report. Your objective is to re-query every original data source,
        cross-check every claim in the report, and produce a structured validation report that
        identifies inaccuracies, inconsistencies, math errors, and format violations.
        
        **Important**: Do NOT generate a new scalability review. Your task is strictly to verify
        the existing report and flag any issues found.
        
        - **Report to validate**: {{REPORT_FILE}}
        - **Service**: {{SERVICE}}
        - **Environment**: {{ENV}}
        - **Namespace**: {{NAMESPACE}}
        - **Lookback window**: 28 days (same as report generation)
        - **MySQL host/cluster**: {{MYSQL_HOST}}  (N/A if service does not use MySQL)
        - **MySQL database**: {{MYSQL_DB}}  (N/A if MYSQL_HOST is N/A)
        - **Atlas MongoDB cluster**: {{ATLAS_CLUSTER}}  (N/A if service does not use MongoDB)
        - **Kafka consumer groups**: {{KAFKA_CONSUMER_GROUPS}}  (comma-separated, or "N/A")
        
        ---
        
        ## Finding Format
        
        For every individual item verified, use this format:
        
        ```
        **[STATUS]** <what was checked>
          - Reported: `<value or claim from the report>`
          - Actual: `<value retrieved now>` [Source: <tool call or file path>]
          - Detail: <explanation of pass / discrepancy / skip reason>
        ```
        
        STATUS values:
        - **PASS** — reported value matches actual within tolerance
        - **FAIL** — reported value is incorrect or inconsistent beyond tolerance
        - **WARNING** — minor discrepancy or ambiguity that warrants reviewer attention
        - **SKIPPED** — check not applicable for this service (e.g., MySQL check when MYSQL_HOST is N/A)
        
        ---
        
        ## Check 1 — Config Accuracy
        
        Parse every row of the "Infrastructure Configuration" table from {{REPORT_FILE}}.
        For each row, identify its `[Source: ...]` annotation and re-fetch the value from that source.
        
        ### 1a — Kubernetes / k8s-gitops values (annotated `[Source: code → ...]`)
        
        Re-read the exact files identified in the source annotations.
        
        For each file-sourced config value in the report, compare the report's claim against the
        file content. Verify:
        - CPU requests and limits
        - Memory requests and limits
        - HPA minReplicas, maxReplicas, target metric, and threshold
        - AKS node pool SKU and min/max nodes (from Terraform, if annotated)
        
        **Tolerance for static config**: exact match required — no tolerance for CPU/memory
        requests, limits, or HPA thresholds.
        
        ### 1b — Azure MCP live config values (annotated `[Source: Azure MCP → ...]`)
        
        For each value in the report annotated as `[Source: Azure MCP → <tool>(<args>)]`,
        re-run the identical tool call now. If {{MYSQL_HOST}} is N/A, skip 1b entirely (SKIPPED).
        
        Re-run each of the following that appears in the report:
        - `[Source: Azure MCP → get_mysql_flexible_server(name={{MYSQL_HOST}})]`
        - `[Source: Azure MCP → list_mysql_flexible_server_configurations(server_name={{MYSQL_HOST}})]`
        - `[Source: Azure MCP → list_mysql_flexible_server_replicas(server_name={{MYSQL_HOST}})]`
        - `[Source: Azure MCP → get_aks_node_pools(cluster=<name>, resource_group=<rg>)]`
        
        Compare the reported property value vs. the value returned now.
        
        **Tolerance**:
        - Static config (SKU, vCPU count, HA mode, max_connections): exact match required
        - Storage used (GB): ±10 GB or ±10% is acceptable (data grows between runs)
        
        ---
        
        ## Check 2 — Metric Numerical Fidelity
        
        Parse all metric values from the "Performance Metrics (Last 28 Days)" section of the report.
        For each bullet with a `[Source: Datadog MCP → query_metrics("...")]` annotation,
        extract the exact query string and re-run it now.
        
        Use `env:{{ENV}}` and `kube_namespace:{{NAMESPACE}}` as base filters. Set `from=now-28d`, `to=now`.
        
        **Metrics to re-verify** (run only the queries that are annotated in the report):
        
        - Avg RPS: `sum:trace.undertow_http.request.hits{env:{{ENV}},service:{{SERVICE}}}.as_count()`
        - P50/P95/P99 latency: `p50:trace.undertow_http.request{...}`, `p95:...`, `p99:...`
        - HTTP error rate: `sum:trace.undertow_http.request.errors{...} / sum:trace.undertow_http.request.hits{...}`
        - Container restarts: `sum:kubernetes.containers.restarts{env:{{ENV}},service:{{SERVICE}}}`
        - CPU avg/peak % of limit: `avg:kubernetes.cpu.usage.total{...}` / `max:...`
        - Memory avg/peak % of limit: `avg:kubernetes.memory.working_set{...}` / `max:...`
        - MySQL connections avg/peak: `avg:azure.dbformysql_flexibleservers.active_connections{name:{{MYSQL_HOST}}}` (SKIPPED if N/A)
        - MySQL slow queries: `sum:azure.dbformysql_flexibleservers.slow_queries{...}` (SKIPPED if N/A)
        - Kafka consumer lag: `max:kafka.consumer_lag{...}` (SKIPPED if {{KAFKA_CONSUMER_GROUPS}} is N/A)
        - MongoDB operation latency (SKIPPED if {{ATLAS_CLUSTER}} is N/A)
        
        **Comparison procedure**:
        1. Run the query.
        2. Compute the same summary statistic as the report (avg, max, p99, etc.).
        3. Compare to the reported value.
        
        **Tolerance**:
        - Average metrics (avg RPS, P50, avg CPU, avg connections): ±20% acceptable
        - Peak metrics (max RPS, P99 peak, peak CPU): ±30% acceptable
        - Count / sum metrics (restarts, slow queries total): ±30% acceptable
        - Values differing by >50% or directionally reversed (report says LOW, data shows HIGH): FAIL
        - If report date is >30 days before today: mark all metric re-checks as SKIPPED (temporal drift
          makes exact comparison meaningless); retrieve current values for informational context only.
        
        ---
        
        ## Check 3 — Cross-Source Metric Consistency
        
        Verify that the same metric value is stated consistently wherever it appears in the report.
        No external queries are needed for this check — read {{REPORT_FILE}} only.
        
        **Consistency points to verify**:
        
        | Metric | Location A | Location B | Expected |
        |--------|-----------|-----------|----------|
        | Avg RPS | Executive Summary (critical findings) | Performance Metrics → Average RPS | Values must match |
        | Peak RPS | Executive Summary | Performance Metrics → Peak RPS | Values must match |
        | P99 latency (avg or peak) | Latency Profile → P99 | Dimension 2 scoring rationale | Value cited in rationale must appear in metrics section |
        | HTTP error rate | Error Rates section | Dimension 3 scoring rationale | Value cited in rationale must appear in metrics section |
        | CPU avg % of limit | Resource Saturation → CPU | Dimension 4 scoring rationale | Must match |
        | MySQL connection avg % | DB Saturation → Connection Pool | Dimension 5 scoring rationale | Must match |
        | Capacity baseline RPS | Performance Metrics → Average RPS | Capacity Projections → Baseline | Must match |
        | Capacity baseline hits/week | Weekly Volumes (most recent week) | Capacity Projections → Baseline | Must be consistent |
        
        For each inconsistency, mark FAIL and quote both conflicting values with their exact locations
        (section name and bullet text).
        
        ---
        
        ## Check 4 — Scoring Accuracy
        
        For each of the 5 fixed scoring dimensions, verify that the stated score (GREEN / YELLOW / RED)
        is correct given the metric values cited in the rationale and the thresholds in ai-scalability-review-service/src/main/java/app/aiscalabilityreview/prompt/MetricScorePrompt.java.
        
        **Procedure for each dimension**:
        1. Parse the scoring rationale — identify every specific metric value cited with its unit.
        2. For each cited metric value, find the matching row in ai-scalability-review-service/src/main/java/app/aiscalabilityreview/prompt/MetricScorePrompt.java.
        3. Determine what color (GREEN / YELLOW / RED) that value individually produces.
        4. Apply the aggregation rule from ai-scalability-review-service/src/main/java/app/aiscalabilityreview/prompt/MetricScorePrompt.java:
           - Any RED metric → dimension is RED
           - No RED but ≥1 YELLOW metric → dimension is YELLOW
           - All GREEN → dimension is GREEN
        5. Compare the computed score to the reported score.
        
        **Score tolerance**:
        - Computed score matches reported score → PASS
        - Computed score differs by 1 level (e.g., report YELLOW, computed GREEN) → WARNING with explanation
        - Computed score differs by 2 levels (e.g., report GREEN, computed RED) → FAIL
        
        **Dimension-specific metrics to look for in the rationale**:
        
        - **Dimension 1 — Traffic & Throughput**: WoW RPS growth %, peak/avg ratio, Kafka lag avg/peak
        - **Dimension 2 — Latency & Thread Concurrency**: P50/P95/P99 avg, P99 peak, outbound timeout config
        - **Dimension 3 — Error Rates & Stability**: HTTP error rate avg and peak, container restarts, OOMKills
        - **Dimension 4 — Resource Saturation**: CPU avg/peak % of limit, memory avg/peak % of limit,
          HPA trigger threshold, HPA headroom
        - **Dimension 5 — Persistence Layer**: MySQL connection % avg/peak, slow query daily avg,
          replication lag avg/peak, storage, largest table; or MongoDB read/write latency,
          connection pool %, query targeting ratio
        
        If the rationale does not cite a concrete metric value for a dimension, flag the dimension
        as WARNING — "rationale lacks specific metric values required by ai-scalability-review-service/src/main/java/app/aiscalabilityreview/prompt/ReportFormatPrompt.java".
        
        ---
        
        ## Check 5 — Capacity Projection Math
        
        Parse the Capacity Projections table from {{REPORT_FILE}}. Verify the mathematical correctness
        of every row and the consistency of the growth model.
        
        **Extract from the report**:
        - Growth model type (linear or compound growth stated in the introductory sentence)
        - Baseline avg RPS and baseline weekly hit volume
        - Stated weekly or quarterly growth rate
        - For each projection row: growth factor, projected weekly hits, projected avg RPS
        
        **Verifications**:
        
        1. **Baseline weekly hits**: Must equal `Baseline Avg RPS × 604,800` (seconds per week).
           Tolerance: ±1%.
        
        2. **Growth factor math**:
           - Compound model: `factor = (1 + weekly_rate)^(weeks)` where +1Q=13 weeks, +2Q=26, +4Q=52
           - Linear model: `factor = 1 + (rate × quarters)` where +1Q=1, +2Q=2, +4Q=4
           - Tolerance: ±2% on each factor.
        
        3. **Projected RPS**: `Projected RPS = Baseline RPS × Growth Factor`. Tolerance: ±1%.
        
        4. **Projected weekly hits**: `Projected hits = Projected RPS × 604,800`. Tolerance: ±1%.
        
        5. **Factor internal consistency**: +2Q factor should be approximately (+1Q factor)² for compound,
           or `1 + 2×(+1Q factor − 1)` for linear. Tolerance: ±3%.
        
        6. **Infrastructure Status color**: The status color in the projection table must be consistent
           with the actual scoring results. If the service currently scores YELLOW on any dimension,
           the "Current (Base)" row must not be labeled GREEN.
        
        For each row with incorrect math: FAIL with expected vs. reported values.
        For minor rounding inconsistencies (off by <1%): WARNING only.
        
        ---
        
        ## Check 6 — NOT_COLLECTED Authenticity
        
        Identify every `NOT_COLLECTED: <reason>` instance in {{REPORT_FILE}}.
        For each one, determine whether the stated inability to collect was legitimate.
        
        **Procedure**:
        1. Parse the NOT_COLLECTED item: metric or config name, reason stated, and source attempted.
        2. Attempt to query the same source now using the same tool and parameters.
        3. Evaluate:
           - Query fails for the same reason as stated → **PASS** (NOT_COLLECTED was legitimate)
           - Query succeeds and returns data → **FAIL** (NOT_COLLECTED was a false assertion; data exists)
           - Query fails for a different reason → **WARNING** (reason may be stale; claim is uncertain)
           - Metric omitted but the service config indicates it applies (e.g., MySQL metrics omitted
             when `{{MYSQL_HOST}}` is not N/A) → **FAIL** (metric was required but absent without justification)
        
        **Legitimate omissions** (mark SKIPPED, not FAIL):
        - Kafka metrics when `{{KAFKA_CONSUMER_GROUPS}}` is N/A
        - MySQL metrics when `{{MYSQL_HOST}}` is N/A
        - MongoDB metrics when `{{ATLAS_CLUSTER}}` is N/A
        
        **Temporal note**: If the report is >30 days old and a metric is now available that may not
        have been at generation time, mark WARNING rather than FAIL.
        
        ---
        
        ## Check 7 — Format & Attribution Completeness
        
        Verify the report structure against the mandatory format in ai-scalability-review-service/src/main/java/app/aiscalabilityreview/prompt/ReportFormatPrompt.java.
        No external queries needed — read {{REPORT_FILE}} only.
        
        ### 7a — Section presence and order
        
        Verify all 8 sections are present in the exact order:
        1. Header Block → 2. Executive Summary → 3. Infrastructure Configuration →
        4. Performance Metrics (Last 28 Days) → 5. Scalability Assessment & Scoring →
        6. Capacity Projections → 7. Recommendations & Action Items → 8. Footer
        
        Missing section → FAIL. Sections out of order → WARNING.
        
        ### 7b — Infrastructure table schema
        
        Verify the Infrastructure Configuration table contains the required rows:
        AKS Node Pool, AKS Scaling, K8s Pod Requests, K8s Pod Limits, K8s Memory Requests,
        K8s Memory Limits, HPA Settings, MySQL Host (or N/A), MySQL Version (or N/A), Storage (or N/A),
        Read Replica (or N/A), Atlas MongoDB (or N/A).
        
        Missing required rows → WARNING (unless the component is not used by this service).
        Extra rows → acceptable (no flag).
        
        ### 7c — Source attribution completeness
        
        Scan every metric bullet in Section 4 and every row in Section 3.
        Count any metric or config value that lacks a `[Source: ...]` annotation.
        
        Zero violations → PASS.
        1–2 violations → WARNING.
        3+ violations → FAIL.
        
        ### 7d — MySQL sub-table presence
        
        If `{{MYSQL_HOST}}` is not N/A: verify the MySQL top-storage sub-table is present
        in the Infrastructure Configuration section. Missing → FAIL.
        If `{{MYSQL_HOST}}` is N/A: verify the sub-table is absent. Present → WARNING.
        
        ### 7e — Weekly volume count
        
        Verify exactly 4 weeks of weekly hit volumes appear in the Performance Metrics section,
        each with a date range (e.g., "Jun 10–16"). More or fewer than 4 → FAIL.
        
        ### 7f — Kafka section presence
        
        If `{{KAFKA_CONSUMER_GROUPS}}` is not N/A: verify a Kafka consumer lag section is present
        in Performance Metrics. Missing → FAIL.
        If `{{KAFKA_CONSUMER_GROUPS}}` is N/A: verify it is absent. Present → WARNING.
        
        ### 7g — Scoring dimension structure
        
        Verify exactly 5 scoring dimensions are present with these exact fixed names:
        1. Traffic & Throughput
        2. Latency & Thread Concurrency
        3. Error Rates & Stability
        4. Resource Saturation
        5. Persistence Layer
        
        Each must include a **Score** line (GREEN / YELLOW / RED) and a **Rationale** line
        that cites ≥1 concrete metric value with a unit.
        
        Wrong names → FAIL. Missing Score or Rationale → FAIL. No metric cited in Rationale → WARNING.
        
        ### 7h — Capacity table row count
        
        Verify exactly 4 rows in the Capacity Projections table:
        Current (Base), +1Q (+13 Weeks), +2Q (+26 Weeks), +4Q (+52 Weeks).
        
        More or fewer rows → FAIL.
        
        ### 7i — Recommendation structure and sort order
        
        Verify:
        - At most 7 recommendations (more → WARNING)
        - Recommendations are sorted High → Medium → Low priority
        - Each recommendation has a **Finding** field and an **Action** field
        
        Out-of-order priority → WARNING. Missing Finding or Action → FAIL.
        
        ### 7j — No invented metrics
        
        Verify that every numeric metric value in the report has a `[Source: ...]` annotation
        or is explicitly marked `NOT_COLLECTED`. Any metric value with no source and no NOT_COLLECTED
        label → FAIL.
        
        ---
        
        ## Output
        
        Produce a complete validation report following the format defined in ai-scalability-review-service/src/main/java/app/aiscalabilityreview/prompt/ValidationFormatPrompt.java.
        
        Execute all 7 checks before writing the output. Do not omit any check —
        use STATUS: SKIPPED for checks not applicable to this service.
        Every finding must include a concrete reported value, actual value, and source.
            """;
    }
}
