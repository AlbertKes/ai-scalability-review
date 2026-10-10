package app.aiscalabilityreview.web;

import app.aiscalabilityreview.api.NotificationWebService;
import app.aiscalabilityreview.api.notification.ListNotificationRequest;
import app.aiscalabilityreview.api.notification.ListNotificationResponse;
import app.aiscalabilityreview.api.notification.NotificationActionResponse;
import app.aiscalabilityreview.api.notification.NotificationView;
import app.aiscalabilityreview.domain.Notification;
import app.aiscalabilityreview.service.NotificationService;
import core.framework.inject.Inject;

public class NotificationWebServiceImpl implements NotificationWebService {
    @Inject
    NotificationService notificationService;

    @Override
    public ListNotificationResponse list(ListNotificationRequest request) {
        boolean unreadOnly = Boolean.TRUE.equals(request.unreadOnly);
        ListNotificationResponse response = new ListNotificationResponse();
        response.notifications = notificationService.list(unreadOnly, request.limit, request.skip)
            .stream().map(this::toView).toList();
        response.unreadCount = notificationService.count(true);
        response.total = notificationService.count(unreadOnly);
        return response;
    }

    @Override
    public NotificationActionResponse read(String notificationId) {
        return action(notificationService.markRead(notificationId));
    }

    @Override
    public NotificationActionResponse readAll() {
        return action(notificationService.markAllRead());
    }

    @Override
    public NotificationActionResponse clear() {
        return action(notificationService.clear());
    }

    private NotificationActionResponse action(int affected) {
        NotificationActionResponse response = new NotificationActionResponse();
        response.affected = affected;
        response.unreadCount = notificationService.count(true);
        return response;
    }

    private NotificationView toView(Notification notification) {
        NotificationView view = new NotificationView();
        view.notificationId = notification.notificationId;
        view.jobId = notification.jobId;
        view.serviceId = notification.serviceId;
        view.type = notification.type;
        view.severity = notification.severity;
        view.title = notification.title;
        view.body = notification.body;
        view.createdAt = notification.createdAt;
        view.readAt = notification.readAt;
        return view;
    }
}
