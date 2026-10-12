package app.aiscalabilityreview.script;

import com.mongodb.client.MongoCollection;
import core.ext.mongo.migration.annotation.Flyway;
import core.ext.mongo.migration.annotation.Script;
import org.bson.Document;

import static com.mongodb.client.model.Indexes.ascending;

@Flyway(collection = "validation_results")
public class ValidationResultScript {
    // Queries: deleteRunData → delete(eq("job_id")); everything else reads by _id.
    @Script(ticket = "AD-6385-console", description = "add index", testMethod = "none", runAlways = true)
    public void addIndexes(MongoCollection<Document> collection) {
        collection.createIndex(ascending("job_id"));
    }
}
