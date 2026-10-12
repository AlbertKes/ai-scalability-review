package app.aiscalabilityreview.script;

import com.mongodb.client.MongoCollection;
import core.ext.mongo.migration.annotation.Flyway;
import core.ext.mongo.migration.annotation.Script;
import org.bson.Document;

import static com.mongodb.client.model.Indexes.ascending;
import static com.mongodb.client.model.Indexes.compoundIndex;

@Flyway(collection = "review_presets")
public class ReviewPresetScript {
    // Queries:
    //   list         → [eq(service_id)] [+ eq(env)] + sort(service_id asc, name asc)
    //   clearDefault → update(and(eq(service_id), ne(_id)))
    // One compound serves the filter and the sort; env is only ever an extra conjunct.
    @Script(ticket = "AD-6385-console", description = "add index", testMethod = "none", runAlways = true)
    public void addIndexes(MongoCollection<Document> collection) {
        collection.createIndex(compoundIndex(ascending("service_id"), ascending("name")));
    }
}
