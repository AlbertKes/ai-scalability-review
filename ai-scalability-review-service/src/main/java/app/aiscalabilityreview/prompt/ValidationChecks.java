package app.aiscalabilityreview.prompt;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
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
    /** Compares the report with the collected metrics and re-reads the infra repository. */
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

    /**
     * The review's five scoring dimensions, which are also what the validation verdict counts.
     *
     * <p>Eight checks and one-FAIL-fails-everything made every report FAIL, however small the slip
     * (a single wrong recommendation, a single mis-copied number). The verdict now asks how many
     * <em>dimensions</em> of the review the validator found wrong: three or more is a FAIL, two is a
     * yellow, one or none is green. Each finding therefore names the dimension it is about, and a
     * finding that does not (or names one that does not exist) is placed by its own wording.
     */
    public enum Dimension {
        TRAFFIC("Traffic", "Traffic & Throughput",
            List.of("rps", "traffic", "throughput", "kafka", "consumer lag", "partition", "week-over-week", "growth", "volume", "hits")),
        LATENCY("Latency", "Latency & Thread Concurrency",
            List.of("latency", "p99", "p95", "p50", "timeout", "thread", "semaphore", "blocking", "concurrency")),
        ERRORS("Errors", "Error Rates & Stability",
            List.of("error rate", "restart", "oomkill", "slo", "5xx", "exception", "stability", "failure")),
        RESOURCES("Resources", "Resource Saturation",
            List.of("cpu", "memory", "hpa", "replicas", "node pool", "autoscal", "limit", "request")),
        PERSISTENCE("Persistence", "Persistence Layer",
            List.of("mysql", "mongo", "atlas", "database", "replication", "storage", "table", "index", "query targeting",
                "slow quer", "connection pool", "connections", "jdbc", "sql", "redis"));

        private final String tag;
        private final String label;
        private final List<String> keywords;

        Dimension(String tag, String label, List<String> keywords) {
            this.tag = tag;
            this.label = label;
            this.keywords = keywords;
        }

        public String tag() {
            return tag;
        }

        public String label() {
            return label;
        }

        /** What the model wrote after {@code Dimension:}, or null when it is none of the five (e.g. {@code General}). */
        public static Dimension fromTag(String text) {
            if (text == null) return null;
            String lower = text.strip().toLowerCase(Locale.ROOT);
            for (Dimension dimension : values()) {
                if (lower.startsWith(dimension.tag.toLowerCase(Locale.ROOT))) return dimension;
            }
            return null;
        }

        /**
         * The dimension a finding is about judged from its words, for a finding that was not tagged.
         * The title is tried first because the body quotes sources and units that mention everything.
         */
        public static Dimension infer(String title, String body) {
            Dimension fromTitle = firstMatch(title);
            return fromTitle != null ? fromTitle : firstMatch(body);
        }

        /** The dimension whose keyword appears earliest in the text, so the subject beats a passing mention. */
        private static Dimension firstMatch(String text) {
            if (text == null) return null;
            String lower = text.toLowerCase(Locale.ROOT);
            Dimension best = null;
            int bestIndex = Integer.MAX_VALUE;
            for (Dimension dimension : values()) {
                for (String keyword : dimension.keywords) {
                    int index = lower.indexOf(keyword);
                    if (index >= 0 && index < bestIndex) {
                        best = dimension;
                        bestIndex = index;
                    }
                }
            }
            return best;
        }

        /** The tag values a session is told to choose from. */
        public static String tagList() {
            return Arrays.stream(values()).map(Dimension::tag).collect(Collectors.joining(" | ")) + " | General";
        }
    }

    /** @param number the fixed check number, which is also its position in the merged report */
    public record Check(int number, String name) {
    }
}
