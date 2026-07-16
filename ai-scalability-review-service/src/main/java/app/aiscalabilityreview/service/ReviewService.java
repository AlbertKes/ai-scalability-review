package app.aiscalabilityreview.service;

import app.aiscalabilityreview.api.serviceconfig.UpsertServiceConfigRequest;
import app.aiscalabilityreview.api.serviceconfig.UpsertServiceConfigResponse;
import app.aiscalabilityreview.domain.ReportComparison;
import app.aiscalabilityreview.domain.ReviewFeedback;
import app.aiscalabilityreview.domain.ReviewJob;
import app.aiscalabilityreview.domain.ReviewReport;
import app.aiscalabilityreview.domain.ServiceConfig;
import app.aiscalabilityreview.domain.ValidationResult;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import core.framework.inject.Inject;
import core.framework.mongo.MongoCollection;
import core.framework.mongo.Query;
import core.framework.util.Strings;
import core.framework.web.exception.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Core service managing all MongoDB CRUD for service configs, review jobs,
 * reports, feedback, comparisons, and validation results.
 */
public class ReviewService {
    private final Logger logger = LoggerFactory.getLogger(ReviewService.class);
    @Inject
    MongoCollection<ServiceConfig> serviceConfigCollection;
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

    public Optional<ServiceConfig> getServiceConfig(String serviceId) {
        return serviceConfigCollection.get(serviceId);
    }

    public List<ServiceConfig> listServiceConfigs() {
        Query query = new Query();
        return serviceConfigCollection.find(query);
    }

    public List<ServiceConfig> listEnabledServiceConfigs() {
        Query query = new Query();
        query.filter = Filters.eq("enabled", true);
        return serviceConfigCollection.find(query);
    }

    public UpsertServiceConfigResponse upsertServiceConfig(String serviceId, UpsertServiceConfigRequest request) {
        ServiceConfig config = serviceId == null ? new ServiceConfig() : getServiceConfig(serviceId).orElseThrow(
            () -> new NotFoundException(Strings.format("service config not found, id = {}", serviceId)));
        if (config.serviceId == null) {
            config.serviceId = UUID.randomUUID().toString();
        }
        config.displayName = request.displayName;
        config.team = request.team;
        config.tier = request.tier;
        config.enabled = request.enabled != null ? request.enabled : true;
        buildRepositoriesAndReview(request, config);
        buildRuntime(request, config);
        var now = ZonedDateTime.now();
        config.updatedAt = now;
        if (serviceId == null) {
            config.createdAt = now;
            serviceConfigCollection.insert(config);
        } else {
            serviceConfigCollection.replace(config);
        }
        var response = new UpsertServiceConfigResponse();
        response.id = config.serviceId;
        return response;
    }

    private void buildRuntime(UpsertServiceConfigRequest request, ServiceConfig config) {
        if (request.runtime != null) {
            config.environment = request.runtime.environment;
            config.namespace = request.runtime.namespace;
            config.hpaType = request.runtime.hpaType;
            config.kafkaConsumerGroups = request.runtime.kafkaConsumerGroups;
            config.mysqlHost = request.runtime.mysqlHost;
            config.mysqlDb = request.runtime.mysqlDb;
            config.atlasCluster = request.runtime.atlasCluster;
            config.redisEnabled = request.runtime.redisEnabled;
            config.datadogDomain = request.runtime.datadogDomain;

            if (request.runtime.deployments != null) {
                config.deployments = request.runtime.deployments.stream().map(d -> {
                    ServiceConfig.DeploymentConfig dc = new ServiceConfig.DeploymentConfig();
                    dc.name = d.name;
                    dc.type = d.type;
                    return dc;
                }).toList();
            }
        }
    }

    private void buildRepositoriesAndReview(UpsertServiceConfigRequest request, ServiceConfig config) {
        if (request.repositories != null) {
            if (request.repositories.app != null) {
                config.repoAppUrl = request.repositories.app.url;
                config.repoAppBranch = request.repositories.app.branch;
                config.repoAppServicePath = request.repositories.app.servicePath;
                config.repoAppIncludePaths = request.repositories.app.includePaths;
                config.repoAppExcludePaths = request.repositories.app.excludePaths;
            }
            if (request.repositories.infra != null) {
                config.repoInfraUrl = request.repositories.infra.url;
                config.repoInfraBranch = request.repositories.infra.branch;
                config.repoInfraServicePath = request.repositories.infra.servicePath;
            }
            if (request.repositories.k8sGitops != null) {
                config.repoK8sGitopsUrl = request.repositories.k8sGitops.url;
                config.repoK8sGitosBranch = request.repositories.k8sGitops.branch;
                config.repoK8sGitopsServicePath = request.repositories.k8sGitops.servicePath;
            }
        }
        if (request.review != null) {
            config.reviewSchedule = request.review.schedule;
            config.aiModel = request.review.aiModel;
            config.metricLookbackDays = request.review.metricLookbackDays != null
                ? request.review.metricLookbackDays : 28;
            config.confluenceSpaceKey = request.review.confluenceSpaceKey;
            config.confluenceParentPageTitle = request.review.confluenceParentPageTitle;
        }
    }

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
    public String createReviewJob(String serviceId, String triggerType, String model, String note) {
        ServiceConfig config = getServiceConfig(serviceId)
            .orElseThrow(() -> new NotFoundException(Strings.format("Service not found, id = {} ", serviceId)));

        ReviewJob job = new ReviewJob();
        job.jobId = UUID.randomUUID().toString();
        job.serviceId = serviceId;
        job.triggerType = triggerType;
        job.aiModel = model != null ? model : config.aiModel;
        job.status = "PENDING";
        job.note = note;
        job.startedAt = ZonedDateTime.now();
        job.stageStatuses = new LinkedHashMap<>();

        reviewJobCollection.insert(job);
        logger.info("Created review job {} for service {}", job.jobId, serviceId);
        return job.jobId;
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

    public void updateJobStatus(String jobId, String status, String currentStage) {
        ReviewJob job = reviewJobCollection.get(jobId).orElse(null);
        if (job == null) return;
        job.status = status;
        job.currentStage = currentStage;
        reviewJobCollection.replace(job);
    }

    public void updateJobStageStatus(String jobId, String stageName, ReviewJob.StageStatus stageStatus) {
        ReviewJob job = reviewJobCollection.get(jobId).orElse(null);
        if (job == null) return;
        if (job.stageStatuses == null) job.stageStatuses = new LinkedHashMap<>();
        job.stageStatuses.put(stageName, stageStatus);
        reviewJobCollection.replace(job);
    }

    public void completeJob(String jobId, String reportId) {
        ReviewJob job = reviewJobCollection.get(jobId).orElse(null);
        if (job == null) return;
        job.status = "COMPLETED";
        job.reportId = reportId;
        job.completedAt = ZonedDateTime.now();
        reviewJobCollection.replace(job);
    }

    public void failJob(String jobId, String errorMessage) {
        ReviewJob job = reviewJobCollection.get(jobId).orElse(null);
        if (job == null) return;
        job.status = "FAILED";
        job.errorMessage = errorMessage;
        job.completedAt = ZonedDateTime.now();
        reviewJobCollection.replace(job);
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

    public String createValidationResult(String reportId, String model, String triggeredBy) {
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
}