package app.aiscalabilityreview.prompt;

public class ReviewTaskPrompt {
    /**
     * Prompt for the main scalability review task.
     * Placeholders: {{SERVICE}}, {{ENV}}, {{NAMESPACE}}, {{MYSQL_HOST}}, {{MYSQL_DB}},
     * {{ATLAS_CLUSTER}}, {{HPA_TYPE}}, {{KAFKA_CONSUMER_GROUPS}}, {{DOMAIN}}
     */
    public static final String CONTENT;

    static {
        CONTENT = """
        # Scalability Review Task
        
        You are a devops engineer performing a structured scalability review for tier-1
        microservices. Perform a complete review for:
        
        - **Service**: {{SERVICE}}
        - **Environment**: {{ENV}}
        - **Namespace**: {{NAMESPACE}}
        - **Lookback window**: 28 days
        - **MySQL host/cluster**: {{MYSQL_HOST}}  (set to "N/A" if service does not use MySQL)
        - **MySQL database**: {{MYSQL_DB}}  (set to "N/A" if MYSQL_HOST is N/A; required when MYSQL_HOST is set)
        - **Atlas MongoDB cluster**: {{ATLAS_CLUSTER}}  (set to "N/A" if service does not use MongoDB)
        - **HPA type**: {{HPA_TYPE}}  (one of: HPA | none)
        - **Kafka consumer groups**: {{KAFKA_CONSUMER_GROUPS}}  (comma-separated, or "N/A")
        
        ## Data Source Attribution Requirement
        
        For **every** piece of configuration or metric value you report, you must explicitly state
        which data source it came from, using one of these labels:
        
        - `[Source: code → <exact file path>]` — value read from a file in a cloned repository
        - `[Source: context → reports/{{SERVICE}}/code-context.md]` — value from the business context document
        - `[Source: Azure MCP → <tool name>(<args>)]` — value retrieved via Azure MCP tool call
        - `[Source: MySQL MCP → <exact SQL query>]` — value retrieved via MySQL MCP tool call
        - `[Source: Datadog MCP → query_metrics("<exact metric query>")]` — value retrieved via Datadog MCP tool call
        
        If a value cannot be retrieved, write `NOT_COLLECTED: <reason>` and note the source you attempted.
        
        **No approximations.** Never write "around", "about", "approximately", "roughly", "~",
        "under", or "over" before a metric value anywhere in the report. Use the exact figure
        returned by the data source. This rule applies to every section: Executive Summary,
        Infrastructure Configuration, Performance Metrics, and Scoring Rationale.
        
        ---
        
        ## Instructions
        
        ### Step 1 — Load Kubernetes and Infrastructure Configuration
        
        The following files are attached. Read them carefully before querying any metrics.
        For each extracted value, record its exact source file path.
        
        **Kubernetes manifests — infra repo**:
        `[Source: code → infra/{{ENV}}/app/{{DOMAIN}}/kube/resource/]`
        
        **Kubernetes manifests (optional) — k8s-gitops repo**:
        `[Source: code → k8s-gitops/environments/{{ENV}}/apps/{{DOMAIN}}/]`
        
        Extract from the manifests:
        - Deployment CPU/memory requests and limits
        - HPA or DatadogPodAutoscaler: minReplicas, maxReplicas, metric triggers and thresholds,
          stabilizationWindow, scaleDown policy
        - Liveness and readiness probe timeouts and thresholds
        - PodDisruptionBudget (minAvailable / maxUnavailable) if present
        - ConfigMap entries related to connection pools, timeouts, or thread counts
        
        **Terraform — MySQL config** (skip if {{MYSQL_HOST}} is N/A):
        `[Source: code → infra/{{ENV}}/app/infra/env/ and infra/{{ENV}}/app/infra/mysql/]`
        Extract: SKU tier, max_connections parameter, any read replicas, auto scale IOPS, storage autogrow, HA config.
        
        **Terraform — Atlas MongoDB config** (skip if {{ATLAS_CLUSTER}} is N/A):
        `[Source: code → infra/{{ENV}}/atlas/]`
        Extract: cluster tier (M-class), auto-scaling bounds, backup config.
        
        **Terraform — AKS node pools**:
        `[Source: code → infra/{{ENV}}/app/infra/env/]`
        Extract: node SKU, min/max nodes per pool, spot vs on-demand.
        
        ---
        
        ### Step 2 — Load Business Context (if available)
        
        `[Source: context → reports/{{SERVICE}}/code-context.md]`
        
        If the file below exists, read it and use it to inform your analysis.
        It describes the service's API patterns, DB access, Kafka usage, and resilience config.
        
        If the file is absent, proceed without it and note "Code context: not available" in the report.
        
        ---
        
        ### Step 3 — Query Azure MCP for Resource Configuration and Metrics
        
        **Condition**: Execute this step if {{MYSQL_HOST}} is not N/A, or if any other Azure-hosted
        resource (AKS node pool, MySQL Flexible Server, etc.) is part of this service's infrastructure.
        Skip entirely only if the service uses no Azure-managed resources.
        
        Use the Azure MCP tools to retrieve live configuration and current metrics for Azure resources.
        These values override Terraform-derived values where they conflict (live state takes precedence).
        Annotate every extracted value with `[Source: Azure MCP → <tool>(<args>)]`.
        
        **MySQL Flexible Server config** (skip if {{MYSQL_HOST}} is N/A):
        
        - **Server properties** (SKU, version, HA mode, storage, compute tier):
          `[Source: Azure MCP → get_mysql_flexible_server(name={{MYSQL_HOST}})]`
        - **Server parameters** (max_connections, slow_query_log, long_query_time):
          `[Source: Azure MCP → list_mysql_flexible_server_configurations(server_name={{MYSQL_HOST}})]`
        - **Read replicas**:
          `[Source: Azure MCP → list_mysql_flexible_server_replicas(server_name={{MYSQL_HOST}})]`
        - **Storage auto-grow and IOPS scaling settings**:
          (included in server properties above)
        
        **MySQL Flexible Server metrics from Azure Monitor** (skip if {{MYSQL_HOST}} is N/A):
        
        - **Current storage used (GB)**:
          `[Source: Azure MCP → get_metric(resource={{MYSQL_HOST}}, metric=storage_used, aggregation=Average)]`
          > If storage auto-grow is ON (confirmed from server properties above), collect **only** this
          > value — do not collect `storage_limit` and do not compute a utilisation percentage.
          > When auto-grow is ON the provisioned limit expands automatically, making the ratio meaningless.
          > Report the raw GB used and score against the absolute thresholds in `ai-scalability-review-service/src/main/java/app/aiscalabilityreview/prompt/MetricScorePrompt.java`.
        - **Storage limit (GB)** — collect **only** if auto-grow is OFF:
          `[Source: Azure MCP → get_metric(resource={{MYSQL_HOST}}, metric=storage_limit, aggregation=Average)]`
          > When auto-grow is OFF, compute utilisation as `storage_used / storage_limit × 100 %`
          > and apply the percentage thresholds in `ai-scalability-review-service/src/main/java/app/aiscalabilityreview/prompt/MetricScorePrompt.java`.
        - **CPU percent**:
          `[Source: Azure MCP → get_metric(resource={{MYSQL_HOST}}, metric=cpu_percent, aggregation=Average)]`
        - **Active connections (current)**:
          `[Source: Azure MCP → get_metric(resource={{MYSQL_HOST}}, metric=active_connections, aggregation=Average)]`
        - **Network in/out (bytes)**:
          `[Source: Azure MCP → get_metric(resource={{MYSQL_HOST}}, metric=network_bytes_ingress, aggregation=Total)]`
          `[Source: Azure MCP → get_metric(resource={{MYSQL_HOST}}, metric=network_bytes_egress, aggregation=Total)]`
        
        **AKS node pool config** (for the cluster hosting namespace {{NAMESPACE}}):
        
        - **Node pool SKUs, min/max node counts, spot vs on-demand**:
          `[Source: Azure MCP → get_aks_node_pools(cluster=<cluster name>, resource_group=<rg>)]`
          Cross-check values against Terraform (Step 1). Flag any drift.
        
        If an Azure MCP tool call fails, mark the value as `NOT_COLLECTED: Azure MCP error — <reason>`.
        
        ---
        
        ### Step 4 — Collect Datadog Metrics
        
        Use the Datadog MCP `query_metrics` tool to query the following. Use `env:{{ENV}}` and
        `kube_namespace:{{NAMESPACE}}` as base tag filters throughout. For each metric,
        call the `query_metrics` tool with the specified query, a `from` of `now - 28d`, and a `to` of `now`.
        Annotate every metric value with `[Source: Datadog MCP → query_metrics("<query>")]`.
        
        **Traffic & Throughput**:
        - **Request rate (RPS), 28-day time-series and weekly peak values**:
          `[Source: Datadog MCP → query_metrics("sum:trace.undertow_http.request.hits{env:{{ENV}},service:{{SERVICE}}}.as_count()")]`
        - **If {{KAFKA_CONSUMER_GROUPS}} is not N/A: consumer lag per group and topic**:
          `[Source: Datadog MCP → query_metrics("max:kafka.consumer_lag{env:{{ENV}},consumer_group:{{KAFKA_CONSUMER_GROUPS}}} by {topic}")]`
        
        **Latency**:
        - **P50, P95, P99 request duration for `service:{{SERVICE}}` over 28 days**:
          `[Source: Datadog MCP → query_metrics("p50:trace.undertow_http.request{env:{{ENV}},service:{{SERVICE}}}")]`
          `[Source: Datadog MCP → query_metrics("p95:trace.undertow_http.request{env:{{ENV}},service:{{SERVICE}}}")]`
          `[Source: Datadog MCP → query_metrics("p99:trace.undertow_http.request{env:{{ENV}},service:{{SERVICE}}}")]`
        
        **Error Rates**:
        - **HTTP error rate (error count/total count), 28-day trend**:
          `[Source: Datadog MCP → query_metrics("sum:trace.undertow_http.request.errors{env:{{ENV}},service:{{SERVICE}}}.as_count() / sum:trace.undertow_http.request.hits{env:{{ENV}},service:{{SERVICE}}}.as_count()")]`
        - **Container restart count (last 7 days); flag any OOMKill events**:
          `[Source: Datadog MCP → query_metrics("sum:kubernetes.containers.restarts{env:{{ENV}},service:{{SERVICE}}}")]`
        
        **Resource Saturation**:
        - **Pod CPU utilization: avg and peak (kube_deployment:{{SERVICE}})**:
          `[Source: Datadog MCP → query_metrics("avg:kubernetes.cpu.usage.total{env:{{ENV}},service:{{SERVICE}}}")]`
          `[Source: Datadog MCP → query_metrics("max:kubernetes.cpu.usage.total{env:{{ENV}},service:{{SERVICE}}}")]`
        - **Pod memory utilization: avg and peak**:
          `[Source: Datadog MCP → query_metrics("avg:kubernetes.memory.working_set{env:{{ENV}},service:{{SERVICE}}}")]`
          `[Source: Datadog MCP → query_metrics("max:kubernetes.memory.working_set{env:{{ENV}},service:{{SERVICE}}}")]`
        - **Replica count: avg and max (last 28 days)**:
          `[Source: Datadog MCP → query_metrics("avg:kubernetes_state.deployment.replicas{env:{{ENV}},kube_deployment:{{SERVICE}}}")]`
          `[Source: Datadog MCP → query_metrics("max:kubernetes_state.deployment.replicas{env:{{ENV}},kube_deployment:{{SERVICE}}}")]`
        
        **MySQL** (skip if {{MYSQL_HOST}} is N/A):
        - **Connection count (avg and peak)**:
          `[Source: Datadog MCP → query_metrics("avg:azure.dbformysql_flexibleservers.active_connections{name:{{MYSQL_HOST}}}")]`
          `[Source: Datadog MCP → query_metrics("max:azure.dbformysql_flexibleservers.active_connections{name:{{MYSQL_HOST}}}")]`
        - **Slow query rate**:
          `[Source: Datadog MCP → query_metrics("sum:azure.dbformysql_flexibleservers.slow_queries{name:{{MYSQL_HOST}}}.as_count()")]`
        - **Replication lag (if replica exists)**:
          `[Source: Datadog MCP → query_metrics("max:azure.dbformysql_flexibleservers.replication_lag{name:{{MYSQL_HOST}}-*}")]`
        
        **Atlas MongoDB** (skip if {{ATLAS_CLUSTER}} is N/A):
        - **Operation latency (reads and writes)**:
          `[Source: Datadog MCP → query_metrics("avg:mongodb.atlas.oplatencies.reads.avg{clustername:{{ATLAS_CLUSTER}}}")]`
          `[Source: Datadog MCP → query_metrics("avg:mongodb.atlas.oplatencies.writes.avg{clustername:{{ATLAS_CLUSTER}}}")]`
        - **Connection pool utilization**:
          `[Source: Datadog MCP → query_metrics("max:mongodb.atlas.connections.current{clustername:{{ATLAS_CLUSTER}}}")]`
        - **Query targeting ratio (scanned / returned)**:
          `[Source: Datadog MCP → query_metrics("sum:mongodb.atlas.metrics.queryexecutor.scannedperreturned{clustername:{{ATLAS_CLUSTER}}}")]`
        
        **SLO & Monitors**:
        - Use the `list_slos` and `list_monitors` tools for `service:{{SERVICE}}` to get SLO compliance and active alerts.
          `[Source: Datadog MCP → list_monitors(service:{{SERVICE}})]`
        
        If a metric is unavailable or the tool call fails, mark it as `NOT_COLLECTED: <reason>`,
        please don't skip this part.
        
        Do **not** invent or estimate metric values.
        
        ---
        
        ### Step 5 — MySQL Table Size Analysis via MySQL MCP
        
        **Condition**: Execute this step **only** if both {{MYSQL_HOST}} is not N/A **and** {{MYSQL_DB}} is not N/A.
        Skip entirely otherwise.
        
        Use the local Gemini MCP named **`{{MYSQL_HOST}}-{{MYSQL_DB}}`** to connect to the database
        and retrieve table-level storage statistics. Annotate every value with the exact SQL used.
        
        **Top 20 largest tables by data + index storage**:
        
        ```sql
        -- [Source: MySQL MCP ({{MYSQL_HOST}}-{{MYSQL_DB}}) →
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
        -- ]
        ```
        
        For each of the top 20 tables, record: `table_name`, `table_rows`, `data_mb`, `index_mb`, `total_mb`.
        
        Flag any tables with `total_mb` exceeding 10 GB as notable storage consumers.
        
        If the MySQL MCP tool call fails, mark as `NOT_COLLECTED: MySQL MCP error — <reason>`.
        
        ---
        
        ### Step 6 — Cross-Reference Config with Metrics
        
        When performing cross-references, cite the exact source for each config value and metric value:
        
        - Does HPA `maxReplicas` leave headroom at projected +1Q traffic? Given {{HPA_TYPE}},
          what trigger metric (CPU / custom DD metric) controls scale-out, and is the threshold
          appropriate for the observed workload?
          Config source: `[Source: code → kube/resource/]`; metric source: Datadog MCP (Step 4).
          **Important — HPA CPU averageUtilization is relative to CPU request, not CPU limit.**
          Before scoring, compute the effective trigger as a percentage of the CPU limit:
          `effective_trigger_pct_of_limit = averageUtilization × (cpu_request / cpu_limit)`
          Apply the HPA threshold from `ai-scalability-review-service/src/main/java/app/aiscalabilityreview/prompt/MetricScorePrompt.java` to this computed value.
          Example: `averageUtilization: 100`, request = `1`, limit = `2` → `100 × 0.5 = 50 %` of limit → GREEN.
          Record both the raw `averageUtilization` and the computed effective % of limit in the report.
        - Are CPU/memory `limits` within 20% of observed peak usage (risk of throttling / OOM)?
          Config source: `[Source: code → kube/resource/]`; metric source: Datadog MCP (Step 4).
        - If MySQL connection count is approaching `max_connections` from the Terraform config
          **and** confirmed by Azure MCP live config (Step 3), flag as RED.
          Config source: `[Source: code → infra/mysql/]` cross-checked with `[Source: Azure MCP]`;
          metric source: Datadog MCP (Step 4).
        - If Atlas MongoDB query targeting ratio > 100 (scans >> docs returned), flag as YELLOW.
          Metric source: Datadog MCP (Step 4).
        - Do active ALERT monitors correlate with the metric findings?
          Source: Datadog MCP `list_monitors` (Step 4).
        - Does the business context (Step 2) reveal batch jobs or scheduled tasks that would
          create predictable traffic spikes not visible in the 28-day average?
          Source: `[Source: context → reports/{{SERVICE}}/code-context.md]`.
        - If MySQL table size analysis was performed (Step 5), does the largest table's growth
          trajectory put storage auto-grow or IOPS scaling at risk within +1Q?
        
        ---
        
        ### Step 7 — Score Each Review Dimension
        
        Use the metric thresholds defined in ai-scalability-review-service/src/main/java/app/aiscalabilityreview/prompt/MetricScorePrompt.java.
        
        Apply the GREEN / YELLOW / RED thresholds from that file to every collected metric.
        The worst-matching threshold across all metrics in a dimension determines the dimension
        score. The five fixed dimensions are:
        1. Traffic & Throughput
        2. Latency & Thread Concurrency
        3. Error Rates & Stability
        4. Resource Saturation
        5. Persistence Layer
        
        When writing each dimension's **Rationale**:
        - Cite only exact metric values that were collected in Steps 1–5 and already annotated
          in the Performance Metrics section. Do **not** round, approximate, or paraphrase values.
        - Every metric value mentioned in the Rationale must carry a `[Source: ...]` annotation
          using the identical query or file path from the metric's bullet in Section 4.
        - Do **not** use "around", "about", "under", "over", or any approximation qualifier.
          Write the precise figure (e.g., `` `1.2%` `` not "under 1.5%"; `` `476 MB` `` not "around 500 MB").
        
        ---
        
        ### Step 8 — Generate Capacity Projections
        
        Project +1Q, +2Q, +4Q using the 28-day growth rate and the model in Section 10 of the
        review design doc.
        
        ---
        
        ### Step 9 — Output
        
        Produce a full scalability report following the exact format defined in ai-scalability-review-service/src/main/java/app/aiscalabilityreview/prompt/ReportFormatPrompt.java.
        
        Complete every section and field in the order specified. Use the drift-prevention
        checklist at the end of that file before finalising the report.
        
        Every metric or config value in the report **must** include its data source label
        (`[Source: ...]`) inline or in a parenthetical note. This is mandatory for traceability.
        
        ---
        
        ## Review Checklist Reference
        
        See ai-scalability-review-service/src/main/resources/doc/scalability-review-checkpoint.md
            """;
    }
}
