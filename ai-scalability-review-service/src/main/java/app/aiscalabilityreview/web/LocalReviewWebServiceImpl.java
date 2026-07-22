package app.aiscalabilityreview.web;

import app.aiscalabilityreview.api.LocalReviewWebService;
import app.aiscalabilityreview.api.localreview.GenerateLocalReviewRequest;
import app.aiscalabilityreview.api.localreview.GenerateLocalReviewResponse;
import app.aiscalabilityreview.service.LocalReviewService;
import core.framework.inject.Inject;
import core.framework.log.ActionLogContext;

public class LocalReviewWebServiceImpl implements LocalReviewWebService {
    @Inject
    LocalReviewService localReviewService;

    @Override
    public GenerateLocalReviewResponse generate(GenerateLocalReviewRequest request) {
        ActionLogContext.put("service", request.service);
        return localReviewService.generate(request);
    }
}
