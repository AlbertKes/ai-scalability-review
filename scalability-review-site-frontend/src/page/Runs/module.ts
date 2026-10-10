import {Interval, Loading, Module, register} from "@wonder/core-fe";
import {LocalReviewAJAXWebService} from "@/service/LocalReviewAJAXWebService";
import {ReviewAJAXWebService} from "@/service/ReviewAJAXWebService";
import {isTerminal} from "@/constants/review";
import {successToast} from "@/components/Message";
import type {RootState} from "type/state";
import type {ListReviewJobRequest} from "type/api";
import {emptyFilter, initialState, loadSavedFilters, persistSavedFilters} from "./state";
import {LOADING_RUNS, type RunFilter, type SavedFilter} from "./type";

const REFRESH_SECONDS = 5;

const toRequest = (state: RootState["app"]["Runs"]): ListReviewJobRequest => {
  const {filter, page, pageSize, sort} = state;
  const list = (values: string[]) => (values.length > 0 ? values.join(",") : null);
  return {
    service_id: filter.service_id || null,
    status: list(filter.status),
    env: filter.env || null,
    operator: filter.operator || null,
    trigger_type: filter.trigger_type || null,
    keyword: filter.keyword || null,
    search_reports: filter.search_reports || null,
    from: filter.from || null,
    to: filter.to || null,
    overall_score: list(filter.overall_score),
    validation_verdict: list(filter.validation_verdict),
    within_budget: filter.within_budget,
    has_failed_stage: filter.has_failed_stage || null,
    label: filter.label || null,
    sort,
    limit: pageSize,
    skip: (page - 1) * pageSize,
  };
};

class RunsModule extends Module<RootState, "Runs"> {
  async onEnter() {
    this.setState({savedFilters: loadSavedFilters()});
  }

  /**
   * Only refreshes while something is actually running. A list of finished runs does not change on
   * its own, and polling it would be a query every five seconds for no new information.
   */
  @Interval(REFRESH_SECONDS)
  async onTick() {
    if (this.state.jobs.some(job => !isTerminal(job.status))) await this.load();
  }

  @Loading(LOADING_RUNS)
  async load() {
    const response = await ReviewAJAXWebService.listJob(toRequest(this.state));
    this.setState({jobs: response.jobs, total: response.total});
  }

  async applyFilter(filter: RunFilter) {
    this.setState({filter, page: 1});
    await this.load();
  }

  async resetFilter() {
    this.setState({filter: emptyFilter(), page: 1});
    await this.load();
  }

  async changePage(page: number, pageSize: number) {
    this.setState({page, pageSize});
    await this.load();
  }

  async changeSort(sort: string) {
    this.setState({sort, page: 1});
    await this.load();
  }

  async select(selectedJobIds: string[]) {
    this.setState({selectedJobIds});
  }

  async saveFilter(name: string) {
    const savedFilters: SavedFilter[] = [
      ...this.state.savedFilters.filter(item => item.name !== name),
      {name, filter: this.state.filter},
    ];
    persistSavedFilters(savedFilters);
    this.setState({savedFilters});
    successToast(`Saved filter "${name}"`);
  }

  async deleteSavedFilter(name: string) {
    const savedFilters = this.state.savedFilters.filter(item => item.name !== name);
    persistSavedFilters(savedFilters);
    this.setState({savedFilters});
  }

  async deleteRuns(jobIds: string[]) {
    for (const jobId of jobIds) {
      await LocalReviewAJAXWebService.delete(jobId);
    }
    successToast(`Deleted ${jobIds.length} run(s) and their artifacts`);
    this.setState({selectedJobIds: []});
    await this.load();
  }
}

export const module = register(new RunsModule("Runs", initialState));
export const actions = module.getActions();
