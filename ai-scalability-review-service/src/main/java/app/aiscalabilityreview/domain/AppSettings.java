package app.aiscalabilityreview.domain;

import app.aiscalabilityreview.domain.embedded.ModelPrice;
import app.aiscalabilityreview.domain.embedded.NotificationPrefs;
import core.framework.mongo.Collection;
import core.framework.mongo.Field;
import core.framework.mongo.Id;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * Singleton settings document for the console, stored under {@link #ID}.
 *
 * <p>The budgets live here rather than as constants because the documented 1,500,000 token target is
 * a pilot figure the real runs already exceed; a budget that is always breached stops being a signal.
 */
@Collection(name = "app_settings")
public class AppSettings {
    public static final String ID = "default";

    @Id
    public String id;

    @Field(name = "output_base_dir")
    public String outputBaseDir;

    @Field(name = "default_operator")
    public String defaultOperator;

    @Field(name = "poll_interval_ms")
    public Integer pollIntervalMs;

    @Field(name = "token_budget")
    public Long tokenBudget;

    @Field(name = "duration_budget_ms")
    public Long durationBudgetMs;

    @Field(name = "monthly_cost_ceiling_usd")
    public Double monthlyCostCeilingUsd;

    @Field(name = "retention_keep_last_n")
    public Integer retentionKeepLastN;

    @Field(name = "retention_max_age_days")
    public Integer retentionMaxAgeDays;

    @Field(name = "max_concurrent_runs")
    public Integer maxConcurrentRuns;

    @Field(name = "price_table_version")
    public String priceTableVersion;

    @Field(name = "model_prices")
    public List<ModelPrice> modelPrices;

    @Field(name = "notification_prefs")
    public NotificationPrefs notificationPrefs;

    @Field(name = "slack_webhook_url")
    public String slackWebhookUrl;

    @Field(name = "theme")
    public String theme;

    @Field(name = "density")
    public String density;

    @Field(name = "updated_at")
    public ZonedDateTime updatedAt;
}
