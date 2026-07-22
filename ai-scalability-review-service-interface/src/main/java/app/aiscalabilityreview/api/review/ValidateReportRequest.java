package app.aiscalabilityreview.api.review;

import app.aiscalabilityreview.api.serviceconfig.embedded.AIModelView;
import core.framework.api.json.Property;

public class ValidateReportRequest {
    @Property(name = "model")
    public AIModelView model;

    @Property(name = "upload_to_confluence")
    public Boolean uploadToConfluence;
}