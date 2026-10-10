import {Loading, Module, register} from "@wonder/core-fe";
import {ReviewAJAXWebService} from "@/service/ReviewAJAXWebService";
import type {RootState} from "type/state";
import {initialState} from "./state";
import {LOADING_SERVICE} from "./type";

const HISTORY_LIMIT = 50;

class ServiceDetailModule extends Module<RootState, "ServiceDetail", {serviceId: string}> {
  onPathnameMatched(params: {serviceId: string}) {
    this.setState({...initialState, serviceId: params.serviceId});
    if (params.serviceId) void this.load(params.serviceId);
  }

  @Loading(LOADING_SERVICE)
  async load(serviceId: string) {
    const response = await ReviewAJAXWebService.listJob({service_id: serviceId, limit: HISTORY_LIMIT, skip: 0});
    this.setState({runs: response.jobs});
  }
}

export const module = register(new ServiceDetailModule("ServiceDetail", initialState));
export const actions = module.getActions();
