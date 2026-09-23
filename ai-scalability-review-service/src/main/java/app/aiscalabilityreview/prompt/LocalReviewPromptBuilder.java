package app.aiscalabilityreview.prompt;

import core.framework.util.ClasspathResources;
import core.framework.util.Strings;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Builds the Gemini CLI prompts for the four local review stages.
 *
 * <p>Each prompt carries only the references and reference material its own stage needs, which is
 * what keeps the token cost down (AD-6383): the collection stage gets no report format and no code
 * context, and the synthesis stage gets no MCP query instructions. All reference material that used
 * to be cited by repository path is embedded inline, because the Gemini CLI runs from a temporary
 * working directory and cannot open this project's source tree.
 *
 * <p>Each builder concatenates its templates first and resolves the {@code {{...}}} placeholders
 * once over the whole prompt, so a placeholder means the same thing in every part of it.
 */
public class LocalReviewPromptBuilder {
    private static final String CHECKPOINT = ClasspathResources.text("doc/scalability-review-checkpoint.md");
    private static final String NOT_AVAILABLE = "N/A";

    private static final String CODE_ANALYSIS_SOURCES = """


        ## Source Code

        The following files are from the **{{SERVICE}}** application repository. Analyze them per the
        instructions above. Ignore generated sources, build output and test fixtures; they do not
        describe runtime behaviour.

        {{SOURCES}}

        ## Output

        Write the complete code analysis output to the file `{{OUTPUT_PATH}}` using the file system
        write tool.
        """;

    private static final String SYNTHESIS_INPUT_FILES = """
        ## Attached Input Files

        The three collected input files are attached below. Do not call any MCP server; everything
        the review needs is in them.

        **Filtered infrastructure configuration** (`{{INFRA_CONTEXT_PATH}}`):
        @{{INFRA_CONTEXT_PATH}}

        **Business context from the application source** (`{{CODE_CONTEXT_PATH}}`):
        @{{CODE_CONTEXT_PATH}}

        **Collected 28-day metrics and live Azure state** (`{{METRICS_PATH}}`):
        @{{METRICS_PATH}}

        ---

        """;

    private static final String COVERAGE_CHECKLIST_NOTE = """


        ## Review Coverage Checklist

        The checklist below is the scalability review programme this report feeds. Use it only to
        sanity-check that your findings and recommendations cover the areas the collected data can
        speak to. Do **not** add, rename or reorder report sections because of it, and do **not**
        report on an area for which no data was collected.

        """;

    private static final String SYNTHESIS_OUTPUT_CONTRACT = """


        Now produce the complete scalability review report.

        Write the complete review report to file `{{OUTPUT_PATH}}` using the file system write tool.

        Then write a second file, `{{SNAPSHOT_PATH}}`, containing only this JSON object. It is
        machine-read by the pipeline, so use exactly these keys, numbers without units or quotes,
        `null` for anything the report marks NOT_COLLECTED, and the same values the report states —
        do not recompute them.

        ```json
        {
          "traffic_score": "GREEN|YELLOW|RED",
          "latency_score": "GREEN|YELLOW|RED",
          "errors_score": "GREEN|YELLOW|RED",
          "resources_score": "GREEN|YELLOW|RED",
          "persistence_score": "GREEN|YELLOW|RED",
          "overall_score": "GREEN|YELLOW|RED",
          "avg_rps": null,
          "p99_latency_ms": null,
          "error_rate_pct": null,
          "cpu_avg_pct": null,
          "mem_avg_pct": null,
          "mysql_connection_pct": null,
          "mysql_replication_lag_s": null
        }
        ```

        `cpu_avg_pct` and `mem_avg_pct` are percentages of the pod limit; `mysql_connection_pct` is
        the average as a percentage of max_connections.
        """;

    private static final String VALIDATION_FILE_REFS = """


        ## Attached Local Files

        **Review report to validate**:
        @{{REPORT_FILE}}

        **Infra repository root** (for re-reading source-annotated config files):
        Local path: `{{INFRA_REPO_PATH}}`

        When the report cites `[Source: code -> <path>]`, read that file from this root with the read
        tool. `{{INFRA_CONTEXT_PATH}}` lists which files the review stage was given and which were
        skipped, so start there when you need to locate a cited file. Read only the files you
        actually need in order to verify a claim.
        """;

    private static final String VALIDATION_OUTPUT_CONTRACT = """


        Now produce the complete validation report.

        Write the complete validation report to file `{{OUTPUT_PATH}}` using the file system write
        tool.
        """;

    private static final String SCORING_REFERENCE_HEADING = "\n\n## Metric Scoring Reference\n\n";
    private static final String FORMAT_REFERENCE_HEADING = "\n\n## Report Format Reference\n\n";
    private static final String VALIDATION_REFERENCE_HEADING = "\n\n## Validation Format Reference\n\n";

    // --- Output path factory methods ---

    public static String codeContextPath(String outputDir) {
        return outputDir + "/code-context.md";
    }

    public static String infraContextPath(String outputDir) {
        return outputDir + "/infra-context.md";
    }

    public static String metricsPath(String outputDir) {
        return outputDir + "/metrics.md";
    }

    public static String reportSnapshotPath(String outputDir) {
        return outputDir + "/report-snapshot.json";
    }

    public static String runStatsPath(String outputDir) {
        return outputDir + "/run-stats.md";
    }

    public static String reviewReportPath(String outputDir, String date) {
        return outputDir + "/" + date + "-review.md";
    }

    public static String validationReportPath(String outputDir, String date) {
        return outputDir + "/" + date + "-validation.md";
    }

    /**
     * Name of the locally configured MySQL MCP server for a host/database pair, e.g.
     * {@code ftiuat-flexible-consumer-db} + {@code order} gives
     * {@code ftiuat-flexible-consumer-db-order}. Returns null when the service has no MySQL database.
     */
    public static String mysqlMcpName(String mysqlHost, String mysqlDb) {
        if (isAbsent(mysqlHost) || isAbsent(mysqlDb)) return null;
        int dot = mysqlHost.indexOf('.');
        String host = dot > 0 ? mysqlHost.substring(0, dot) : mysqlHost;
        return host + "-" + mysqlDb;
    }

    /**
     * Stage 1 — code analysis. {@code codePaths} narrows the injected source tree to the modules
     * that belong to this service; the whole repository is injected when it is null or empty.
     */
    public static String buildCodeAnalysisPrompt(String serviceId, String appRepoPath, List<String> codePaths, String outputPath) {
        String sources = codePaths == null || codePaths.isEmpty()
            ? "@" + appRepoPath
            : codePaths.stream().map(path -> "@" + appRepoPath + '/' + trimSlashes(path)).collect(Collectors.joining("\n"));
        return (CodeAnalysisTaskPrompt.CONTENT + CODE_ANALYSIS_SOURCES)
            .replace("{{SERVICE}}", serviceId)
            .replace("{{SOURCES}}", sources)
            .replace("{{OUTPUT_PATH}}", outputPath);
    }

    /** Stage 3 — metric collection. The only stage that reaches an MCP server. */
    public static String buildMetricCollectionPrompt(MetricCollectionParams p) {
        String mysqlMcp = mysqlMcpName(p.mysqlHost, p.mysqlDb);
        return MetricCollectionTaskPrompt.CONTENT
            .replace("{{SERVICE}}", p.serviceId)
            .replace("{{ENV}}", lower(p.env))
            .replace("{{NAMESPACE}}", p.namespace)
            .replace("{{MYSQL_HOST}}", nvl(p.mysqlHost, NOT_AVAILABLE))
            .replace("{{MYSQL_DB}}", nvl(p.mysqlDb, NOT_AVAILABLE))
            .replace("{{MYSQL_MCP}}", mysqlMcp == null ? "not configured for this service" : mysqlMcp)
            .replace("{{ATLAS_CLUSTER}}", nvl(p.atlasCluster, NOT_AVAILABLE))
            .replace("{{KAFKA_CONSUMER_GROUPS}}", nvl(p.kafkaConsumerGroups, NOT_AVAILABLE))
            .replace("{{OUTPUT_PATH}}", p.outputPath);
    }

    /** Stage 4 — synthesis and scoring. Reads the three collected files, calls no MCP server. */
    public static String buildReviewPrompt(ReviewPromptParams p) {
        String prompt = SYNTHESIS_INPUT_FILES
            + ReviewTaskPrompt.CONTENT
            + SCORING_REFERENCE_HEADING + MetricScorePrompt.CONTENT
            + FORMAT_REFERENCE_HEADING + ReportFormatPrompt.CONTENT
            + COVERAGE_CHECKLIST_NOTE + CHECKPOINT
            + SYNTHESIS_OUTPUT_CONTRACT;
        return prompt
            .replace("{{SERVICE}}", p.serviceId)
            .replace("{{ENV}}", lower(p.env))
            .replace("{{NAMESPACE}}", p.namespace)
            .replace("{{MYSQL_HOST}}", nvl(p.mysqlHost, NOT_AVAILABLE))
            .replace("{{MYSQL_DB}}", nvl(p.mysqlDb, NOT_AVAILABLE))
            .replace("{{ATLAS_CLUSTER}}", nvl(p.atlasCluster, NOT_AVAILABLE))
            .replace("{{HPA_TYPE}}", nvl(p.hpaType, "none"))
            .replace("{{KAFKA_CONSUMER_GROUPS}}", nvl(p.kafkaConsumerGroups, NOT_AVAILABLE))
            .replace("{{CODE_CONTEXT_PATH}}", p.codeContextPath)
            .replace("{{INFRA_CONTEXT_PATH}}", p.infraContextPath)
            .replace("{{METRICS_PATH}}", p.metricsPath)
            .replace("{{SNAPSHOT_PATH}}", p.snapshotPath)
            .replace("{{OUTPUT_PATH}}", p.outputPath);
    }

    /** Stage 5 — independent validation of the generated report. */
    public static String buildValidationPrompt(ValidationPromptParams p) {
        String prompt = ValidateReportTaskPrompt.CONTENT
            + VALIDATION_FILE_REFS
            + SCORING_REFERENCE_HEADING + MetricScorePrompt.CONTENT
            + FORMAT_REFERENCE_HEADING + ReportFormatPrompt.CONTENT
            + VALIDATION_REFERENCE_HEADING + ValidationFormatPrompt.CONTENT
            + VALIDATION_OUTPUT_CONTRACT;
        return prompt
            .replace("{{REPORT_FILE}}", p.reviewReportPath)
            .replace("{{SERVICE}}", p.serviceId)
            .replace("{{ENV}}", lower(p.env))
            .replace("{{NAMESPACE}}", p.namespace)
            .replace("{{MYSQL_HOST}}", nvl(p.mysqlHost, NOT_AVAILABLE))
            .replace("{{MYSQL_DB}}", nvl(p.mysqlDb, NOT_AVAILABLE))
            .replace("{{ATLAS_CLUSTER}}", nvl(p.atlasCluster, NOT_AVAILABLE))
            .replace("{{KAFKA_CONSUMER_GROUPS}}", nvl(p.kafkaConsumerGroups, NOT_AVAILABLE))
            .replace("{{INFRA_REPO_PATH}}", nvl(p.localInfraRepoPath, "not provided"))
            .replace("{{INFRA_CONTEXT_PATH}}", p.infraContextPath)
            .replace("{{OUTPUT_PATH}}", p.outputPath);
    }

    private static boolean isAbsent(String value) {
        return Strings.isBlank(value) || NOT_AVAILABLE.equalsIgnoreCase(value);
    }

    private static String lower(String value) {
        return value.toLowerCase(Locale.US);
    }

    private static String trimSlashes(String path) {
        String trimmed = path;
        while (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1);
        }
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private static String nvl(String value, String defaultValue) {
        return Strings.isBlank(value) ? defaultValue : value;
    }

    public static class MetricCollectionParams {
        public String serviceId;
        public String env;
        public String namespace;
        public String mysqlHost;
        public String mysqlDb;
        public String atlasCluster;
        public String kafkaConsumerGroups;
        public String outputPath;
    }

    public static class ReviewPromptParams {
        public String serviceId;
        public String codeContextPath;
        public String infraContextPath;
        public String metricsPath;
        public String env;
        public String namespace;
        public String mysqlHost;
        public String mysqlDb;
        public String atlasCluster;
        public String hpaType;
        public String kafkaConsumerGroups;
        public String outputPath;
        public String snapshotPath;
    }

    public static class ValidationPromptParams {
        public String serviceId;
        public String reviewReportPath;
        public String infraContextPath;
        public String localInfraRepoPath;
        public String env;
        public String namespace;
        public String mysqlHost;
        public String mysqlDb;
        public String atlasCluster;
        public String kafkaConsumerGroups;
        public String outputPath;
    }
}
