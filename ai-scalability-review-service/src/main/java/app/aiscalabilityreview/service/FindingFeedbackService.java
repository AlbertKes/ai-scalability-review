package app.aiscalabilityreview.service;

import app.aiscalabilityreview.api.review.ListFindingFeedbackResponse;
import app.aiscalabilityreview.api.review.ListFindingFeedbackResponse.FindingFeedbackView;
import app.aiscalabilityreview.api.review.SaveFindingFeedbackRequest;
import app.aiscalabilityreview.domain.ReviewFeedback;
import app.aiscalabilityreview.domain.ReviewReport;
import core.framework.inject.Inject;
import core.framework.web.exception.NotFoundException;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Records what a reader concluded about an individual finding.
 *
 * <p>This is the only ground truth there is for whether the pipeline produces useful findings: a
 * report that reads well and a report that is right are not the same thing, and only a human
 * marking false positives distinguishes them over time.
 */
public class FindingFeedbackService {
    @Inject
    ReviewService reviewService;

    public ListFindingFeedbackResponse list(String reportId) {
        ListFindingFeedbackResponse response = new ListFindingFeedbackResponse();
        response.feedback = reviewService.listFeedbackByReport(reportId).stream().map(this::toView).toList();
        return response;
    }

    public ListFindingFeedbackResponse save(String reportId, SaveFindingFeedbackRequest request) {
        ReviewReport report = reviewService.getReviewReport(reportId)
            .orElseThrow(() -> new NotFoundException("Report not found: " + reportId));

        ReviewFeedback feedback = new ReviewFeedback();
        feedback.feedbackId = UUID.randomUUID().toString();
        feedback.serviceId = report.serviceId;
        feedback.reportId = reportId;
        feedback.jobId = request.jobId == null ? report.jobId : request.jobId;
        feedback.findingKey = request.findingKey;
        feedback.findingTitle = request.findingTitle;
        feedback.verdict = request.verdict;
        feedback.rawText = request.comment;
        feedback.author = request.author;
        feedback.source = "CONSOLE";
        feedback.status = "PENDING";
        feedback.createdAt = ZonedDateTime.now();
        feedback.ingestedAt = feedback.createdAt;
        reviewService.saveFeedback(feedback);
        return list(reportId);
    }

    private FindingFeedbackView toView(ReviewFeedback feedback) {
        FindingFeedbackView view = new FindingFeedbackView();
        view.feedbackId = feedback.feedbackId;
        view.reportId = feedback.reportId;
        view.jobId = feedback.jobId;
        view.findingKey = feedback.findingKey;
        view.findingTitle = feedback.findingTitle;
        view.verdict = feedback.verdict;
        view.comment = feedback.rawText;
        view.author = feedback.author;
        view.createdAt = feedback.createdAt;
        return view;
    }
}
