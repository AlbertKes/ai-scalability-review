package app.aiscalabilityreview.api.review;

import core.framework.api.json.Property;

import java.util.List;

public class ListReviewReportResponse {
    @Property(name = "items")
    public List<ReviewReportView> items;

    @Property(name = "total")
    public Integer total;
}