package app.aiscalabilityreview.service.builder;

import app.aiscalabilityreview.api.review.GetReviewReportValidationResponse;
import app.aiscalabilityreview.api.review.ReviewReportView;
import app.aiscalabilityreview.domain.ReviewReport;
import app.aiscalabilityreview.domain.ValidationResult;

public class ReviewReportBuilder {
    public static ReviewReportView reviewReportView(ReviewReport report) {
        var view = new ReviewReportView();
        view.reportId = report.reportId;
        view.jobId = report.jobId;
        view.serviceId = report.serviceId;
        view.periodLabel = report.periodLabel;
        view.aiModel = report.aiModel;
        view.reportMarkdown = report.reportMarkdown;
        view.confluencePageUrl = report.confluencePageUrl;
        view.generatedAt = report.generatedAt;
        ReviewReportView.ScoresView scores = new ReviewReportView.ScoresView();
        scores.traffic = report.trafficScore;
        scores.latency = report.latencyScore;
        scores.errors = report.errorsScore;
        scores.resources = report.resourcesScore;
        scores.persistence = report.persistenceScore;
        scores.overall = report.overallScore;
        view.scores = scores;
        return view;
    }

    public static GetReviewReportValidationResponse reviewReportValidationResponse(ValidationResult result) {
        var response = new GetReviewReportValidationResponse();
        response.validationId = result.validationId;
        response.reportId = result.reportId;
        response.serviceId = result.serviceId;
        response.aiModel = result.aiModel;
        response.status = result.status;
        response.verdict = result.verdict;
        response.inputTokens = result.inputTokens;
        response.outputTokens = result.outputTokens;
        response.estimatedCostUsd = result.estimatedCostUsd;
        response.confluencePageUrl = result.confluencePageUrl;
        response.generatedAt = result.generatedAt;
        response.validationReportMarkdown = result.validationReportMarkdown;
        return response;
    }
}
