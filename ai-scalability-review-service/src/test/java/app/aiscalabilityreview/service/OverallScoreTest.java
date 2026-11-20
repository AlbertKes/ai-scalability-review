package app.aiscalabilityreview.service;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class OverallScoreTest {
    @Test
    void threeRedIsRed() {
        assertThat(OverallScore.of(Arrays.asList("RED", "RED", "GREEN", "RED", "GREEN", "GREEN"))).isEqualTo("RED");
    }

    @Test
    void twoRedIsYellow() {
        assertThat(OverallScore.of(Arrays.asList("RED", "GREEN", "RED", "GREEN", "GREEN"))).isEqualTo("YELLOW");
    }

    @Test
    void moreThanTwoYellowIsYellow() {
        assertThat(OverallScore.of(Arrays.asList("YELLOW", "YELLOW", "YELLOW", "GREEN", "GREEN"))).isEqualTo("YELLOW");
    }

    @Test
    void oneRedAndOneYellowIsGreen() {
        assertThat(OverallScore.of(Arrays.asList("RED", "YELLOW", "GREEN", "GREEN", "GREEN"))).isEqualTo("GREEN");
    }

    @Test
    void twoYellowIsGreen() {
        assertThat(OverallScore.of(Arrays.asList("YELLOW", "YELLOW", "GREEN"))).isEqualTo("GREEN");
    }

    @Test
    void unknownIsNotReportedAsGreen() {
        assertThat(OverallScore.of(Arrays.asList("UNKNOWN", "GREEN"))).isEqualTo("UNKNOWN");
    }

    @Test
    void validationVerdictCountsAsADimension() {
        assertThat(OverallScore.ofValidation("FAIL")).isEqualTo("RED");
        assertThat(OverallScore.ofValidation("CONDITIONAL PASS")).isEqualTo("YELLOW");
        assertThat(OverallScore.ofValidation("PASS")).isEqualTo("GREEN");
        assertThat(OverallScore.ofValidation(null)).isNull();
        assertThat(OverallScore.of(Arrays.asList("RED", "RED", "GREEN", "GREEN", "GREEN", OverallScore.ofValidation("FAIL")))).isEqualTo("RED");
        assertThat(OverallScore.of(Arrays.asList("RED", "GREEN", "GREEN", "GREEN", "GREEN", OverallScore.ofValidation("FAIL")))).isEqualTo("YELLOW");
    }
}
