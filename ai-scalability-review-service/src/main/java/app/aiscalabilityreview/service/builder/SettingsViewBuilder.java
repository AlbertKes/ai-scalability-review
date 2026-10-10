package app.aiscalabilityreview.service.builder;

import app.aiscalabilityreview.api.settings.SettingsView;
import app.aiscalabilityreview.api.settings.SettingsView.ModelPriceView;
import app.aiscalabilityreview.api.settings.SettingsView.NotificationPrefsView;
import app.aiscalabilityreview.domain.AppSettings;
import app.aiscalabilityreview.domain.embedded.ModelPrice;
import app.aiscalabilityreview.domain.embedded.NotificationPrefs;

import java.util.List;

/** Maps the settings document to and from the shape the settings form posts back. */
public final class SettingsViewBuilder {
    public static SettingsView toView(AppSettings settings) {
        SettingsView view = new SettingsView();
        view.outputBaseDir = settings.outputBaseDir;
        view.defaultOperator = settings.defaultOperator;
        view.pollIntervalMs = settings.pollIntervalMs;
        view.tokenBudget = settings.tokenBudget;
        view.durationBudgetMs = settings.durationBudgetMs;
        view.monthlyCostCeilingUsd = settings.monthlyCostCeilingUsd;
        view.retentionKeepLastN = settings.retentionKeepLastN;
        view.retentionMaxAgeDays = settings.retentionMaxAgeDays;
        view.maxConcurrentRuns = settings.maxConcurrentRuns;
        view.priceTableVersion = settings.priceTableVersion;
        view.slackWebhookUrl = settings.slackWebhookUrl;
        view.theme = settings.theme;
        view.density = settings.density;
        view.updatedAt = settings.updatedAt;
        view.modelPrices = settings.modelPrices == null ? List.of()
            : settings.modelPrices.stream().map(SettingsViewBuilder::toPriceView).toList();
        view.notificationPrefs = toPrefsView(settings.notificationPrefs);
        return view;
    }

    public static AppSettings toDomain(SettingsView view) {
        AppSettings settings = new AppSettings();
        settings.outputBaseDir = view.outputBaseDir;
        settings.defaultOperator = view.defaultOperator;
        settings.pollIntervalMs = view.pollIntervalMs;
        settings.tokenBudget = view.tokenBudget;
        settings.durationBudgetMs = view.durationBudgetMs;
        settings.monthlyCostCeilingUsd = view.monthlyCostCeilingUsd;
        settings.retentionKeepLastN = view.retentionKeepLastN;
        settings.retentionMaxAgeDays = view.retentionMaxAgeDays;
        settings.maxConcurrentRuns = view.maxConcurrentRuns;
        settings.priceTableVersion = view.priceTableVersion;
        settings.slackWebhookUrl = view.slackWebhookUrl;
        settings.theme = view.theme;
        settings.density = view.density;
        settings.modelPrices = view.modelPrices == null ? null
            : view.modelPrices.stream().map(SettingsViewBuilder::toPriceDomain).toList();
        settings.notificationPrefs = toPrefsDomain(view.notificationPrefs);
        return settings;
    }

    private static ModelPriceView toPriceView(ModelPrice price) {
        ModelPriceView view = new ModelPriceView();
        view.model = price.model;
        view.inputUsdPerMillion = price.inputUsdPerMillion;
        view.cachedInputUsdPerMillion = price.cachedInputUsdPerMillion;
        view.outputUsdPerMillion = price.outputUsdPerMillion;
        return view;
    }

    private static ModelPrice toPriceDomain(ModelPriceView view) {
        ModelPrice price = new ModelPrice();
        price.model = view.model;
        price.inputUsdPerMillion = view.inputUsdPerMillion;
        price.cachedInputUsdPerMillion = view.cachedInputUsdPerMillion;
        price.outputUsdPerMillion = view.outputUsdPerMillion;
        return price;
    }

    private static NotificationPrefsView toPrefsView(NotificationPrefs prefs) {
        if (prefs == null) return null;
        NotificationPrefsView view = new NotificationPrefsView();
        view.onCompleted = prefs.onCompleted;
        view.onFailed = prefs.onFailed;
        view.onCancelled = prefs.onCancelled;
        view.onStageFailed = prefs.onStageFailed;
        view.onOverBudget = prefs.onOverBudget;
        view.onValidationFailed = prefs.onValidationFailed;
        view.onRedScore = prefs.onRedScore;
        view.onStale = prefs.onStale;
        view.browserEnabled = prefs.browserEnabled;
        view.slackEnabled = prefs.slackEnabled;
        return view;
    }

    private static NotificationPrefs toPrefsDomain(NotificationPrefsView view) {
        if (view == null) return null;
        NotificationPrefs prefs = new NotificationPrefs();
        prefs.onCompleted = view.onCompleted;
        prefs.onFailed = view.onFailed;
        prefs.onCancelled = view.onCancelled;
        prefs.onStageFailed = view.onStageFailed;
        prefs.onOverBudget = view.onOverBudget;
        prefs.onValidationFailed = view.onValidationFailed;
        prefs.onRedScore = view.onRedScore;
        prefs.onStale = view.onStale;
        prefs.browserEnabled = view.browserEnabled;
        prefs.slackEnabled = view.slackEnabled;
        return prefs;
    }

    private SettingsViewBuilder() {
    }
}
