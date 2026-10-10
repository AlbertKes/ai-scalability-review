package app.aiscalabilityreview.service;

import app.aiscalabilityreview.domain.AppSettings;
import app.aiscalabilityreview.domain.Notification;
import app.aiscalabilityreview.domain.embedded.NotificationPrefs;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Updates;
import core.framework.http.ContentType;
import core.framework.http.HTTPClient;
import core.framework.http.HTTPMethod;
import core.framework.http.HTTPRequest;
import core.framework.inject.Inject;
import core.framework.json.JSON;
import core.framework.mongo.MongoCollection;
import core.framework.mongo.Query;
import core.framework.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Raises and serves console notifications.
 *
 * <p>Notifications are stored rather than pushed: a review takes minutes, and the tab that started
 * it is usually not the tab that reads the result, so an unread badge has to survive a reload.
 */
public class NotificationService {
    public static final String TYPE_RUN_COMPLETED = "RUN_COMPLETED";
    public static final String TYPE_RUN_FAILED = "RUN_FAILED";
    public static final String TYPE_RUN_CANCELLED = "RUN_CANCELLED";
    public static final String TYPE_STAGE_FAILED = "STAGE_FAILED";
    public static final String TYPE_OVER_BUDGET = "OVER_BUDGET";
    public static final String TYPE_VALIDATION_FAILED = "VALIDATION_FAILED";
    public static final String TYPE_RED_SCORE = "RED_SCORE";
    public static final String TYPE_RUN_STALE = "RUN_STALE";

    public static final String SEVERITY_SUCCESS = "SUCCESS";
    public static final String SEVERITY_WARNING = "WARNING";
    public static final String SEVERITY_ERROR = "ERROR";

    private final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    @Inject
    MongoCollection<Notification> notificationCollection;
    @Inject
    AppSettingsService appSettingsService;
    @Inject
    HTTPClient httpClient;

    public void raise(NotificationRequest request) {
        AppSettings settings = appSettingsService.get();
        if (!enabled(settings.notificationPrefs, request.type())) return;

        Notification notification = new Notification();
        notification.notificationId = UUID.randomUUID().toString();
        notification.jobId = request.jobId();
        notification.serviceId = request.serviceId();
        notification.type = request.type();
        notification.severity = request.severity();
        notification.title = request.title();
        notification.body = request.body();
        notification.createdAt = ZonedDateTime.now();
        notificationCollection.insert(notification);
        logger.info("Raised notification {} for job {}", request.type(), request.jobId());

        postToSlack(settings, notification);
    }

    public List<Notification> list(boolean unreadOnly, int limit, int skip) {
        Query query = new Query();
        if (unreadOnly) query.filter = Filters.eq("read_at", null);
        query.sort = Sorts.descending("created_at");
        query.limit = limit;
        query.skip = skip;
        return notificationCollection.find(query);
    }

    public int count(boolean unreadOnly) {
        return (int) notificationCollection.count(unreadOnly ? Filters.eq("read_at", null) : Filters.empty());
    }

    public int markRead(String notificationId) {
        return (int) notificationCollection.update(Filters.eq("_id", notificationId),
            Updates.set("read_at", ZonedDateTime.now()));
    }

    public int markAllRead() {
        return (int) notificationCollection.update(Filters.eq("read_at", null),
            Updates.set("read_at", ZonedDateTime.now()));
    }

    public int clear() {
        return (int) notificationCollection.delete(Filters.empty());
    }

    public void deleteByJob(String jobId) {
        notificationCollection.delete(Filters.eq("job_id", jobId));
    }

    private boolean enabled(NotificationPrefs prefs, String type) {
        if (prefs == null) return true;
        Boolean flag = switch (type) {
            case TYPE_RUN_COMPLETED -> prefs.onCompleted;
            case TYPE_RUN_FAILED -> prefs.onFailed;
            case TYPE_RUN_CANCELLED -> prefs.onCancelled;
            case TYPE_STAGE_FAILED -> prefs.onStageFailed;
            case TYPE_OVER_BUDGET -> prefs.onOverBudget;
            case TYPE_VALIDATION_FAILED -> prefs.onValidationFailed;
            case TYPE_RED_SCORE -> prefs.onRedScore;
            case TYPE_RUN_STALE -> prefs.onStale;
            default -> Boolean.TRUE;
        };
        return !Boolean.FALSE.equals(flag);
    }

    /** Best effort: a console notification is already stored, so a failed webhook is not an error. */
    private void postToSlack(AppSettings settings, Notification notification) {
        NotificationPrefs prefs = settings.notificationPrefs;
        if (prefs == null || !Boolean.TRUE.equals(prefs.slackEnabled)) return;
        if (Strings.isBlank(settings.slackWebhookUrl)) return;
        try {
            HTTPRequest request = new HTTPRequest(HTTPMethod.POST, settings.slackWebhookUrl);
            request.body(JSON.toJSON(Map.of("text", notification.title + " — " + orEmpty(notification.body)))
                .getBytes(StandardCharsets.UTF_8), ContentType.APPLICATION_JSON);
            httpClient.execute(request);
        } catch (RuntimeException e) {
            logger.warn("Could not post the notification to Slack: {}", e.getMessage());
        }
    }

    private String orEmpty(String value) {
        return value == null ? "" : value;
    }

    /**
     * @param type     one of the TYPE_* constants
     * @param severity one of the SEVERITY_* constants
     */
    public record NotificationRequest(String jobId,
                                      String serviceId,
                                      String type,
                                      String severity,
                                      String title,
                                      String body) {
    }
}
