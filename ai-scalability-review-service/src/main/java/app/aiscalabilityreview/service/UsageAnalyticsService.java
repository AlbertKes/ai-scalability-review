package app.aiscalabilityreview.service;

import app.aiscalabilityreview.api.review.GetUsageRequest;
import app.aiscalabilityreview.api.review.GetUsageResponse;
import app.aiscalabilityreview.api.review.GetUsageResponse.UsageBucketView;
import app.aiscalabilityreview.api.review.GetUsageResponse.UsageRunView;
import app.aiscalabilityreview.domain.AppSettings;
import app.aiscalabilityreview.domain.ReviewJob;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import core.framework.inject.Inject;
import core.framework.mongo.MongoCollection;
import core.framework.mongo.Query;
import core.framework.util.Strings;
import org.bson.conversions.Bson;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Token, latency and cost aggregates over the stored runs (FR-7.6).
 *
 * <p>Grouping by stage and by model is the point rather than a convenience: the stage split exists
 * because an agentic loop re-sends the whole context on every tool round-trip, so a stage that
 * holds a large context and makes many tool calls pays the product of the two. Seeing the spend per
 * stage is how a regression in that shape becomes visible.
 */
public class UsageAnalyticsService {
    private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final int TOP_RUNS = 10;
    private static final int MAX_RUNS_SCANNED = 2000;

    @Inject
    MongoCollection<ReviewJob> reviewJobCollection;
    @Inject
    AppSettingsService appSettingsService;

    public GetUsageResponse usage(GetUsageRequest request) {
        AppSettings settings = appSettingsService.get();
        List<ReviewJob> jobs = jobs(request);
        String groupBy = Strings.isBlank(request.groupBy) ? "stage" : request.groupBy;

        GetUsageResponse response = new GetUsageResponse();
        response.groupBy = groupBy;
        response.runs = jobs.size();
        response.priceTableVersion = settings.priceTableVersion;
        response.monthlyCeilingUsd = settings.monthlyCostCeilingUsd;
        applyTotals(response, jobs);
        response.buckets = buckets(jobs, groupBy);
        response.topRuns = topRuns(jobs);
        response.monthToDateCostUsd = monthToDateCost();
        return response;
    }

    private List<ReviewJob> jobs(GetUsageRequest request) {
        List<Bson> filters = new ArrayList<>();
        if (request.from != null) filters.add(Filters.gte("started_at", request.from));
        if (request.to != null) filters.add(Filters.lte("started_at", request.to));
        if (!Strings.isBlank(request.serviceId)) filters.add(Filters.eq("service_id", request.serviceId));
        Query query = new Query();
        query.filter = filters.isEmpty() ? Filters.empty() : Filters.and(filters);
        query.sort = Sorts.descending("started_at");
        query.limit = MAX_RUNS_SCANNED;
        return reviewJobCollection.find(query);
    }

    private void applyTotals(GetUsageResponse response, List<ReviewJob> jobs) {
        long input = 0;
        long output = 0;
        long cached = 0;
        long duration = 0;
        double cost = 0d;
        for (ReviewJob job : jobs) {
            input += orZero(job.totalInputTokens);
            output += orZero(job.totalOutputTokens);
            cached += orZero(job.totalCachedTokens);
            duration += orZero(job.totalDurationMs);
            if (job.estimatedCostUsd != null) cost += job.estimatedCostUsd;
        }
        response.totalInputTokens = input;
        response.totalOutputTokens = output;
        response.totalCachedTokens = cached;
        response.totalTokens = input + output;
        response.totalDurationMs = duration;
        response.totalCostUsd = round(cost);
    }

    private List<UsageBucketView> buckets(List<ReviewJob> jobs, String groupBy) {
        Map<String, UsageBucketView> buckets = new LinkedHashMap<>();
        for (ReviewJob job : jobs) {
            if ("service".equals(groupBy)) {
                accumulateRun(buckets, job.serviceId == null ? "unknown" : job.serviceId, job);
            } else if ("day".equals(groupBy)) {
                accumulateRun(buckets, job.startedAt == null ? "unknown" : DAY_FORMAT.format(job.startedAt), job);
            } else {
                accumulateStages(buckets, job, groupBy);
            }
        }
        List<UsageBucketView> result = new ArrayList<>(buckets.values());
        result.sort(Comparator.comparing((UsageBucketView bucket) -> bucket.totalTokens).reversed());
        return result;
    }

    private void accumulateStages(Map<String, UsageBucketView> buckets, ReviewJob job, String groupBy) {
        if (job.stageStatuses == null) return;
        job.stageStatuses.forEach((stage, status) -> {
            if (status == null) return;
            String key = "model".equals(groupBy) ? (status.model == null ? "unknown" : status.model) : stage;
            UsageBucketView bucket = bucket(buckets, key);
            bucket.runs++;
            bucket.inputTokens += orZero(status.inputTokens);
            bucket.outputTokens += orZero(status.outputTokens);
            bucket.cachedTokens += orZero(status.cachedTokens);
            bucket.totalTokens = bucket.inputTokens + bucket.outputTokens;
            bucket.durationMs += orZero(status.durationMs);
            bucket.mcpToolCalls += orZero(status.mcpToolCalls);
            bucket.apiRequests += orZero(status.apiRequests);
            if (status.estimatedCostUsd != null) bucket.costUsd = round(bucket.costUsd + status.estimatedCostUsd);
        });
    }

    private void accumulateRun(Map<String, UsageBucketView> buckets, String key, ReviewJob job) {
        UsageBucketView bucket = bucket(buckets, key);
        bucket.runs++;
        bucket.inputTokens += orZero(job.totalInputTokens);
        bucket.outputTokens += orZero(job.totalOutputTokens);
        bucket.cachedTokens += orZero(job.totalCachedTokens);
        bucket.totalTokens = bucket.inputTokens + bucket.outputTokens;
        bucket.durationMs += orZero(job.totalDurationMs);
        bucket.mcpToolCalls += orZero(job.totalMcpToolCalls);
        if (job.estimatedCostUsd != null) bucket.costUsd = round(bucket.costUsd + job.estimatedCostUsd);
    }

    private UsageBucketView bucket(Map<String, UsageBucketView> buckets, String key) {
        return buckets.computeIfAbsent(key, name -> {
            UsageBucketView bucket = new UsageBucketView();
            bucket.key = name;
            bucket.runs = 0;
            bucket.inputTokens = 0L;
            bucket.outputTokens = 0L;
            bucket.cachedTokens = 0L;
            bucket.totalTokens = 0L;
            bucket.durationMs = 0L;
            bucket.mcpToolCalls = 0;
            bucket.apiRequests = 0;
            bucket.costUsd = 0d;
            return bucket;
        });
    }

    private List<UsageRunView> topRuns(List<ReviewJob> jobs) {
        return jobs.stream()
            .sorted(Comparator.comparingLong((ReviewJob job) -> orZero(job.totalTokens)).reversed())
            .limit(TOP_RUNS)
            .map(this::toRunView)
            .toList();
    }

    private UsageRunView toRunView(ReviewJob job) {
        UsageRunView view = new UsageRunView();
        view.jobId = job.jobId;
        view.serviceId = job.serviceId;
        view.env = job.env;
        view.startedAt = job.startedAt;
        view.totalTokens = job.totalTokens;
        view.durationMs = job.totalDurationMs;
        view.costUsd = job.estimatedCostUsd;
        view.withinBudget = job.withinBudget;
        return view;
    }

    private Double monthToDateCost() {
        ZonedDateTime from = ZonedDateTime.now().withDayOfMonth(1).toLocalDate().atStartOfDay(ZonedDateTime.now().getZone());
        Query query = new Query();
        query.filter = Filters.gte("started_at", from);
        query.limit = MAX_RUNS_SCANNED;
        double cost = 0d;
        boolean any = false;
        for (ReviewJob job : reviewJobCollection.find(query)) {
            if (job.estimatedCostUsd != null) {
                cost += job.estimatedCostUsd;
                any = true;
            }
        }
        return any ? round(cost) : null;
    }

    private long orZero(Long value) {
        return value == null ? 0 : value;
    }

    private int orZero(Integer value) {
        return value == null ? 0 : value;
    }

    private double round(double value) {
        return Math.round(value * 10000d) / 10000d;
    }
}
