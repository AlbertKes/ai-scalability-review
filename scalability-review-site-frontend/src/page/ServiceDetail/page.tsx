import {Alert, Card, Col, Empty, Row, Space, Table, Tag, Typography} from "antd";
import {useLoadingStatus, useNavigate, useParams} from "@wonder/core-fe";
import {useMemo} from "react";
import {CartesianGrid, Line, LineChart, ResponsiveContainer, Tooltip as ChartTooltip, XAxis, YAxis} from "recharts";
import {ScoreBadge} from "@/components/ScoreBadge";
import {RunStatusTag} from "@/components/StatusTag";
import {COMPARE_PATH, RUN_DETAIL_PATH} from "@/route";
import {formatCost, formatDuration, formatShortDateTime, formatTokens} from "@/utils/format";
import {useRuntimeState} from "@/utils/useRuntimeState";
import type {ReviewJobView, Score} from "type/api";
import {LOADING_SERVICE} from "./type";

const SCORE_VALUES: Record<Score, number | null> = {RED: 1, YELLOW: 2, GREEN: 3, UNKNOWN: null};

export default function ServiceDetail() {
  const {serviceId} = useParams<{serviceId: string}>();
  const {runs} = useRuntimeState("ServiceDetail");
  const loading = useLoadingStatus(LOADING_SERVICE);
  const navigate = useNavigate();

  /**
   * Oldest first so the trend reads left to right, and only completed runs: a failed run has no
   * score, and plotting it as a dip would say the service got worse when the pipeline did.
   */
  const trend = useMemo(
    () =>
      [...runs]
        .filter(run => run.status === "COMPLETED")
        .reverse()
        .map(run => ({
          label: formatShortDateTime(run.started_at),
          score: SCORE_VALUES[run.overall_score ?? "UNKNOWN"],
          tokens: run.totals?.total_tokens ?? null,
          durationMin: run.totals?.duration_ms ? Math.round(run.totals.duration_ms / 600) / 100 : null,
        })),
    [runs]
  );

  const baseline = runs.find(run => run.baseline);

  if (!loading && runs.length === 0) return <Empty description={`No runs for ${serviceId} yet`} />;

  return (
    <Space direction="vertical" size={16} style={{width: "100%"}}>
      <Card size="small" title={serviceId}>
        <Space wrap>
          <Typography.Text type="secondary">{runs.length} run(s)</Typography.Text>
          {baseline ? (
            <Tag color="blue">baseline: {formatShortDateTime(baseline.started_at)}</Tag>
          ) : (
            <Typography.Text type="secondary">no baseline pinned</Typography.Text>
          )}
        </Space>
      </Card>

      {trend.length < 2 ? (
        <Alert
          type="info"
          showIcon
          message="One completed run so far"
          description="Trends appear once this service has been reviewed twice."
        />
      ) : (
        <Row gutter={16}>
          <Col span={12}>
            <Card size="small" title="Overall score (1 RED · 2 YELLOW · 3 GREEN)">
              <ResponsiveContainer width="100%" height={220}>
                <LineChart data={trend}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="label" fontSize={11} />
                  <YAxis domain={[0, 3]} ticks={[1, 2, 3]} fontSize={11} />
                  <ChartTooltip />
                  <Line type="monotone" dataKey="score" stroke="#1677ff" connectNulls />
                </LineChart>
              </ResponsiveContainer>
            </Card>
          </Col>
          <Col span={12}>
            <Card size="small" title="Tokens and wall clock (minutes)">
              <ResponsiveContainer width="100%" height={220}>
                <LineChart data={trend}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="label" fontSize={11} />
                  <YAxis yAxisId="left" fontSize={11} />
                  <YAxis yAxisId="right" orientation="right" fontSize={11} />
                  <ChartTooltip />
                  <Line yAxisId="left" type="monotone" dataKey="tokens" stroke="#722ed1" connectNulls />
                  <Line yAxisId="right" type="monotone" dataKey="durationMin" stroke="#13c2c2" connectNulls />
                </LineChart>
              </ResponsiveContainer>
            </Card>
          </Col>
        </Row>
      )}

      <Card size="small" title="Run history">
        <Table<ReviewJobView>
          rowKey="job_id"
          size="small"
          loading={loading}
          dataSource={runs}
          pagination={false}
          columns={[
            {
              title: "Started",
              width: 150,
              render: (_, run) => (
                <a onClick={() => navigate(`/${RUN_DETAIL_PATH}/${run.job_id}`)}>
                  {formatShortDateTime(run.started_at)}
                </a>
              ),
            },
            {title: "Status", width: 120, render: (_, run) => <RunStatusTag status={run.status} />},
            {title: "Env", dataIndex: "env", width: 80, render: value => value ?? "—"},
            {title: "Score", width: 110, render: (_, run) => <ScoreBadge score={run.overall_score} />},
            {
              title: "Verdict",
              width: 130,
              render: (_, run) => (run.validation_verdict ? <Tag>{run.validation_verdict}</Tag> : <Tag>none</Tag>),
            },
            {title: "Duration", width: 110, render: (_, run) => formatDuration(run.totals?.duration_ms)},
            {title: "Tokens", width: 110, align: "right", render: (_, run) => formatTokens(run.totals?.total_tokens)},
            {title: "Cost", width: 110, align: "right", render: (_, run) => formatCost(run.totals?.estimated_cost_usd)},
            {
              title: "",
              width: 120,
              render: (_, run) =>
                baseline && baseline.job_id !== run.job_id ? (
                  <a onClick={() => navigate(`/${COMPARE_PATH}?a=${baseline.job_id}&b=${run.job_id}`)}>vs baseline</a>
                ) : null,
            },
          ]}
        />
      </Card>
    </Space>
  );
}
