package app.aiscalabilityreview.job;

import app.aiscalabilityreview.api.localreview.GenerateLocalReviewRequest;
import app.aiscalabilityreview.domain.ReviewJob.StageStatus;
import app.aiscalabilityreview.domain.ReviewReport;
import app.aiscalabilityreview.domain.embedded.AIModel;
import app.aiscalabilityreview.exception.FatalStageException;
import app.aiscalabilityreview.prompt.LocalReviewPromptBuilder;
import app.aiscalabilityreview.prompt.LocalReviewPromptBuilder.ReviewPromptParams;
import app.aiscalabilityreview.prompt.LocalReviewPromptBuilder.ValidationPromptParams;
import app.aiscalabilityreview.service.GeminiCliService;
import app.aiscalabilityreview.service.ReviewService;
import core.framework.inject.Inject;
import core.framework.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Orchestrates the three local review stages (code analysis, review, validation)
 * using the local Gemini CLI. Outputs are written to disk under the configured output directory.
 * Job status is tracked via the existing ReviewJob + ReviewReport MongoDB collections.
 */
public class LocalReviewJobExecutor {
    private static final String GEMINI_MODEL = "gemini-2.5-pro";
    private static final Pattern SCORE_PATTERN = Pattern.compile(
        "(?i)###\\s*Dimension\\s+\\d+\\s*[—\\-–]\\s*([^:]+):\\s*(GREEN|YELLOW|RED)");
    private static final String[] DEFAULT_SCORES = {"UNKNOWN", "UNKNOWN", "UNKNOWN", "UNKNOWN", "UNKNOWN"};

    public static String resolveOutputDir(GenerateLocalReviewRequest request) {
        String base = !Strings.isBlank(request.outputBaseDir) ? request.outputBaseDir : "reports";
        return base + "/" + request.service;
    }

    private final Logger logger = LoggerFactory.getLogger(LocalReviewJobExecutor.class);

    // Holds request params for async execution, keyed by jobId.
    private final Map<String, GenerateLocalReviewRequest> pendingRequests = new ConcurrentHashMap<>();

    @Inject
    ReviewService reviewService;
    @Inject
    GeminiCliService geminiCliService;

    public void registerRequest(String jobId, GenerateLocalReviewRequest request) {
        pendingRequests.put(jobId, request);
    }

    public void executeLocalReview(String jobId) {
        GenerateLocalReviewRequest request = pendingRequests.remove(jobId);
        if (request == null) {
            logger.error("No request registered for local review job {}", jobId);
            reviewService.failJob(jobId, "No request params found for job " + jobId);
            return;
        }

        String outputDir = resolveOutputDir(request);
        reviewService.updateJobStatus(jobId, "RUNNING", "STAGE_0_CODE_ANALYSIS");
        logger.info("Starting local review job {} (service={}, outputDir={})", jobId, request.service, outputDir);

        try {
            Files.createDirectories(Path.of(outputDir));
        } catch (IOException e) {
            reviewService.failJob(jobId, "Failed to create output directory: " + e.getMessage());
            return;
        }

        String codeContextPath = outputDir + "/code-context.md";
        String today = ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String reviewReportPath = outputDir + "/" + today + "-review.md";
        String validationReportPath = outputDir + "/" + today + "-validation.md";

        try {
            runStages(jobId, request, codeContextPath, reviewReportPath, validationReportPath);
        } catch (FatalStageException e) {
            logger.error("Fatal stage failure in local review job {} at {}: {}", jobId, e.stageName, e.getMessage());
            reviewService.failJob(jobId, "Stage " + e.stageName + " failed: " + e.getCause().getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error in local review job {}: {}", jobId, e.getMessage(), e);
            reviewService.failJob(jobId, "Unexpected error: " + e.getMessage());
        }
    }

    private void runStages(String jobId, GenerateLocalReviewRequest request,
                           String codeContextPath, String reviewReportPath, String validationReportPath) {
        runCodeAnalysisStage(jobId, request, codeContextPath);

        String[] reviewOutput = {null};
        String outputDir = Path.of(resolveOutputDir(request)).toAbsolutePath().toString();
        runStage(jobId, "STAGE_1_REVIEW", true, () -> {
            ReviewPromptParams params = buildReviewPromptParams(request, codeContextPath);
            List<String> dirs = List.of(request.localInfraRepoPath, outputDir);
            String output = geminiCliService.run(LocalReviewPromptBuilder.buildReviewPrompt(params), GEMINI_MODEL, dirs);
            writeFile(reviewReportPath, output);
            reviewOutput[0] = output;
            logger.info("Stage 1 complete: wrote review report to {}", reviewReportPath);
        });

        runStage(jobId, "STAGE_2_VALIDATION", false, () -> {
            ValidationPromptParams params = buildValidationPromptParams(request, reviewReportPath);
            String output = geminiCliService.run(LocalReviewPromptBuilder.buildValidationPrompt(params), GEMINI_MODEL, List.of(outputDir, request.localInfraRepoPath));
            writeFile(validationReportPath, output);
            logger.info("Stage 2 complete: wrote validation report to {}", validationReportPath);
        });

        ReviewReport report = buildReviewReport(jobId, request, reviewOutput[0], reviewReportPath);
        reviewService.saveReport(report);
        reviewService.completeJob(jobId, report.reportId);
        logger.info("Local review job {} completed: reportId={}, outputDir={}",
            jobId, report.reportId, resolveOutputDir(request));
    }

    private void runCodeAnalysisStage(String jobId, GenerateLocalReviewRequest request, String codeContextPath) {
        if (Boolean.TRUE.equals(request.skipCodeAnalysis)) {
            logger.info("Skipping Stage 0 code analysis for job {}", jobId);
            reviewService.updateJobStatus(jobId, "RUNNING", "STAGE_0_CODE_ANALYSIS");
            StageStatus skipped = new StageStatus();
            skipped.status = "SKIPPED";
            skipped.startedAt = ZonedDateTime.now();
            skipped.completedAt = ZonedDateTime.now();
            skipped.durationMs = 0L;
            reviewService.updateJobStageStatus(jobId, "STAGE_0_CODE_ANALYSIS", skipped);
            return;
        }
        runStage(jobId, "STAGE_0_CODE_ANALYSIS", true, () -> {
            String prompt = LocalReviewPromptBuilder.buildCodeAnalysisPrompt(request.service, request.localAppRepoPath);
            String output = geminiCliService.run(prompt, GEMINI_MODEL, List.of(request.localAppRepoPath));
            writeFile(codeContextPath, output);
            logger.info("Stage 0 complete: wrote code-context to {}", codeContextPath);
        });
    }

    private void runStage(String jobId, String stageName, boolean fatal, StageAction action) {
        reviewService.updateJobStatus(jobId, "RUNNING", stageName);
        StageStatus stageStatus = new StageStatus();
        stageStatus.status = "RUNNING";
        stageStatus.startedAt = ZonedDateTime.now();
        reviewService.updateJobStageStatus(jobId, stageName, stageStatus);

        long startMs = System.currentTimeMillis();
        try {
            action.execute();
            stageStatus.status = "COMPLETED";
            stageStatus.completedAt = ZonedDateTime.now();
            stageStatus.durationMs = System.currentTimeMillis() - startMs;
            reviewService.updateJobStageStatus(jobId, stageName, stageStatus);
            logger.info("Stage {} completed in {}ms for job {}", stageName, stageStatus.durationMs, jobId);
        } catch (Exception e) {
            stageStatus.status = "FAILED";
            stageStatus.completedAt = ZonedDateTime.now();
            stageStatus.durationMs = System.currentTimeMillis() - startMs;
            stageStatus.errorMessage = e.getMessage();
            reviewService.updateJobStageStatus(jobId, stageName, stageStatus);
            logger.error("Stage {} failed after {}ms for job {}: {}", stageName, stageStatus.durationMs, jobId, e.getMessage());
            if (fatal) {
                throw new FatalStageException(stageName, e);
            }
        }
    }

    private ReviewPromptParams buildReviewPromptParams(GenerateLocalReviewRequest request, String codeContextPath) {
        ReviewPromptParams params = new ReviewPromptParams();
        params.serviceId = request.service;
        params.localInfraRepoPath = request.localInfraRepoPath;
        params.codeContextPath = codeContextPath;
        params.env = request.env;
        params.namespace = request.namespace;
        params.domain = request.domain;
        params.mysqlHost = request.mysqlHost;
        params.mysqlDb = request.mysqlDB;
        params.atlasCluster = request.atlasCluster;
        params.hpaType = request.hpaType;
        params.kafkaConsumerGroups = request.kafkaConsumerGroups;
        return params;
    }

    private ValidationPromptParams buildValidationPromptParams(GenerateLocalReviewRequest request, String reviewReportPath) {
        ValidationPromptParams params = new ValidationPromptParams();
        params.serviceId = request.service;
        params.reviewReportPath = reviewReportPath;
        params.env = request.env;
        params.namespace = request.namespace;
        params.mysqlHost = request.mysqlHost;
        params.mysqlDb = request.mysqlDB;
        params.atlasCluster = request.atlasCluster;
        params.localInfraRepoPath = request.localInfraRepoPath;
        params.kafkaConsumerGroups = request.kafkaConsumerGroups;
        return params;
    }

    private ReviewReport buildReviewReport(String jobId, GenerateLocalReviewRequest request,
                                           String reportMarkdown, String reportPath) {
        String[] scores = parseScores(reportMarkdown);
        ReviewReport report = new ReviewReport();
        report.reportId = UUID.randomUUID().toString();
        report.jobId = jobId;
        report.serviceId = request.service;
        report.periodLabel = ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        report.aiModel = AIModel.GEMINI_2_5_PRO;
        report.reportMarkdown = reportMarkdown != null ? reportMarkdown : "";
        report.trafficScore = scores[0];
        report.latencyScore = scores[1];
        report.errorsScore = scores[2];
        report.resourcesScore = scores[3];
        report.persistenceScore = scores[4];
        report.overallScore = computeOverallScore(scores);
        report.generatedAt = ZonedDateTime.now();
        report.recommendationsHigh = countOccurrences(reportMarkdown, "(High Priority)");
        report.recommendationsMedium = countOccurrences(reportMarkdown, "(Medium Priority)");
        report.recommendationsLow = countOccurrences(reportMarkdown, "(Low Priority)");
        // Store output file path in confluencePageUrl for discoverability (no Confluence upload for local review)
        report.confluencePageUrl = "file://" + reportPath;
        return report;
    }

    private String[] parseScores(String markdown) {
        String[] scores = DEFAULT_SCORES.clone();
        if (markdown == null) return scores;
        Matcher m = SCORE_PATTERN.matcher(markdown);
        int idx = 0;
        while (idx < 5 && m.find()) {
            scores[idx++] = m.group(2).toUpperCase(Locale.US);
        }
        return scores;
    }

    private String computeOverallScore(String[] scores) {
        boolean hasRed = false;
        boolean hasYellow = false;
        for (String s : scores) {
            if ("RED".equals(s)) hasRed = true;
            if ("YELLOW".equals(s)) hasYellow = true;
        }
        if (hasRed) return "RED";
        if (hasYellow) return "YELLOW";
        return "GREEN";
    }

    private int countOccurrences(String text, String pattern) {
        if (text == null) return 0;
        int count = 0;
        int idx = text.indexOf(pattern);
        while (idx != -1) {
            count++;
            idx = text.indexOf(pattern, idx + pattern.length());
        }
        return count;
    }

    private void writeFile(String path, String content) throws IOException {
        Path file = Path.of(path).toAbsolutePath();
        Path parent = file.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(file, content);
    }

    @FunctionalInterface
    private interface StageAction {
        void execute() throws Exception;
    }
}
