package app.aiscalabilityreview.service;

import app.aiscalabilityreview.service.DatadogMetricsCollector.Run;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static app.aiscalabilityreview.service.DatadogMetricsCollector.DAY;
import static app.aiscalabilityreview.service.MetricsDocument.num;

/** Section 6: consumer lag of each Kafka consumer group, for its eight worst topics. */
final class KafkaMetrics {
    private static final int TOP_TOPICS = 8;

    private final Run run;
    private final MetricsDocument doc;

    KafkaMetrics(Run run) {
        this.run = run;
        this.doc = run.doc;
    }

    void collect(String env, String consumerGroups) {
        doc.heading("6. Kafka Consumer Lag");
        double worstAverage = Double.NaN;
        double worstPeak = Double.NaN;
        boolean any = false;
        for (String group : consumerGroups.split(",")) {
            String name = group.strip();
            if (name.isEmpty()) continue;
            // One call per group and per aggregation: a comma inside a tag value is an AND filter.
            String avgQuery = "top(avg:kafka.consumer_lag{env:" + env + ",consumer_group:" + name + "} by {topic}.rollup(avg, " + DAY + "), " + TOP_TOPICS + ", 'mean', 'desc')";
            String maxQuery = "top(max:kafka.consumer_lag{env:" + env + ",consumer_group:" + name + "} by {topic}.rollup(max, " + DAY + "), " + TOP_TOPICS + ", 'max', 'desc')";
            List<MetricSeries> averages = run.series(avgQuery);
            List<MetricSeries> maxima = run.series(maxQuery);
            if (averages == null || maxima == null) {
                doc.gap("Kafka Consumer Lag (" + name + ")", avgQuery, "the Datadog query failed");
                continue;
            }
            if (averages.isEmpty() && maxima.isEmpty()) {
                doc.gap("Kafka Consumer Lag (" + name + ")", avgQuery, "no data reported in the window");
                continue;
            }
            any = true;
            doc.line("- **Consumer group `" + name + "`** (messages; top " + TOP_TOPICS + " topics) [Source: Datadog API → query-metrics(\"" + avgQuery + "\") and query-metrics(\"" + maxQuery + "\")]:");
            Map<String, MetricSeries> avgByTopic = byTopic(averages);
            Map<String, MetricSeries> maxByTopic = byTopic(maxima);
            Set<String> topics = new LinkedHashSet<>(maxByTopic.keySet());
            topics.addAll(avgByTopic.keySet());
            String avgFloor = floor(averages);
            String maxFloor = floor(maxima);
            for (String topic : topics) {
                MetricSeries avg = avgByTopic.get(topic);
                MetricSeries max = maxByTopic.get(topic);
                doc.line("  - `" + topic + "`: 28-day average " + (avg == null ? "< " + avgFloor : num(avg.mean(), 2))
                    + "; peak " + (max == null ? "< " + maxFloor : num(max.peak(), 0) + " (" + max.dayAt(max.peakIndex()) + ")"));
                if (avg != null && (Double.isNaN(worstAverage) || avg.mean() > worstAverage)) worstAverage = avg.mean();
                if (max != null && (Double.isNaN(worstPeak) || max.peak() > worstPeak)) worstPeak = max.peak();
            }
        }
        if (any) {
            doc.put("kafka_lag_avg", worstAverage, 2);
            doc.put("kafka_lag_peak", worstPeak, 0);
            doc.line("- Snapshot values are the worst topic's 28-day average and the highest peak across the groups above.");
        }
    }

    private Map<String, MetricSeries> byTopic(List<MetricSeries> series) {
        Map<String, MetricSeries> result = new LinkedHashMap<>();
        for (MetricSeries one : series) {
            String topic = one.tag("topic");
            if (topic != null && one.count() > 0) result.put(topic, one);
        }
        return result;
    }

    /** The 8th-largest value of a capped call: a topic that is not in it is below that. */
    private String floor(List<MetricSeries> series) {
        if (series.size() < TOP_TOPICS) return "0 (not in the call: outside its top)";
        double smallest = Double.NaN;
        for (MetricSeries one : series) {
            double value = one.peak();
            if (Double.isNaN(smallest) || value < smallest) smallest = value;
        }
        return num(smallest, 2);
    }
}
