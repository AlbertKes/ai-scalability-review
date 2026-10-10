import {ajax} from "@wonder/core-fe";
import {
  ListPresetRequest,
  ListPresetResponse,
  PresetActionResponse,
  PresetView,
  SavePresetRequest,
  SettingsView,
} from "type/api";

export class SettingsAJAXWebService {
  static get(signal?: AbortSignal): Promise<SettingsView> {
    return ajax("GET", "/settings", {}, null, {signal});
  }

  static update(request: SettingsView): Promise<SettingsView> {
    return ajax("PUT", "/settings", {}, request);
  }

  static listPreset(request: ListPresetRequest, signal?: AbortSignal): Promise<ListPresetResponse> {
    return ajax("GET", "/preset", {}, request, {signal});
  }

  static savePreset(request: SavePresetRequest): Promise<PresetView> {
    return ajax("POST", "/preset", {}, request);
  }

  static deletePreset(presetId: string): Promise<PresetActionResponse> {
    return ajax("DELETE", "/preset/:presetId", {presetId}, null);
  }
}
