package app.aiscalabilityreview.web;

import app.aiscalabilityreview.api.SettingsWebService;
import app.aiscalabilityreview.api.preset.ListPresetRequest;
import app.aiscalabilityreview.api.preset.ListPresetResponse;
import app.aiscalabilityreview.api.preset.PresetActionResponse;
import app.aiscalabilityreview.api.preset.PresetView;
import app.aiscalabilityreview.api.preset.SavePresetRequest;
import app.aiscalabilityreview.api.settings.SettingsView;
import app.aiscalabilityreview.service.AppSettingsService;
import app.aiscalabilityreview.service.PresetService;
import app.aiscalabilityreview.service.builder.SettingsViewBuilder;
import core.framework.inject.Inject;

public class SettingsWebServiceImpl implements SettingsWebService {
    @Inject
    AppSettingsService appSettingsService;
    @Inject
    PresetService presetService;

    @Override
    public SettingsView get() {
        return SettingsViewBuilder.toView(appSettingsService.get());
    }

    @Override
    public SettingsView update(SettingsView request) {
        return SettingsViewBuilder.toView(appSettingsService.update(SettingsViewBuilder.toDomain(request)));
    }

    @Override
    public ListPresetResponse listPreset(ListPresetRequest request) {
        return presetService.list(request);
    }

    @Override
    public PresetView savePreset(SavePresetRequest request) {
        return presetService.save(request);
    }

    @Override
    public PresetActionResponse deletePreset(String presetId) {
        return presetService.delete(presetId);
    }
}
