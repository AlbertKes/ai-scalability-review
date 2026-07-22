package app.aiscalabilityreview.prompt;

public class CodeAnalysisTaskPrompt {
    /**
     * Prompt for code analysis stage (code-analysis-task.md).
     * Placeholder: {{SERVICE}}
     */
    public static final String CONTENT;

    static {
        CONTENT = """
        # Service Code Analysis — Business Context Extraction
        
        You are an application engineer. Analyze the source code of **{{SERVICE}}** and produce a structured business context \
        summary for use in a scalability review. Do not use abbreviated forms, be factual and detailed. Do not speculate — only \
        report what is directly observable in the code.
        
        ## What to Extract
        
        ### 1. API Surface
        - List all REST endpoints (method + path) or message consumers this service exposes.
        - Identify which endpoints are on the critical path (synchronous, user-facing).
        - Note any batch or scheduled jobs and their approximate frequency.
        
        ### 2. Database Access Patterns
        - **MySQL** (if used): list tables accessed, query types (SELECT/INSERT/UPDATE/DELETE),
          any large scans or JOINs without indexed filters, connection pool config.
        - **MongoDB / Atlas** (if used): list collections accessed, query patterns (indexed vs
          full-scan), aggregation pipelines, update patterns (full doc vs `$set`).
        - Flag any N+1 query patterns or unbounded queries.
        
        ### 3. Kafka Usage
        - List Kafka topics produced to and consumed from.
        - Note consumer group IDs and concurrency / partition count if configured.
        - Identify any blocking `poll()` loops.
        
        ### 4. Caching Strategy
        - List Redis keys or cache regions, TTLs, and eviction policy if configured.
        - Note whether the service can degrade gracefully on cache miss.
        
        ### 5. Resilience Configuration
        - List circuit-breaker instances, retry policies, bulkhead limits, timeout values.
        - Note any missing circuit-breakers on critical external calls.
        
        ### 6. External Dependencies
        - List all outbound HTTP calls (client beans, RestTemplate).
        - Note configured timeouts (connect + read) and any missing timeout settings.
        
        ### 7. Scalability Signals
        - Any known singletons or in-process state that would break with multiple replicas.
        - Scheduled tasks: are they guarded against concurrent execution across replicas(e.g. database lock)?
        - Any stateful operations (file system writes, local caches) that are not replica-safe.
        
        ## Output Format
        
        Produce a Markdown document with one section per topic above.
        Where a topic is not applicable (e.g. the service does not support multiple replicas), write "N/A".
        End with a **Key Scalability Signals** section listing the top 3–5 concerns found. If no concerns, \
        write "N/A".
            """;
    }
}
