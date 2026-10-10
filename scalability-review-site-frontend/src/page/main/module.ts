import {Interval, Loading, Module, register} from "@wonder/core-fe";
import {NotificationAJAXWebService} from "@/service/NotificationAJAXWebService";
import {SettingsAJAXWebService} from "@/service/SettingsAJAXWebService";
import type {RootState} from "type/state";
import type {NotificationView, SettingsView} from "type/api";
import {initialState} from "./state";
import {LOAD_SETTINGS} from "./type";

const NOTIFICATION_PAGE_SIZE = 20;
const POLL_SECONDS = 10;

class MainModule extends Module<RootState, "main"> {
  @Loading(LOAD_SETTINGS)
  async onEnter() {
    await Promise.all([this.loadSettings(), this.loadNotifications()]);
  }

  /**
   * A review takes minutes, so the bell is polled rather than waited on. Ten seconds is slow enough
   * to be invisible on the database and fast enough that a finished run is noticed before the
   * operator switches back to the tab anyway.
   */
  @Interval(POLL_SECONDS)
  async onTick() {
    await this.loadNotifications();
  }

  async loadSettings() {
    const settings = await SettingsAJAXWebService.get();
    this.setState({settings});
  }

  async applySettings(settings: SettingsView) {
    this.setState({settings});
  }

  async loadNotifications() {
    const response = await NotificationAJAXWebService.list({limit: NOTIFICATION_PAGE_SIZE, skip: 0});
    this.setState({notifications: response.notifications, unreadCount: response.unread_count});
    this.announce(response.notifications);
  }

  async markRead(notificationId: string) {
    const response = await NotificationAJAXWebService.read(notificationId);
    this.setState({
      unreadCount: response.unread_count,
      notifications: this.state.notifications.map(item =>
        item.notification_id === notificationId ? {...item, read_at: new Date().toISOString()} : item
      ),
    });
  }

  async markAllRead() {
    await NotificationAJAXWebService.readAll();
    await this.loadNotifications();
  }

  async clearNotifications() {
    await NotificationAJAXWebService.clear();
    this.setState({notifications: [], unreadCount: 0});
  }

  async toggleNav() {
    this.setState({navCollapsed: !this.state.navCollapsed});
  }

  async requestBrowserPermission() {
    if (typeof Notification === "undefined") return;
    if (Notification.permission === "default") await Notification.requestPermission();
  }

  /**
   * Raises a desktop notification for anything new since the last poll, so a fifteen-minute run does
   * not need a tab watching it. Opt-in: nothing is shown unless the setting is on and the browser
   * permission has been granted.
   */
  private announce(notifications: NotificationView[]) {
    const newest = notifications[0];
    const {lastAnnouncedId, settings} = this.state;
    if (!newest) return;
    if (lastAnnouncedId === null) {
      this.setState({lastAnnouncedId: newest.notification_id});
      return;
    }
    if (newest.notification_id === lastAnnouncedId) return;

    const unseen: NotificationView[] = [];
    for (const item of notifications) {
      if (item.notification_id === lastAnnouncedId) break;
      unseen.push(item);
    }
    this.setState({lastAnnouncedId: newest.notification_id});

    if (!settings?.notification_prefs?.browser_enabled) return;
    if (typeof Notification === "undefined" || Notification.permission !== "granted") return;
    unseen.reverse().forEach(item => {
      new Notification(item.title, {body: item.body ?? "", tag: item.notification_id});
    });
  }
}

export const module = register(new MainModule("main", initialState));
export const actions = module.getActions();
