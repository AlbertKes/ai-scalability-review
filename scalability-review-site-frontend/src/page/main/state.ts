import type {State} from "./type";

export const initialState: State = {
  settings: null,
  notifications: [],
  unreadCount: 0,
  navCollapsed: false,
  lastAnnouncedId: null,
};
