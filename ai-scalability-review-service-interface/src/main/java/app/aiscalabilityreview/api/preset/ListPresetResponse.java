package app.aiscalabilityreview.api.preset;

import core.framework.api.json.Property;

import java.util.List;

public class ListPresetResponse {
    @Property(name = "presets")
    public List<PresetView> presets;
}
