package app.aiscalabilityreview.job;

import app.aiscalabilityreview.domain.ReviewJob;
import app.aiscalabilityreview.domain.ReviewJob.StageStatus;
import app.aiscalabilityreview.exception.FatalStageException;
import app.aiscalabilityreview.job.LocalReviewRunRegistry.RunHandle;
import app.aiscalabilityreview.service.CostService;
import app.aiscalabilityreview.service.GeminiCliService.GeminiRunContext;
import app.aiscalabilityreview.service.GeminiCliService.GeminiRunResult;
import app.aiscalabilityreview.service.ReviewService;
import core.framework.inject.Inject;
import core.framework.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Bookkeeping around one stage: status transitions, timing, token and cost accounting, the artifacts
 * it left behind and the log file the console tails.
 *
 * <p>Separated from {@link LocalReviewJobExecutor} so the executor reads as the stages it runs
 * rather than as the same twenty lines of instrumentation once per stage.
 */
public class LocalReviewStageRunner {
    private final Logger logger = LoggerFactory.getLogger(LocalReviewStageRunner.class);

    @Inject
    ReviewService reviewService;
    @Inject
    CostService costService;

    /** Runs a stage that calls the Gemini CLI, recording everything the CLI reported about it. */
    public void runGemini(StageInput input, GeminiStageAction action) {
        StageStatus stageStatus = start(input.jobId(), input.stageName());
        long startMs = System.currentTimeMillis();
        String logFile = LocalReviewOutputPaths.stageLogName(input.stageName());
        stageStatus.logFile = logFile;
        try {
            GeminiRunContext context = new GeminiRunContext(
                Path.of(input.runDir(), logFile).toAbsolutePath().toString(),
                process -> input.handle().attach(process));
            GeminiRunResult result = action.execute(context);
            input.stats().record(input.stageName(), result);
            applyResult(stageStatus, result);
            stageStatus.artifacts = existing(input.runDir(), input.artifacts());
            complete(input.jobId(), input.stageName(), stageStatus, startMs);
        } catch (Exception e) {
            fail(input, stageStatus, startMs, e);
        }
    }

    /** Runs a stage that executes inside this service and spends no tokens. */
    public void runLocal(StageInput input, LocalStageAction action) {
        StageStatus stageStatus = start(input.jobId(), input.stageName());
        long startMs = System.currentTimeMillis();
        try {
            stageStatus.outputSummary = action.execute();
            input.stats().recordLocal(input.stageName(), System.currentTimeMillis() - startMs);
            stageStatus.model = "none (local)";
            stageStatus.artifacts = existing(input.runDir(), input.artifacts());
            complete(input.jobId(), input.stageName(), stageStatus, startMs);
        } catch (Exception e) {
            fail(input, stageStatus, startMs, e);
        }
    }

    /**
     * Waits for a stage handed to another thread and makes its failure this thread's failure, so a
     * parallel arm reports exactly as a sequential one would.
     */
    public void awaitStage(String stageName, Future<Void> future) {
        Throwable failure = failureOf(future);
        if (failure == null) return;
        if (failure instanceof FatalStageException fatal) throw fatal;
        throw new FatalStageException(stageName, failure);
    }

    /** @return what the stage failed with, unwrapped from its {@link Future}, or null if it passed */
    private Throwable failureOf(Future<Void> future) {
        try {
            future.get();
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return e;
        } catch (ExecutionException e) {
            return e.getCause() == null ? e : e.getCause();
        }
    }

    /**
     * Waits for a stage whose outcome no longer matters because another arm has already failed.
     * The point is not to report it but to let its subprocess finish before the run is wound up —
     * a run that reports failure while a CLI is still writing into its directory is worse than a
     * few seconds of waiting.
     */
    public void awaitQuietly(Future<Void> future) {
        try {
            future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            logger.info("A parallel stage also failed while the run was already failing: {}", e.getMessage());
        }
    }

    /**
     * Skips a stage a retry is reusing instead of re-running.
     *
     * @return true when the stage was skipped, so the caller returns without starting it
     */
    public boolean skipIfReused(ReviewJob job, String stage) {
        if (Strings.isBlank(job.retryFromStage)) return false;
        if (LocalReviewStages.indexOf(stage) >= LocalReviewStages.indexOf(job.retryFromStage)) return false;
        skip(job.jobId, stage, "reused from run " + job.retryOfJobId);
        return true;
    }

    public void skip(String jobId, String stageName, String reason) {
        logger.info("Skipping {} for job {}: {}", stageName, jobId, reason);
        StageStatus skipped = new StageStatus();
        skipped.status = "SKIPPED";
        skipped.startedAt = ZonedDateTime.now();
        skipped.completedAt = skipped.startedAt;
        skipped.durationMs = 0L;
        skipped.outputSummary = reason;
        reviewService.updateJobStageStatus(jobId, stageName, skipped);
    }

    private void applyResult(StageStatus stageStatus, GeminiRunResult result) {
        stageStatus.model = result.model();
        stageStatus.inputTokens = result.inputTokens();
        stageStatus.outputTokens = result.outputTokens();
        stageStatus.cachedTokens = result.cachedTokens();
        stageStatus.totalTokens = result.totalTokens();
        stageStatus.apiRequests = result.apiRequests();
        stageStatus.toolCalls = result.toolCalls();
        stageStatus.mcpToolCalls = result.mcpToolCalls();
        stageStatus.sessionId = result.sessionId();
        stageStatus.rawStatsJson = result.rawStatsJson();
        stageStatus.estimatedCostUsd = costService.estimate(result.model(),
            result.inputTokens(), result.outputTokens(), result.cachedTokens());
    }

    private StageStatus start(String jobId, String stageName) {
        reviewService.updateJobStatus(jobId, ReviewJob.STATUS_RUNNING, stageName);
        StageStatus stageStatus = new StageStatus();
        stageStatus.status = ReviewJob.STATUS_RUNNING;
        stageStatus.startedAt = ZonedDateTime.now();
        reviewService.updateJobStageStatus(jobId, stageName, stageStatus);
        return stageStatus;
    }

    private void complete(String jobId, String stageName, StageStatus stageStatus, long startMs) {
        stageStatus.status = "COMPLETED";
        stageStatus.completedAt = ZonedDateTime.now();
        stageStatus.durationMs = System.currentTimeMillis() - startMs;
        reviewService.updateJobStageStatus(jobId, stageName, stageStatus);
        logger.info("Stage {} completed in {}ms for job {} (model={}, inputTokens={}, outputTokens={}, mcpToolCalls={})",
            stageName, stageStatus.durationMs, jobId, stageStatus.model, stageStatus.inputTokens,
            stageStatus.outputTokens, stageStatus.mcpToolCalls);
    }

    /**
     * A stage that ended because the run was cancelled is recorded as CANCELLED rather than FAILED,
     * and always stops the run — a cancelled run must not carry on into the next stage.
     */
    private void fail(StageInput input, StageStatus stageStatus, long startMs, Exception cause) {
        boolean cancelled = input.handle().isCancelled();
        stageStatus.status = cancelled ? "CANCELLED" : "FAILED";
        stageStatus.completedAt = ZonedDateTime.now();
        stageStatus.durationMs = System.currentTimeMillis() - startMs;
        stageStatus.errorMessage = cancelled ? "cancelled by the operator" : cause.getMessage();
        stageStatus.artifacts = existing(input.runDir(), input.artifacts());
        reviewService.updateJobStageStatus(input.jobId(), input.stageName(), stageStatus);
        logger.error("Stage {} {} after {}ms for job {}: {}", input.stageName(),
            cancelled ? "cancelled" : "failed", stageStatus.durationMs, input.jobId(), cause.getMessage());
        if (cancelled || input.fatal()) throw new FatalStageException(input.stageName(), cause);
    }

    /** Only the artifacts that are actually on disk, so a partial stage does not advertise a missing file. */
    private List<String> existing(String runDir, List<String> artifacts) {
        List<String> present = new ArrayList<>(artifacts.size());
        for (String artifact : artifacts) {
            if (Files.isRegularFile(Path.of(runDir, artifact))) present.add(artifact);
        }
        return present;
    }

    /**
     * @param fatal     whether a failure stops the run; validation is the one stage that does not
     * @param artifacts file names this stage is expected to write, relative to the run directory
     */
    public record StageInput(String jobId,
                             String stageName,
                             boolean fatal,
                             LocalReviewRunStats stats,
                             String runDir,
                             RunHandle handle,
                             List<String> artifacts) {
    }

    @FunctionalInterface
    public interface GeminiStageAction {
        GeminiRunResult execute(GeminiRunContext context) throws Exception;
    }

    @FunctionalInterface
    public interface LocalStageAction {
        String execute() throws Exception;
    }
}
