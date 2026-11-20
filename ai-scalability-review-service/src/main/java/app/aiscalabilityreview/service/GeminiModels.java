package app.aiscalabilityreview.service;

import app.aiscalabilityreview.domain.embedded.AIModel;
import core.framework.util.Strings;

import java.util.Set;

/**
 * Gemini CLI model ids used by the local review pipeline plus the per-stage defaults (AD-6383).
 *
 * <p>Model tiering rationale: every stage runs on the mid tier. Metric collection used to run on the
 * cheapest one when a model copied the Datadog figures by hand, and it fabricated round numbers and
 * sent malformed tool arguments; the Datadog figures are now read by the pipeline, and what is left
 * of that stage (choosing Azure sub-commands and resource names) needs the better model. Every default can be overridden per request so a run
 * can be pinned to a different tier for an A/B quality comparison.
 *
 * <p>Gemini CLI 0.69 rewrites the model it is asked for when the account has access to a newer
 * one: asked for {@code gemini-3.5-flash} on Vertex it runs {@code gemini-3.8-flash}, and asked for
 * {@code gemini-3.1-flash-lite} it runs {@code gemini-3.5-flash-lite} (both measured). The defaults
 * therefore name the models that actually run, so the stage log, the cost estimate and the report
 * agree with what was billed; the older ids stay accepted for a pinned comparison.
 */
public final class GeminiModels {
    /** Cheapest / fastest tier — mechanical data collection, no synthesis. */
    public static final String FLASH_LITE = "gemini-3.5-flash-lite";
    /** Mid tier — synthesis, scoring and validation judgement. */
    public static final String FLASH = "gemini-3.8-flash";

    /**
     * Model ids Gemini CLI accepts. An id outside this set is only rejected inside the CLI
     * subprocess, minutes into a run, so the console checks it before anything is started.
     */
    public static final Set<String> VALID_MODELS = Set.of(
        "gemini-3.8-flash", "gemini-3.5-flash-lite", "gemini-3.5-flash", "gemini-3.1-flash-lite",
        "gemini-3.1-pro-preview", "gemini-3-pro-preview", "gemini-2.5-pro", "gemini-2.5-flash");

    public static final String CODE_ANALYSIS_DEFAULT = FLASH;
    public static final String METRIC_COLLECTION_DEFAULT = FLASH;
    public static final String SYNTHESIS_DEFAULT = FLASH;
    public static final String VALIDATION_DEFAULT = FLASH;

    public static String orDefault(String override, String defaultModel) {
        return Strings.isBlank(override) ? defaultModel : override;
    }

    /** Maps a CLI model id onto the persisted enum; unknown ids fall back to the synthesis default. */
    public static AIModel toAIModel(String model) {
        if (model == null) return AIModel.GEMINI_3_8_FLASH;
        return switch (model) {
            case "gemini-3.5-flash-lite" -> AIModel.GEMINI_3_5_FLASH_LITE;
            case "gemini-3.1-flash-lite" -> AIModel.GEMINI_3_1_FLASH_LITE;
            case "gemini-3.5-flash" -> AIModel.GEMINI_3_5_FLASH;
            default -> AIModel.GEMINI_3_8_FLASH;
        };
    }

    private GeminiModels() {
    }
}
