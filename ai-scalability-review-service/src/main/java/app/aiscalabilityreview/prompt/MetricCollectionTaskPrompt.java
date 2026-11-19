package app.aiscalabilityreview.prompt;

public class MetricCollectionTaskPrompt {
    /**
     * Prompt for the metric collection stage — the only stage that talks to MCP servers.
     * It collects and writes raw values; it does no scoring, ranking or narrative writing, so it
     * runs on the cheapest model tier and keeps the expensive synthesis stage free of tool round-trips.
     * Placeholders: {{SERVICE}}, {{ENV}}, {{NAMESPACE}}, {{MYSQL_HOST}}, {{MYSQL_DB}},
     * {{ATLAS_CLUSTER}}, {{KAFKA_CONSUMER_GROUPS}}, {{MYSQL_TABLES_PATH}},
     * {{AZURE_RESOURCE_GROUP}}, {{AZURE_SUBSCRIPTION}}, {{OUTPUT_PATH}}
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
        - **MySQL host/cluster**: {{MYSQL_HOST}}  ("N/A" if the service does not use MySQL)
        - **MySQL database**: {{MYSQL_DB}}  ("N/A" if MYSQL_HOST is N/A)
        - **Atlas MongoDB cluster**: {{ATLAS_CLUSTER}}  ("N/A" if the service does not use MongoDB)
        - **Kafka consumer groups**: {{KAFKA_CONSUMER_GROUPS}}  (comma-separated, or "N/A")

        ## The lookback window

        Every Datadog query uses the **same day-aligned 28-day window**, so that the daily buckets
        below come back complete and the four weekly totals are four whole weeks:

        - `to` = today's 00:00:00 UTC, as epoch **seconds**.
        - `from` = `to - 2419200` (28 × 86400).

        Compute both numbers once, state them at the top of your working, and pass exactly those
        values to every `query-metrics` call. Do not use `now` as `to` — a partial final day makes
        the last daily bucket and the last week incomplete.

        ## Rules

        1. **Attribution.** Every value you write carries the source that produced it:
           - `[Source: Datadog MCP → query-metrics("<exact metric query>")]`
           - `[Source: Datadog MCP → <tool name>(<args>)]` for monitors and SLOs
           - `[Source: Azure MCP → <tool name>(<args>)]`
           When you fetch several metrics in one batched call, annotate each value with **its own**
           query expression, not the batched string.
        2. **No approximations.** Write the exact figure returned by the tool. Never write "around",
           "about", "approximately", "roughly", "~", "under" or "over" in front of a value.
        3. **`NOT_COLLECTED` is for tool failures, not for work.** Write `NOT_COLLECTED: <reason>`
           only when the tool returned an error, the component is not used by this service, or the
           metric reported no data at all — and name the source you attempted. Deriving an average,
           a peak, a per-day or per-week total, a growth percentage, or **the timestamp of a peak**
           from data a tool has already returned is part of this task, not a limitation of the tool.
           Never write a reason of the form "complex data derivation", "beyond current capabilities",
           "the MCP does not support …" or "timestamp extraction is not supported" — if the numbers
           are in the response, compute them. Never guess and never estimate.
        4. **Minimise round-trips.** Datadog accepts several metric expressions in a single
           `query-metrics` call as a comma-separated query string. Make exactly the calls listed
           below — they are sized to stay under the response limit described further down. Do not
           re-run a query you already ran in this session; reuse the result.
        5. **Skip conditions are explicit.** When a block says "skip if X is N/A" and X is N/A, write
           the section heading followed by `SKIPPED: <parameter> is N/A` instead of dropping it.

            """ + DatadogMcpPrompt.RESPONSE_RULES + """

        Everything below this line is arithmetic over `points`, and all of it is your job:

        - **Average over the window** = the mean of the point values.
        - **Peak** = the point with the largest value. Its time is derived from its index as above
          and formatted `YYYY-MM-DD HH:mm UTC` — `YYYY-MM-DD` for a daily bucket.
        - **Worst day** = with a `rollup(…, 86400)` bucket, the bucket with the worst value; its date
          is `from + i × 86400`.
        - **Point values are raw.** Use them exactly as returned and convert them with the rule
          stated in each group. Never copy a pre-rounded figure out of `displayName` or `unit`, and
          sanity-check the magnitude: one raw `kubernetes.cpu.usage.total` point for a live pod is
          in the millions of nanocores (1e6 – 1e9), never single digits.
        - **An average comes from an average rollup.** A `rollup(max, …)` series answers "what was
          the worst moment of each day"; the mean of those daily maxima is not an average and must
          never be reported as one. Wherever a group below asks for an average and a peak, it gives
          one expression for each — use each for its own figure.
        - **Bucket with `.rollup(<aggregator>, <seconds>)`** wherever a group asks for per-day or
          per-week figures. It is the only way to get one point per bucket, and it also keeps the
          response small: an unrolled 28-day query returns thousands of points and costs far more
          than the same query rolled up to 28.

        ---

        ## Group 1 — Datadog: daily traffic, latency, errors

        One `query-metrics` call, query string = these expressions joined by commas. Each returns
        exactly 28 daily points over the aligned window:

        - `sum:trace.undertow_http.request.hits{env:{{ENV}},service:{{SERVICE}}}.as_count().rollup(sum, 86400)`
        - `sum:trace.undertow_http.request.errors{env:{{ENV}},service:{{SERVICE}}}.as_count().rollup(sum, 86400)`
        - `p50:trace.undertow_http.request{env:{{ENV}},service:{{SERVICE}}}.rollup(avg, 86400)`
        - `p95:trace.undertow_http.request{env:{{ENV}},service:{{SERVICE}}}.rollup(avg, 86400)`
        - `p99:trace.undertow_http.request{env:{{ENV}},service:{{SERVICE}}}.rollup(avg, 86400)`
        - `p99:trace.undertow_http.request{env:{{ENV}},service:{{SERVICE}}}.rollup(max, 86400)`
        - `sum:kubernetes.containers.restarts{env:{{ENV}},service:{{SERVICE}}}.rollup(sum, 86400)`

        Derive and record, from the daily buckets alone:

        - **Average RPS** = (sum of all 28 daily hit buckets) / 2419200.
        - **Weekly volumes**: group the 28 daily hit buckets into 4 consecutive blocks of 7,
          oldest first, and sum each block. Label each row with the date range of its own buckets,
          taken from the bucket timestamps — `YYYY-MM-DD to YYYY-MM-DD`. This is **exactly 4 rows**;
          no more and no fewer.
        - **Week-over-week average RPS growth** = `(w4 - w3) / w3 × 100`, where `w3` and `w4` are the
          third and fourth weekly volumes above (the two most recent weeks). Record it as a
          percentage with its sign.
        - **P50, P95 and P99 averages** = the mean of the corresponding `rollup(avg, 86400)` series.
          Record them in milliseconds — `trace.undertow_http.request` is reported in **seconds**, so
          ms = value × 1000.
        - **Peak P99** = the largest point of the `rollup(max, 86400)` series, in milliseconds, with
          the date of that bucket.
        - **HTTP error rate, 28-day average** = (sum of all error buckets) / (sum of all hit
          buckets) × 100, as a percentage.
        - **HTTP error rate, worst single day** = the largest per-day `errors[i] / hits[i] × 100`,
          with that bucket's date. When every error bucket is 0, the worst day is `0.0%` — say so
          and give the date of any day; do not mark it `NOT_COLLECTED`.
        - **Container restarts** = the sum of all restart buckets, plus the date of the worst bucket
          if any is non-zero. Note whether any restart was an OOMKill; if this metric cannot
          distinguish the reason, say that instead of inventing a count.

        ## Group 2 — Datadog: peak RPS

        Peak RPS needs a finer bucket than a day. 672 hourly points do not fit in one response, so
        issue **four** `query-metrics` calls, one per week — `from + k × 604800` to
        `from + (k + 1) × 604800` for `k` = 0…3 — each with:

        - `sum:trace.undertow_http.request.hits{env:{{ENV}},service:{{SERVICE}}}.as_count().rollup(sum, 3600)`

        Each returns 168 points. **Peak RPS** = (largest hourly bucket across the four) / 3600,
        recorded with the time of that bucket: its week's `from` + index × 3600.

        ## Group 3 — Datadog: resource saturation

        One `query-metrics` call with these expressions:

        - `avg:kubernetes.cpu.usage.total{env:{{ENV}},service:{{SERVICE}}}.rollup(avg, 86400)`
        - `max:kubernetes.cpu.usage.total{env:{{ENV}},service:{{SERVICE}}}.rollup(max, 86400)`
        - `avg:kubernetes.memory.working_set{env:{{ENV}},service:{{SERVICE}}}.rollup(avg, 86400)`
        - `max:kubernetes.memory.working_set{env:{{ENV}},service:{{SERVICE}}}.rollup(max, 86400)`
        - `avg:kubernetes_state.deployment.replicas{env:{{ENV}},kube_deployment:{{SERVICE}}}.rollup(avg, 86400)`
        - `max:kubernetes_state.deployment.replicas{env:{{ENV}},kube_deployment:{{SERVICE}}}.rollup(max, 86400)`

        **Unit conversion is mandatory** — the later stage scores against percentages of the pod
        limits, so record both the raw value and the converted one:
        - `kubernetes.cpu.usage.total` is reported in **nanocores**. Cores = value / 1000000000.
          A raw point in the millions therefore converts to thousandths of a core; a converted
          figure of the order of 1e-9 cores means you used a rendered number instead of the raw one.
        - `kubernetes.memory.working_set` is reported in **bytes**. MiB = value / 1048576.
        - Percentages need the pod requests and limits, which this stage does not read. Record the
          raw and converted absolute values only, and state the unit explicitly next to each value.

        Record the average and the peak of each, and the date of each peak.

        ## Group 4 — Datadog: Kafka consumer lag

        Skip if {{KAFKA_CONSUMER_GROUPS}} is N/A. Otherwise issue **two calls per consumer group**
        (a comma-separated tag value is an AND filter in Datadog and would return nothing), one per
        expression. A group can consume dozens of topics, so each is capped to its 8 worst topics:

        - `top(avg:kafka.consumer_lag{env:{{ENV}},consumer_group:<group>} by {topic}.rollup(avg, 86400), 8, 'mean', 'desc')`
        - `top(max:kafka.consumer_lag{env:{{ENV}},consumer_group:<group>} by {topic}.rollup(max, 86400), 8, 'max', 'desc')`

        One series comes back per topic; read the topic from each series' `scope`. Record, all in
        messages, per group and per topic that appears in either call: the 28-day average lag = the
        mean of its `rollup(avg, …)` series, and the peak lag with its date from its
        `rollup(max, …)` series. A topic missing from one call is outside that call's top 8 — write
        `< <the 8th value>` for it rather than a number. Measured on one live topic over the same
        28 days, the mean of the daily maxima was 2.82 while the true average was 0.03 — the two
        answer different questions.

        ## Group 5 — Datadog: database metrics

        Skip the MySQL call if {{MYSQL_HOST}} is N/A, and the three MongoDB calls if
        {{ATLAS_CLUSTER}} is N/A. Each list below is **one** `query-metrics` call.

        A datastore the service does not use is **absent, not missing**. Say nothing about it at
        all: no row, no `NOT_COLLECTED` line, no "N/A". `NOT_COLLECTED` means data this review
        should have and could not get, and diluting it with things that were never applicable is
        what makes a reader stop reading it.

        MySQL:
        - `avg:azure.dbformysql_flexibleservers.active_connections{name:{{MYSQL_HOST}}}.rollup(avg, 86400)`
        - `max:azure.dbformysql_flexibleservers.active_connections{name:{{MYSQL_HOST}}}.rollup(max, 86400)`
        - `sum:azure.dbformysql_flexibleservers.slow_queries{name:{{MYSQL_HOST}}}.as_count().rollup(sum, 86400)`
        - `avg:azure.dbformysql_flexibleservers.replication_lag{name:{{MYSQL_HOST}}-*}.rollup(avg, 86400)`
        - `max:azure.dbformysql_flexibleservers.replication_lag{name:{{MYSQL_HOST}}-*}.rollup(max, 86400)`
        - `max:azure.dbformysql_flexibleservers.storage_used{name:{{MYSQL_HOST}}}.rollup(max, 86400)`

        MongoDB performance:
        - `avg:mongodb.atlas.oplatencies.reads.avg{clustername:{{ATLAS_CLUSTER}}}.rollup(avg, 86400)`
        - `avg:mongodb.atlas.oplatencies.writes.avg{clustername:{{ATLAS_CLUSTER}}}.rollup(avg, 86400)`
        - `max:mongodb.atlas.oplatencies.reads.avg{clustername:{{ATLAS_CLUSTER}}}.rollup(max, 86400)`
        - `max:mongodb.atlas.connections.current{clustername:{{ATLAS_CLUSTER}}}.rollup(max, 86400)`
        - `avg:mongodb.atlas.metrics.queryexecutor.scannedperreturned{clustername:{{ATLAS_CLUSTER}}}.rollup(avg, 86400)`
        - `avg:mongodb.atlas.metrics.queryexecutor.scannedperreturned{clustername:{{ATLAS_CLUSTER}}}.rollup(max, 86400)`

        MongoDB storage:
        - `top(sum:mongodb.atlas.dbstats.data.size{clustername:{{ATLAS_CLUSTER}}} by {host}.rollup(max, 86400), 1, 'last', 'desc')`
        - `top(sum:mongodb.atlas.dbstats.index.size{clustername:{{ATLAS_CLUSTER}}} by {host}.rollup(max, 86400), 1, 'last', 'desc')`
        - `top(sum:mongodb.atlas.dbstats.collections{clustername:{{ATLAS_CLUSTER}}} by {host}.rollup(max, 86400), 1, 'last', 'desc')`
        - `top(sum:mongodb.atlas.dbstats.objects{clustername:{{ATLAS_CLUSTER}}} by {host}.rollup(max, 86400), 1, 'last', 'desc')`

        MongoDB disk:
        - `max:mongodb.atlas.system.disk.space.used{clustername:{{ATLAS_CLUSTER}}} by {host}.rollup(max, 86400)`
        - `max:mongodb.atlas.system.disk.space.free{clustername:{{ATLAS_CLUSTER}}} by {host}.rollup(min, 86400)`

        The Atlas aggregators above are not interchangeable, so copy them as written.
        `connections.current` takes `max:` because the busiest member is the one that runs out of
        connections. The targeting ratio is a **ratio**: summing it over a day multiplies it by the
        number of samples, which turned a real 1.5–62 into 50,000–2,000,000 and scored every MongoDB
        service RED on a threshold of 100.

        `dbstats` is reported **once per database on every replica-set member**, and the Atlas
        integration leaves the `database` tag empty (`database:N/A`), so per-database figures are not
        available from Datadog. `sum:… by {host}` adds up the databases on each member, and
        `top(…, 1, 'last', 'desc')` keeps the largest member — every member holds a full copy, so
        their totals agree to within replication delay. A bare `avg:` over the cluster averages
        across databases and members and yields the size of an average database, not of anything
        that exists (measured: `avg:` 3.5 GB
        and 9.6 collections against a real 66.7 GB and 182 collections).

        These are **cluster-wide** figures. The cluster is usually shared by many services, so they
        describe the capacity of the cluster this service depends on, not data this service owns.

        Record, each with the date of its peak bucket:
        - Active connections: 28-day average and peak.
        - Slow queries: daily average = (sum of all buckets) / 28, and the worst single day.
        - Replication lag: the 28-day average = the mean of the `rollup(avg, …)` series, and the peak
          from the `rollup(max, …)` series, in seconds. The replica-name wildcard covers every
          replica of the server.
        - Storage used, in GB: the **last** bucket is the current figure, and last minus first is the
          28-day growth. Report those two, not an average — storage only ever grows, so its mean over
          the window describes no moment that existed.
          The raw points are **bytes**: divide by 1024³ once, and only once. Sanity-check the result
          against the provisioned `StorageSizeGB` from Group 7 — used storage cannot exceed it by
          much, and cannot plausibly be a millionth of it. If your converted figure fails that check
          the raw unit is not what was assumed: write
          `NOT_COLLECTED: storage_used unit unexpected — raw value <value>` and move on rather than
          publishing a number that cannot be true.
        - MongoDB operation latency: read average and peak, write average, in milliseconds.
        - MongoDB connections: peak, and the percentage of the cluster's connection limit it
          represents when that limit is known.
        - MongoDB query targeting ratio (documents scanned per document returned): the 28-day
          average from the `rollup(avg, ...)` series and the worst day from the `rollup(max, ...)`
          one. Report both; a healthy average hides the day a collection scan ran.
        - MongoDB storage (cluster-wide, from the one series each `top(…, 1, …)` returns):
          data size and index size in GB, the collection count and the document count, each from the
          **last** bucket. The `data.size` and `index.size` points are **bytes** (their `unit` is
          `null`): divide by 1024³ once, and only once. Sanity-check the result — a cluster holding
          tens of millions of documents is not 8 bytes and not 8 petabytes. If the converted figure
          is implausible, write `NOT_COLLECTED: dbstats unit unexpected — raw value <value>` rather
          than publishing it. Also report the 28-day growth of data size and document count: last
          minus first, as a percentage. That trend, not the absolute size, is what says whether the
          cluster tier holds for another quarter.
        - MongoDB disk: per member, used and free bytes from the last bucket; record the member with
          the highest `used / (used + free)` — its used GB, its total GB and that percentage. This is
          the figure that runs out first, because data, indexes and the oplog all live on it.
          `dbstats.data.size` is the **uncompressed** size of the documents, so it can exceed the
          disk used several times over (measured: 228 GB of data on 64 GB of disk). That is
          compression, not a unit error — do not reject either figure for it.

            """ + DatadogMcpPrompt.MONITORS_AND_SLOS + """

        ## Group 7 — Azure MCP: live resource configuration

        Skip entirely only if the service uses no Azure-managed resource.

        ### Naming — read this before the first Azure call

        - **Resource group**: `{{AZURE_RESOURCE_GROUP}}`
        - **Subscription**: `{{AZURE_SUBSCRIPTION}}`

        **`{{ENV}}` is a Datadog tag value, not an Azure name.** The resource group is *not*
        derivable from it and never equals it — the environment tagged `uat` lives in the group
        `FTIUat`, and `prod` lives in `RFProdV2`. Passing `{{ENV}}` as a resource group is the single
        most common failure of this group and produces
        `Resource group '{{ENV}}' could not be found`. Pass the value given above, verbatim.

        If the resource group above reads `unknown`, **discover it, do not guess**: call
        `group_list` once and choose the group whose name matches this environment, then use that
        name for every later call in this group. One extra call is far cheaper than a whole group of
        `NOT_COLLECTED`. Same for a subscription of `unknown` — omit the parameter entirely and let
        the Azure CLI profile default apply; do not invent a GUID.

        Azure MCP server names take the **short** server name. Strip any
        `.mysql.database.azure.com` suffix from `{{MYSQL_HOST}}` before passing it.

        ### Calling convention

        The Azure MCP tools are hierarchical command routers, not flat functions: each call sets
        `command` to a sub-command name and wraps that sub-command's arguments in `parameters`. Use
        exactly the sub-command names below — they are the only ones that exist, and a plausible
        invention like `get_mysql_flexible_server` or `get_aks_node_pools` will simply fail. If a
        name below is ever rejected, call the same tool once with `learn=true` to list the real
        sub-commands rather than guessing a second name.

        ### MySQL Flexible Server (skip if {{MYSQL_HOST}} is N/A)

        - `mysql` / `mysql_server_config_get` with `{server, resource-group, subscription}` —
          returns ServerName, Location, Version, SKU, StorageSizeGB, BackupRetentionDays,
          GeoRedundantBackup. Record the SKU, version and provisioned storage in GB.
        - `mysql` / `mysql_server_param_get` with `{param, server, resource-group, subscription}`,
          once per parameter, for `max_connections`, `slow_query_log` and `long_query_time`.
          `max_connections` is the denominator the next stage scores connection saturation against,
          so it matters more than anything else in this group — it is a per-SKU value in the
          thousands, never something to assume.
        - `mysql` / `mysql_list` with `{resource-group, subscription}` — every server in the group.
          Read the replica hosts of `{{MYSQL_HOST}}` off this list; there is no dedicated replica
          sub-command.

        Storage: take **provisioned** storage from `mysql_server_config_get` (StorageSizeGB) and
        **used** storage from the `storage_used` expression already issued in Group 5. Record the
        absolute used figure in GB.

        Do not query `storage_percent`. These servers run with storage auto-grow ON, which moves the
        provisioned figure on its own, so a percentage of it measures the server's willingness to
        grow rather than any risk to the service. Should a later stage ever need the ratio — it does
        only when auto-grow is OFF — it is used ÷ provisioned from the two figures above, so there is
        nothing to collect for it.

        ### AKS node pools

        - `aks` / `aks_cluster_get` with `{resource-group, subscription}` — lists the clusters in the
          group. Take the cluster name from here; do not construct it from `{{ENV}}` or the
          namespace `{{NAMESPACE}}`.
        - `aks` / `aks_nodepool_get` with `{cluster, resource-group, subscription}` — per pool:
          `name`, `vmSize`, `minCount`, `maxCount`, `count`, `enableAutoScaling`, `mode`. Record the
          pool the application runs on, which is the `User`-mode pool with the largest `maxCount`.

        Note that AKS creates a second, auto-managed group named after the cluster (e.g.
        `<group>AKSClusterV2`) holding the node VMSS. The cluster resource itself lives in
        `{{AZURE_RESOURCE_GROUP}}` — query that one, not the node group.

        On failure write `NOT_COLLECTED: Azure MCP error — <reason>` once per affected value and move
        on; do not retry the same call with the same arguments. Retry once only if the reason names
        something you can correct from the rules above — a wrong resource group, a long server name,
        or an invented sub-command.

        ## Group 8 — MySQL table storage: already collected

        **Do not query MySQL and do not look for a MySQL MCP server.** The pipeline collects table
        storage itself over JDBC, before this stage runs, and has already written it to:

        `{{MYSQL_TABLES_PATH}}`

        Read that file with the read tool and copy its body verbatim into Section 9 of your output,
        keeping the `[Source: ...]` annotations it already carries. It holds either the top-20
        table or a single `NOT_COLLECTED:` line; either way, reproduce what it says rather than
        re-deriving it. Take `largest_table_gb` for the JSON snapshot from its largest row
        (total MB / 1024), or `null` when the file reports `NOT_COLLECTED`.

        ---

        ## Output

        Write the collected data to `{{OUTPUT_PATH}}` with the file system write tool, using exactly
        these headings in this order. Values only — no assessment, no colour scores, no advice.

        ```markdown
        # Collected Metrics — {{SERVICE}} ({{ENV}})
        **Collected At**: <YYYY-MM-DD HH:mm zone>
        **Lookback**: 28 days (<from date> to <to date>, UTC day-aligned)

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

        Section 1 contains the average RPS, the peak RPS with its timestamp, the week-over-week
        growth percentage, and the 4-row weekly volume table.

        Section 10 lists every `NOT_COLLECTED` item with its reason, or `None`. Only data that was
        expected and could not be fetched belongs there — a datastore this service does not use is
        not a gap and must not appear.

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
          "mongo_read_latency_peak_ms": null,
          "mongo_write_latency_ms": null,
          "mongo_connections_peak": null,
          "mongo_targeting_ratio_avg": null,
          "mongo_targeting_ratio_peak": null,
          "mongo_data_size_gb": null,
          "mongo_index_size_gb": null,
          "mongo_collections": null,
          "mongo_documents": null,
          "mongo_data_growth_pct": null,
          "mongo_disk_used_gb": null,
          "mongo_disk_used_pct": null,
          "largest_table_gb": null,
          "slo_count": null,
          "slo_breached_count": null,
          "slo_min_error_budget_remaining_pct": null
        }
        ```

        Latency values are milliseconds, lag values are messages, storage values are GB.
        `slo_count` is `0` when the service has no SLO, and the two other SLO keys are then `null`.
        The JSON block is the last thing in the file.
            """;
    }
}
