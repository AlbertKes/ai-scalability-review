package app.aiscalabilityreview.api.serviceconfig.embedded;

import core.framework.api.json.Property;

public enum HPATypeView {
    @Property(name = "CPU")
    CPU,
    @Property(name = "CUSTOM_METRICS")
    CUSTOM_METRICS,
    @Property(name = "NONE")
    NONE
}
