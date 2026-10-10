import type {PreflightResponse$CheckView, PresetView, SettingsView} from "type/api";

export const LOADING_SETTINGS = "@@settings/loading";
export const SAVING_SETTINGS = "@@settings/saving";
export const PREFLIGHTING_SETTINGS = "@@settings/preflight";

export interface State {
  settings: SettingsView | null;
  presets: PresetView[];
  preflight: PreflightResponse$CheckView[];
}
