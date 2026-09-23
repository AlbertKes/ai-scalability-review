package app.aiscalabilityreview.api.serviceconfig.embedded;

import core.framework.api.json.Property;

public enum AIModelView {
    @Property(name = "CLAUDE_OPUS_5")
    CLAUDE_OPUS_5,
    @Property(name = "GEMINI_3_5_FLASH")
    GEMINI_3_5_FLASH,
    @Property(name = "GEMINI_3_1_FLASH_LITE")
    GEMINI_3_1_FLASH_LITE
}
