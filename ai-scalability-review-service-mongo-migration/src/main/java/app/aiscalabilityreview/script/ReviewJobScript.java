package app.aiscalabilityreview.script;

import com.mongodb.client.MongoCollection;
import core.ext.mongo.migration.annotation.Flyway;
import core.ext.mongo.migration.annotation.Script;
import org.bson.Document;

import java.util.List;

import static com.mongodb.client.model.Indexes.ascending;
import static com.mongodb.client.model.Indexes.compoundIndex;
import static com.mongodb.client.model.Indexes.descending;

/**
 * Indexes for the run list and the console's run queries.
 *
 * <p>The local MongoDB runs with {@code notablescan=1}, so a query with any predicate that no index
 * can serve fails outright with {@code NoQueryExecutionPlans} rather than quietly scanning. Two
 * facts shape the set below:
 *
 * <ul>
 *   <li>Every list query sorts by {@code started_at}, so {@code {started_at:-1}} alone gives the
 *       planner an index to walk and already rescues every <em>find</em>, whatever it filters on.</li>
 *   <li>The same page also asks for an exact total, and {@code countDocuments} has no sort to lean
 *       on — so every field the console can filter by <em>on its own</em> needs its own index. That,
 *       not the find, is why this list is long.</li>
 * </ul>
 *
 * <p>Fields the UI leads with are compounded with {@code started_at} so one index serves the filter
 * and the sort together; the rest are single-field, which is all their count needs. The collection
 * holds one document per local review — hundreds at most — so the write cost of carrying them is
 * not a consideration here, and this set should not be copied to a collection where it would be.
 */
@Flyway(collection = "review_jobs")
public class ReviewJobScript {
    /**
     * {@code has_failed_stage} ORs over these paths, and every branch of an {@code $or} has to be
     * indexed or the whole query plans as a collection scan.
     *
     * <p>{@code STAGE_5_VALIDATION} is kept alongside the four stages that replaced it: jobs
     * recorded before validation was split still carry it, and dropping it here would make those
     * documents invisible to the failed-stage filter.
     */
    private static final List<String> STAGES = List.of(
        "STAGE_1_CODE_ANALYSIS", "STAGE_2_INFRA_CONTEXT", "STAGE_3_METRIC_COLLECTION",
        "STAGE_4_SYNTHESIS", "STAGE_5_VALIDATION", "STAGE_5A_VALIDATION_STATIC",
        "STAGE_5B_VALIDATION_DATA", "STAGE_5C_VALIDATION_ADVICE", "STAGE_5D_VALIDATION_REPORT");

    /** Superseded by {@code {status:1, started_at:-1}}, which serves everything it did. */
    private static final String OBSOLETE_STATUS_INDEX = "status_1";

    // Queries:
    //   ReviewJobQueryService.list   → any combination of the filters below + sort on
    //                                  started_at | total_duration_ms | total_tokens | estimated_cost_usd
    //   ReviewJobQueryService.count  → the same filters with no sort
    //   ReviewService.listReviewJobs / listJobsByService → eq(service_id) + sort(started_at)
    //   ReviewService.clearBaseline  → and(eq(service_id), eq(baseline))        [service_id prefix]
    //   StaleRunDetector             → in(status), no sort                      [status prefix]
    //   UsageAnalyticsService        → range(started_at) [+ eq(service_id)] + sort(started_at)
    //   ServiceSummaryService        → no filter + sort(started_at)
    @Script(ticket = "AD-6385-console", description = "add console query indexes", testMethod = "none", runAlways = true)
    public void addIndexes(MongoCollection<Document> collection) {
        dropIfPresent(collection, OBSOLETE_STATUS_INDEX);

        // the sort every list shares, and the date-range filter
        collection.createIndex(descending("started_at"));

        // filter + sort in one scan, for the fields the run list leads with
        collection.createIndex(compoundIndex(ascending("service_id"), descending("started_at")));
        collection.createIndex(compoundIndex(ascending("status"), descending("started_at")));
        collection.createIndex(compoundIndex(ascending("env"), descending("started_at")));
        collection.createIndex(compoundIndex(ascending("triggered_by"), descending("started_at")));

        // secondary filters: single-field is enough, because the find's sort is already covered
        collection.createIndex(ascending("trigger_type"));
        collection.createIndex(ascending("overall_score"));
        collection.createIndex(ascending("validation_verdict"));
        collection.createIndex(ascending("within_budget"));
        collection.createIndex(ascending("ai_model"));
        collection.createIndex(ascending("labels"));
        collection.createIndex(ascending("note"));

        for (String stage : STAGES) {
            collection.createIndex(ascending("stage_statuses." + stage + ".status"));
        }
    }

    /** {@code runAlways} means this executes on every migration, so the drop has to be idempotent. */
    private void dropIfPresent(MongoCollection<Document> collection, String name) {
        for (Document index : collection.listIndexes()) {
            if (name.equals(index.getString("name"))) {
                collection.dropIndex(name);
                return;
            }
        }
    }
}
