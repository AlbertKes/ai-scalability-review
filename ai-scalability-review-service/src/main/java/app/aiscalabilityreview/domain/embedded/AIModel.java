package app.aiscalabilityreview.domain.embedded;

import core.framework.mongo.MongoEnumValue;

public enum AIModel {
    @MongoEnumValue("CLAUDE_SONNET_4_6")
    CLAUDE_SONNET_4_6,
    @MongoEnumValue("GEMINI_2_5_PRO")
    GEMINI_2_5_PRO


}
