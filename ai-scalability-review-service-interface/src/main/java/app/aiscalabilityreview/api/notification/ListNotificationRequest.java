package app.aiscalabilityreview.api.notification;

import core.framework.api.validate.Min;
import core.framework.api.validate.NotNull;
import core.framework.api.web.service.QueryParam;

public class ListNotificationRequest {
    @QueryParam(name = "unread_only")
    public Boolean unreadOnly;

    @NotNull
    @Min(1)
    @QueryParam(name = "limit")
    public Integer limit;

    @NotNull
    @Min(0)
    @QueryParam(name = "skip")
    public Integer skip;
}
