import {Alert, Card, Col, Input, Row, Segmented, Select, Space, Statistic, Table, Tag} from "antd";
import {useLoadingStatus, useNavigate} from "@wonder/core-fe";
import {
  Bar,
  BarChart,
  CartesianGrid,
  Legend,
  ResponsiveContainer,
  Tooltip as ChartTooltip,
  XAxis,
  YAxis,
} from "recharts";
import {STAGE_LABELS} from "@/constants/review";
import {RUN_DETAIL_PATH} from "@/route";
import {formatCost, formatDuration, formatShortDateTime, formatTokens} from "@/utils/format";
import {useRuntimeState} from "@/utils/useRuntimeState";
import {actions} from "./module";
import {LOADING_USAGE} from "./type";

const GROUPS = [
  {value: "stage", label: "By stage"},
  {value: "model", label: "By model"},
  {value: "service", label: "By service"},
  {value: "day", label: "By day"},
];

export default function Usage() {
  const {groupBy, serviceId, days, usage} = useRuntimeState("Usage");
  const loading = useLoadingStatus(LOADING_USAGE);
  const navigate = useNavigate();

  const chartData = (usage?.buckets ?? []).map(bucket => ({
    key: groupBy === "stage" ? (STAGE_LABELS[bucket.key] ?? bucket.key) : bucket.key,
    input: bucket.input_tokens - bucket.cached_tokens,
    cached: bucket.cached_tokens,
    output: bucket.output_tokens,
  }));

  const unpriced = usage?.price_table_version === "unconfigured";

  return (
    <Space direction="vertical" size={16} style={{width: "100%"}}>
      <Card size="small">
        <Space wrap>
          <Segmented options={GROUPS} value={groupBy} onChange={value => void actions.setGroupBy(String(value))} />
          <Select
            style={{width: 130}}
            value={days}
            options={[7, 30, 90, 365].map(value => ({value, label: `last ${value}d`}))}
            onChange={value => void actions.setDays(value)}
          />
          <Input
            allowClear
            placeholder="service (all)"
            style={{width: 220}}
            defaultValue={serviceId}
            onPressEnter={event => void actions.setService((event.target as HTMLInputElement).value)}
          />
        </Space>
      </Card>

      {unpriced ? (
        <Alert
          type="info"
          showIcon
          message="Costs are blank because the price table is not configured"
          description="Token counts below are exact. Fill in the per-model rates on the Settings page to turn them into money; this console will not invent a rate."
        />
      ) : null}

      <Row gutter={16}>
        <Col span={6}>
          <Card size="small">
            <Statistic title="Runs" value={usage?.runs ?? 0} />
          </Card>
        </Col>
        <Col span={6}>
          <Card size="small">
            <Statistic title="Total tokens" value={formatTokens(usage?.total_tokens)} />
          </Card>
        </Col>
        <Col span={6}>
          <Card size="small">
            <Statistic title="Cached" value={formatTokens(usage?.total_cached_tokens)} />
          </Card>
        </Col>
        <Col span={6}>
          <Card size="small">
            <Statistic title="Estimated cost" value={formatCost(usage?.total_cost_usd)} />
          </Card>
        </Col>
      </Row>

      <Card size="small" title="Token spend">
        <ResponsiveContainer width="100%" height={300}>
          <BarChart data={chartData}>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="key" fontSize={11} />
            <YAxis fontSize={11} />
            <ChartTooltip />
            <Legend />
            <Bar dataKey="input" stackId="tokens" name="input (billed)" fill="#1677ff" />
            <Bar dataKey="cached" stackId="tokens" name="input (cached)" fill="#91caff" />
            <Bar dataKey="output" stackId="tokens" name="output" fill="#722ed1" />
          </BarChart>
        </ResponsiveContainer>
      </Card>

      <Card size="small" title="Breakdown">
        <Table
          rowKey="key"
          size="small"
          loading={loading}
          pagination={false}
          dataSource={usage?.buckets ?? []}
          columns={[
            {
              title: groupBy,
              dataIndex: "key",
              render: value => (groupBy === "stage" ? (STAGE_LABELS[value] ?? value) : value),
            },
            {title: "Runs", dataIndex: "runs", width: 90, align: "right"},
            {title: "Input", width: 110, align: "right", render: (_, row) => formatTokens(row.input_tokens)},
            {title: "Cached", width: 110, align: "right", render: (_, row) => formatTokens(row.cached_tokens)},
            {title: "Output", width: 110, align: "right", render: (_, row) => formatTokens(row.output_tokens)},
            {title: "Duration", width: 120, align: "right", render: (_, row) => formatDuration(row.duration_ms)},
            {title: "API calls", dataIndex: "api_requests", width: 100, align: "right"},
            {title: "MCP calls", dataIndex: "mcp_tool_calls", width: 110, align: "right"},
            {
              title: "Tokens per MCP call",
              width: 170,
              align: "right",
              render: (_, row) =>
                row.mcp_tool_calls > 0 ? formatTokens(Math.round(row.total_tokens / row.mcp_tool_calls)) : "—",
            },
            {title: "Cost", width: 110, align: "right", render: (_, row) => formatCost(row.cost_usd)},
          ]}
        />
      </Card>

      <Card size="small" title="Most expensive runs">
        <Table
          rowKey="job_id"
          size="small"
          pagination={false}
          dataSource={usage?.top_runs ?? []}
          columns={[
            {
              title: "Run",
              render: (_, row) => <a onClick={() => navigate(`/${RUN_DETAIL_PATH}/${row.job_id}`)}>{row.service_id}</a>,
            },
            {title: "Env", dataIndex: "env", width: 90, render: value => value ?? "—"},
            {title: "Started", width: 150, render: (_, row) => formatShortDateTime(row.started_at)},
            {title: "Tokens", width: 110, align: "right", render: (_, row) => formatTokens(row.total_tokens)},
            {title: "Duration", width: 110, align: "right", render: (_, row) => formatDuration(row.duration_ms)},
            {title: "Cost", width: 110, align: "right", render: (_, row) => formatCost(row.cost_usd)},
            {
              title: "Budget",
              width: 120,
              render: (_, row) =>
                row.within_budget === false ? <Tag color="warning">over</Tag> : <Tag color="success">within</Tag>,
            },
          ]}
        />
      </Card>
    </Space>
  );
}
