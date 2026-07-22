package app.aiscalabilityreview.script;

import com.mongodb.client.MongoCollection;
import core.ext.mongo.migration.annotation.Flyway;
import core.ext.mongo.migration.annotation.Script;
import org.bson.Document;

import static com.mongodb.client.model.Indexes.ascending;
import static com.mongodb.client.model.Indexes.compoundIndex;
import static com.mongodb.client.model.Indexes.descending;

@Flyway(collection = "review_jobs")
public class ReviewJobScript {
    // Queries: listReviewJobs → Filters.eq("service_id") + Sorts.descending("started_at")
    @Script(ticket = "init", description = "add index", testMethod = "none", runAlways = true)
    public void addIndexes(MongoCollection<Document> collection) {
        collection.createIndex(compoundIndex(ascending("service_id"), descending("started_at")));
    }
}
