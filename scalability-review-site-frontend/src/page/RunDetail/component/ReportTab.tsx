import {DownloadOutlined, PrinterOutlined} from "@ant-design/icons";
import {Affix, Alert, Anchor, Button, Card, Col, Row, Space, Switch, Tag, Typography} from "antd";
import {useMemo} from "react";
import {Markdown} from "@/components/Markdown";
import {extractHeadings} from "@/utils/markdown";
import {ScoreBadge} from "@/components/ScoreBadge";
import type {ListFindingFeedbackResponse$FindingFeedbackView, ReviewJobView, ReviewReportView} from "type/api";
import {FindingFeedback} from "./FindingFeedback";

const REPORT_BODY_ID = "report-body";

interface Props {
  job: ReviewJobView;
  report: ReviewReportView;
  showSources: boolean;
  notCollected: string[];
  feedback: ListFindingFeedbackResponse$FindingFeedbackView[];
  onToggleSources: () => void;
  onSaveFeedback: (findingKey: string, title: string, verdict: string, comment: string) => void;
}

/** Exports exactly what is on screen, styles inlined, so the file stands on its own. */
const exportHtml = (report: ReviewReportView) => {
  const body = document.getElementById(REPORT_BODY_ID)?.innerHTML ?? "";
  const html = `<!DOCTYPE html>
<html lang="en"><head><meta charset="utf-8" />
<title>${report.service_id ?? "Scalability review"}</title>
<style>
body{font-family:-apple-system,BlinkMacSystemFont,"Segoe UI",Roboto,sans-serif;max-width:960px;margin:40px auto;padding:0 24px;color:#1f1f1f;line-height:1.6}
table{border-collapse:collapse;width:100%;margin:12px 0}th,td{border:1px solid #d9d9d9;padding:6px 10px;text-align:left;font-size:13px}
th{background:#fafafa}code{background:#f5f5f5;padding:1px 4px;border-radius:3px}
pre{background:#f5f5f5;padding:12px;border-radius:6px;overflow:auto}
h1,h2,h3{margin-top:28px}
</style></head><body>${body}</body></html>`;
  const blob = new Blob([html], {type: "text/html;charset=utf-8"});
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = `${report.service_id}-${report.period_label ?? "review"}.html`;
  link.click();
  URL.revokeObjectURL(url);
};

const downloadMarkdown = (report: ReviewReportView) => {
  const blob = new Blob([report.report_markdown ?? ""], {type: "text/markdown;charset=utf-8"});
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = `${report.service_id}-${report.period_label ?? "review"}.md`;
  link.click();
  URL.revokeObjectURL(url);
};

export const ReportTab = ({
  job,
  report,
  showSources,
  notCollected,
  feedback,
  onToggleSources,
  onSaveFeedback,
}: Props) => {
  const markdown = report.report_markdown ?? "";
  const headings = useMemo(() => extractHeadings(markdown), [markdown]);
  const scores = report.scores;

  return (
    <Row gutter={16}>
      <Col flex="auto" style={{minWidth: 0}}>
        <Space direction="vertical" size={12} style={{width: "100%"}}>
          <Card size="small">
            <Space wrap size={10}>
              <ScoreBadge score={scores?.overall} label="overall" />
              <ScoreBadge score={scores?.traffic} label="traffic" />
              <ScoreBadge score={scores?.latency} label="latency" />
              <ScoreBadge score={scores?.errors} label="errors" />
              <ScoreBadge score={scores?.resources} label="resources" />
              <ScoreBadge score={scores?.persistence} label="persistence" />
              {job.validation_verdict ? (
                <Tag
                  color={
                    job.validation_verdict === "PASS"
                      ? "success"
                      : job.validation_verdict === "FAIL"
                        ? "error"
                        : "warning"
                  }
                >
                  validation {job.validation_verdict}
                </Tag>
              ) : (
                <Tag>validation not run</Tag>
              )}
            </Space>
          </Card>

          {notCollected.length > 0 ? (
            <Alert
              type="warning"
              showIcon
              message={`${notCollected.length} metric(s) were not collected`}
              description={
                <ul style={{margin: "4px 0 0", paddingInlineStart: 18}}>
                  {notCollected.slice(0, 8).map((reason, index) => (
                    <li key={index}>{reason}</li>
                  ))}
                  {notCollected.length > 8 ? <li>…and {notCollected.length - 8} more, see metrics.md</li> : null}
                </ul>
              }
            />
          ) : null}

          <Card
            size="small"
            extra={
              <Space>
                <Switch size="small" checked={showSources} onChange={onToggleSources} />
                <Typography.Text type="secondary" style={{fontSize: 12}}>
                  source attributions
                </Typography.Text>
                <Button size="small" icon={<DownloadOutlined />} onClick={() => exportHtml(report)}>
                  HTML
                </Button>
                <Button size="small" icon={<DownloadOutlined />} onClick={() => downloadMarkdown(report)}>
                  Markdown
                </Button>
                <Button size="small" icon={<PrinterOutlined />} onClick={() => window.print()}>
                  Print
                </Button>
              </Space>
            }
          >
            <div id={REPORT_BODY_ID}>
              <Markdown content={markdown} showSources={showSources} />
            </div>
          </Card>

          <FindingFeedback headings={headings} feedback={feedback} onSave={onSaveFeedback} />
        </Space>
      </Col>
      <Col flex="260px">
        <Affix offsetTop={16}>
          <Card size="small" title="Contents" styles={{body: {maxHeight: "70vh", overflow: "auto"}}}>
            <Anchor
              affix={false}
              items={headings
                .filter(heading => heading.level <= 3)
                .map(heading => ({
                  key: heading.id,
                  href: `#${heading.id}`,
                  title: heading.text,
                }))}
            />
          </Card>
        </Affix>
      </Col>
    </Row>
  );
};
