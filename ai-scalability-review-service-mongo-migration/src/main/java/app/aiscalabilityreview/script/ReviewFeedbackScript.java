package app.aiscalabilityreview.script;

import com.mongodb.client.MongoCollection;
import core.ext.mongo.migration.annotation.Flyway;
import core.ext.mongo.migration.annotation.Script;
import org.bson.Document;

import static com.mongodb.client.model.Indexes.ascending;
import static com.mongodb.client.model.Indexes.compoundIndex;

@Flyway(collection = "review_feedback")
public class ReviewFeedbackScript {
    // Queries:
    //   listPendingFeedback → Filters.and(Filters.eq("service_id"), Filters.eq("status", "PENDING"))
    //   feedbackExists      → Filters.eq("confluence_comment_id")
    @Script(ticket = "init", description = "add index", testMethod = "none", runAlways = true)
    public void addIndexes(MongoCollection<Document> collection) {
        collection.createIndex(compoundIndex(ascending("service_id"), ascending("status")));
        collection.createIndex(ascending("confluence_comment_id"));
    }
}
