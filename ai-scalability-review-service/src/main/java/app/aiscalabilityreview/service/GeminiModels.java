package app.aiscalabilityreview.service;

import app.aiscalabilityreview.domain.embedded.AIModel;
import core.framework.util.Strings;

/**
 * Gemini CLI model ids used by the local review pipeline plus the per-stage defaults (AD-6383).
 *
 * <p>Model tiering rationale: the data-collection stages are mechanical (run a tool, copy the number
 * into a markdown table) and run on the cheapest tier, while the stages that reason — synthesis,
 * scoring and validation — stay on a mid tier. Every default can be overridden per request so a run
 * can be pinned to a different tier for an A/B quality comparison.
 */
public final class GeminiModels {
    /** Cheapest / fastest tier — mechanical data collection, no synthesis. */
    public static final String FLASH_LITE = "gemini-3.1-flash-lite";
    /** Mid tier — synthesis, scoring and validation judgement. */
    public static final String FLASH = "gemini-3.5-flash";

    public static final String CODE_ANALYSIS_DEFAULT = FLASH;
    public static final String METRIC_COLLECTION_DEFAULT = FLASH_LITE;
    public static final String SYNTHESIS_DEFAULT = FLASH;
    public static final String VALIDATION_DEFAULT = FLASH;

    public static String orDefault(String override, String defaultModel) {
        return Strings.isBlank(override) ? defaultModel : override;
    }

    /** Maps a CLI model id onto the persisted enum; unknown ids fall back to the synthesis default. */
    public static AIModel toAIModel(String model) {
        if (model == null) return AIModel.GEMINI_3_5_FLASH;
        return switch (model) {
            case FLASH_LITE -> AIModel.GEMINI_3_1_FLASH_LITE;
            default -> AIModel.GEMINI_3_5_FLASH;
        };
    }

    private GeminiModels() {
    }
}
