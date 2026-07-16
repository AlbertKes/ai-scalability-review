package app.aiscalabilityreview.job;

import app.aiscalabilityreview.domain.ReviewFeedback;
import app.aiscalabilityreview.domain.ReviewReport;
import app.aiscalabilityreview.service.AuditLogService;
import app.aiscalabilityreview.service.ConfluenceService;
import app.aiscalabilityreview.service.ReviewService;
import core.framework.inject.Inject;
import core.framework.scheduler.Job;
import core.framework.scheduler.JobContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Scheduled job that runs every 6 hours to sync feedback comments from Confluence
 * back into the review_feedback MongoDB collection.
 * <p>
 * Only ingests comments that begin with [FEEDBACK] and have not been previously ingested
 * (identified by their Confluence comment ID).
 */
public class ConfluenceFeedbackSyncJob implements Job {
    private static final String FEEDBACK_PREFIX = "[FEEDBACK]";
    private static final int LOOKBACK_DAYS = 90;

    private final Logger logger = LoggerFactory.getLogger(ConfluenceFeedbackSyncJob.class);

    @Inject
    ReviewService reviewService;

    @Inject
    ConfluenceService confluenceService;

    @Inject
    AuditLogService auditLogService;

    @Override
    public void execute(JobContext context) throws Exception {
        logger.info("FeedbackSyncJob started at {}", ZonedDateTime.now());

        List<ReviewReport> recentReports = reviewService.findRecentReportsWithConfluence(LOOKBACK_DAYS);
        logger.info("Checking {} recent reports with Confluence pages for feedback", recentReports.size());

        int newFeedbackCount = 0;

        for (ReviewReport report : recentReports) {
            if (report.confluencePageId == null || report.confluencePageId.isBlank()) continue;

            try {
                List<ConfluenceService.ConfluenceComment> comments =
                    confluenceService.getPageComments(report.confluencePageId);

                for (ConfluenceService.ConfluenceComment comment : comments) {
                    if (!isFeedbackComment(comment)) continue;

                    // Skip if already ingested
                    if (reviewService.feedbackExists(comment.id)) continue;

                    ReviewFeedback feedback = new ReviewFeedback();
                    feedback.feedbackId = UUID.randomUUID().toString();
                    feedback.serviceId = report.serviceId;
                    feedback.reportId = report.reportId;
                    feedback.confluencePageId = report.confluencePageId;
                    feedback.confluenceCommentId = comment.id;
                    feedback.author = comment.author;
                    feedback.dimension = parseDimension(comment.bodyText);
                    feedback.rawText = stripFeedbackPrefix(comment.bodyText);
                    feedback.status = "PENDING";
                    feedback.createdAt = ZonedDateTime.now();
                    feedback.ingestedAt = ZonedDateTime.now();

                    reviewService.saveFeedback(feedback);
                    newFeedbackCount++;

                    logger.info("Ingested feedback for service {} report {} from author {}: {}",
                        report.serviceId, report.reportId, comment.author,
                        truncate(feedback.rawText, 80));
                }

            } catch (Exception e) {
                logger.warn("Failed to fetch comments for report {} (page {}): {}",
                    report.reportId, report.confluencePageId, e.getMessage());
            }
        }

        logger.info("FeedbackSyncJob completed: {} new feedback items ingested", newFeedbackCount);
    }

    private boolean isFeedbackComment(ConfluenceService.ConfluenceComment comment) {
        return comment.bodyText != null
            && comment.bodyText.trim().toUpperCase(Locale.US).startsWith(FEEDBACK_PREFIX);
    }

    /**
     * Parse the dimension from a feedback comment body.
     * Expected format: [FEEDBACK] <DimensionName>: <feedback text>
     * Example: [FEEDBACK] Persistence Layer: MySQL connections are actually lower than reported.
     */
    private String parseDimension(String bodyText) {
        if (bodyText == null) return "General";
        String stripped = stripFeedbackPrefix(bodyText).trim();

        // Look for "Dimension Name:" at the start
        int colonIdx = stripped.indexOf(':');
        if (colonIdx > 0 && colonIdx < 50) {
            String candidate = stripped.substring(0, colonIdx).trim();
            // Validate against known dimension names
            for (String dim : new String[]{
                "Traffic & Throughput", "Latency & Thread Concurrency",
                "Error Rates & Stability", "Resource Saturation", "Persistence Layer"
            }) {
                if (candidate.equalsIgnoreCase(dim) || dim.toLowerCase(Locale.US).contains(candidate.toLowerCase(Locale.US))) {
                    return dim;
                }
            }
            // If it looks like a dimension name (no spaces unusual), return it as-is
            if (candidate.length() < 40) return candidate;
        }
        return "General";
    }

    private String stripFeedbackPrefix(String bodyText) {
        if (bodyText == null) return "";
        String trimmed = bodyText.trim();
        if (trimmed.toUpperCase(Locale.US).startsWith(FEEDBACK_PREFIX)) {
            return trimmed.substring(FEEDBACK_PREFIX.length()).trim();
        }
        return trimmed;
    }

    private String truncate(String text, int maxLen) {
        if (text == null || text.length() <= maxLen) return text;
        return text.substring(0, maxLen) + "...";
    }
}