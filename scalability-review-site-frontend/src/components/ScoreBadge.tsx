import {Tooltip} from "antd";
import {SCORE_BACKGROUNDS, SCORE_COLORS} from "@/constants/review";
import type {Score} from "type/api";

const UNKNOWN_HINT =
  "No score. The synthesis stage writes report-snapshot.json with the dimension scores; without it " +
  "the score is unknown, and an unknown score is never promoted to green.";

/**
 * A score badge always carries its word as well as its colour — the three states have to stay
 * distinguishable without colour vision, and RED/YELLOW/GREEN are the whole point of the report.
 */
export const ScoreBadge = ({score, label}: {score: Score | null | undefined; label?: string}) => {
  const value: Score = score ?? "UNKNOWN";
  const badge = (
    <span
      style={{
        display: "inline-flex",
        alignItems: "center",
        gap: 6,
        padding: "1px 10px",
        borderRadius: 12,
        fontSize: 12,
        fontWeight: 600,
        color: SCORE_COLORS[value],
        background: SCORE_BACKGROUNDS[value],
        border: `1px solid ${SCORE_COLORS[value]}33`,
      }}
    >
      {label ? <span style={{fontWeight: 400, color: "#595959"}}>{label}</span> : null}
      {value}
    </span>
  );
  return value === "UNKNOWN" ? <Tooltip title={UNKNOWN_HINT}>{badge}</Tooltip> : badge;
};
