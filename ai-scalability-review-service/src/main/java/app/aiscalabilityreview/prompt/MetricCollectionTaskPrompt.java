package app.aiscalabilityreview.prompt;

public class MetricCollectionTaskPrompt {
    /**
     * Prompt for the metric collection stage — the only stage that talks to MCP servers.
     * It collects and writes raw values; it does no scoring, ranking or narrative writing, so it
     * runs on the cheapest model tier and keeps the expensive synthesis stage free of tool round-trips.
     * Placeholders: {{SERVICE}}, {{ENV}}, {{NAMESPACE}}, {{MYSQL_HOST}}, {{MYSQL_DB}},
     * {{ATLAS_CLUSTER}}, {{KAFKA_CONSUMER_GROUPS}}, {{MYSQL_MCP}}, {{OUTPUT_PATH}}
     */
    public static final String CONTENT;

    static {
        CONTENT = """
        # Metric Collection Task

        You are a data collector. Retrieve the live configuration and the 28-day metric history for
        one service and write them into a single data file. You do **not** score anything, you do
        **not** write an assessment, and you do **not** produce recommendations — a later stage does
        that from the file you write.

        - **Service**: {{SERVICE}}
        - **Environment**: {{ENV}}
        - **Namespace**: {{NAMESPACE}}
        - **Lookback window**: 28 days (`from = now - 28d`, `to = now`)
        - **MySQL host/cluster**: {{MYSQL_HOST}}  ("N/A" if the service does not use MySQL)
        - **MySQL database**: {{MYSQL_DB}}  ("N/A" if MYSQL_HOST is N/A)
        - **MySQL MCP server**: {{MYSQL_MCP}}
        - **Atlas MongoDB cluster**: {{ATLAS_CLUSTER}}  ("N/A" if the service does not use MongoDB)
        - **Kafka consumer groups**: {{KAFKA_CONSUMER_GROUPS}}  (comma-separated, or "N/A")

        ## Rules

        1. **Attribution.** Every value you write carries the source that produced it:
           - `[Source: Datadog MCP → query_metrics("<exact metric query>")]`
           - `[Source: Azure MCP → <tool name>(<args>)]`
           - `[Source: MySQL MCP ({{MYSQL_MCP}}) → <exact SQL>]`
           When you fetch several metrics in one batched call, annotate each value with **its own**
           query expression, not the batched string.
        2. **No approximations.** Write the exact figure returned by the tool. Never write "around",
           "about", "approximately", "roughly", "~", "under" or "over" in front of a value.
        3. **No invention.** If a value cannot be retrieved, write
           `NOT_COLLECTED: <reason>` and name the source you attempted. Never guess, never estimate,
           and never leave a required bullet out of the file.
        4. **Minimise round-trips.** Datadog accepts several metric expressions in a single
           `query_metrics` call as a comma-separated query string. Group the queries below into one
           call per group. Do not re-run a query you already ran in this session; reuse the result.
        5. **Skip conditions are explicit.** When a block says "skip if X is N/A" and X is N/A, write
           the section heading followed by `SKIPPED: <parameter> is N/A` instead of dropping it.

        ---

        ## Group 1 — Datadog: traffic, latency, errors

        One `query_metrics` call, query string = these expressions joined by commas:

        - `sum:trace.undertow_http.request.hits{env:{{ENV}},service:{{SERVICE}}}.as_count()`
        - `p50:trace.undertow_http.request{env:{{ENV}},service:{{SERVICE}}}`
        - `p95:trace.undertow_http.request{env:{{ENV}},service:{{SERVICE}}}`
        - `p99:trace.undertow_http.request{env:{{ENV}},service:{{SERVICE}}}`
        - `sum:trace.undertow_http.request.errors{env:{{ENV}},service:{{SERVICE}}}.as_count()`
        - `sum:kubernetes.containers.restarts{env:{{ENV}},service:{{SERVICE}}}`

        From the returned series derive and record:
        - Average RPS over the window, and peak RPS with the timestamp of the peak.
        - Request volume for each of the **last 4 complete weeks**, each with its date range.
        - Week-over-week average RPS growth percentage between the last two complete weeks.
        - P50, P95, P99 averages over the window, and the peak P99 value.
        - HTTP error rate = errors / hits, both as a 28-day average and as the worst single day.
        - Total container restarts, and whether any restart was an OOMKill.

        ## Group 2 — Datadog: resource saturation

        One `query_metrics` call with these expressions:

        - `avg:kubernetes.cpu.usage.total{env:{{ENV}},service:{{SERVICE}}}`
        - `max:kubernetes.cpu.usage.total{env:{{ENV}},service:{{SERVICE}}}`
        - `avg:kubernetes.memory.working_set{env:{{ENV}},service:{{SERVICE}}}`
        - `max:kubernetes.memory.working_set{env:{{ENV}},service:{{SERVICE}}}`
        - `avg:kubernetes_state.deployment.replicas{env:{{ENV}},kube_deployment:{{SERVICE}}}`
        - `max:kubernetes_state.deployment.replicas{env:{{ENV}},kube_deployment:{{SERVICE}}}`

        **Unit conversion is mandatory** — the later stage scores against percentages of the pod
        limits, so record both the raw value and the converted one:
        - `kubernetes.cpu.usage.total` is reported in **nanocores**. Cores = value / 1000000000.
        - `kubernetes.memory.working_set` is reported in **bytes**. MiB = value / 1048576.
        - Percentages need the pod requests and limits, which this stage does not read. Record the
          raw and converted absolute values only, and state the unit explicitly next to each value.

        ## Group 3 — Datadog: Kafka consumer lag

        Skip if {{KAFKA_CONSUMER_GROUPS}} is N/A. Otherwise issue **one query per consumer group**
        (a comma-separated tag value is an AND filter in Datadog and would return nothing):

        `max:kafka.consumer_lag{env:{{ENV}},consumer_group:<group>} by {topic}`

        Record, per group and topic: 28-day average lag and peak lag, both in messages.

        ## Group 4 — Datadog: database metrics

        Skip the MySQL expressions if {{MYSQL_HOST}} is N/A, and the MongoDB ones if
        {{ATLAS_CLUSTER}} is N/A. Otherwise one `query_metrics` call with the applicable expressions:

        - `avg:azure.dbformysql_flexibleservers.active_connections{name:{{MYSQL_HOST}}}`
        - `max:azure.dbformysql_flexibleservers.active_connections{name:{{MYSQL_HOST}}}`
        - `sum:azure.dbformysql_flexibleservers.slow_queries{name:{{MYSQL_HOST}}}.as_count()`
        - `max:azure.dbformysql_flexibleservers.replication_lag{name:{{MYSQL_HOST}}-*}`
        - `avg:mongodb.atlas.oplatencies.reads.avg{clustername:{{ATLAS_CLUSTER}}}`
        - `avg:mongodb.atlas.oplatencies.writes.avg{clustername:{{ATLAS_CLUSTER}}}`
        - `max:mongodb.atlas.oplatencies.reads.avg{clustername:{{ATLAS_CLUSTER}}}`
        - `max:mongodb.atlas.connections.current{clustername:{{ATLAS_CLUSTER}}}`
        - `sum:mongodb.atlas.metrics.queryexecutor.scannedperreturned{clustername:{{ATLAS_CLUSTER}}}`

        Record averages and peaks, plus the slow-query daily average and the worst single day.

        ## Group 5 — Datadog: SLOs and monitors

        Call `list_monitors` (and `list_slos` if the server exposes it) filtered to
        `service:{{SERVICE}}`. Record every monitor in ALERT or WARN state with its name and state,
        and the compliance figure of each SLO. If there are none, write `None in ALERT or WARN`.

        ## Group 6 — Azure MCP: live resource configuration

        Skip entirely only if the service uses no Azure-managed resource.

        MySQL Flexible Server (skip if {{MYSQL_HOST}} is N/A):
        - `get_mysql_flexible_server(name={{MYSQL_HOST}})` — SKU, vCPU, RAM, version, HA mode,
          provisioned storage, storage auto-grow, IO scaling, compute tier.
        - `list_mysql_flexible_server_configurations(server_name={{MYSQL_HOST}})` — `max_connections`,
          `slow_query_log`, `long_query_time`.
        - `list_mysql_flexible_server_replicas(server_name={{MYSQL_HOST}})` — replica hosts and HA.
        - `get_metric(resource={{MYSQL_HOST}}, metric=storage_used, aggregation=Average)`.
          If storage auto-grow is ON, collect **only** storage used and write it in GB — do not
          collect `storage_limit` and do not compute a utilisation percentage, because the
          provisioned limit expands automatically and the ratio is meaningless.
          If auto-grow is OFF, also collect
          `get_metric(resource={{MYSQL_HOST}}, metric=storage_limit, aggregation=Average)`
          and record both values so the next stage can compute the percentage.
        - `get_metric(resource={{MYSQL_HOST}}, metric=cpu_percent, aggregation=Average)`.

        AKS node pools for the cluster hosting namespace {{NAMESPACE}}:
        - `get_aks_node_pools(cluster=<cluster name>, resource_group=<rg>)` — per pool: node SKU,
          min/max node count, spot vs on-demand.

        On failure write `NOT_COLLECTED: Azure MCP error — <reason>`.

        ## Group 7 — MySQL MCP: table storage

        Execute only if both {{MYSQL_HOST}} and {{MYSQL_DB}} are not N/A. Use the MCP server named
        **{{MYSQL_MCP}}** and run exactly one query:

        ```sql
        SELECT
            table_name,
            table_rows,
            ROUND(data_length / 1024 / 1024, 2)   AS data_mb,
            ROUND(index_length / 1024 / 1024, 2)  AS index_mb,
            ROUND((data_length + index_length) / 1024 / 1024, 2) AS total_mb
        FROM information_schema.tables
        WHERE table_schema = '{{MYSQL_DB}}'
        ORDER BY (data_length + index_length) DESC
        LIMIT 20;
        ```

        Record all 20 rows as a table and flag every table above 10 GB. On failure write
        `NOT_COLLECTED: MySQL MCP error — <reason>`.

        ---

        ## Output

        Write the collected data to `{{OUTPUT_PATH}}` with the file system write tool, using exactly
        these headings in this order. Values only — no assessment, no colour scores, no advice.

        ```markdown
        # Collected Metrics — {{SERVICE}} ({{ENV}})
        **Collected At**: <YYYY-MM-DD HH:mm zone>
        **Lookback**: 28 days

        ## 1. Traffic & Throughput
        ## 2. Latency Profile
        ## 3. Error Rates & Stability
        ## 4. Resource Saturation
        ## 5. Persistence Layer
        ## 6. Kafka Consumer Lag
        ## 7. SLO & Monitors
        ## 8. Infrastructure Live State (Azure MCP)
        ## 9. MySQL Table Storage (Top 20)
        ## 10. Collection Gaps
        ## 11. Metrics Snapshot (JSON)
        ```

        Section 10 lists every `NOT_COLLECTED` item with its reason, or `None`.

        Section 11 is a single fenced ```json block, machine-read by the pipeline. Use exactly these
        keys, numbers without units and without quotes, and `null` for anything not collected:

        ```json
        {
          "avg_rps": null,
          "peak_rps": null,
          "wow_rps_growth_pct": null,
          "p50_latency_ms": null,
          "p95_latency_ms": null,
          "p99_latency_ms": null,
          "p99_latency_peak_ms": null,
          "error_rate_pct": null,
          "container_restarts": null,
          "cpu_avg_cores": null,
          "cpu_peak_cores": null,
          "mem_avg_mib": null,
          "mem_peak_mib": null,
          "replicas_avg": null,
          "replicas_max": null,
          "kafka_lag_avg": null,
          "kafka_lag_peak": null,
          "mysql_connection_avg": null,
          "mysql_connection_peak": null,
          "mysql_max_connections": null,
          "mysql_slow_queries_per_day": null,
          "mysql_replication_lag_avg_s": null,
          "mysql_storage_used_gb": null,
          "mongo_read_latency_ms": null,
          "mongo_write_latency_ms": null,
          "largest_table_gb": null
        }
        ```

        Latency values are milliseconds, lag values are messages, storage values are GB.
        The JSON block is the last thing in the file.
            """;
    }
}
