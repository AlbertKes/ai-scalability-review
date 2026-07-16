package app.aiscalabilityreview.api.review;

import core.framework.api.json.Property;

public class ValidateReportRequest {
    @Property(name = "model")
    public String model;

    @Property(name = "upload_to_confluence")
    public Boolean uploadToConfluence;
}