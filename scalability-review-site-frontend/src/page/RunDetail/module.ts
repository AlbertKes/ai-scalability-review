import {Interval, Loading, Module, app, register} from "@wonder/core-fe";
import {LocalReviewAJAXWebService} from "@/service/LocalReviewAJAXWebService";
import {ReviewAJAXWebService} from "@/service/ReviewAJAXWebService";
import {successToast} from "@/components/Message";
import {isTerminal} from "@/constants/review";
import {RUNS_PATH} from "@/route";
import type {RootState} from "type/state";
import type {SaveFindingFeedbackRequest} from "type/api";
import {initialState} from "./state";
import {LOADING_RUN} from "./type";

const POLL_SECONDS = 3;
const METRICS_ARTIFACT = "metrics.md";
const NOT_COLLECTED_PATTERN = /NOT_COLLECTED:?\s*([^[\]\n]*)/g;

class RunDetailModule extends Module<RootState, "RunDetail", {jobId: string}> {
  onPathnameMatched(params: {jobId: string}) {
    this.setState(initialState);
    if (params.jobId) void this.load(params.jobId);
  }

  onDestroy() {
    this.setState(initialState);
  }

  /**
   * Polls only while the run is live, and never after it reaches a terminal state. The CLI writes
   * nothing to stdout until a stage ends, so stage granularity is the best fidelity available;
   * the elapsed timer in the UI is what makes a long stage look alive rather than stuck.
   */
  @Interval(POLL_SECONDS)
  async onTick() {
    const {job} = this.state;
    if (!job || isTerminal(job.status)) return;
    await this.refresh(job.job_id);
    if (this.state.logStage) await this.tailLog();
  }

  @Loading(LOADING_RUN)
  async load(jobId: string) {
    await this.refresh(jobId);
  }

  async refresh(jobId: string) {
    const [jobResponse, artifactResponse] = await Promise.all([
      ReviewAJAXWebService.getJob(jobId),
      ReviewAJAXWebService.listArtifact(jobId),
    ]);
    this.setState({
      job: jobResponse.view,
      artifacts: artifactResponse.artifacts,
      outputDir: artifactResponse.output_dir,
    });
    await Promise.all([this.loadReport(), this.loadValidation(), this.loadNotCollected()]);
  }

  async loadReport() {
    const {job} = this.state;
    if (!job?.report_id) return;
    const [report, feedback] = await Promise.all([
      ReviewAJAXWebService.getReport(job.report_id),
      ReviewAJAXWebService.listFindingFeedback(job.report_id),
    ]);
    this.setState({report: report.view, feedback: feedback.feedback});
  }

  async loadValidation() {
    const {job} = this.state;
    if (!job?.validation_result_id) return;
    const validation = await ReviewAJAXWebService.getValidation(job.validation_result_id);
    this.setState({validation});
  }

  /**
   * Counts the gaps in the collected data. A metric that could not be collected is not a zero, and
   * a report resting on one has to say so where the reader will see it.
   */
  async loadNotCollected() {
    const {job, artifacts} = this.state;
    if (!job || !artifacts.some(artifact => artifact.name === METRICS_ARTIFACT)) return;
    const content = await this.fetchArtifact(job.job_id, METRICS_ARTIFACT);
    const reasons: string[] = [];
    for (const match of content.matchAll(NOT_COLLECTED_PATTERN)) {
      reasons.push(match[1].trim() || "no reason given");
    }
    this.setState({notCollected: reasons});
  }

  async openPreview(name: string) {
    const {job} = this.state;
    if (!job) return;
    const content = await this.fetchArtifact(job.job_id, name);
    this.setState({preview: {name, content}});
  }

  async closePreview() {
    this.setState({preview: null});
  }

  async toggleSources() {
    this.setState({showSources: !this.state.showSources});
  }

  async selectLogStage(stage: string | null) {
    this.setState({logStage: stage, logContent: "", logOffset: 0, logComplete: true});
    if (stage) await this.tailLog();
  }

  async tailLog() {
    const {job, logStage, logOffset, logContent} = this.state;
    if (!job || !logStage) return;
    const response = await ReviewAJAXWebService.getStageLog(job.job_id, {stage: logStage, offset: logOffset});
    this.setState({
      logContent: logContent + response.content,
      logOffset: response.next_offset,
      logComplete: response.complete,
    });
  }

  async cancel() {
    const {job} = this.state;
    if (!job) return;
    const response = await LocalReviewAJAXWebService.cancel(job.job_id);
    successToast(response.message);
    await this.refresh(job.job_id);
  }

  async retry(fromStage: string | null, operator: string) {
    const {job} = this.state;
    if (!job) return;
    const response = await LocalReviewAJAXWebService.retry(job.job_id, {from_stage: fromStage, operator});
    successToast(`Retry started — ${response.output_dir}`);
    app.navigate(`/RunDetail/${response.job_id}`);
  }

  async updateRun(patch: {note?: string; labels?: string[]; baseline?: boolean}) {
    const {job} = this.state;
    if (!job) return;
    await LocalReviewAJAXWebService.update(job.job_id, patch);
    await this.refresh(job.job_id);
  }

  async remove() {
    const {job} = this.state;
    if (!job) return;
    const response = await LocalReviewAJAXWebService.delete(job.job_id);
    successToast(response.message);
    app.navigate(`/${RUNS_PATH}`);
  }

  async saveFeedback(request: SaveFindingFeedbackRequest) {
    const {job} = this.state;
    if (!job?.report_id) return;
    const response = await ReviewAJAXWebService.saveFindingFeedback(job.report_id, request);
    this.setState({feedback: response.feedback});
    successToast("Recorded");
  }

  private async fetchArtifact(jobId: string, name: string): Promise<string> {
    const response = await fetch(ReviewAJAXWebService.artifactURL(jobId, name));
    if (!response.ok) return "";
    return response.text();
  }
}

export const module = register(new RunDetailModule("RunDetail", initialState));
export const actions = module.getActions();
