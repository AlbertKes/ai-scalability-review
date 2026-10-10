import {Loading, Module, register} from "@wonder/core-fe";
import {ReviewAJAXWebService} from "@/service/ReviewAJAXWebService";
import type {RootState} from "type/state";
import {initialState} from "./state";
import {LOADING_SERVICES} from "./type";

class ServicesModule extends Module<RootState, "Services"> {
  @Loading(LOADING_SERVICES)
  async onEnter() {
    const response = await ReviewAJAXWebService.listService();
    this.setState({services: response.services});
  }
}

export const module = register(new ServicesModule("Services", initialState));
export const actions = module.getActions();
