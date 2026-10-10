import type {ComponentType, ReactNode} from "react";

export interface Breadcrumb {
  label: string;
  path: string;
}

export interface RouteHandle {
  title?: string;
  backTo?: Breadcrumb;
  menuSelectedKeys?: string[];
}

export interface ConsoleRoute {
  path: string;
  component?: ComponentType<any>;
  handle?: RouteHandle;
}

export interface ConsoleMenu {
  title: string;
  key: string;
  path: string;
  icon: ReactNode;
}
