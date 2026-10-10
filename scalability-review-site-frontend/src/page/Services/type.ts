import type {ListServiceSummaryResponse$ServiceSummaryView} from "type/api";

export const LOADING_SERVICES = "@@services/loading";

export interface State {
  services: ListServiceSummaryResponse$ServiceSummaryView[];
}
