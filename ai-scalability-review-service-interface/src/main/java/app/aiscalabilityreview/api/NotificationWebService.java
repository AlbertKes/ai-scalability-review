package app.aiscalabilityreview.api;

import app.aiscalabilityreview.api.notification.ListNotificationRequest;
import app.aiscalabilityreview.api.notification.ListNotificationResponse;
import app.aiscalabilityreview.api.notification.NotificationActionResponse;
import core.framework.api.web.service.DELETE;
import core.framework.api.web.service.GET;
import core.framework.api.web.service.PUT;
import core.framework.api.web.service.Path;
import core.framework.api.web.service.PathParam;

public interface NotificationWebService {
    @GET
    @Path("/notification")
    ListNotificationResponse list(ListNotificationRequest request);

    @PUT
    @Path("/notification/:notificationId/read")
    NotificationActionResponse read(@PathParam("notificationId") String notificationId);

    @PUT
    @Path("/notification/read-all")
    NotificationActionResponse readAll();

    @DELETE
    @Path("/notification")
    NotificationActionResponse clear();
}
