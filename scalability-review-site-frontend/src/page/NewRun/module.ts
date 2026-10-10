import {Loading, Module, app, register} from "@wonder/core-fe";
import {LocalReviewAJAXWebService} from "@/service/LocalReviewAJAXWebService";
import {ReviewAJAXWebService} from "@/service/ReviewAJAXWebService";
import {SettingsAJAXWebService} from "@/service/SettingsAJAXWebService";
import {successToast, warningToast} from "@/components/Message";
import {RUN_DETAIL_PATH} from "@/route";
import type {RootState} from "type/state";
import type {GenerateLocalReviewRequest, LocalReviewRequestView, ReviewJobView} from "type/api";
import {emptyRequest, initialState} from "./state";
import {PREFLIGHTING, SUBMITTING, VALIDATING, type CostEstimate} from "./type";

const ESTIMATE_SAMPLE_SIZE = 5;
const SKIPPABLE_STAGES: Record<string, string> = {
  skip_code_analysis: "STAGE_1_CODE_ANALYSIS",
  skip_validation: "STAGE_5_VALIDATION",
};

class NewRunModule extends Module<RootState, "NewRun"> {
  async onEnter() {
    this.setState({...initialState, request: emptyRequest()});
    const settings = await SettingsAJAXWebService.get();
    this.setState({
      request: {
        ...this.state.request,
        operator: settings.default_operator ?? "",
        output_base_dir: settings.output_base_dir ?? "",
      },
    });
    await this.loadPresets();
  }

  async onDestroy() {
    this.setState(initialState);
  }

  async update(patch: Partial<LocalReviewRequestView>) {
    this.setState({request: {...this.state.request, ...patch}, validated: false, acknowledged: false});
  }

  async setLabels(labels: string[]) {
    this.setState({labels});
  }

  async loadPresets() {
    const response = await SettingsAJAXWebService.listPreset({});
    this.setState({presets: response.presets});
    const fallback = response.presets.find(preset => preset.is_default);
    if (fallback?.request && !this.state.request.service) await this.applyPreset(fallback.preset_id);
  }

  async applyPreset(presetId: string) {
    const preset = this.state.presets.find(item => item.preset_id === presetId);
    if (!preset?.request) return;
    this.setState({request: {...emptyRequest(), ...preset.request}, validated: false, acknowledged: false});
    await this.validate();
  }

  async savePreset(name: string) {
    const {request} = this.state;
    if (!request.service) {
      warningToast("A preset needs a service name");
      return;
    }
    await SettingsAJAXWebService.savePreset({
      preset_id: null,
      service_id: request.service,
      env: request.env,
      name,
      request,
      is_default: false,
      created_by: request.operator || "unknown",
    });
    successToast(`Saved preset "${name}"`);
    await this.loadPresets();
  }

  /**
   * Runs the whole rule set and keeps every problem it returns. Costs nothing and starts nothing,
   * so it can be called on every pause in typing.
   */
  @Loading(VALIDATING)
  async validate() {
    const response = await LocalReviewAJAXWebService.validateRequest(this.state.request);
    this.setState({errors: response.errors, warnings: response.warnings, validated: true});
  }

  @Loading(PREFLIGHTING)
  async runPreflight() {
    const {request} = this.state;
    const response = await LocalReviewAJAXWebService.preflight({
      service: request.service,
      env: request.env,
      domain: request.domain,
      local_app_repo_path: request.local_app_repo_path,
      local_infra_repo_path: request.local_infra_repo_path,
      output_base_dir: request.output_base_dir,
      mysql_host: request.mysql_host,
      mysql_db: request.mysql_db,
    });
    this.setState({preflight: response.checks});
  }

  async acknowledge(acknowledged: boolean) {
    this.setState({acknowledged});
  }

  /**
   * Validates first and submits only on a clean pass. Warnings do not block, but they have to be
   * acknowledged explicitly, and the acknowledgement travels with the run so a later reader knows
   * it started with known gaps.
   */
  @Loading(SUBMITTING)
  async submit() {
    const response = await LocalReviewAJAXWebService.validateRequest(this.state.request);
    this.setState({errors: response.errors, warnings: response.warnings, validated: true});

    if (response.errors.length > 0) {
      warningToast(`${response.errors.length} problem(s) must be fixed before this review can start`);
      return;
    }
    if (response.warnings.length > 0 && !this.state.acknowledged) {
      warningToast(`${response.warnings.length} warning(s) — tick "run anyway" to accept them`);
      return;
    }

    const request: GenerateLocalReviewRequest = {
      ...this.state.request,
      labels: this.state.labels.length > 0 ? this.state.labels : null,
      acknowledged_warnings: response.warnings.map(warning => warning.code),
    };
    const result = await LocalReviewAJAXWebService.generate(request);
    successToast(`Review started — writing to ${result.output_dir}`);
    app.navigate(`/${RUN_DETAIL_PATH}/${result.job_id}`);
  }

  /**
   * Estimates this run from this service's own history rather than from a generic figure, and only
   * counts the stages that will actually run.
   */
  async estimate() {
    const {request} = this.state;
    if (!request.service) {
      this.setState({estimate: null});
      return;
    }
    const response = await ReviewAJAXWebService.listJob({
      service_id: request.service,
      status: "COMPLETED",
      limit: ESTIMATE_SAMPLE_SIZE,
      skip: 0,
    });
    this.setState({estimate: buildEstimate(response.jobs, request)});
  }
}

function buildEstimate(jobs: ReviewJobView[], request: LocalReviewRequestView): CostEstimate | null {
  if (jobs.length === 0) return null;
  const skipped = new Set(
    Object.entries(SKIPPABLE_STAGES)
      .filter(([flag]) => Boolean(request[flag as keyof LocalReviewRequestView]))
      .map(([, stage]) => stage)
  );

  let tokens = 0;
  let durationMs = 0;
  let costUsd = 0;
  let priced = false;
  jobs.forEach(job => {
    (job.stages ?? []).forEach(stage => {
      if (skipped.has(stage.name)) return;
      tokens += stage.total_tokens ?? 0;
      durationMs += stage.duration_ms ?? 0;
      if (stage.estimated_cost_usd !== null && stage.estimated_cost_usd !== undefined) {
        costUsd += stage.estimated_cost_usd;
        priced = true;
      }
    });
  });

  return {
    basedOnRuns: jobs.length,
    tokens: Math.round(tokens / jobs.length),
    durationMs: Math.round(durationMs / jobs.length),
    costUsd: priced ? Number((costUsd / jobs.length).toFixed(4)) : null,
  };
}

export const module = register(new NewRunModule("NewRun", initialState));
export const actions = module.getActions();
