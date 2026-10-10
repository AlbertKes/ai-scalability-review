import {PlayCircleOutlined, SafetyCertificateOutlined, SaveOutlined} from "@ant-design/icons";
import {Button, Card, Checkbox, Collapse, Descriptions, Input, Select, Space, Tooltip, Typography} from "antd";
import {useLoadingStatus} from "@wonder/core-fe";
import {useEffect, useMemo, useRef, useState} from "react";
import {GEMINI_MODELS, STAGE_MODEL_DEFAULTS} from "@/constants/review";
import {formatCost, formatDuration, formatTokens} from "@/utils/format";
import {useRuntimeState} from "@/utils/useRuntimeState";
import type {LocalReviewRequestView, ValidateLocalReviewResponse$IssueView} from "type/api";
import {IssuePanel} from "./component/IssuePanel";
import {PreflightPanel} from "./component/PreflightPanel";
import {actions} from "./module";
import {PREFLIGHTING, SUBMITTING, VALIDATING} from "./type";

const REVALIDATE_DELAY_MS = 800;

const SECTION_FIELDS: Record<string, string[]> = {
  target: ["service", "env", "namespace", "domain"],
  repositories: ["local_app_repo_path", "local_infra_repo_path", "app_code_paths"],
  sources: [
    "mysql_host",
    "mysql_db",
    "atlas_cluster",
    "kafka_consumer_groups",
    "azure_resource_group",
    "azure_subscription_id",
    "hpa_type",
  ],
  options: ["skip_code_analysis", "skip_validation", "output_base_dir", "operator"],
  models: ["code_analysis_model", "metric_collection_model", "synthesis_model", "validation_model"],
};

const sectionOf = (field: string): string | null => {
  const base = field.replace(/\[\d+\]$/, "");
  const entry = Object.entries(SECTION_FIELDS).find(([, fields]) => fields.includes(base));
  return entry?.[0] ?? null;
};

const Field = ({
  name,
  label,
  hint,
  issues,
  children,
}: {
  name: string;
  label: string;
  hint?: string;
  issues: ValidateLocalReviewResponse$IssueView[];
  children: React.ReactNode;
}) => {
  const own = issues.filter(issue => issue.field === name || issue.field?.startsWith(`${name}[`));
  const error = own.find(issue => issue.severity === "ERROR");
  const warning = own.find(issue => issue.severity === "WARNING");
  return (
    <div id={`field-${name}`} style={{marginBottom: 14}}>
      <Typography.Text style={{display: "block", marginBottom: 4, fontSize: 13}}>{label}</Typography.Text>
      {children}
      {hint ? (
        <Typography.Text type="secondary" style={{display: "block", fontSize: 12, marginTop: 2}}>
          {hint}
        </Typography.Text>
      ) : null}
      {own.map(issue => (
        <Typography.Text
          key={issue.code}
          type={issue.severity === "ERROR" ? "danger" : "warning"}
          style={{display: "block", fontSize: 12, marginTop: 2}}
        >
          {issue.message}
          {issue.hint ? ` — ${issue.hint}` : ""}
        </Typography.Text>
      ))}
      {!error && !warning ? null : null}
    </div>
  );
};

export default function NewRun() {
  const {request, labels, errors, warnings, validated, acknowledged, presets, preflight, estimate} =
    useRuntimeState("NewRun");
  const validating = useLoadingStatus(VALIDATING);
  const submitting = useLoadingStatus(SUBMITTING);
  const preflighting = useLoadingStatus(PREFLIGHTING);
  const [presetName, setPresetName] = useState("");
  const [activeSections, setActiveSections] = useState<string[]>(["target", "repositories", "sources", "options"]);
  const timer = useRef<ReturnType<typeof setTimeout>>();

  const issues = useMemo(() => [...errors, ...warnings], [errors, warnings]);

  /** Re-validates shortly after typing stops, so fixed problems disappear without a second click. */
  useEffect(() => {
    if (!request.service && !request.local_app_repo_path) return;
    if (timer.current) clearTimeout(timer.current);
    timer.current = setTimeout(() => {
      void actions.validate();
      void actions.estimate();
    }, REVALIDATE_DELAY_MS);
    return () => {
      if (timer.current) clearTimeout(timer.current);
    };
  }, [request]);

  /** A section that hides a problem is useless, so sections with issues open themselves. */
  useEffect(() => {
    const needed = new Set(activeSections);
    issues.forEach(issue => {
      const section = issue.field ? sectionOf(issue.field) : null;
      if (section) needed.add(section);
    });
    if (needed.size !== activeSections.length) setActiveSections([...needed]);
  }, [issues]);

  const jump = (field: string | null) => {
    if (!field) return;
    const section = sectionOf(field);
    if (section && !activeSections.includes(section)) setActiveSections([...activeSections, section]);
    window.setTimeout(() => {
      document
        .getElementById(`field-${field.replace(/\[\d+\]$/, "")}`)
        ?.scrollIntoView({behavior: "smooth", block: "center"});
    }, 50);
  };

  const set = (patch: Partial<LocalReviewRequestView>) => void actions.update(patch);
  const blocked = validated && errors.length > 0;

  return (
    <Space direction="vertical" size={16} style={{width: "100%", maxWidth: 1100}}>
      <IssuePanel errors={errors} warnings={warnings} validated={validated} onJump={jump} />

      <Card
        size="small"
        title="Start a local review"
        extra={
          <Space>
            <Select
              allowClear
              placeholder="load a preset"
              style={{width: 220}}
              options={presets.map(preset => ({
                value: preset.preset_id,
                label: `${preset.name} — ${preset.service_id}${preset.env ? ` (${preset.env})` : ""}`,
              }))}
              onChange={value => value && void actions.applyPreset(value)}
            />
            <Input
              placeholder="save as preset…"
              style={{width: 170}}
              value={presetName}
              onChange={event => setPresetName(event.target.value)}
            />
            <Button
              icon={<SaveOutlined />}
              disabled={!presetName.trim()}
              onClick={() => {
                void actions.savePreset(presetName.trim());
                setPresetName("");
              }}
            />
          </Space>
        }
      >
        <Collapse
          activeKey={activeSections}
          onChange={keys => setActiveSections(keys as string[])}
          items={[
            {
              key: "target",
              label: "Target",
              children: (
                <>
                  <Field name="service" label="Service" issues={issues}>
                    <Input
                      value={request.service ?? ""}
                      onChange={e => set({service: e.target.value})}
                      placeholder="wonder-cart-service"
                    />
                  </Field>
                  <Field
                    name="env"
                    label="Environment"
                    hint="The Datadog tag — uat, prod. Also a directory in the infra repository."
                    issues={issues}
                  >
                    <Input value={request.env ?? ""} onChange={e => set({env: e.target.value})} placeholder="uat" />
                  </Field>
                  <Field name="namespace" label="Kubernetes namespace" issues={issues}>
                    <Input
                      value={request.namespace ?? ""}
                      onChange={e => set({namespace: e.target.value})}
                      placeholder="uat-consumer"
                    />
                  </Field>
                  <Field
                    name="domain"
                    label="Infra domain"
                    hint="Directory under <env>/app in the infra repository."
                    issues={issues}
                  >
                    <Input
                      value={request.domain ?? ""}
                      onChange={e => set({domain: e.target.value})}
                      placeholder="consumer"
                    />
                  </Field>
                </>
              ),
            },
            {
              key: "repositories",
              label: "Repositories",
              children: (
                <>
                  <Field
                    name="local_app_repo_path"
                    label="Application repository"
                    hint="Absolute path to the local clone."
                    issues={issues}
                  >
                    <Input
                      value={request.local_app_repo_path ?? ""}
                      onChange={e => set({local_app_repo_path: e.target.value})}
                      placeholder="/Users/you/IdeaProjects/03Wonder/wonder-consumer-project"
                    />
                  </Field>
                  <Field name="local_infra_repo_path" label="Infra repository" issues={issues}>
                    <Input
                      value={request.local_infra_repo_path ?? ""}
                      onChange={e => set({local_infra_repo_path: e.target.value})}
                      placeholder="/Users/you/IdeaProjects/03Wonder/infra"
                    />
                  </Field>
                  <Field
                    name="app_code_paths"
                    label="Modules to analyse"
                    hint="Repo-relative paths. The single biggest lever on stage 1 cost: naming the modules on wonder-cart-service cut the injected context from 807 KB to 111 KB."
                    issues={issues}
                  >
                    <Select
                      mode="tags"
                      style={{width: "100%"}}
                      value={request.app_code_paths ?? []}
                      onChange={value => set({app_code_paths: value})}
                      placeholder="backend/wonder-cart-service"
                      tokenSeparators={[",", " "]}
                    />
                  </Field>
                </>
              ),
            },
            {
              key: "sources",
              label: "Data sources",
              children: (
                <>
                  <Field
                    name="mysql_host"
                    label="MySQL host"
                    hint="Azure MySQL Flexible Server name. Leave empty if the service has no MySQL."
                    issues={issues}
                  >
                    <Input value={request.mysql_host ?? ""} onChange={e => set({mysql_host: e.target.value})} />
                  </Field>
                  <Field name="mysql_db" label="MySQL database" issues={issues}>
                    <Input value={request.mysql_db ?? ""} onChange={e => set({mysql_db: e.target.value})} />
                  </Field>
                  <Field name="atlas_cluster" label="Atlas cluster" issues={issues}>
                    <Input value={request.atlas_cluster ?? ""} onChange={e => set({atlas_cluster: e.target.value})} />
                  </Field>
                  <Field
                    name="kafka_consumer_groups"
                    label="Kafka consumer groups"
                    hint="Comma separated."
                    issues={issues}
                  >
                    <Input
                      value={request.kafka_consumer_groups ?? ""}
                      onChange={e => set({kafka_consumer_groups: e.target.value})}
                    />
                  </Field>
                  <Field
                    name="azure_resource_group"
                    label="Azure resource group"
                    hint="Leave empty to read it from <env>/app/infra/env/01-variables.tf — it is not the env name (uat lives in FTIUat)."
                    issues={issues}
                  >
                    <Input
                      value={request.azure_resource_group ?? ""}
                      onChange={e => set({azure_resource_group: e.target.value})}
                    />
                  </Field>
                  <Field
                    name="azure_subscription_id"
                    label="Azure subscription"
                    hint="Defaults to the Azure CLI profile."
                    issues={issues}
                  >
                    <Input
                      value={request.azure_subscription_id ?? ""}
                      onChange={e => set({azure_subscription_id: e.target.value})}
                    />
                  </Field>
                  <Field name="hpa_type" label="HPA" issues={issues}>
                    <Select
                      style={{width: 160}}
                      value={request.hpa_type ?? "none"}
                      onChange={value => set({hpa_type: value})}
                      options={[
                        {value: "HPA", label: "HPA"},
                        {value: "none", label: "none"},
                      ]}
                    />
                  </Field>
                </>
              ),
            },
            {
              key: "options",
              label: "Run options",
              children: (
                <>
                  <Field name="operator" label="Operator" issues={issues}>
                    <Input value={request.operator ?? ""} onChange={e => set({operator: e.target.value})} />
                  </Field>
                  <Field
                    name="output_base_dir"
                    label="Output base directory"
                    hint="Each run gets its own subdirectory under <base>/<service>."
                    issues={issues}
                  >
                    <Input
                      value={request.output_base_dir ?? ""}
                      onChange={e => set({output_base_dir: e.target.value})}
                      placeholder="reports"
                    />
                  </Field>
                  <Field name="skip_code_analysis" label="Skip code analysis" issues={issues}>
                    <Checkbox
                      checked={Boolean(request.skip_code_analysis)}
                      onChange={e => set({skip_code_analysis: e.target.checked})}
                    >
                      reuse the newest previous code-context.md
                    </Checkbox>
                  </Field>
                  <Field name="skip_validation" label="Skip validation" issues={issues}>
                    <Checkbox
                      checked={Boolean(request.skip_validation)}
                      onChange={e => set({skip_validation: e.target.checked})}
                    >
                      roughly halves the MCP round-trips, and leaves the report with no verdict
                    </Checkbox>
                  </Field>
                  <Field name="labels" label="Labels" hint="Searchable from the run list." issues={issues}>
                    <Select
                      mode="tags"
                      style={{width: "100%"}}
                      value={labels}
                      onChange={value => void actions.setLabels(value)}
                      placeholder="before HPA change"
                    />
                  </Field>
                </>
              ),
            },
            {
              key: "models",
              label: "Models (defaults shown)",
              children: (
                <>
                  {Object.entries(STAGE_MODEL_DEFAULTS).map(([field, fallback]) => (
                    <Field
                      key={field}
                      name={field}
                      label={field.replace(/_/g, " ")}
                      hint={`default ${fallback}`}
                      issues={issues}
                    >
                      <Select
                        allowClear
                        style={{width: 280}}
                        placeholder={fallback}
                        value={(request[field as keyof LocalReviewRequestView] as string) || undefined}
                        onChange={value => set({[field]: value ?? ""} as Partial<LocalReviewRequestView>)}
                        options={GEMINI_MODELS.map(model => ({value: model, label: model}))}
                      />
                    </Field>
                  ))}
                </>
              ),
            },
          ]}
        />
      </Card>

      <Card size="small" title="Before it runs">
        <Space direction="vertical" size={14} style={{width: "100%"}}>
          <PreflightPanel checks={preflight} loading={preflighting} onRun={() => void actions.runPreflight()} />

          <Descriptions size="small" column={3} bordered title="Estimated from this service's own history">
            <Descriptions.Item label="Tokens">
              {estimate ? formatTokens(estimate.tokens) : "no history yet"}
            </Descriptions.Item>
            <Descriptions.Item label="Wall clock">
              {estimate ? formatDuration(estimate.durationMs) : "—"}
            </Descriptions.Item>
            <Descriptions.Item label="Cost">{estimate ? formatCost(estimate.costUsd) : "—"}</Descriptions.Item>
            <Descriptions.Item label="Based on" span={3}>
              {estimate
                ? `${estimate.basedOnRuns} completed run(s), counting only the stages this run will execute`
                : "This service has not completed a review yet, so there is nothing to estimate from."}
            </Descriptions.Item>
          </Descriptions>

          {validated && warnings.length > 0 ? (
            <Checkbox checked={acknowledged} onChange={event => void actions.acknowledge(event.target.checked)}>
              I understand the {warnings.length} warning(s) above and want to run anyway — they are recorded with the
              run.
            </Checkbox>
          ) : null}

          <Space>
            <Tooltip title={blocked ? "Fix the problems listed above first" : undefined}>
              <Button
                type="primary"
                size="large"
                icon={<PlayCircleOutlined />}
                loading={submitting}
                disabled={blocked}
                onClick={() => void actions.submit()}
              >
                Run review
              </Button>
            </Tooltip>
            <Button icon={<SafetyCertificateOutlined />} loading={validating} onClick={() => void actions.validate()}>
              Validate
            </Button>
          </Space>
        </Space>
      </Card>
    </Space>
  );
}
