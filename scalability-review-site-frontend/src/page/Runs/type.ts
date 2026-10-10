import type {ReviewJobView} from "type/api";

export const LOADING_RUNS = "@@runs/loading";

/** Mirrors ListReviewJobRequest, minus paging, so it can round-trip through the URL. */
export interface RunFilter {
  service_id: string;
  status: string[];
  env: string;
  operator: string;
  trigger_type: string;
  keyword: string;
  search_reports: boolean;
  from: string;
  to: string;
  overall_score: string[];
  validation_verdict: string[];
  within_budget: boolean | null;
  has_failed_stage: boolean;
  label: string;
}

export interface SavedFilter {
  name: string;
  filter: RunFilter;
}

export interface State {
  jobs: ReviewJobView[];
  total: number;
  filter: RunFilter;
  page: number;
  pageSize: number;
  sort: string;
  selectedJobIds: string[];
  savedFilters: SavedFilter[];
}
