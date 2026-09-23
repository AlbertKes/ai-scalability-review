package app.aiscalabilityreview.domain.embedded;

import core.framework.mongo.MongoEnumValue;

public enum AIModel {
    @MongoEnumValue("CLAUDE_OPUS_5")
    CLAUDE_OPUS_5,
    @MongoEnumValue("GEMINI_3_5_FLASH")
    GEMINI_3_5_FLASH,
    @MongoEnumValue("GEMINI_3_1_FLASH_LITE")
    GEMINI_3_1_FLASH_LITE
}
