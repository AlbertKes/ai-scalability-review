package app.aiscalabilityreview.prompt;

/**
 * Checks 1, 2 and 6 — the validation checks that re-query the live sources.
 *
 * <p>Held apart from the other two groups because this is the only one that needs MCP servers, and
 * it is the only one whose cost is dominated by tool round-trips rather than by context. It is given
 * the infra repository (Check 1a re-reads the manifests the report cites) but not the application
 * repository, which only Check 8 needs, and neither the scoring thresholds nor the report format
 * spec, which only Checks 4 and 7 need.
 */
public final class ValidationDataChecksPrompt {
    public static final String CONTENT;

    /** The repository this group reads, and the one rule that keeps it from browsing the whole tree. */
    public static final String FILE_REFS;

    static {
        CONTENT = """
        ## Re-querying Datadog

        The report's 28-day figures come from a day-aligned window: `to` = today's 00:00 UTC in epoch
        seconds, `from` = `to - 2419200`. Use the same window, and the same `.rollup(<aggregator>,
        86400)` suffix the report's query annotation shows, or your values will not be comparable.

        `query_metrics` returns `series[].pointlist` as `[[<epoch_milliseconds>, <value>], ...]`.
        Averages, peaks, worst days, per-week totals **and the timestamp of any peak** are all
        arithmetic over that array — a report that marks one of them `NOT_COLLECTED` because the tool
        "does not support" it is asserting something false, and Check 6 must fail it.

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

        Use `env:{{ENV}}` as the base filter and set `from=now-28d`, `to=now`. Datadog accepts
        several metric expressions in one `query_metrics` call as a comma-separated query string —
        group the re-checks below into one call per block rather than one call per metric, and never
        run the same query twice in this session.

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

        **Never legitimate** (always FAIL, no re-query needed): a reason that claims the derivation
        itself was impossible rather than the query — "complex data derivation", "beyond current
        capabilities", "does not support timestamp extraction", or any variant. The data was returned;
        only the arithmetic was skipped.

        **Temporal note**: If the report is >30 days old and a metric is now available that may not
        have been at generation time, mark WARNING rather than FAIL.

        ---
            """;

        FILE_REFS = """


        ## Attached Local Files

        **Review report to validate**:
        @{{REPORT_FILE}}

        **Infra repository root** (for re-reading the source-annotated config files of Check 1a):
        Local path: `{{INFRA_REPO_PATH}}`

        When the report cites `[Source: code -> <path>]`, read that file from this root with the read
        tool. `{{INFRA_CONTEXT_PATH}}` lists which files the review stage was given and which were
        skipped, so start there when you need to locate a cited file. Read only the files you
        actually need in order to verify a claim: every tool call re-sends the whole context, so a
        file opened out of curiosity is paid for again on every round-trip that follows.
            """;
    }

    private ValidationDataChecksPrompt() {
    }
}
