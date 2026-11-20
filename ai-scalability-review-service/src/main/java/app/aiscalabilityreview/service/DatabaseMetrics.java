package app.aiscalabilityreview.service;

import app.aiscalabilityreview.service.DatadogMetricsCollector.Run;

import java.util.List;
import java.util.Locale;

import static app.aiscalabilityreview.service.DatadogMetricsCollector.DAY;
import static app.aiscalabilityreview.service.DatadogMetricsCollector.DAYS;
import static app.aiscalabilityreview.service.MetricsDocument.num;

/** Section 5: MySQL and MongoDB. A datastore the service does not use gets no line at all. */
final class DatabaseMetrics {
    private static final double GIB = 1024.0 * 1024 * 1024;
    private static final int OVERVIEW_DATABASES = 10;
    private static final int OVERVIEW_WINDOW = 2 * DAY;

    private final Run run;
    private final MetricsDocument doc;
    private boolean headed;

    DatabaseMetrics(Run run) {
        this.run = run;
        this.doc = run.doc;
    }

    private void head() {
        if (!headed) doc.heading("5. Persistence Layer");
        headed = true;
    }

    void mysql(String host) {
        head();
        String name = "name:" + host;
        String azure = "azure.dbformysql_flexibleservers.";
        String avgConnections = "avg:" + azure + "active_connections{" + name + "}.rollup(avg, " + DAY + ")";
        String maxConnections = "max:" + azure + "active_connections{" + name + "}.rollup(max, " + DAY + ")";
        MetricSeries avg = run.single("MySQL Active Connections - Average", avgConnections);
        if (avg != null) {
            doc.bullet("MySQL Active Connections - Average", num(avg.mean(), 2), avgConnections);
            doc.put("mysql_connection_avg", avg.mean(), 2);
        }
        MetricSeries max = run.single("MySQL Active Connections - Peak", maxConnections);
        if (max != null) {
            doc.bullet("MySQL Active Connections - Peak", Math.round(max.peak()) + " (" + max.dayAt(max.peakIndex()) + ")", maxConnections);
            doc.put("mysql_connection_peak", max.peak(), 0);
        }
        String slowQuery = "sum:" + azure + "slow_queries{" + name + "}.as_count().rollup(sum, " + DAY + ")";
        List<MetricSeries> slow = run.series(slowQuery);
        if (slow == null) {
            doc.gap("MySQL Slow Queries", slowQuery, "the Datadog query failed");
        } else {
            // No series for an as_count sum means no slow query was counted.
            MetricSeries series = slow.isEmpty() ? null : slow.get(0);
            double perDay = series == null ? 0 : series.sum() / DAYS;
            int worst = series == null ? -1 : series.peakIndex();
            doc.bullet("MySQL Slow Queries", num(perDay, 2) + " per day on average (total " + Math.round(series == null ? 0 : series.sum()) + " over 28 days); worst single day "
                + (worst < 0 ? "0" : Math.round(series.peak()) + " (" + series.dayAt(worst) + ")"), slowQuery);
            doc.put("mysql_slow_queries_per_day", perDay, 2);
        }
        replicationLag(host);
        storage(azure, name);
    }

    private void replicationLag(String host) {
        String scope = "{name:" + host + "-*}";
        String avgQuery = "avg:azure.dbformysql_flexibleservers.replication_lag" + scope + ".rollup(avg, " + DAY + ")";
        String maxQuery = "max:azure.dbformysql_flexibleservers.replication_lag" + scope + ".rollup(max, " + DAY + ")";
        MetricSeries avg = run.single("MySQL Replication Lag - Average", avgQuery);
        if (avg != null) {
            doc.bullet("MySQL Replication Lag - Average", num(avg.mean(), 2) + " s", avgQuery);
            doc.put("mysql_replication_lag_avg_s", avg.mean(), 2);
        }
        MetricSeries max = run.single("MySQL Replication Lag - Peak", maxQuery);
        if (max != null) {
            doc.bullet("MySQL Replication Lag - Peak", num(max.peak(), 2) + " s (" + max.dayAt(max.peakIndex()) + ")", maxQuery);
        }
    }

    private void storage(String azure, String name) {
        String query = "max:" + azure + "storage_used{" + name + "}.rollup(max, " + DAY + ")";
        MetricSeries storage = run.single("MySQL Storage Used", query);
        if (storage == null) return;
        double last = storage.last() / GIB;
        double growth = last - storage.first() / GIB;
        doc.bullet("MySQL Storage Used", num(last, 2) + " GB at the last bucket (" + storage.dayAt(storage.size() - 1) + "); 28-day growth "
            + String.format(Locale.ROOT, "%+.2f", growth) + " GB", query);
        doc.put("mysql_storage_used_gb", last, 2);
    }

    void atlas(String cluster, String database) {
        head();
        String scope = "{clustername:" + cluster + "}";
        MetricSeries readAvg = latency("avg:mongodb.atlas.oplatencies.reads.avg", scope, "avg", "MongoDB Read Latency - Average");
        if (readAvg != null) {
            double ms = readAvg.mean();
            doc.put("mongo_read_latency_ms", ms, 2);
        }
        MetricSeries readMax = latency("max:mongodb.atlas.oplatencies.reads.avg", scope, "max", "MongoDB Read Latency - Peak");
        if (readMax != null) doc.put("mongo_read_latency_peak_ms", readMax.peak(), 2);
        MetricSeries write = latency("avg:mongodb.atlas.oplatencies.writes.avg", scope, "avg", "MongoDB Write Latency - Average");
        if (write != null) doc.put("mongo_write_latency_ms", write.mean(), 2);
        String connectionsQuery = "max:mongodb.atlas.connections.current" + scope + ".rollup(max, " + DAY + ")";
        MetricSeries connections = run.single("MongoDB Connections - Peak", connectionsQuery);
        if (connections != null) {
            doc.bullet("MongoDB Connections - Peak", Math.round(connections.peak()) + " (" + connections.dayAt(connections.peakIndex()) + ")", connectionsQuery);
            doc.put("mongo_connections_peak", connections.peak(), 0);
        }
        String targeting = "metrics.queryexecutor.scannedperreturned";
        String targetingAvg = "avg:mongodb.atlas." + targeting + scope + ".rollup(avg, " + DAY + ")";
        String targetingMax = "avg:mongodb.atlas." + targeting + scope + ".rollup(max, " + DAY + ")";
        MetricSeries ratioAvg = run.single("MongoDB Query Targeting Ratio - Average", targetingAvg);
        MetricSeries ratioMax = run.single("MongoDB Query Targeting Ratio - Worst Day", targetingMax);
        if (ratioAvg != null && ratioMax != null) {
            doc.bullet("MongoDB Query Targeting Ratio (documents scanned per returned)", "28-day average " + num(ratioAvg.mean(), 2) + "; worst day "
                + num(ratioMax.peak(), 2) + " (" + ratioMax.dayAt(ratioMax.peakIndex()) + ")", targetingAvg);
            doc.put("mongo_targeting_ratio_avg", ratioAvg.mean(), 2);
            doc.put("mongo_targeting_ratio_peak", ratioMax.peak(), 2);
        }
        if (RequestValues.isAbsent(database)) {
            overview(scope);
        } else {
            databaseStorage(cluster, database);
        }
        disk(scope);
    }

    private MetricSeries latency(String prefix, String scope, String rollup, String label) {
        String query = prefix + scope + ".rollup(" + rollup + ", " + DAY + ")";
        MetricSeries series = run.single(label, query);
        if (series != null) {
            double value = "max".equals(rollup) ? series.peak() : series.mean();
            doc.bullet(label, num(value, 2) + " ms", query);
        }
        return series;
    }

    private void databaseStorage(String cluster, String database) {
        String scope = "{clustername:" + cluster + ",databasename:" + database + "} by {databasename}";
        String storageQuery = "avg:mongodb.atlas.dbstats.storage.size" + scope + ".rollup(max, " + DAY + ")";
        MetricSeries storage = run.single("MongoDB Database Storage", storageQuery);
        doc.put("mongo_database", database);
        if (storage == null) return;
        double last = storage.last() / GIB;
        double firstValue = storage.first() / GIB;
        double growth = firstValue == 0 ? Double.NaN : (last - firstValue) / firstValue * 100;
        doc.bullet("MongoDB Database Storage (`" + database + "`, on disk)", num(last, 2) + " GB at the last bucket; 28-day growth "
            + (Double.isNaN(growth) ? "n/a" : String.format(Locale.ROOT, "%+.2f%%", growth)), storageQuery);
        doc.put("mongo_db_storage_gb", last, 2);
        doc.put("mongo_db_storage_growth_pct", growth, 2);
        MetricSeries data = run.single("MongoDB Database Data Size", "avg:mongodb.atlas.dbstats.data.size" + scope + ".rollup(max, " + DAY + ")");
        if (data != null) {
            doc.bullet("MongoDB Database Data Size (uncompressed)", num(data.last() / GIB, 2) + " GB", "avg:mongodb.atlas.dbstats.data.size" + scope + ".rollup(max, " + DAY + ")");
            doc.put("mongo_db_data_size_gb", data.last() / GIB, 2);
        }
        MetricSeries index = run.single("MongoDB Database Index Size", "avg:mongodb.atlas.dbstats.index.size" + scope + ".rollup(max, " + DAY + ")");
        if (index != null) {
            doc.bullet("MongoDB Database Index Size", num(index.last() / GIB, 2) + " GB", "avg:mongodb.atlas.dbstats.index.size" + scope + ".rollup(max, " + DAY + ")");
            doc.put("mongo_db_index_size_gb", index.last() / GIB, 2);
        }
        MetricSeries collections = run.single("MongoDB Collections", "avg:mongodb.atlas.dbstats.collections" + scope + ".rollup(max, " + DAY + ")");
        if (collections != null) {
            doc.bullet("MongoDB Collections", String.valueOf(Math.round(collections.last())), "avg:mongodb.atlas.dbstats.collections" + scope + ".rollup(max, " + DAY + ")");
            doc.put("mongo_collections", collections.last(), 0);
        }
        MetricSeries objects = run.single("MongoDB Documents", "avg:mongodb.atlas.dbstats.objects" + scope + ".rollup(max, " + DAY + ")");
        if (objects != null) {
            doc.bullet("MongoDB Documents", String.valueOf(Math.round(objects.last())), "avg:mongodb.atlas.dbstats.objects" + scope + ".rollup(max, " + DAY + ")");
            doc.put("mongo_documents", objects.last(), 0);
        }
    }

    /** The service's database is unknown, so the cluster's largest are listed and nothing is scored from them. */
    private void overview(String scope) {
        String query = "top(avg:mongodb.atlas.dbstats.storage.size" + scope + " by {databasename}.rollup(max, " + DAY + "), " + OVERVIEW_DATABASES + ", 'last', 'desc')";
        List<MetricSeries> series = MetricSeries.of(run.datadog.queryRange(query, run.to - OVERVIEW_WINDOW, run.to));
        if (series == null || series.isEmpty()) {
            doc.gap("MongoDB Cluster Overview", query, series == null ? "the Datadog query failed" : "the query returned no data");
            return;
        }
        doc.line("- **MongoDB cluster overview** (largest databases, GB on disk; shared, not scored) [Source: Datadog API → query-metrics(\"" + query + "\")]:");
        for (MetricSeries one : series) {
            doc.line("  - `" + one.tag("databasename") + "`: " + num(one.last() / GIB, 2) + " GB");
        }
    }

    private void disk(String scope) {
        String usedQuery = "max:mongodb.atlas.system.disk.space.used" + scope + " by {host}.rollup(max, " + DAY + ")";
        String freeQuery = "max:mongodb.atlas.system.disk.space.free" + scope + " by {host}.rollup(min, " + DAY + ")";
        List<MetricSeries> used = run.series(usedQuery);
        List<MetricSeries> free = run.series(freeQuery);
        if (used == null || free == null || used.isEmpty() || free.isEmpty()) {
            doc.gap("MongoDB Disk (cluster-wide)", usedQuery, used == null || free == null ? "the Datadog query failed" : "no data reported in the window");
            return;
        }
        String bestHost = null;
        double bestUsed = 0;
        double bestTotal = 0;
        for (MetricSeries member : used) {
            String host = member.tag("host");
            for (MetricSeries spare : free) {
                if (host != null && host.equals(spare.tag("host"))) {
                    double total = member.last() + spare.last();
                    if (total > 0 && (bestHost == null || member.last() / total > bestUsed / bestTotal)) {
                        bestHost = host;
                        bestUsed = member.last();
                        bestTotal = total;
                    }
                }
            }
        }
        if (bestHost == null) {
            doc.gap("MongoDB Disk (cluster-wide)", usedQuery, "no member reports both used and free space");
            return;
        }
        doc.bullet("MongoDB Disk (cluster-wide, shared; fullest member `" + bestHost + "`)", num(bestUsed / GIB, 2) + " GB used of " + num(bestTotal / GIB, 2)
            + " GB (" + num(bestUsed / bestTotal * 100, 1) + "%)", usedQuery);
        doc.put("mongo_disk_used_gb", bestUsed / GIB, 2);
        doc.put("mongo_disk_used_pct", bestUsed / bestTotal * 100, 1);
    }
}
