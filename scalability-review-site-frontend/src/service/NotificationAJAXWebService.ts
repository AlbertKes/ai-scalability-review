import {ajax} from "@wonder/core-fe";
import {ListNotificationRequest, ListNotificationResponse, NotificationActionResponse} from "type/api";

export class NotificationAJAXWebService {
  static list(request: ListNotificationRequest, signal?: AbortSignal): Promise<ListNotificationResponse> {
    return ajax("GET", "/notification", {}, request, {signal});
  }

  static read(notificationId: string): Promise<NotificationActionResponse> {
    return ajax("PUT", "/notification/:notificationId/read", {notificationId}, null);
  }

  static readAll(): Promise<NotificationActionResponse> {
    return ajax("PUT", "/notification/read-all", {}, null);
  }

  static clear(): Promise<NotificationActionResponse> {
    return ajax("DELETE", "/notification", {}, null);
  }
}
