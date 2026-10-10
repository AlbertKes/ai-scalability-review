import type {
  LocalReviewRequestView,
  PreflightResponse$CheckView,
  PresetView,
  ValidateLocalReviewResponse$IssueView,
} from "type/api";

export const VALIDATING = "@@new-run/validating";
export const SUBMITTING = "@@new-run/submitting";
export const PREFLIGHTING = "@@new-run/preflighting";

/** Derived from this service's own past runs; null when there is nothing to derive it from. */
export interface CostEstimate {
  basedOnRuns: number;
  tokens: number;
  durationMs: number;
  costUsd: number | null;
}

export interface State {
  request: LocalReviewRequestView;
  labels: string[];
  errors: ValidateLocalReviewResponse$IssueView[];
  warnings: ValidateLocalReviewResponse$IssueView[];
  /** False whenever the form changed after the last validation pass. */
  validated: boolean;
  acknowledged: boolean;
  presets: PresetView[];
  preflight: PreflightResponse$CheckView[];
  estimate: CostEstimate | null;
}
