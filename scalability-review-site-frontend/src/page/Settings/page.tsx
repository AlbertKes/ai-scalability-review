import {SaveOutlined} from "@ant-design/icons";
import {
  Alert,
  Button,
  Card,
  Checkbox,
  Col,
  Input,
  InputNumber,
  Row,
  Select,
  Skeleton,
  Space,
  Table,
  Typography,
} from "antd";
import {useLoadingStatus} from "@wonder/core-fe";
import {PreflightPanel} from "@/page/NewRun/component/PreflightPanel";
import {formatDateTime} from "@/utils/format";
import {useRuntimeState} from "@/utils/useRuntimeState";
import type {SettingsView} from "type/api";
import {actions} from "./module";
import {LOADING_SETTINGS, PREFLIGHTING_SETTINGS, SAVING_SETTINGS} from "./type";

const PREF_LABELS: Array<[keyof NonNullable<SettingsView["notification_prefs"]>, string]> = [
  ["on_completed", "Run completed"],
  ["on_failed", "Run failed"],
  ["on_cancelled", "Run cancelled"],
  ["on_stage_failed", "A stage failed but the run continued"],
  ["on_over_budget", "Run exceeded the token or time budget"],
  ["on_validation_failed", "Validation verdict is FAIL"],
  ["on_red_score", "Overall score is RED"],
  ["on_stale", "Run went stale after a restart"],
];

const MINUTE_MS = 60_000;

export default function Settings() {
  const {settings, presets, preflight} = useRuntimeState("Settings");
  const loading = useLoadingStatus(LOADING_SETTINGS);
  const saving = useLoadingStatus(SAVING_SETTINGS);
  const preflighting = useLoadingStatus(PREFLIGHTING_SETTINGS);

  if (loading || !settings) return <Skeleton active paragraph={{rows: 8}} />;
  const prefs = settings.notification_prefs;
  const unpriced = settings.price_table_version === "unconfigured";

  return (
    <Space direction="vertical" size={16} style={{width: "100%", maxWidth: 1000}}>
      <Card
        size="small"
        title="General"
        extra={
          <Button type="primary" icon={<SaveOutlined />} loading={saving} onClick={() => void actions.save()}>
            Save
          </Button>
        }
      >
        <Row gutter={16}>
          <Col span={12}>
            <Typography.Text>Output base directory</Typography.Text>
            <Input
              value={settings.output_base_dir ?? ""}
              onChange={event => void actions.edit({output_base_dir: event.target.value})}
            />
            <Typography.Text type="secondary" style={{fontSize: 12}}>
              Each run writes into its own subdirectory under &lt;base&gt;/&lt;service&gt;, so runs never overwrite each
              other.
            </Typography.Text>
          </Col>
          <Col span={6}>
            <Typography.Text>Default operator</Typography.Text>
            <Input
              value={settings.default_operator ?? ""}
              onChange={event => void actions.edit({default_operator: event.target.value})}
            />
          </Col>
          <Col span={6}>
            <Typography.Text>Concurrent runs</Typography.Text>
            <InputNumber
              min={1}
              max={4}
              style={{width: "100%"}}
              value={settings.max_concurrent_runs ?? 1}
              onChange={value => void actions.edit({max_concurrent_runs: value ?? 1})}
            />
          </Col>
        </Row>
      </Card>

      <Card size="small" title="Budgets">
        <Alert
          type="info"
          showIcon
          style={{marginBottom: 12}}
          message="A budget that is always breached stops being a signal"
          description="The documented 1,500,000-token target is a pilot figure that real runs already exceed. Set these from your own observed baseline so the badge means a regression."
        />
        <Row gutter={16}>
          <Col span={8}>
            <Typography.Text>Token budget per run</Typography.Text>
            <InputNumber
              min={0}
              step={100000}
              style={{width: "100%"}}
              value={settings.token_budget ?? 0}
              formatter={value => `${value}`.replace(/\B(?=(\d{3})+(?!\d))/g, ",")}
              parser={value => Number((value ?? "").replace(/,/g, ""))}
              onChange={value => void actions.edit({token_budget: value ?? 0})}
            />
          </Col>
          <Col span={8}>
            <Typography.Text>Wall-clock budget (minutes)</Typography.Text>
            <InputNumber
              min={1}
              style={{width: "100%"}}
              value={Math.round((settings.duration_budget_ms ?? 0) / MINUTE_MS)}
              onChange={value => void actions.edit({duration_budget_ms: (value ?? 0) * MINUTE_MS})}
            />
          </Col>
          <Col span={8}>
            <Typography.Text>Monthly cost ceiling (USD)</Typography.Text>
            <InputNumber
              min={0}
              style={{width: "100%"}}
              value={settings.monthly_cost_ceiling_usd ?? undefined}
              onChange={value => void actions.edit({monthly_cost_ceiling_usd: value ?? null})}
            />
          </Col>
        </Row>
      </Card>

      <Card size="small" title="Model prices (USD per million tokens)">
        {unpriced ? (
          <Alert
            type="warning"
            showIcon
            style={{marginBottom: 12}}
            message="No prices configured"
            description="Token counts are exact everywhere in the console; costs stay blank until these rows are filled in. Cached input is priced separately because it is usually most of the input."
          />
        ) : null}
        <Table
          rowKey="model"
          size="small"
          pagination={false}
          dataSource={settings.model_prices ?? []}
          columns={[
            {title: "Model", dataIndex: "model"},
            {
              title: "Input",
              width: 160,
              render: (_, price) => (
                <InputNumber
                  min={0}
                  step={0.01}
                  style={{width: "100%"}}
                  value={price.input_usd_per_million ?? undefined}
                  onChange={value => void actions.editPrice(price.model, {input_usd_per_million: value ?? null})}
                />
              ),
            },
            {
              title: "Cached input",
              width: 160,
              render: (_, price) => (
                <InputNumber
                  min={0}
                  step={0.01}
                  style={{width: "100%"}}
                  value={price.cached_input_usd_per_million ?? undefined}
                  onChange={value => void actions.editPrice(price.model, {cached_input_usd_per_million: value ?? null})}
                />
              ),
            },
            {
              title: "Output",
              width: 160,
              render: (_, price) => (
                <InputNumber
                  min={0}
                  step={0.01}
                  style={{width: "100%"}}
                  value={price.output_usd_per_million ?? undefined}
                  onChange={value => void actions.editPrice(price.model, {output_usd_per_million: value ?? null})}
                />
              ),
            },
          ]}
        />
        <Space style={{marginTop: 12}}>
          <Typography.Text>Price table version</Typography.Text>
          <Input
            style={{width: 220}}
            value={settings.price_table_version ?? ""}
            onChange={event => void actions.edit({price_table_version: event.target.value})}
          />
          <Typography.Text type="secondary" style={{fontSize: 12}}>
            Shown next to every cost, so a figure can be traced to the rates it used.
          </Typography.Text>
        </Space>
      </Card>

      <Card size="small" title="Notifications">
        <Space direction="vertical" size={6}>
          {PREF_LABELS.map(([key, label]) => (
            <Checkbox
              key={key}
              checked={prefs?.[key] !== false}
              onChange={event => void actions.editPrefs({[key]: event.target.checked})}
            >
              {label}
            </Checkbox>
          ))}
          <Checkbox
            checked={Boolean(prefs?.browser_enabled)}
            onChange={event =>
              event.target.checked
                ? void actions.enableBrowserNotifications()
                : void actions.editPrefs({browser_enabled: false})
            }
          >
            Desktop notifications (asks the browser for permission)
          </Checkbox>
          <Space>
            <Checkbox
              checked={Boolean(prefs?.slack_enabled)}
              onChange={event => void actions.editPrefs({slack_enabled: event.target.checked})}
            >
              Slack webhook
            </Checkbox>
            <Input
              style={{width: 420}}
              placeholder="https://hooks.slack.com/services/…"
              value={settings.slack_webhook_url ?? ""}
              onChange={event => void actions.edit({slack_webhook_url: event.target.value})}
            />
          </Space>
        </Space>
      </Card>

      <Card size="small" title="Appearance and retention">
        <Row gutter={16}>
          <Col span={6}>
            <Typography.Text>Theme</Typography.Text>
            <Select
              style={{width: "100%"}}
              value={settings.theme ?? "light"}
              options={[
                {value: "light", label: "light"},
                {value: "dark", label: "dark"},
              ]}
              onChange={value => void actions.edit({theme: value})}
            />
          </Col>
          <Col span={6}>
            <Typography.Text>Density</Typography.Text>
            <Select
              style={{width: "100%"}}
              value={settings.density ?? "comfortable"}
              options={[
                {value: "comfortable", label: "comfortable"},
                {value: "compact", label: "compact"},
              ]}
              onChange={value => void actions.edit({density: value})}
            />
          </Col>
          <Col span={6}>
            <Typography.Text>Keep last N runs per service</Typography.Text>
            <InputNumber
              min={0}
              style={{width: "100%"}}
              value={settings.retention_keep_last_n ?? 0}
              onChange={value => void actions.edit({retention_keep_last_n: value ?? 0})}
            />
            <Typography.Text type="secondary" style={{fontSize: 12}}>
              0 keeps everything — deleting a run is a deliberate act.
            </Typography.Text>
          </Col>
          <Col span={6}>
            <Typography.Text>Prune runs older than (days)</Typography.Text>
            <InputNumber
              min={0}
              style={{width: "100%"}}
              value={settings.retention_max_age_days ?? 0}
              onChange={value => void actions.edit({retention_max_age_days: value ?? 0})}
            />
          </Col>
        </Row>
      </Card>

      <Card size="small" title="Environment">
        <PreflightPanel checks={preflight} loading={preflighting} onRun={() => void actions.runPreflight()} />
      </Card>

      <Card size="small" title="Presets">
        <Table
          rowKey="preset_id"
          size="small"
          pagination={false}
          dataSource={presets}
          columns={[
            {title: "Name", dataIndex: "name"},
            {title: "Service", dataIndex: "service_id"},
            {title: "Env", dataIndex: "env", render: value => value ?? "—"},
            {title: "Default", dataIndex: "is_default", width: 100, render: value => (value ? "yes" : "")},
            {title: "Created by", dataIndex: "created_by", width: 140},
            {title: "Updated", dataIndex: "updated_at", width: 170, render: value => formatDateTime(value)},
            {
              title: "",
              width: 90,
              render: (_, preset) => (
                <Button size="small" danger onClick={() => void actions.deletePreset(preset.preset_id)}>
                  Delete
                </Button>
              ),
            },
          ]}
        />
      </Card>

      <Typography.Text type="secondary">Last saved {formatDateTime(settings.updated_at)}</Typography.Text>
    </Space>
  );
}
