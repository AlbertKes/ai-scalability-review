package app.aiscalabilityreview.api.preset;

import core.framework.api.json.Property;

public class PresetActionResponse {
    @Property(name = "preset_id")
    public String presetId;

    @Property(name = "message")
    public String message;
}
