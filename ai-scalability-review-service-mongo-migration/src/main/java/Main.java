import core.ext.mongo.migration.MongoMigration;
import core.framework.util.Properties;

public class Main {
    public static void main(String[] args) {
        var properties = new Properties();
        properties.load("sys.properties");
        String uri = properties.get("sys.mongo.uri").orElseThrow();
        new MongoMigration(uri).scanPackagePath("app.aiscalabilityreview.script").migration();
    }
}
