package app.aiscalabilityreview.api.review;

import core.framework.api.json.Property;

import java.time.ZonedDateTime;
import java.util.List;

/** Token and cost aggregates (FR-7.6). Every monetary figure is an estimate from the price table. */
public class GetUsageResponse {
    @Property(name = "group_by")
    public String groupBy;

    @Property(name = "runs")
    public Integer runs;

    @Property(name = "total_tokens")
    public Long totalTokens;

    @Property(name = "total_input_tokens")
    public Long totalInputTokens;

    @Property(name = "total_output_tokens")
    public Long totalOutputTokens;

    @Property(name = "total_cached_tokens")
    public Long totalCachedTokens;

    @Property(name = "total_duration_ms")
    public Long totalDurationMs;

    @Property(name = "total_cost_usd")
    public Double totalCostUsd;

    @Property(name = "month_to_date_cost_usd")
    public Double monthToDateCostUsd;

    @Property(name = "monthly_ceiling_usd")
    public Double monthlyCeilingUsd;

    @Property(name = "price_table_version")
    public String priceTableVersion;

    @Property(name = "buckets")
    public List<UsageBucketView> buckets;

    @Property(name = "top_runs")
    public List<UsageRunView> topRuns;

    public static class UsageBucketView {
        @Property(name = "key")
        public String key;

        @Property(name = "runs")
        public Integer runs;

        @Property(name = "input_tokens")
        public Long inputTokens;

        @Property(name = "output_tokens")
        public Long outputTokens;

        @Property(name = "cached_tokens")
        public Long cachedTokens;

        @Property(name = "total_tokens")
        public Long totalTokens;

        @Property(name = "duration_ms")
        public Long durationMs;

        @Property(name = "mcp_tool_calls")
        public Integer mcpToolCalls;

        @Property(name = "api_requests")
        public Integer apiRequests;

        @Property(name = "cost_usd")
        public Double costUsd;
    }

    public static class UsageRunView {
        @Property(name = "job_id")
        public String jobId;

        @Property(name = "service_id")
        public String serviceId;

        @Property(name = "env")
        public String env;

        @Property(name = "started_at")
        public ZonedDateTime startedAt;

        @Property(name = "total_tokens")
        public Long totalTokens;

        @Property(name = "duration_ms")
        public Long durationMs;

        @Property(name = "cost_usd")
        public Double costUsd;

        @Property(name = "within_budget")
        public Boolean withinBudget;
    }
}
