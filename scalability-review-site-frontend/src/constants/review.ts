import type {RunStatus, Score, StageStatus} from "type/api";

/** The five pipeline stages, in the order the backend runs them. */
export const STAGES = [
  "STAGE_1_CODE_ANALYSIS",
  "STAGE_2_INFRA_CONTEXT",
  "STAGE_3_METRIC_COLLECTION",
  "STAGE_4_SYNTHESIS",
  "STAGE_5_VALIDATION",
] as const;

export const STAGE_LABELS: Record<string, string> = {
  STAGE_1_CODE_ANALYSIS: "Code analysis",
  STAGE_2_INFRA_CONTEXT: "Infra context",
  STAGE_3_METRIC_COLLECTION: "Metric collection",
  STAGE_4_SYNTHESIS: "Synthesis & scoring",
  STAGE_5_VALIDATION: "Validation",
};

/** What each stage reads and writes, shown next to the timeline so the run explains itself. */
export const STAGE_DESCRIPTIONS: Record<string, string> = {
  STAGE_1_CODE_ANALYSIS: "Reads the application source. No MCP server, no live data.",
  STAGE_2_INFRA_CONTEXT:
    "Runs inside the service: selects infra files and queries MySQL table sizes over JDBC. Spends no tokens.",
  STAGE_3_METRIC_COLLECTION:
    "The only stage with MCP servers (Datadog, Azure). Small context, many tool calls, cheapest model.",
  STAGE_4_SYNTHESIS: "Large context, zero tool calls. Writes the report and the score snapshot.",
  STAGE_5_VALIDATION:
    "Independent re-check of the report's data and its advice. Not fatal — a failure still leaves a usable report.",
};

export const RUN_STATUSES: RunStatus[] = ["PENDING", "QUEUED", "RUNNING", "COMPLETED", "FAILED", "CANCELLED", "STALE"];

export const TERMINAL_STATUSES: RunStatus[] = ["COMPLETED", "FAILED", "CANCELLED", "STALE"];

export const isTerminal = (status: RunStatus | null | undefined): boolean =>
  !!status && TERMINAL_STATUSES.includes(status);

export const STATUS_COLORS: Record<RunStatus, string> = {
  PENDING: "default",
  QUEUED: "default",
  RUNNING: "processing",
  COMPLETED: "success",
  FAILED: "error",
  CANCELLED: "warning",
  STALE: "warning",
};

export const STAGE_STATUS_COLORS: Record<StageStatus, string> = {
  PENDING: "default",
  RUNNING: "processing",
  COMPLETED: "success",
  FAILED: "error",
  SKIPPED: "default",
  CANCELLED: "warning",
};

/**
 * Scores never rely on hue alone — every badge carries its word too, so the three states stay
 * distinguishable without colour vision.
 */
export const SCORE_COLORS: Record<Score, string> = {
  GREEN: "#237804",
  YELLOW: "#ad6800",
  RED: "#a8071a",
  UNKNOWN: "#8c8c8c",
};

export const SCORE_BACKGROUNDS: Record<Score, string> = {
  GREEN: "#f6ffed",
  YELLOW: "#fffbe6",
  RED: "#fff1f0",
  UNKNOWN: "#fafafa",
};

export const SCORES: Score[] = ["GREEN", "YELLOW", "RED", "UNKNOWN"];

export const VALIDATION_VERDICTS = ["PASS", "CONDITIONAL_PASS", "FAIL", "UNKNOWN"];

/** Model ids Gemini CLI 0.46 accepts; the backend rejects anything else before a run starts. */
export const GEMINI_MODELS = [
  "gemini-3.5-flash",
  "gemini-3.1-flash-lite",
  "gemini-3.1-pro-preview",
  "gemini-3-pro-preview",
  "gemini-2.5-pro",
  "gemini-2.5-flash",
];

export const STAGE_MODEL_DEFAULTS: Record<string, string> = {
  code_analysis_model: "gemini-3.5-flash",
  metric_collection_model: "gemini-3.1-flash-lite",
  synthesis_model: "gemini-3.5-flash",
  validation_model: "gemini-3.5-flash",
};

/**
 * What the run is doing right now, as a label.
 *
 * Code analysis runs beside the infra and metric stages, so there can be two at once and the job's
 * own `current_stage` is only whichever reported last. The per-stage statuses are the honest answer.
 */
export const runningStageLabel = (
  stages: Array<{name: string; status: StageStatus}> | null | undefined,
  currentStage: string | null | undefined
): string => {
  const running = (stages ?? []).filter(stage => stage.status === "RUNNING");
  if (running.length === 0) return currentStage ? (STAGE_LABELS[currentStage] ?? currentStage) : "starting";
  return running.map(stage => STAGE_LABELS[stage.name] ?? stage.name).join(" + ");
};

export const DEFAULT_POLL_INTERVAL_MS = 3000;
/** Polling slows down rather than stopping when the tab is hidden, to stay off the database. */
export const HIDDEN_POLL_INTERVAL_MS = 15000;
