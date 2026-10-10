import {useModuleState} from "@wonder/core-fe";
import type {RootState} from "type/state";

/**
 * Typed wrapper over the framework's state hook, so a page reads its own slice by name and gets the
 * right shape back without a cast at every call site.
 */
export function useRuntimeState<M extends keyof RootState["app"]>(moduleName: M): RootState["app"][M] {
  return useModuleState<RootState["app"], M, keyof RootState["app"][M]>(moduleName) as RootState["app"][M];
}
