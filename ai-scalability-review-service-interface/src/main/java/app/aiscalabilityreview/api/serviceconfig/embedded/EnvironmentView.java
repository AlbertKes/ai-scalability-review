package app.aiscalabilityreview.api.serviceconfig.embedded;

import core.framework.api.json.Property;

public enum EnvironmentView {
    @Property(name = "DEV")
    DEV,
    @Property(name = "UAT")
    UAT,
    @Property(name = "PROD")
    PROD
}
