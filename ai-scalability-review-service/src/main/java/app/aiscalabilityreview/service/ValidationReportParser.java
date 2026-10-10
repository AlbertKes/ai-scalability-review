package app.aiscalabilityreview.service;

import app.aiscalabilityreview.domain.ValidationResult.CheckResult;

import java.util.ArrayList;
import java.util.Locale;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads the verdict and the per-check table out of a validation report.
 *
 * <p>The verdict used to be parsed only to write a log line and then thrown away, which left the
 * console with no way to say whether a report had been checked at all. Parsing is deliberately
 * tolerant: a report whose table cannot be read still yields its verdict, and a missing verdict
 * yields {@code UNKNOWN} rather than an optimistic PASS.
 */
public final class ValidationReportParser {
    public static final String VERDICT_UNKNOWN = "UNKNOWN";

    private static final Pattern VERDICT_PATTERN =
        Pattern.compile("(?i)Overall Validation Verdict:\\s*\\**\\s*([A-Z_ ]+)");
    private static final Pattern CHECK_ROW_PATTERN = Pattern.compile(
        "(?m)^\\|\\s*(\\d+)\\s*\\|\\s*([^|]+?)\\s*\\|\\s*(PASS|FAIL|WARNING|SKIPPED)\\s*\\|\\s*(\\d+)");
    private static final Pattern NOT_COLLECTED_PATTERN = Pattern.compile("NOT_COLLECTED:?\\s*([^\\[\\]\\n]*)");

    public static String verdict(String markdown) {
        if (markdown == null) return VERDICT_UNKNOWN;
        Matcher matcher = VERDICT_PATTERN.matcher(markdown);
        if (!matcher.find()) return VERDICT_UNKNOWN;
        return matcher.group(1).trim().replace(' ', '_').toUpperCase(Locale.ROOT);
    }

    public static List<CheckResult> checks(String markdown) {
        List<CheckResult> checks = new ArrayList<>();
        if (markdown == null) return checks;
        Matcher matcher = CHECK_ROW_PATTERN.matcher(markdown);
        while (matcher.find()) {
            CheckResult check = new CheckResult();
            check.checkNumber = Integer.valueOf(matcher.group(1));
            check.checkName = matcher.group(2).trim();
            check.status = matcher.group(3);
            check.issueCount = Integer.valueOf(matcher.group(4));
            checks.add(check);
        }
        return checks;
    }

    /**
     * Every {@code NOT_COLLECTED} marker and the reason given for it.
     *
     * <p>Surfaced so the console can state plainly how much of the report rests on data that was
     * never collected. A metric that could not be collected must never read as a zero.
     */
    public static List<String> notCollected(String markdown) {
        List<String> reasons = new ArrayList<>();
        if (markdown == null) return reasons;
        Matcher matcher = NOT_COLLECTED_PATTERN.matcher(markdown);
        while (matcher.find()) {
            String reason = matcher.group(1).trim();
            reasons.add(reason.isEmpty() ? "no reason given" : reason);
        }
        return reasons;
    }

    private ValidationReportParser() {
    }
}
