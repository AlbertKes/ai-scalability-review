package app.aiscalabilityreview.prompt;

public class MetricScorePrompt {
    /**
     * Metric scoring reference (metric-score.md) — embedded verbatim for reference.
     */
    public static final String CONTENT = """
        # Metric Scoring Reference
        
        Use this file during **Step 5 — Score Each Review Dimension** in `review-task.md`.
        For every collectible metric listed below, apply the corresponding GREEN / YELLOW / RED
        threshold. Assign the worst-matching threshold across all metrics within a dimension to
        determine that dimension's overall score.
        
        ---
        
        ## Scoring Principles
        
        - **GREEN** — healthy, within targets with comfortable headroom.
        - **YELLOW** — approaching a threshold or a configuration gap exists that poses risk
          under a 2× traffic surge.
        - **RED** — a critical threshold is exceeded, an SLO is at risk, or data required for a
          Tier-1 service is missing / unavailable.
        
        A dimension with **any RED metric** is scored RED.
        A dimension with **no RED but at least one YELLOW metric** is scored YELLOW.
        A dimension is GREEN only if all its metrics are GREEN.
        
        ---
        
        ## Dimension 1 — Traffic & Throughput
        
        | Metric | GREEN | YELLOW | RED |
        | :--- | :--- | :--- | :--- |
        | **Average RPS week-over-week growth** | < 5 % | 5 % – 15 % | > 15 % |
        | **Peak RPS / Average RPS ratio** (spikiness) | < 3× | 3× – 6× | > 6× |
        | **Kafka consumer lag — 28-day average** (messages) | ≤ 1 | 1 – 10 | > 10 |
        | **Kafka consumer lag — peak observed** (messages) | ≤ 50 | 50 – 200 | > 200 |
        | **Metric availability** | All collected | 1–2 missing | > 2 missing or gaps > 3 days |
        
        ---
        
        ## Dimension 2 — Latency & Thread Concurrency
        
        | Metric | GREEN | YELLOW | RED |
        | :--- | :--- | :--- | :--- |
        | **P50 latency — 28-day average** | < 20 ms | 20 – 50 ms | > 50 ms |
        | **P95 latency — 28-day average** | < 200 ms | 200 – 500 ms | > 500 ms |
        | **P99 latency — 28-day average** | < 500 ms | 500 ms – 1.5 s | > 1.5 s |
        | **P99 latency — peak observed** | < 1 s | 1 – 3 s | > 3 s |
        | **Outbound HTTP client timeout configured** | All clients explicit ≤ 5 s | Some clients explicit | Any client uses framework default > 10 s |
        | **Circuit breakers on critical paths** | All paths covered | Partial coverage | No circuit breakers on any critical path |
        
        > **Note on thread concurrency**: if code context reveals `.get()` inside a thread-pool
        > loop (sequential blocking) or equivalent patterns that serialise async work, treat this
        > as a YELLOW structural risk even when current latency metrics are GREEN.
        
        ---
        
        ## Dimension 3 — Error Rates & Stability
        
        | Metric | GREEN | YELLOW | RED |
        | :--- | :--- | :--- | :--- |
        | **HTTP error rate — 28-day average** | < 0.1 % | 0.1 % – 1 % | > 1 % |
        | **HTTP error rate — peak observed** | < 1 % | 1 % – 5 % | > 5 % |
        | **Container restarts — last 28 days** | 0 | 1 – 3 | > 3 |
        | **OOMKill events — last 28 days** | 0 | 1 | ≥ 2 |
        
        ---
        
        ## Dimension 4 — Resource Saturation
        
        ### CPU
        
        | Metric | GREEN | YELLOW | RED |
        | :--- | :--- | :--- | :--- |
        | **Avg CPU usage as % of CPU *limit*** | < 30 % | 30 % – 60 % | > 60 % |
        | **Peak CPU usage as % of CPU *limit*** | < 70 % | 70 % – 90 % | > 90 % |
        | **Avg CPU usage as % of CPU *request*** | < 60 % | 60 % – 90 % | > 90 % |
        
        ### Memory
        
        | Metric | GREEN | YELLOW | RED |
        | :--- | :--- | :--- | :--- |
        | **Avg memory as % of memory *limit*** | < 50 % | 50 % – 75 % | > 75 % |
        | **Peak memory as % of memory *limit*** | < 70 % | 70 % – 90 % | > 90 % |
        
        ### HPA / Replica Scaling
        
        > **HPA CPU averageUtilization is relative to CPU request, not CPU limit.**
        > Kubernetes scales when `current_cpu_usage / cpu_request` hits the configured
        > `averageUtilization`. To make this comparable across services with different
        > request/limit ratios, always convert to an **effective % of CPU limit** before scoring:
        >
        > `effective_trigger_pct_of_limit = averageUtilization × (cpu_request / cpu_limit)`
        >
        > Example: `averageUtilization: 100`, `cpu_request = 1`, `cpu_limit = 2`
        > → `100 × (1/2) = 50 %` of limit → GREEN.
        >
        > Apply all thresholds below to this **computed effective value**, never to the raw
        > `averageUtilization` number directly.
        
        | Metric | GREEN | YELLOW | RED |
        | :--- | :--- | :--- | :--- |
        | **HPA CPU effective trigger (= `averageUtilization × request/limit`) as % of CPU limit** | ≤ 75 % | 76 % – 85 % | ≥ 86 % |
        | **HPA headroom: (maxReplicas − avgReplicas) / maxReplicas** | > 50 % | 25 % – 50 % | < 25 % |
        | **HPA configured for a stateless service with variable load** | Yes | Planned / partial | No HPA, no documented justification |
        
        ---
        
        ## Dimension 5 — Persistence Layer
        
        ### MySQL
        
        | Metric | GREEN | YELLOW | RED |
        | :--- | :--- | :--- | :--- |
        | **Active connections — avg as % of `max_connections`** | < 50 % | 50 % – 75 % | > 75 % |
        | **Active connections — peak as % of `max_connections`** | < 65 % | 65 % – 85 % | > 85 % |
        | **Slow queries — 28-day daily average** | < 10 / day | 10 – 50 / day | > 50 / day |
        | **Slow queries — single-day spike** | < 50 | 50 – 200 | > 200 |
        | **Replication lag — 28-day average** | < 1 s | 1 – 5 s | > 5 s |
        | **Replication lag — peak observed** | < 10 s | 10 – 60 s | > 60 s |
        | **Missing indexes on high-frequency query columns** | None found | 1 missing | ≥ 2 missing |
        | **DB storage used — absolute (GB)** when auto-grow is ON (Azure MCP) | < 100 GB | 100 – 500 GB | > 500 GB |
        | **DB storage used — % of provisioned** when auto-grow is OFF (Azure MCP) | < 50 % | 50 % – 75 % | > 75 % |
        | **Largest single table size** (MySQL MCP) | < 5 GB | 5 – 10 GB | > 10 GB |
        | **Number of tables > 10 GB** (MySQL MCP) | 0 | 1 | ≥ 2 |
        
        > **Note on MySQL table size metrics**: these are collected only when the MySQL MCP is available
        > (i.e., both `MYSQL_HOST` and `MYSQL_DB` are set). If the MySQL MCP was not run, omit these rows
        > from scoring — do **not** default them to RED for a missing MCP; only mark RED if the MCP ran
        > and the threshold was exceeded. If the MCP was skipped (not configured), note
        > `NOT_COLLECTED: MySQL MCP not configured` and exclude from dimension scoring.
        
        ### MongoDB Atlas
        
        | Metric | GREEN | YELLOW | RED |
        | :--- | :--- | :--- | :--- |
        | **Read op latency — 28-day average** | < 1 ms | 1 – 5 ms | > 5 ms |
        | **Write op latency — 28-day average** | < 2 ms | 2 – 10 ms | > 10 ms |
        | **Read op latency — peak observed** | < 5 ms | 5 – 20 ms | > 20 ms |
        | **Connection pool — peak as % of cluster connection limit** | < 40 % | 40 % – 70 % | > 70 % |
        | **Query targeting ratio (scanned / returned) — 28-day average** | < 10 | 10 – 100 | > 100 |
        | **Query targeting ratio — peak observed** | < 20 | 20 – 100 | > 100 |
        
        ### Redis (if used)
        
        | Metric | GREEN | YELLOW | RED |
        | :--- | :--- | :--- | :--- |
        | **Memory usage as % of `maxmemory`** | < 60 % | 60 % – 80 % | > 80 % |
        | **Eviction rate** | ≈ 0 / s | > 0, < 100 / s | > 100 / s |
        | **Keyspace miss rate** | < 5 % | 5 % – 20 % | > 20 % |
        
        ---
        
        ## Scoring Quick-Reference Matrix
        
        | Dimension | Primary Signal | Escalation Trigger |
        | :--- | :--- | :--- |
        | Traffic & Throughput | Avg RPS growth, Kafka lag | Any metric RED |
        | Latency & Thread Concurrency | P99 average & peak | P99 avg > 500 ms **or** missing circuit breakers on critical path |
        | Error Rates & Stability | HTTP error rate, restarts | Any restart = YELLOW; error rate > 1 % = RED |
        | Resource Saturation | Peak CPU/mem % of limit | Peak CPU > 90 % limit **or** HPA effective trigger (`averageUtilization × request/limit`) ≥ 86 % of limit |
        | Persistence Layer | Connections %, slow queries, replication lag, storage % (Azure MCP), largest table (MySQL MCP) | Connections > 85 % limit **or** replication lag peak > 60 s **or** storage > 75 % **or** any table > 10 GB |
        """;
}
