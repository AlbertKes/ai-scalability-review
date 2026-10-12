package app.aiscalabilityreview.script;

import com.mongodb.client.MongoCollection;
import core.ext.mongo.migration.annotation.Flyway;
import core.ext.mongo.migration.annotation.Script;
import org.bson.Document;

import static com.mongodb.client.model.Indexes.ascending;
import static com.mongodb.client.model.Indexes.compoundIndex;
import static com.mongodb.client.model.Indexes.descending;

/**
 * Indexes for the notification bell.
 *
 * <p>This is the collection behind the errors seen at startup: the bell polls the unread count
 * every ten seconds from the moment a page loads, and {@code eq(read_at, null)} had nothing to walk.
 */
@Flyway(collection = "notifications")
public class NotificationScript {
    // Queries:
    //   list(unreadOnly) → [eq(read_at, null)] + sort(created_at desc)
    //   count(unreadOnly) → [eq(read_at, null)], no sort — polled every 10s by the bell
    //   markAllRead      → update(eq(read_at, null))
    //   deleteByJob      → delete(eq(job_id))
    @Script(ticket = "AD-6385-console", description = "add index", testMethod = "none", runAlways = true)
    public void addIndexes(MongoCollection<Document> collection) {
        collection.createIndex(compoundIndex(ascending("read_at"), descending("created_at")));
        collection.createIndex(ascending("job_id"));
    }
}
