import {Alert, Card, Empty, Space, Table, Tag} from "antd";
import {Markdown} from "@/components/Markdown";
import type {GetReviewReportValidationResponse} from "type/api";

const STATUS_COLORS: Record<string, string> = {
  PASS: "success",
  FAIL: "error",
  WARNING: "warning",
  SKIPPED: "default",
};

/**
 * The report's own verdict on itself (FR-6.4).
 *
 * Stage 5 re-checks the report's data and its advice; that answer used to be parsed only to write a
 * log line, which left the reader with no way to know whether the report had been checked at all.
 */
export const ValidationTab = ({validation}: {validation: GetReviewReportValidationResponse | null}) => {
  if (!validation) {
    return (
      <Empty
        image={Empty.PRESENTED_IMAGE_SIMPLE}
        description="This run has no validation result — stage 5 was skipped or did not complete."
      />
    );
  }

  const failed = (validation.checks ?? []).filter(check => check.status === "FAIL");

  return (
    <Space direction="vertical" size={12} style={{width: "100%"}}>
      <Alert
        type={validation.verdict === "PASS" ? "success" : validation.verdict === "FAIL" ? "error" : "warning"}
        showIcon
        message={`Overall validation verdict: ${validation.verdict ?? "UNKNOWN"}`}
        description={
          failed.length > 0
            ? `${failed.length} check(s) failed: ${failed.map(check => check.check_name).join(", ")}`
            : "Every check passed."
        }
      />

      <Card size="small" title="Checks">
        <Table
          rowKey={record => String(record.check_number)}
          size="small"
          pagination={false}
          dataSource={validation.checks ?? []}
          columns={[
            {title: "#", dataIndex: "check_number", width: 60},
            {title: "Check", dataIndex: "check_name"},
            {
              title: "Status",
              dataIndex: "status",
              width: 120,
              render: value => <Tag color={STATUS_COLORS[value] ?? "default"}>{value}</Tag>,
            },
            {title: "Issues", dataIndex: "issue_count", width: 100, align: "right"},
          ]}
        />
      </Card>

      {validation.validation_report_markdown ? (
        <Card size="small" title="Full validation report">
          <Markdown content={validation.validation_report_markdown} />
        </Card>
      ) : null}
    </Space>
  );
};
