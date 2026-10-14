package app.aiscalabilityreview.prompt;

/**
 * The part of the validation prompt every check group shares: who the model is, what it may not do,
 * the service it is validating, and the shape of a single finding.
 *
 * <p>Validation used to be one Gemini session holding all eight checks. In an agentic loop the whole
 * context is re-sent on every tool round-trip, so the source files Check 8 read to verify a
 * recommendation were re-sent on every later round-trip — including the ones that only re-read the
 * report's own markdown. Measured on wonder-cart-service that stage spent 6.0M input tokens over 61
 * tool calls, 64 % of the whole run. The checks are now three independent sessions, each carrying
 * only the context its own checks need; this is the text all three begin with.
 *
 * <p>Placeholders: {@code {{CHECK_SCOPE}}}, {@code {{REPORT_FILE}}}, {@code {{SERVICE}}},
 * {@code {{ENV}}}, {@code {{NAMESPACE}}}, {@code {{MYSQL_HOST}}}, {@code {{MYSQL_DB}}},
 * {@code {{ATLAS_CLUSTER}}}, {@code {{KAFKA_CONSUMER_GROUPS}}}, {@code {{MYSQL_TABLES_PATH}}}.
 */
public final class ValidationPreamblePrompt {
    public static final String CONTENT;

    /** What the static session is told it owns. */
    public static final String SCOPE_STATIC = """
        This session owns **Checks 3, 4, 5 and 7** — the ones that need no external data at all. \
        Everything you need is in the report itself plus the two reference sections below, so you \
        should not need any tool other than the one that writes your output file. Another session \
        re-queries Datadog, Azure and the repositories; a third re-verifies the recommendations.""";

    /** What the data session is told it owns. */
    public static final String SCOPE_DATA = """
        This session owns **Checks 1, 2 and 6** — the ones that re-query the live data sources and \
        re-read the infra repository. Another session checks the report's internal consistency, \
        scoring, projection math and format; a third re-verifies the recommendations. Do not \
        comment on those.""";

    /** What the advice session is told it owns. */
    public static final String SCOPE_ADVICE = """
        This session owns **Check 8** alone — whether the report's recommendations are sound. \
        Another session re-queries the live data sources; a third checks the report's internal \
        consistency, scoring, projection math and format. Do not comment on those: a recommendation \
        is in scope, the metric table behind it is not.""";

    static {
        CONTENT = """
        # Scalability Review Validation

        You are a quality-assurance engineer performing an **independent validation** of an existing
        scalability review report. Your objective is to re-check the claims in that report against
        their primary sources and produce structured findings that identify inaccuracies,
        inconsistencies, math errors and format violations.

        **Important**: Do NOT generate a new scalability review. Your task is strictly to verify the
        existing report and flag any issues found.

        ## Your scope in this run

        The eight validation checks are split across three independent sessions so that no session
        carries context it does not need. {{CHECK_SCOPE}}

        Do not attempt the checks another session owns, and do not write a validation header, a
        summary table or an overall verdict — the pipeline assembles those from all three sessions
        once they have finished, and a copy written here would be merged in twice.

        ## What is being validated

        - **Report to validate**: {{REPORT_FILE}}
        - **Service**: {{SERVICE}}
        - **Environment**: {{ENV}}
        - **Namespace**: {{NAMESPACE}}
        - **Lookback window**: 28 days (same as report generation)
        - **MySQL host/cluster**: {{MYSQL_HOST}}  (N/A if service does not use MySQL)
        - **MySQL database**: {{MYSQL_DB}}  (N/A if MYSQL_HOST is N/A)
        - **Atlas MongoDB cluster**: {{ATLAS_CLUSTER}}  (N/A if service does not use MongoDB)
        - **Kafka consumer groups**: {{KAFKA_CONSUMER_GROUPS}}  (comma-separated, or "N/A")
        - **MySQL table storage**: collected by the pipeline over JDBC into `{{MYSQL_TABLES_PATH}}`.
          There is no MySQL MCP server in this run; re-read that file instead of querying the database.

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
            """;
    }

    private ValidationPreamblePrompt() {
    }
}
