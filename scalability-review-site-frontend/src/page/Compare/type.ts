import type {CompareRunResponse, ReviewJobView} from "type/api";

export const LOADING_COMPARE = "@@compare/loading";

export interface State {
  jobIdA: string | null;
  jobIdB: string | null;
  candidates: ReviewJobView[];
  result: CompareRunResponse | null;
}
