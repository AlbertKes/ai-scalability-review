package app.aiscalabilityreview.service;

import app.aiscalabilityreview.api.review.ListReviewJobRequest;
import app.aiscalabilityreview.domain.ReviewJob;
import app.aiscalabilityreview.domain.ReviewReport;
import app.aiscalabilityreview.job.LocalReviewStages;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Sorts;
import core.framework.inject.Inject;
import core.framework.mongo.MongoCollection;
import core.framework.mongo.Query;
import core.framework.util.Strings;
import org.bson.conversions.Bson;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * The run list query.
 *
 * <p>Built because the original listing took a mandatory {@code service_id} and filtered on nothing
 * else, which made a global "what has been run" view impossible and left the caller to page without
 * a total.
 */
public class ReviewJobQueryService {
    private static final int MAX_REPORT_MATCHES = 500;

    @Inject
    MongoCollection<ReviewJob> reviewJobCollection;
    @Inject
    MongoCollection<ReviewReport> reviewReportCollection;

    public List<ReviewJob> list(ListReviewJobRequest request) {
        Query query = new Query();
        query.filter = filter(request);
        query.sort = sort(request.sort);
        query.limit = request.limit;
        query.skip = request.skip;
        return reviewJobCollection.find(query);
    }

    public int count(ListReviewJobRequest request) {
        return (int) reviewJobCollection.count(filter(request));
    }

    private Bson filter(ListReviewJobRequest request) {
        List<Bson> filters = new ArrayList<>();
        eq(filters, "service_id", request.serviceId);
        eq(filters, "env", request.env);
        eq(filters, "triggered_by", request.operator);
        eq(filters, "trigger_type", request.triggerType);
        in(filters, "status", request.status);
        in(filters, "overall_score", request.overallScore);
        in(filters, "validation_verdict", request.validationVerdict);
        if (request.withinBudget != null) filters.add(Filters.eq("within_budget", request.withinBudget));
        if (!Strings.isBlank(request.model)) filters.add(Filters.eq("ai_model", request.model));
        if (!Strings.isBlank(request.label)) filters.add(Filters.eq("labels", request.label));
        range(filters, request.from, request.to);
        if (Boolean.TRUE.equals(request.hasFailedStage)) filters.add(failedStageFilter());
        if (!Strings.isBlank(request.keyword)) filters.add(keywordFilter(request));
        return filters.isEmpty() ? Filters.empty() : Filters.and(filters);
    }

    private void eq(List<Bson> filters, String field, String value) {
        if (!Strings.isBlank(value)) filters.add(Filters.eq(field, value));
    }

    private void in(List<Bson> filters, String field, String commaSeparated) {
        if (Strings.isBlank(commaSeparated)) return;
        List<String> values = Arrays.stream(commaSeparated.split(",")).map(String::trim).filter(v -> !v.isEmpty()).toList();
        if (!values.isEmpty()) filters.add(Filters.in(field, values));
    }

    private void range(List<Bson> filters, ZonedDateTime from, ZonedDateTime to) {
        if (from != null) filters.add(Filters.gte("started_at", from));
        if (to != null) filters.add(Filters.lte("started_at", to));
    }

    /** Stage names are a fixed set, so "any stage failed" is an or over the five known keys. */
    private Bson failedStageFilter() {
        List<Bson> stageFilters = new ArrayList<>(LocalReviewStages.ORDER.size());
        for (String stage : LocalReviewStages.ORDER) {
            stageFilters.add(Filters.eq("stage_statuses." + stage + ".status", "FAILED"));
        }
        return Filters.or(stageFilters);
    }

    /**
     * Matches the run's own identifying fields, and — when asked — the body of the report it
     * produced, which is what makes "which services mention replication lag" answerable.
     */
    private Bson keywordFilter(ListReviewJobRequest request) {
        Pattern pattern = Pattern.compile(Pattern.quote(request.keyword), Pattern.CASE_INSENSITIVE);
        List<Bson> matches = new ArrayList<>(List.of(
            Filters.regex("service_id", pattern),
            Filters.regex("_id", pattern),
            Filters.regex("triggered_by", pattern),
            Filters.regex("note", pattern),
            Filters.regex("labels", pattern)));
        if (Boolean.TRUE.equals(request.searchReports)) {
            List<String> jobIds = jobIdsWithReportMatching(pattern);
            if (!jobIds.isEmpty()) matches.add(Filters.in("_id", jobIds));
        }
        return Filters.or(matches);
    }

    private List<String> jobIdsWithReportMatching(Pattern pattern) {
        Query query = new Query();
        query.filter = Filters.regex("report_markdown", pattern);
        query.projection = Projections.include("job_id");
        query.limit = MAX_REPORT_MATCHES;
        return reviewReportCollection.find(query).stream().map(report -> report.jobId).filter(Objects::nonNull).toList();
    }

    private Bson sort(String sort) {
        if (Strings.isBlank(sort)) return Sorts.descending("started_at");
        String[] parts = sort.split(":", 2);
        String field = switch (parts[0]) {
            case "duration" -> "total_duration_ms";
            case "tokens" -> "total_tokens";
            case "cost" -> "estimated_cost_usd";
            case "service" -> "service_id";
            default -> "started_at";
        };
        boolean ascending = parts.length > 1 && "asc".equalsIgnoreCase(parts[1]);
        return ascending ? Sorts.ascending(field) : Sorts.descending(field);
    }
}
