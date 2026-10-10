import {Interval, Loading, Module, register} from "@wonder/core-fe";
import {LocalReviewAJAXWebService} from "@/service/LocalReviewAJAXWebService";
import {ReviewAJAXWebService} from "@/service/ReviewAJAXWebService";
import type {RootState} from "type/state";
import {initialState} from "./state";
import {LOADING_DASHBOARD} from "./type";

const POLL_SECONDS = 5;

class DashboardModule extends Module<RootState, "Dashboard"> {
  @Loading(LOADING_DASHBOARD)
  async onEnter() {
    await Promise.all([this.load(), this.loadPreflight()]);
  }

  /** Only the live section is worth polling; the rest changes when a run finishes, which it covers. */
  @Interval(POLL_SECONDS)
  async onTick() {
    await this.load();
  }

  async load() {
    const [running, recent, overBudget, usage] = await Promise.all([
      ReviewAJAXWebService.listJob({status: "RUNNING,QUEUED,PENDING", limit: 10, skip: 0}),
      ReviewAJAXWebService.listJob({limit: 8, skip: 0}),
      ReviewAJAXWebService.listJob({within_budget: false, limit: 5, skip: 0}),
      ReviewAJAXWebService.usage({group_by: "stage"}),
    ]);
    this.setState({
      running: running.jobs,
      recent: recent.jobs,
      overBudget: overBudget.jobs,
      usage,
    });
  }

  async loadPreflight() {
    const response = await LocalReviewAJAXWebService.preflight({});
    this.setState({preflight: response.checks});
  }
}

export const module = register(new DashboardModule("Dashboard", initialState));
export const actions = module.getActions();
