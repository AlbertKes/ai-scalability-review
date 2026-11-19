package app.aiscalabilityreview.prompt;

/**
 * How to call and read the {@code @us-all/datadog-mcp} server, shared by the two stages that query
 * Datadog (metric collection and data validation) so they read the same response the same way.
 *
 * <p>Everything here was measured against that server (v1.21.1), not taken from its documentation:
 * <ul>
 *   <li>{@code query-metrics} rewrites Datadog's {@code pointlist} into {@code points[].timestamp}
 *       by multiplying the already-millisecond epoch by 1000, so every timestamp comes back in a year
 *       like {@code +058776}. The values and their order are correct; only the dates are not.</li>
 *   <li>It drops Datadog's {@code status} and {@code error} fields. One invalid expression in a
 *       comma-separated query empties the whole response, which then looks exactly like "no data".</li>
 *   <li>An expression with no data in the window returns no series at all.</li>
 * </ul>
 */
public final class DatadogMcpPrompt {
    public static final String RESPONSE_RULES = """
        ## Calling and reading the Datadog MCP server

        The Datadog tools are named with hyphens: `query-metrics`, `get-monitors`, `list-slos`,
        `slo-compliance-snapshot` (listed to you as `mcp_datadog_query-metrics` and so on). Names
        with underscores (`query_metrics`, `get_monitors`) belong to an older server and do not
        exist here.

        **Keep every response under 40,000 characters.** The CLI cuts a longer tool response down to
        its first 8,000 and last 32,000 characters, so the series in the middle silently vanish.
        This server is verbose: one 28-point daily series costs about 3,500 characters and one
        168-point hourly series about 18,000, so a call holds **at most 8 daily series** — count one
        per group for a `by {…}` expression — or one week of hourly points. A response that begins
        `Output too large` has lost its middle: never compute from it; split the call and re-run.

        `query-metrics` takes `{query, from, to}` — `from` and `to` in epoch **seconds**. Datadog
        accepts several expressions in one `query` as a comma-separated string. The response is:

        ```
        {"query": ..., "from": "<ISO>", "to": "<ISO>", "seriesCount": <n>,
         "series": [{"expression": ..., "scope": ..., "unit": [<unit>, <per-unit>],
                     "pointCount": <n>, "points": [{"timestamp": ..., "value": ...}, ...]}]}
        ```

        - **Match a series to its expression by the `expression` field**, never by position.
        - **A grouped expression (`by {topic}`, `by {host}`) comes back rewritten**, one series per
          group, with the group tag moved into the filter and the `by {…}` clause gone:
          `avg:kafka.consumer_lag{env:prod,consumer_group:g} by {topic}.rollup(avg, 86400)` returns
          series whose `expression` reads
          `avg:kafka.consumer_lag{topic:t1,consumer_group:g,env:prod}.rollup(avg, 86400)`. Match
          those by aggregator, metric and rollup — the parts that survive — and read the group from
          `scope`. A grouped expression with no exact `expression` match has not returned nothing.
        - **Never read a date from `points[].timestamp`.** This server mis-converts it and every
          value comes back in a year like `+058776`. The points are in time order, oldest first,
          one per rollup bucket starting at `from`, so the time of point `i` (0-based) is
          `from + i × <rollup seconds>` — `86400` for the daily rollups, `3600` for the hourly one.
          That holds only when `pointCount` equals the number of buckets in the window (28 daily,
          672 hourly). If it is smaller, buckets were dropped: report the value and write
          `date unresolved (<pointCount> of <expected> buckets returned)` instead of a date.
        - **`unit[0]`** describes the value: `name` (`byte`, `nanocore`, `second`, `hit`, ...) and a
          `scaleFactor` meant for rendering — do not apply it. `unit` may be `null`; then the unit
          stated in this prompt for that metric applies.
        - **An expression with no data returns no series.** For an `.as_count()` sum that means `0`;
          for a gauge write `NOT_COLLECTED: no data reported in the window`.
        - **`seriesCount: 0` for a whole multi-expression call is not proof of no data.** This server
          drops Datadog's error field, so a single malformed expression empties the entire
          response. Before writing any value of that call as missing, re-run each of its
          expressions on its own; only an expression that is still empty alone has no data.
        """;

    /** Group 6 of metric collection; {@code {{SERVICE}}} is filled in by the prompt builder. */
    public static final String MONITORS_AND_SLOS = """
        ## Group 6 — Datadog: monitors and SLOs

        **Monitors.** Call `get-monitors` with `tags = "service:{{SERVICE}}"` and
        `groupStates = "alert,warn"` — both are comma-separated strings, not arrays. Record every
        monitor returned with its name and `overallState`; if the list is empty, write
        `None in ALERT or WARN`.

        **SLOs.**
        1. Call `list-slos` with `tagsQuery = "service:{{SERVICE}}"`. Only objectives tagged with this
           service count; do not widen the search by name — a name match picks up report SLOs and
           other services' objectives.
        2. For each SLO returned, call `slo-compliance-snapshot` with `sloId = <id>`,
           `historyDays = 28` and
           `extractFields = "slo.name,slo.type,slo.target,slo.thresholds,history.sliValue,errorBudgetRemainingPct,status,caveats"`.
        3. Record per SLO: name, type, target and its timeframe (from `slo.thresholds`), the 28-day
           attainment (`history.sliValue`, %), `errorBudgetRemainingPct` and `status`
           (`compliant | at-risk | breached`). The snapshot's window is the last 28 days to now
           rather than the day-aligned window above; say so once.
        4. If `list-slos` returns none, write `None defined for service:{{SERVICE}}`. That is a
           finding about the service, not a collection gap — it does not belong in Section 10. Only
           a tool error is `NOT_COLLECTED`. Never offer monitors in place of SLOs — a monitor is not
           an objective.
        """;

    private DatadogMcpPrompt() {
    }
}
