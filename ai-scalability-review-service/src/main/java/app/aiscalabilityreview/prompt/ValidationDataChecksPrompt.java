package app.aiscalabilityreview.prompt;

/**
 * Checks 1, 2 and 6 — the validation checks that compare the report with its collected inputs.
 *
 * <p>This group used to re-query Datadog and Azure through MCP servers and compare the answers with the
 * report. That cost eight minutes and produced its own errors (a second model's arithmetic over a
 * second query, with a window or an aggregator that differed from the first), so it now compares the
 * report with what the pipeline collected: the Datadog figures are computed in code, once. It is given
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
        ## What this session compares

        No live source is queried in this run. The metrics the report cites were collected by the
        pipeline into `{{METRICS_PATH}}` (Datadog values read over the REST API, computed in code, over
        one fixed day-aligned 28-day window), and the Azure state into `{{AZURE_LIVE_PATH}}`. Those two
        files are the ground truth for Checks 2 and 6: the report is right when it says what they say.
        Read each once, fully, with the read tool, then compare. Do not call any other tool except the
        one that writes your output file and, for Check 1a, the read tool on a cited manifest.

        Be economical: do not restate a finding that matches. Report every FAIL, WARNING and SKIPPED
        finding in the full format, and collapse everything that passes into **one** line per check:

        `**[PASS]** <n> items verified: <comma-separated names>` followed by `- Dimension: General`.

        ---

        ## Check 1 — Config Accuracy

        Parse every row of the "Infrastructure Configuration" table from {{REPORT_FILE}}.
        For each row, identify its `[Source: ...]` annotation and compare the value with that source.

        ### 1a — Kubernetes / k8s-gitops values (annotated `[Source: code → ...]`)

        Re-read the exact files named in the annotations and compare the report's claim against the
        file content: CPU and memory requests and limits, HPA minReplicas, maxReplicas, target metric
        and threshold, and the AKS node pool SKU and min/max nodes (from Terraform, if annotated).
        Exact match required.

        ### 1b — Azure values (annotated `[Source: Azure MCP → ...]`)

        Compare each such value with `{{AZURE_LIVE_PATH}}`, which holds what the Azure MCP server
        returned during this run. If {{MYSQL_HOST}} is N/A, skip the MySQL rows (SKIPPED). Static
        configuration (SKU, vCPU count, max_connections, node pools) must match exactly. Check the
        **primary** server only: ignore replicas and any secondary attribute (high-availability mode,
        standby zone, storage auto-grow or IO scaling flags), whether or not the file lists them. A
        primary value the report states that the file does not contain is SKIPPED, not a WARNING.

        ---

        ## Check 2 — Metric Numerical Fidelity

        Parse the metric values of the report's "Performance Metrics (Last 28 Days)" section and compare
        each with the same metric in `{{METRICS_PATH}}` (its sections 1–7 and the JSON snapshot).

        - The value must be the one the file holds, in the same unit. A unit conversion the report
          applies (nanocores to cores, bytes to GB, seconds to milliseconds, a percentage of a limit)
          is checked by redoing the conversion from the file's value.
        - **Tolerance**: equal after rounding to the digits the report shows. A difference larger than
          1% is a FAIL; a difference of rounding only is a PASS.
        - A metric the report states that the file does not contain is a FAIL ("not in the collected
          data"). A metric the file holds that the report leaves out is not an issue.
        - The date of a peak must be the date the file gives, and inside the file's lookback window.
        - SLOs: the set of SLO names, each attainment, error budget and status must equal the file's.
        - Do not judge whether a value is plausible, high or low: that is not this check.

        ---

        ## Check 6 — NOT_COLLECTED Authenticity

        Identify every `NOT_COLLECTED: <reason>` instance in {{REPORT_FILE}}, and compare with section
        10 "Collection Gaps" of `{{METRICS_PATH}}`.

        - A `NOT_COLLECTED` in the report whose metric is a listed gap, or is marked `NOT_COLLECTED` in
          the file's body, is **PASS** (it is real).
        - A `NOT_COLLECTED` in the report for a metric the file holds a value for is a **FAIL** (a
          false assertion: the data exists).
        - A metric the service's configuration says applies (MySQL when {{MYSQL_HOST}} is not N/A, Kafka
          when {{KAFKA_CONSUMER_GROUPS}} is not N/A, MongoDB when {{ATLAS_CLUSTER}} is not N/A) that is
          missing from the report without a `NOT_COLLECTED` and without a value is a **FAIL**.
        - A gap in the file that the report does not mention is a **WARNING**.
        - A metric of a datastore the service does not use is a legitimate omission (SKIPPED).

        **Never legitimate** (always FAIL): a reason that claims the derivation itself was impossible
        — "complex data derivation", "beyond current capabilities", "does not support timestamp
        extraction", or any variant. The data was collected; only the arithmetic was skipped.

        ---
            """;

        FILE_REFS = """


        ## Attached Local Files

        **Review report to validate**:
        @{{REPORT_FILE}}

        **Collected metrics** (ground truth for Checks 2 and 6):
        @{{METRICS_PATH}}

        **Collected Azure state** (ground truth for Check 1b):
        @{{AZURE_LIVE_PATH}}

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
