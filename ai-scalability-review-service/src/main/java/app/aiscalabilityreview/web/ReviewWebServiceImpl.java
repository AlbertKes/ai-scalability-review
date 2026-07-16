package app.aiscalabilityreview.web;

import app.aiscalabilityreview.api.ReviewWebService;
import app.aiscalabilityreview.api.review.CompareReviewRequest;
import app.aiscalabilityreview.api.review.CompareReviewResponse;
import app.aiscalabilityreview.api.review.GetReviewJobResponse;
import app.aiscalabilityreview.api.review.GetReviewReportResponse;
import app.aiscalabilityreview.api.review.GetReviewReportValidationResponse;
import app.aiscalabilityreview.api.review.ListReviewJobRequest;
import app.aiscalabilityreview.api.review.ListReviewJobResponse;
import app.aiscalabilityreview.api.review.ListReviewReportRequest;
import app.aiscalabilityreview.api.review.ListReviewReportResponse;
import app.aiscalabilityreview.api.review.TriggerReviewRequest;
import app.aiscalabilityreview.api.review.TriggerReviewResponse;
import app.aiscalabilityreview.api.review.ValidateReportRequest;
import app.aiscalabilityreview.api.review.ValidateReportResponse;
import app.aiscalabilityreview.service.ReviewJobService;
import app.aiscalabilityreview.service.ReviewReportService;
import core.framework.inject.Inject;
import core.framework.log.ActionLogContext;

public class ReviewWebServiceImpl implements ReviewWebService {
    @Inject
    ReviewJobService reviewJobService;
    @Inject
    ReviewReportService reviewReportService;

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
    public CompareReviewResponse compare(CompareReviewRequest request) {
        ActionLogContext.put("service_id", request.serviceId);
        return reviewReportService.compare(request);
    }
}
