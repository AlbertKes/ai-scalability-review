import {Loading, Module, register} from "@wonder/core-fe";
import {ReviewAJAXWebService} from "@/service/ReviewAJAXWebService";
import type {RootState} from "type/state";
import {initialState} from "./state";
import {LOADING_COMPARE} from "./type";

const CANDIDATE_LIMIT = 100;

class CompareModule extends Module<RootState, "Compare"> {
  async onEnter() {
    this.setState(initialState);
  }

  async start(jobIdA: string | null, jobIdB: string | null) {
    this.setState({jobIdA, jobIdB});
    await this.loadCandidates(jobIdA);
    if (jobIdA && jobIdB) await this.compare();
  }

  /** Only runs of the same service are offered — comparing two services' scores means nothing. */
  async loadCandidates(jobId: string | null) {
    let serviceId: string | null = null;
    if (jobId) {
      const response = await ReviewAJAXWebService.getJob(jobId);
      serviceId = response.view.service_id;
    }
    const list = await ReviewAJAXWebService.listJob({
      service_id: serviceId,
      status: "COMPLETED",
      limit: CANDIDATE_LIMIT,
      skip: 0,
    });
    this.setState({candidates: list.jobs});
  }

  async select(side: "a" | "b", jobId: string) {
    if (side === "a") this.setState({jobIdA: jobId});
    else this.setState({jobIdB: jobId});
    if (side === "a") await this.loadCandidates(jobId);
    const {jobIdA, jobIdB} = this.state;
    if (jobIdA && jobIdB) await this.compare();
  }

  @Loading(LOADING_COMPARE)
  async compare() {
    const {jobIdA, jobIdB} = this.state;
    if (!jobIdA || !jobIdB) return;
    const result = await ReviewAJAXWebService.compareRun({job_id_a: jobIdA, job_id_b: jobIdB});
    this.setState({result});
  }
}

export const module = register(new CompareModule("Compare", initialState));
export const actions = module.getActions();
