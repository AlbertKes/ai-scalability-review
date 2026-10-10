package app.aiscalabilityreview.web;

import app.aiscalabilityreview.api.ReviewWebService;
import app.aiscalabilityreview.api.review.CompareReviewRequest;
import app.aiscalabilityreview.api.review.CompareReviewResponse;
import app.aiscalabilityreview.api.review.CompareRunRequest;
import app.aiscalabilityreview.api.review.CompareRunResponse;
import app.aiscalabilityreview.api.review.GetReviewJobResponse;
import app.aiscalabilityreview.api.review.GetReviewReportResponse;
import app.aiscalabilityreview.api.review.GetReviewReportValidationResponse;
import app.aiscalabilityreview.api.review.GetStageLogRequest;
import app.aiscalabilityreview.api.review.GetStageLogResponse;
import app.aiscalabilityreview.api.review.GetUsageRequest;
import app.aiscalabilityreview.api.review.GetUsageResponse;
import app.aiscalabilityreview.api.review.ListArtifactResponse;
import app.aiscalabilityreview.api.review.ListFindingFeedbackResponse;
import app.aiscalabilityreview.api.review.ListReviewJobRequest;
import app.aiscalabilityreview.api.review.ListReviewJobResponse;
import app.aiscalabilityreview.api.review.ListReviewReportRequest;
import app.aiscalabilityreview.api.review.ListReviewReportResponse;
import app.aiscalabilityreview.api.review.ListServiceSummaryResponse;
import app.aiscalabilityreview.api.review.SaveFindingFeedbackRequest;
import app.aiscalabilityreview.api.review.TriggerReviewRequest;
import app.aiscalabilityreview.api.review.TriggerReviewResponse;
import app.aiscalabilityreview.api.review.ValidateReportRequest;
import app.aiscalabilityreview.api.review.ValidateReportResponse;
import app.aiscalabilityreview.domain.ReviewJob;
import app.aiscalabilityreview.service.ArtifactService;
import app.aiscalabilityreview.service.FindingFeedbackService;
import app.aiscalabilityreview.service.ReviewJobService;
import app.aiscalabilityreview.service.ReviewReportService;
import app.aiscalabilityreview.service.ReviewService;
import app.aiscalabilityreview.service.RunComparisonService;
import app.aiscalabilityreview.service.ServiceSummaryService;
import app.aiscalabilityreview.service.UsageAnalyticsService;
import core.framework.inject.Inject;
import core.framework.log.ActionLogContext;
import core.framework.web.exception.NotFoundException;

public class ReviewWebServiceImpl implements ReviewWebService {
    @Inject
    ReviewJobService reviewJobService;
    @Inject
    ReviewReportService reviewReportService;
    @Inject
    ReviewService reviewService;
    @Inject
    ArtifactService artifactService;
    @Inject
    RunComparisonService runComparisonService;
    @Inject
    UsageAnalyticsService usageAnalyticsService;
    @Inject
    ServiceSummaryService serviceSummaryService;
    @Inject
    FindingFeedbackService findingFeedbackService;

    @Override
    public TriggerReviewResponse trigger(TriggerReviewRequest request) {
        ActionLogContext.put("service_id", request.serviceId);
        return reviewJobService.trigger(request);
    }

    @Override
    public GetReviewJobResponse getReviewJob(String jobId) {
        ActionLogContext.put("review_job_id", jobId);
        return reviewJobService.get(jobId);
    }

    @Override
    public ListReviewJobResponse listReviewJob(ListReviewJobRequest request) {
        ActionLogContext.put("service_id", request.serviceId);
        return reviewJobService.list(request);
    }

    @Override
    public ListArtifactResponse listArtifact(String jobId) {
        ActionLogContext.put("review_job_id", jobId);
        return artifactService.list(job(jobId));
    }

    @Override
    public GetStageLogResponse getStageLog(String jobId, GetStageLogRequest request) {
        ActionLogContext.put("review_job_id", jobId);
        return artifactService.stageLog(job(jobId), request.stage, request.offset);
    }

    @Override
    public CompareRunResponse compareRun(CompareRunRequest request) {
        return runComparisonService.compare(request.jobIdA, request.jobIdB);
    }

    @Override
    public GetUsageResponse usage(GetUsageRequest request) {
        return usageAnalyticsService.usage(request);
    }

    @Override
    public ListServiceSummaryResponse listService() {
        return serviceSummaryService.list();
    }

    @Override
    public GetReviewReportResponse getReviewReport(String reportId) {
        ActionLogContext.put("review_report_id", reportId);
        return reviewReportService.get(reportId);
    }

    @Override
    public ValidateReportResponse validateReviewReport(String reportId, ValidateReportRequest request) {
        ActionLogContext.put("review_report_id", reportId);
        return reviewReportService.validate(reportId, request);
    }

    @Override
    public GetReviewReportValidationResponse getReviewReportValidation(String validationId) {
        ActionLogContext.put("review_report_validation_id", validationId);
        return reviewReportService.getValidation(validationId);
    }

    @Override
    public ListReviewReportResponse listReviewReport(ListReviewReportRequest request) {
        ActionLogContext.put("service_id", request.serviceId);
        return reviewReportService.list(request);
    }

    @Override
    public ListFindingFeedbackResponse listFindingFeedback(String reportId) {
        ActionLogContext.put("review_report_id", reportId);
        return findingFeedbackService.list(reportId);
    }

    @Override
    public ListFindingFeedbackResponse saveFindingFeedback(String reportId, SaveFindingFeedbackRequest request) {
        ActionLogContext.put("review_report_id", reportId);
        return findingFeedbackService.save(reportId, request);
    }

    @Override
    public CompareReviewResponse compare(CompareReviewRequest request) {
        ActionLogContext.put("service_id", request.serviceId);
        return reviewReportService.compare(request);
    }

    private ReviewJob job(String jobId) {
        return reviewService.getReviewJob(jobId).orElseThrow(() -> new NotFoundException("Run not found: " + jobId));
    }
}
