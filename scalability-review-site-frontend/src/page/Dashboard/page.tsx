import {CheckCircleTwoTone, CloseCircleTwoTone, WarningTwoTone} from "@ant-design/icons";
import {Card, Col, Empty, List, Progress, Row, Space, Statistic, Tag, Typography} from "antd";
import {useNavigate} from "@wonder/core-fe";
import {ScoreBadge} from "@/components/ScoreBadge";
import {RunStatusTag} from "@/components/StatusTag";
import {runningStageLabel} from "@/constants/review";
import {RUN_DETAIL_PATH} from "@/route";
import {formatCost, formatDuration, formatShortDateTime, formatTokens} from "@/utils/format";
import {useRuntimeState} from "@/utils/useRuntimeState";
import type {PreflightStatus} from "type/api";

const PREFLIGHT_ICONS: Record<PreflightStatus, React.ReactNode> = {
  PASS: <CheckCircleTwoTone twoToneColor="#52c41a" />,
  WARN: <WarningTwoTone twoToneColor="#faad14" />,
  FAIL: <CloseCircleTwoTone twoToneColor="#ff4d4f" />,
};

export default function Dashboard() {
  const {running, recent, overBudget, usage, preflight} = useRuntimeState("Dashboard");
  const navigate = useNavigate();
  const ceiling = usage?.monthly_ceiling_usd ?? null;
  const spent = usage?.month_to_date_cost_usd ?? null;

  return (
    <Space direction="vertical" size={16} style={{width: "100%"}}>
      <Row gutter={16}>
        <Col span={6}>
          <Card size="small">
            <Statistic title="Running now" value={running.length} />
          </Card>
        </Col>
        <Col span={6}>
          <Card size="small">
            <Statistic title="Tokens (recent runs)" value={formatTokens(usage?.total_tokens)} />
          </Card>
        </Col>
        <Col span={6}>
          <Card size="small">
            <Statistic title="Month to date" value={formatCost(spent)} />
            {ceiling ? (
              <Progress
                percent={Math.min(100, Math.round(((spent ?? 0) / ceiling) * 100))}
                size="small"
                status={(spent ?? 0) > ceiling ? "exception" : "normal"}
              />
            ) : (
              <Typography.Text type="secondary" style={{fontSize: 12}}>
                no monthly ceiling set
              </Typography.Text>
            )}
          </Card>
        </Col>
        <Col span={6}>
          <Card size="small">
            <Statistic
              title="Over budget"
              value={overBudget.length}
              valueStyle={{color: overBudget.length > 0 ? "#d4380d" : undefined}}
            />
          </Card>
        </Col>
      </Row>

      <Row gutter={16}>
        <Col span={14}>
          <Card size="small" title="In flight">
            {running.length === 0 ? (
              <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="Nothing running" />
            ) : (
              <List
                size="small"
                dataSource={running}
                renderItem={job => (
                  <List.Item onClick={() => navigate(`/${RUN_DETAIL_PATH}/${job.job_id}`)} style={{cursor: "pointer"}}>
                    <Space>
                      <RunStatusTag status={job.status} />
                      <strong>{job.service_id}</strong>
                      {job.env ? <Tag>{job.env}</Tag> : null}
                      <Typography.Text type="secondary">
                        {runningStageLabel(job.stages, job.current_stage)}
                      </Typography.Text>
                    </Space>
                  </List.Item>
                )}
              />
            )}
          </Card>

          <Card size="small" title="Latest runs" style={{marginTop: 16}}>
            <List
              size="small"
              dataSource={recent}
              renderItem={job => (
                <List.Item onClick={() => navigate(`/${RUN_DETAIL_PATH}/${job.job_id}`)} style={{cursor: "pointer"}}>
                  <Space wrap>
                    <RunStatusTag status={job.status} />
                    <strong>{job.service_id}</strong>
                    {job.env ? <Tag>{job.env}</Tag> : null}
                    <ScoreBadge score={job.overall_score} />
                    <Typography.Text type="secondary">{formatShortDateTime(job.started_at)}</Typography.Text>
                    <Typography.Text type="secondary">{formatDuration(job.totals?.duration_ms)}</Typography.Text>
                    <Typography.Text type="secondary">{formatTokens(job.totals?.total_tokens)} tokens</Typography.Text>
                  </Space>
                </List.Item>
              )}
            />
          </Card>
        </Col>

        <Col span={10}>
          <Card size="small" title="Environment">
            <List
              size="small"
              dataSource={preflight}
              renderItem={check => (
                <List.Item>
                  <Space align="start">
                    {PREFLIGHT_ICONS[check.status]}
                    <Space direction="vertical" size={0}>
                      <span>{check.name}</span>
                      <Typography.Text type="secondary" style={{fontSize: 12}}>
                        {check.detail}
                      </Typography.Text>
                    </Space>
                  </Space>
                </List.Item>
              )}
            />
          </Card>
        </Col>
      </Row>
    </Space>
  );
}
