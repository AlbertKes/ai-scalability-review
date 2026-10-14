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
    /** Reads as an instruction in the prompts: an unresolved Azure name must be discovered, not guessed. */
    private static final String UNKNOWN = "unknown";

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

    private static final String VALIDATION_OUTPUT_HEADING = "\n\n---\n\n";
    private static final String SCORING_REFERENCE_HEADING = "\n\n## Metric Scoring Reference\n\n";
    private static final String FORMAT_REFERENCE_HEADING = "\n\n## Report Format Reference\n\n";

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

    public static String mysqlTablesPath(String outputDir) {
        return outputDir + "/mysql-tables.md";
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

    public static String validationStaticPath(String outputDir) {
        return outputDir + "/validation-static.md";
    }

    public static String validationDataPath(String outputDir) {
        return outputDir + "/validation-data.md";
    }

    public static String validationAdvicePath(String outputDir) {
        return outputDir + "/validation-advice.md";
    }

    /**
     * Key under which a host/database pair is configured in {@code ~/.gemini/settings.json}, e.g.
     * {@code ftiuat-flexible-consumer-db} + {@code order} gives
     * {@code ftiuat-flexible-consumer-db-order}. Returns null when the service has no MySQL database.
     *
     * <p>No stage starts that server any more — Gemini CLI 0.46 cannot read a row-returning MySQL
     * MCP response (see {@link app.aiscalabilityreview.service.MySQLTableStorageService}) — but its
     * entry is still where the local connection details live, so the name is resolved the same way.
     */
    public static String mysqlConnectionName(String mysqlHost, String mysqlDb) {
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
        return MetricCollectionTaskPrompt.CONTENT
            .replace("{{SERVICE}}", p.serviceId)
            .replace("{{ENV}}", lower(p.env))
            .replace("{{NAMESPACE}}", p.namespace)
            .replace("{{MYSQL_HOST}}", nvl(p.mysqlHost, NOT_AVAILABLE))
            .replace("{{MYSQL_DB}}", nvl(p.mysqlDb, NOT_AVAILABLE))
            .replace("{{MYSQL_TABLES_PATH}}", p.mysqlTablesPath)
            .replace("{{AZURE_RESOURCE_GROUP}}", nvl(p.azureResourceGroup, UNKNOWN))
            .replace("{{AZURE_SUBSCRIPTION}}", nvl(p.azureSubscriptionId, UNKNOWN))
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

    /**
     * Stage 5A — the checks that are answerable from the report alone (3, 4, 5, 7). It is the only
     * group that needs the scoring thresholds and the report format spec, and now the only one that
     * carries them.
     */
    public static String buildStaticValidationPrompt(ValidationPromptParams p) {
        String prompt = preamble(ValidationPreamblePrompt.SCOPE_STATIC)
            + ValidationStaticChecksPrompt.CONTENT
            + ValidationStaticChecksPrompt.FILE_REFS
            + SCORING_REFERENCE_HEADING + MetricScorePrompt.CONTENT
            + FORMAT_REFERENCE_HEADING + ReportFormatPrompt.CONTENT
            + outputContract(ValidationChecks.STATIC_GROUP);
        return resolveValidation(prompt, p, p.staticOutputPath);
    }

    /**
     * Stage 5B — the checks that re-query the live sources (1, 2, 6). It is given neither the
     * scoring thresholds nor the report format spec: 27 KB that only checks 4 and 7 read, and in an
     * agentic loop every byte of it was re-sent on each of this group's tool round-trips.
     */
    public static String buildDataValidationPrompt(ValidationPromptParams p) {
        String prompt = preamble(ValidationPreamblePrompt.SCOPE_DATA)
            + ValidationDataChecksPrompt.CONTENT
            + ValidationDataChecksPrompt.FILE_REFS
            + outputContract(ValidationChecks.DATA_GROUP);
        return resolveValidation(prompt, p, p.dataOutputPath);
    }

    /**
     * Stage 5C — the one check that re-reads the application source (8). Its file reads are what
     * made the single-session validation expensive, so nothing else now shares a context with them.
     */
    public static String buildAdviceValidationPrompt(ValidationPromptParams p) {
        String prompt = preamble(ValidationPreamblePrompt.SCOPE_ADVICE)
            + RecommendationSoundnessPrompt.CONTENT
            + RecommendationSoundnessPrompt.FILE_REFS
            + outputContract(ValidationChecks.ADVICE_GROUP);
        return resolveValidation(prompt, p, p.adviceOutputPath);
    }

    private static String preamble(String scope) {
        return ValidationPreamblePrompt.CONTENT.replace("{{CHECK_SCOPE}}", scope);
    }

    private static String outputContract(List<Integer> group) {
        return VALIDATION_OUTPUT_HEADING
            + ValidationFormatPrompt.CONTENT.replace("{{CHECK_HEADERS}}", ValidationChecks.headers(group))
            + ValidationFormatPrompt.FILE_OUTPUT;
    }

    /** The placeholders every validation group shares, resolved once over the assembled prompt. */
    private static String resolveValidation(String prompt, ValidationPromptParams p, String outputPath) {
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
            .replace("{{APP_REPO_PATH}}", nvl(p.localAppRepoPath, "not provided"))
            .replace("{{APP_SOURCE_ROOTS}}", appSourceRoots(p.localAppRepoPath, p.appCodePaths))
            .replace("{{INFRA_CONTEXT_PATH}}", p.infraContextPath)
            .replace("{{MYSQL_TABLES_PATH}}", p.mysqlTablesPath)
            .replace("{{OUTPUT_PATH}}", outputPath);
    }

    /**
     * Where this service's own code lives, for the stage that re-reads it.
     *
     * <p>Naming the modules rather than the repository root is the difference between opening a
     * file and searching a monorepo for it: the measured validation stage spent 49 tool calls, and
     * every application file it ended up citing was inside the three modules the request already
     * named.
     */
    private static String appSourceRoots(String appRepoPath, List<String> codePaths) {
        if (appRepoPath == null) return "not provided";
        if (codePaths == null || codePaths.isEmpty()) return appRepoPath;
        return codePaths.stream()
            .map(path -> appRepoPath + '/' + trimSlashes(path))
            .collect(Collectors.joining("\n- ", "\n- ", ""));
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
        public String mysqlTablesPath;
        public String azureResourceGroup;
        public String azureSubscriptionId;
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
        public String localAppRepoPath;
        public List<String> appCodePaths;
        public String env;
        public String namespace;
        public String mysqlHost;
        public String mysqlDb;
        public String atlasCluster;
        public String kafkaConsumerGroups;
        public String mysqlTablesPath;
        /** Where each group writes its partial; the pipeline merges the three into one report. */
        public String staticOutputPath;
        public String dataOutputPath;
        public String adviceOutputPath;
    }
}
