package app.aiscalabilityreview.service;

import app.aiscalabilityreview.api.review.CompareRunResponse;
import app.aiscalabilityreview.api.review.CompareRunResponse.ConfigDriftView;
import app.aiscalabilityreview.api.review.CompareRunResponse.MetricDiffView;
import app.aiscalabilityreview.api.review.CompareRunResponse.RunRefView;
import app.aiscalabilityreview.api.review.CompareRunResponse.ScoreDiffView;
import app.aiscalabilityreview.domain.ReviewJob;
import app.aiscalabilityreview.domain.ReviewReport;
import app.aiscalabilityreview.job.LocalReviewOutputPaths;
import core.framework.inject.Inject;
import core.framework.util.Strings;
import core.framework.web.exception.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Compares two runs with arithmetic rather than with a model.
 *
 * <p>An AI-written narrative already exists for reports; this is the part that has to be the same
 * every time it is asked, available for any pair of runs, and free. Model variance is exactly what
 * you are trying to see through when you compare two runs of the same service.
 */
public class RunComparisonService {
    private static final DateTimeFormatter LABEL_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final Pattern FILE_HEADING = Pattern.compile("(?m)^###\\s+`([^`]+)`\\s*$");
    private static final int MAX_DRIFT_ENTRIES = 50;

    private final Logger logger = LoggerFactory.getLogger(RunComparisonService.class);

    @Inject
    ReviewService reviewService;

    public CompareRunResponse compare(String jobIdA, String jobIdB) {
        ReviewJob jobA = job(jobIdA);
        ReviewJob jobB = job(jobIdB);
        ReviewReport reportA = report(jobA);
        ReviewReport reportB = report(jobB);

        CompareRunResponse response = new CompareRunResponse();
        response.serviceId = jobA.serviceId;
        response.runA = ref(jobA, reportA);
        response.runB = ref(jobB, reportB);
        response.scoreDiffs = scoreDiffs(reportA, reportB);
        response.metricDiffs = metricDiffs(reportA, reportB);
        response.recommendationDiffs = recommendationDiffs(reportA, reportB);
        response.costDiffs = costDiffs(jobA, jobB);
        response.configDrift = configDrift(jobA, jobB);
        return response;
    }

    private ReviewJob job(String jobId) {
        return reviewService.getReviewJob(jobId).orElseThrow(() -> new NotFoundException("Run not found: " + jobId));
    }

    private ReviewReport report(ReviewJob job) {
        return job.reportId == null ? null : reviewService.getReviewReport(job.reportId).orElse(null);
    }

    private RunRefView ref(ReviewJob job, ReviewReport report) {
        RunRefView ref = new RunRefView();
        ref.jobId = job.jobId;
        ref.reportId = report == null ? null : report.reportId;
        ref.label = job.startedAt == null ? job.jobId : LABEL_FORMAT.format(job.startedAt);
        ref.env = job.env;
        ref.overallScore = job.overallScore;
        ref.validationVerdict = job.validationVerdict;
        return ref;
    }

    private List<ScoreDiffView> scoreDiffs(ReviewReport a, ReviewReport b) {
        List<ScoreDiffView> diffs = new ArrayList<>();
        diffs.add(scoreDiff("Traffic", a, b, report -> report.trafficScore));
        diffs.add(scoreDiff("Latency", a, b, report -> report.latencyScore));
        diffs.add(scoreDiff("Errors", a, b, report -> report.errorsScore));
        diffs.add(scoreDiff("Resources", a, b, report -> report.resourcesScore));
        diffs.add(scoreDiff("Persistence", a, b, report -> report.persistenceScore));
        diffs.add(scoreDiff("Overall", a, b, report -> report.overallScore));
        return diffs;
    }

    private ScoreDiffView scoreDiff(String dimension, ReviewReport a, ReviewReport b, Function<ReviewReport, String> field) {
        ScoreDiffView diff = new ScoreDiffView();
        diff.dimension = dimension;
        diff.scoreA = a == null ? null : field.apply(a);
        diff.scoreB = b == null ? null : field.apply(b);
        diff.change = change(diff.scoreA, diff.scoreB);
        return diff;
    }

    /** GREEN &gt; YELLOW &gt; RED; anything unscored stays UNKNOWN rather than being read as a move. */
    private String change(String a, String b) {
        int rankA = rank(a);
        int rankB = rank(b);
        if (rankA < 0 || rankB < 0) return "UNKNOWN";
        if (rankB > rankA) return "IMPROVED";
        if (rankB < rankA) return "DEGRADED";
        return "UNCHANGED";
    }

    private int rank(String score) {
        if (score == null) return -1;
        return switch (score) {
            case "RED" -> 0;
            case "YELLOW" -> 1;
            case "GREEN" -> 2;
            default -> -1;
        };
    }

    private List<MetricDiffView> metricDiffs(ReviewReport a, ReviewReport b) {
        List<MetricDiffView> diffs = new ArrayList<>();
        diffs.add(metricDiff("Average RPS", "req/s", a, b, report -> report.avgRps, false));
        diffs.add(metricDiff("P99 latency", "ms", a, b, report -> report.p99LatencyMs, true));
        diffs.add(metricDiff("Error rate", "%", a, b, report -> report.errorRatePct, true));
        diffs.add(metricDiff("CPU average", "%", a, b, report -> report.cpuAvgPct, true));
        diffs.add(metricDiff("Memory average", "%", a, b, report -> report.memAvgPct, true));
        diffs.add(metricDiff("MySQL connections", "%", a, b, report -> report.mysqlConnectionPct, true));
        diffs.add(metricDiff("Replication lag", "s", a, b, report -> report.mysqlReplicationLagS, true));
        return diffs;
    }

    private List<MetricDiffView> recommendationDiffs(ReviewReport a, ReviewReport b) {
        List<MetricDiffView> diffs = new ArrayList<>();
        diffs.add(metricDiff("High priority", "count", a, b, report -> toDouble(report.recommendationsHigh), true));
        diffs.add(metricDiff("Medium priority", "count", a, b, report -> toDouble(report.recommendationsMedium), true));
        diffs.add(metricDiff("Low priority", "count", a, b, report -> toDouble(report.recommendationsLow), true));
        return diffs;
    }

    private List<MetricDiffView> costDiffs(ReviewJob a, ReviewJob b) {
        List<MetricDiffView> diffs = new ArrayList<>();
        diffs.add(diff("Total tokens", "tokens", toDouble(a.totalTokens), toDouble(b.totalTokens), true));
        diffs.add(diff("Cached tokens", "tokens", toDouble(a.totalCachedTokens), toDouble(b.totalCachedTokens), false));
        diffs.add(diff("Duration", "s", seconds(a.totalDurationMs), seconds(b.totalDurationMs), true));
        diffs.add(diff("MCP tool calls", "calls", toDouble(a.totalMcpToolCalls), toDouble(b.totalMcpToolCalls), true));
        diffs.add(diff("Estimated cost", "USD", a.estimatedCostUsd, b.estimatedCostUsd, true));
        return diffs;
    }

    private MetricDiffView metricDiff(String metric, String unit, ReviewReport a, ReviewReport b,
                                      Function<ReviewReport, Double> field, boolean higherIsWorse) {
        return diff(metric, unit, a == null ? null : field.apply(a), b == null ? null : field.apply(b), higherIsWorse);
    }

    private MetricDiffView diff(String metric, String unit, Double a, Double b, boolean higherIsWorse) {
        MetricDiffView view = new MetricDiffView();
        view.metric = metric;
        view.unit = unit;
        view.valueA = a;
        view.valueB = b;
        view.higherIsWorse = higherIsWorse;
        if (a != null && b != null) {
            view.delta = round(b - a);
            view.deltaPct = a == 0d ? null : round((b - a) / Math.abs(a) * 100d);
        }
        return view;
    }

    /**
     * Files whose infra-context block changed between the two runs.
     *
     * <p>A deterministic diff of what the pipeline read, so configuration drift is visible
     * independently of what either report said about it.
     */
    private List<ConfigDriftView> configDrift(ReviewJob a, ReviewJob b) {
        Map<String, String> blocksA = infraBlocks(a);
        Map<String, String> blocksB = infraBlocks(b);
        Set<String> keys = new LinkedHashSet<>(blocksA.keySet());
        keys.addAll(blocksB.keySet());

        List<ConfigDriftView> drift = new ArrayList<>();
        for (String key : keys) {
            if (drift.size() >= MAX_DRIFT_ENTRIES) break;
            String blockA = blocksA.get(key);
            String blockB = blocksB.get(key);
            if (blockA == null || blockB == null) {
                drift.add(driftView(key, blockA == null ? "absent" : "present", blockB == null ? "absent" : "present"));
            } else if (!blockA.equals(blockB)) {
                String[] firstDifference = firstDifferingLine(blockA, blockB);
                drift.add(driftView(key, firstDifference[0], firstDifference[1]));
            }
        }
        return drift;
    }

    private ConfigDriftView driftView(String key, String valueA, String valueB) {
        ConfigDriftView view = new ConfigDriftView();
        view.key = key;
        view.valueA = valueA;
        view.valueB = valueB;
        return view;
    }

    private String[] firstDifferingLine(String a, String b) {
        String[] linesA = a.split("\n");
        String[] linesB = b.split("\n");
        int max = Math.max(linesA.length, linesB.length);
        for (int i = 0; i < max; i++) {
            String lineA = i < linesA.length ? linesA[i].trim() : "";
            String lineB = i < linesB.length ? linesB[i].trim() : "";
            if (!lineA.equals(lineB)) return new String[]{lineA.isEmpty() ? "(absent)" : lineA, lineB.isEmpty() ? "(absent)" : lineB};
        }
        return new String[]{"changed", "changed"};
    }

    /** Splits infra-context.md into the per-file blocks the stage assembled it from. */
    private Map<String, String> infraBlocks(ReviewJob job) {
        Map<String, String> blocks = new LinkedHashMap<>();
        if (Strings.isBlank(job.outputDir)) return blocks;
        Path path = Path.of(job.outputDir, LocalReviewOutputPaths.INFRA_CONTEXT);
        if (!Files.isRegularFile(path)) return blocks;
        try {
            String markdown = Files.readString(path, StandardCharsets.UTF_8);
            Matcher matcher = FILE_HEADING.matcher(markdown);
            List<String> names = new ArrayList<>();
            List<Integer> starts = new ArrayList<>();
            while (matcher.find()) {
                names.add(matcher.group(1));
                starts.add(matcher.end());
            }
            for (int i = 0; i < names.size(); i++) {
                int end = i + 1 < starts.size() ? markdown.lastIndexOf("###", starts.get(i + 1)) : markdown.length();
                blocks.put(names.get(i), markdown.substring(starts.get(i), Math.max(starts.get(i), end)).trim());
            }
        } catch (IOException e) {
            logger.warn("Could not read the infra context of run {}: {}", job.jobId, e.getMessage());
        }
        return blocks;
    }

    private Double seconds(Long millis) {
        return millis == null ? null : round(millis / 1000d);
    }

    private Double toDouble(Number value) {
        return value == null ? null : value.doubleValue();
    }

    private Double round(double value) {
        return Math.round(value * 1000d) / 1000d;
    }
}
