import type {GetUsageResponse, PreflightResponse$CheckView, ReviewJobView} from "type/api";

export const LOADING_DASHBOARD = "@@dashboard/loading";

export interface State {
  running: ReviewJobView[];
  recent: ReviewJobView[];
  overBudget: ReviewJobView[];
  usage: GetUsageResponse | null;
  preflight: PreflightResponse$CheckView[];
}
