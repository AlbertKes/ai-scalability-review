import type {NotificationView, SettingsView} from "type/api";

export const LOAD_SETTINGS = "@@main/load-settings";

export interface State {
  settings: SettingsView | null;
  notifications: NotificationView[];
  unreadCount: number;
  navCollapsed: boolean;
  /**
   * Newest notification id already shown as a desktop notification. Without it, every poll would
   * re-announce the same finished run.
   */
  lastAnnouncedId: string | null;
}
