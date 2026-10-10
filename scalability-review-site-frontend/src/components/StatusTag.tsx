import {Tag, Tooltip} from "antd";
import {STAGE_STATUS_COLORS, STATUS_COLORS} from "@/constants/review";
import type {RunStatus, StageStatus} from "type/api";

const STATUS_HINTS: Partial<Record<RunStatus, string>> = {
  QUEUED: "Waiting for a run slot; another review is in flight.",
  STALE: "The service restarted while this run was going. Nothing is running behind it.",
  CANCELLED: "Stopped by the operator.",
};

export const RunStatusTag = ({status}: {status: RunStatus | null | undefined}) => {
  if (!status) return <Tag>UNKNOWN</Tag>;
  const tag = <Tag color={STATUS_COLORS[status]}>{status}</Tag>;
  const hint = STATUS_HINTS[status];
  return hint ? <Tooltip title={hint}>{tag}</Tooltip> : tag;
};

export const StageStatusTag = ({status}: {status: StageStatus | null | undefined}) => {
  if (!status) return <Tag>PENDING</Tag>;
  return <Tag color={STAGE_STATUS_COLORS[status]}>{status}</Tag>;
};
