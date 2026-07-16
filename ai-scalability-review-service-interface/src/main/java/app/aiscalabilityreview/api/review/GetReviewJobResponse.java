package app.aiscalabilityreview.api.review;

import core.framework.api.json.Property;
import core.framework.api.validate.NotNull;

public class GetReviewJobResponse {
    @NotNull
    @Property(name = "view")
    public ReviewJobView view;
}
