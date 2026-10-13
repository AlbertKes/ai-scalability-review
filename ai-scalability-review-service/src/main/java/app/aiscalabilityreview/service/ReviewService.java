package app.aiscalabilityreview.service;

import app.aiscalabilityreview.api.localreview.GenerateLocalReviewRequest;
import app.aiscalabilityreview.domain.ReportComparison;
import app.aiscalabilityreview.domain.ReviewFeedback;
import app.aiscalabilityreview.domain.ReviewJob;
import app.aiscalabilityreview.domain.ReviewReport;
import app.aiscalabilityreview.domain.ServiceConfig;
import app.aiscalabilityreview.domain.ValidationResult;
import app.aiscalabilityreview.domain.embedded.AIModel;
import app.aiscalabilityreview.service.builder.LocalReviewRequestMapper;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Updates;
import core.framework.inject.Inject;
import core.framework.mongo.MongoCollection;
import core.framework.mongo.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.function.Consumer;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.IntStream;

/**
 * Core service managing all MongoDB CRUD for service configs, review jobs,
 * reports, feedback, comparisons, and validation results.
 */
public class ReviewService {
    /**
     * Every mutation of a job is a read-modify-write of the whole document, so two writers racing
     * lose one of the two updates. That became reachable the moment code analysis started running
     * alongside the infra and metric stages: both report their status into the same
     * {@code stage_statuses} map. Striped rather than per-job so the table cannot grow without
     * bound; 64 stripes is far more than a local console ever needs.
     */
    private static final int JOB_LOCK_STRIPES = 64;

    private final Logger logger = LoggerFactory.getLogger(ReviewService.class);
    private final List<ReentrantLock> jobLocks = IntStream.range(0, JOB_LOCK_STRIPES)
        .mapToObj(stripe -> new ReentrantLock())
        .toList();
    @Inject
    MongoCollection<ReviewJob> reviewJobCollection;
    @Inject
    MongoCollection<ReviewReport> reviewReportCollection;
    @Inject
    MongoCollection<ReviewFeedback> reviewFeedbackCollection;
    @Inject
    MongoCollection<ReportComparison> reportComparisonCollection;
    @Inject
    MongoCollection<ValidationResult> validationResultCollection;
    @Inject
    ServiceConfigService serviceConfigService;

    // ---- ReviewJob ----

    /**
     * Create a new ReviewJob for a service and persist it.
     *
     * @param serviceId   service being reviewed
     * @param triggerType "MANUAL" or "SCHEDULED"
     * @param model       AI model override (can be null to use service default)
     * @param note        optional note or label
     * @return the new job ID
     */
    public String createReviewJob(String serviceId, String triggerType, AIModel model, String note) {
        ServiceConfig config = serviceConfigService.getServiceConfig(serviceId);

        ReviewJob job = new ReviewJob();
        job.jobId = UUID.randomUUID().toString();
        job.serviceId = serviceId;
        job.triggerType = triggerType;
        job.aiModel = model != null ? model : config.reviewConfig.aiModel;
        job.status = "PENDING";
        job.note = note;
        job.startedAt = ZonedDateTime.now();
        job.stageStatuses = new LinkedHashMap<>();

        reviewJobCollection.insert(job);
        logger.info("Created review job {} for service {}", job.jobId, serviceId);
        return job.jobId;
    }

    /**
     * Creates a ReviewJob for a local (Gemini CLI) review run. Does not require a ServiceConfig.
     *
     * <p>The trigger request is stored on the job rather than held in memory: that is what lets the
     * console say what a run looked at, repeat it, and recognise a run that a restart abandoned.
     *
     * @return the new job ID
     */
    public String createLocalReviewJob(LocalReviewJobParams params) {
        ReviewJob job = new ReviewJob();
        job.jobId = UUID.randomUUID().toString();
        job.serviceId = params.request().service;
        job.triggerType = "MANUAL";
        job.triggeredBy = params.request().operator;
        job.aiModel = params.model();
        job.status = ReviewJob.STATUS_PENDING;
        job.startedAt = ZonedDateTime.now();
        job.stageStatuses = new LinkedHashMap<>();
        job.env = params.request().env;
        job.namespace = params.request().namespace;
        job.domain = params.request().domain;
        job.request = LocalReviewRequestMapper.toSnapshot(params.request());
        job.labels = params.request().labels;
        job.acknowledgedWarnings = params.request().acknowledgedWarnings;
        job.retryOfJobId = params.retryOfJobId();
        job.retryFromStage = params.retryFromStage();
        job.baseline = Boolean.FALSE;

        reviewJobCollection.insert(job);
        logger.info("Created local review job {} for service {}", job.jobId, job.serviceId);
        return job.jobId;
    }

    public void updateJob(ReviewJob job) {
        reviewJobCollection.replace(job);
    }

    public void deleteJob(String jobId) {
        reviewJobCollection.delete(jobId);
    }

    /** Marks a run as waiting for a concurrency slot, so the list can show it as queued. */
    public void markQueued(String jobId) {
        mutateJob(jobId, job -> job.status = ReviewJob.STATUS_QUEUED);
    }

    public void cancelJob(String jobId, String cancelledBy) {
        mutateJob(jobId, job -> {
            job.status = ReviewJob.STATUS_CANCELLED;
            job.cancelledAt = ZonedDateTime.now();
            job.cancelledBy = cancelledBy;
            job.completedAt = job.cancelledAt;
        });
    }

    /**
     * Marks a run whose process no longer exists. Used at startup: a job left RUNNING by a restart
     * has no one behind it, and a spinner that never resolves is worse than an honest dead end.
     */
    public void markStale(String jobId, String reason) {
        mutateJob(jobId, job -> {
            job.status = ReviewJob.STATUS_STALE;
            job.errorMessage = reason;
            job.completedAt = ZonedDateTime.now();
        });
    }

    /** Only one run per service can be the baseline, so setting one clears the rest. */
    public void clearBaseline(String serviceId) {
        reviewJobCollection.update(
            Filters.and(Filters.eq("service_id", serviceId), Filters.eq("baseline", Boolean.TRUE)),
            Updates.set("baseline", Boolean.FALSE));
    }

    /** Removes a run and everything derived from it, so a deleted run leaves no orphan documents. */
    public void deleteRunData(ReviewJob job) {
        validationResultCollection.delete(Filters.eq("job_id", job.jobId));
        reviewReportCollection.delete(Filters.eq("job_id", job.jobId));
        reviewJobCollection.delete(job.jobId);
        logger.info("Deleted review job {} and its derived documents", job.jobId);
    }

    public List<ReviewJob> listJobsByService(String serviceId, int limit) {
        Query query = new Query();
        query.filter = Filters.eq("service_id", serviceId);
        query.sort = Sorts.descending("started_at");
        query.limit = limit;
        return reviewJobCollection.find(query);
    }

    public List<ReviewFeedback> listFeedbackByReport(String reportId) {
        Query query = new Query();
        query.filter = Filters.eq("report_id", reportId);
        query.sort = Sorts.descending("created_at");
        return reviewFeedbackCollection.find(query);
    }


    public Optional<ReviewJob> getReviewJob(String jobId) {
        return reviewJobCollection.get(jobId);
    }

    public List<ReviewJob> listReviewJobs(String serviceId, int limit, int skip) {
        Query query = new Query();
        query.filter = Filters.eq("service_id", serviceId);
        query.sort = Sorts.descending("started_at");
        query.limit = limit;
        query.skip = skip;
        return reviewJobCollection.find(query);
    }

    /**
     * Reads, mutates and writes one job under its stripe, so concurrent stages cannot overwrite
     * each other. Every mutation of a job document goes through here.
     */
    public void mutateJob(String jobId, Consumer<ReviewJob> mutation) {
        ReentrantLock lock = jobLocks.get(Math.floorMod(jobId.hashCode(), JOB_LOCK_STRIPES));
        lock.lock();
        try {
            ReviewJob job = reviewJobCollection.get(jobId).orElse(null);
            if (job == null) return;
            mutation.accept(job);
            reviewJobCollection.replace(job);
        } finally {
            lock.unlock();
        }
    }

    /**
     * {@code currentStage} is last-writer-wins while two stages run at once. The per-stage statuses
     * are the authoritative view of what is happening, and the console renders those.
     */
    public void updateJobStatus(String jobId, String status, String currentStage) {
        mutateJob(jobId, job -> {
            job.status = status;
            job.currentStage = currentStage;
        });
    }

    public void updateJobStageStatus(String jobId, String stageName, ReviewJob.StageStatus stageStatus) {
        mutateJob(jobId, job -> {
            if (job.stageStatuses == null) job.stageStatuses = new LinkedHashMap<>();
            job.stageStatuses.put(stageName, stageStatus);
        });
    }

    public void completeJob(String jobId, String reportId) {
        mutateJob(jobId, job -> {
            job.status = ReviewJob.STATUS_COMPLETED;
            job.reportId = reportId;
            job.completedAt = ZonedDateTime.now();
        });
    }

    public void failJob(String jobId, String errorMessage) {
        mutateJob(jobId, job -> {
            job.status = ReviewJob.STATUS_FAILED;
            job.errorMessage = errorMessage;
            job.completedAt = ZonedDateTime.now();
        });
    }

    // ---- ReviewReport ----

    public Optional<ReviewReport> getReviewReport(String reportId) {
        return reviewReportCollection.get(reportId);
    }

    public Optional<ReviewReport> getReportByJobId(String jobId) {
        Query query = new Query();
        query.filter = Filters.eq("job_id", jobId);
        query.limit = 1;
        List<ReviewReport> results = reviewReportCollection.find(query);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public List<ReviewReport> listReportsByService(String serviceId, int limit, int skip) {
        Query query = new Query();
        query.filter = Filters.eq("service_id", serviceId);
        query.sort = Sorts.descending("generated_at");
        query.limit = limit;
        query.skip = skip;
        return reviewReportCollection.find(query);
    }

    public void saveReport(ReviewReport report) {
        reviewReportCollection.insert(report);
    }

    public void updateReport(ReviewReport report) {
        reviewReportCollection.replace(report);
    }

    public List<ReviewFeedback> listPendingFeedback(String serviceId) {
        Query query = new Query();
        query.filter = Filters.and(
            Filters.eq("service_id", serviceId),
            Filters.eq("status", "PENDING")
        );
        return reviewFeedbackCollection.find(query);
    }

    public void saveFeedback(ReviewFeedback feedback) {
        reviewFeedbackCollection.insert(feedback);
    }

    public boolean feedbackExists(String confluenceCommentId) {
        Query query = new Query();
        query.filter = Filters.eq("confluence_comment_id", confluenceCommentId);
        query.limit = 1;
        return !reviewFeedbackCollection.find(query).isEmpty();
    }

    public void saveComparison(ReportComparison comparison) {
        reportComparisonCollection.insert(comparison);
    }

    public String createValidationResult(String reportId, AIModel model, String triggeredBy) {
        ReviewReport report = reviewReportCollection.get(reportId).orElse(null);
        if (report == null) {
            throw new IllegalArgumentException("Report not found: " + reportId);
        }

        ValidationResult result = new ValidationResult();
        result.validationId = UUID.randomUUID().toString();
        result.reportId = reportId;
        result.jobId = report.jobId;
        result.serviceId = report.serviceId;
        result.aiModel = model;
        result.status = "PENDING";
        result.triggeredBy = triggeredBy;
        result.generatedAt = ZonedDateTime.now();
        result.reportOriginalDate = report.generatedAt;

        validationResultCollection.insert(result);
        return result.validationId;
    }

    public Optional<ValidationResult> getValidationResult(String validationId) {
        return validationResultCollection.get(validationId);
    }

    public void updateValidationResult(ValidationResult result) {
        validationResultCollection.replace(result);
    }

    /**
     * Find reports with a Confluence page ID, generated within the last N days.
     */
    public List<ReviewReport> findRecentReportsWithConfluence(int daysBack) {
        ZonedDateTime since = ZonedDateTime.now().minusDays(daysBack);
        Query query = new Query();
        query.filter = Filters.and(
            Filters.exists("confluence_page_id"),
            Filters.gte("generated_at", since)
        );
        query.sort = Sorts.descending("generated_at");
        return reviewReportCollection.find(query);
    }

    /**
     * @param retryOfJobId  the run this one repeats, or null for a fresh run
     * @param retryFromStage first stage to actually execute; earlier stages reuse the source run
     */
    public record LocalReviewJobParams(GenerateLocalReviewRequest request,
                                       AIModel model,
                                       String retryOfJobId,
                                       String retryFromStage) {
    }
}
