package app.aiscalabilityreview.prompt;

/**
 * Checks 3, 4, 5 and 7 — the validation checks that are answerable from the report alone.
 *
 * <p>Cross-source consistency, scoring, projection arithmetic and format are all decided by reading
 * {@code {{REPORT_FILE}}} against two reference documents. No data source is queried and no
 * repository is read, so this group starts no MCP server and is given no repository root: it should
 * reach for no tool but the one that writes its output. Keeping it out of the session that reads
 * source files is the point of the split — these checks used to re-send every file Check 8 had
 * opened on each of their own round-trips.
 */
public final class ValidationStaticChecksPrompt {
    public static final String CONTENT;

    /** This group reads one file and queries nothing, so it is handed the report and nothing else. */
    public static final String FILE_REFS;

    static {
        CONTENT = """
        ## Check 3 — Cross-Source Metric Consistency

        Verify that the same metric value is stated consistently wherever it appears in the report.
        No external queries are needed for this check — read {{REPORT_FILE}} only.

        **Consistency points to verify**:

        | Metric | Location A | Location B | Expected |
        |--------|-----------|-----------|----------|
        | Avg RPS | Executive Summary (critical findings) | 3.1 Traffic & Throughput → Average RPS | Values must match |
        | Peak RPS | Executive Summary | 3.1 Traffic & Throughput → Peak RPS | Values must match |
        | P99 latency (avg or peak) | 3.2 Latency Profile → P99 | Dimension 2 scoring rationale | Value cited in rationale must appear in metrics section |
        | HTTP error rate | 3.3 Error Rates & Stability | Dimension 3 scoring rationale | Value cited in rationale must appear in metrics section |
        | CPU avg % of limit | 3.4 Resource Saturation | Dimension 4 scoring rationale | Must match |
        | MySQL connection avg % | 3.5 Persistence Layer / DB Saturation → Connection Pool | Dimension 5 scoring rationale | Must match |
        | Capacity baseline RPS | 3.1 Traffic & Throughput → Average RPS | Capacity Projections → Current (Base) | Must match |
        | Capacity baseline requests/week | 3.1 Weekly Volumes (most recent week) | Capacity Projections → Current (Base) | Must be consistent |
        | Overall Score | Dimension scores in Section 4 | Capacity Projections → Current (Base) status | Must match the worst dimension |

        For each inconsistency, mark FAIL and quote both conflicting values with their exact locations
        (section name and bullet text).

        ---

        ## Check 4 — Scoring Accuracy

        For each of the 5 fixed scoring dimensions, verify that the stated score (GREEN / YELLOW / RED)
        is correct given the metric values cited in the rationale and the thresholds in the
        **Metric Scoring Reference** section below.

        **Procedure for each dimension**:
        1. Parse the scoring rationale — identify every specific metric value cited with its unit.
        2. For each cited metric value, find the matching row in the Metric Scoring Reference below.
        3. Determine what color (GREEN / YELLOW / RED) that value individually produces.
        4. Apply the aggregation rule from the Metric Scoring Reference:
           - Any RED metric → dimension is RED
           - No RED but ≥1 YELLOW metric → dimension is YELLOW
           - All GREEN → dimension is GREEN
        5. Compare the computed score to the reported score.

        **Score tolerance**:
        - Computed score matches reported score → PASS
        - Computed score differs by 1 level (e.g., report YELLOW, computed GREEN) → WARNING with explanation
        - Computed score differs by 2 levels (e.g., report GREEN, computed RED) → FAIL

        **Dimension-specific metrics to look for in the rationale**:

        - **Dimension 1 — Traffic & Throughput**: WoW RPS growth %, peak/avg ratio, Kafka lag avg/peak
        - **Dimension 2 — Latency & Thread Concurrency**: P50/P95/P99 avg, P99 peak, outbound timeout config
        - **Dimension 3 — Error Rates & Stability**: HTTP error rate avg and peak, container restarts, OOMKills
        - **Dimension 4 — Resource Saturation**: CPU avg/peak % of limit, memory avg/peak % of limit,
          HPA trigger threshold, HPA headroom
        - **Dimension 5 — Persistence Layer**: MySQL connection % avg/peak, slow query daily avg,
          replication lag avg/peak, storage, largest table; or MongoDB read/write latency,
          connection pool %, query targeting ratio

        If the rationale does not cite a concrete metric value for a dimension, flag the dimension
        as WARNING — "rationale lacks the specific metric values the report format requires".

        ---

        ## Check 5 — Capacity Projection Math

        Parse the Capacity Projections table from {{REPORT_FILE}}. Verify the mathematical correctness
        of every row and the consistency of the growth model.

        **Extract from the report**:
        - Growth model type (linear or compound growth stated in the introductory sentence)
        - Baseline avg RPS and baseline weekly hit volume
        - Stated weekly or quarterly growth rate
        - For each projection row: growth factor, projected weekly hits, projected avg RPS

        **Verifications**:

        1. **Baseline weekly hits**: Must equal `Baseline Avg RPS × 604,800` (seconds per week).
           Tolerance: ±1%.

        2. **Growth factor math**:
           - Compound model: `factor = (1 + weekly_rate)^(weeks)` where +1Q=13 weeks, +2Q=26, +4Q=52
           - Linear model: `factor = 1 + (rate × quarters)` where +1Q=1, +2Q=2, +4Q=4
           - Tolerance: ±2% on each factor.

        3. **Projected RPS**: `Projected RPS = Baseline RPS × Growth Factor`. Tolerance: ±1%.

        4. **Projected weekly hits**: `Projected hits = Projected RPS × 604,800`. Tolerance: ±1%.

        5. **Factor internal consistency**: +2Q factor should be approximately (+1Q factor)² for compound,
           or `1 + 2×(+1Q factor − 1)` for linear. Tolerance: ±3%.

        6. **Infrastructure Status color**: The status color in the projection table must be consistent
           with the actual scoring results. If the service currently scores YELLOW on any dimension,
           the "Current (Base)" row must not be labeled GREEN.

        For each row with incorrect math: FAIL with expected vs. reported values.
        For minor rounding inconsistencies (off by <1%): WARNING only.

        ---

        ## Check 7 — Format & Attribution Completeness

        Verify the report structure against the mandatory format in the **Report Format Reference**
        section below. No external queries needed — read {{REPORT_FILE}} only.

        ### 7a — Section presence and order

        Verify all 8 sections are present in the exact order:
        1. Header Block → 2. Executive Summary → 3. Infrastructure Configuration →
        4. Performance Metrics (Last 28 Days) → 5. Scalability Assessment & Scoring →
        6. Capacity Projections → 7. Recommendations & Action Items → 8. Footer

        Missing section → FAIL. Sections out of order → WARNING.

        ### 7b — Infrastructure table schema

        Verify the Infrastructure Configuration table contains the required rows:
        AKS Node Pool, AKS Scaling, K8s Pod Requests, K8s Pod Limits, K8s Memory Requests,
        K8s Memory Limits, HPA Settings, MySQL Host (or N/A), MySQL Version (or N/A), Storage (or N/A),
        Read Replica (or N/A), Atlas MongoDB (or N/A).

        Missing required rows → WARNING (unless the component is not used by this service).
        Extra rows → acceptable (no flag).

        ### 7c — Source attribution completeness

        Scan every metric bullet in Section 4 and every row in Section 3.
        Count any metric or config value that lacks a `[Source: ...]` annotation.

        Zero violations → PASS.
        1–2 violations → WARNING.
        3+ violations → FAIL.

        ### 7d — MySQL sub-table presence

        If both `{{MYSQL_HOST}}` and `{{MYSQL_DB}}` are not N/A: verify the MySQL table storage
        sub-table is present in the Infrastructure Configuration section and matches
        `{{MYSQL_TABLES_PATH}}`. Missing → FAIL, unless that file itself reports `NOT_COLLECTED`, in
        which case → WARNING.
        If either is N/A: verify the sub-table is absent. Present → WARNING.

        ### 7e — Weekly volume count

        Verify the Weekly Volumes table in 3.1 Traffic & Throughput has exactly 4 rows, each with a
        date range (e.g., "Jun 10-16"). More or fewer than 4 → FAIL.

        ### 7f — Kafka section presence

        If `{{KAFKA_CONSUMER_GROUPS}}` is not N/A: verify a Kafka consumer lag section is present
        in Performance Metrics. Missing → FAIL.
        If `{{KAFKA_CONSUMER_GROUPS}}` is N/A: verify it is absent. Present → WARNING.

        ### 7g — Scoring dimension structure

        Verify exactly 5 scoring dimensions are present with these exact fixed names:
        1. Traffic & Throughput
        2. Latency & Thread Concurrency
        3. Error Rates & Stability
        4. Resource Saturation
        5. Persistence Layer

        Each must use the heading shape `### Dimension <n> — <name>: <SCORE>` and include a
        **Score** line (GREEN / YELLOW / RED) and a **Rationale** line that cites at least one
        concrete metric value with a unit.

        Wrong names → FAIL. Heading shape altered → FAIL (the pipeline parses the score from it).
        Missing Score or Rationale → FAIL. No metric cited in Rationale → WARNING.

        ### 7h — Capacity table row count

        Verify exactly 4 rows in the Capacity Projections table:
        Current (Base), +1Q (+13 Weeks), +2Q (+26 Weeks), +4Q (+52 Weeks).

        More or fewer rows → FAIL.

        ### 7i — Recommendation structure and sort order

        Verify:
        - At most 7 recommendations (more → WARNING)
        - Each title carries a verbatim `(High Priority)`, `(Medium Priority)` or `(Low Priority)`
          marker — the pipeline counts these, so a missing or reworded marker → FAIL
        - Recommendations are sorted High → Medium → Low priority
        - Each recommendation has a **Finding** field and an **Action** field

        Out-of-order priority → WARNING. Missing Finding or Action → FAIL.

        ### 7j — No invented metrics

        Verify that every numeric metric value in the report has a `[Source: ...]` annotation
        or is explicitly marked `NOT_COLLECTED`. Any metric value with no source and no NOT_COLLECTED
        label → FAIL.

        ---
            """;

        FILE_REFS = """


        ## Attached Local Files

        **Review report to validate**:
        @{{REPORT_FILE}}

        Every one of your checks is decided from that report plus the two reference sections below.
        No data source and no repository is in scope for this session; if a check appears to need
        one, it belongs to another session and you should not attempt it.
            """;
    }

    private ValidationStaticChecksPrompt() {
    }
}
