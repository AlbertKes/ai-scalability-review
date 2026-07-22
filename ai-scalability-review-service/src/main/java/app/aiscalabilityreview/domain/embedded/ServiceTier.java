package app.aiscalabilityreview.domain.embedded;

import core.framework.mongo.MongoEnumValue;

public enum ServiceTier {
    @MongoEnumValue("TIER_1")
    TIER_1,
    @MongoEnumValue("TIER_2")
    TIER_2,
    @MongoEnumValue("TIER_3")
    TIER_3
}
