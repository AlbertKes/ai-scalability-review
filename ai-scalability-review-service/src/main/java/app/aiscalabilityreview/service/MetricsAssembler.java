package app.aiscalabilityreview.service;

import app.aiscalabilityreview.service.DatadogMetricsCollector.Collected;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Puts the three sources of the metrics file together: the Datadog figures the pipeline read itself,
 * the Azure state a model collected, and the MySQL table sizes read over JDBC. The synthesis stage
 * and every validation stage read the result, so its section numbers and snapshot keys are a contract.
 */
public final class MetricsAssembler {
    /** The snapshot keys in the order the file lists them; a key nothing wrote is {@code null}. */
    static final List<String> SNAPSHOT_KEYS = List.of(
        "avg_rps", "peak_rps", "wow_rps_growth_pct", "p50_latency_ms", "p95_latency_ms", "p99_latency_ms",
        "p99_latency_peak_ms", "error_rate_pct", "container_restarts", "cpu_avg_cores", "cpu_peak_cores",
        "mem_avg_mib", "mem_peak_mib", "replicas_avg", "replicas_max", "kafka_lag_avg", "kafka_lag_peak",
        "mysql_connection_avg", "mysql_connection_peak", "mysql_max_connections", "mysql_slow_queries_per_day",
        "mysql_replication_lag_avg_s", "mysql_storage_used_gb", "mongo_read_latency_ms", "mongo_read_latency_peak_ms",
        "mongo_write_latency_ms", "mongo_connections_peak", "mongo_targeting_ratio_avg", "mongo_targeting_ratio_peak",
        "mongo_database", "mongo_db_storage_gb", "mongo_db_storage_growth_pct", "mongo_db_data_size_gb",
        "mongo_db_index_size_gb", "mongo_collections", "mongo_documents", "mongo_disk_used_gb", "mongo_disk_used_pct",
        "largest_table_gb", "slo_count", "slo_breached_count", "slo_min_error_budget_remaining_pct");

    private static final Pattern MAX_CONNECTIONS = Pattern.compile("\"mysql_max_connections\"\\s*:\\s*(\\d+)");
    private static final Pattern LARGEST_TABLE = Pattern.compile("Largest table\\*\\*:.*? at ([0-9.]+) GB");
    private static final Pattern AZURE_JSON = Pattern.compile("(?s)\\n?```json.*?```\\s*$");
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC);
    private static final DateTimeFormatter MINUTE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'").withZone(ZoneOffset.UTC);
    private static final int WINDOW_DAYS = 28;

    /**
     * @param azureLive   the Azure session's file, or null when it did not produce one
     * @param mysqlTables the JDBC stage's file, or null
     */
    public static String assemble(String service, String env, Collected datadog, String azureLive, String mysqlTables, long windowEnd) {
        Map<String, Object> snapshot = new LinkedHashMap<>(datadog.snapshot());
        List<String> gaps = new ArrayList<>(datadog.gaps());
        String header = "# Collected Metrics — " + service + " (" + env + ")\n**Collected At**: " + MINUTE.format(Instant.now())
            + "\n**Lookback**: " + WINDOW_DAYS + " days (" + DAY.format(Instant.ofEpochSecond(windowEnd - (long) WINDOW_DAYS * DatadogMetricsCollector.DAY))
            + " to " + DAY.format(Instant.ofEpochSecond(windowEnd)) + ", UTC day-aligned)\n";
        String azure = azureSection(azureLive, snapshot, gaps);
        String mysql = mysqlSection(mysqlTables, snapshot, gaps);
        String gapSection = "\n## 10. Collection Gaps\n" + (gaps.isEmpty() ? "None\n" : gaps.stream().map(gap -> "- " + gap + "\n").collect(Collectors.joining()));
        return header + datadog.markdown() + azure + mysql + gapSection
            + "\n## 11. Metrics Snapshot (JSON)\n```json\n" + snapshotJson(snapshot) + "\n```\n";
    }

    private static String azureSection(String azureLive, Map<String, Object> snapshot, List<String> gaps) {
        String heading = "\n## 8. Infrastructure Live State (Azure MCP)\n";
        if (azureLive == null) {
            gaps.add("Azure live state — the Azure collection did not produce a file");
            return heading + "- NOT_COLLECTED: the Azure collection did not produce a file [Source: Azure MCP]\n";
        }
        Matcher connections = MAX_CONNECTIONS.matcher(azureLive);
        if (connections.find()) snapshot.put("mysql_max_connections", Double.valueOf(connections.group(1)));
        azureLive.lines().filter(line -> line.contains("NOT_COLLECTED")).forEach(line -> gaps.add(line.strip().replaceFirst("^[-*] ", "")));
        return heading + stripHeading(AZURE_JSON.matcher(azureLive).replaceFirst("")).strip() + "\n";
    }

    private static String mysqlSection(String mysqlTables, Map<String, Object> snapshot, List<String> gaps) {
        String heading = "\n## 9. MySQL Table Storage (Top 20)\n";
        if (mysqlTables == null) return heading + "- NOT_COLLECTED: no MySQL table file was produced\n";
        Matcher largest = LARGEST_TABLE.matcher(mysqlTables);
        if (largest.find()) snapshot.put("largest_table_gb", Double.valueOf(largest.group(1)));
        if (mysqlTables.contains("NOT_COLLECTED")) gaps.add("MySQL table storage — " + mysqlTables.strip());
        return heading + mysqlTables.strip() + "\n";
    }

    private static String stripHeading(String azure) {
        return azure.replaceFirst("(?m)^## 8\\..*\\n?", "");
    }

    static String snapshotJson(Map<String, Object> snapshot) {
        return SNAPSHOT_KEYS.stream().map(key -> "  \"" + key + "\": " + render(snapshot.get(key)))
            .collect(Collectors.joining(",\n", "{\n", "\n}"));
    }

    private static String render(Object value) {
        if (value == null) return "null";
        if (value instanceof Double number) {
            if (number.isNaN() || number.isInfinite()) return "null";
            return Double.compare(number, Math.rint(number)) == 0 && Math.abs(number) < 1e15 ? String.valueOf(number.longValue()) : String.valueOf(number);
        }
        if (value instanceof Number number) return number.toString();
        return "\"" + value.toString().replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private MetricsAssembler() {
    }
}
