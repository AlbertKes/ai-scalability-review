package app.aiscalabilityreview.prompt;

public class CodeAnalysisTaskPrompt {
    /**
     * Prompt for code analysis stage.
     * Placeholder: {{SERVICE}}
     */
    public static final String CONTENT;

    static {
        CONTENT = """
        # Service Code Analysis — Business Context Extraction
        
        You are an application engineer. Analyze the source code of **{{SERVICE}}** and produce a structured business context \
        summary for use in a scalability review. Do not use abbreviated forms, be factual and detailed. Do not speculate — only \
        report what is directly observable in the code.
        
        ## First: identify the framework

        State the framework in one line before anything else. Wonder services are normally
        **core-ng** (`core.framework.*`, `Module`, `bind(...)`, `HTTPClient`, `WebService`,
        `MessagePublisher`, `app.properties`), not Spring. Describe what the code uses in that
        framework's own vocabulary — do not translate it into Spring terms (`@Transactional`,
        `RestTemplate`, `application.yml`, Resilience4j) that the code does not contain.

        ## How to work — read this first, it decides how long you take

        Every tool call re-sends everything read so far, so cost grows with the number of calls. The
        first run of this stage made 116 of them. Make at most **about 25**:

        1. List the module directories once (`list_directory` on the source roots below), then read
           the **entry points first**: the `Module`/`App` classes and `app.properties`. They name every
           web service, message handler, scheduled job, database and HTTP client the service has.
        2. Read files in **batches** with `read_many_files` (a glob or a list of paths), never one file
           per call. Typical batches: the module/config classes; the web service implementations; the
           Kafka handlers and jobs; the services that touch the database; the HTTP client wrappers.
        3. Do not open a file whose name already tells you what it is (DTOs, enums, mappers, tests,
           generated code, migrations). Do not search for something a module class already told you.
        4. Do not re-read a file. Do not read to "double check".

        ## What to Extract
        
        ### 1. API Surface
        - Group the REST endpoints by web service and give the count and the path prefix of each group
          (not one line per endpoint); list message consumers by topic.
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
        - List retry policies, bulkhead / executor limits and timeout values. Do **not** report
          missing circuit breakers: core-ng has none and that is out of scope for this review.
        - For each value that comes from a property, give the property key, the default in code or
          `app.properties`, **and the file it was read from**. Say explicitly that it is the default:
          the deployed value can be overridden by an environment variable (the key upper-cased, dots
          replaced by underscores) set in the K8s manifests, which you cannot see from here.
        
        ### 6. External Dependencies
        - List all outbound HTTP calls (core-ng `HTTPClient` beans, or the client the code actually uses).
        - Note configured timeouts (connect + read) and any missing timeout settings.
        
        ### 7. Scalability Signals
        - Any known singletons or in-process state that would break with multiple replicas.
        - Scheduled tasks: are they guarded against concurrent execution across replicas(e.g. database lock)?
        - Any stateful operations (file system writes, local caches) that are not replica-safe.
        
        ## Paths

        Quote file paths exactly as they are in the repository and only for files you actually opened.
        Later stages cannot see the source and will reuse the paths you write here as evidence.

        ## Output Format

        Keep the document **under about 12 KB** — it is read by two later stages that pay for every
        byte, and a longer one is not a better one. Prefer a table or a short bullet to a paragraph,
        and omit what is unremarkable instead of describing it.

        Produce a Markdown document with one section per topic above.
        Where a topic is not applicable (e.g. the service does not support multiple replicas), write "N/A".
        End with a **Key Scalability Signals** section listing the top 3–5 concerns found. If no concerns, \
        write "N/A".
            """;
    }
}
