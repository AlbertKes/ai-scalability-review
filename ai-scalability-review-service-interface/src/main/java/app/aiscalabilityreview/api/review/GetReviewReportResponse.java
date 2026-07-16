package app.aiscalabilityreview.api.review;

import core.framework.api.json.Property;
import core.framework.api.validate.NotNull;

public class GetReviewReportResponse {
    @NotNull
    @Property(name = "view")
    public ReviewReportView view;
}
