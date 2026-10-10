package app.aiscalabilityreview.job;

import java.util.List;
import java.util.Map;

/** The five pipeline stages, in order, with the artifacts each one is expected to leave behind. */
public final class LocalReviewStages {
    public static final String CODE_ANALYSIS = "STAGE_1_CODE_ANALYSIS";
    public static final String INFRA_CONTEXT = "STAGE_2_INFRA_CONTEXT";
    public static final String METRIC_COLLECTION = "STAGE_3_METRIC_COLLECTION";
    public static final String SYNTHESIS = "STAGE_4_SYNTHESIS";
    public static final String VALIDATION = "STAGE_5_VALIDATION";

    public static final List<String> ORDER = List.of(
        CODE_ANALYSIS, INFRA_CONTEXT, METRIC_COLLECTION, SYNTHESIS, VALIDATION);

    private static final Map<String, String> DISPLAY_NAMES = Map.of(
        CODE_ANALYSIS, "Code analysis",
        INFRA_CONTEXT, "Infra context",
        METRIC_COLLECTION, "Metric collection",
        SYNTHESIS, "Synthesis & scoring",
        VALIDATION, "Validation");

    public static String displayName(String stage) {
        return DISPLAY_NAMES.getOrDefault(stage, stage);
    }

    public static int indexOf(String stage) {
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
        if (artifactName.endsWith("-validation.md")) return VALIDATION;
        return null;
    }

    private LocalReviewStages() {
    }
}
