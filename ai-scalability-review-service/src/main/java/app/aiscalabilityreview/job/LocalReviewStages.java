package app.aiscalabilityreview.job;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** The pipeline stages, in order, with the artifacts each one is expected to leave behind. */
public final class LocalReviewStages {
    public static final String CODE_ANALYSIS = "STAGE_1_CODE_ANALYSIS";
    public static final String INFRA_CONTEXT = "STAGE_2_INFRA_CONTEXT";
    public static final String METRIC_COLLECTION = "STAGE_3_METRIC_COLLECTION";
    public static final String SYNTHESIS = "STAGE_4_SYNTHESIS";
    /** Checks 3, 4, 5, 7 — decided from the report alone, so no MCP server and no repository. */
    public static final String VALIDATION_STATIC = "STAGE_5A_VALIDATION_STATIC";
    /** Checks 1, 2, 6 — re-queries Datadog and Azure, re-reads the infra repository. */
    public static final String VALIDATION_DATA = "STAGE_5B_VALIDATION_DATA";
    /** Check 8 — re-reads the application source to re-verify each recommendation. */
    public static final String VALIDATION_ADVICE = "STAGE_5C_VALIDATION_ADVICE";
    /** Merges the three partials into one report; runs in this service and spends no tokens. */
    public static final String VALIDATION_REPORT = "STAGE_5D_VALIDATION_REPORT";

    /**
     * Validation before it was split. Jobs recorded under the old name still exist, and a retry of
     * one of them names it, so it still has to resolve to a position in the pipeline.
     */
    public static final String LEGACY_VALIDATION = "STAGE_5_VALIDATION";

    public static final List<String> ORDER = List.of(
        CODE_ANALYSIS, INFRA_CONTEXT, METRIC_COLLECTION, SYNTHESIS,
        VALIDATION_STATIC, VALIDATION_DATA, VALIDATION_ADVICE, VALIDATION_REPORT);

    /** The three Gemini sessions validation is split across; the fourth stage merges their output. */
    public static final List<String> VALIDATION_GROUPS = List.of(
        VALIDATION_STATIC, VALIDATION_DATA, VALIDATION_ADVICE);

    private static final Map<String, String> DISPLAY_NAMES = displayNames();

    private static Map<String, String> displayNames() {
        Map<String, String> names = new HashMap<>();
        names.put(CODE_ANALYSIS, "Code analysis");
        names.put(INFRA_CONTEXT, "Infra context");
        names.put(METRIC_COLLECTION, "Metric collection");
        names.put(SYNTHESIS, "Synthesis & scoring");
        names.put(VALIDATION_STATIC, "Validation: report self-check");
        names.put(VALIDATION_DATA, "Validation: data re-check");
        names.put(VALIDATION_ADVICE, "Validation: recommendation soundness");
        names.put(VALIDATION_REPORT, "Validation: report assembly");
        names.put(LEGACY_VALIDATION, "Validation");
        return Map.copyOf(names);
    }

    public static String displayName(String stage) {
        return DISPLAY_NAMES.getOrDefault(stage, stage);
    }

    /** The old validation stage resolves to the first of the stages that replaced it. */
    public static int indexOf(String stage) {
        if (LEGACY_VALIDATION.equals(stage)) return ORDER.indexOf(VALIDATION_STATIC);
        return ORDER.indexOf(stage);
    }

    public static boolean isValid(String stage) {
        return indexOf(stage) >= 0;
    }

    /** Stage that produced an artifact file name, or null when it cannot be attributed. */
    public static String producerOf(String artifactName) {
        if (LocalReviewOutputPaths.CODE_CONTEXT.equals(artifactName)) return CODE_ANALYSIS;
        if (LocalReviewOutputPaths.INFRA_CONTEXT.equals(artifactName)
            || LocalReviewOutputPaths.MYSQL_TABLES.equals(artifactName)) return INFRA_CONTEXT;
        if (LocalReviewOutputPaths.METRICS.equals(artifactName)) return METRIC_COLLECTION;
        if (LocalReviewOutputPaths.REPORT_SNAPSHOT.equals(artifactName)
            || artifactName.endsWith("-review.md")) return SYNTHESIS;
        if (LocalReviewOutputPaths.VALIDATION_STATIC.equals(artifactName)) return VALIDATION_STATIC;
        if (LocalReviewOutputPaths.VALIDATION_DATA.equals(artifactName)) return VALIDATION_DATA;
        if (LocalReviewOutputPaths.VALIDATION_ADVICE.equals(artifactName)) return VALIDATION_ADVICE;
        if (artifactName.endsWith("-validation.md")) return VALIDATION_REPORT;
        return null;
    }

    private LocalReviewStages() {
    }
}
