package app.aiscalabilityreview.api.review;

import core.framework.api.web.service.QueryParam;

import java.time.ZonedDateTime;

public class GetUsageRequest {
    @QueryParam(name = "from")
    public ZonedDateTime from;

    @QueryParam(name = "to")
    public ZonedDateTime to;

    /** stage | model | service | day — defaults to stage. */
    @QueryParam(name = "group_by")
    public String groupBy;

    @QueryParam(name = "service_id")
    public String serviceId;
}
