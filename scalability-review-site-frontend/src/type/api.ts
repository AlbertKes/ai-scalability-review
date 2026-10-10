/**
 * Mirrors ai-scalability-review-service-interface.
 *
 * Normally this file is generated — `core-fe api --api-url http://localhost:8080/_sys/api` against a
 * running backend rewrites it. It is written by hand here so the console can be built before the
 * service is up; regenerate it rather than editing it once an endpoint changes.
 */

// ---------------------------------------------------------------- local review

export interface LocalReviewRequestView {
  service: string | null;
  local_app_repo_path: string | null;
  local_infra_repo_path: string | null;
  env: string | null;
  namespace: string | null;
  domain: string | null;
  mysql_host: string | null;
  mysql_db: string | null;
  atlas_cluster: string | null;
  azure_resource_group: string | null;
  azure_subscription_id: string | null;
  hpa_type: string | null;
  kafka_consumer_groups: string | null;
  skip_code_analysis: boolean | null;
  skip_validation: boolean | null;
  app_code_paths: string[] | null;
  code_analysis_model: string | null;
  metric_collection_model: string | null;
  synthesis_model: string | null;
  validation_model: string | null;
  output_base_dir: string | null;
  operator: string | null;
}

export interface GenerateLocalReviewRequest extends LocalReviewRequestView {
  labels: string[] | null;
  acknowledged_warnings: string[] | null;
}

export interface GenerateLocalReviewResponse {
  job_id: string;
  output_dir: string;
}

export type IssueSeverity = "ERROR" | "WARNING";

export interface ValidateLocalReviewResponse$IssueView {
  field: string | null;
  code: string;
  severity: IssueSeverity;
  message: string;
  hint: string | null;
}

export interface ValidateLocalReviewResponse {
  valid: boolean;
  errors: ValidateLocalReviewResponse$IssueView[];
  warnings: ValidateLocalReviewResponse$IssueView[];
}

export interface LocalReviewJobActionResponse {
  job_id: string;
  status: string;
  message: string;
}

export interface RetryLocalReviewRequest {
  from_stage: string | null;
  operator: string;
}

export interface UpdateLocalReviewJobRequest {
  note?: string | null;
  labels?: string[] | null;
  baseline?: boolean | null;
}

export interface PreflightRequest {
  service?: string | null;
  env?: string | null;
  domain?: string | null;
  local_app_repo_path?: string | null;
  local_infra_repo_path?: string | null;
  output_base_dir?: string | null;
  mysql_host?: string | null;
  mysql_db?: string | null;
}

export type PreflightStatus = "PASS" | "WARN" | "FAIL";

export interface PreflightResponse$CheckView {
  id: string;
  name: string;
  category: string;
  status: PreflightStatus;
  detail: string | null;
  hint: string | null;
}

export interface PreflightResponse {
  ok: boolean;
  checks: PreflightResponse$CheckView[];
}

// ---------------------------------------------------------------- review jobs

export type RunStatus = "PENDING" | "QUEUED" | "RUNNING" | "COMPLETED" | "FAILED" | "CANCELLED" | "STALE";
export type StageStatus = "PENDING" | "RUNNING" | "COMPLETED" | "FAILED" | "SKIPPED" | "CANCELLED";
export type Score = "GREEN" | "YELLOW" | "RED" | "UNKNOWN";

export interface ReviewJobView$StageView {
  name: string;
  status: StageStatus;
  started_at: string | null;
  completed_at: string | null;
  duration_ms: number | null;
  error: string | null;
  error_hint: string | null;
  output_summary: string | null;
  model: string | null;
  input_tokens: number | null;
  output_tokens: number | null;
  cached_tokens: number | null;
  total_tokens: number | null;
  api_requests: number | null;
  tool_calls: number | null;
  mcp_tool_calls: number | null;
  estimated_cost_usd: number | null;
  artifacts: string[] | null;
  log_file: string | null;
  session_id: string | null;
}

export interface ReviewJobView$RunTotalsView {
  input_tokens: number | null;
  output_tokens: number | null;
  cached_tokens: number | null;
  total_tokens: number | null;
  duration_ms: number | null;
  mcp_tool_calls: number | null;
  estimated_cost_usd: number | null;
  within_budget: boolean | null;
  token_budget: number | null;
  duration_budget_ms: number | null;
  cache_hit_ratio: number | null;
  price_table_version: string | null;
}

export interface ReviewJobView$FingerprintView {
  cli_version: string | null;
  prompt_version: string | null;
  repo_sha_app: string | null;
  repo_sha_infra: string | null;
  repo_dirty_app: boolean | null;
  repo_dirty_infra: boolean | null;
  azure_resource_group: string | null;
}

export interface ReviewJobView {
  job_id: string;
  service_id: string;
  status: RunStatus;
  current_stage: string | null;
  ai_model: string | null;
  trigger_type: string | null;
  triggered_by: string | null;
  note: string | null;
  labels: string[] | null;
  baseline: boolean | null;
  started_at: string | null;
  completed_at: string | null;
  cancelled_at: string | null;
  cancelled_by: string | null;
  report_id: string | null;
  error_message: string | null;
  env: string | null;
  namespace: string | null;
  domain: string | null;
  output_dir: string | null;
  request: LocalReviewRequestView | null;
  acknowledged_warnings: string[] | null;
  totals: ReviewJobView$RunTotalsView | null;
  fingerprint: ReviewJobView$FingerprintView | null;
  overall_score: Score | null;
  validation_verdict: string | null;
  validation_result_id: string | null;
  stages: ReviewJobView$StageView[] | null;
}

export interface GetReviewJobResponse {
  view: ReviewJobView;
}

export interface ListReviewJobRequest {
  service_id?: string | null;
  status?: string | null;
  env?: string | null;
  operator?: string | null;
  trigger_type?: string | null;
  keyword?: string | null;
  search_reports?: boolean | null;
  from?: string | null;
  to?: string | null;
  overall_score?: string | null;
  validation_verdict?: string | null;
  within_budget?: boolean | null;
  has_failed_stage?: boolean | null;
  model?: string | null;
  label?: string | null;
  sort?: string | null;
  limit: number;
  skip: number;
}

export interface ListReviewJobResponse {
  jobs: ReviewJobView[];
  total: number;
}

// ---------------------------------------------------------------- artifacts

export interface ListArtifactResponse$ArtifactView {
  name: string;
  stage: string | null;
  kind: "MARKDOWN" | "JSON" | "LOG" | "OTHER";
  size_bytes: number | null;
  modified_at: string | null;
}

export interface ListArtifactResponse {
  output_dir: string | null;
  artifacts: ListArtifactResponse$ArtifactView[];
}

export interface GetStageLogRequest {
  stage: string;
  offset: number;
}

export interface GetStageLogResponse {
  stage: string;
  content: string;
  next_offset: number;
  size: number;
  complete: boolean;
}

// ---------------------------------------------------------------- comparison

export interface CompareRunRequest {
  job_id_a: string;
  job_id_b: string;
}

export interface CompareRunResponse$RunRefView {
  job_id: string;
  report_id: string | null;
  label: string;
  env: string | null;
  overall_score: Score | null;
  validation_verdict: string | null;
}

export interface CompareRunResponse$ScoreDiffView {
  dimension: string;
  score_a: Score | null;
  score_b: Score | null;
  change: "IMPROVED" | "DEGRADED" | "UNCHANGED" | "UNKNOWN";
}

export interface CompareRunResponse$MetricDiffView {
  metric: string;
  unit: string | null;
  value_a: number | null;
  value_b: number | null;
  delta: number | null;
  delta_pct: number | null;
  higher_is_worse: boolean | null;
}

export interface CompareRunResponse$ConfigDriftView {
  key: string;
  value_a: string | null;
  value_b: string | null;
}

export interface CompareRunResponse {
  service_id: string;
  run_a: CompareRunResponse$RunRefView;
  run_b: CompareRunResponse$RunRefView;
  score_diffs: CompareRunResponse$ScoreDiffView[];
  metric_diffs: CompareRunResponse$MetricDiffView[];
  recommendation_diffs: CompareRunResponse$MetricDiffView[];
  cost_diffs: CompareRunResponse$MetricDiffView[];
  config_drift: CompareRunResponse$ConfigDriftView[];
}

// ---------------------------------------------------------------- usage

export interface GetUsageRequest {
  from?: string | null;
  to?: string | null;
  group_by?: string | null;
  service_id?: string | null;
}

export interface GetUsageResponse$UsageBucketView {
  key: string;
  runs: number;
  input_tokens: number;
  output_tokens: number;
  cached_tokens: number;
  total_tokens: number;
  duration_ms: number;
  mcp_tool_calls: number;
  api_requests: number;
  cost_usd: number | null;
}

export interface GetUsageResponse$UsageRunView {
  job_id: string;
  service_id: string;
  env: string | null;
  started_at: string | null;
  total_tokens: number | null;
  duration_ms: number | null;
  cost_usd: number | null;
  within_budget: boolean | null;
}

export interface GetUsageResponse {
  group_by: string;
  runs: number;
  total_tokens: number;
  total_input_tokens: number;
  total_output_tokens: number;
  total_cached_tokens: number;
  total_duration_ms: number;
  total_cost_usd: number | null;
  month_to_date_cost_usd: number | null;
  monthly_ceiling_usd: number | null;
  price_table_version: string | null;
  buckets: GetUsageResponse$UsageBucketView[];
  top_runs: GetUsageResponse$UsageRunView[];
}

// ---------------------------------------------------------------- services

export interface ListServiceSummaryResponse$ServiceSummaryView {
  service_id: string;
  display_name: string | null;
  team: string | null;
  tier: string | null;
  has_config: boolean | null;
  run_count: number;
  last_run_at: string | null;
  last_job_id: string | null;
  last_status: RunStatus | null;
  last_overall_score: Score | null;
  last_validation_verdict: string | null;
  last_env: string | null;
  baseline_job_id: string | null;
  tokens_30d: number | null;
  cost_30d_usd: number | null;
  days_since_last_run: number | null;
}

export interface ListServiceSummaryResponse {
  services: ListServiceSummaryResponse$ServiceSummaryView[];
}

// ---------------------------------------------------------------- reports

export interface ReviewReportView$ScoresView {
  traffic: Score | null;
  latency: Score | null;
  errors: Score | null;
  resources: Score | null;
  persistence: Score | null;
  overall: Score | null;
}

export interface ReviewReportView {
  report_id: string;
  job_id: string | null;
  service_id: string | null;
  period_label: string | null;
  ai_model: string | null;
  report_markdown: string | null;
  confluence_page_url: string | null;
  scores: ReviewReportView$ScoresView | null;
  generated_at: string | null;
}

export interface GetReviewReportResponse {
  view: ReviewReportView;
}

export interface ValidationResultView$CheckView {
  check_number: number | null;
  check_name: string | null;
  status: string | null;
  issue_count: number | null;
}

export interface GetReviewReportValidationResponse {
  validation_id: string;
  report_id: string | null;
  job_id: string | null;
  service_id: string | null;
  status: string | null;
  verdict: string | null;
  checks: ValidationResultView$CheckView[] | null;
  validation_report_markdown: string | null;
  generated_at: string | null;
}

export interface SaveFindingFeedbackRequest {
  job_id: string | null;
  finding_key: string;
  finding_title: string | null;
  verdict: "CONFIRMED" | "FALSE_POSITIVE" | "WONT_FIX";
  comment: string | null;
  author: string;
}

export interface ListFindingFeedbackResponse$FindingFeedbackView {
  feedback_id: string;
  report_id: string | null;
  job_id: string | null;
  finding_key: string | null;
  finding_title: string | null;
  verdict: string | null;
  comment: string | null;
  author: string | null;
  created_at: string | null;
}

export interface ListFindingFeedbackResponse {
  feedback: ListFindingFeedbackResponse$FindingFeedbackView[];
}

// ---------------------------------------------------------------- notifications

export type NotificationSeverity = "SUCCESS" | "WARNING" | "ERROR";

export interface NotificationView {
  notification_id: string;
  job_id: string | null;
  service_id: string | null;
  type: string;
  severity: NotificationSeverity;
  title: string;
  body: string | null;
  created_at: string | null;
  read_at: string | null;
}

export interface ListNotificationRequest {
  unread_only?: boolean | null;
  limit: number;
  skip: number;
}

export interface ListNotificationResponse {
  notifications: NotificationView[];
  unread_count: number;
  total: number;
}

export interface NotificationActionResponse {
  unread_count: number;
  affected: number;
}

// ---------------------------------------------------------------- settings & presets

export interface SettingsView$ModelPriceView {
  model: string;
  input_usd_per_million: number | null;
  cached_input_usd_per_million: number | null;
  output_usd_per_million: number | null;
}

export interface SettingsView$NotificationPrefsView {
  on_completed: boolean | null;
  on_failed: boolean | null;
  on_cancelled: boolean | null;
  on_stage_failed: boolean | null;
  on_over_budget: boolean | null;
  on_validation_failed: boolean | null;
  on_red_score: boolean | null;
  on_stale: boolean | null;
  browser_enabled: boolean | null;
  slack_enabled: boolean | null;
}

export interface SettingsView {
  output_base_dir: string | null;
  default_operator: string | null;
  poll_interval_ms: number | null;
  token_budget: number | null;
  duration_budget_ms: number | null;
  monthly_cost_ceiling_usd: number | null;
  retention_keep_last_n: number | null;
  retention_max_age_days: number | null;
  max_concurrent_runs: number | null;
  price_table_version: string | null;
  model_prices: SettingsView$ModelPriceView[] | null;
  notification_prefs: SettingsView$NotificationPrefsView | null;
  slack_webhook_url: string | null;
  theme: string | null;
  density: string | null;
  updated_at: string | null;
}

export interface PresetView {
  preset_id: string;
  service_id: string;
  env: string | null;
  name: string;
  request: LocalReviewRequestView | null;
  is_default: boolean | null;
  created_by: string | null;
  created_at: string | null;
  updated_at: string | null;
}

export interface ListPresetRequest {
  service_id?: string | null;
  env?: string | null;
}

export interface ListPresetResponse {
  presets: PresetView[];
}

export interface SavePresetRequest {
  preset_id: string | null;
  service_id: string;
  env: string | null;
  name: string;
  request: LocalReviewRequestView;
  is_default: boolean | null;
  created_by: string;
}

export interface PresetActionResponse {
  preset_id: string;
  message: string;
}
