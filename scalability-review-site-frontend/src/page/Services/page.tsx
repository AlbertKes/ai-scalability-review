import {Card, Space, Table, Tag, Tooltip, Typography} from "antd";
import {useLoadingStatus, useNavigate} from "@wonder/core-fe";
import {ScoreBadge} from "@/components/ScoreBadge";
import {RunStatusTag} from "@/components/StatusTag";
import {SERVICES_PATH} from "@/route";
import {formatCost, formatShortDateTime, formatTokens} from "@/utils/format";
import {useRuntimeState} from "@/utils/useRuntimeState";
import type {ListServiceSummaryResponse$ServiceSummaryView} from "type/api";
import {LOADING_SERVICES} from "./type";

const STALE_AFTER_DAYS = 30;

export default function Services() {
  const {services} = useRuntimeState("Services");
  const loading = useLoadingStatus(LOADING_SERVICES);
  const navigate = useNavigate();

  return (
    <Card size="small" title={`${services.length} service(s)`}>
      <Table<ListServiceSummaryResponse$ServiceSummaryView>
        rowKey="service_id"
        size="small"
        loading={loading}
        dataSource={services}
        pagination={false}
        columns={[
          {
            title: "Service",
            dataIndex: "service_id",
            render: (value, row) => (
              <Space direction="vertical" size={0}>
                <a onClick={() => navigate(`/${SERVICES_PATH}/${value}`)}>{value}</a>
                {row.display_name && row.display_name !== value ? (
                  <Typography.Text type="secondary" style={{fontSize: 12}}>
                    {row.display_name}
                  </Typography.Text>
                ) : null}
              </Space>
            ),
          },
          {title: "Team", dataIndex: "team", width: 140, render: value => value ?? "—"},
          {title: "Tier", dataIndex: "tier", width: 100, render: value => (value ? <Tag>{value}</Tag> : "—")},
          {title: "Runs", dataIndex: "run_count", width: 80, align: "right"},
          {
            title: "Last run",
            width: 170,
            render: (_, row) => (
              <Space direction="vertical" size={0}>
                <span>{formatShortDateTime(row.last_run_at)}</span>
                {row.days_since_last_run !== null && row.days_since_last_run > STALE_AFTER_DAYS ? (
                  <Tooltip title="Longer ago than the review cadence this console assumes">
                    <Tag color="warning">{row.days_since_last_run}d ago</Tag>
                  </Tooltip>
                ) : null}
              </Space>
            ),
          },
          {title: "Status", width: 120, render: (_, row) => <RunStatusTag status={row.last_status} />},
          {title: "Score", width: 110, render: (_, row) => <ScoreBadge score={row.last_overall_score} />},
          {
            title: "Verdict",
            width: 130,
            render: (_, row) =>
              row.last_validation_verdict ? (
                <Tag color={row.last_validation_verdict === "PASS" ? "success" : "error"}>
                  {row.last_validation_verdict}
                </Tag>
              ) : (
                <Tag>none</Tag>
              ),
          },
          {title: "Tokens 30d", width: 110, align: "right", render: (_, row) => formatTokens(row.tokens_30d)},
          {title: "Cost 30d", width: 110, align: "right", render: (_, row) => formatCost(row.cost_30d_usd)},
        ]}
      />
    </Card>
  );
}
