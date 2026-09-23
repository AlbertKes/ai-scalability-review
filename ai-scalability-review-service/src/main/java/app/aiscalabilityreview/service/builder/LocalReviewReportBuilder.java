package app.aiscalabilityreview.service.builder;

import app.aiscalabilityreview.domain.ReviewReport;
import app.aiscalabilityreview.service.GeminiModels;
import core.framework.api.json.Property;
import core.framework.json.JSON;
import core.framework.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Builds the persisted {@link ReviewReport} from the files a local review run produced.
 *
 * <p>Scores and the key metric snapshot come from the {@code report-snapshot.json} the synthesis
 * stage writes, so the persisted values are the ones the report states rather than a re-parse of
 * prose. Scores fall back to the dimension headings in the markdown when the snapshot is missing,
 * and stay {@code UNKNOWN} when neither source has them — an unknown score is never silently
 * promoted to GREEN.
 */
public class LocalReviewReportBuilder {
    public static final String UNKNOWN = "UNKNOWN";
    private static final Pattern SCORE_PATTERN = Pattern.compile(
        "(?i)###\\s*Dimension\\s+\\d+\\s*[—\\-–]\\s*([^:]+):\\s*(GREEN|YELLOW|RED)");
    private static final Logger LOGGER = LoggerFactory.getLogger(LocalReviewReportBuilder.class);
    private static final int DIMENSION_COUNT = 5;

    public static ReviewReport reviewReport(LocalReportParams params) {
        ReportSnapshot snapshot = parseSnapshot(params.snapshotJson());
        String[] scores = scores(snapshot, params.reportMarkdown());

        ReviewReport report = new ReviewReport();
        report.reportId = UUID.randomUUID().toString();
        report.jobId = params.jobId();
        report.serviceId = params.serviceId();
        report.periodLabel = ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        report.aiModel = GeminiModels.toAIModel(params.model());
        report.reportMarkdown = params.reportMarkdown() == null ? "" : params.reportMarkdown();
        report.trafficScore = scores[0];
        report.latencyScore = scores[1];
        report.errorsScore = scores[2];
        report.resourcesScore = scores[3];
        report.persistenceScore = scores[4];
        report.overallScore = overallScore(snapshot, scores);
        report.generatedAt = ZonedDateTime.now();
        report.recommendationsHigh = countOccurrences(params.reportMarkdown(), "(High Priority)");
        report.recommendationsMedium = countOccurrences(params.reportMarkdown(), "(Medium Priority)");
        report.recommendationsLow = countOccurrences(params.reportMarkdown(), "(Low Priority)");
        if (snapshot != null) {
            report.avgRps = snapshot.avgRps;
            report.p99LatencyMs = snapshot.p99LatencyMs;
            report.errorRatePct = snapshot.errorRatePct;
            report.cpuAvgPct = snapshot.cpuAvgPct;
            report.memAvgPct = snapshot.memAvgPct;
            report.mysqlConnectionPct = snapshot.mysqlConnectionPct;
            report.mysqlReplicationLagS = snapshot.mysqlReplicationLagS;
        }
        // Local runs are not published to Confluence; keep the output file path discoverable instead.
        report.confluencePageUrl = "file://" + params.reportPath();
        return report;
    }

    private static String[] scores(ReportSnapshot snapshot, String markdown) {
        String[] fromSnapshot = {
            normalize(snapshot == null ? null : snapshot.trafficScore),
            normalize(snapshot == null ? null : snapshot.latencyScore),
            normalize(snapshot == null ? null : snapshot.errorsScore),
            normalize(snapshot == null ? null : snapshot.resourcesScore),
            normalize(snapshot == null ? null : snapshot.persistenceScore)
        };
        String[] fromMarkdown = parseScores(markdown);
        String[] scores = new String[DIMENSION_COUNT];
        for (int i = 0; i < scores.length; i++) {
            scores[i] = UNKNOWN.equals(fromSnapshot[i]) ? fromMarkdown[i] : fromSnapshot[i];
        }
        return scores;
    }

    private static String[] parseScores(String markdown) {
        String[] scores = new String[DIMENSION_COUNT];
        Arrays.fill(scores, UNKNOWN);
        if (markdown == null) return scores;
        Matcher matcher = SCORE_PATTERN.matcher(markdown);
        int index = 0;
        while (index < scores.length && matcher.find()) {
            scores[index++] = matcher.group(2).toUpperCase(Locale.US);
        }
        return scores;
    }

    /**
     * The worst of the five dimensions. Unknown dimensions are not treated as healthy: a run where
     * no score could be read reports UNKNOWN rather than GREEN.
     */
    private static String overallScore(ReportSnapshot snapshot, String[] scores) {
        String reported = normalize(snapshot == null ? null : snapshot.overallScore);
        if (!UNKNOWN.equals(reported)) return reported;

        boolean hasRed = false;
        boolean hasYellow = false;
        boolean hasUnknown = false;
        for (String score : scores) {
            if ("RED".equals(score)) hasRed = true;
            else if ("YELLOW".equals(score)) hasYellow = true;
            else if (UNKNOWN.equals(score)) hasUnknown = true;
        }
        if (hasRed) return "RED";
        if (hasYellow) return "YELLOW";
        return hasUnknown ? UNKNOWN : "GREEN";
    }

    private static String normalize(String score) {
        if (Strings.isBlank(score)) return UNKNOWN;
        String upper = score.trim().toUpperCase(Locale.US);
        return switch (upper) {
            case "GREEN", "YELLOW", "RED" -> upper;
            default -> UNKNOWN;
        };
    }

    private static ReportSnapshot parseSnapshot(String json) {
        if (Strings.isBlank(json)) {
            LOGGER.warn("No report snapshot produced by the synthesis stage; scores fall back to the report headings");
            return null;
        }
        int start = json.indexOf('{');
        int end = json.lastIndexOf('}');
        if (start < 0 || end <= start) return null;
        try {
            return JSON.fromJSON(ReportSnapshot.class, json.substring(start, end + 1));
        } catch (RuntimeException e) {
            LOGGER.warn("Could not parse report snapshot: {}", e.getMessage());
            return null;
        }
    }

    private static int countOccurrences(String text, String pattern) {
        if (text == null) return 0;
        int count = 0;
        int index = text.indexOf(pattern);
        while (index != -1) {
            count++;
            index = text.indexOf(pattern, index + pattern.length());
        }
        return count;
    }

    /**
     * @param snapshotJson content of {@code report-snapshot.json}, or null when it was not written
     * @param model        Gemini CLI model id of the synthesis stage
     */
    public record LocalReportParams(String jobId,
                                    String serviceId,
                                    String reportMarkdown,
                                    String reportPath,
                                    String snapshotJson,
                                    String model) {
    }

    public static class ReportSnapshot {
        @Property(name = "traffic_score")
        public String trafficScore;

        @Property(name = "latency_score")
        public String latencyScore;

        @Property(name = "errors_score")
        public String errorsScore;

        @Property(name = "resources_score")
        public String resourcesScore;

        @Property(name = "persistence_score")
        public String persistenceScore;

        @Property(name = "overall_score")
        public String overallScore;

        @Property(name = "avg_rps")
        public Double avgRps;

        @Property(name = "p99_latency_ms")
        public Double p99LatencyMs;

        @Property(name = "error_rate_pct")
        public Double errorRatePct;

        @Property(name = "cpu_avg_pct")
        public Double cpuAvgPct;

        @Property(name = "mem_avg_pct")
        public Double memAvgPct;

        @Property(name = "mysql_connection_pct")
        public Double mysqlConnectionPct;

        @Property(name = "mysql_replication_lag_s")
        public Double mysqlReplicationLagS;
    }
}
