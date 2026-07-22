package app.aiscalabilityreview.domain.embedded;

import core.framework.mongo.MongoEnumValue;

public enum Environment {
    @MongoEnumValue("DEV")
    DEV,
    @MongoEnumValue("UAT")
    UAT,
    @MongoEnumValue("PROD")
    PROD
}
