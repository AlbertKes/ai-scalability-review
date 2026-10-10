import type {
  GetReviewReportValidationResponse,
  ListArtifactResponse$ArtifactView,
  ListFindingFeedbackResponse$FindingFeedbackView,
  ReviewJobView,
  ReviewReportView,
} from "type/api";

export const LOADING_RUN = "@@run-detail/loading";

export interface State {
  job: ReviewJobView | null;
  artifacts: ListArtifactResponse$ArtifactView[];
  outputDir: string | null;
  report: ReviewReportView | null;
  validation: GetReviewReportValidationResponse | null;
  feedback: ListFindingFeedbackResponse$FindingFeedbackView[];
  /** Stage whose captured CLI output is being tailed, if any. */
  logStage: string | null;
  logContent: string;
  logOffset: number;
  logComplete: boolean;
  preview: {name: string; content: string} | null;
  showSources: boolean;
  /** Every NOT_COLLECTED marker found in the collected metrics, with the reason given. */
  notCollected: string[];
}
