package app.aiscalabilityreview.api.settings;

import core.framework.api.json.Property;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * Console settings (FR-12). Used for both the read and the write, so the form posts back exactly
 * what it was given.
 *
 * <p>The budgets are configurable rather than constant because the documented 1,500,000-token
 * target is a pilot figure real runs already exceed, and a threshold that is always breached stops
 * being a signal.
 */
public class SettingsView {
    @Property(name = "output_base_dir")
    public String outputBaseDir;

    @Property(name = "default_operator")
    public String defaultOperator;

    @Property(name = "poll_interval_ms")
    public Integer pollIntervalMs;

    @Property(name = "token_budget")
    public Long tokenBudget;

    @Property(name = "duration_budget_ms")
    public Long durationBudgetMs;

    @Property(name = "monthly_cost_ceiling_usd")
    public Double monthlyCostCeilingUsd;

    @Property(name = "retention_keep_last_n")
    public Integer retentionKeepLastN;

    @Property(name = "retention_max_age_days")
    public Integer retentionMaxAgeDays;

    @Property(name = "max_concurrent_runs")
    public Integer maxConcurrentRuns;

    @Property(name = "price_table_version")
    public String priceTableVersion;

    @Property(name = "model_prices")
    public List<ModelPriceView> modelPrices;

    @Property(name = "notification_prefs")
    public NotificationPrefsView notificationPrefs;

    @Property(name = "slack_webhook_url")
    public String slackWebhookUrl;

    @Property(name = "theme")
    public String theme;  // light | dark

    @Property(name = "density")
    public String density;  // comfortable | compact

    @Property(name = "updated_at")
    public ZonedDateTime updatedAt;

    public static class ModelPriceView {
        @Property(name = "model")
        public String model;

        @Property(name = "input_usd_per_million")
        public Double inputUsdPerMillion;

        @Property(name = "cached_input_usd_per_million")
        public Double cachedInputUsdPerMillion;

        @Property(name = "output_usd_per_million")
        public Double outputUsdPerMillion;
    }

    public static class NotificationPrefsView {
        @Property(name = "on_completed")
        public Boolean onCompleted;

        @Property(name = "on_failed")
        public Boolean onFailed;

        @Property(name = "on_cancelled")
        public Boolean onCancelled;

        @Property(name = "on_stage_failed")
        public Boolean onStageFailed;

        @Property(name = "on_over_budget")
        public Boolean onOverBudget;

        @Property(name = "on_validation_failed")
        public Boolean onValidationFailed;

        @Property(name = "on_red_score")
        public Boolean onRedScore;

        @Property(name = "on_stale")
        public Boolean onStale;

        @Property(name = "browser_enabled")
        public Boolean browserEnabled;

        @Property(name = "slack_enabled")
        public Boolean slackEnabled;
    }
}
