import {Alert, Card, Descriptions, Progress, Space, Table, Tag, Tooltip, Typography} from "antd";
import type {ColumnsType} from "antd/es/table";
import {STAGE_LABELS} from "@/constants/review";
import {formatCost, formatDuration, formatPercent, formatTokens} from "@/utils/format";
import type {ReviewJobView} from "type/api";

/**
 * What the run spent (FR-7).
 *
 * Cached input is its own column rather than folded into the input total: in the pilot run most of
 * the validation stage's input came from cache, and a cost that ignores that is wrong by a
 * multiple, not by a rounding error.
 */
export const CostPanel = ({job}: {job: ReviewJobView}) => {
  const totals = job.totals;
  const stages = (job.stages ?? []).filter(stage => (stage.total_tokens ?? 0) > 0 || stage.duration_ms);
  const maxTokens = Math.max(1, ...stages.map(stage => stage.total_tokens ?? 0));
  const unpriced = totals?.price_table_version === "unconfigured";

  const columns: ColumnsType<(typeof stages)[number]> = [
    {title: "Stage", dataIndex: "name", render: value => STAGE_LABELS[value] ?? value},
    {title: "Model", dataIndex: "model", render: value => value ?? "—"},
    {title: "Duration", align: "right", render: (_, stage) => formatDuration(stage.duration_ms)},
    {title: "Input", align: "right", render: (_, stage) => formatTokens(stage.input_tokens)},
    {title: "Output", align: "right", render: (_, stage) => formatTokens(stage.output_tokens)},
    {title: "Cached", align: "right", render: (_, stage) => formatTokens(stage.cached_tokens)},
    {title: "API", align: "right", render: (_, stage) => stage.api_requests ?? "—"},
    {title: "Tools", align: "right", render: (_, stage) => stage.tool_calls ?? "—"},
    {title: "MCP", align: "right", render: (_, stage) => stage.mcp_tool_calls ?? "—"},
    {title: "Cost", align: "right", render: (_, stage) => formatCost(stage.estimated_cost_usd)},
    {
      title: "Share of tokens",
      width: 180,
      render: (_, stage) => (
        <Progress percent={Math.round(((stage.total_tokens ?? 0) / maxTokens) * 100)} size="small" showInfo={false} />
      ),
    },
  ];

  return (
    <Space direction="vertical" size={12} style={{width: "100%"}}>
      {unpriced ? (
        <Alert
          type="info"
          showIcon
          message="The price table is not configured"
          description="Token counts are exact; the dollar figures stay blank until the per-model rates are filled in on the Settings page. An invented rate would read as fact."
        />
      ) : null}

      <Descriptions size="small" bordered column={4}>
        <Descriptions.Item label="Total tokens">
          <Space>
            {formatTokens(totals?.total_tokens)}
            {totals?.within_budget === false ? (
              <Tooltip
                title={`Budget is ${formatTokens(totals?.token_budget)} tokens / ${formatDuration(totals?.duration_budget_ms)}`}
              >
                <Tag color="warning">over budget</Tag>
              </Tooltip>
            ) : (
              <Tag color="success">within budget</Tag>
            )}
          </Space>
        </Descriptions.Item>
        <Descriptions.Item label="Input">{formatTokens(totals?.input_tokens)}</Descriptions.Item>
        <Descriptions.Item label="Output">{formatTokens(totals?.output_tokens)}</Descriptions.Item>
        <Descriptions.Item label="Cached">
          <Space>
            {formatTokens(totals?.cached_tokens)}
            <Typography.Text type="secondary">{formatPercent(totals?.cache_hit_ratio)} hit</Typography.Text>
          </Space>
        </Descriptions.Item>
        <Descriptions.Item label="Wall clock">{formatDuration(totals?.duration_ms)}</Descriptions.Item>
        <Descriptions.Item label="MCP calls">{totals?.mcp_tool_calls ?? "—"}</Descriptions.Item>
        <Descriptions.Item label="Estimated cost">{formatCost(totals?.estimated_cost_usd)}</Descriptions.Item>
        <Descriptions.Item label="Price table">{totals?.price_table_version ?? "—"}</Descriptions.Item>
      </Descriptions>

      <Card size="small" title="Per stage">
        <Table rowKey="name" size="small" pagination={false} columns={columns} dataSource={stages} />
      </Card>
    </Space>
  );
};
