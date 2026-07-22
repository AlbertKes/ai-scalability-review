package app.aiscalabilityreview.job.stage;

import app.aiscalabilityreview.domain.ReviewReport;
import app.aiscalabilityreview.domain.ServiceConfig;
import app.aiscalabilityreview.service.AuditLogService;
import app.aiscalabilityreview.service.ConfluenceService;
import app.aiscalabilityreview.service.ReviewService;
import core.framework.inject.Inject;
import core.framework.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Stage 7: Publish the generated report to Confluence and update MongoDB records.
 *
 * Produces:
 *   - ReviewReport.confluencePageId and confluencePageUrl are populated
 */
public class ReportPublishStage {
    private final Logger logger = LoggerFactory.getLogger(ReportPublishStage.class);
    @Inject
    ConfluenceService confluenceService;
    @Inject
    ReviewService reviewService;
    @Inject
    AuditLogService auditLogService;

    public void execute(ReviewContext context) throws Exception {
        ServiceConfig config = context.config;

        if (context.reportMarkdown == null || context.reportMarkdown.isBlank()) {
            logger.warn("No report markdown available for service {}; skipping Confluence publish", config.serviceId);
            return;
        }

        if (Strings.isBlank(config.reviewConfig.confluenceSpaceKey)) {
            logger.info("No Confluence space configured for service {}; skipping publish", config.serviceId);
            return;
        }

        // Find the Confluence parent page ID if not already stored
        String parentPageId = config.reviewConfig.confluenceParentPageId;
        if ((parentPageId == null || parentPageId.isBlank()) && config.reviewConfig.confluenceParentPageTitle != null) {
            Optional<String> found = confluenceService.getPageIdByTitle(
                    config.reviewConfig.confluenceSpaceKey, config.reviewConfig.confluenceParentPageTitle);
            if (found.isPresent()) {
                parentPageId = found.get();
                // Cache it in the service config
                config.reviewConfig.confluenceParentPageId = parentPageId;
            }
        }

        String dateLabel = ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String pageTitle = "Scalability Review: " + config.serviceId + " (" + dateLabel + ")";

        long startMs = System.currentTimeMillis();
        try {
            String pageUrl = confluenceService.createOrUpdatePage(
                    config.reviewConfig.confluenceSpaceKey, parentPageId, pageTitle, context.reportMarkdown);
            long durationMs = System.currentTimeMillis() - startMs;
            // Update the report record with Confluence details
            Optional<ReviewReport> reportOpt = reviewService.getReportByJobId(context.job.jobId);
            if (reportOpt.isPresent()) {
                // Derive page ID from URL (last segment)
                String pageId = pageUrl.substring(pageUrl.lastIndexOf('/') + 1);
                ReviewReport report = reportOpt.get();
                report.confluencePageId = pageId;
                report.confluencePageUrl = pageUrl;
                reviewService.updateReport(report);
            }

            auditLogService.log(new AuditLogService.AuditLogParam(context.job.jobId, config.serviceId,
                    "CONFLUENCE_PUBLISH", "ReportPublishStage", pageTitle,
                    "Publish scalability review to Confluence space " + config.reviewConfig.confluenceSpaceKey,
                    null, 200, durationMs, "Published to: " + pageUrl, null, true));

            logger.info("Report published to Confluence: {}", pageUrl);

        } catch (Exception e) {
            long durationMs = System.currentTimeMillis() - startMs;
            auditLogService.log(new AuditLogService.AuditLogParam(context.job.jobId, config.serviceId,
                    "CONFLUENCE_PUBLISH", "ReportPublishStage", pageTitle,
                    "Publish scalability review to Confluence",
                    null, 500, durationMs, null, e.getMessage(), false));

            logger.error("Failed to publish report to Confluence for service {}: {}", config.serviceId, e.getMessage());
            // Non-fatal: the report is still stored in MongoDB
        }
    }
}