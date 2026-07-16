package app.aiscalabilityreview.job.stage;

import app.aiscalabilityreview.domain.ReviewFeedback;
import app.aiscalabilityreview.domain.ReviewReport;
import app.aiscalabilityreview.domain.ServiceConfig;
import app.aiscalabilityreview.prompt.MetricScorePrompt;
import app.aiscalabilityreview.prompt.ReportFormatPrompt;
import app.aiscalabilityreview.prompt.ReviewTaskPrompt;
import app.aiscalabilityreview.service.AnthropicService;
import app.aiscalabilityreview.service.AuditLogService;
import app.aiscalabilityreview.service.ReviewService;
import core.framework.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Stage 6: Build the complete review prompt and call Claude AI to generate the report.
 *
 * Produces:
 *   - context.reportMarkdown — the full AI-generated scalability review
 *   - a ReviewReport record saved to MongoDB
 */
public class AIScoringStage {
    private static final int MAX_TOKENS = 16384;
    private static final double TEMPERATURE = 0.1;

    // Score extraction patterns
    private static final Pattern SCORE_PATTERN = Pattern.compile(
            "(?i)###\\s*Dimension\\s+\\d+\\s*[—\\-–]\\s*([^:]+):\\s*(GREEN|YELLOW|RED)");
    private final Logger logger = LoggerFactory.getLogger(AIScoringStage.class);
    @Inject
    AnthropicService anthropicService;
    @Inject
    AuditLogService auditLogService;
    @Inject
    ReviewService reviewService;

    public void execute(ReviewContext context) throws Exception {
        ServiceConfig config = context.config;
        String systemPrompt = buildSystemPrompt(config);
        String userPrompt = buildUserPrompt(context);
        logger.info("Calling AI for scalability review: service={}, model={}", config.serviceId, context.job.aiModel);

        long startMs = System.currentTimeMillis();
        AnthropicService.GeneratedContent result = anthropicService.generate(
                context.job.aiModel, systemPrompt, userPrompt, MAX_TOKENS, TEMPERATURE);
        long durationMs = System.currentTimeMillis() - startMs;

        context.reportMarkdown = result.text();

        auditLogService.log(new AuditLogService.AuditLogParam(context.job.jobId, config.serviceId,
                "REVIEW_AI_CALL", "AIScoringStage",
                "anthropic/" + context.job.aiModel,
                "Scalability review for " + config.serviceId
                        + " (" + userPrompt.length() + " chars input)",
                result.inputTokens(),
                200, durationMs,
                "Generated " + result.text().length() + " chars report",
                null, true));

        logger.info("AI review generated: inputTokens={}, outputTokens={}, durationMs={}",
                result.inputTokens(), result.outputTokens(), durationMs);
        String[] scores = parseScores(context.reportMarkdown);
        ReviewReport report = new ReviewReport();
        report.reportId = UUID.randomUUID().toString();
        report.jobId = context.job.jobId;
        report.serviceId = config.serviceId;
        report.periodLabel = ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        report.aiModel = context.job.aiModel;
        report.reportMarkdown = context.reportMarkdown;
        report.trafficScore = scores[0];
        report.latencyScore = scores[1];
        report.errorsScore = scores[2];
        report.resourcesScore = scores[3];
        report.persistenceScore = scores[4];
        report.overallScore = computeOverallScore(scores);
        report.generatedAt = ZonedDateTime.now();
        report.repoShaApp = context.repoShas.get("app");
        report.repoShaInfra = context.repoShas.get("infra");
        report.repoShaK8sGitops = context.repoShas.get("k8s_gitops");

        report.recommendationsHigh = countOccurrences(context.reportMarkdown, "(High Priority)");
        report.recommendationsMedium = countOccurrences(context.reportMarkdown, "(Medium Priority)");
        report.recommendationsLow = countOccurrences(context.reportMarkdown, "(Low Priority)");

        reviewService.saveReport(report);

        reviewService.completeJob(context.job.jobId, report.reportId);

        logger.info("Review report saved: reportId={}, service={}, overallScore={}",
                report.reportId, config.serviceId, report.overallScore);
    }

    private String buildSystemPrompt(ServiceConfig config) {
        return ReviewTaskPrompt.CONTENT
                .replace("{{SERVICE}}", config.serviceId)
                .replace("{{ENV}}", nvl(config.environment, "prod"))
                .replace("{{NAMESPACE}}", nvl(config.namespace, config.serviceId))
                .replace("{{MYSQL_HOST}}", nvl(config.mysqlHost, "N/A"))
                .replace("{{MYSQL_DB}}", nvl(config.mysqlDb, "N/A"))
                .replace("{{ATLAS_CLUSTER}}", nvl(config.atlasCluster, "N/A"))
                .replace("{{HPA_TYPE}}", nvl(config.hpaType, "HPA"))
                .replace("{{KAFKA_CONSUMER_GROUPS}}", kafkaGroupsOrNA(config))
                .replace("{{DOMAIN}}", config.serviceId);
    }

    private String buildUserPrompt(ReviewContext context) {
        ServiceConfig config = context.config;
        StringBuilder sb = new StringBuilder();

        sb.append("Please perform a complete scalability review for **").append(config.serviceId).append("**.\n\n");

        sb.append("## Code Context (Business Analysis)\n\n");
        sb.append(nvl(context.codeContextDocument, "Code context: not available")).append("\n\n");

        sb.append("## Infrastructure Configuration\n\n");
        sb.append(nvl(context.infraSnapshot, "NOT_COLLECTED: no infrastructure configuration available")).append("\n\n");

        sb.append("## Datadog Metrics (28-day window)\n\n");
        sb.append(nvl(context.datadogMetricsData, "NOT_COLLECTED: no Datadog metrics available")).append("\n\n");

        if (context.mysqlTableData != null && !context.mysqlTableData.isBlank()) {
            sb.append("## MySQL Table Sizes\n\n");
            sb.append(context.mysqlTableData).append("\n\n");
        }

        List<ReviewFeedback> pendingFeedback = reviewService.listPendingFeedback(config.serviceId);
        if (!pendingFeedback.isEmpty()) {
            sb.append("## Previous Review Feedback (from Confluence comments)\n\n");
            sb.append("The following corrections were submitted by engineers on previous reports. ")
                    .append("Please address them in this review:\n\n");
            for (ReviewFeedback fb : pendingFeedback) {
                sb.append("**[FEEDBACK] ").append(nvl(fb.dimension, "General")).append("**: ")
                        .append(fb.rawText).append("\n\n");
            }
        }

        sb.append("## Report Format\n\n");
        sb.append("Follow the exact format from report-format.md:\n\n");
        sb.append(ReportFormatPrompt.CONTENT).append("\n\n");

        sb.append("## Scoring Reference\n\n");
        sb.append(MetricScorePrompt.CONTENT).append("\n\n");

        sb.append("Now produce the complete scalability review report.\n");
        return sb.toString();
    }

    /**
     * Parse dimension scores from the generated markdown.
     * Returns array: [traffic, latency, errors, resources, persistence]
     */
    private String[] parseScores(String markdown) {
        String[] scores = new String[]{"UNKNOWN", "UNKNOWN", "UNKNOWN", "UNKNOWN", "UNKNOWN"};
        if (markdown == null) return scores;

        Matcher m = SCORE_PATTERN.matcher(markdown);
        int idx = 0;
        while (m.find() && idx < 5) {
            scores[idx++] = m.group(2).toUpperCase(Locale.US);
        }
        return scores;
    }

    private String computeOverallScore(String[] dimensionScores) {
        boolean hasRed = false;
        boolean hasYellow = false;
        for (String s : dimensionScores) {
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
        int idx = 0;
        while ((idx = text.indexOf(pattern, idx)) != -1) {
            count++;
            idx += pattern.length();
        }
        return count;
    }

    private String nvl(String value, String defaultValue) {
        return (value != null && !value.isBlank()) ? value : defaultValue;
    }

    private String kafkaGroupsOrNA(ServiceConfig config) {
        if (config.kafkaConsumerGroups == null || config.kafkaConsumerGroups.isEmpty()) return "N/A";
        return String.join(",", config.kafkaConsumerGroups);
    }
}