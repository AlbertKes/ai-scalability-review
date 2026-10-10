import {FileTextOutlined, ProfileOutlined} from "@ant-design/icons";
import {Alert, Button, Card, Space, Tag, Tooltip, Typography} from "antd";
import {STAGE_DESCRIPTIONS, STAGE_LABELS} from "@/constants/review";
import {StageStatusTag} from "@/components/StatusTag";
import {formatCost, formatDuration, formatTokens} from "@/utils/format";
import type {ReviewJobView$StageView} from "type/api";

interface Props {
  stages: ReviewJobView$StageView[];
  logStage: string | null;
  logContent: string;
  logComplete: boolean;
  onSelectLog: (stage: string | null) => void;
  onPreview: (name: string) => void;
  onRetryFrom: (stage: string) => void;
  canRetry: boolean;
}

/**
 * The run as its five stages (FR-3.1).
 *
 * Every stage is always shown, including the ones that have not started: a timeline that grows a
 * row at a time cannot tell the reader what is still to come.
 */
export const StageTimeline = ({
  stages,
  logStage,
  logContent,
  logComplete,
  onSelectLog,
  onPreview,
  onRetryFrom,
  canRetry,
}: Props) => (
  <Space direction="vertical" size={10} style={{width: "100%"}}>
    {stages.map((stage, index) => (
      <Card key={stage.name} size="small" styles={{body: {padding: "12px 16px"}}}>
        <Space direction="vertical" size={6} style={{width: "100%"}}>
          <Space align="center" wrap>
            <Typography.Text strong>
              {index + 1}. {STAGE_LABELS[stage.name] ?? stage.name}
            </Typography.Text>
            <StageStatusTag status={stage.status} />
            {stage.model ? <Tag>{stage.model}</Tag> : null}
            {stage.duration_ms !== null ? <Tag>{formatDuration(stage.duration_ms)}</Tag> : null}
            {stage.total_tokens ? (
              <Tooltip
                title={`in ${formatTokens(stage.input_tokens)} · out ${formatTokens(
                  stage.output_tokens
                )} · cached ${formatTokens(stage.cached_tokens)}`}
              >
                <Tag>{formatTokens(stage.total_tokens)} tokens</Tag>
              </Tooltip>
            ) : null}
            {stage.mcp_tool_calls ? <Tag>{stage.mcp_tool_calls} MCP calls</Tag> : null}
            {stage.estimated_cost_usd !== null && stage.estimated_cost_usd !== undefined ? (
              <Tag>{formatCost(stage.estimated_cost_usd)}</Tag>
            ) : null}
          </Space>

          <Typography.Text type="secondary" style={{fontSize: 12}}>
            {STAGE_DESCRIPTIONS[stage.name] ?? ""}
          </Typography.Text>

          {stage.output_summary ? (
            <Typography.Text type="secondary" style={{fontSize: 12}}>
              {stage.output_summary}
            </Typography.Text>
          ) : null}

          {stage.error ? (
            <Alert
              type={stage.status === "CANCELLED" ? "warning" : "error"}
              showIcon
              message={stage.error}
              description={stage.error_hint ?? undefined}
            />
          ) : null}

          <Space size={6} wrap>
            {(stage.artifacts ?? []).map(artifact => (
              <Button key={artifact} size="small" icon={<FileTextOutlined />} onClick={() => onPreview(artifact)}>
                {artifact}
              </Button>
            ))}
            {stage.log_file ? (
              <Button
                size="small"
                icon={<ProfileOutlined />}
                type={logStage === stage.name ? "primary" : "default"}
                onClick={() => onSelectLog(logStage === stage.name ? null : stage.name)}
              >
                console
              </Button>
            ) : null}
            {canRetry && stage.name !== "STAGE_1_CODE_ANALYSIS" ? (
              <Tooltip title="Re-run from this stage, reusing the artifacts of the stages before it">
                <Button size="small" type="link" onClick={() => onRetryFrom(stage.name)}>
                  retry from here
                </Button>
              </Tooltip>
            ) : null}
            {stage.session_id ? (
              <Typography.Text type="secondary" style={{fontSize: 11}}>
                session {stage.session_id.slice(0, 8)}
              </Typography.Text>
            ) : null}
          </Space>

          {logStage === stage.name ? (
            <pre
              style={{
                margin: 0,
                maxHeight: 280,
                overflow: "auto",
                background: "#141414",
                color: "#d9d9d9",
                padding: 12,
                borderRadius: 6,
                fontSize: 12,
                whiteSpace: "pre-wrap",
              }}
            >
              {logContent || "(nothing captured yet)"}
              {logComplete ? "" : "\n…"}
            </pre>
          ) : null}
        </Space>
      </Card>
    ))}
  </Space>
);
