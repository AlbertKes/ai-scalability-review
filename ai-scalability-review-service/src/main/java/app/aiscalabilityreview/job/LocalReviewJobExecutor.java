package app.aiscalabilityreview.job;

import app.aiscalabilityreview.api.localreview.GenerateLocalReviewRequest;
import app.aiscalabilityreview.domain.ReviewJob;
import app.aiscalabilityreview.domain.ReviewReport;
import app.aiscalabilityreview.exception.FatalStageException;
import app.aiscalabilityreview.job.LocalReviewRunRegistry.RunHandle;
import app.aiscalabilityreview.job.LocalReviewStageRunner.StageInput;
import app.aiscalabilityreview.prompt.LocalReviewPromptBuilder;
import app.aiscalabilityreview.prompt.LocalReviewPromptBuilder.MetricCollectionParams;
import app.aiscalabilityreview.prompt.LocalReviewPromptBuilder.ReviewPromptParams;
import app.aiscalabilityreview.service.AppSettingsService;
import app.aiscalabilityreview.service.DatadogMetricsCollector;
import app.aiscalabilityreview.service.DatadogMetricsCollector.Collected;
import app.aiscalabilityreview.service.DatadogMetricsCollector.Params;
import app.aiscalabilityreview.service.GeminiCliService;
import app.aiscalabilityreview.service.GeminiCliService.GeminiRunRequest;
import app.aiscalabilityreview.service.GeminiCliService.GeminiRunResult;
import app.aiscalabilityreview.service.GeminiModels;
import app.aiscalabilityreview.service.InfraContextService;
import app.aiscalabilityreview.service.InfraContextService.InfraContextParams;
import app.aiscalabilityreview.service.MetricsAssembler;
import app.aiscalabilityreview.service.MySQLTableStorageService;
import app.aiscalabilityreview.service.MySQLTableStorageService.MySQLTableParams;
import app.aiscalabilityreview.service.ReviewService;
import app.aiscalabilityreview.service.builder.LocalReviewRequestMapper;
import core.framework.async.Executor;
import core.framework.inject.Inject;
import core.framework.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Future;

/**
 * Orchestrates the local review stages using the local Gemini CLI. Outputs are written to disk in a
 * directory of this run's own, and job status is tracked in the ReviewJob + ReviewReport MongoDB
 * collections.
 *
 * <p>The run is split so that no single model turn carries both a large context and a long tool
 * loop (AD-6383) — in an agentic loop the whole context is re-sent on every round-trip, so those two
 * costs multiply:
 * <ol>
 *   <li><b>code analysis</b> — reads the application source, no MCP server.</li>
 *   <li><b>infra context</b> — runs in this service, selects the relevant infra files and queries
 *       the MySQL table sizes over JDBC, zero tokens.</li>
 *   <li><b>metric collection</b> — the Datadog figures are read here over the REST API; a model reads
 *       the Azure state through the Azure MCP server.</li>
 *   <li><b>synthesis</b> — large context, no tool calls, no MCP servers; does the scoring.</li>
 *   <li><b>validation</b> — independent re-check, optional.</li>
 * </ol>
 *
 * <p>The request comes from the job document rather than from memory, so a restart cannot strand a
 * run, and every run writes into {@link LocalReviewOutputPaths#runDir} rather than into a directory
 * shared by every run of the service.
 */
public class LocalReviewJobExecutor {
    /** Bumped whenever the prompt templates change, so two runs can be told apart (FR-6.7). */
    public static final String PROMPT_VERSION = "AD-6385-overall-rule";

    private static final String AZURE_MCP = "azure";
    /**
     * Only the Azure MCP server is started, and only by the metric stage; Datadog is read over its REST API
     * (see {@link DatadogMetricsCollector}). The MySQL MCP server is deliberately absent:
     * Gemini CLI (first seen on 0.46, not re-tested on 0.69) cannot read a row-returning response from it (see
     * {@link MySQLTableStorageService}), so the table sizes are collected in stage 2 instead and
     * leaving the server out of the allowlist means it is neither started nor described to the model.
     */
    private static final List<String> AZURE_MCP_ONLY = List.of(AZURE_MCP);
    private static final List<String> NO_MCP = List.of(GeminiCliService.MCP_DISABLED);

    private static final int CODE_ANALYSIS_TIMEOUT_MINUTES = GeminiCliService.STAGE_TIMEOUT_MINUTES;
    private static final int METRIC_COLLECTION_TIMEOUT_MINUTES = GeminiCliService.STAGE_TIMEOUT_MINUTES;
    private static final int SYNTHESIS_TIMEOUT_MINUTES = GeminiCliService.STAGE_TIMEOUT_MINUTES;

    private final Logger logger = LoggerFactory.getLogger(LocalReviewJobExecutor.class);

    @Inject
    ReviewService reviewService;
    @Inject
    GeminiCliService geminiCliService;
    @Inject
    InfraContextService infraContextService;
    @Inject
    MySQLTableStorageService mysqlTableStorageService;
    @Inject
    DatadogMetricsCollector datadogMetricsCollector;
    @Inject
    LocalReviewStageRunner stageRunner;
    @Inject
    LocalReviewValidationRunner validationRunner;
    @Inject
    LocalReviewRunFinalizer finalizer;
    @Inject
    LocalReviewRunRegistry registry;
    @Inject
    AppSettingsService appSettingsService;
    @Inject
    LocalReviewRunPreparer preparer;
    @Inject
    Executor executor;

    public void executeLocalReview(String jobId) {
        ReviewJob job = reviewService.getReviewJob(jobId).orElse(null);
        if (job == null || job.request == null) {
            logger.error("No stored request for local review job {}", jobId);
            reviewService.failJob(jobId, "No request found for job " + jobId);
            return;
        }
        GenerateLocalReviewRequest request = LocalReviewRequestMapper.toRequest(job.request);
        RunHandle handle = registry.register(jobId, job.serviceId);
        if (!awaitSlot(jobId)) {
            registry.unregister(jobId);
            return;
        }

        LocalReviewArtifactPaths paths;
        try {
            paths = preparer.prepare(job, request);
        } catch (IOException e) {
            reviewService.failJob(jobId, "Failed to create the run directory: " + e.getMessage());
            releaseRun(jobId);
            return;
        }

        LocalReviewRunStats stats = new LocalReviewRunStats();
        try {
            runStages(job, request, paths, stats, handle);
            ReviewReport report = finalizer.publishReport(job, paths,
                GeminiModels.orDefault(request.synthesisModel, GeminiModels.SYNTHESIS_DEFAULT));
            reviewService.completeJob(jobId, report.reportId);
            logger.info("Local review job {} completed: reportId={}, outputDir={}", jobId, report.reportId, paths.runDir());
        } catch (FatalStageException e) {
            recordStop(jobId, request.operator, handle, e);
        } catch (Exception e) {
            logger.error("Unexpected error in local review job {}: {}", jobId, e.getMessage(), e);
            reviewService.failJob(jobId, "Unexpected error: " + e.getMessage());
        } finally {
            finalizer.summarise(jobId, paths, stats);
            finalizer.notifyOutcome(jobId);
            releaseRun(jobId);
        }
    }

    private boolean awaitSlot(String jobId) {
        Integer configured = appSettingsService.get().maxConcurrentRuns;
        int limit = configured == null ? 1 : configured;
        try {
            reviewService.markQueued(jobId);
            registry.acquireSlot(jobId, limit);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            reviewService.failJob(jobId, "Interrupted while waiting for a run slot");
            return false;
        }
    }

    private void recordStop(String jobId, String operator, RunHandle handle, FatalStageException e) {
        if (handle.isCancelled()) {
            logger.info("Local review job {} cancelled at {}", jobId, e.stageName);
            reviewService.cancelJob(jobId, operator);
        } else {
            logger.error("Fatal stage failure in local review job {} at {}: {}", jobId, e.stageName, e.getMessage());
            reviewService.failJob(jobId, "Stage " + e.stageName + " failed: " + causeMessage(e));
        }
    }

    private void releaseRun(String jobId) {
        registry.releaseSlot();
        registry.unregister(jobId);
    }

    private String causeMessage(FatalStageException e) {
        return e.getCause() == null ? e.getMessage() : e.getCause().getMessage();
    }

    private void runStages(ReviewJob job, GenerateLocalReviewRequest request,
                           LocalReviewArtifactPaths paths, LocalReviewRunStats stats, RunHandle handle) {
        Future<Void> codeAnalysis = executor.submit("code-analysis-" + job.jobId,
            () -> runCodeAnalysisStage(job, request, paths, stats, handle));
        try {
            runInfraContextStage(job, request, paths, stats, handle);
            runMetricCollectionStage(job, request, paths, stats, handle);
        } catch (RuntimeException e) {
            // this arm failed and owns the outcome; still let the other one stop before we wind up
            stageRunner.awaitQuietly(codeAnalysis);
            throw e;
        }
        stageRunner.awaitStage(LocalReviewStages.CODE_ANALYSIS, codeAnalysis);

        runSynthesisStage(job, request, paths, stats, handle);
        validationRunner.run(job, request, paths, stats, handle);
    }

    private void runCodeAnalysisStage(ReviewJob job, GenerateLocalReviewRequest request,
                                      LocalReviewArtifactPaths paths, LocalReviewRunStats stats, RunHandle handle) {
        String stage = LocalReviewStages.CODE_ANALYSIS;
        if (stageRunner.skipIfReused(job, stage)) return;
        if (Boolean.TRUE.equals(request.skipCodeAnalysis)) {
            preparer.ensureCodeContextExists(request, job.serviceId, paths);
            stageRunner.skip(job.jobId, stage, "skipped by request");
            return;
        }
        String model = GeminiModels.orDefault(request.codeAnalysisModel, GeminiModels.CODE_ANALYSIS_DEFAULT);
        StageInput input = new StageInput(job.jobId, stage, true, stats, paths.runDir(), handle,
            List.of(LocalReviewOutputPaths.CODE_CONTEXT));
        stageRunner.runGemini(input, context -> {
            String prompt = LocalReviewPromptBuilder.buildCodeAnalysisPrompt(
                request.service, request.localAppRepoPath, request.appCodePaths, paths.codeContext());
            return geminiCliService.run(new GeminiRunRequest(prompt, model,
                List.of(request.localAppRepoPath, paths.runDir()), NO_MCP, CODE_ANALYSIS_TIMEOUT_MINUTES), context);
        });
    }

    private void runInfraContextStage(ReviewJob job, GenerateLocalReviewRequest request,
                                      LocalReviewArtifactPaths paths, LocalReviewRunStats stats, RunHandle handle) {
        String stage = LocalReviewStages.INFRA_CONTEXT;
        if (stageRunner.skipIfReused(job, stage)) return;
        StageInput input = new StageInput(job.jobId, stage, true, stats, paths.runDir(), handle,
            List.of(LocalReviewOutputPaths.INFRA_CONTEXT, LocalReviewOutputPaths.MYSQL_TABLES));
        stageRunner.runLocal(input, () -> {
            String infraSummary = infraContextService.build(new InfraContextParams(request.localInfraRepoPath, request.env,
                request.domain, request.service, request.mysqlHost, request.atlasCluster, paths.infraContext()));
            String mysqlSummary = mysqlTableStorageService.collect(
                new MySQLTableParams(request.mysqlHost, request.mysqlDB, paths.mysqlTables()));
            return infraSummary + " | " + mysqlSummary;
        });
    }

    /**
     * The Datadog figures are read here, over the REST API, and the Azure state by a model. The model
     * used to do both and its copy of the numbers was wrong often enough to fail validation (see
     * {@link DatadogMetricsCollector}); what remains for it is the part that needs judgement.
     */
    private void runMetricCollectionStage(ReviewJob job, GenerateLocalReviewRequest request,
                                          LocalReviewArtifactPaths paths, LocalReviewRunStats stats, RunHandle handle) {
        String stage = LocalReviewStages.METRIC_COLLECTION;
        if (stageRunner.skipIfReused(job, stage)) return;
        String model = GeminiModels.orDefault(request.metricCollectionModel, GeminiModels.METRIC_COLLECTION_DEFAULT);
        StageInput input = new StageInput(job.jobId, stage, true, stats, paths.runDir(), handle,
            List.of(LocalReviewOutputPaths.METRICS, LocalReviewOutputPaths.AZURE_LIVE));
        stageRunner.runGemini(input, context -> {
            Collected datadog = datadogMetricsCollector.collect(new Params(request.service, request.env.toLowerCase(Locale.US),
                request.mysqlHost, request.atlasCluster, request.atlasDatabase, request.kafkaConsumerGroups));
            String azurePath = paths.runDir() + "/" + LocalReviewOutputPaths.AZURE_LIVE;
            MetricCollectionParams params = new MetricCollectionParams();
            params.serviceId = request.service;
            params.env = request.env;
            params.namespace = request.namespace;
            params.mysqlHost = request.mysqlHost;
            params.azureResourceGroup = job.azureResourceGroup;
            params.azureSubscriptionId = firstSet(request.azureSubscriptionId, System.getenv("AZURE_SUBSCRIPTION_ID"));
            params.outputPath = azurePath;
            GeminiRunResult result = geminiCliService.run(new GeminiRunRequest(
                LocalReviewPromptBuilder.buildMetricCollectionPrompt(params), model,
                List.of(paths.runDir()), AZURE_MCP_ONLY, METRIC_COLLECTION_TIMEOUT_MINUTES), context);
            Files.writeString(Path.of(paths.metrics()), MetricsAssembler.assemble(request.service, request.env.toLowerCase(Locale.US),
                datadog, readIfPresent(azurePath), readIfPresent(paths.mysqlTables()), datadog.windowEnd()));
            return result;
        });
    }

    private String readIfPresent(String path) throws IOException {
        Path file = Path.of(path);
        return Files.exists(file) ? Files.readString(file) : null;
    }

    private void runSynthesisStage(ReviewJob job, GenerateLocalReviewRequest request,
                                   LocalReviewArtifactPaths paths, LocalReviewRunStats stats, RunHandle handle) {
        String stage = LocalReviewStages.SYNTHESIS;
        if (stageRunner.skipIfReused(job, stage)) return;
        String model = GeminiModels.orDefault(request.synthesisModel, GeminiModels.SYNTHESIS_DEFAULT);
        StageInput input = new StageInput(job.jobId, stage, true, stats, paths.runDir(), handle,
            List.of(fileName(paths.reviewReport()), LocalReviewOutputPaths.REPORT_SNAPSHOT));
        stageRunner.runGemini(input, context -> {
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
            params.atlasDatabase = request.atlasDatabase;
            params.hpaType = request.hpaType;
            params.kafkaConsumerGroups = request.kafkaConsumerGroups;
            params.outputPath = paths.reviewReport();
            params.snapshotPath = paths.reportSnapshot();
            return geminiCliService.run(new GeminiRunRequest(
                LocalReviewPromptBuilder.buildReviewPrompt(params), model,
                List.of(paths.runDir()), NO_MCP, SYNTHESIS_TIMEOUT_MINUTES), context);
        });
    }

    private String fileName(String path) {
        return String.valueOf(Path.of(path).getFileName());
    }

    private String firstSet(String... values) {
        for (String value : values) {
            if (!Strings.isBlank(value)) return value;
        }
        return null;
    }
}
