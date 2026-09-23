package app.aiscalabilityreview.prompt;

public class ReportFormatPrompt {
    /**
     * Report format specification. This is the single source of truth for the report structure:
     * the validation stage checks a report against it, and the pipeline parses dimension scores and
     * recommendation priorities out of the shapes defined here, so the literal markers
     * ("### Dimension N — <name>: <SCORE>", "(High Priority)") must not be changed on one side only.
     */
    public static final String CONTENT;

    static {
        CONTENT = """
        # Scalability Report Format

        Fill every section and field exactly as specified. Do **not** add, remove, or rename
        sections. Do **not** invent metrics; write `NOT_COLLECTED: <reason>` for any metric that
        could not be retrieved. Following this format ensures that repeated reviews of the same
        service produce structurally identical reports with minimal drift.

        ---

        ## Global Metric Value Rules (apply to every section without exception)

        **No approximations.** Never write "around", "about", "approximately", "roughly",
        "~", "under", "over", or any other qualifier in front of a metric value.
        Use the exact figure returned by the data source.

        | Wrong | Correct |
        |-------|---------|
        | memory stable at around 0.5 GB | memory stable at `0.476 GB` |
        | under 1.5% of the limit | `1.2%` of the limit |
        | approximately 500 MB working set | `476 MB` working set |

        **Every metric value must carry a `[Source: ...]` annotation** referencing the exact
        Datadog query, Azure MCP call, MySQL MCP query, or code file path that produced it.
        This rule applies in **all sections**. The only exception is the bulleted critical findings
        list in the Executive Summary, where a value that is already fully annotated in Section 4
        does not need a second `[Source: ...]`.

        Every derived percentage states the denominator it used, e.g.
        `1.34 cores (67% of the 2 core limit)`.

        ---

        ## File Naming Convention

        ```
        reports/<service>/<YYYY-MM-DD>-review.md
        ```

        ---

        ## Section Order (mandatory)

        1. Header Block
        2. Executive Summary
        3. Infrastructure Configuration
        4. Performance Metrics (Last 28 Days)
        5. Scalability Assessment & Scoring
        6. Capacity Projections
        7. Recommendations & Action Items
        8. Footer

        ---

        ## 1. Header Block

        ```markdown
        # Scalability Review: <service> (<env>)
        **Date**: <YYYY-MM-DD>
        **Reviewer**: <agent name or engineer name>

        ---
        ```

        Rules:
        - `<service>` is the exact Kubernetes service name, lowercase-hyphenated.
        - `<env>` is one of: `prod`, `uat`, `dev`.
        - Date is the date the review was run (ISO 8601).

        ---

        ## 2. Executive Summary

        ```markdown
        ## 1. Executive Summary
        <One paragraph (3-6 sentences) describing the service's purpose and tier.>

        <One paragraph summarising the overall infrastructure health observed over the 28-day
        lookback window (positive findings first).>

        <Numbered list of critical or notable findings, ordered by severity (most severe first).
        Limit to the top 5 items. Each item must be bold-prefixed.>
        1. **<Finding title>**: <one-sentence description of the risk or gap>.
        ```

        Rules:
        - If there are no critical findings, write: "No critical findings. The service is healthy."
        - The two narrative paragraphs (service purpose; overall health) must contain **no raw metric
          values** (no numbers with units, no percentages, no durations). Describe quality, not
          quantity. Quantitative data belongs exclusively in Section 4.
        - The critical findings list **may** cite specific peak metric values only when those exact
          values are already fully annotated in Section 4.

        ---

        ## 3. Infrastructure Configuration

        ```markdown
        ## 2. Infrastructure Configuration

        | Infrastructure Component | Property Value / Configuration Detail |
        | :--- | :--- |
        | **AKS Node Pool** | `<pool name>` (`<SKU>`: <vCPU> vCPU, <RAM>GB RAM) |
        | **AKS Scaling** | Min <n> / Max <n> nodes |
        | **K8s Pod Requests** | CPU: `<value>` |
        | **K8s Pod Limits** | CPU: `<value>` |
        | **K8s Memory Requests** | Memory: `<value>` |
        | **K8s Memory Limits** | Memory: `<value>` |
        | **HPA Settings** | <Min n / Max n (Target: <metric> <threshold>)> OR <None (Static replica count = n)> |
        | **MySQL Host** | `<host>` (`<SKU>`: <vCPU> vCPU, <RAM>GB RAM) OR N/A |
        | **MySQL Version** | <version> (HA: <SameZone/ZoneRedundant/Disabled>) OR N/A |
        | **Storage** | <size>GB (Auto-grow: ON/OFF, IO Scaling: ON/OFF) OR N/A |
        | **Read Replica** | `<host>` (HA: ON/OFF) OR N/A |
        | **Atlas MongoDB** | `<cluster name>` (<tier>, <n> electable nodes) OR N/A |
        ```

        Rules:
        - If a service has multiple deployments (e.g. consumer variant, kafka-consumer variant),
          add one sub-table per deployment rather than merging them.
        - Use exact values from Terraform / Kubernetes manifests or the Azure live state. Do not round.
        - When MySQL table storage was collected, add the sub-table below immediately after the main
          table. Omit it entirely when it was not collected.

        ```markdown
        ### 2.1 MySQL Table Storage (Top 20 by data + index)

        | Table | Rows | Data (MB) | Index (MB) | Total (MB) |
        | :--- | ---: | ---: | ---: | ---: |
        | `<table_name>` | <table_rows> | <data_mb> | <index_mb> | <total_mb> |
        ```

        ---

        ## 4. Performance Metrics (Last 28 Days)

        Every bullet below is mandatory. When a value was not collected, keep the bullet and write
        `NOT_COLLECTED: <reason>` in place of the value. Omit a whole sub-section only where the
        rules under it say so.

        ```markdown
        ## 3. Performance Metrics (Last 28 Days)

        ### 3.1 Traffic & Throughput
        - **Average RPS**: `<value>` [Source: ...]
        - **Peak RPS**: `<value>` at <YYYY-MM-DD HH:mm> [Source: ...]
        - **Peak / Average ratio**: `<value>x`
        - **Week-over-week average RPS growth**: `<value>%`
        - **Weekly Volumes** (exactly 4 rows, oldest first):

        | Week | Requests |
        | :--- | ---: |
        | <Mon D-D> | `<value>` |

        ### 3.2 Latency Profile
        - **P50 (28-day average)**: `<value> ms` [Source: ...]
        - **P95 (28-day average)**: `<value> ms` [Source: ...]
        - **P99 (28-day average)**: `<value> ms` [Source: ...]
        - **P99 (peak observed)**: `<value> ms` at <YYYY-MM-DD HH:mm> [Source: ...]
        - **Outbound HTTP client timeouts**: <configured values, or none configured> [Source: context ...]
        - **Circuit breakers on critical paths**: <covered / partial / none> [Source: context ...]

        ### 3.3 Error Rates & Stability
        - **HTTP error rate (28-day average)**: `<value>%` [Source: ...]
        - **HTTP error rate (worst day)**: `<value>%` on <YYYY-MM-DD> [Source: ...]
        - **Container restarts (28 days)**: `<value>` [Source: ...]
        - **OOMKill events (28 days)**: `<value>` [Source: ...]

        ### 3.4 Resource Saturation
        - **CPU average**: `<value> cores` (`<value>%` of the `<limit>` limit, `<value>%` of the `<request>` request) [Source: ...]
        - **CPU peak**: `<value> cores` (`<value>%` of the `<limit>` limit) [Source: ...]
        - **Memory average**: `<value> MiB` (`<value>%` of the `<limit>` limit) [Source: ...]
        - **Memory peak**: `<value> MiB` (`<value>%` of the `<limit>` limit) [Source: ...]
        - **Replica count**: average `<value>`, max `<value>` of maxReplicas `<value>` [Source: ...]
        - **HPA effective trigger**: raw averageUtilization `<value>%` of request = `<value>%` of limit

        ### 3.5 Persistence Layer / DB Saturation
        MySQL rows are omitted when the service uses no MySQL; MongoDB rows when it uses no MongoDB.
        - **Connection Pool**: average `<value>` (`<value>%` of max_connections `<value>`), peak `<value>` (`<value>%`) [Source: ...]
        - **Slow queries**: `<value>` per day average, worst day `<value>` [Source: ...]
        - **Replication lag**: average `<value> s`, peak `<value> s` [Source: ...]
        - **Storage used**: `<value> GB` (auto-grow ON: absolute figure only; auto-grow OFF: `<value>%` of `<provisioned>` GB) [Source: ...]
        - **Largest table**: `<table>` at `<value> GB`; tables above 10 GB: `<value>` [Source: ...]
        - **MongoDB op latency**: read average `<value> ms`, write average `<value> ms`, read peak `<value> ms` [Source: ...]
        - **MongoDB connections**: peak `<value>` (`<value>%` of the cluster limit `<value>`) [Source: ...]
        - **MongoDB query targeting ratio**: average `<value>`, peak `<value>` [Source: ...]

        ### 3.6 Kafka Consumer Lag
        Include this sub-section only when the service has Kafka consumer groups; omit it entirely
        otherwise. One row per consumer group and topic.

        | Consumer Group | Topic | Avg Lag | Peak Lag |
        | :--- | :--- | ---: | ---: |
        | `<group>` | `<topic>` | `<value>` | `<value>` |

        ### 3.7 SLO & Monitors
        - **SLO compliance**: <per SLO: name and compliance figure, or None defined> [Source: ...]
        - **Monitors in ALERT or WARN**: <per monitor: name and state, or None> [Source: ...]
        ```

        ---

        ## 5. Scalability Assessment & Scoring

        Exactly five dimensions, in this order, with these exact names. The heading line carries the
        score and is parsed by tooling, so its shape is fixed, with an em dash between the number
        and the name:  `### Dimension <n> — <name>: <SCORE>`

        ```markdown
        ## 4. Scalability Assessment & Scoring

        ### Dimension 1 — Traffic & Throughput: <GREEN | YELLOW | RED>
        - **Score**: <GREEN | YELLOW | RED>
        - **Rationale**: <at least one concrete metric value with its unit and `[Source: ...]`,
          named against the threshold row it matched>

        ### Dimension 2 — Latency & Thread Concurrency: <GREEN | YELLOW | RED>
        ### Dimension 3 — Error Rates & Stability: <GREEN | YELLOW | RED>
        ### Dimension 4 — Resource Saturation: <GREEN | YELLOW | RED>
        ### Dimension 5 — Persistence Layer: <GREEN | YELLOW | RED>

        **Overall Score**: <GREEN | YELLOW | RED>  (the worst of the five dimensions)
        ```

        Each of the five dimensions repeats the full `**Score**` / `**Rationale**` block.

        ---

        ## 6. Capacity Projections

        Exactly four rows. State the growth model and the weekly rate in one sentence above the
        table. Baseline weekly requests equal `baseline_avg_rps x 604800`.

        ```markdown
        ## 5. Capacity Projections

        Compound growth model at `<value>%` per week, derived from the 28-day week-over-week trend.

        | Period | Growth Factor | Projected Weekly Requests | Projected Avg RPS | Infrastructure Status |
        | :--- | ---: | ---: | ---: | :--- |
        | Current (Base) | 1.00x | `<value>` | `<value>` | <GREEN/YELLOW/RED> |
        | +1Q (+13 Weeks) | `<value>x` | `<value>` | `<value>` | <GREEN/YELLOW/RED> |
        | +2Q (+26 Weeks) | `<value>x` | `<value>` | `<value>` | <GREEN/YELLOW/RED> |
        | +4Q (+52 Weeks) | `<value>x` | `<value>` | `<value>` | <GREEN/YELLOW/RED> |

        **Binding constraint**: <the first resource that runs out, and at which projection point>
        ```

        The `Current (Base)` status must equal the Overall Score from Section 5.

        ---

        ## 7. Recommendations & Action Items

        At most 7 recommendations, numbered, sorted High then Medium then Low priority. The priority
        marker text is parsed by tooling and must appear verbatim as `(High Priority)`,
        `(Medium Priority)` or `(Low Priority)`.

        ```markdown
        ## 6. Recommendations & Action Items

        ### 1. <short title> (High Priority)
        - **Finding**: <what the data shows, citing the metric or config value and its source>
        - **Action**: <the concrete change to make, specific enough to become a ticket>
        - **Owner**: <team, or TBD>
        ```

        If there is nothing to recommend, write "No action items." and no numbered entries.

        ---

        ## 8. Footer

        ```markdown
        ---
        *Report automatically compiled and validated against Datadog performance history, Azure Resource
        metrics, MySQL performance, and infrastructure configurations.*
        ```

        ---

        ## Drift-Prevention Checklist

        Confirm every line before finalising the report:

        - [ ] All 8 sections present, in order, with the numbering shown above
        - [ ] Executive Summary narrative paragraphs contain no numeric metric values
        - [ ] Infrastructure Configuration table has every required row (or `N/A`)
        - [ ] MySQL sub-table present if and only if MySQL table storage was collected
        - [ ] The Weekly Volumes table in 3.1 has exactly 4 rows, each with a date range
        - [ ] The 3.6 Kafka sub-section is present if and only if the service has consumer groups
        - [ ] Every metric value carries `[Source: ...]` or is marked `NOT_COLLECTED: <reason>`
        - [ ] Every derived percentage names the denominator it used
        - [ ] Exactly 5 scoring dimensions, with the fixed names and the `### Dimension n — name: SCORE` heading
        - [ ] Every dimension has a `**Score**` line and a `**Rationale**` line citing a concrete value
        - [ ] Overall Score equals the worst dimension score
        - [ ] Capacity table has exactly 4 rows and its `Current (Base)` status equals the Overall Score
        - [ ] Baseline weekly requests equal baseline avg RPS x 604800
        - [ ] At most 7 recommendations, sorted High then Medium then Low, each with Finding and Action
        - [ ] No approximation qualifier appears anywhere in the report
            """;
    }
}
