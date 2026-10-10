import {Alert, Space, Typography} from "antd";
import type {ValidateLocalReviewResponse$IssueView} from "type/api";

interface Props {
  errors: ValidateLocalReviewResponse$IssueView[];
  warnings: ValidateLocalReviewResponse$IssueView[];
  validated: boolean;
  onJump: (field: string | null) => void;
}

const IssueList = ({issues, onJump}: {issues: ValidateLocalReviewResponse$IssueView[]; onJump: Props["onJump"]}) => (
  <ul style={{margin: "6px 0 0", paddingInlineStart: 18}}>
    {issues.map(issue => (
      <li key={`${issue.code}-${issue.field ?? "run"}`} style={{marginBottom: 2}}>
        {issue.field ? (
          <a onClick={() => onJump(issue.field)} style={{marginInlineEnd: 6}}>
            {issue.field}
          </a>
        ) : (
          <Typography.Text type="secondary" style={{marginInlineEnd: 6}}>
            environment
          </Typography.Text>
        )}
        <span>{issue.message}</span>
        {issue.hint ? (
          <Typography.Text type="secondary" style={{display: "block", fontSize: 12}}>
            {issue.hint}
          </Typography.Text>
        ) : null}
      </li>
    ))}
  </ul>
);

/**
 * The summary banner above the form.
 *
 * It lists everything at once on purpose: the validation endpoint evaluates every rule on every
 * call, so the operator fixes one list instead of discovering the next problem after each attempt.
 */
export const IssuePanel = ({errors, warnings, validated, onJump}: Props) => {
  if (!validated) return null;
  if (errors.length === 0 && warnings.length === 0) {
    return <Alert type="success" showIcon message="All checks passed. This review is ready to run." />;
  }

  return (
    <Space direction="vertical" size={8} style={{width: "100%"}}>
      {errors.length > 0 ? (
        <Alert
          type="error"
          showIcon
          message={`${errors.length} problem${errors.length === 1 ? "" : "s"} must be fixed`}
          description={<IssueList issues={errors} onJump={onJump} />}
        />
      ) : null}
      {warnings.length > 0 ? (
        <Alert
          type="warning"
          showIcon
          message={`${warnings.length} warning${warnings.length === 1 ? "" : "s"} — the run can still start`}
          description={<IssueList issues={warnings} onJump={onJump} />}
        />
      ) : null}
    </Space>
  );
};
