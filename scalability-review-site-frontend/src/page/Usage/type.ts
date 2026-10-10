import type {GetUsageResponse} from "type/api";

export const LOADING_USAGE = "@@usage/loading";

export interface State {
  groupBy: string;
  serviceId: string;
  days: number;
  usage: GetUsageResponse | null;
}
