import {Loading, Module, register} from "@wonder/core-fe";
import {LocalReviewAJAXWebService} from "@/service/LocalReviewAJAXWebService";
import {SettingsAJAXWebService} from "@/service/SettingsAJAXWebService";
import {successToast} from "@/components/Message";
import {actions as mainActions} from "@/page/main/module";
import type {RootState} from "type/state";
import type {SettingsView} from "type/api";
import {initialState} from "./state";
import {LOADING_SETTINGS, PREFLIGHTING_SETTINGS, SAVING_SETTINGS} from "./type";

class SettingsModule extends Module<RootState, "Settings"> {
  @Loading(LOADING_SETTINGS)
  async onEnter() {
    const [settings, presets] = await Promise.all([
      SettingsAJAXWebService.get(),
      SettingsAJAXWebService.listPreset({}),
    ]);
    this.setState({settings, presets: presets.presets});
  }

  async edit(patch: Partial<SettingsView>) {
    if (!this.state.settings) return;
    this.setState({settings: {...this.state.settings, ...patch}});
  }

  async editPrice(model: string, patch: Partial<SettingsView["model_prices"] extends Array<infer T> ? T : never>) {
    const {settings} = this.state;
    if (!settings?.model_prices) return;
    this.setState({
      settings: {
        ...settings,
        model_prices: settings.model_prices.map(price => (price.model === model ? {...price, ...patch} : price)),
      },
    });
  }

  async editPrefs(patch: Partial<NonNullable<SettingsView["notification_prefs"]>>) {
    const {settings} = this.state;
    if (!settings) return;
    this.setState({
      settings: {
        ...settings,
        notification_prefs: {...(settings.notification_prefs ?? {}), ...patch} as SettingsView["notification_prefs"],
      },
    });
  }

  @Loading(SAVING_SETTINGS)
  async save() {
    const {settings} = this.state;
    if (!settings) return;
    const saved = await SettingsAJAXWebService.update(settings);
    this.setState({settings: saved});
    await mainActions.applySettings(saved);
    successToast("Settings saved");
  }

  /** Browser notifications are opt-in and need a user gesture, so the permission is asked for here. */
  async enableBrowserNotifications() {
    await mainActions.requestBrowserPermission();
    await this.editPrefs({browser_enabled: true});
  }

  @Loading(PREFLIGHTING_SETTINGS)
  async runPreflight() {
    const response = await LocalReviewAJAXWebService.preflight({
      output_base_dir: this.state.settings?.output_base_dir,
    });
    this.setState({preflight: response.checks});
  }

  async deletePreset(presetId: string) {
    await SettingsAJAXWebService.deletePreset(presetId);
    const presets = await SettingsAJAXWebService.listPreset({});
    this.setState({presets: presets.presets});
  }
}

export const module = register(new SettingsModule("Settings", initialState));
export const actions = module.getActions();
