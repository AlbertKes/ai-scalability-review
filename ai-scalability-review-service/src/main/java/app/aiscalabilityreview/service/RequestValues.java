package app.aiscalabilityreview.service;

import core.framework.util.Strings;

import java.util.Locale;
import java.util.Set;

/**
 * Whether an optional request field is actually set.
 *
 * <p>A blank check alone is not enough: a caller that stringifies an absent field sends the four
 * characters {@code null}, which is not blank and then matches as a substring. That is how an entire
 * Atlas Terraform tree once joined the context of a service with no MongoDB, and why this lives in
 * one place rather than being re-decided per call site.
 */
public final class RequestValues {
    private static final Set<String> ABSENT = Set.of("n/a", "null", "none", "-");

    public static boolean isPresent(String value) {
        return !Strings.isBlank(value) && !ABSENT.contains(value.trim().toLowerCase(Locale.US));
    }

    public static boolean isAbsent(String value) {
        return !isPresent(value);
    }

    private RequestValues() {
    }
}
