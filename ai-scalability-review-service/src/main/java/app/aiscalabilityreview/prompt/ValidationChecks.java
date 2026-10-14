package app.aiscalabilityreview.prompt;

import java.util.List;
import java.util.stream.Collectors;

/**
 * The eight validation checks, their fixed names, and which session owns each one.
 *
 * <p>One list, read from both ends: the prompt builder turns a group into the exact section headers
 * that session must emit, and
 * {@link app.aiscalabilityreview.service.ValidationReportAssembler} uses the same list to order the
 * merged report and to decide which checks are missing from it. Splitting validation across three
 * sessions only works if both ends agree on the names, so there is one place that states them.
 */
public final class ValidationChecks {
    public static final List<Check> ALL = List.of(
        new Check(1, "Config Accuracy"),
        new Check(2, "Metric Numerical Fidelity"),
        new Check(3, "Cross-Source Metric Consistency"),
        new Check(4, "Scoring Accuracy"),
        new Check(5, "Capacity Projection Math"),
        new Check(6, "NOT_COLLECTED Authenticity"),
        new Check(7, "Format & Attribution Completeness"),
        new Check(8, "Recommendation Soundness"));

    /** Answerable from the report alone — no data source, no repository, no MCP server. */
    public static final List<Integer> STATIC_GROUP = List.of(3, 4, 5, 7);
    /** Re-queries Datadog and Azure and re-reads the infra repository. */
    public static final List<Integer> DATA_GROUP = List.of(1, 2, 6);
    /** Re-reads the application source to re-verify the advice. */
    public static final List<Integer> ADVICE_GROUP = List.of(8);

    /** The header line of one check section, with {@code status} already filled in. */
    public static String header(int number, String status) {
        return "## Check " + number + " — " + name(number) + ": " + status;
    }

    /** The headers one session must emit, as a markdown list for its prompt. */
    public static String headers(List<Integer> group) {
        return group.stream().map(number -> "- `" + header(number, "<STATUS>") + "`")
            .collect(Collectors.joining("\n"));
    }

    public static String name(int number) {
        return ALL.stream().filter(check -> check.number == number).findFirst()
            .map(Check::name).orElse("Check " + number);
    }

    private ValidationChecks() {
    }

    /** @param number the fixed check number, which is also its position in the merged report */
    public record Check(int number, String name) {
    }
}
