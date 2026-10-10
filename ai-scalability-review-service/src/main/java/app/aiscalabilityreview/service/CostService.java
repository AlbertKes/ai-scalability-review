package app.aiscalabilityreview.service;

import app.aiscalabilityreview.domain.AppSettings;
import app.aiscalabilityreview.domain.embedded.ModelPrice;
import core.framework.inject.Inject;

import java.util.List;

/**
 * Turns token counts into an estimated dollar figure.
 *
 * <p>Cached input is priced separately rather than folded into the input rate: in the pilot run
 * 3,155,521 of the validation stage's 3,931,963 input tokens came from cache, so charging them at
 * the full input rate overstates that stage by a multiple.
 *
 * <p>A model with no row in the price table returns {@code null}, not zero. The table ships
 * unpriced because this service has no authoritative Vertex AI rate card, and "unknown" is the
 * truthful answer until someone fills it in — a zero would read as "this run was free".
 */
public class CostService {
    private static final double PER_MILLION = 1_000_000d;

    @Inject
    AppSettingsService appSettingsService;

    /** @return the estimated cost in USD, or null when the model has no configured price */
    public Double estimate(String model, long inputTokens, long outputTokens, long cachedTokens) {
        return estimate(appSettingsService.get(), model, inputTokens, outputTokens, cachedTokens);
    }

    /** Overload for callers that already hold the settings, to avoid a read per stage. */
    public Double estimate(AppSettings settings, String model, long inputTokens, long outputTokens, long cachedTokens) {
        ModelPrice price = price(settings, model);
        if (price == null || !isPriced(price)) return null;
        long billedInput = Math.max(0, inputTokens - cachedTokens);
        double cost = billedInput / PER_MILLION * orZero(price.inputUsdPerMillion)
            + cachedTokens / PER_MILLION * orZero(price.cachedInputUsdPerMillion)
            + outputTokens / PER_MILLION * orZero(price.outputUsdPerMillion);
        return round(cost);
    }

    /** Sums the per-stage estimates, yielding null when no stage could be priced at all. */
    public Double sum(List<Double> costs) {
        double total = 0d;
        boolean any = false;
        for (Double cost : costs) {
            if (cost != null) {
                total += cost;
                any = true;
            }
        }
        return any ? round(total) : null;
    }

    private boolean isPriced(ModelPrice price) {
        return price.inputUsdPerMillion != null || price.outputUsdPerMillion != null;
    }

    private ModelPrice price(AppSettings settings, String model) {
        if (model == null || settings == null || settings.modelPrices == null) return null;
        for (ModelPrice price : settings.modelPrices) {
            if (model.equals(price.model)) return price;
        }
        return null;
    }

    private double orZero(Double value) {
        return value == null ? 0d : value;
    }

    private double round(double value) {
        return Math.round(value * 10000d) / 10000d;
    }
}
