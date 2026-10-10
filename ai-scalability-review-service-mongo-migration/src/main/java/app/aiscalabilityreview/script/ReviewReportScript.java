package app.aiscalabilityreview.script;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexOptions;
import core.ext.mongo.migration.annotation.Flyway;
import core.ext.mongo.migration.annotation.Script;
import org.bson.Document;

import static com.mongodb.client.model.Indexes.ascending;
import static com.mongodb.client.model.Indexes.compoundIndex;
import static com.mongodb.client.model.Indexes.descending;

@Flyway(collection = "review_reports")
public class ReviewReportScript {
    // Queries:
    //   getReportByJobId         → Filters.eq("job_id")
    //   listReportsByService     → Filters.eq("service_id") + Sorts.descending("generated_at")
    //   findRecentReportsWithConfluence → Filters.exists("confluence_page_id") + Filters.gte("generated_at") + Sorts.descending("generated_at")
    //   jobIdsWithReportMatching → Filters.regex("report_markdown") — the console's "search reports" box
    @Script(ticket = "init", description = "add index", testMethod = "none", runAlways = true)
    public void addIndexes(MongoCollection<Document> collection) {
        collection.createIndex(ascending("job_id"));
        collection.createIndex(compoundIndex(ascending("service_id"), descending("generated_at")));
        collection.createIndex(compoundIndex(ascending("confluence_page_id"), descending("generated_at")),
                new IndexOptions().sparse(true));
        // A plain index rather than a text index: the console promises substring search, and $text
        // only matches whole tokens — it finds "replication_lag" but not "replication". The regex
        // walks this index instead of the documents, which is what notablescan requires. Reports run
        // ~30 KB, so the index is roughly half the size of the collection; acceptable for a local
        // tool with tens of reports, and the reason to revisit this if it ever grows.
        collection.createIndex(ascending("report_markdown"));
    }
}
