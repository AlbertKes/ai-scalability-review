import type {ReviewJobView} from "type/api";

export const LOADING_SERVICE = "@@service-detail/loading";

export interface State {
  serviceId: string | null;
  runs: ReviewJobView[];
}
