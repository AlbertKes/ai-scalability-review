package app.aiscalabilityreview.service;

import app.aiscalabilityreview.service.ValidationReportAssembler.AssemblyParams;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The summary table and the verdict used to be written by the same model turn that wrote the
 * findings, which is how a report could carry a PASS row over a section containing a FAIL. They are
 * computed here now, so what these tests pin down is that the computation follows the findings.
 */
class ValidationReportAssemblerTest {
    private static final String STATIC_PARTIAL = """
        ## Check 3 — Cross-Source Metric Consistency: PASS

        ### Findings

        **[PASS]** Avg RPS stated consistently
          - Reported: `12.4 RPS`
          - Actual: `12.4 RPS` [Source: report 3.1]
          - Detail: Matches the executive summary.

        ## Check 4 — Scoring Accuracy: WARNING

        ### Findings

        **[WARNING]** Dimension 4 computes one level lower
          - Reported: `YELLOW`
          - Actual: `GREEN`
          - Detail: CPU avg 31% is under the YELLOW threshold.

        ## Check 5 — Capacity Projection Math: PASS

        ### Findings

        **[PASS]** Baseline weekly hits
          - Reported: `7.5M`
          - Actual: `7.5M`
          - Detail: Within 1%.

        ## Check 7 — Format & Attribution Completeness: PASS

        ### Findings

        **[PASS]** All 8 sections present
          - Reported: `8`
          - Actual: `8`
          - Detail: In order.
        """;

    private static final String DATA_PARTIAL = """
        ## Check 1 — Config Accuracy: FAIL

        ### Findings

        **[FAIL]** HPA maxReplicas
          - Reported: `20`
          - Actual: `15` [Source: code -> hpa.yaml:L18]
          - Detail: Report overstates maxReplicas by 5.

        ## Check 2 — Metric Numerical Fidelity: PASS

        ### Findings

        **[PASS]** Avg RPS
          - Reported: `12.4`
          - Actual: `12.1`
          - Detail: Within 20%.

        ## Check 6 — NOT_COLLECTED Authenticity: SKIPPED

        ### Findings

        **[SKIPPED]** No NOT_COLLECTED markers in the report
          - Reported: `none`
          - Actual: `none`
          - Detail: Nothing to re-query.
        """;

    private static final String ADVICE_PARTIAL = """
        ## Check 8 — Recommendation Soundness: PASS

        ### Findings

        **[PASS]** Recommendation 1 — add an index on cart.user_id
          - Reported: `no index`
          - Actual: `no index` [Source: V12__cart.sql]
          - Detail: Premise holds and the action is not already implemented.
        """;

    private ValidationReportAssembler assembler;

    @BeforeEach
    void createValidationReportAssembler() {
        assembler = new ValidationReportAssembler();
    }

    @Test
    void summaryTableCarriesOneRowPerCheckInNumberOrder() {
        String report = merge(STATIC_PARTIAL, DATA_PARTIAL, ADVICE_PARTIAL);

        assertThat(report)
            .contains("| 1 | Config Accuracy | FAIL | 1 issue(s) |")
            .contains("| 2 | Metric Numerical Fidelity | PASS | 0 issue(s) |")
            .contains("| 4 | Scoring Accuracy | WARNING | 1 issue(s) |")
            .contains("| 6 | NOT_COLLECTED Authenticity | SKIPPED | 0 issue(s) |")
            .contains("| 8 | Recommendation Soundness | PASS | 0 issue(s) |");
        assertThat(report.indexOf("| 1 | Config")).isLessThan(report.indexOf("| 8 | Recommendation"));
    }

    /** The groups finish in any order and write only what they own; the report is still check 1 to 8. */
    @Test
    void checkSectionsAreOrderedByNumberWhateverOrderThePartialsArriveIn() {
        String report = merge(ADVICE_PARTIAL, STATIC_PARTIAL, DATA_PARTIAL);

        assertThat(report.indexOf("## Check 1 — Config Accuracy"))
            .isLessThan(report.indexOf("## Check 3 — Cross-Source Metric Consistency"));
        assertThat(report.indexOf("## Check 7 — Format & Attribution Completeness"))
            .isLessThan(report.indexOf("## Check 8 — Recommendation Soundness"));
    }

    /** The reason for the rule: a single wrong claim used to turn the whole report FAIL. */
    @Test
    void failuresInOneDimensionStillLeaveAGreenVerdictButAreListed() {
        assertThat(merge(STATIC_PARTIAL, DATA_PARTIAL, ADVICE_PARTIAL))
            .contains("## Overall Validation Verdict: PASS")
            .contains("**1 of 5 dimensions failed**")
            .contains("| Resource Saturation | FAIL | 1 | 1 |")
            .contains("- **[Check 1]** HPA maxReplicas")
            .contains("- **[Check 4]** Dimension 4 computes one level lower");
    }

    @Test
    void twoFailedDimensionsAreYellow() {
        assertThat(merge(STATIC_PARTIAL, DATA_PARTIAL, adviceFailing("Persistence")))
            .contains("## Overall Validation Verdict: CONDITIONAL PASS")
            .contains("**2 of 5 dimensions failed**");
    }

    @Test
    void threeFailedDimensionsFailTheReport() {
        String traffic = adviceFailing("Traffic").replace("## Check 8", "## Check 3").replace("Recommendation Soundness", "Cross-Source Metric Consistency");

        assertThat(merge(traffic, DATA_PARTIAL, adviceFailing("Persistence")))
            .contains("## Overall Validation Verdict: FAIL")
            .contains("**3 of 5 dimensions failed**");
    }

    /** Two FAIL findings about the same dimension are one failed dimension. */
    @Test
    void manyFailuresInOneDimensionCountOnce() {
        String twoInOne = DATA_PARTIAL.replace("Metric Numerical Fidelity: PASS", "Metric Numerical Fidelity: FAIL")
            .replace("**[PASS]** Avg RPS", "**[FAIL]** CPU limit");

        assertThat(merge(STATIC_PARTIAL, twoInOne, ADVICE_PARTIAL))
            .contains("## Overall Validation Verdict: PASS")
            .contains("**1 of 5 dimensions failed**");
    }

    /** A finding the model did not tag is placed by its own wording. */
    @Test
    void anUntaggedFindingIsPlacedByItsWording() {
        String untagged = """
            ## Check 8 — Recommendation Soundness: FAIL

            ### Findings

            **[FAIL]** Recommendation 1 — add an index on cart.user_id
              - Reported: `no index`
              - Actual: `index exists` [Source: V12__cart.sql]
              - Detail: The MySQL migration already creates it.
            """;

        assertThat(merge("", "", untagged)).contains("| Persistence Layer | FAIL | 1 | 0 |");
    }

    /** A finding the model calls General belongs to no dimension and cannot, alone, fail the report. */
    @Test
    void aGeneralFindingCountsTowardNoDimension() {
        String general = adviceFailing("General");

        assertThat(merge(STATIC_PARTIAL, DATA_PARTIAL, general))
            .contains("**1 of 5 dimensions failed**")
            .contains("1 FAIL finding(s) belong to no single dimension");
    }

    @Test
    void warningsDoNotMoveTheVerdict() {
        String noFail = DATA_PARTIAL.replace("Config Accuracy: FAIL", "Config Accuracy: PASS")
            .replace("**[FAIL]**", "**[PASS]**");

        assertThat(merge(STATIC_PARTIAL, noFail, ADVICE_PARTIAL))
            .contains("## Overall Validation Verdict: PASS")
            .doesNotContain("### Validation Notes");
    }

    @Test
    void everythingPassingAndNothingMissingIsAPass() {
        String allPass = STATIC_PARTIAL.replace("Scoring Accuracy: WARNING", "Scoring Accuracy: PASS")
            .replace("**[WARNING]**", "**[PASS]**");
        String noFail = DATA_PARTIAL.replace("Config Accuracy: FAIL", "Config Accuracy: PASS")
            .replace("**[FAIL]**", "**[PASS]**");

        assertThat(merge(allPass, noFail, ADVICE_PARTIAL)).contains("## Overall Validation Verdict: PASS");
    }

    /**
     * A group whose session failed leaves no partial. Its checks must say so rather than read as
     * passing, and the verdict must not come out clean on checks that never ran.
     */
    @Test
    void aGroupThatDidNotRunIsReportedAsUncheckedAndHoldsTheVerdictBelowPass() {
        String report = merge(STATIC_PARTIAL, "", ADVICE_PARTIAL);

        assertThat(report)
            .contains("| 1 | Config Accuracy | SKIPPED | 0 issue(s) — not run")
            .contains("| 6 | NOT_COLLECTED Authenticity | SKIPPED | 0 issue(s) — not run")
            .contains("## Overall Validation Verdict: CONDITIONAL PASS")
            .contains("### Validation Notes")
            .contains("Checks 1, 2, 6 did not run");
    }

    /** Em dash, plain dash and a bold status are all shapes the model has produced. */
    @Test
    void sectionHeadersAreParsedWhicheverDashAndEmphasisTheModelUsed() {
        String oddly = """
            ## Check 8 - Recommendation Soundness: **FAIL**

            ### Findings

            **[FAIL]** Recommendation 2 is already implemented
              - Reported: `missing`
              - Actual: `present`
              - Detail: The semaphore is active with 5 permits.
            """;

        assertThat(merge("", "", oddly))
            .contains("| 8 | Recommendation Soundness | FAIL | 1 issue(s) |")
            .contains("| Latency & Thread Concurrency | FAIL | 1 | 0 |");
    }

    private String adviceFailing(String dimension) {
        return """
            ## Check 8 — Recommendation Soundness: FAIL

            ### Findings

            **[FAIL]** Recommendation 1 is wrong
              - Reported: `missing`
              - Actual: `present`
              - Dimension: DIMENSION
              - Detail: The source contradicts it.
            """.replace("DIMENSION", dimension);
    }

    private String merge(String... partials) {
        return assembler.merge(new AssemblyParams("wonder-cart-service", "prod", "/out/2026-10-13-review.md",
            "2026-10-13", "gemini-3.5-flash", "/out/2026-10-13-validation.md"), List.of(partials));
    }
}
