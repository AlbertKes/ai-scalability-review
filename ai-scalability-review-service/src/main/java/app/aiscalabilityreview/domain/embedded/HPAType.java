package app.aiscalabilityreview.domain.embedded;

import core.framework.mongo.MongoEnumValue;

public enum HPAType {
    @MongoEnumValue("CPU")
    CPU,
    @MongoEnumValue("CUSTOM_METRICS")
    CUSTOM_METRICS,
    @MongoEnumValue("NONE")
    NONE
}
