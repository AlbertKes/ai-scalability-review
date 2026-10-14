package app.aiscalabilityreview.job;

import app.aiscalabilityreview.api.localreview.GenerateLocalReviewRequest;
import app.aiscalabilityreview.domain.ReviewJob;
import app.aiscalabilityreview.job.LocalReviewRunRegistry.RunHandle;
import app.aiscalabilityreview.job.LocalReviewStageRunner.StageInput;
import app.aiscalabilityreview.prompt.LocalReviewPromptBuilder;
import app.aiscalabilityreview.prompt.LocalReviewPromptBuilder.ValidationPromptParams;
import app.aiscalabilityreview.service.GeminiCliService;
import app.aiscalabilityreview.service.GeminiCliService.GeminiRunRequest;
import app.aiscalabilityreview.service.GeminiModels;
import app.aiscalabilityreview.service.ValidationReportAssembler;
import app.aiscalabilityreview.service.ValidationReportAssembler.AssemblyParams;
import core.framework.async.Executor;
import core.framework.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Future;

/**
 * Stage 5, which is three Gemini sessions and a merge rather than the one session it used to be.
 *
 * <p>Validation held all eight checks in a single agentic loop. In such a loop the whole context is
 * re-sent on every tool round-trip, and the eight checks have very different appetites: three
 * re-query Datadog and Azure, one reads the application source file by file, and four need nothing
 * but the report's own markdown. Holding them together meant every source file the recommendation
 * check opened was re-sent on each later round-trip, including the ones that were only re-counting
 * table rows. Measured on wonder-cart-service the stage spent 6.0M input tokens over 61 tool calls
 * in 547 s — 64 % of the run's tokens and 51 % of its wall clock.
 *
 * <p>Each group now carries only the context its own checks need: the static group starts no MCP
 * server and is given no repository, and the data group is given neither the scoring thresholds nor
 * the report format spec. Because the three are independent they run side by side rather than one
 * after another, so the group's wall clock is the slowest of them rather than their sum. The fourth
 * stage merges their partials into the one report a reader opens.
 */
public class LocalReviewValidationRunner {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final List<String> DATA_MCP = List.of("datadog", "azure");
    private static final List<String> NO_MCP = List.of(GeminiCliService.MCP_DISABLED);

    /** The static group makes no tool call, so it has no loop to run long; the other two do. */
    private static final int STATIC_TIMEOUT_MINUTES = 10;
    private static final int DATA_TIMEOUT_MINUTES = 20;
    private static final int ADVICE_TIMEOUT_MINUTES = 20;

    private final Logger logger = LoggerFactory.getLogger(LocalReviewValidationRunner.class);

    @Inject
    GeminiCliService geminiCliService;
    @Inject
    LocalReviewStageRunner stageRunner;
    @Inject
    ValidationReportAssembler assembler;
    @Inject
    Executor executor;

    public void run(ReviewJob job, GenerateLocalReviewRequest request,
                    LocalReviewArtifactPaths paths, LocalReviewRunStats stats, RunHandle handle) {
        if (Boolean.TRUE.equals(request.skipValidation)) {
            skipAll(job);
            return;
        }
        String model = GeminiModels.orDefault(request.validationModel, GeminiModels.VALIDATION_DEFAULT);
        Context context = new Context(job, request, promptParams(request, paths), model, paths, stats, handle);
        logger.info("Validating job {} in {} parallel check groups on {}", job.jobId,
            LocalReviewStages.VALIDATION_GROUPS.size(), model);

        List<Arm> arms = List.of(
            arm(LocalReviewStages.VALIDATION_STATIC, job, () -> runStatic(context)),
            arm(LocalReviewStages.VALIDATION_DATA, job, () -> runData(context)),
            arm(LocalReviewStages.VALIDATION_ADVICE, job, () -> runAdvice(context)));
        awaitAll(arms);
        assemble(context);
    }

    /** {@code skip_validation} drops the whole group, the merge included. */
    private void skipAll(ReviewJob job) {
        int synthesis = LocalReviewStages.indexOf(LocalReviewStages.SYNTHESIS);
        LocalReviewStages.ORDER.stream()
            .filter(stage -> LocalReviewStages.indexOf(stage) > synthesis)
            .forEach(stage -> stageRunner.skip(job.jobId, stage, "skipped by request"));
    }

    /**
     * Starts one group unless a retry is reusing it, in which case the skip is recorded on this
     * thread and nothing is submitted.
     */
    private Arm arm(String stage, ReviewJob job, Runnable action) {
        if (stageRunner.skipIfReused(job, stage)) return new Arm(stage, null);
        return new Arm(stage, executor.submit(stage.toLowerCase(Locale.ROOT) + "-" + job.jobId, action::run));
    }

    /**
     * Waits for every group before merging, and reports the first one that stopped the run.
     *
     * <p>None of the three is fatal, so a group that merely failed leaves its checks out of the
     * merged report and the run carries on — which is what the assembler reports as a check that did
     * not run. A cancel is the one thing that throws here, and even then every arm is waited for
     * first: winding a run up while a CLI is still writing into its directory is worse than a few
     * seconds of waiting.
     */
    private void awaitAll(List<Arm> arms) {
        RuntimeException failure = null;
        for (Arm arm : arms) {
            if (arm.future() == null) continue;
            try {
                stageRunner.awaitStage(arm.stage(), arm.future());
            } catch (RuntimeException e) {
                if (failure == null) failure = e;
            }
        }
        if (failure != null) throw failure;
    }

    /** Checks 3, 4, 5, 7 — no MCP server, and the run directory is the only path it can read. */
    private void runStatic(Context c) {
        StageInput input = stageInput(c, LocalReviewStages.VALIDATION_STATIC, LocalReviewOutputPaths.VALIDATION_STATIC);
        stageRunner.runGemini(input, context -> geminiCliService.run(new GeminiRunRequest(
            LocalReviewPromptBuilder.buildStaticValidationPrompt(c.params()), c.model(),
            List.of(c.paths().runDir()), NO_MCP, STATIC_TIMEOUT_MINUTES), context));
    }

    /** Checks 1, 2, 6 — the only group that starts an MCP server, and it reads the infra repo only. */
    private void runData(Context c) {
        StageInput input = stageInput(c, LocalReviewStages.VALIDATION_DATA, LocalReviewOutputPaths.VALIDATION_DATA);
        stageRunner.runGemini(input, context -> geminiCliService.run(new GeminiRunRequest(
            LocalReviewPromptBuilder.buildDataValidationPrompt(c.params()), c.model(),
            List.of(c.paths().runDir(), c.request().localInfraRepoPath), DATA_MCP, DATA_TIMEOUT_MINUTES), context));
    }

    /** Check 8 — the application repository, and no MCP server to re-query anything with. */
    private void runAdvice(Context c) {
        StageInput input = stageInput(c, LocalReviewStages.VALIDATION_ADVICE, LocalReviewOutputPaths.VALIDATION_ADVICE);
        stageRunner.runGemini(input, context -> geminiCliService.run(new GeminiRunRequest(
            LocalReviewPromptBuilder.buildAdviceValidationPrompt(c.params()), c.model(),
            List.of(c.paths().runDir(), c.request().localAppRepoPath, c.request().localInfraRepoPath),
            NO_MCP, ADVICE_TIMEOUT_MINUTES), context));
    }

    /** Merges whatever the three groups produced. Runs in this service and spends no tokens. */
    private void assemble(Context c) {
        String stage = LocalReviewStages.VALIDATION_REPORT;
        if (stageRunner.skipIfReused(c.job(), stage)) return;
        String reportName = String.valueOf(Path.of(c.paths().validationReport()).getFileName());
        StageInput input = new StageInput(c.job().jobId, stage, false, c.stats(), c.paths().runDir(),
            c.handle(), List.of(reportName));
        stageRunner.runLocal(input, () -> {
            AssemblyParams params = new AssemblyParams(c.job().serviceId, c.job().env, c.paths().reviewReport(),
                DATE_FORMAT.format(c.job().startedAt), c.model(), c.paths().validationReport());
            assembler.assemble(params, List.of(c.paths().validationStatic(), c.paths().validationData(),
                c.paths().validationAdvice()));
            return "merged the static, data and advice check groups into " + reportName;
        });
    }

    private StageInput stageInput(Context c, String stage, String artifact) {
        return new StageInput(c.job().jobId, stage, false, c.stats(), c.paths().runDir(), c.handle(), List.of(artifact));
    }

    private ValidationPromptParams promptParams(GenerateLocalReviewRequest request, LocalReviewArtifactPaths paths) {
        ValidationPromptParams params = new ValidationPromptParams();
        params.serviceId = request.service;
        params.reviewReportPath = paths.reviewReport();
        params.infraContextPath = paths.infraContext();
        params.localInfraRepoPath = request.localInfraRepoPath;
        params.localAppRepoPath = request.localAppRepoPath;
        params.env = request.env;
        params.namespace = request.namespace;
        params.mysqlHost = request.mysqlHost;
        params.mysqlDb = request.mysqlDB;
        params.atlasCluster = request.atlasCluster;
        params.kafkaConsumerGroups = request.kafkaConsumerGroups;
        params.mysqlTablesPath = paths.mysqlTables();
        params.appCodePaths = request.appCodePaths;
        params.staticOutputPath = paths.validationStatic();
        params.dataOutputPath = paths.validationData();
        params.adviceOutputPath = paths.validationAdvice();
        return params;
    }

    /** Everything the four stages share, so none of them has to take it a parameter at a time. */
    private record Context(ReviewJob job,
                           GenerateLocalReviewRequest request,
                           ValidationPromptParams params,
                           String model,
                           LocalReviewArtifactPaths paths,
                           LocalReviewRunStats stats,
                           RunHandle handle) {
    }

    /** One check group in flight; a null future means a retry is reusing that group's output. */
    private record Arm(String stage, Future<Void> future) {
    }
}
