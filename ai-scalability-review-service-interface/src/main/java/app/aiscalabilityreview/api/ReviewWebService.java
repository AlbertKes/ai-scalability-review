package app.aiscalabilityreview.api;

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
import core.framework.api.http.HTTPStatus;
import core.framework.api.web.service.GET;
import core.framework.api.web.service.POST;
import core.framework.api.web.service.Path;
import core.framework.api.web.service.PathParam;
import core.framework.api.web.service.ResponseStatus;

public interface ReviewWebService {
    @POST
    @ResponseStatus(HTTPStatus.CREATED)
    @Path("/review/trigger")
    TriggerReviewResponse trigger(TriggerReviewRequest request);

    @GET
    @Path("/review/job/:jobId")
    GetReviewJobResponse getReviewJob(@PathParam("jobId") String jobId);

    @GET
    @Path("/review/job")
    ListReviewJobResponse listReviewJob(ListReviewJobRequest request);

    @GET
    @Path("/review/job/:jobId/artifact")
    ListArtifactResponse listArtifact(@PathParam("jobId") String jobId);

    @GET
    @Path("/review/job/:jobId/stage-log")
    GetStageLogResponse getStageLog(@PathParam("jobId") String jobId, GetStageLogRequest request);

    @GET
    @Path("/review/run/compare")
    CompareRunResponse compareRun(CompareRunRequest request);

    @GET
    @Path("/review/usage")
    GetUsageResponse usage(GetUsageRequest request);

    @GET
    @Path("/review/service")
    ListServiceSummaryResponse listService();

    @GET
    @Path("/review/report/:reportId")
    GetReviewReportResponse getReviewReport(@PathParam("reportId") String reportId);

    @POST
    @ResponseStatus(HTTPStatus.CREATED)
    @Path("/review/report/:reportId/validate")
    ValidateReportResponse validateReviewReport(@PathParam("reportId") String reportId, ValidateReportRequest request);

    @GET
    @Path("/review/report/validation/:validationId")
    GetReviewReportValidationResponse getReviewReportValidation(@PathParam("validationId") String validationId);

    @GET
    @Path("/review/report")
    ListReviewReportResponse listReviewReport(ListReviewReportRequest request);

    @GET
    @Path("/review/report/:reportId/feedback")
    ListFindingFeedbackResponse listFindingFeedback(@PathParam("reportId") String reportId);

    @POST
    @ResponseStatus(HTTPStatus.CREATED)
    @Path("/review/report/:reportId/feedback")
    ListFindingFeedbackResponse saveFindingFeedback(@PathParam("reportId") String reportId, SaveFindingFeedbackRequest request);

    @POST
    @ResponseStatus(HTTPStatus.CREATED)
    @Path("/review/compare")
    CompareReviewResponse compare(CompareReviewRequest request);
}
