package app.aiscalabilityreview.job;

import app.aiscalabilityreview.api.localreview.GenerateLocalReviewRequest;
import app.aiscalabilityreview.domain.ReviewJob.StageStatus;
import app.aiscalabilityreview.domain.ReviewReport;
import app.aiscalabilityreview.exception.FatalStageException;
import app.aiscalabilityreview.prompt.LocalReviewPromptBuilder;
import app.aiscalabilityreview.prompt.LocalReviewPromptBuilder.MetricCollectionParams;
import app.aiscalabilityreview.prompt.LocalReviewPromptBuilder.ReviewPromptParams;
import app.aiscalabilityreview.prompt.LocalReviewPromptBuilder.ValidationPromptParams;
import app.aiscalabilityreview.service.GeminiCliService;
import app.aiscalabilityreview.service.GeminiCliService.GeminiRunRequest;
import app.aiscalabilityreview.service.GeminiCliService.GeminiRunResult;
import app.aiscalabilityreview.service.GeminiModels;
import app.aiscalabilityreview.service.InfraContextService;
import app.aiscalabilityreview.service.InfraContextService.InfraContextParams;
import app.aiscalabilityreview.service.ReviewService;
import app.aiscalabilityreview.service.builder.LocalReviewReportBuilder;
import app.aiscalabilityreview.service.builder.LocalReviewReportBuilder.LocalReportParams;
import core.framework.inject.Inject;
import core.framework.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Orchestrates the local review stages using the local Gemini CLI. Outputs are written to disk under
 * the configured output directory, and job status is tracked in the ReviewJob + ReviewReport
 * MongoDB collections.
 *
 * <p>The run is split so that no single model turn carries both a large context and a long tool
 * loop (AD-6383) — in an agentic loop the whole context is re-sent on every round-trip, so those two
 * costs multiply:
 * <ol>
 *   <li><b>code analysis</b> — reads the application source, no MCP server.</li>
 *   <li><b>infra context</b> — runs in this service, selects the relevant infra files, zero tokens.</li>
 *   <li><b>metric collection</b> — the only stage with MCP servers; small context, many tool calls,
 *       cheapest model.</li>
 *   <li><b>synthesis</b> — large context, no tool calls, no MCP servers; does the scoring.</li>
 *   <li><b>validation</b> — independent re-check, optional.</li>
 * </ol>
 */
public class LocalReviewJobExecutor {
    private static final String STAGE_CODE_ANALYSIS = "STAGE_1_CODE_ANALYSIS";
    private static final String STAGE_INFRA_CONTEXT = "STAGE_2_INFRA_CONTEXT";
    private static final String STAGE_METRIC_COLLECTION = "STAGE_3_METRIC_COLLECTION";
    private static final String STAGE_SYNTHESIS = "STAGE_4_SYNTHESIS";
    private static final String STAGE_VALIDATION = "STAGE_5_VALIDATION";

    private static final String DATADOG_MCP = "datadog";
    private static final String AZURE_MCP = "azure";
    private static final List<String> NO_MCP = List.of(GeminiCliService.MCP_DISABLED);

    private static final int CODE_ANALYSIS_TIMEOUT_MINUTES = 15;
    private static final int METRIC_COLLECTION_TIMEOUT_MINUTES = 20;
    private static final int SYNTHESIS_TIMEOUT_MINUTES = 15;
    private static final int VALIDATION_TIMEOUT_MINUTES = 20;

    private static final Pattern VERDICT_PATTERN = Pattern.compile("(?i)Overall Validation Verdict:\\s*\\**\\s*([A-Z ]+)");
    private static final String CODE_CONTEXT_PLACEHOLDER =
        "Code context: not available — the code analysis stage was skipped for this run.\n";

    public static String resolveOutputDir(GenerateLocalReviewRequest request) {
        String base = !Strings.isBlank(request.outputBaseDir) ? request.outputBaseDir : "reports";
        return base + "/" + request.service;
    }

    private final Logger logger = LoggerFactory.getLogger(LocalReviewJobExecutor.class);

    // Holds request params for async execution, keyed by jobId.
    private final Map<String, GenerateLocalReviewRequest> pendingRequests = new ConcurrentHashMap<>();

    @Inject
    ReviewService reviewService;
    @Inject
    GeminiCliService geminiCliService;
    @Inject
    InfraContextService infraContextService;

    public void registerRequest(String jobId, GenerateLocalReviewRequest request) {
        pendingRequests.put(jobId, request);
    }

    public void executeLocalReview(String jobId) {
        GenerateLocalReviewRequest request = pendingRequests.remove(jobId);
        if (request == null) {
            logger.error("No request registered for local review job {}", jobId);
            reviewService.failJob(jobId, "No request params found for job " + jobId);
            return;
        }

        String outputDir = Path.of(resolveOutputDir(request)).toAbsolutePath().toString();
        logger.info("Starting local review job {} (service={}, outputDir={})", jobId, request.service, outputDir);
        try {
            Files.createDirectories(Path.of(outputDir));
        } catch (IOException e) {
            reviewService.failJob(jobId, "Failed to create output directory: " + e.getMessage());
            return;
        }

        String today = ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        OutputPaths paths = new OutputPaths(outputDir,
            LocalReviewPromptBuilder.codeContextPath(outputDir),
            LocalReviewPromptBuilder.infraContextPath(outputDir),
            LocalReviewPromptBuilder.metricsPath(outputDir),
            LocalReviewPromptBuilder.reviewReportPath(outputDir, today),
            LocalReviewPromptBuilder.validationReportPath(outputDir, today),
            LocalReviewPromptBuilder.reportSnapshotPath(outputDir));
        LocalReviewRunStats stats = new LocalReviewRunStats();

        try {
            runStages(jobId, request, paths, stats);
        } catch (FatalStageException e) {
            logger.error("Fatal stage failure in local review job {} at {}: {}", jobId, e.stageName, e.getMessage());
            reviewService.failJob(jobId, "Stage " + e.stageName + " failed: " + e.getCause().getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error in local review job {}: {}", jobId, e.getMessage(), e);
            reviewService.failJob(jobId, "Unexpected error: " + e.getMessage());
        } finally {
            reportRunStats(jobId, request.service, paths, stats);
        }
    }

    private void runStages(String jobId, GenerateLocalReviewRequest request, OutputPaths paths, LocalReviewRunStats stats) {
        runCodeAnalysisStage(jobId, request, paths, stats);
        runInfraContextStage(jobId, request, paths, stats);
        runMetricCollectionStage(jobId, request, paths, stats);
        runSynthesisStage(jobId, request, paths, stats);
        runValidationStage(jobId, request, paths, stats);

        String reviewMarkdown = readFileQuietly(paths.reviewReport());
        ReviewReport report = LocalReviewReportBuilder.reviewReport(new LocalReportParams(jobId, request.service,
            reviewMarkdown, paths.reviewReport(), readFileQuietly(paths.reportSnapshot()),
            GeminiModels.orDefault(request.synthesisModel, GeminiModels.SYNTHESIS_DEFAULT)));
        reviewService.saveReport(report);
        reviewService.completeJob(jobId, report.reportId);
        logger.info("Local review job {} completed: reportId={}, overallScore={}, outputDir={}",
            jobId, report.reportId, report.overallScore, paths.outputDir());
    }

    private void runCodeAnalysisStage(String jobId, GenerateLocalReviewRequest request, OutputPaths paths, LocalReviewRunStats stats) {
        if (Boolean.TRUE.equals(request.skipCodeAnalysis)) {
            ensureCodeContextExists(paths.codeContext());
            skipStage(jobId, STAGE_CODE_ANALYSIS, "skipped by request");
            return;
        }
        String model = GeminiModels.orDefault(request.codeAnalysisModel, GeminiModels.CODE_ANALYSIS_DEFAULT);
        runGeminiStage(jobId, STAGE_CODE_ANALYSIS, true, stats, () -> {
            String prompt = LocalReviewPromptBuilder.buildCodeAnalysisPrompt(
                request.service, request.localAppRepoPath, request.appCodePaths, paths.codeContext());
            return geminiCliService.run(new GeminiRunRequest(prompt, model,
                List.of(request.localAppRepoPath, paths.outputDir()), NO_MCP, CODE_ANALYSIS_TIMEOUT_MINUTES));
        });
    }

    private void runInfraContextStage(String jobId, GenerateLocalReviewRequest request, OutputPaths paths, LocalReviewRunStats stats) {
        runLocalStage(jobId, STAGE_INFRA_CONTEXT, stats, () ->
            infraContextService.build(new InfraContextParams(request.localInfraRepoPath, request.env, request.domain,
                request.service, request.mysqlHost, request.atlasCluster, paths.infraContext())));
    }

    private void runMetricCollectionStage(String jobId, GenerateLocalReviewRequest request, OutputPaths paths, LocalReviewRunStats stats) {
        String model = GeminiModels.orDefault(request.metricCollectionModel, GeminiModels.METRIC_COLLECTION_DEFAULT);
        runGeminiStage(jobId, STAGE_METRIC_COLLECTION, true, stats, () -> {
            MetricCollectionParams params = new MetricCollectionParams();
            params.serviceId = request.service;
            params.env = request.env;
            params.namespace = request.namespace;
            params.mysqlHost = request.mysqlHost;
            params.mysqlDb = request.mysqlDB;
            params.atlasCluster = request.atlasCluster;
            params.kafkaConsumerGroups = request.kafkaConsumerGroups;
            params.outputPath = paths.metrics();
            return geminiCliService.run(new GeminiRunRequest(
                LocalReviewPromptBuilder.buildMetricCollectionPrompt(params), model,
                List.of(paths.outputDir()), dataMcpServers(request), METRIC_COLLECTION_TIMEOUT_MINUTES));
        });
    }

    private void runSynthesisStage(String jobId, GenerateLocalReviewRequest request, OutputPaths paths, LocalReviewRunStats stats) {
        String model = GeminiModels.orDefault(request.synthesisModel, GeminiModels.SYNTHESIS_DEFAULT);
        runGeminiStage(jobId, STAGE_SYNTHESIS, true, stats, () -> {
            ReviewPromptParams params = new ReviewPromptParams();
            params.serviceId = request.service;
            params.codeContextPath = paths.codeContext();
            params.infraContextPath = paths.infraContext();
            params.metricsPath = paths.metrics();
            params.env = request.env;
            params.namespace = request.namespace;
            params.mysqlHost = request.mysqlHost;
            params.mysqlDb = request.mysqlDB;
            params.atlasCluster = request.atlasCluster;
            params.hpaType = request.hpaType;
            params.kafkaConsumerGroups = request.kafkaConsumerGroups;
            params.outputPath = paths.reviewReport();
            params.snapshotPath = paths.reportSnapshot();
            return geminiCliService.run(new GeminiRunRequest(
                LocalReviewPromptBuilder.buildReviewPrompt(params), model,
                List.of(paths.outputDir()), NO_MCP, SYNTHESIS_TIMEOUT_MINUTES));
        });
    }

    private void runValidationStage(String jobId, GenerateLocalReviewRequest request, OutputPaths paths, LocalReviewRunStats stats) {
        if (Boolean.TRUE.equals(request.skipValidation)) {
            skipStage(jobId, STAGE_VALIDATION, "skipped by request");
            return;
        }
        String model = GeminiModels.orDefault(request.validationModel, GeminiModels.VALIDATION_DEFAULT);
        runGeminiStage(jobId, STAGE_VALIDATION, false, stats, () -> {
            ValidationPromptParams params = new ValidationPromptParams();
            params.serviceId = request.service;
            params.reviewReportPath = paths.reviewReport();
            params.infraContextPath = paths.infraContext();
            params.localInfraRepoPath = request.localInfraRepoPath;
            params.env = request.env;
            params.namespace = request.namespace;
            params.mysqlHost = request.mysqlHost;
            params.mysqlDb = request.mysqlDB;
            params.atlasCluster = request.atlasCluster;
            params.kafkaConsumerGroups = request.kafkaConsumerGroups;
            params.outputPath = paths.validationReport();
            GeminiRunResult result = geminiCliService.run(new GeminiRunRequest(
                LocalReviewPromptBuilder.buildValidationPrompt(params), model,
                List.of(paths.outputDir(), request.localInfraRepoPath), dataMcpServers(request), VALIDATION_TIMEOUT_MINUTES));
            logger.info("Validation verdict for job {}: {}", jobId, verdict(readFileQuietly(paths.validationReport())));
            return result;
        });
    }

    /**
     * The synthesis prompt attaches the code context with an @file reference, so a skipped stage must
     * still leave a readable file behind — a previous run's file is reused, otherwise a placeholder
     * tells the synthesis stage the context is genuinely unavailable rather than silently empty.
     */
    private void ensureCodeContextExists(String codeContextPath) {
        Path path = Path.of(codeContextPath);
        if (Files.exists(path)) return;
        try {
            Files.writeString(path, CODE_CONTEXT_PLACEHOLDER);
            logger.warn("No code context at {}; wrote a placeholder because code analysis was skipped", path);
        } catch (IOException e) {
            logger.warn("Could not write the code context placeholder to {}: {}", path, e.getMessage());
        }
    }

    /** Only the MCP servers the data stages actually query, so the rest are neither started nor described to the model. */
    private List<String> dataMcpServers(GenerateLocalReviewRequest request) {
        List<String> servers = new ArrayList<>(3);
        servers.add(DATADOG_MCP);
        servers.add(AZURE_MCP);
        String mysqlMcp = LocalReviewPromptBuilder.mysqlMcpName(request.mysqlHost, request.mysqlDB);
        if (mysqlMcp != null) servers.add(mysqlMcp);
        return servers;
    }

    private void runGeminiStage(String jobId, String stageName, boolean fatal, LocalReviewRunStats stats, GeminiStageAction action) {
        StageStatus stageStatus = startStage(jobId, stageName);
        long startMs = System.currentTimeMillis();
        try {
            GeminiRunResult result = action.execute();
            stats.record(stageName, result);
            stageStatus.model = result.model();
            stageStatus.inputTokens = result.inputTokens();
            stageStatus.outputTokens = result.outputTokens();
            stageStatus.cachedTokens = result.cachedTokens();
            stageStatus.totalTokens = result.totalTokens();
            stageStatus.apiRequests = result.apiRequests();
            stageStatus.toolCalls = result.toolCalls();
            stageStatus.mcpToolCalls = result.mcpToolCalls();
            completeStage(jobId, stageName, stageStatus, startMs);
        } catch (Exception e) {
            failStage(jobId, stageName, stageStatus, startMs, e);
            if (fatal) throw new FatalStageException(stageName, e);
        }
    }

    private void runLocalStage(String jobId, String stageName, LocalReviewRunStats stats, LocalStageAction action) {
        StageStatus stageStatus = startStage(jobId, stageName);
        long startMs = System.currentTimeMillis();
        try {
            stageStatus.outputSummary = action.execute();
            stats.recordLocal(stageName, System.currentTimeMillis() - startMs);
            completeStage(jobId, stageName, stageStatus, startMs);
        } catch (Exception e) {
            failStage(jobId, stageName, stageStatus, startMs, e);
            throw new FatalStageException(stageName, e);
        }
    }

    private StageStatus startStage(String jobId, String stageName) {
        reviewService.updateJobStatus(jobId, "RUNNING", stageName);
        StageStatus stageStatus = new StageStatus();
        stageStatus.status = "RUNNING";
        stageStatus.startedAt = ZonedDateTime.now();
        reviewService.updateJobStageStatus(jobId, stageName, stageStatus);
        return stageStatus;
    }

    private void completeStage(String jobId, String stageName, StageStatus stageStatus, long startMs) {
        stageStatus.status = "COMPLETED";
        stageStatus.completedAt = ZonedDateTime.now();
        stageStatus.durationMs = System.currentTimeMillis() - startMs;
        reviewService.updateJobStageStatus(jobId, stageName, stageStatus);
        logger.info("Stage {} completed in {}ms for job {} (model={}, inputTokens={}, outputTokens={}, mcpToolCalls={})",
            stageName, stageStatus.durationMs, jobId, stageStatus.model, stageStatus.inputTokens,
            stageStatus.outputTokens, stageStatus.mcpToolCalls);
    }

    private void failStage(String jobId, String stageName, StageStatus stageStatus, long startMs, Exception e) {
        stageStatus.status = "FAILED";
        stageStatus.completedAt = ZonedDateTime.now();
        stageStatus.durationMs = System.currentTimeMillis() - startMs;
        stageStatus.errorMessage = e.getMessage();
        reviewService.updateJobStageStatus(jobId, stageName, stageStatus);
        logger.error("Stage {} failed after {}ms for job {}: {}", stageName, stageStatus.durationMs, jobId, e.getMessage());
    }

    private void skipStage(String jobId, String stageName, String reason) {
        logger.info("Skipping {} for job {}: {}", stageName, jobId, reason);
        StageStatus skipped = new StageStatus();
        skipped.status = "SKIPPED";
        skipped.startedAt = ZonedDateTime.now();
        skipped.completedAt = skipped.startedAt;
        skipped.durationMs = 0L;
        skipped.outputSummary = reason;
        reviewService.updateJobStageStatus(jobId, stageName, skipped);
    }

    private void reportRunStats(String jobId, String serviceId, OutputPaths paths, LocalReviewRunStats stats) {
        if (stats.withinBudget()) {
            logger.info("Local review job {} cost: {}", jobId, stats.summaryLine());
        } else {
            logger.warn("Local review job {} exceeded the cost/latency budget: {}", jobId, stats.summaryLine());
        }
        Path statsPath = Path.of(LocalReviewPromptBuilder.runStatsPath(paths.outputDir()));
        try {
            Files.writeString(statsPath, stats.render(serviceId, jobId));
        } catch (IOException e) {
            logger.warn("Could not write run stats to {}: {}", statsPath, e.getMessage());
        }
    }

    private String verdict(String validationMarkdown) {
        if (validationMarkdown == null) return "unknown";
        Matcher matcher = VERDICT_PATTERN.matcher(validationMarkdown);
        return matcher.find() ? matcher.group(1).trim() : "unknown";
    }

    private String readFileQuietly(String path) {
        try {
            return Files.readString(Path.of(path).toAbsolutePath());
        } catch (IOException e) {
            logger.warn("Could not read file {}: {}", path, e.getMessage());
            return "";
        }
    }

    @FunctionalInterface
    private interface GeminiStageAction {
        GeminiRunResult execute() throws Exception;
    }

    @FunctionalInterface
    private interface LocalStageAction {
        String execute() throws Exception;
    }

    private record OutputPaths(String outputDir,
                               String codeContext,
                               String infraContext,
                               String metrics,
                               String reviewReport,
                               String validationReport,
                               String reportSnapshot) {
    }
}
