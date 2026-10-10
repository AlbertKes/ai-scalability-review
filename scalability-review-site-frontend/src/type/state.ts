import type {State} from "@wonder/core-fe";
import type {State as CompareState} from "@/page/Compare/type";
import type {State as DashboardState} from "@/page/Dashboard/type";
import type {State as MainState} from "@/page/main/type";
import type {State as NewRunState} from "@/page/NewRun/type";
import type {State as RunDetailState} from "@/page/RunDetail/type";
import type {State as RunsState} from "@/page/Runs/type";
import type {State as ServiceDetailState} from "@/page/ServiceDetail/type";
import type {State as ServicesState} from "@/page/Services/type";
import type {State as SettingsState} from "@/page/Settings/type";
import type {State as UsageState} from "@/page/Usage/type";

export interface RootState extends State {
  app: {
    main: MainState;
    Dashboard: DashboardState;
    Runs: RunsState;
    NewRun: NewRunState;
    RunDetail: RunDetailState;
    Compare: CompareState;
    Services: ServicesState;
    ServiceDetail: ServiceDetailState;
    Usage: UsageState;
    Settings: SettingsState;
  };
}
