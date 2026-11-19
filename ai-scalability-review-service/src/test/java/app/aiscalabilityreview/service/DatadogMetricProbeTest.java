package app.aiscalabilityreview.service;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DatadogMetricProbeTest {
    @Test
    void readsTheUsAllServerVariableNames() {
        DatadogMetricProbe.Credentials credentials = DatadogMetricProbe.readCredentials(
            Map.of("DD_API_KEY", "api", "DD_APP_KEY", "app", "DD_SITE", "us5.datadoghq.com"));

        assertThat(credentials).isEqualTo(new DatadogMetricProbe.Credentials("api", "app", "us5.datadoghq.com"));
    }

    @Test
    void stillReadsThePreviousServerVariableNames() {
        DatadogMetricProbe.Credentials credentials = DatadogMetricProbe.readCredentials(
            Map.of("DATADOG_API_KEY", "api", "DATADOG_APP_KEY", "app"));

        assertThat(credentials).isEqualTo(new DatadogMetricProbe.Credentials("api", "app", "datadoghq.com"));
    }

    @Test
    void needsBothKeys() {
        assertThat(DatadogMetricProbe.readCredentials(Map.of("DD_API_KEY", "api"))).isNull();
    }
}
