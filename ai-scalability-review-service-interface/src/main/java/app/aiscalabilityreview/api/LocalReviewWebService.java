package app.aiscalabilityreview.api;

import app.aiscalabilityreview.api.localreview.GenerateLocalReviewRequest;
import app.aiscalabilityreview.api.localreview.GenerateLocalReviewResponse;
import core.framework.api.http.HTTPStatus;
import core.framework.api.web.service.POST;
import core.framework.api.web.service.Path;
import core.framework.api.web.service.ResponseStatus;

public interface LocalReviewWebService {
    @POST
    @ResponseStatus(HTTPStatus.CREATED)
    @Path("/local-review/generate")
    GenerateLocalReviewResponse generate(GenerateLocalReviewRequest request);
}
