import {CheckCircleTwoTone, CloseCircleTwoTone, WarningTwoTone} from "@ant-design/icons";
import {Button, List, Space, Typography} from "antd";
import type {PreflightResponse$CheckView, PreflightStatus} from "type/api";

const ICONS: Record<PreflightStatus, React.ReactNode> = {
  PASS: <CheckCircleTwoTone twoToneColor="#52c41a" />,
  WARN: <WarningTwoTone twoToneColor="#faad14" />,
  FAIL: <CloseCircleTwoTone twoToneColor="#ff4d4f" />,
};

interface Props {
  checks: PreflightResponse$CheckView[];
  loading: boolean;
  onRun: () => void;
}

/**
 * Environment diagnostics (FR-10). Most local review failures are environmental rather than
 * anything to do with the service under review, so every line carries the remediation.
 */
export const PreflightPanel = ({checks, loading, onRun}: Props) => (
  <Space direction="vertical" size={8} style={{width: "100%"}}>
    <Space>
      <Button size="small" loading={loading} onClick={onRun}>
        Run environment checks
      </Button>
      <Typography.Text type="secondary" style={{fontSize: 12}}>
        Gemini CLI, credentials, MCP servers, repositories, MongoDB — nothing is sent to a model.
      </Typography.Text>
    </Space>
    {checks.length > 0 ? (
      <List
        size="small"
        bordered
        dataSource={checks}
        renderItem={check => (
          <List.Item>
            <Space align="start">
              {ICONS[check.status]}
              <Space direction="vertical" size={0}>
                <Typography.Text strong>{check.name}</Typography.Text>
                <Typography.Text type="secondary" style={{fontSize: 12}}>
                  {check.detail}
                </Typography.Text>
                {check.hint ? (
                  <Typography.Text type="warning" style={{fontSize: 12}}>
                    {check.hint}
                  </Typography.Text>
                ) : null}
              </Space>
            </Space>
          </List.Item>
        )}
      />
    ) : null}
  </Space>
);
