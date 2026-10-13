import {DeleteOutlined, ReloadOutlined, StopOutlined, SwapOutlined} from "@ant-design/icons";
import {
  Alert,
  Button,
  Card,
  Descriptions,
  Empty,
  Input,
  Modal,
  Skeleton,
  Space,
  Spin,
  Tabs,
  Tag,
  Tooltip,
  Typography,
} from "antd";
import {useLoadingStatus, useNavigate, useParams} from "@wonder/core-fe";
import {useEffect, useState} from "react";
import {RunStatusTag} from "@/components/StatusTag";
import {isTerminal, runningStageLabel, STAGE_LABELS} from "@/constants/review";
import {COMPARE_PATH} from "@/route";
import {elapsedSince, formatCost, formatDateTime, formatDuration, formatTokens, utcHint} from "@/utils/format";
import {useRuntimeState} from "@/utils/useRuntimeState";
import {ArtifactsTab} from "./component/ArtifactsTab";
import {CostPanel} from "./component/CostPanel";
import {ReportTab} from "./component/ReportTab";
import {StageTimeline} from "./component/StageTimeline";
import {ValidationTab} from "./component/ValidationTab";
import {actions} from "./module";
import {LOADING_RUN} from "./type";

const TICK_MS = 1000;

export default function RunDetail() {
  const {jobId, tab} = useParams<{jobId: string; tab?: string}>();
  const {
    job,
    artifacts,
    outputDir,
    report,
    validation,
    feedback,
    logStage,
    logContent,
    logComplete,
    preview,
    showSources,
    notCollected,
  } = useRuntimeState("RunDetail");
  const loading = useLoadingStatus(LOADING_RUN);
  const navigate = useNavigate();
  const [now, setNow] = useState(Date.now());
  const [note, setNote] = useState("");

  /** A long stage needs a moving clock, because the CLI reports nothing until it finishes. */
  useEffect(() => {
    if (!job || isTerminal(job.status)) return;
    const timer = window.setInterval(() => setNow(Date.now()), TICK_MS);
    return () => window.clearInterval(timer);
  }, [job]);

  useEffect(() => {
    setNote(job?.note ?? "");
  }, [job?.job_id, job?.note]);

  if (loading && !job) return <Skeleton active paragraph={{rows: 8}} />;
  if (!job) return <Empty description="Run not found" />;

  const running = !isTerminal(job.status);
  const operator = job.triggered_by ?? job.request?.operator ?? "unknown";

  const confirmDelete = () =>
    Modal.confirm({
      title: "Delete this run?",
      content: `The run document and ${outputDir ?? "its artifact directory"} are removed. This cannot be undone.`,
      okButtonProps: {danger: true},
      okText: "Delete",
      onOk: () => actions.remove(),
    });

  const retryFrom = (stage: string | null) =>
    Modal.confirm({
      title: stage ? `Re-run from ${STAGE_LABELS[stage] ?? stage}?` : "Re-run this review?",
      content: stage
        ? "A new run is created with its own directory; the stages before this one reuse the artifacts of this run."
        : "A new run is created with the same parameters.",
      okText: "Run",
      onOk: () => actions.retry(stage, operator),
    });

  return (
    <Space direction="vertical" size={16} style={{width: "100%"}}>
      <Card size="small">
        <Space direction="vertical" size={10} style={{width: "100%"}}>
          <Space wrap align="center" style={{justifyContent: "space-between", width: "100%"}}>
            <Space wrap align="center">
              <Typography.Title level={4} style={{margin: 0}}>
                {job.service_id}
              </Typography.Title>
              {job.env ? <Tag>{job.env}</Tag> : null}
              <RunStatusTag status={job.status} />
              {running ? (
                <Space size={4}>
                  <Spin size="small" />
                  <Typography.Text type="secondary">
                    {runningStageLabel(job.stages, job.current_stage)} · {elapsedSince(job.started_at, now)}
                  </Typography.Text>
                </Space>
              ) : null}
              {job.baseline ? <Tag color="blue">baseline</Tag> : null}
            </Space>
            <Space>
              {running ? (
                <Button danger icon={<StopOutlined />} onClick={() => void actions.cancel()}>
                  Cancel
                </Button>
              ) : (
                <Button icon={<ReloadOutlined />} onClick={() => retryFrom(null)}>
                  Re-run
                </Button>
              )}
              <Button icon={<SwapOutlined />} onClick={() => navigate(`/${COMPARE_PATH}?a=${job.job_id}`)}>
                Compare
              </Button>
              <Button
                onClick={() => void actions.updateRun({baseline: !job.baseline})}
                disabled={!isTerminal(job.status)}
              >
                {job.baseline ? "Unset baseline" : "Set as baseline"}
              </Button>
              <Button danger icon={<DeleteOutlined />} disabled={running} onClick={confirmDelete}>
                Delete
              </Button>
            </Space>
          </Space>

          {job.error_message ? <Alert type="error" showIcon message={job.error_message} /> : null}
          {job.status === "STALE" ? (
            <Alert
              type="warning"
              showIcon
              message="This run did not survive a service restart"
              description="Nothing is running behind it. Re-run it to get a result."
            />
          ) : null}
          {job.acknowledged_warnings?.length ? (
            <Alert
              type="info"
              showIcon
              message={`Started with ${job.acknowledged_warnings.length} acknowledged warning(s)`}
              description={job.acknowledged_warnings.join(", ")}
            />
          ) : null}

          <Descriptions size="small" column={4}>
            <Descriptions.Item label="Operator">{operator}</Descriptions.Item>
            <Descriptions.Item label="Trigger">{job.trigger_type}</Descriptions.Item>
            <Descriptions.Item label="Started">
              <Tooltip title={utcHint(job.started_at)}>{formatDateTime(job.started_at)}</Tooltip>
            </Descriptions.Item>
            <Descriptions.Item label="Finished">{formatDateTime(job.completed_at)}</Descriptions.Item>
            <Descriptions.Item label="Duration">{formatDuration(job.totals?.duration_ms)}</Descriptions.Item>
            <Descriptions.Item label="Tokens">{formatTokens(job.totals?.total_tokens)}</Descriptions.Item>
            <Descriptions.Item label="Cost">{formatCost(job.totals?.estimated_cost_usd)}</Descriptions.Item>
            <Descriptions.Item label="Job id">
              <Typography.Text copyable style={{fontSize: 12}}>
                {job.job_id}
              </Typography.Text>
            </Descriptions.Item>
          </Descriptions>

          <Space.Compact style={{width: 520}}>
            <Input
              placeholder="note for this run"
              value={note}
              onChange={event => setNote(event.target.value)}
              onPressEnter={() => void actions.updateRun({note})}
            />
            <Button onClick={() => void actions.updateRun({note})}>Save note</Button>
          </Space.Compact>
        </Space>
      </Card>

      <Tabs
        activeKey={tab ?? "overview"}
        onChange={key => navigate(`/RunDetail/${job.job_id}/${key}`)}
        items={[
          {
            key: "overview",
            label: "Stages",
            children: (
              <StageTimeline
                stages={job.stages ?? []}
                logStage={logStage}
                logContent={logContent}
                logComplete={logComplete}
                onSelectLog={stage => void actions.selectLogStage(stage)}
                onPreview={name => void actions.openPreview(name)}
                onRetryFrom={stage => retryFrom(stage)}
                canRetry={isTerminal(job.status)}
              />
            ),
          },
          {key: "cost", label: "AI usage", children: <CostPanel job={job} />},
          {
            key: "report",
            label: "Report",
            children: report ? (
              <ReportTab
                job={job}
                report={report}
                showSources={showSources}
                notCollected={notCollected}
                feedback={feedback}
                onToggleSources={() => void actions.toggleSources()}
                onSaveFeedback={(findingKey, title, verdict, comment) =>
                  void actions.saveFeedback({
                    job_id: job.job_id,
                    finding_key: findingKey,
                    finding_title: title,
                    verdict: verdict as "CONFIRMED" | "FALSE_POSITIVE" | "WONT_FIX",
                    comment,
                    author: operator,
                  })
                }
              />
            ) : (
              <Empty description="No report yet — the synthesis stage has not produced one." />
            ),
          },
          {key: "validation", label: "Validation", children: <ValidationTab validation={validation} />},
          {
            key: "artifacts",
            label: "Artifacts",
            children: (
              <ArtifactsTab
                jobId={job.job_id}
                outputDir={outputDir}
                artifacts={artifacts}
                preview={preview}
                onPreview={name => void actions.openPreview(name)}
                onClosePreview={() => void actions.closePreview()}
              />
            ),
          },
          {
            key: "provenance",
            label: "Provenance",
            children: (
              <Space direction="vertical" size={12} style={{width: "100%"}}>
                <Alert
                  type="info"
                  showIcon
                  message="What produced this report"
                  description="Without these, two runs of the same service cannot be told apart as 'the service changed' versus 'the pipeline changed'."
                />
                <Descriptions size="small" bordered column={2}>
                  <Descriptions.Item label="Gemini CLI">{job.fingerprint?.cli_version ?? "unknown"}</Descriptions.Item>
                  <Descriptions.Item label="Prompt version">
                    {job.fingerprint?.prompt_version ?? "unknown"}
                  </Descriptions.Item>
                  <Descriptions.Item label="App repo">
                    {job.fingerprint?.repo_sha_app ?? "unknown"}
                    {job.fingerprint?.repo_dirty_app ? (
                      <Tag color="warning" style={{marginInlineStart: 6}}>
                        uncommitted
                      </Tag>
                    ) : null}
                  </Descriptions.Item>
                  <Descriptions.Item label="Infra repo">
                    {job.fingerprint?.repo_sha_infra ?? "unknown"}
                    {job.fingerprint?.repo_dirty_infra ? (
                      <Tag color="warning" style={{marginInlineStart: 6}}>
                        uncommitted
                      </Tag>
                    ) : null}
                  </Descriptions.Item>
                  <Descriptions.Item label="Azure resource group">
                    {job.fingerprint?.azure_resource_group ?? "not resolved"}
                  </Descriptions.Item>
                  <Descriptions.Item label="Namespace">{job.namespace ?? "—"}</Descriptions.Item>
                </Descriptions>
                <Card size="small" title="Request as submitted">
                  <pre style={{margin: 0, fontSize: 12, whiteSpace: "pre-wrap"}}>
                    {JSON.stringify(job.request ?? {}, null, 2)}
                  </pre>
                </Card>
                <Card size="small" title="Raw job document">
                  <pre style={{margin: 0, fontSize: 12, whiteSpace: "pre-wrap", maxHeight: 400, overflow: "auto"}}>
                    {JSON.stringify(job, null, 2)}
                  </pre>
                </Card>
              </Space>
            ),
          },
        ]}
      />
    </Space>
  );
}
