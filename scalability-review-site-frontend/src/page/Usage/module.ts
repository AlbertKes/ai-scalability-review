import {Loading, Module, register} from "@wonder/core-fe";
import {ReviewAJAXWebService} from "@/service/ReviewAJAXWebService";
import type {RootState} from "type/state";
import {initialState} from "./state";
import {LOADING_USAGE} from "./type";

const MS_PER_DAY = 24 * 60 * 60 * 1000;

class UsageModule extends Module<RootState, "Usage"> {
  async onEnter() {
    await this.load();
  }

  @Loading(LOADING_USAGE)
  async load() {
    const {groupBy, serviceId, days} = this.state;
    const usage = await ReviewAJAXWebService.usage({
      group_by: groupBy,
      service_id: serviceId || null,
      from: new Date(Date.now() - days * MS_PER_DAY).toISOString(),
    });
    this.setState({usage});
  }

  async setGroupBy(groupBy: string) {
    this.setState({groupBy});
    await this.load();
  }

  async setService(serviceId: string) {
    this.setState({serviceId});
    await this.load();
  }

  async setDays(days: number) {
    this.setState({days});
    await this.load();
  }
}

export const module = register(new UsageModule("Usage", initialState));
export const actions = module.getActions();
