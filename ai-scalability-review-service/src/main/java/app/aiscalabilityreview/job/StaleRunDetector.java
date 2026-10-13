package app.aiscalabilityreview.job;

import app.aiscalabilityreview.domain.ReviewJob;
import app.aiscalabilityreview.service.NotificationService;
import app.aiscalabilityreview.service.NotificationService.NotificationRequest;
import app.aiscalabilityreview.service.ReviewService;
import com.mongodb.client.model.Filters;
import core.framework.inject.Inject;
import core.framework.mongo.MongoCollection;
import core.framework.mongo.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Closes out runs that a restart abandoned.
 *
 * <p>A job left PENDING, QUEUED or RUNNING by a process that is gone has nothing behind it. Leaving
 * it as RUNNING gives the console a spinner that never resolves; marking it STALE says plainly that
 * the run stopped and nothing is known about why.
 */
public class StaleRunDetector {
    private static final String REASON = "The service restarted while this run was in flight; nothing is running behind it.";
    private final Logger logger = LoggerFactory.getLogger(StaleRunDetector.class);

    @Inject
    MongoCollection<ReviewJob> reviewJobCollection;
    @Inject
    ReviewService reviewService;
    @Inject
    LocalReviewRunRegistry runRegistry;
    @Inject
    NotificationService notificationService;

    public void markAbandonedRuns() {
        Query query = new Query();
        query.filter = Filters.in("status",
            List.of(ReviewJob.STATUS_PENDING, ReviewJob.STATUS_QUEUED, ReviewJob.STATUS_RUNNING));
        List<ReviewJob> jobs = reviewJobCollection.find(query);
        for (ReviewJob job : jobs) {
            if (runRegistry.isTracked(job.jobId)) continue;
            reviewService.markStale(job.jobId, REASON);
            notificationService.raise(new NotificationRequest(job.jobId, job.serviceId,
                NotificationService.TYPE_RUN_STALE, NotificationService.SEVERITY_WARNING,
                "Run marked stale — " + job.serviceId, REASON));
            logger.warn("Marked abandoned review job {} as stale", job.jobId);
        }
        if (!jobs.isEmpty()) logger.info("Checked {} in-flight review jobs at startup", jobs.size());
    }
}
