package app.aiscalabilityreview.web;

import app.aiscalabilityreview.api.LocalReviewWebService;
import app.aiscalabilityreview.api.localreview.GenerateLocalReviewRequest;
import app.aiscalabilityreview.api.localreview.GenerateLocalReviewResponse;
import app.aiscalabilityreview.api.localreview.LocalReviewJobActionResponse;
import app.aiscalabilityreview.api.localreview.LocalReviewRequestView;
import app.aiscalabilityreview.api.localreview.PreflightRequest;
import app.aiscalabilityreview.api.localreview.PreflightResponse;
import app.aiscalabilityreview.api.localreview.RetryLocalReviewRequest;
import app.aiscalabilityreview.api.localreview.UpdateLocalReviewJobRequest;
import app.aiscalabilityreview.api.localreview.ValidateLocalReviewResponse;
import app.aiscalabilityreview.service.LocalReviewService;
import core.framework.inject.Inject;
import core.framework.log.ActionLogContext;

public class LocalReviewWebServiceImpl implements LocalReviewWebService {
    @Inject
    LocalReviewService localReviewService;

    @Override
    public ValidateLocalReviewResponse validateRequest(LocalReviewRequestView request) {
        ActionLogContext.put("service", request.service);
        return localReviewService.validate(request);
    }

    @Override
    public GenerateLocalReviewResponse generate(GenerateLocalReviewRequest request) {
        ActionLogContext.put("service", request.service);
        return localReviewService.generate(request);
    }

    @Override
    public LocalReviewJobActionResponse cancel(String jobId) {
        ActionLogContext.put("review_job_id", jobId);
        return localReviewService.cancel(jobId);
    }

    @Override
    public GenerateLocalReviewResponse retry(String jobId, RetryLocalReviewRequest request) {
        ActionLogContext.put("review_job_id", jobId);
        return localReviewService.retry(jobId, request);
    }

    @Override
    public LocalReviewJobActionResponse update(String jobId, UpdateLocalReviewJobRequest request) {
        ActionLogContext.put("review_job_id", jobId);
        return localReviewService.update(jobId, request);
    }

    @Override
    public LocalReviewJobActionResponse delete(String jobId) {
        ActionLogContext.put("review_job_id", jobId);
        return localReviewService.delete(jobId);
    }

    @Override
    public PreflightResponse preflight(PreflightRequest request) {
        return localReviewService.preflight(request);
    }
}
