package app.aiscalabilityreview.prompt;

/**
 * The output contract of one validation session: the shape of a check section, and the instruction
 * to write nothing around it.
 *
 * <p>This used to specify the whole validation report — header, summary table, overall verdict,
 * footer — and the model wrote all of it. With the checks split across three sessions no single
 * session can see enough to write a summary table, so
 * {@link app.aiscalabilityreview.service.ValidationReportAssembler} builds those parts from the
 * three partials instead. That is not only a consequence of the split but a correctness gain: the
 * verdict and the issue counts are now derived from the findings rather than asserted alongside
 * them, which is what a report whose summary disagreed with its own sections used to cost a reader.
 *
 * <p>Placeholders: {@code {{CHECK_HEADERS}}}, {@code {{OUTPUT_PATH}}}.
 */
public final class ValidationFormatPrompt {
    public static final String CONTENT;

    /** Where a validation session that writes a file puts its sections. Placeholder: {@code {{OUTPUT_PATH}}}. */
    public static final String FILE_OUTPUT;

    /** The same contract for a one-shot call that has no file system to write to. */
    public static final String RESPONSE_OUTPUT;

    static {
        CONTENT = """
        # Output Format

        Write one section per check you own, in ascending check number, using exactly these headers:

        {{CHECK_HEADERS}}

        The `<STATUS>` on the header line is the status of the check as a whole:

        - **PASS** — every finding in it is PASS, or the check was fully SKIPPED
        - **WARNING** — at least one WARNING finding and no FAIL findings
        - **FAIL** — at least one FAIL finding
        - **SKIPPED** — the entire check is not applicable (e.g., MySQL checks when MYSQL_HOST is N/A)

        Each section carries its findings in the Finding Format given above, then a short summary:

        ```markdown
        ## Check <n> — <the name given above>: FAIL

        ### Findings

        **[PASS]** CPU requests (K8s deployment)
          - Reported: `500m`
          - Actual: `500m` [Source: code → k8s-gitops/environments/prod/apps/wonder/deployment.yaml:L42]
          - Detail: Exact match.

        **[FAIL]** HPA maxReplicas
          - Reported: `20`
          - Actual: `15` [Source: code → k8s-gitops/environments/prod/apps/wonder/hpa.yaml:L18]
          - Detail: Report overstates maxReplicas by 5. Likely copied from an older manifest version.

        **[WARNING]** MySQL storage used
          - Reported: `312 GB`
          - Actual: `329 GB` [Source: Datadog MCP → query_metrics("max:azure.dbformysql_flexibleservers.storage_used{name:wonder-cart-mysql-prod}.rollup(max, 86400)")]
          - Detail: 5.4% increase since report generation (within 10% tolerance for growing storage).

        ### Summary
        <1–2 sentences summarizing the overall result of this check.>
        ```

        ---

        ## Before you write

        - [ ] Every check you own has a section, with the exact header above
        - [ ] Every section header ends in a status, and that status matches its findings
              (any FAIL → FAIL; else any WARNING → WARNING; else PASS or SKIPPED)
        - [ ] Every finding has a STATUS tag, a Reported value, an Actual value with `[Source: ...]`,
              and a Detail
        - [ ] Every SKIPPED finding states why it was skipped
        - [ ] A check you could not complete is reported as its findings actually stand — never as
              PASS. A check that silently passes because it could not look is worse than no check.
        - [ ] Temporal context is noted when the report is more than 30 days old
            """;

        FILE_OUTPUT = """


        ## Where to write it

        Write those sections — and nothing else — to `{{OUTPUT_PATH}}` using the file system write
        tool.

        **Nothing else** means: no validation header, no summary table, no overall verdict, no
        critical-issues or warnings list, no footer. The pipeline assembles all of those from the
        sessions once they have all finished, and anything of that kind written here would be merged
        into the report twice.
            """;

        RESPONSE_OUTPUT = """


        ## What to return

        Return those sections — and nothing else — as your response.

        **Nothing else** means: no validation header, no summary table, no overall verdict, no
        critical-issues or warnings list, no footer. The pipeline computes all of those from your
        findings, and a copy of them here would be merged into the report twice.
            """;
    }

    private ValidationFormatPrompt() {
    }
}
