1. Service Dependency Review 
   1. Build a complete dependency map across internal microservices. 
   2. Validate each dependency's capacity, SLA, timeout, retry, and error handling strategy. 
   3. Assess cross-service cascading failure risks.
2. Current & Future Request Volume Assessment 
   1. Estimate current request volumes, traffic patterns, and peak-hour loads,  including correlation with business entities such as orders, HDR, customer, and item. 
   2. Forecast future load based on product roadmap, seasonal spikes, and business projections. 
   3. Identify request rate growth trends over time. 
   4. Determine Kafka message throughput growth. 
   5. Document expected QPS/RPS ranges and quarterly/semi-annual/annual growth assumptions.
3. Data Growth & Storage Estimation 
   1. Perform large-table growth estimation: record count growth, storage size. 
   2. Estimate storage expansion needs for MySQL, MongoDB, Redis, and Elasticsearch. 
   3. Review data retention strategies and archiving policy. 
   4. Validate that schemas and indexes support projected data growth.
4. Service Capacity Review 
   1. Review horizontal and vertical scaling capabilities. 
   2. Validate container CPU, memory, disk, and network configurations. 
   3. Confirm auto-scaling strategies are in place for needed stateless services. 
   4. Measure and optimize service response time, throughput, and concurrency handling. 
   5. Ensure readiness for load bursts.
5. Middleware Architecture Review 
   1. MySQL
      1. Check server config and metrics, and review related alerts. 
      2. Review slow queries, indexing coverage, and hot partitions. 
      3. Evaluate read/write splitting, connection pool sizing, and replication lag. 
   2. MongoDB 
      1. Review document size growth, and index usage. 
      2. Evaluate replica-set health and query hotspots. 
   3. Redis (DB / Cache / Session)
      1. Review key expiration strategies, memory sizing, and eviction policies. 
      2. Validate latency, resource usage, and persistence (AOF/RDB) needs. 
   4. Elasticsearch 
      1. Review index settings, and query performance. 
      2. Validate cluster capacity.
6. Critical Path Review
   1. Identify critical synchronous paths for user-facing requests. 
   2. Validate latency SLAs per step (service → DB → cache → 3rd-party → messaging). 
   3. Map dependency graph and identify single points of failure. 
   4. Evaluate fallback, retry, timeout, and circuit breaker strategies.
7. Third-Party Dependency Review 
   1. Evaluate third-party API rate limits. 
   2. Review risks of HTTP 429 and vendor throttling. 
   3. Assess resilience mechanisms: retries, exponential backoff, caching, and queue buffering. 
   4. Validate monitoring and alerting around vendor-side failures.
8. Scaling Strategy (Manual vs Automatic)
   1. Decide per system whether scaling is:
      1. Manual (scheduled capacity increases)
      2. Automatic (metrics-driven auto-scaling)
      3. Not required due to low/flat workload or 3rd-party depence 
   2. Validate auto-scaling triggers (CPU, latency, custom metrics). 
   3. Ensure safe scaling boundaries and cooldown periods.
9. Load & Stress Testing 
   1. Identify systems requiring load tests. 
   2. Define test scenarios: stree test, spike testing, sustained load, failover testing. 
   3. Validate system behavior under degraded dependencies. 
   4. Document throughput limits, error thresholds, and bottlenecks.
10. SLO Review
    1. Validate Service Level Objectives (availability, latency, error rate). 
    2. Compare current performance against SLO targets. 
    3. Identify components regularly violating performance budgets. 
    4. Align SLOs with business expectations.
11. Observability & Monitoring
    1. Dashboards
       1. Ensure dashboards exist in Kibana, Grafana or Datadog. 
       2. Include metrics for throughput, latency, error rate, saturation, and resource usage. 
    2. Alerts 
       1. Ensure alerts exist for critical KPIs:
          1. High error rates 
          2. Pod failure 
          3. DB performance 
          4. Resource saturation(like disk)
          5. Latency SLA violations 
       2. Validate on-call escalation paths.
12. Review Frequency
    1. Run full scalability reviews quarterly or semi-annual or before major launches.
    2. Run targeted reviews after major incidents, large migrations, or traffic surges. 
13. Collect Action Items 
    1. Document all issues found during the review. 
    2. Prioritize items by impact: High / Medium / Low. 
    3. Record owners, deadlines, and expected outcomes. 
    4. Track follow-up progress in project management tools.