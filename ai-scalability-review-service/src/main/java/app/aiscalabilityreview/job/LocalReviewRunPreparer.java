package app.aiscalabilityreview.job;

import app.aiscalabilityreview.api.localreview.GenerateLocalReviewRequest;
import app.aiscalabilityreview.domain.ReviewJob;
import app.aiscalabilityreview.domain.ReviewJob.StageStatus;
import app.aiscalabilityreview.prompt.LocalReviewPromptBuilder;
import app.aiscalabilityreview.service.AppSettingsService;
import app.aiscalabilityreview.service.GeminiCliService;
import app.aiscalabilityreview.service.GitRepoInspector;
import app.aiscalabilityreview.service.InfraContextService;
import app.aiscalabilityreview.service.ReviewService;
import core.framework.inject.Inject;
import core.framework.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.format.DateTimeFormatter;

/**
 * Everything a run needs in place before its first stage: its own output directory, the
 * reproducibility fingerprint, and the artifacts a retry is reusing from the run it repeats.
 *
 * <p>Separated from {@link LocalReviewJobExecutor} so that class reads as the stage graph it
 * orchestrates rather than as setup followed by stages.
 */
public class LocalReviewRunPreparer {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String CODE_CONTEXT_PLACEHOLDER =
        "Code context: not available — the code analysis stage was skipped for this run.\n";

    private final Logger logger = LoggerFactory.getLogger(LocalReviewRunPreparer.class);

    @Inject
    ReviewService reviewService;
    @Inject
    GeminiCliService geminiCliService;
    @Inject
    InfraContextService infraContextService;
    @Inject
    AppSettingsService appSettingsService;
    @Inject
    GitRepoInspector gitRepoInspector;

    /**
     * Creates this run's own directory, records the reproducibility fingerprint and carries over the
     * artifacts of any stages a retry is reusing.
     *
     * <p>Runs after the slot is acquired and before any stage starts, so it is the one place that
     * can write the job document without contending with a stage.
     */
    public LocalReviewArtifactPaths prepare(ReviewJob job, GenerateLocalReviewRequest request) throws IOException {
        String base = baseDir(request);
        request.outputBaseDir = base;
        String runDir = Path.of(LocalReviewOutputPaths.runDir(base, job.serviceId, job.startedAt, job.jobId))
            .toAbsolutePath().toString();
        Files.createDirectories(Path.of(runDir));
        String today = DATE_FORMAT.format(job.startedAt);
        LocalReviewArtifactPaths paths = new LocalReviewArtifactPaths(runDir,
            LocalReviewPromptBuilder.codeContextPath(runDir),
            LocalReviewPromptBuilder.infraContextPath(runDir),
            LocalReviewPromptBuilder.metricsPath(runDir),
            LocalReviewPromptBuilder.mysqlTablesPath(runDir),
            LocalReviewPromptBuilder.reviewReportPath(runDir, today),
            LocalReviewPromptBuilder.validationReportPath(runDir, today),
            LocalReviewPromptBuilder.reportSnapshotPath(runDir));

        job.outputDir = runDir;
        job.request.outputBaseDir = base;
        job.cliVersion = geminiCliService.version();
        job.promptVersion = LocalReviewJobExecutor.PROMPT_VERSION;
        job.repoShaApp = gitRepoInspector.sha(request.localAppRepoPath);
        job.repoShaInfra = gitRepoInspector.sha(request.localInfraRepoPath);
        job.repoDirtyApp = gitRepoInspector.isDirty(request.localAppRepoPath);
        job.repoDirtyInfra = gitRepoInspector.isDirty(request.localInfraRepoPath);
        job.azureResourceGroup = azureResourceGroup(request);
        reviewService.updateJob(job);

        copyReusedArtifacts(job, runDir);
        logger.info("Local review job {} writing to {}", job.jobId, runDir);
        return paths;
    }

    public String baseDir(GenerateLocalReviewRequest request) {
        if (!Strings.isBlank(request.outputBaseDir)) return request.outputBaseDir;
        String configured = appSettingsService.get().outputBaseDir;
        return Strings.isBlank(configured) ? "reports" : configured;
    }

    /**
     * The synthesis prompt attaches the code context with an @file reference, so a skipped stage must
     * still leave a readable file behind. With one directory per run there is no fixed path to reuse,
     * so the newest previous run of this service supplies it; failing that, a placeholder says the
     * context is genuinely unavailable rather than silently empty.
     */
    public void ensureCodeContextExists(GenerateLocalReviewRequest request, String serviceId,
                                        LocalReviewArtifactPaths paths) {
        Path target = Path.of(paths.codeContext());
        if (Files.exists(target)) return;
        String previous = LocalReviewOutputPaths.latestCodeContext(baseDir(request), serviceId, paths.runDir());
        try {
            if (previous != null) {
                Files.copy(Path.of(previous), target, StandardCopyOption.REPLACE_EXISTING);
                logger.info("Reusing the code context from {}", previous);
            } else {
                Files.writeString(target, CODE_CONTEXT_PLACEHOLDER);
                logger.warn("No previous code context for {}; wrote a placeholder because code analysis was skipped", serviceId);
            }
        } catch (IOException e) {
            logger.warn("Could not prepare the code context at {}: {}", target, e.getMessage());
        }
    }

    /**
     * A retry runs in its own directory, so the stages it is not re-running must have their files
     * brought across — otherwise the run would be missing the context those stages produced.
     */
    private void copyReusedArtifacts(ReviewJob job, String runDir) {
        if (Strings.isBlank(job.retryOfJobId)) return;
        ReviewJob source = reviewService.getReviewJob(job.retryOfJobId).orElse(null);
        if (source == null || Strings.isBlank(source.outputDir) || source.stageStatuses == null) return;
        int from = LocalReviewStages.indexOf(job.retryFromStage);
        if (from < 0) return;
        for (String stage : LocalReviewStages.ORDER.subList(0, from)) {
            StageStatus status = source.stageStatuses.get(stage);
            if (status == null || status.artifacts == null) continue;
            status.artifacts.forEach(artifact -> copyQuietly(
                Path.of(source.outputDir, artifact), Path.of(runDir, artifact)));
        }
    }

    private void copyQuietly(Path from, Path to) {
        try {
            if (Files.isRegularFile(from)) Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            logger.warn("Could not carry {} over to the retry run: {}", from, e.getMessage());
        }
    }

    /**
     * The request wins, so a service whose resources sit outside the environment's own group can say
     * so; otherwise the group is read from the infra repository. Null is passed through to the prompt
     * as "unknown", which asks the model to discover the group — guessing it from {@code env} is what
     * produced a report full of `Resource group 'uat' could not be found`.
     */
    private String azureResourceGroup(GenerateLocalReviewRequest request) {
        String configured = firstSet(request.azureResourceGroup, System.getenv("AZURE_RESOURCE_GROUP"));
        if (configured != null) return configured;
        String derived = infraContextService.azureResourceGroup(request.localInfraRepoPath, request.env);
        if (derived == null) {
            logger.warn("Could not resolve the Azure resource group for env {}; the metric stage will discover it",
                request.env);
        } else {
            logger.info("Resolved the Azure resource group for env {} to {}", request.env, derived);
        }
        return derived;
    }

    private String firstSet(String... values) {
        for (String value : values) {
            if (!Strings.isBlank(value)) return value;
        }
        return null;
    }
}
