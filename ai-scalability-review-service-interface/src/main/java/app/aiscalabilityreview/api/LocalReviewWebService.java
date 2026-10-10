package app.aiscalabilityreview.api;

import app.aiscalabilityreview.api.localreview.GenerateLocalReviewRequest;
import app.aiscalabilityreview.api.localreview.GenerateLocalReviewResponse;
import app.aiscalabilityreview.api.localreview.LocalReviewJobActionResponse;
import app.aiscalabilityreview.api.localreview.LocalReviewRequestView;
import app.aiscalabilityreview.api.localreview.PreflightRequest;
import app.aiscalabilityreview.api.localreview.PreflightResponse;
import app.aiscalabilityreview.api.localreview.RetryLocalReviewRequest;
import app.aiscalabilityreview.api.localreview.UpdateLocalReviewJobRequest;
import app.aiscalabilityreview.api.localreview.ValidateLocalReviewResponse;
import core.framework.api.http.HTTPStatus;
import core.framework.api.web.service.DELETE;
import core.framework.api.web.service.GET;
import core.framework.api.web.service.POST;
import core.framework.api.web.service.PUT;
import core.framework.api.web.service.Path;
import core.framework.api.web.service.PathParam;
import core.framework.api.web.service.ResponseStatus;

public interface LocalReviewWebService {
    /**
     * Dry-runs the full rule set and returns every problem at once. Creates nothing — no job, no
     * output directory, no notification — and spends no tokens, so the form may call it freely.
     *
     * <p>Takes the unvalidated view rather than {@link GenerateLocalReviewRequest} on purpose: the
     * request bean's own {@code @NotNull} contract would reject a half-filled form at the framework
     * boundary, returning one flattened message instead of the structured, field-addressed list the
     * form needs.
     */
    @POST
    @Path("/local-review/validate")
    ValidateLocalReviewResponse validateRequest(LocalReviewRequestView request);

    /**
     * Validates first and only then submits: a request carrying a blocking error is rejected with
     * the complete list before any state is created.
     */
    @POST
    @ResponseStatus(HTTPStatus.CREATED)
    @Path("/local-review/generate")
    GenerateLocalReviewResponse generate(GenerateLocalReviewRequest request);

    @POST
    @Path("/local-review/job/:jobId/cancel")
    LocalReviewJobActionResponse cancel(@PathParam("jobId") String jobId);

    @POST
    @ResponseStatus(HTTPStatus.CREATED)
    @Path("/local-review/job/:jobId/retry")
    GenerateLocalReviewResponse retry(@PathParam("jobId") String jobId, RetryLocalReviewRequest request);

    @PUT
    @Path("/local-review/job/:jobId")
    LocalReviewJobActionResponse update(@PathParam("jobId") String jobId, UpdateLocalReviewJobRequest request);

    /** Deletes the run document and its artifact directory. */
    @DELETE
    @Path("/local-review/job/:jobId")
    LocalReviewJobActionResponse delete(@PathParam("jobId") String jobId);

    @GET
    @Path("/local-review/preflight")
    PreflightResponse preflight(PreflightRequest request);
}
