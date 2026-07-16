package app.aiscalabilityreview;

import app.aiscalabilityreview.domain.AuditLog;
import app.aiscalabilityreview.domain.ModelConfig;
import app.aiscalabilityreview.domain.ReportComparison;
import app.aiscalabilityreview.domain.ReviewFeedback;
import app.aiscalabilityreview.domain.ReviewJob;
import app.aiscalabilityreview.domain.ReviewReport;
import app.aiscalabilityreview.domain.ServiceConfig;
import app.aiscalabilityreview.domain.ValidationResult;
import core.framework.http.HTTPClient;
import core.framework.module.App;
import core.framework.module.SystemModule;
import core.framework.mongo.module.MongoConfig;

import java.time.Duration;

public class AIScalabilityReviewApp extends App {
    @Override
    protected void initialize() {
        load(new SystemModule("sys.properties"));
        loadProperties("app.properties");
        bindCollections();
        bindClients();
        load(new ReviewModule());
    }

    private void bindCollections() {
        var mongo = config(MongoConfig.class);
        mongo.uri(requiredProperty("sys.mongo.uri"));
        mongo.collection(AuditLog.class);
        mongo.collection(ModelConfig.class);
        mongo.collection(ReportComparison.class);
        mongo.collection(ReviewFeedback.class);
        mongo.collection(ReviewJob.class);
        mongo.collection(ReviewReport.class);
        mongo.collection(ServiceConfig.class);
        mongo.collection(ValidationResult.class);
    }

    private void bindClients() {
        bind(HTTPClient.class, HTTPClient.builder()
            .timeout(Duration.ofSeconds(60))
            .build());
    }
}
