package app.aiscalabilityreview.api.review;

import core.framework.api.json.Property;

import java.util.List;

public class ListReviewJobResponse {
    @Property(name = "items")
    public List<ReviewJobView> items;

    @Property(name = "total")
    public Integer total;
}