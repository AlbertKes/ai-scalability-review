package app.aiscalabilityreview.api.review;

import core.framework.api.json.Property;
import core.framework.api.validate.NotBlank;
import core.framework.api.validate.NotNull;

public class CompareReviewRequest {
    @NotNull
    @NotBlank
    @Property(name = "service_id")
    public String serviceId;

    @Property(name = "report_id_a")
    public String reportIdA;

    @Property(name = "report_id_b")
    public String reportIdB;

    @Property(name = "period_a")
    public String periodA;  // "2026-06" shorthand

    @Property(name = "period_b")
    public String periodB;
}