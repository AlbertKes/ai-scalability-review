package app.aiscalabilityreview.job;

import app.aiscalabilityreview.domain.ServiceConfig;
import app.aiscalabilityreview.service.ReviewService;
import core.framework.async.Executor;
import core.framework.inject.Inject;
import core.framework.scheduler.Job;
import core.framework.scheduler.JobContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Locale;

/**
 * Scheduled job that runs every hour, checks all enabled service configs with a matching cron
 * schedule, and triggers review jobs asynchronously for each matching service.
 */
public class ScheduledScalabilityReviewJob implements Job {
    private final Logger logger = LoggerFactory.getLogger(ScheduledScalabilityReviewJob.class);

    @Inject
    ReviewService reviewService;

    @Inject
    ReviewJobExecutor reviewJobExecutor;

    @Inject
    Executor executor;

    @Override
    public void execute(JobContext context) throws Exception {
        ZonedDateTime now = ZonedDateTime.now();
        List<ServiceConfig> enabledConfigs = reviewService.listEnabledServiceConfigs();

        logger.info("ScheduledReviewJob triggered at {}; checking {} enabled services", now, enabledConfigs.size());

        for (ServiceConfig config : enabledConfigs) {
            if (shouldRunNow(config.reviewSchedule, now)) {
                logger.info("Triggering scheduled review for service {}", config.serviceId);
                try {
                    String jobId = reviewService.createReviewJob(
                            config.serviceId, "SCHEDULED", null, "Scheduled review");

                    // Submit async — capture jobId in effectively-final variable
                    final String finalJobId = jobId;
                    executor.submit("review-" + config.serviceId, () -> {
                        reviewJobExecutor.executeReview(finalJobId);
                    });

                } catch (Exception e) {
                    logger.error("Failed to create/submit scheduled review job for service {}: {}",
                            config.serviceId, e.getMessage());
                }
            }
        }
    }

    /**
     * Determine if a service's review schedule should trigger at the given time.
     *
     * Supports simple schedule descriptors:
     *   - "hourly"           — triggers every time the job runs (every hour)
     *   - "daily"            — triggers at hour 2 (02:00)
     *   - "weekly"           — triggers on Monday at 02:00
     *   - "monthly"          — triggers on the 1st of the month at 02:00
     *   - "@H<hh>"           — triggers at the specified hour (e.g. "@H06" = 06:00)
     *   - "@W<d>H<hh>"       — triggers at day d (1=Mon) and hour hh (e.g. "@W1H06")
     *   - null / blank       — never triggers
     */
    boolean shouldRunNow(String schedule, ZonedDateTime now) {
        if (schedule == null || schedule.isBlank()) return false;

        return switch (schedule.toLowerCase(Locale.US)) {
            case "hourly" -> true;
            case "daily" -> now.getHour() == 2;
            case "weekly" -> now.getDayOfWeek().getValue() == 1 && now.getHour() == 2;
            case "monthly" -> now.getDayOfMonth() == 1 && now.getHour() == 2;
            default -> matchCustomSchedule(schedule, now);
        };
    }

    private boolean matchCustomSchedule(String schedule, ZonedDateTime now) {
        try {
            if (schedule.startsWith("@W")) {
                // @W<dayOfWeek>H<hour>
                int dayOfWeek = Integer.parseInt(schedule.substring(2, 3));
                int hour = Integer.parseInt(schedule.substring(4, 6));
                return now.getDayOfWeek().getValue() == dayOfWeek && now.getHour() == hour;
            }
            if (schedule.startsWith("@H")) {
                // @H<hour>
                int hour = Integer.parseInt(schedule.substring(2, 4));
                return now.getHour() == hour;
            }
        } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
            logger.warn("Could not parse review schedule '{}': {}", schedule, e.getMessage());
        }
        return false;
    }
}