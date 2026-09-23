package app.aiscalabilityreview.prompt;

public class ReviewTaskPrompt {
    /**
     * Prompt for the synthesis and scoring stage. All data has already been collected by the
     * earlier stages into three local files, so this stage uses no MCP server and makes no tool
     * call other than reading those files and writing the report.
     * Placeholders: {{SERVICE}}, {{ENV}}, {{NAMESPACE}}, {{MYSQL_HOST}}, {{MYSQL_DB}},
     * {{ATLAS_CLUSTER}}, {{HPA_TYPE}}, {{KAFKA_CONSUMER_GROUPS}},
     * {{CODE_CONTEXT_PATH}}, {{INFRA_CONTEXT_PATH}}, {{METRICS_PATH}}
     */
    public static final String CONTENT;

    static {
        CONTENT = """
        # Scalability Review Task

        You are a devops engineer performing a structured scalability review for tier-1
        microservices. Perform a complete review for:

        - **Service**: {{SERVICE}}
        - **Environment**: {{ENV}}
        - **Namespace**: {{NAMESPACE}}
        - **Lookback window**: 28 days
        - **MySQL host/cluster**: {{MYSQL_HOST}}  (set to "N/A" if service does not use MySQL)
        - **MySQL database**: {{MYSQL_DB}}  (set to "N/A" if MYSQL_HOST is N/A)
        - **Atlas MongoDB cluster**: {{ATLAS_CLUSTER}}  (set to "N/A" if service does not use MongoDB)
        - **HPA type**: {{HPA_TYPE}}  (one of: HPA | none)
        - **Kafka consumer groups**: {{KAFKA_CONSUMER_GROUPS}}  (comma-separated, or "N/A")

        ## Input Files

        Everything you need has already been collected. Read these three files and work only from
        them — do **not** call any MCP server, and do **not** re-query any metric.

        | File | Contents |
        | :--- | :--- |
        | `{{INFRA_CONTEXT_PATH}}` | Kubernetes manifests and Terraform for this service, already filtered to the relevant files. Each file appears under a heading showing its repository path. |
        | `{{CODE_CONTEXT_PATH}}` | Business context extracted from the application source: API surface, DB access, Kafka usage, caching, resilience config. |
        | `{{METRICS_PATH}}` | The 28-day Datadog metrics, Azure live configuration, and MySQL table storage, every value already annotated with its source. |

        If one of these files is missing or empty, note that explicitly in the report
        (`Code context: not available`) and continue with the remaining inputs.

        ## Data Source Attribution Requirement

        For **every** piece of configuration or metric value you report, state which data source it
        came from, reusing the annotation that is already attached to the value in the input files:

        - `[Source: code → <exact file path>]` — a configuration value from `{{INFRA_CONTEXT_PATH}}`;
          use the repository path shown in that file's heading, not the context file name.
        - `[Source: context → code-context.md]` — a value from the business context document.
        - `[Source: Azure MCP → <tool name>(<args>)]`, `[Source: MySQL MCP → <exact SQL query>]`,
          `[Source: Datadog MCP → query_metrics("<exact metric query>")]` — copy the annotation
          verbatim from `{{METRICS_PATH}}`.

        If a value is marked `NOT_COLLECTED: <reason>` in the input files, carry that marker and its
        reason into the report unchanged. Never substitute an estimate for a missing value.

        **No approximations.** Never write "around", "about", "approximately", "roughly", "~",
        "under", or "over" before a metric value anywhere in the report. Use the exact figure from
        the input file. This rule applies to every section: Executive Summary, Infrastructure
        Configuration, Performance Metrics, and Scoring Rationale.

        ---

        ## Instructions

        ### Step 1 — Extract the Infrastructure Configuration

        From `{{INFRA_CONTEXT_PATH}}`, extract and record with the source path of each value:

        Kubernetes:
        - Deployment CPU/memory requests and limits, per deployment when the service has more than one.
        - HPA or DatadogPodAutoscaler: minReplicas, maxReplicas, metric triggers and thresholds,
          stabilizationWindow, scaleDown policy.
        - Liveness and readiness probe timeouts and thresholds.
        - PodDisruptionBudget (minAvailable / maxUnavailable) if present.
        - ConfigMap entries related to connection pools, timeouts, or thread counts.

        Terraform — AKS node pools: node SKU, min/max nodes per pool, spot vs on-demand.

        Terraform — MySQL (skip if {{MYSQL_HOST}} is N/A): SKU tier, `max_connections`, read
        replicas, IOPS auto scaling, storage autogrow, HA config.

        Terraform — Atlas MongoDB (skip if {{ATLAS_CLUSTER}} is N/A): cluster tier (M-class),
        auto-scaling bounds, backup config.

        Where a Terraform value and the Azure live state in `{{METRICS_PATH}}` disagree, the live
        state wins; report the live value and flag the drift.

        ### Step 2 — Load the Business Context

        Read `{{CODE_CONTEXT_PATH}}` and use it to inform the analysis: critical synchronous paths,
        unbounded queries, blocking patterns, missing timeouts, missing circuit breakers, scheduled
        jobs, and any in-process state that is not replica-safe.

        ### Step 3 — Load the Collected Metrics

        Read `{{METRICS_PATH}}`. It already holds every metric the review scores. Compute the
        percentages the scoring reference needs, using the requests and limits from Step 1:

        - `cpu_avg_pct_of_limit = cpu_avg_cores / cpu_limit_cores × 100`
        - `cpu_peak_pct_of_limit = cpu_peak_cores / cpu_limit_cores × 100`
        - `cpu_avg_pct_of_request = cpu_avg_cores / cpu_request_cores × 100`
        - `mem_avg_pct_of_limit = mem_avg_mib / mem_limit_mib × 100`
        - `mem_peak_pct_of_limit = mem_peak_mib / mem_limit_mib × 100`
        - MySQL connections as a percentage of `max_connections`, average and peak.
        - MySQL storage utilisation **only** when auto-grow is OFF; when it is ON, score the
          absolute GB figure instead.

        Show both the absolute value and the computed percentage in the report, and state the limit
        you divided by so the arithmetic is reproducible.

        ### Step 4 — Cross-Reference Config with Metrics

        Cite the exact source for each config value and metric value in every cross-reference.

        - Does HPA `maxReplicas` leave headroom at projected +1Q traffic? Given {{HPA_TYPE}}, what
          trigger metric controls scale-out, and is the threshold appropriate for the observed load?
          **Important — HPA CPU averageUtilization is relative to the CPU request, not the CPU limit.**
          Compute the effective trigger as a percentage of the limit:
          `effective_trigger_pct_of_limit = averageUtilization × (cpu_request / cpu_limit)`
          and score that computed value. Example: `averageUtilization: 100`, request `1`, limit `2`
          → `100 × 0.5 = 50 %` of limit → GREEN. Record both the raw `averageUtilization` and the
          computed effective percentage.
        - Are CPU/memory limits within 20 % of observed peak usage (throttling / OOM risk)?
        - If MySQL connection count approaches `max_connections` (Terraform value cross-checked
          against the Azure live configuration), flag as RED.
        - If the Atlas query targeting ratio exceeds 100 (scans far exceed documents returned),
          flag as YELLOW.
        - Do monitors in ALERT state correlate with the metric findings?
        - Does the business context reveal batch jobs or scheduled tasks that would create
          predictable traffic spikes invisible in the 28-day average?
        - If MySQL table storage was collected, does the largest table's growth trajectory put
          storage auto-grow or IOPS scaling at risk within +1Q?

        ### Step 5 — Score Each Review Dimension

        Apply the GREEN / YELLOW / RED thresholds from the **Metric Scoring Reference** section
        below to every collected metric. The worst-matching threshold across all metrics in a
        dimension determines the dimension score. The five fixed dimensions are:

        1. Traffic & Throughput
        2. Latency & Thread Concurrency
        3. Error Rates & Stability
        4. Resource Saturation
        5. Persistence Layer

        When writing each dimension's **Rationale**:
        - Cite only exact metric values that appear in the Performance Metrics section of the report.
        - Every metric value mentioned must carry the same `[Source: ...]` annotation it has there.
        - Do **not** round, approximate, or paraphrase. Write `1.2%`, not "under 1.5%"; write
          `476 MB`, not "around 500 MB".
        - A metric that is `NOT_COLLECTED` because the component is not used by this service is
          excluded from scoring. A metric that is `NOT_COLLECTED` because the query failed is a
          YELLOW metric availability finding, and more than two such gaps make the dimension RED.

        ### Step 6 — Generate Capacity Projections

        Project +1Q (13 weeks), +2Q (26 weeks) and +4Q (52 weeks) with a compound weekly growth
        model built on the 28-day week-over-week growth rate from `{{METRICS_PATH}}`. When that rate
        is flat or negative, default to +1.5 % per week and say so. Baseline weekly requests must
        equal `baseline_avg_rps × 604800`.

        ### Step 7 — Output

        Produce the full report in the exact structure defined in the **Report Format Reference**
        section below. Complete every section and field in the order specified, and run the
        drift-prevention checklist at the end of that reference before finalising.

        Every metric or config value in the report **must** carry its data source label
        (`[Source: ...]`) inline or in a parenthetical note. This is mandatory for traceability.
            """;
    }
}
