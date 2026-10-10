package app.aiscalabilityreview.service;

import app.aiscalabilityreview.api.review.ListServiceSummaryResponse;
import app.aiscalabilityreview.api.review.ListServiceSummaryResponse.ServiceSummaryView;
import app.aiscalabilityreview.domain.ReviewJob;
import app.aiscalabilityreview.domain.ServiceConfig;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import core.framework.inject.Inject;
import core.framework.mongo.MongoCollection;
import core.framework.mongo.Query;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One row per service, assembled from the runs rather than from the registry alone.
 *
 * <p>A service that has been reviewed locally but never registered in {@code service_configs} still
 * has to appear — and a registered service that has never been reviewed is exactly the row worth
 * seeing, so both sources are merged.
 */
public class ServiceSummaryService {
    private static final int MAX_RUNS_SCANNED = 2000;
    private static final int THIRTY_DAYS = 30;

    @Inject
    MongoCollection<ReviewJob> reviewJobCollection;
    @Inject
    MongoCollection<ServiceConfig> serviceConfigCollection;

    public ListServiceSummaryResponse list() {
        Map<String, ServiceSummaryView> services = new LinkedHashMap<>();
        for (ServiceConfig config : serviceConfigCollection.find(new Query())) {
            ServiceSummaryView view = view(services, config.serviceId);
            view.hasConfig = Boolean.TRUE;
            view.displayName = config.displayName;
            view.team = config.team;
            view.tier = config.tier == null ? null : config.tier.name();
        }

        ZonedDateTime cutoff = ZonedDateTime.now().minusDays(THIRTY_DAYS);
        for (ReviewJob job : recentFirst()) {
            ServiceSummaryView view = view(services, job.serviceId);
            view.runCount++;
            if (view.lastRunAt == null) {
                view.lastRunAt = job.startedAt;
                view.lastJobId = job.jobId;
                view.lastStatus = job.status;
                view.lastOverallScore = job.overallScore;
                view.lastValidationVerdict = job.validationVerdict;
                view.lastEnv = job.env;
                view.daysSinceLastRun = job.startedAt == null ? null
                    : (int) Duration.between(job.startedAt, ZonedDateTime.now()).toDays();
            }
            if (Boolean.TRUE.equals(job.baseline)) view.baselineJobId = job.jobId;
            if (job.startedAt != null && job.startedAt.isAfter(cutoff)) {
                view.tokens30d += job.totalTokens == null ? 0 : job.totalTokens;
                if (job.estimatedCostUsd != null) {
                    view.cost30dUsd = round(view.cost30dUsd + job.estimatedCostUsd);
                }
            }
        }

        List<ServiceSummaryView> sorted = new ArrayList<>(services.values());
        sorted.sort(Comparator.comparing((ServiceSummaryView view) -> view.serviceId));
        ListServiceSummaryResponse response = new ListServiceSummaryResponse();
        response.services = sorted;
        return response;
    }

    private List<ReviewJob> recentFirst() {
        Query query = new Query();
        query.filter = Filters.empty();
        query.sort = Sorts.descending("started_at");
        query.limit = MAX_RUNS_SCANNED;
        return reviewJobCollection.find(query);
    }

    private ServiceSummaryView view(Map<String, ServiceSummaryView> services, String serviceId) {
        return services.computeIfAbsent(serviceId, id -> {
            ServiceSummaryView view = new ServiceSummaryView();
            view.serviceId = id;
            view.hasConfig = Boolean.FALSE;
            view.runCount = 0;
            view.tokens30d = 0L;
            view.cost30dUsd = 0d;
            return view;
        });
    }

    private double round(double value) {
        return Math.round(value * 10000d) / 10000d;
    }
}
