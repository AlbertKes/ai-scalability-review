package app.aiscalabilityreview.prompt;

public class ReportFormatPrompt {
    /**
     * Report format specification (report-format.md) — embedded verbatim for reference.
     */
    public static final String CONTENT;

    static {
        CONTENT = """
        # Scalability Report Format
        
        Use this file during **Step 7 — Output** in `review-task.md`.
        Fill every section and field exactly as specified. Do **not** add, remove, or rename
        sections. Do **not** invent metrics; write `NOT_COLLECTED: <reason>` for any metric
        that could not be retrieved. Following this format ensures that repeated reviews of the
        same service produce structurally identical reports with minimal drift.
        
        ---
        
        ## Global Metric Value Rules (apply to every section without exception)
        
        **No approximations.** Never write "around", "about", "approximately", "roughly",
        "~", "under", "over", or any other qualifier in front of a metric value.
        Use the exact figure returned by the data source.
        
        | ✗ Wrong | ✓ Correct |
        |---------|-----------|
        | `memory stable at around 0.5 GB` | `memory stable at \\`0.476 GB\\`` |
        | `under 1.5% of the limit` | `\\`1.2%\\` of the limit` |
        | `approximately 500 MB working set` | `\\`476 MB\\` working set` |
        
        **Every metric value must carry a `[Source: ...]` annotation** referencing the exact
        Datadog query, Azure MCP call, MySQL MCP query, or code file path that produced it.
        This rule applies in **all sections**: Executive Summary, Infrastructure Configuration,
        Performance Metrics, and Scoring Rationale. The only exception is the bulleted critical
        findings list in the Executive Summary, where a value that is already fully annotated
        in Section 4 does not need a second `[Source: ...]` here.
        
        ---
        
        ## File Naming Convention
        
        ```
        scalability/ai/reports/<service>-<YYYY-MM-DD>.md
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
        <One paragraph (3–6 sentences) describing the service's purpose and tier.>
        
        <One paragraph summarising the overall infrastructure health observed over the 28-day
        lookback window (positive findings first).>
        
        <Bulleted list of critical or notable findings, numbered, ordered by severity
        (most severe first). Limit to the top 5 items. Each item must be bold-prefixed.>
        1. **<Finding title>**: <one-sentence description of the risk or gap>.
        2. …
        ```
        
        Rules:
        - If there are no critical findings, write: "No critical findings. The service is healthy."
        - The two narrative paragraphs (service purpose; overall health) must contain **no raw metric
          values** (no numbers with units, no percentages, no durations). Describe quality, not
          quantity. Quantitative data belongs exclusively in Section 4.
        - The critical findings bulleted list **may** cite specific peak metric values (e.g., lag
          peaks, error rate spikes) only when those exact values are already fully annotated with
          `[Source: ...]` in Section 4. Do not add a second `[Source: ...]` here — the reference
          is implied.
        
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
        - Use exact values from Terraform / Kubernetes manifests or Azure MCP (live state). Do not round.
        - If MySQL table size analysis was performed (Step 5 of review-task.md), add a MySQL sub-table.
        
        ---
        
        ## 4. Performance Metrics (Last 28 Days)
        
        See report-format.md for the full metric template structure.
        
        ---
        
        ## 5. Scalability Assessment & Scoring
        
        Score each of the 5 fixed dimensions as GREEN / YELLOW / RED.
        
        ---
        
        ## 6. Capacity Projections
        
        Project +1Q, +2Q, +4Q growth. Use a compound growth model; default to +1.5% per week if trend is flat.
        
        ---
        
        ## 7. Recommendations & Action Items
        
        Numbered, sorted High → Medium → Low priority. Maximum 7 recommendations.
        
        ---
        
        ## 8. Footer
        
        ```markdown
        ---
        *Report automatically compiled and validated against Datadog performance history, Azure Resource
        metrics, MySQL performance, and infrastructure configurations.*
        ```
            """;
    }
}
