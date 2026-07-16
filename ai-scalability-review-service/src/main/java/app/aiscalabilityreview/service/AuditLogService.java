package app.aiscalabilityreview.service;

import app.aiscalabilityreview.domain.AuditLog;
import core.framework.inject.Inject;
import core.framework.mongo.MongoCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Persists audit log entries to MongoDB for all external API calls and key operations.
 */
public class AuditLogService {
    private final Logger logger = LoggerFactory.getLogger(AuditLogService.class);

    @Inject
    MongoCollection<AuditLog> auditLogCollection;

    /**
     * Log an external API call or significant system event.
     */
    public void log(AuditLogParam param) {
        try {
            AuditLog entry = new AuditLog();
            entry.logId = UUID.randomUUID().toString();
            entry.jobId = param.jobId;
            entry.serviceId = param.serviceId;
            entry.eventType = param.eventType;
            entry.actor = param.actor;
            entry.target = param.target;
            entry.requestSummary = param.requestSummary;
            entry.requestTokenCount = param.requestTokenCount;
            entry.responseStatusCode = param.statusCode;
            entry.responseDurationMs = param.durationMs;
            entry.responseSummary = param.responseSummary;
            entry.responseErrorMessage = param.errorMessage;
            entry.success = param.success;
            entry.timestamp = ZonedDateTime.now();

            auditLogCollection.insert(entry);
        } catch (Exception e) {
            logger.warn("Failed to write audit log entry for job={}, eventType={}: {}", param.jobId, param.eventType, e.getMessage());
        }
    }

    /**
     * @param jobId             associated review job ID (can be null)
     * @param serviceId         service being reviewed
     * @param eventType         event type, e.g. "ANTHROPIC_API_CALL", "DATADOG_METRICS_QUERY"
     * @param actor             caller identifier, e.g. "AIScoringStage"
     * @param target            target system or resource, e.g. "anthropic/claude-sonnet-4-6"
     * @param requestSummary    brief summary of the request
     * @param requestTokenCount token count of the request (for AI calls), or null
     * @param statusCode        HTTP or logical status code of the response
     * @param durationMs        time taken for the call in milliseconds
     * @param responseSummary   brief summary of the response
     * @param errorMessage      error message if the call failed, or null
     * @param success           whether the call was successful
     */
    public record AuditLogParam(String jobId,
                                String serviceId,
                                String eventType,
                                String actor,
                                String target,
                                String requestSummary,
                                Integer requestTokenCount,
                                int statusCode,
                                long durationMs,
                                String responseSummary,
                                String errorMessage,
                                boolean success) {
    }
}