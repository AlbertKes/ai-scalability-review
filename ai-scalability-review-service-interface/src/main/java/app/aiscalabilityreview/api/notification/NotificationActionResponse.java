package app.aiscalabilityreview.api.notification;

import core.framework.api.json.Property;

public class NotificationActionResponse {
    @Property(name = "unread_count")
    public Integer unreadCount;

    @Property(name = "affected")
    public Integer affected;
}
