package app.aiscalabilityreview.api.review;

import core.framework.api.validate.NotBlank;
import core.framework.api.validate.NotNull;
import core.framework.api.web.service.QueryParam;

public class CompareRunRequest {
    @NotNull
    @NotBlank
    @QueryParam(name = "job_id_a")
    public String jobIdA;

    @NotNull
    @NotBlank
    @QueryParam(name = "job_id_b")
    public String jobIdB;
}
