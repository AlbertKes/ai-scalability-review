package app.aiscalabilityreview.service;

import app.aiscalabilityreview.service.DatadogMetricProbe.QueryResponse;
import app.aiscalabilityreview.service.DatadogMetricProbe.Series;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * One Datadog series with its arithmetic: the mean, the peak and the date of the peak.
 *
 * <p>All of it is computed here, from the points as the API returned them, because the same sums done
 * by a model reading the response were wrong often enough to fail validation (a peak CPU of 0.0245 cores
 * against a real 1.19, a slow-query average of 0 against a real 974 a day).
 */
final class MetricSeries {
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC);
    private static final DateTimeFormatter HOUR = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'").withZone(ZoneOffset.UTC);

    /** The series of a response; empty when Datadog had no data for the query, null when it could not be asked. */
    static List<MetricSeries> of(QueryResponse response) {
        if (response == null) return null;
        if (response.series == null) return new ArrayList<>();
        List<MetricSeries> result = new ArrayList<>(response.series.size());
        for (Series series : response.series) {
            result.add(new MetricSeries(series));
        }
        return result;
    }

    final String scope;
    private final long[] seconds;
    private final double[] values;

    MetricSeries(Series series) {
        scope = series.scope == null ? "" : series.scope;
        List<List<Double>> points = series.pointlist == null ? List.of() : series.pointlist;
        seconds = new long[points.size()];
        values = new double[points.size()];
        for (int i = 0; i < points.size(); i++) {
            List<Double> point = points.get(i);
            seconds[i] = Math.round(point.get(0) / 1000);
            values[i] = point.size() < 2 || point.get(1) == null ? Double.NaN : point.get(1);
        }
    }

    /** The value of a tag in this series' scope, e.g. {@code topic}, or null. */
    String tag(String key) {
        for (String part : scope.split(",")) {
            if (part.startsWith(key + ":")) return part.substring(key.length() + 1);
        }
        return null;
    }

    int size() {
        return values.length;
    }

    /** Buckets that hold a value; an empty bucket is not a zero. */
    int count() {
        int count = 0;
        for (double value : values) {
            if (!Double.isNaN(value)) count++;
        }
        return count;
    }

    double value(int index) {
        return values[index];
    }

    double sum() {
        double sum = 0;
        for (double value : values) {
            if (!Double.isNaN(value)) sum += value;
        }
        return sum;
    }

    /** Sum of the buckets in {@code [from, to)}. */
    double sum(int from, int to) {
        double sum = 0;
        for (int i = from; i < Math.min(to, values.length); i++) {
            if (!Double.isNaN(values[i])) sum += values[i];
        }
        return sum;
    }

    double mean() {
        int count = count();
        return count == 0 ? Double.NaN : sum() / count;
    }

    /** Index of the largest value, or -1 when there is none. */
    int peakIndex() {
        int best = -1;
        for (int i = 0; i < values.length; i++) {
            if (!Double.isNaN(values[i]) && (best < 0 || values[i] > values[best])) best = i;
        }
        return best;
    }

    double peak() {
        int index = peakIndex();
        return index < 0 ? Double.NaN : values[index];
    }

    /** The newest bucket that holds a value. */
    double last() {
        for (int i = values.length - 1; i >= 0; i--) {
            if (!Double.isNaN(values[i])) return values[i];
        }
        return Double.NaN;
    }

    /** The oldest bucket that holds a value. */
    double first() {
        for (double value : values) {
            if (!Double.isNaN(value)) return value;
        }
        return Double.NaN;
    }

    String dayAt(int index) {
        return DAY.format(Instant.ofEpochSecond(seconds[index]));
    }

    String hourAt(int index) {
        return HOUR.format(Instant.ofEpochSecond(seconds[index]));
    }

    /** The date of the first bucket and of the last, for labelling a block of days. */
    String dayRange(int from, int to) {
        return dayAt(from) + " to " + dayAt(Math.min(to, values.length) - 1);
    }
}
