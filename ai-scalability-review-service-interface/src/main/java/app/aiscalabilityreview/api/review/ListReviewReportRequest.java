package app.aiscalabilityreview.api.review;

import core.framework.api.validate.Min;
import core.framework.api.validate.NotBlank;
import core.framework.api.validate.NotNull;
import core.framework.api.web.service.QueryParam;

public class ListReviewReportRequest {
    @NotNull
    @NotBlank
    @QueryParam(name = "service_id")
    public String serviceId;

    @NotNull
    @Min(1)
    @QueryParam(name = "limit")
    public Integer limit;

    @NotNull
    @Min(0)
    @QueryParam(name = "skip")
    public Integer skip;
}
