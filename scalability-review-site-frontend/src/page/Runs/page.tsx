import {DeleteOutlined, DownloadOutlined, ReloadOutlined, SwapOutlined} from "@ant-design/icons";
import {Button, Card, Checkbox, Input, Modal, Select, Space, Table, Tag, Tooltip, Typography} from "antd";
import type {ColumnsType} from "antd/es/table";
import {useLoadingStatus, useLocation, useNavigate} from "@wonder/core-fe";
import {useEffect, useState} from "react";
import {ScoreBadge} from "@/components/ScoreBadge";
import {RunStatusTag} from "@/components/StatusTag";
import {runningStageLabel, RUN_STATUSES, SCORES, VALIDATION_VERDICTS} from "@/constants/review";
import {COMPARE_PATH, RUN_DETAIL_PATH} from "@/route";
import {downloadCsv} from "@/utils/csv";
import {formatCost, formatDuration, formatShortDateTime, formatTokens} from "@/utils/format";
import {useRuntimeState} from "@/utils/useRuntimeState";
import type {ReviewJobView} from "type/api";
import {actions} from "./module";
import {emptyFilter} from "./state";
import {LOADING_RUNS, type RunFilter} from "./type";

const FILTER_KEYS: Array<keyof RunFilter> = [
  "service_id",
  "status",
  "env",
  "operator",
  "trigger_type",
  "keyword",
  "search_reports",
  "overall_score",
  "validation_verdict",
  "within_budget",
  "has_failed_stage",
  "label",
];

/** The filter lives in the URL so a filtered view can be bookmarked and shared (FR-2.2). */
const toSearch = (filter: RunFilter): string => {
  const params = new URLSearchParams();
  FILTER_KEYS.forEach(key => {
    const value = filter[key];
    if (Array.isArray(value) && value.length > 0) params.set(key, value.join(","));
    else if (typeof value === "boolean" && value) params.set(key, "true");
    else if (typeof value === "string" && value) params.set(key, value);
  });
  return params.toString();
};

const fromSearch = (search: string): RunFilter => {
  const params = new URLSearchParams(search);
  const filter = emptyFilter();
  filter.service_id = params.get("service_id") ?? "";
  filter.env = params.get("env") ?? "";
  filter.operator = params.get("operator") ?? "";
  filter.trigger_type = params.get("trigger_type") ?? "";
  filter.keyword = params.get("keyword") ?? "";
  filter.label = params.get("label") ?? "";
  filter.search_reports = params.get("search_reports") === "true";
  filter.has_failed_stage = params.get("has_failed_stage") === "true";
  filter.status = params.get("status")?.split(",").filter(Boolean) ?? [];
  filter.overall_score = params.get("overall_score")?.split(",").filter(Boolean) ?? [];
  filter.validation_verdict = params.get("validation_verdict")?.split(",").filter(Boolean) ?? [];
  const budget = params.get("within_budget");
  filter.within_budget = budget === null ? null : budget === "true";
  return filter;
};

export default function Runs() {
  const {jobs, total, filter, page, pageSize, selectedJobIds, savedFilters} = useRuntimeState("Runs");
  const loading = useLoadingStatus(LOADING_RUNS);
  const navigate = useNavigate();
  const location = useLocation();
  const [draft, setDraft] = useState<RunFilter>(filter);
  const [saveName, setSaveName] = useState("");

  useEffect(() => {
    const parsed = fromSearch(location.search);
    setDraft(parsed);
    void actions.applyFilter(parsed);
    // reading the URL once on mount is intentional; later changes go through apply()
  }, []);

  const apply = (next: RunFilter) => {
    setDraft(next);
    const search = toSearch(next);
    navigate(search ? `${location.pathname}?${search}` : location.pathname, {replace: true});
    void actions.applyFilter(next);
  };

  const exportCsv = () => {
    downloadCsv(
      "scalability-runs.csv",
      jobs.map(job => ({
        job_id: job.job_id,
        service: job.service_id,
        env: job.env,
        status: job.status,
        started_at: job.started_at,
        duration_ms: job.totals?.duration_ms ?? "",
        total_tokens: job.totals?.total_tokens ?? "",
        estimated_cost_usd: job.totals?.estimated_cost_usd ?? "",
        overall_score: job.overall_score ?? "",
        validation_verdict: job.validation_verdict ?? "",
        operator: job.triggered_by ?? "",
      }))
    );
  };

  const confirmDelete = () => {
    Modal.confirm({
      title: `Delete ${selectedJobIds.length} run(s)?`,
      content: "The run documents and their artifact directories are removed. This cannot be undone.",
      okButtonProps: {danger: true},
      okText: "Delete",
      onOk: () => actions.deleteRuns(selectedJobIds),
    });
  };

  const columns: ColumnsType<ReviewJobView> = [
    {
      title: "Status",
      dataIndex: "status",
      width: 120,
      render: (_, job) => <RunStatusTag status={job.status} />,
    },
    {
      title: "Service",
      dataIndex: "service_id",
      render: (_, job) => (
        <Space direction="vertical" size={0}>
          <a onClick={() => navigate(`/${RUN_DETAIL_PATH}/${job.job_id}`)}>{job.service_id}</a>
          {job.labels?.length ? (
            <Space size={4} wrap>
              {job.labels.map(label => (
                <Tag key={label} style={{marginInlineEnd: 0}}>
                  {label}
                </Tag>
              ))}
            </Space>
          ) : null}
        </Space>
      ),
    },
    {title: "Env", dataIndex: "env", width: 90, render: value => value ?? "—"},
    {
      title: "Operator",
      dataIndex: "triggered_by",
      width: 120,
      render: (value, job) => (
        <Space direction="vertical" size={0}>
          <span>{value ?? "—"}</span>
          <Typography.Text type="secondary" style={{fontSize: 11}}>
            {job.trigger_type}
          </Typography.Text>
        </Space>
      ),
    },
    {
      title: "Started",
      dataIndex: "started_at",
      width: 130,
      sorter: true,
      render: value => formatShortDateTime(value),
    },
    {
      title: "Duration",
      width: 110,
      sorter: true,
      render: (_, job) => formatDuration(job.totals?.duration_ms),
    },
    {
      title: "Stage",
      width: 170,
      render: (_, job) =>
        job.current_stage || job.stages?.some(stage => stage.status === "RUNNING") ? (
          <Typography.Text type="secondary">{runningStageLabel(job.stages, job.current_stage)}</Typography.Text>
        ) : (
          "—"
        ),
    },
    {
      title: "Score",
      width: 110,
      render: (_, job) => <ScoreBadge score={job.overall_score} />,
    },
    {
      title: "Validation",
      width: 130,
      render: (_, job) =>
        job.validation_verdict ? (
          <Tag
            color={
              job.validation_verdict === "PASS" ? "success" : job.validation_verdict === "FAIL" ? "error" : "warning"
            }
          >
            {job.validation_verdict}
          </Tag>
        ) : (
          <Tooltip title="Stage 5 did not run, so this report carries no verdict.">
            <Tag>none</Tag>
          </Tooltip>
        ),
    },
    {
      title: "Tokens",
      width: 110,
      align: "right",
      sorter: true,
      render: (_, job) => (
        <Space size={4}>
          {formatTokens(job.totals?.total_tokens)}
          {job.totals?.within_budget === false ? (
            <Tooltip
              title={`Over the configured budget of ${formatTokens(job.totals?.token_budget)} tokens / ${formatDuration(
                job.totals?.duration_budget_ms
              )}`}
            >
              <Tag color="warning" style={{marginInlineEnd: 0}}>
                !
              </Tag>
            </Tooltip>
          ) : null}
        </Space>
      ),
    },
    {
      title: "Cost",
      width: 110,
      align: "right",
      sorter: true,
      render: (_, job) => formatCost(job.totals?.estimated_cost_usd),
    },
  ];

  return (
    <Space direction="vertical" size={16} style={{width: "100%"}}>
      <Card size="small">
        <Space wrap size={8} style={{width: "100%"}}>
          <Input
            allowClear
            placeholder="service, job id, operator, note"
            style={{width: 240}}
            value={draft.keyword}
            onChange={event => setDraft({...draft, keyword: event.target.value})}
            onPressEnter={() => apply(draft)}
          />
          <Checkbox
            checked={draft.search_reports}
            onChange={event => apply({...draft, search_reports: event.target.checked})}
          >
            <Tooltip title="Also search inside the generated report text">search reports</Tooltip>
          </Checkbox>
          <Select
            mode="multiple"
            allowClear
            placeholder="status"
            style={{minWidth: 180}}
            value={draft.status}
            options={RUN_STATUSES.map(status => ({value: status, label: status}))}
            onChange={value => apply({...draft, status: value})}
          />
          <Input
            allowClear
            placeholder="env"
            style={{width: 110}}
            value={draft.env}
            onChange={event => setDraft({...draft, env: event.target.value})}
            onPressEnter={() => apply(draft)}
          />
          <Input
            allowClear
            placeholder="operator"
            style={{width: 130}}
            value={draft.operator}
            onChange={event => setDraft({...draft, operator: event.target.value})}
            onPressEnter={() => apply(draft)}
          />
          <Select
            mode="multiple"
            allowClear
            placeholder="score"
            style={{minWidth: 150}}
            value={draft.overall_score}
            options={SCORES.map(score => ({value: score, label: score}))}
            onChange={value => apply({...draft, overall_score: value})}
          />
          <Select
            mode="multiple"
            allowClear
            placeholder="verdict"
            style={{minWidth: 180}}
            value={draft.validation_verdict}
            options={VALIDATION_VERDICTS.map(verdict => ({value: verdict, label: verdict}))}
            onChange={value => apply({...draft, validation_verdict: value})}
          />
          <Select
            allowClear
            placeholder="budget"
            style={{width: 140}}
            value={draft.within_budget === null ? undefined : draft.within_budget}
            options={[
              {value: true, label: "within budget"},
              {value: false, label: "over budget"},
            ]}
            onChange={value => apply({...draft, within_budget: value === undefined ? null : value})}
          />
          <Checkbox
            checked={draft.has_failed_stage}
            onChange={event => apply({...draft, has_failed_stage: event.target.checked})}
          >
            has failed stage
          </Checkbox>
          <Button onClick={() => apply(draft)}>Apply</Button>
          <Button
            onClick={() => {
              navigate(location.pathname, {replace: true});
              setDraft(emptyFilter());
              void actions.resetFilter();
            }}
          >
            Reset
          </Button>
        </Space>
      </Card>

      <Card
        size="small"
        title={
          <Space>
            <span>{total} run(s)</span>
            {selectedJobIds.length > 0 ? (
              <Typography.Text type="secondary">{selectedJobIds.length} selected</Typography.Text>
            ) : null}
          </Space>
        }
        extra={
          <Space>
            <Input
              size="small"
              placeholder="save this filter as…"
              style={{width: 170}}
              value={saveName}
              onChange={event => setSaveName(event.target.value)}
              onPressEnter={() => {
                if (saveName.trim()) {
                  void actions.saveFilter(saveName.trim());
                  setSaveName("");
                }
              }}
            />
            {savedFilters.map(saved => (
              <Tag
                key={saved.name}
                closable
                onClose={() => void actions.deleteSavedFilter(saved.name)}
                onClick={() => apply(saved.filter)}
                style={{cursor: "pointer"}}
              >
                {saved.name}
              </Tag>
            ))}
            <Button
              size="small"
              icon={<SwapOutlined />}
              disabled={selectedJobIds.length !== 2}
              onClick={() => navigate(`/${COMPARE_PATH}?a=${selectedJobIds[0]}&b=${selectedJobIds[1]}`)}
            >
              Compare
            </Button>
            <Button size="small" icon={<DownloadOutlined />} onClick={exportCsv} disabled={jobs.length === 0}>
              CSV
            </Button>
            <Button
              size="small"
              danger
              icon={<DeleteOutlined />}
              disabled={selectedJobIds.length === 0}
              onClick={confirmDelete}
            >
              Delete
            </Button>
            <Button size="small" icon={<ReloadOutlined />} onClick={() => void actions.load()} />
          </Space>
        }
      >
        <Table<ReviewJobView>
          rowKey="job_id"
          size="small"
          loading={loading}
          columns={columns}
          dataSource={jobs}
          rowSelection={{
            selectedRowKeys: selectedJobIds,
            onChange: keys => void actions.select(keys as string[]),
          }}
          onChange={(_pagination, _filters, sorter) => {
            const field = Array.isArray(sorter) ? undefined : sorter.field;
            const order = Array.isArray(sorter) ? undefined : sorter.order;
            if (!field || !order) return;
            const map: Record<string, string> = {
              started_at: "started_at",
              duration: "duration",
              tokens: "tokens",
              cost: "cost",
            };
            const column = String(field);
            const key = map[column] ?? "started_at";
            void actions.changeSort(order === "ascend" ? `${key}:asc` : key);
          }}
          pagination={{
            current: page,
            pageSize,
            total,
            showSizeChanger: true,
            pageSizeOptions: [25, 50, 100],
            onChange: (nextPage, nextSize) => void actions.changePage(nextPage, nextSize),
          }}
        />
      </Card>
    </Space>
  );
}
