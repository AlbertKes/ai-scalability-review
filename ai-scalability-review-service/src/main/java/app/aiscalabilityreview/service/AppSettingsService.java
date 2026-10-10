package app.aiscalabilityreview.service;

import app.aiscalabilityreview.domain.AppSettings;
import app.aiscalabilityreview.domain.embedded.ModelPrice;
import app.aiscalabilityreview.domain.embedded.NotificationPrefs;
import app.aiscalabilityreview.job.LocalReviewRunStats;
import core.framework.inject.Inject;
import core.framework.mongo.MongoCollection;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads and writes the single settings document, seeding it on first use.
 *
 * <p>The price table is seeded with a row per known model id but with no rates. There is no
 * authoritative rate card in this repository, and an invented number presented as a cost is worse
 * than an empty field: the console shows "price table not configured" until the rows are filled in.
 */
public class AppSettingsService {
    public static final String PRICE_TABLE_UNCONFIGURED = "unconfigured";
    private static final List<String> SEEDED_MODELS = List.of(
        "gemini-3.5-flash", "gemini-3.1-flash-lite", "gemini-3.1-pro-preview",
        "gemini-3-pro-preview", "gemini-2.5-pro", "gemini-2.5-flash");

    @Inject
    MongoCollection<AppSettings> appSettingsCollection;

    public AppSettings get() {
        return appSettingsCollection.get(AppSettings.ID).orElseGet(this::seed);
    }

    public AppSettings update(AppSettings settings) {
        settings.id = AppSettings.ID;
        settings.updatedAt = ZonedDateTime.now();
        if (settings.modelPrices == null) settings.modelPrices = defaultPrices();
        if (settings.notificationPrefs == null) settings.notificationPrefs = defaultPrefs();
        appSettingsCollection.replace(settings);
        return get();
    }

    public long tokenBudget() {
        AppSettings settings = get();
        return settings.tokenBudget == null ? LocalReviewRunStats.TOKEN_BUDGET : settings.tokenBudget;
    }

    public long durationBudgetMs() {
        AppSettings settings = get();
        return settings.durationBudgetMs == null ? LocalReviewRunStats.DURATION_BUDGET_MS : settings.durationBudgetMs;
    }

    private AppSettings seed() {
        AppSettings settings = new AppSettings();
        settings.id = AppSettings.ID;
        settings.outputBaseDir = "reports";
        settings.pollIntervalMs = 3000;
        settings.tokenBudget = LocalReviewRunStats.TOKEN_BUDGET;
        settings.durationBudgetMs = LocalReviewRunStats.DURATION_BUDGET_MS;
        settings.retentionKeepLastN = 0;        // 0 = keep everything; pruning is a deliberate act
        settings.retentionMaxAgeDays = 0;
        settings.maxConcurrentRuns = 1;         // artifacts are per run, but the CLI is not cheap
        settings.priceTableVersion = PRICE_TABLE_UNCONFIGURED;
        settings.modelPrices = defaultPrices();
        settings.notificationPrefs = defaultPrefs();
        settings.theme = "light";
        settings.density = "comfortable";
        settings.updatedAt = ZonedDateTime.now();
        appSettingsCollection.insert(settings);
        return settings;
    }

    private List<ModelPrice> defaultPrices() {
        List<ModelPrice> prices = new ArrayList<>(SEEDED_MODELS.size());
        for (String model : SEEDED_MODELS) {
            ModelPrice price = new ModelPrice();
            price.model = model;
            prices.add(price);
        }
        return prices;
    }

    private NotificationPrefs defaultPrefs() {
        NotificationPrefs prefs = new NotificationPrefs();
        prefs.onCompleted = Boolean.TRUE;
        prefs.onFailed = Boolean.TRUE;
        prefs.onCancelled = Boolean.TRUE;
        prefs.onStageFailed = Boolean.TRUE;
        prefs.onOverBudget = Boolean.TRUE;
        prefs.onValidationFailed = Boolean.TRUE;
        prefs.onRedScore = Boolean.TRUE;
        prefs.onStale = Boolean.TRUE;
        prefs.browserEnabled = Boolean.FALSE;
        prefs.slackEnabled = Boolean.FALSE;
        return prefs;
    }
}
