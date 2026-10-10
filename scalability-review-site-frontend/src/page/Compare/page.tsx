import {ArrowDownOutlined, ArrowUpOutlined, MinusOutlined} from "@ant-design/icons";
import {Card, Col, Empty, Row, Select, Space, Table, Tag, Typography} from "antd";
import {useLoadingStatus, useLocation} from "@wonder/core-fe";
import {useEffect} from "react";
import {ScoreBadge} from "@/components/ScoreBadge";
import {formatDelta, formatShortDateTime} from "@/utils/format";
import {useRuntimeState} from "@/utils/useRuntimeState";
import type {CompareRunResponse$MetricDiffView} from "type/api";
import {actions} from "./module";
import {LOADING_COMPARE} from "./type";

const CHANGE_COLORS: Record<string, string> = {
  IMPROVED: "success",
  DEGRADED: "error",
  UNCHANGED: "default",
  UNKNOWN: "default",
};

/** Colour follows the metric's own direction, so "lower is better" is not read as a regression. */
const deltaIcon = (diff: CompareRunResponse$MetricDiffView) => {
  if (diff.delta === null || diff.delta === undefined || diff.delta === 0) return <MinusOutlined />;
  const worse = diff.higher_is_worse ? diff.delta > 0 : diff.delta < 0;
  const color = worse ? "#cf1322" : "#389e0d";
  return diff.delta > 0 ? <ArrowUpOutlined style={{color}} /> : <ArrowDownOutlined style={{color}} />;
};

const metricColumns = [
  {title: "Metric", dataIndex: "metric"},
  {title: "Unit", dataIndex: "unit", width: 80, render: (value: string | null) => value ?? "—"},
  {
    title: "Run A",
    width: 130,
    align: "right" as const,
    render: (_: unknown, diff: CompareRunResponse$MetricDiffView) =>
      diff.value_a === null || diff.value_a === undefined ? "—" : diff.value_a.toLocaleString(),
  },
  {
    title: "Run B",
    width: 130,
    align: "right" as const,
    render: (_: unknown, diff: CompareRunResponse$MetricDiffView) =>
      diff.value_b === null || diff.value_b === undefined ? "—" : diff.value_b.toLocaleString(),
  },
  {
    title: "Change",
    width: 180,
    render: (_: unknown, diff: CompareRunResponse$MetricDiffView) => (
      <Space size={4}>
        {deltaIcon(diff)}
        <span>{formatDelta(diff.delta)}</span>
        {diff.delta_pct !== null && diff.delta_pct !== undefined ? (
          <Typography.Text type="secondary">({formatDelta(diff.delta_pct, 1)}%)</Typography.Text>
        ) : null}
      </Space>
    ),
  },
];

export default function Compare() {
  const {jobIdA, jobIdB, candidates, result} = useRuntimeState("Compare");
  const loading = useLoadingStatus(LOADING_COMPARE);
  const location = useLocation();

  useEffect(() => {
    const params = new URLSearchParams(location.search);
    void actions.start(params.get("a"), params.get("b"));
  }, []);

  const options = candidates.map(job => ({
    value: job.job_id,
    label: `${job.service_id} · ${formatShortDateTime(job.started_at)} · ${job.overall_score ?? "UNKNOWN"}`,
  }));

  return (
    <Space direction="vertical" size={16} style={{width: "100%"}}>
      <Card size="small">
        <Row gutter={16}>
          <Col span={12}>
            <Typography.Text type="secondary">Run A (baseline)</Typography.Text>
            <Select
              showSearch
              style={{width: "100%"}}
              value={jobIdA ?? undefined}
              options={options}
              optionFilterProp="label"
              onChange={value => void actions.select("a", value)}
            />
          </Col>
          <Col span={12}>
            <Typography.Text type="secondary">Run B (compared against A)</Typography.Text>
            <Select
              showSearch
              style={{width: "100%"}}
              value={jobIdB ?? undefined}
              options={options}
              optionFilterProp="label"
              onChange={value => void actions.select("b", value)}
            />
          </Col>
        </Row>
      </Card>

      {!result ? (
        <Empty description="Pick two completed runs of the same service" />
      ) : (
        <Space direction="vertical" size={16} style={{width: "100%"}}>
          <Card
            size="small"
            title={`${result.service_id}: ${result.run_a.label} → ${result.run_b.label}`}
            loading={loading}
          >
            <Table
              rowKey="dimension"
              size="small"
              pagination={false}
              dataSource={result.score_diffs}
              columns={[
                {title: "Dimension", dataIndex: "dimension"},
                {title: "Run A", width: 140, render: (_, diff) => <ScoreBadge score={diff.score_a} />},
                {title: "Run B", width: 140, render: (_, diff) => <ScoreBadge score={diff.score_b} />},
                {
                  title: "Change",
                  width: 140,
                  render: (_, diff) => <Tag color={CHANGE_COLORS[diff.change]}>{diff.change}</Tag>,
                },
              ]}
            />
          </Card>

          <Card size="small" title="Key metrics">
            <Table
              rowKey="metric"
              size="small"
              pagination={false}
              dataSource={result.metric_diffs}
              columns={metricColumns}
            />
          </Card>

          <Row gutter={16}>
            <Col span={12}>
              <Card size="small" title="Recommendations">
                <Table
                  rowKey="metric"
                  size="small"
                  pagination={false}
                  dataSource={result.recommendation_diffs}
                  columns={metricColumns}
                />
              </Card>
            </Col>
            <Col span={12}>
              <Card size="small" title="Cost and latency">
                <Table
                  rowKey="metric"
                  size="small"
                  pagination={false}
                  dataSource={result.cost_diffs}
                  columns={metricColumns}
                />
              </Card>
            </Col>
          </Row>

          <Card
            size="small"
            title="Infrastructure drift"
            extra={
              <Typography.Text type="secondary" style={{fontSize: 12}}>
                A line-level diff of what the pipeline actually read — independent of what either report said.
              </Typography.Text>
            }
          >
            {result.config_drift.length === 0 ? (
              <Typography.Text type="secondary">No difference between the two runs' infra context.</Typography.Text>
            ) : (
              <Table
                rowKey="key"
                size="small"
                pagination={false}
                dataSource={result.config_drift}
                columns={[
                  {title: "File", dataIndex: "key"},
                  {title: "Run A", dataIndex: "value_a", render: value => <code style={{fontSize: 12}}>{value}</code>},
                  {title: "Run B", dataIndex: "value_b", render: value => <code style={{fontSize: 12}}>{value}</code>},
                ]}
              />
            )}
          </Card>
        </Space>
      )}
    </Space>
  );
}
