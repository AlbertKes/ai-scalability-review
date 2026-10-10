import {async} from "@wonder/core-fe";
import React from "react";
import {
  AppstoreOutlined,
  BarChartOutlined,
  DashboardOutlined,
  SettingOutlined,
  UnorderedListOutlined,
} from "@ant-design/icons";
import type {ConsoleMenu, ConsoleRoute} from "./type";

export const DASHBOARD_PATH = "Dashboard";
export const RUNS_PATH = "Runs";
export const NEW_RUN_PATH = "NewRun";
export const RUN_DETAIL_PATH = "RunDetail";
export const COMPARE_PATH = "Compare";
export const SERVICES_PATH = "Services";
export const USAGE_PATH = "Usage";
export const SETTINGS_PATH = "Settings";

export const routes: ConsoleRoute[] = [
  {
    path: DASHBOARD_PATH,
    component: async(() => import("@/page/Dashboard"), "Dashboard"),
    handle: {title: "Dashboard", menuSelectedKeys: [DASHBOARD_PATH]},
  },
  {
    path: RUNS_PATH,
    component: async(() => import("@/page/Runs"), "Runs"),
    handle: {title: "Runs", menuSelectedKeys: [RUNS_PATH]},
  },
  {
    path: NEW_RUN_PATH,
    component: async(() => import("@/page/NewRun"), "NewRun"),
    handle: {title: "New review", backTo: {label: "Runs", path: RUNS_PATH}, menuSelectedKeys: [RUNS_PATH]},
  },
  {
    path: `${RUN_DETAIL_PATH}/:jobId/:tab?`,
    component: async(() => import("@/page/RunDetail"), "RunDetail"),
    handle: {backTo: {label: "Runs", path: RUNS_PATH}, menuSelectedKeys: [RUNS_PATH]},
  },
  {
    path: COMPARE_PATH,
    component: async(() => import("@/page/Compare"), "Compare"),
    handle: {title: "Compare runs", backTo: {label: "Runs", path: RUNS_PATH}, menuSelectedKeys: [RUNS_PATH]},
  },
  {
    path: SERVICES_PATH,
    component: async(() => import("@/page/Services"), "Services"),
    handle: {title: "Services", menuSelectedKeys: [SERVICES_PATH]},
  },
  {
    path: `${SERVICES_PATH}/:serviceId`,
    component: async(() => import("@/page/ServiceDetail"), "ServiceDetail"),
    handle: {backTo: {label: "Services", path: SERVICES_PATH}, menuSelectedKeys: [SERVICES_PATH]},
  },
  {
    path: USAGE_PATH,
    component: async(() => import("@/page/Usage"), "Usage"),
    handle: {title: "AI usage", menuSelectedKeys: [USAGE_PATH]},
  },
  {
    path: SETTINGS_PATH,
    component: async(() => import("@/page/Settings"), "Settings"),
    handle: {title: "Settings", menuSelectedKeys: [SETTINGS_PATH]},
  },
];

export const menus: ConsoleMenu[] = [
  {title: "Dashboard", key: DASHBOARD_PATH, path: DASHBOARD_PATH, icon: React.createElement(DashboardOutlined)},
  {title: "Runs", key: RUNS_PATH, path: RUNS_PATH, icon: React.createElement(UnorderedListOutlined)},
  {title: "Services", key: SERVICES_PATH, path: SERVICES_PATH, icon: React.createElement(AppstoreOutlined)},
  {title: "AI usage", key: USAGE_PATH, path: USAGE_PATH, icon: React.createElement(BarChartOutlined)},
  {title: "Settings", key: SETTINGS_PATH, path: SETTINGS_PATH, icon: React.createElement(SettingOutlined)},
];
