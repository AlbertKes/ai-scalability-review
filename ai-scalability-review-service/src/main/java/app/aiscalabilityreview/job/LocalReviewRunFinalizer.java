package app.aiscalabilityreview.job;

import app.aiscalabilityreview.domain.AppSettings;
import app.aiscalabilityreview.domain.ReviewJob;
import app.aiscalabilityreview.domain.ReviewJob.StageStatus;
import app.aiscalabilityreview.domain.ReviewReport;
import app.aiscalabilityreview.domain.ValidationResult;
import app.aiscalabilityreview.prompt.LocalReviewPromptBuilder;
import app.aiscalabilityreview.service.AppSettingsService;
import app.aiscalabilityreview.service.CostService;
import app.aiscalabilityreview.service.GeminiModels;
import app.aiscalabilityreview.service.NotificationService;
import app.aiscalabilityreview.service.NotificationService.NotificationRequest;
import app.aiscalabilityreview.service.ReviewService;
import app.aiscalabilityreview.service.ValidationReportParser;
import app.aiscalabilityreview.service.builder.LocalReviewReportBuilder;
import app.aiscalabilityreview.service.builder.LocalReviewReportBuilder.LocalReportParams;
import core.framework.inject.Inject;
import core.framework.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Everything that happens once the stages have stopped, whatever the outcome: the report is
 * persisted, the validation verdict is stored rather than logged away, the run's totals and cost
 * are rolled up onto the job so the list can sort by them, and the operator is told.
 */
public class LocalReviewRunFinalizer {
    private final Logger logger = LoggerFactory.getLogger(LocalReviewRunFinalizer.class);

    @Inject
    ReviewService reviewService;
    @Inject
    CostService costService;
    @Inject
    AppSettingsService appSettingsService;
    @Inject
    NotificationService notificationService;

    /** Builds and stores the report from the synthesis stage's output. */
    public ReviewReport publishReport(ReviewJob job, LocalReviewArtifactPaths paths, String model) {
        String reviewMarkdown = read(paths.reviewReport());
        ReviewReport report = LocalReviewReportBuilder.reviewReport(new LocalReportParams(job.jobId, job.serviceId,
            reviewMarkdown, paths.reviewReport(), read(paths.reportSnapshot()), model));
        reviewService.saveReport(report);
        return report;
    }

    /**
     * Rolls the stage numbers up onto the run and writes {@code run-stats.md} beside the artifacts.
     * Runs in a finally block, so it must tolerate a run that failed at any stage.
     */
    public void summarise(String jobId, LocalReviewArtifactPaths paths, LocalReviewRunStats stats) {
        ReviewJob loaded = reviewService.getReviewJob(jobId).orElse(null);
        if (loaded == null) return;
        AppSettings settings = appSettingsService.get();

        // Everything that reads a file or writes another collection happens before the lock, so the
        // job stays locked only for the field assignments.
        String overallScore = resolveScore(loaded);
        Validation validation = resolveValidation(loaded, paths);
        long wallClockMs = wallClockMs(loaded);
        reviewService.mutateJob(jobId, job -> {
            applyTotals(job, settings, wallClockMs);
            job.overallScore = overallScore;
            job.validationVerdict = validation.verdict();
            job.validationResultId = validation.resultId();
        });

        ReviewJob job = reviewService.getReviewJob(jobId).orElse(loaded);
        writeRunStats(job, paths, stats, settings, wallClockMs);
        if (job.request != null && !Strings.isBlank(job.request.outputBaseDir)) {
            LocalReviewOutputPaths.updateLatestLink(job.request.outputBaseDir, job.serviceId, paths.runDir());
        }
    }

    /** Raises the console notifications for a finished run, honouring the configured switches. */
    public void notifyOutcome(String jobId) {
        ReviewJob job = reviewService.getReviewJob(jobId).orElse(null);
        if (job == null) return;
        String label = job.serviceId + (Strings.isBlank(job.env) ? "" : " (" + job.env + ")");

        if (ReviewJob.STATUS_COMPLETED.equals(job.status)) {
            notificationService.raise(new NotificationRequest(jobId, job.serviceId,
                NotificationService.TYPE_RUN_COMPLETED, NotificationService.SEVERITY_SUCCESS,
                "Review completed — " + label, summaryLine(job)));
        } else if (ReviewJob.STATUS_CANCELLED.equals(job.status)) {
            notificationService.raise(new NotificationRequest(jobId, job.serviceId,
                NotificationService.TYPE_RUN_CANCELLED, NotificationService.SEVERITY_WARNING,
                "Review cancelled — " + label, "Cancelled at " + displayStage(job.currentStage)));
        } else if (ReviewJob.STATUS_FAILED.equals(job.status)) {
            notificationService.raise(new NotificationRequest(jobId, job.serviceId,
                NotificationService.TYPE_RUN_FAILED, NotificationService.SEVERITY_ERROR,
                "Review failed — " + label,
                displayStage(job.currentStage) + ": " + orUnknown(job.errorMessage)));
        }
        raiseQualitySignals(job, label);
    }

    private void raiseQualitySignals(ReviewJob job, String label) {
        if (Boolean.FALSE.equals(job.withinBudget)) {
            notificationService.raise(new NotificationRequest(job.jobId, job.serviceId,
                NotificationService.TYPE_OVER_BUDGET, NotificationService.SEVERITY_WARNING,
                "Over budget — " + label, summaryLine(job)));
        }
        if ("FAIL".equals(job.validationVerdict)) {
            notificationService.raise(new NotificationRequest(job.jobId, job.serviceId,
                NotificationService.TYPE_VALIDATION_FAILED, NotificationService.SEVERITY_WARNING,
                "Validation failed — " + label, "The report did not pass its own checks."));
        }
        if ("RED".equals(job.overallScore)) {
            notificationService.raise(new NotificationRequest(job.jobId, job.serviceId,
                NotificationService.TYPE_RED_SCORE, NotificationService.SEVERITY_WARNING,
                "Overall score RED — " + label, "The review scored this service RED overall."));
        }
        failedStages(job).forEach(stage -> notificationService.raise(new NotificationRequest(job.jobId, job.serviceId,
            NotificationService.TYPE_STAGE_FAILED, NotificationService.SEVERITY_WARNING,
            "Stage failed — " + label, LocalReviewStages.displayName(stage) + " did not complete.")));
    }

    /** Stages that failed without stopping the run — validation is the only one that can. */
    private List<String> failedStages(ReviewJob job) {
        List<String> failed = new ArrayList<>();
        if (job.stageStatuses == null || !ReviewJob.STATUS_COMPLETED.equals(job.status)) return failed;
        for (Map.Entry<String, StageStatus> entry : job.stageStatuses.entrySet()) {
            if (entry.getValue() != null && "FAILED".equals(entry.getValue().status)) failed.add(entry.getKey());
        }
        return failed;
    }

    /**
     * How long the run actually took: the first stage to start until the last to finish. Not the sum
     * of the stage durations, which double-counts the overlap now that code analysis runs beside the
     * infra and metric stages, and not {@code startedAt} to {@code completedAt}, which would fold in
     * any time the run spent queued behind another.
     */
    private long wallClockMs(ReviewJob job) {
        if (job.stageStatuses == null || job.stageStatuses.isEmpty()) return 0;
        ZonedDateTime first = null;
        ZonedDateTime last = null;
        for (StageStatus stage : job.stageStatuses.values()) {
            if (stage == null) continue;
            if (stage.startedAt != null && (first == null || stage.startedAt.isBefore(first))) first = stage.startedAt;
            if (stage.completedAt != null && (last == null || stage.completedAt.isAfter(last))) last = stage.completedAt;
        }
        if (first == null || last == null) return 0;
        return Math.max(0, Duration.between(first, last).toMillis());
    }

    private void applyTotals(ReviewJob job, AppSettings settings, long wallClockMs) {
        long input = 0;
        long output = 0;
        long cached = 0;
        int mcpCalls = 0;
        List<Double> costs = new ArrayList<>();
        if (job.stageStatuses != null) {
            for (StageStatus stage : job.stageStatuses.values()) {
                if (stage == null) continue;
                input += orZero(stage.inputTokens);
                output += orZero(stage.outputTokens);
                cached += orZero(stage.cachedTokens);
                mcpCalls += orZero(stage.mcpToolCalls);
                costs.add(stage.estimatedCostUsd);
            }
        }
        job.totalInputTokens = input;
        job.totalOutputTokens = output;
        job.totalCachedTokens = cached;
        job.totalTokens = input + output;
        job.totalDurationMs = wallClockMs;
        job.totalMcpToolCalls = mcpCalls;
        job.estimatedCostUsd = costService.sum(costs);
        long tokenBudget = settings.tokenBudget == null ? LocalReviewRunStats.TOKEN_BUDGET : settings.tokenBudget;
        long durationBudget = settings.durationBudgetMs == null
            ? LocalReviewRunStats.DURATION_BUDGET_MS : settings.durationBudgetMs;
        boolean within = input + output <= tokenBudget && wallClockMs <= durationBudget;
        job.withinBudget = within;
    }

    /**
     * The report's overall score, for the run list to filter on. An absent report leaves it null —
     * UNKNOWN is a score the report itself states, not one invented here.
     */
    private String resolveScore(ReviewJob job) {
        if (job.reportId == null) return job.overallScore;
        return reviewService.getReviewReport(job.reportId).map(report -> report.overallScore).orElse(job.overallScore);
    }

    /**
     * Stores the validation result and returns what belongs on the job. A run that skipped stage 5
     * keeps a null verdict rather than an implied pass.
     */
    private Validation resolveValidation(ReviewJob job, LocalReviewArtifactPaths paths) {
        if (!Files.isRegularFile(Path.of(paths.validationReport()))) {
            return new Validation(job.validationVerdict, job.validationResultId);
        }
        String markdown = read(paths.validationReport());
        String verdict = ValidationReportParser.verdict(markdown);
        if (job.reportId == null) return new Validation(verdict, null);
        try {
            String validationId = reviewService.createValidationResult(job.reportId,
                GeminiModels.toAIModel(validationModel(job)), job.triggeredBy);
            ValidationResult result = reviewService.getValidationResult(validationId).orElse(null);
            if (result == null) return new Validation(verdict, null);
            result.status = ReviewJob.STATUS_COMPLETED;
            result.verdict = verdict;
            result.checkResults = ValidationReportParser.checks(markdown);
            result.validationReportMarkdown = markdown;
            reviewService.updateValidationResult(result);
            return new Validation(verdict, validationId);
        } catch (IllegalArgumentException e) {
            logger.warn("Could not store the validation result for job {}: {}", job.jobId, e.getMessage());
            return new Validation(verdict, null);
        }
    }

    private String validationModel(ReviewJob job) {
        if (job.request == null) return GeminiModels.VALIDATION_DEFAULT;
        return GeminiModels.orDefault(job.request.validationModel, GeminiModels.VALIDATION_DEFAULT);
    }

    private void writeRunStats(ReviewJob job, LocalReviewArtifactPaths paths, LocalReviewRunStats stats,
                               AppSettings settings, long wallClockMs) {
        long tokenBudget = settings.tokenBudget == null ? LocalReviewRunStats.TOKEN_BUDGET : settings.tokenBudget;
        long durationBudget = settings.durationBudgetMs == null
            ? LocalReviewRunStats.DURATION_BUDGET_MS : settings.durationBudgetMs;
        if (!stats.withinBudget(wallClockMs, tokenBudget, durationBudget)) {
            logger.warn("Local review job {} exceeded the cost/latency budget: {}", job.jobId,
                stats.summaryLine(wallClockMs, tokenBudget, durationBudget));
        }
        Path statsPath = Path.of(LocalReviewPromptBuilder.runStatsPath(paths.runDir()));
        try {
            Files.writeString(statsPath, stats.render(job.serviceId, job.jobId, wallClockMs, tokenBudget, durationBudget));
        } catch (IOException e) {
            logger.warn("Could not write run stats to {}: {}", statsPath, e.getMessage());
        }
    }

    private String summaryLine(ReviewJob job) {
        return orZero(job.totalTokens) + " tokens, " + orZero(job.totalDurationMs) / 1000 + "s, overall "
            + orUnknown(job.overallScore) + ", validation " + orUnknown(job.validationVerdict);
    }

    private String displayStage(String stage) {
        return stage == null ? "the run" : LocalReviewStages.displayName(stage);
    }

    private String orUnknown(String value) {
        return Strings.isBlank(value) ? "unknown" : value;
    }

    private long orZero(Long value) {
        return value == null ? 0 : value;
    }

    private int orZero(Integer value) {
        return value == null ? 0 : value;
    }

    private String read(String path) {
        try {
            return Files.readString(Path.of(path).toAbsolutePath());
        } catch (IOException e) {
            logger.warn("Could not read file {}: {}", path, e.getMessage());
            return "";
        }
    }

    /** What {@link #resolveValidation} found, to be assigned once the job is locked. */
    private record Validation(String verdict, String resultId) {
    }
}
