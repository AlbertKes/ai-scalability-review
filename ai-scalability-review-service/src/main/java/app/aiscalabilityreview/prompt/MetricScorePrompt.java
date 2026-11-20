package app.aiscalabilityreview.prompt;

public class MetricScorePrompt {
    /**
     * Metric scoring reference — embedded verbatim for reference.
     */
    public static final String CONTENT;

    static {
        CONTENT = """
        # Metric Scoring Reference
        
        Use this reference when scoring the review dimensions. For every collectible metric listed
        below, apply the corresponding GREEN / YELLOW / RED threshold. Assign the worst-matching
        threshold across all metrics within a dimension to determine that dimension's overall score.
        
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
        
        **Overall score** — over the dimension scores: three or more RED → RED; exactly two RED, or more
        than two YELLOW → YELLOW; otherwise GREEN. One RED dimension alone does not make the overall RED.
        
        ---
        
        ## Dimension 1 — Traffic & Throughput
        
        | Metric | GREEN | YELLOW | RED |
        | :--- | :--- | :--- | :--- |
        | **Average RPS week-over-week growth** | < 5 % | 5 % – 15 % | > 15 % |
        | **Peak RPS / Average RPS ratio** (spikiness) | < 3× | 3× – 6× | > 6× |
        | **Kafka consumer lag — 28-day average** (messages) | ≤ 100 | 100 – 1,000 | > 1,000 |
        | **Kafka consumer lag — peak observed** (messages) | ≤ 1,000 | 1,000 – 10,000 | > 10,000 |
        | **Metric availability** | All collected | 1–2 missing | > 2 missing or gaps > 3 days |
        
        ---
        
        ## Dimension 2 — Latency & Thread Concurrency
        
        | Metric | GREEN | YELLOW | RED |
        | :--- | :--- | :--- | :--- |
        | **P50 latency — 28-day average** | < 20 ms | 20 – 50 ms | > 50 ms |
        | **P95 latency — 28-day average** | < 200 ms | 200 – 500 ms | > 500 ms |
        | **P99 latency — 28-day average** | < 500 ms | 500 ms – 1.5 s | > 1.5 s |
        | **P99 latency — peak observed** | < 1 s | 1 – 3 s | > 3 s |
        
        > **Not scored here:** circuit breakers (core-ng has none) and outbound HTTP client timeouts
        > (not assessed for now). Latency is scored from the four latency rows alone.
        >
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
        | **SLO status — worst of the service's SLOs** (Datadog `status`) | All `compliant`, error budget ≥ 50 % | `compliant` with budget < 50 %, or `at-risk` | Any `breached` |
        
        > **Note on SLOs**: a service with no SLO tagged `service:<name>` omits the SLO row from
        > scoring and says so in the findings — an undefined objective is a process gap, not a
        > measured breach.
        
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

        > **Do not judge scaling headroom.** Deployments of a service set their own `maxReplicas`, and a
        > `maxReplicas` above the observed average replica count is normal. Report min/max replicas as
        > configuration; `maxReplicas` versus the average replica count never sets a score.
        
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
        | **HPA configured for a stateless service with variable load** | Yes | Planned / partial | No HPA, no documented justification |
        
        ---
        
        ## Dimension 5 — Persistence Layer
        
        ### MySQL
        
        | Metric | GREEN | YELLOW | RED |
        | :--- | :--- | :--- | :--- |
        | **Active connections — avg as % of `max_connections`** | < 50 % | 50 % – 75 % | > 75 % |
        | **Active connections — peak as % of `max_connections`** | < 65 % | 65 % – 85 % | > 85 % |
        | **Slow queries — 28-day daily average** (shared server, at most YELLOW) | < 10 / day | ≥ 10 / day | — |
        | **Slow queries — single-day spike** (shared server, at most YELLOW) | < 50 | ≥ 50 | — |
        | **Missing indexes on high-frequency query columns of tables > 1 GB** | None found | 1 missing | ≥ 2 missing |
        | **Largest single table size** (MySQL) | < 5 GB | 5 – 10 GB | > 10 GB |
        | **Number of tables > 10 GB** (MySQL) | 0 | 1 | ≥ 2 |
        
        > **Slow queries are counted for the whole MySQL server**, which many services share, so they can
        > never make this service RED: the slow-query rows score GREEN or YELLOW only, whatever the count.
        > Name the shared server in the rationale.
        >
        > **A missing index only scores on a table over 1 GB.** A table that is small in the MySQL table
        > storage figures is scanned in milliseconds whatever the filter; mention a missing index there as
        > a note under the rationale and leave the row GREEN. A table whose size is not known does not score.
        >
        > **Shared MySQL server — reminders, not scored.** Replication lag and the server's total
        > storage used (`azure.dbformysql_flexibleservers.*`) are per **server**, and one server and its
        > replicas serve many services. They never set this service's score: a lagging shared replica
        > would otherwise turn every service on that database RED. Report them in the "Shared-resource
        > reminders" list of the Persistence section with the thresholds below, as information for the
        > team that owns the server.
        >
        > | Reminder | Raise it when |
        > | :--- | :--- |
        > | Replication lag — 28-day average | > 5 s |
        > | Replication lag — peak observed | > 60 s |
        > | Server storage used (GB) | > 500 GB (or > 75 % of provisioned when auto-grow is OFF) |
        >
        > **Note on MySQL table size metrics**: the pipeline queries `information_schema` over JDBC and
        > only when both `MYSQL_HOST` and `MYSQL_DB` are set. If the query did not run, omit these rows
        > from scoring — do **not** default them to RED for missing data; only mark RED if the query ran
        > and the threshold was exceeded. If it was skipped (not configured), note
        > `NOT_COLLECTED: MySQL not configured` and exclude from dimension scoring.
        
        ### MongoDB Atlas
        
        | Metric | GREEN | YELLOW | RED |
        | :--- | :--- | :--- | :--- |
        | **Read op latency — 28-day average** | < 1 ms | 1 – 5 ms | > 5 ms |
        | **Write op latency — 28-day average** | < 2 ms | 2 – 10 ms | > 10 ms |
        | **Read op latency — peak observed** | < 5 ms | 5 – 20 ms | > 20 ms |
        | **Connection pool — peak as % of cluster connection limit** | < 40 % | 40 % – 70 % | > 70 % |
        | **Query targeting ratio (scanned / returned) — 28-day average** | < 10 | 10 – 100 | > 100 |
        | **Query targeting ratio — peak observed** | < 20 | 20 – 100 | > 100 |
        | **Database storage size** (Datadog `dbstats.storage.size`, this service's database) | < 50 GB | ≥ 50 GB (**warning only — never RED**) | — |
        | **Database storage — 28-day growth** | < 10 % | 10 % – 50 % | > 50 % |
        | **Index size as a share of the database's data size** | < 50 % | 50 % – 100 % | > 100 % |
        
        > **Note on MongoDB storage**: these rows apply to the database named in the request
        > (`databasename` tag), not the cluster. When no database was given, or its size was not
        > collected, omit them — do not score them and do not call them a gap. A database of 50 GB or
        > more is a YELLOW warning that it should be reviewed (archiving, TTL, sharding); it is not a
        > RED condition. Cluster disk used (% of disk, fullest member) is a shared-resource reminder,
        > raised when it exceeds 80 %; it does not set this service's score.
        
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
        | Latency & Thread Concurrency | P99 average & peak | P99 avg > 500 ms |
        | Error Rates & Stability | HTTP error rate, restarts | Any restart = YELLOW; error rate > 1 % = RED |
        | Resource Saturation | Peak CPU/mem % of limit | Peak CPU > 90 % limit **or** HPA effective trigger (`averageUtilization × request/limit`) ≥ 86 % of limit |
        | Persistence Layer | Connections %, slow queries, largest table (MySQL), database storage (MongoDB, ≥ 50 GB = YELLOW) | Connections > 85 % limit **or** any table > 10 GB (shared-server lag / storage and cluster disk are reminders, not triggers) |
            """;
    }
}
