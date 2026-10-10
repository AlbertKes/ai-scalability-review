import type {RunFilter, SavedFilter, State} from "./type";

const SAVED_FILTER_KEY = "scalability-review.saved-filters";

export const emptyFilter = (): RunFilter => ({
  service_id: "",
  status: [],
  env: "",
  operator: "",
  trigger_type: "",
  keyword: "",
  search_reports: false,
  from: "",
  to: "",
  overall_score: [],
  validation_verdict: [],
  within_budget: null,
  has_failed_stage: false,
  label: "",
});

export const loadSavedFilters = (): SavedFilter[] => {
  try {
    const raw = window.localStorage.getItem(SAVED_FILTER_KEY);
    return raw ? (JSON.parse(raw) as SavedFilter[]) : [];
  } catch {
    return [];
  }
};

export const persistSavedFilters = (filters: SavedFilter[]): void => {
  window.localStorage.setItem(SAVED_FILTER_KEY, JSON.stringify(filters));
};

export const initialState: State = {
  jobs: [],
  total: 0,
  filter: emptyFilter(),
  page: 1,
  pageSize: 25,
  sort: "started_at",
  selectedJobIds: [],
  savedFilters: [],
};
