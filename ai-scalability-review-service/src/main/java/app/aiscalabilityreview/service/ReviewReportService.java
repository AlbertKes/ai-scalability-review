package app.aiscalabilityreview.service;

import app.aiscalabilityreview.api.review.CompareReviewRequest;
import app.aiscalabilityreview.api.review.CompareReviewResponse;
import app.aiscalabilityreview.api.review.GetReviewReportResponse;
import app.aiscalabilityreview.api.review.GetReviewReportValidationResponse;
import app.aiscalabilityreview.api.review.ListReviewReportRequest;
import app.aiscalabilityreview.api.review.ListReviewReportResponse;
import app.aiscalabilityreview.api.review.ReviewReportView;
import app.aiscalabilityreview.api.review.ValidateReportRequest;
import app.aiscalabilityreview.api.review.ValidateReportResponse;
import app.aiscalabilityreview.domain.ReportComparison;
import app.aiscalabilityreview.domain.ReviewReport;
import app.aiscalabilityreview.domain.ServiceConfig;
import app.aiscalabilityreview.domain.ValidationResult;
import app.aiscalabilityreview.domain.embedded.AIModel;
import app.aiscalabilityreview.prompt.ComparisonPrompt;
import app.aiscalabilityreview.prompt.ValidateReportTaskPrompt;
import app.aiscalabilityreview.prompt.ValidationFormatPrompt;
import app.aiscalabilityreview.service.builder.ReviewReportBuilder;
import core.framework.api.json.Property;
import core.framework.async.Executor;
import core.framework.inject.Inject;
import core.framework.json.JSON;
import core.framework.util.Strings;
import core.framework.web.exception.BadRequestException;
import core.framework.web.exception.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

public class ReviewReportService {
    private static final int COMPARE_MAX_TOKENS = 4096;
    private static final double COMPARE_TEMPERATURE = 0.1;
    private static final int VALIDATE_MAX_TOKENS = 8192;
    private static final double VALIDATE_TEMPERATURE = 0.1;
    private static final AIModel DEFAULT_MODEL = AIModel.GEMINI_2_5_PRO;
    private final Logger logger = LoggerFactory.getLogger(ReviewReportService.class);
    @Inject
    Executor executor;
    @Inject
    ReviewService reviewService;
    @Inject
    ServiceConfigService serviceConfigService;
    @Inject
    AnthropicService anthropicService;
    @Inject
    ConfluenceService confluenceService;

    public GetReviewReportResponse get(String reportId) {
        ReviewReport report = reviewService.getReviewReport(reportId).orElseThrow(() -> new
            NotFoundException(Strings.format("Report not found, id = {}", reportId)));
        var response = new GetReviewReportResponse();
        response.view = ReviewReportBuilder.reviewReportView(report);
        return response;
    }

    public ListReviewReportResponse list(ListReviewReportRequest request) {
        List<ReviewReport> reports = reviewService.listReportsByService(request.serviceId, request.limit, request.skip);
        List<ReviewReportView> views = reports.stream().map(ReviewReportBuilder::reviewReportView).toList();
        var response = new ListReviewReportResponse();
        response.items = views;
        response.total = views.size();  //todo
        return response;
    }

    public CompareReviewResponse compare(CompareReviewRequest request) {
        ReviewReport reportA = resolveReport(request.reportIdA, request.serviceId, request.periodA);
        ReviewReport reportB = resolveReport(request.reportIdB, request.serviceId, request.periodB);
        String periodA = request.periodA != null ? request.periodA : reportA.periodLabel;
        String periodB = request.periodB != null ? request.periodB : reportB.periodLabel;
        String userPrompt = ComparisonPrompt.CONTENT
            .replace("{SERVICE}", request.serviceId)
            .replace("{PERIOD_A}", periodA)
            .replace("{PERIOD_B}", periodB)
            .replace("{REPORT_A}", reportA.reportMarkdown)
            .replace("{REPORT_B}", reportB.reportMarkdown);

        logger.info("Comparing reports for service {} periods {} vs {}", request.serviceId, periodA, periodB);
        AnthropicService.GeneratedContent aiResult = anthropicService.generate(
            DEFAULT_MODEL, "You are a scalability review analyst.", userPrompt, COMPARE_MAX_TOKENS, COMPARE_TEMPERATURE);
        ComparisonAIResponse aiResponse = JSON.fromJSON(ComparisonAIResponse.class, aiResult.text());
        ReportComparison comparison = buildReportComparison(request, reportA, reportB, periodA, periodB, aiResponse);
        reviewService.saveComparison(comparison);

        CompareReviewResponse response = new CompareReviewResponse();
        response.comparisonId = comparison.comparisonId;
        response.overallTrajectory = comparison.overallTrajectory;
        response.summaryText = comparison.summaryText;
        response.generatedAt = comparison.generatedAt;
        if (comparison.dimensionDiffs != null) {
            response.dimensionDiffs = comparison.dimensionDiffs.stream().map(d -> {
                CompareReviewResponse.DimensionDiff diff = new CompareReviewResponse.DimensionDiff();
                diff.dimension = d.dimension;
                diff.scoreA = d.scoreA;
                diff.scoreB = d.scoreB;
                diff.change = d.change;
                diff.keyChanges = d.keyChanges;
                return diff;
            }).toList();
        }
        return response;
    }

    private ReportComparison buildReportComparison(CompareReviewRequest request, ReviewReport reportA, ReviewReport reportB, String periodA, String periodB, ComparisonAIResponse aiResponse) {
        ReportComparison comparison = new ReportComparison();
        comparison.comparisonId = UUID.randomUUID().toString();
        comparison.serviceId = request.serviceId;
        comparison.reportIdA = reportA.reportId;
        comparison.reportIdB = reportB.reportId;
        comparison.periodA = periodA;
        comparison.periodB = periodB;
        comparison.aiModel = DEFAULT_MODEL;
        comparison.overallTrajectory = aiResponse.overallTrajectory;
        comparison.summaryText = aiResponse.summaryText;
        comparison.requestedBy = "api";
        comparison.generatedAt = ZonedDateTime.now();
        if (aiResponse.dimensionDiffs != null) {
            comparison.dimensionDiffs = aiResponse.dimensionDiffs.stream().map(d -> {
                ReportComparison.DimensionDiff diff = new ReportComparison.DimensionDiff();
                diff.dimension = d.dimension;
                diff.scoreA = d.scoreA;
                diff.scoreB = d.scoreB;
                diff.change = d.change;
                diff.keyChanges = d.keyChanges;
                return diff;
            }).toList();
        }
        return comparison;
    }

    private ReviewReport resolveReport(String reportId, String serviceId, String period) {
        if (reportId != null && !reportId.isBlank()) {
            return reviewService.getReviewReport(reportId)
                .orElseThrow(() -> new NotFoundException("Report not found: " + reportId));
        }
        if (period != null && !period.isBlank()) {
            return reviewService.listReportsByService(serviceId, 50, 0).stream()
                .filter(r -> period.equals(r.periodLabel))
                .findFirst()
                .orElseThrow(() -> new NotFoundException(
                    "No report found for service " + serviceId + " period " + period));
        }
        throw new BadRequestException("Either report_id or period must be specified for comparison");
    }

    public ValidateReportResponse validate(String reportId, ValidateReportRequest request) {
        ReviewReport report = reviewService.getReviewReport(reportId).orElseThrow(() -> new NotFoundException("Report not found: " + reportId));
        AIModel model = request.model != null ? AIModel.valueOf(request.model.name()) : DEFAULT_MODEL;
        String validationId = reviewService.createValidationResult(reportId, model, "api");
        final boolean uploadToConfluence = Boolean.TRUE.equals(request.uploadToConfluence);
        executor.submit("validate-" + reportId, () -> {
            runValidation(validationId, report, model, uploadToConfluence);
        });
        logger.info("Triggered validation {} for report {}", validationId, reportId);
        ValidateReportResponse response = new ValidateReportResponse();
        response.validationId = validationId;
        return response;
    }

    private void runValidation(String validationId, ReviewReport report, AIModel model, boolean uploadToConfluence) {
        ValidationResult result = reviewService.getValidationResult(validationId).orElse(null);
        if (result == null) return;

        result.status = "RUNNING";
        reviewService.updateValidationResult(result);

        try {
            String systemPrompt = ValidateReportTaskPrompt.CONTENT
                .replace("{{REPORT_FILE}}", "report-" + report.reportId)
                .replace("{{SERVICE}}", report.serviceId)
                .replace("{{ENV}}", "prod")
                .replace("{{NAMESPACE}}", report.serviceId)
                .replace("{{MYSQL_HOST}}", "N/A")
                .replace("{{MYSQL_DB}}", "N/A")
                .replace("{{ATLAS_CLUSTER}}", "N/A")
                .replace("{{KAFKA_CONSUMER_GROUPS}}", "N/A");

            String userPrompt = "Here is the report to validate:\n\n" + report.reportMarkdown
                + "\n\nFollow the validation format:\n\n" + ValidationFormatPrompt.CONTENT;

            AnthropicService.GeneratedContent aiResult = anthropicService.generate(
                model, systemPrompt, userPrompt, VALIDATE_MAX_TOKENS, VALIDATE_TEMPERATURE);

            result.validationReportMarkdown = aiResult.text();
            result.inputTokens = aiResult.inputTokens();
            result.outputTokens = aiResult.outputTokens();
            result.estimatedCostUsd = aiResult.estimatedCostUsd();
            result.status = "COMPLETED";
            result.generatedAt = ZonedDateTime.now();
            result.verdict = parseVerdict(aiResult.text());

            // Optionally publish to Confluence
            if (uploadToConfluence) {
                uploadToConfluence(report, aiResult, result);
            }
        } catch (Exception e) {
            result.status = "FAILED";
            logger.error("Validation failed for report {}: {}", report.reportId, e.getMessage());
        }

        reviewService.updateValidationResult(result);
    }

    private void uploadToConfluence(ReviewReport report, AnthropicService.GeneratedContent aiResult, ValidationResult result) {
        try {
            ServiceConfig config = serviceConfigService.getServiceConfig(report.serviceId);
            if (config.reviewConfig.confluenceSpaceKey != null) {
                String pageTitle = "Validation: " + report.serviceId
                    + " (" + report.periodLabel + ")";
                String pageUrl = confluenceService.createOrUpdatePage(
                    config.reviewConfig.confluenceSpaceKey,
                    report.confluencePageId,
                    pageTitle,
                    aiResult.text());
                result.confluencePageUrl = pageUrl;
            }
        } catch (Exception e) {
            logger.warn("Failed to publish validation to Confluence: {}", e.getMessage());
        }
    }

    private String parseVerdict(String markdown) {
        if (markdown == null) return "UNKNOWN";
        if (markdown.contains("Overall Validation Verdict: PASS")) return "PASS";
        if (markdown.contains("Overall Validation Verdict: FAIL")) return "FAIL";
        if (markdown.contains("Overall Validation Verdict: CONDITIONAL PASS")) return "CONDITIONAL_PASS";
        return "UNKNOWN";
    }

    public GetReviewReportValidationResponse getValidation(String validationId) {
        ValidationResult result = reviewService.getValidationResult(validationId)
            .orElseThrow(() -> new NotFoundException("Validation not found: " + validationId));
        return ReviewReportBuilder.reviewReportValidationResponse(result);
    }

    public static class ComparisonAIResponse {
        @Property(name = "overall_trajectory")
        public String overallTrajectory;

        @Property(name = "summary_text")
        public String summaryText;

        @Property(name = "dimension_diffs")
        public List<DimensionDiffAI> dimensionDiffs;

        public static class DimensionDiffAI {
            @Property(name = "dimension")
            public String dimension;

            @Property(name = "score_a")
            public String scoreA;

            @Property(name = "score_b")
            public String scoreB;

            @Property(name = "change")
            public String change;

            @Property(name = "key_changes")
            public List<String> keyChanges;
        }
    }
}
