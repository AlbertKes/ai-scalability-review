import dayjs from "dayjs";

const EMPTY = "—";

/**
 * Formatting rules for the console.
 *
 * The one principle worth stating: a missing value is rendered as missing. A metric that was not
 * collected, a score that was never produced and a cost with no price behind it are all different
 * from zero, and showing them as zero is how a report starts lying.
 */
export const formatNumber = (value: number | null | undefined, fractionDigits = 0): string => {
  if (value === null || value === undefined) return EMPTY;
  return value.toLocaleString(undefined, {
    minimumFractionDigits: fractionDigits,
    maximumFractionDigits: fractionDigits,
  });
};

export const formatTokens = (value: number | null | undefined): string => {
  if (value === null || value === undefined) return EMPTY;
  if (value >= 1_000_000) return `${(value / 1_000_000).toFixed(2)}M`;
  if (value >= 1_000) return `${(value / 1_000).toFixed(1)}K`;
  return String(value);
};

export const formatDuration = (millis: number | null | undefined): string => {
  if (millis === null || millis === undefined) return EMPTY;
  const totalSeconds = Math.round(millis / 1000);
  const minutes = Math.floor(totalSeconds / 60);
  const seconds = totalSeconds % 60;
  if (minutes === 0) return `${seconds}s`;
  return `${minutes}m ${String(seconds).padStart(2, "0")}s`;
};

/** Null means "no price configured for this model", which is not the same as free. */
export const formatCost = (value: number | null | undefined): string => {
  if (value === null || value === undefined) return "not priced";
  return `$${value.toFixed(value < 1 ? 4 : 2)}`;
};

export const formatPercent = (ratio: number | null | undefined): string => {
  if (ratio === null || ratio === undefined) return EMPTY;
  return `${(ratio * 100).toFixed(0)}%`;
};

export const formatDateTime = (value: string | null | undefined): string =>
  value ? dayjs(value).format("YYYY-MM-DD HH:mm:ss") : EMPTY;

export const formatDate = (value: string | null | undefined): string =>
  value ? dayjs(value).format("YYYY-MM-DD") : EMPTY;

export const formatShortDateTime = (value: string | null | undefined): string =>
  value ? dayjs(value).format("MM-DD HH:mm") : EMPTY;

/** The UTC value is shown on hover, because the 28-day collection window is computed in UTC. */
export const utcHint = (value: string | null | undefined): string =>
  value ? `${new Date(value).toISOString()} (UTC)` : "";

export const elapsedSince = (value: string | null | undefined, now: number): string => {
  if (!value) return EMPTY;
  return formatDuration(Math.max(0, now - dayjs(value).valueOf()));
};

export const formatBytes = (bytes: number | null | undefined): string => {
  if (bytes === null || bytes === undefined) return EMPTY;
  if (bytes >= 1024 * 1024) return `${(bytes / 1024 / 1024).toFixed(1)} MB`;
  if (bytes >= 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${bytes} B`;
};

export const formatDelta = (value: number | null | undefined, digits = 2): string => {
  if (value === null || value === undefined) return EMPTY;
  const sign = value > 0 ? "+" : "";
  return `${sign}${value.toFixed(digits)}`;
};

export {EMPTY};
