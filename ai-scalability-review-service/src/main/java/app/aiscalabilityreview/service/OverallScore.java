package app.aiscalabilityreview.service;

import java.util.List;
import java.util.Locale;

/**
 * The overall score of a review, from its dimension scores.
 *
 * <p>Three or more RED dimensions make the overall RED; exactly two RED, or more than two YELLOW,
 * make it YELLOW; anything else is GREEN. One RED dimension on its own does not decide the outcome,
 * because a single noisy dimension (a shared database, say) would otherwise colour every review.
 * A score that is not GREEN, YELLOW or RED is UNKNOWN: it counts toward nothing, and an otherwise
 * GREEN result that includes one stays UNKNOWN rather than claiming health nobody measured.
 */
public final class OverallScore {
    private static final String RED = "RED";
    private static final String YELLOW = "YELLOW";
    private static final String GREEN = "GREEN";
    private static final String UNKNOWN = "UNKNOWN";
    private static final int RED_FOR_RED = 3;
    private static final int RED_FOR_YELLOW = 2;
    private static final int YELLOW_FOR_YELLOW = 3;

    /** @return the validation verdict as a score: FAIL is RED, CONDITIONAL PASS YELLOW, PASS GREEN, else null (not counted) */
    public static String ofValidation(String verdict) {
        if (verdict == null) return null;
        return switch (verdict.trim().toUpperCase(Locale.US)) {
            case "FAIL" -> RED;
            case "CONDITIONAL PASS" -> YELLOW;
            case "PASS" -> GREEN;
            default -> null;
        };
    }

    /** @param scores the dimension scores; null entries are ignored */
    public static String of(List<String> scores) {
        int red = 0;
        int yellow = 0;
        boolean unknown = false;
        for (String score : scores) {
            if (score == null) continue;
            if (RED.equals(score)) red++;
            else if (YELLOW.equals(score)) yellow++;
            else if (!GREEN.equals(score)) unknown = true;
        }
        if (red >= RED_FOR_RED) return RED;
        if (red == RED_FOR_YELLOW || yellow >= YELLOW_FOR_YELLOW) return YELLOW;
        return unknown ? UNKNOWN : GREEN;
    }

    private OverallScore() {
    }
}
