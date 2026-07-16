package app.aiscalabilityreview.service;

import core.framework.http.HTTPClient;
import core.framework.http.HTTPMethod;
import core.framework.http.HTTPRequest;
import core.framework.http.HTTPResponse;
import core.framework.inject.Inject;
import core.framework.json.JSON;
import core.framework.api.json.Property;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.DoubleSummaryStatistics;
import java.util.List;

/**
 * Queries Datadog metrics and monitors APIs.
 */
public class DatadogService {
    private static final String DATADOG_METRICS_URL = "https://api.datadoghq.com/api/v1/query";
    private static final String DATADOG_MONITORS_URL = "https://api.datadoghq.com/api/v1/monitor";
    private static final long SECONDS_IN_28_DAYS = 28L * 24 * 3600;

    private final Logger logger = LoggerFactory.getLogger(DatadogService.class);

    @Inject
    HTTPClient httpClient;

    /**
     * Query Datadog metrics for a time range.
     *
     * @param query       Datadog metric query string
     * @param fromEpochSec start of range (Unix epoch seconds)
     * @param toEpochSec   end of range (Unix epoch seconds)
     * @return MetricQueryResult containing point list and summary statistics
     */
    public MetricQueryResult queryMetrics(String query, long fromEpochSec, long toEpochSec) {
        String apiKey = requireEnv("DATADOG_API_KEY");
        String appKey = requireEnv("DATADOG_APP_KEY");

        String url = DATADOG_METRICS_URL
                + "?from=" + fromEpochSec
                + "&to=" + toEpochSec
                + "&query=" + urlEncode(query);

        HTTPRequest request = new HTTPRequest(HTTPMethod.GET, url);
        request.headers.put("DD-API-KEY", apiKey);
        request.headers.put("DD-APPLICATION-KEY", appKey);
        request.headers.put("Accept", "application/json");

        HTTPResponse response = httpClient.execute(request);
        if (response.statusCode != 200) {
            String body = response.body == null ? "(empty)" : new String(response.body, StandardCharsets.UTF_8);
            logger.warn("Datadog metrics query failed: status={}, query={}, body={}", response.statusCode, query, body);
            throw new RuntimeException("Datadog API error " + response.statusCode + " for query: " + query);
        }

        DatadogQueryResponse ddResponse = JSON.fromJSON(DatadogQueryResponse.class,
                new String(response.body, StandardCharsets.UTF_8));

        return buildResult(query, ddResponse);
    }

    /**
     * Query Datadog metrics for the last 28 days (now - 28d to now).
     *
     * @param query Datadog metric query string
     * @return MetricQueryResult
     */
    public MetricQueryResult queryMetrics28Day(String query) {
        long now = Instant.now().getEpochSecond();
        return queryMetrics(query, now - SECONDS_IN_28_DAYS, now);
    }

    /**
     * List Datadog monitors filtered by a service tag.
     *
     * @param serviceTag service tag value (e.g. "my-service")
     * @return JSON string summary of monitor states
     */
    public String listMonitors(String serviceTag) {
        String apiKey = requireEnv("DATADOG_API_KEY");
        String appKey = requireEnv("DATADOG_APP_KEY");

        String url = DATADOG_MONITORS_URL + "?tags=service:" + urlEncode(serviceTag);

        HTTPRequest request = new HTTPRequest(HTTPMethod.GET, url);
        request.headers.put("DD-API-KEY", apiKey);
        request.headers.put("DD-APPLICATION-KEY", appKey);
        request.headers.put("Accept", "application/json");

        HTTPResponse response = httpClient.execute(request);
        if (response.statusCode != 200) {
            logger.warn("Datadog monitors query failed: status={}, service={}", response.statusCode, serviceTag);
            return "NOT_COLLECTED: Datadog monitors API returned " + response.statusCode;
        }

        return new String(response.body, StandardCharsets.UTF_8);
    }

    private MetricQueryResult buildResult(String query, DatadogQueryResponse ddResponse) {
        MetricQueryResult result = new MetricQueryResult();
        result.query = query;

        if (ddResponse.series == null || ddResponse.series.isEmpty()) {
            result.pointList = List.of();
            return result;
        }

        // Take first series
        DatadogSeries series = ddResponse.series.get(0);
        result.pointList = series.pointlist;

        if (result.pointList != null && !result.pointList.isEmpty()) {
            DoubleSummaryStatistics stats = result.pointList.stream()
                    .filter(p -> p != null && p.size() >= 2 && p.get(1) != null)
                    .mapToDouble(p -> p.get(1))
                    .summaryStatistics();

            result.avg = stats.getAverage();
            result.max = stats.getMax();
            result.min = stats.getMin();
            result.sum = stats.getSum();
            result.count = (int) stats.getCount();
        }

        return result;
    }

    private String requireEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Environment variable " + name + " is not set");
        }
        return value;
    }

    private String urlEncode(String value) {
        try {
            return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return value;
        }
    }

    public static class MetricQueryResult {
        public String query;
        public List<List<Double>> pointList;
        public double avg;
        public double max;
        public double min;
        public double sum;
        public int count;
    }

    public static class DatadogQueryResponse {
        @Property(name = "status")
        public String status;

        @Property(name = "from_date")
        public Long fromDate;

        @Property(name = "to_date")
        public Long toDate;

        @Property(name = "series")
        public List<DatadogSeries> series;

        @Property(name = "error")
        public String error;
    }

    public static class DatadogSeries {
        @Property(name = "metric")
        public String metric;

        @Property(name = "display_name")
        public String displayName;

        @Property(name = "pointlist")
        public List<List<Double>> pointlist;

        @Property(name = "aggr")
        public String aggr;

        @Property(name = "start")
        public Long start;

        @Property(name = "end")
        public Long end;

        @Property(name = "interval")
        public Long interval;

        @Property(name = "length")
        public Integer length;
    }
}