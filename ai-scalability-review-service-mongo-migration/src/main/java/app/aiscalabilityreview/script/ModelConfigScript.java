package app.aiscalabilityreview.script;

import com.mongodb.client.MongoCollection;
import core.ext.mongo.migration.annotation.Flyway;
import core.ext.mongo.migration.annotation.Script;
import org.bson.Document;
import static com.mongodb.client.model.Indexes.ascending;

@Flyway(collection = "model_configs")
public class ModelConfigScript {
    @Script(ticket = "init", description = "add index", testMethod = "none", runAlways = true)
    public void addIndexes(MongoCollection<Document> collection) {
        collection.createIndex(ascending("provider"));
    }
}
