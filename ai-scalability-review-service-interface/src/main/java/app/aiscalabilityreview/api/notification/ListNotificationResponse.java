package app.aiscalabilityreview.api.notification;

import core.framework.api.json.Property;

import java.util.List;

public class ListNotificationResponse {
    @Property(name = "notifications")
    public List<NotificationView> notifications;

    /** Drives the badge; the UI caps the rendering at 9+, the count itself is exact. */
    @Property(name = "unread_count")
    public Integer unreadCount;

    @Property(name = "total")
    public Integer total;
}
