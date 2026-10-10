package app.aiscalabilityreview.api.review;

import core.framework.api.json.Property;

import java.util.List;

public class ListReviewJobResponse {
    @Property(name = "jobs")
    public List<ReviewJobView> jobs;

    /** Total matching the filters, not the page size — the list pages server side. */
    @Property(name = "total")
    public Integer total;
}
