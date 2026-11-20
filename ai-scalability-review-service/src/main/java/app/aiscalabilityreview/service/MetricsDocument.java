package app.aiscalabilityreview.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The text and the machine-readable snapshot of the collected metrics, built together so a value that
 * is written into one is written into the other.
 */
final class MetricsDocument {
    private static final int INITIAL_CAPACITY = 16384;

    static String num(double value, int decimals) {
        return String.format(Locale.ROOT, "%." + decimals + "f", value);
    }

    /** The source annotation every value carries; the stage that writes the report copies it verbatim. */
    static String source(String query) {
        return " [Source: Datadog API → query-metrics(\"" + query + "\")]";
    }

    final Map<String, Object> snapshot = new LinkedHashMap<>();
    final List<String> gaps = new ArrayList<>();
    private final StringBuilder text = new StringBuilder(INITIAL_CAPACITY);

    void heading(String title) {
        text.append("\n## ").append(title).append('\n');
    }

    void line(String line) {
        text.append(line).append('\n');
    }

    /** A bullet for a value that was collected. */
    void bullet(String label, String value, String query) {
        line("- **" + label + "**: " + value + source(query));
    }

    /** Records a metric that could not be collected, in the body and in the list of gaps. */
    void gap(String label, String query, String reason) {
        line("- **" + label + "**: NOT_COLLECTED: " + reason + source(query));
        gaps.add(label + " — " + reason);
    }

    void put(String key, double value, int decimals) {
        double scale = Math.pow(10, decimals);
        snapshot.put(key, Double.isNaN(value) ? null : (Object) (Math.round(value * scale) / scale));
    }

    void put(String key, Object value) {
        snapshot.put(key, value);
    }

    String text() {
        return text.toString();
    }
}
