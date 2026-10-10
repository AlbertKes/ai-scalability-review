package app.aiscalabilityreview.service;

import app.aiscalabilityreview.api.localreview.GenerateLocalReviewRequest;
import app.aiscalabilityreview.api.localreview.GenerateLocalReviewResponse;
import app.aiscalabilityreview.api.localreview.LocalReviewJobActionResponse;
import app.aiscalabilityreview.api.localreview.LocalReviewRequestView;
import app.aiscalabilityreview.api.localreview.PreflightRequest;
import app.aiscalabilityreview.api.localreview.PreflightResponse;
import app.aiscalabilityreview.api.localreview.RetryLocalReviewRequest;
import app.aiscalabilityreview.api.localreview.UpdateLocalReviewJobRequest;
import app.aiscalabilityreview.api.localreview.ValidateLocalReviewResponse;
import app.aiscalabilityreview.api.localreview.ValidateLocalReviewResponse.IssueView;
import app.aiscalabilityreview.domain.ReviewJob;
import app.aiscalabilityreview.job.LocalReviewJobExecutor;
import app.aiscalabilityreview.job.LocalReviewOutputPaths;
import app.aiscalabilityreview.job.LocalReviewRunRegistry;
import app.aiscalabilityreview.job.LocalReviewStages;
import app.aiscalabilityreview.service.LocalReviewValidator.Issue;
import app.aiscalabilityreview.service.ReviewService.LocalReviewJobParams;
import app.aiscalabilityreview.service.builder.LocalReviewRequestMapper;
import core.framework.async.Executor;
import core.framework.inject.Inject;
import core.framework.util.Strings;
import core.framework.web.exception.BadRequestException;
import core.framework.web.exception.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;

/**
 * Entry point for everything the console does to a local review run.
 *
 * <p>Submitting is deliberately two-phase: the same rule set that backs
 * {@code POST /local-review/validate} runs again here, before any job document exists, and a request
 * carrying a blocking error is rejected with the complete list rather than with the first problem
 * found. Nothing is created on a failed validation — no job, no directory, no notification.
 */
public class LocalReviewService {
    private final Logger logger = LoggerFactory.getLogger(LocalReviewService.class);

    @Inject
    ReviewService reviewService;
    @Inject
    LocalReviewJobExecutor localReviewJobExecutor;
    @Inject
    LocalReviewValidator validator;
    @Inject
    LocalReviewRunRegistry runRegistry;
    @Inject
    PreflightService preflightService;
    @Inject
    ArtifactService artifactService;
    @Inject
    AppSettingsService appSettingsService;
    @Inject
    NotificationService notificationService;
    @Inject
    Executor executor;

    public ValidateLocalReviewResponse validate(LocalReviewRequestView request) {
        List<Issue> issues = validator.validate(LocalReviewRequestMapper.toRequest(request));
        ValidateLocalReviewResponse response = new ValidateLocalReviewResponse();
        response.errors = validator.errors(issues).stream().map(this::toView).toList();
        response.warnings = validator.warnings(issues).stream().map(this::toView).toList();
        response.valid = response.errors.isEmpty();
        return response;
    }

    public GenerateLocalReviewResponse generate(GenerateLocalReviewRequest request) {
        List<Issue> errors = validator.errors(validator.validate(request));
        if (!errors.isEmpty()) throw new BadRequestException(message(errors));
        return submit(request, null, null);
    }

    /**
     * Repeats a run, optionally from a later stage. A retry is a new run with its own directory and
     * its own accounting; the stages it is not re-running have their artifacts carried across, so
     * the new run's directory is still complete on its own.
     */
    public GenerateLocalReviewResponse retry(String jobId, RetryLocalReviewRequest request) {
        ReviewJob source = reviewService.getReviewJob(jobId).orElseThrow(() -> new NotFoundException("Run not found: " + jobId));
        if (source.request == null) throw new BadRequestException("This run has no stored request, so it cannot be repeated");
        if (!Strings.isBlank(request.fromStage) && !LocalReviewStages.isValid(request.fromStage)) {
            throw new BadRequestException("Unknown stage: " + request.fromStage);
        }
        if (!Strings.isBlank(request.fromStage) && Strings.isBlank(source.outputDir)) {
            throw new BadRequestException("The original run left no artifacts, so it can only be repeated in full");
        }
        GenerateLocalReviewRequest retryRequest = LocalReviewRequestMapper.toRequest(source.request);
        retryRequest.operator = request.operator;
        List<Issue> errors = validator.errors(validator.validate(retryRequest));
        if (!errors.isEmpty()) throw new BadRequestException(message(errors));
        return submit(retryRequest, jobId, request.fromStage);
    }

    public LocalReviewJobActionResponse cancel(String jobId) {
        ReviewJob job = reviewService.getReviewJob(jobId).orElseThrow(() -> new NotFoundException("Run not found: " + jobId));
        if (isTerminal(job.status)) {
            return action(jobId, job.status, "This run has already finished.");
        }
        boolean stopped = runRegistry.cancel(jobId);
        if (!stopped) {
            reviewService.markStale(job, "No process behind this run — it did not survive a restart.");
            notificationService.raise(new NotificationService.NotificationRequest(jobId, job.serviceId,
                NotificationService.TYPE_RUN_STALE, NotificationService.SEVERITY_WARNING,
                "Run marked stale — " + job.serviceId, "Nothing was running behind it, so it was not cancelled."));
            return action(jobId, ReviewJob.STATUS_STALE, "Nothing was running behind this job; marked stale.");
        }
        logger.info("Cancellation requested for run {}", jobId);
        return action(jobId, ReviewJob.STATUS_CANCELLED, "Cancelling — the current stage is being stopped.");
    }

    public LocalReviewJobActionResponse update(String jobId, UpdateLocalReviewJobRequest request) {
        ReviewJob job = reviewService.getReviewJob(jobId).orElseThrow(() -> new NotFoundException("Run not found: " + jobId));
        if (request.note != null) job.note = request.note;
        if (request.labels != null) job.labels = request.labels;
        if (request.baseline != null) {
            if (Boolean.TRUE.equals(request.baseline)) reviewService.clearBaseline(job.serviceId);
            job.baseline = request.baseline;
        }
        reviewService.updateJob(job);
        return action(jobId, job.status, "Run updated.");
    }

    public LocalReviewJobActionResponse delete(String jobId) {
        ReviewJob job = reviewService.getReviewJob(jobId).orElseThrow(() -> new NotFoundException("Run not found: " + jobId));
        if (!isTerminal(job.status)) {
            throw new BadRequestException("Cancel this run before deleting it; it is still " + job.status);
        }
        String outputDir = job.outputDir;
        artifactService.deleteRunDir(job);
        notificationService.deleteByJob(jobId);
        reviewService.deleteRunData(job);
        return action(jobId, "DELETED", outputDir == null ? "Run deleted." : "Run and " + outputDir + " deleted.");
    }

    public PreflightResponse preflight(PreflightRequest request) {
        return preflightService.check(request);
    }

    private GenerateLocalReviewResponse submit(GenerateLocalReviewRequest request, String retryOfJobId, String fromStage) {
        String synthesisModel = GeminiModels.orDefault(request.synthesisModel, GeminiModels.SYNTHESIS_DEFAULT);
        String jobId = reviewService.createLocalReviewJob(new LocalReviewJobParams(
            request, GeminiModels.toAIModel(synthesisModel), retryOfJobId, fromStage));

        executor.submit("local-review-" + request.service, () -> localReviewJobExecutor.executeLocalReview(jobId));

        GenerateLocalReviewResponse response = new GenerateLocalReviewResponse();
        response.jobId = jobId;
        response.outputDir = predictedOutputDir(jobId, request);
        logger.info("Triggered local review job {} for service {}, outputDir={}, synthesisModel={}",
            jobId, request.service, response.outputDir, synthesisModel);
        return response;
    }

    /**
     * The directory the executor will create. Derived from the same inputs rather than guessed, so
     * the value the caller is handed is the one the run actually writes to.
     */
    private String predictedOutputDir(String jobId, GenerateLocalReviewRequest request) {
        ReviewJob job = reviewService.getReviewJob(jobId).orElse(null);
        String base = !Strings.isBlank(request.outputBaseDir) ? request.outputBaseDir : appSettingsService.get().outputBaseDir;
        if (Strings.isBlank(base)) base = "reports";
        if (job == null) return Path.of(LocalReviewOutputPaths.serviceDir(base, request.service)).toAbsolutePath().toString();
        return Path.of(LocalReviewOutputPaths.runDir(base, request.service, job.startedAt, jobId))
            .toAbsolutePath().toString();
    }

    private boolean isTerminal(String status) {
        return ReviewJob.STATUS_COMPLETED.equals(status) || ReviewJob.STATUS_FAILED.equals(status)
            || ReviewJob.STATUS_CANCELLED.equals(status) || ReviewJob.STATUS_STALE.equals(status);
    }

    /** One message carrying every blocking problem, so the API answer is as complete as the UI's. */
    private String message(List<Issue> errors) {
        StringBuilder builder = new StringBuilder(128);
        builder.append(errors.size()).append(errors.size() == 1 ? " problem" : " problems").append(" must be fixed:");
        for (Issue issue : errors) {
            builder.append("\n- ");
            if (issue.field() != null) builder.append(issue.field()).append(": ");
            builder.append(issue.message());
            if (issue.hint() != null) builder.append(" (").append(issue.hint()).append(')');
        }
        return builder.toString();
    }

    private IssueView toView(Issue issue) {
        IssueView view = new IssueView();
        view.field = issue.field();
        view.code = issue.code();
        view.severity = issue.severity();
        view.message = issue.message();
        view.hint = issue.hint();
        return view;
    }

    private LocalReviewJobActionResponse action(String jobId, String status, String message) {
        LocalReviewJobActionResponse response = new LocalReviewJobActionResponse();
        response.jobId = jobId;
        response.status = status;
        response.message = message;
        return response;
    }
}
