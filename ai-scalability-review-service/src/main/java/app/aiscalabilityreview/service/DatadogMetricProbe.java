package app.aiscalabilityreview.service;

import core.framework.api.json.Property;
import core.framework.http.HTTPClient;
import core.framework.http.HTTPMethod;
import core.framework.http.HTTPRequest;
import core.framework.http.HTTPResponse;
import core.framework.inject.Inject;
import core.framework.json.JSON;
import core.framework.util.Strings;
import core.framework.util.Types;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Asks Datadog whether a metric query returns anything, before a review bets fifteen minutes on it.
 *
 * <p>This exists for one failure mode: a tag value that looks right and matches nothing. Datadog
 * answers an unknown {@code clustername} with an empty series rather than an error, so the review
 * runs to completion and reports every MongoDB metric as {@code NOT_COLLECTED} — indistinguishable
 * from a cluster that genuinely has no data. Checking first turns that into a sentence on the form.
 *
 * <p>Credentials come from the environment when set, and otherwise from the {@code datadog} entry in
 * {@code ~/.gemini/settings.json} — the same place the Gemini CLI reads them from, so the probe
 * tests the credentials the run will actually use rather than a second set that could disagree.
 */
public class DatadogMetricProbe {
    private static final Path GEMINI_SETTINGS = Path.of(System.getProperty("user.home"), ".gemini", "settings.json");
    private static final String DEFAULT_SITE = "datadoghq.com";
    private static final List<String> CREDENTIAL_PREFIXES = List.of("DD_", "DATADOG_");
    private static final long WINDOW_SECONDS = 7L * 24 * 3600;
    private static final Duration TIMEOUT = Duration.ofSeconds(15);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(60);
    private static final int ATTEMPTS = 2;

    /**
     * Reads either naming: {@code @us-all/datadog-mcp} takes {@code DD_API_KEY / DD_APP_KEY / DD_SITE},
     * the previous {@code mcp-server-datadog} took {@code DATADOG_API_KEY / DATADOG_APP_KEY / DATADOG_SITE}.
     * A settings file written for either server keeps working.
     */
    static Credentials readCredentials(Map<String, String> env) {
        for (String prefix : CREDENTIAL_PREFIXES) {
            String apiKey = env.get(prefix + "API_KEY");
            String appKey = env.get(prefix + "APP_KEY");
            if (!Strings.isBlank(apiKey) && !Strings.isBlank(appKey)) {
                String site = env.get(prefix + "SITE");
                return new Credentials(apiKey, appKey, Strings.isBlank(site) ? DEFAULT_SITE : site);
            }
        }
        return null;
    }

    private final Logger logger = LoggerFactory.getLogger(DatadogMetricProbe.class);

    @Inject
    HTTPClient httpClient;

    public boolean hasCredentials() {
        return credentials() != null;
    }

    /**
     * @return the number of series the query returns, or null when Datadog could not be asked at
     *         all — a missing credential is a different answer from "no data", and the caller has
     *         to be able to say which
     */
    public Integer seriesCount(String query) {
        QueryResponse response = query(query);
        return response == null ? null : response.series == null ? 0 : response.series.size();
    }

    /**
     * Every value a tag takes across the metric, sorted. Returned whole rather than capped: the
     * caller narrows by environment first, and a cap applied here would truncate the very names it
     * is looking for. Best effort — an empty list means either no data or no credentials.
     */
    public List<String> tagValues(String query, String tag) {
        QueryResponse response = query(query);
        if (response == null || response.series == null) return List.of();
        String prefix = tag + ':';
        List<String> values = new ArrayList<>();
        for (Series series : response.series) {
            if (series.scope == null) continue;
            for (String part : series.scope.split(",")) {
                String trimmed = part.trim();
                if (trimmed.startsWith(prefix) && !values.contains(trimmed.substring(prefix.length()))) {
                    values.add(trimmed.substring(prefix.length()));
                }
            }
        }
        values.sort(String::compareTo);
        return values;
    }

    /**
     * One metric query over an explicit window, with the points.
     *
     * <p>The review's metrics used to be read by a model through the Datadog MCP server and copied into
     * a file by hand, and the copy was wrong often enough to fail validation: values rounded or invented,
     * a day-aligned peak dated {@code +058794} because the server's timestamps are not epoch milliseconds.
     * The REST API returns the points with correct timestamps, so the numbers are now read here.
     *
     * @return the response, or null when Datadog could not be asked or refused (the caller records a gap)
     */
    public QueryResponse queryRange(String query, long fromSeconds, long toSeconds) {
        HTTPResponse response = request("/api/v1/query", Map.of(
            "from", String.valueOf(fromSeconds), "to", String.valueOf(toSeconds), "query", query));
        if (response == null) return null;
        try {
            QueryResponse parsed = JSON.fromJSON(QueryResponse.class, response.text());
            return parsed.error == null ? parsed : null;
        } catch (RuntimeException e) {
            logger.warn("Datadog answered {} with something that is not a query response: {}", query, e.getMessage());
            return null;
        }
    }

    /**
     * Monitors of the service that are in ALERT or WARN. Datadog filters by host tags ({@code tags}) and by
     * the tags on the monitor itself ({@code monitor_tags}); a service's monitors carry it in one or the
     * other, so both are asked and the answers are merged.
     *
     * @return null when Datadog could not be asked
     */
    public List<Monitor> monitors(String service) {
        Map<Long, Monitor> merged = new LinkedHashMap<>();
        for (String filter : List.of("tags", "monitor_tags")) {
            HTTPResponse response = request("/api/v1/monitor", Map.of(filter, "service:" + service, "group_states", "alert,warn"));
            if (response == null) return null;
            List<Monitor> found = JSON.fromJSON(Types.list(Monitor.class), response.text());
            for (Monitor monitor : found) {
                merged.put(monitor.id, monitor);
            }
        }
        return new ArrayList<>(merged.values());
    }

    /**
     * The SLOs tagged with the service, each with its attainment over the window. Only objectives carrying
     * the service tag count; a name search also finds other services' objectives and report SLOs.
     *
     * @return null when Datadog could not be asked
     */
    public List<Slo> slos(String service, long fromSeconds, long toSeconds) {
        HTTPResponse list = request("/api/v1/slo", Map.of("tags_query", "service:" + service));
        if (list == null) return null;
        List<Slo> objectives = JSON.fromJSON(SloList.class, list.text()).data;
        List<Slo> result = new ArrayList<>(objectives.size());
        for (Slo slo : objectives) {
            HTTPResponse history = request("/api/v1/slo/" + slo.id + "/history",
                Map.of("from_ts", String.valueOf(fromSeconds), "to_ts", String.valueOf(toSeconds)));
            if (history != null) {
                SloHistory parsed = JSON.fromJSON(SloHistory.class, history.text());
                if (parsed.data != null && parsed.data.overall != null) slo.sliValue = parsed.data.overall.sliValue;
            }
            result.add(slo);
        }
        return result;
    }

    /** GET against the Datadog API with the configured keys; null on a missing credential, an error status or a timeout. */
    public HTTPResponse request(String path, Map<String, String> params) {
        Credentials credentials = credentials();
        if (credentials == null) return null;
        HTTPRequest request = new HTTPRequest(HTTPMethod.GET, "https://api." + credentials.site() + path);
        request.params.putAll(params);
        request.headers.put("DD-API-KEY", credentials.apiKey());
        request.headers.put("DD-APPLICATION-KEY", credentials.appKey());
        request.timeout = REQUEST_TIMEOUT;
        for (int attempt = 1; attempt <= ATTEMPTS; attempt++) {
            try {
                HTTPResponse response = httpClient.execute(request);
                if (response.statusCode == 200) return response;
                logger.warn("Datadog returned {} for {} {}", response.statusCode, path, params);
            } catch (RuntimeException e) {
                logger.warn("Datadog request {} failed (attempt {}): {}", path, attempt, e.getMessage());
            }
        }
        return null;
    }

    private QueryResponse query(String query) {
        Credentials credentials = credentials();
        if (credentials == null) return null;
        long to = Instant.now().atZone(ZoneOffset.UTC).toLocalDate().atStartOfDay(ZoneOffset.UTC).toEpochSecond();

        HTTPRequest request = new HTTPRequest(HTTPMethod.GET, "https://api." + credentials.site() + "/api/v1/query");
        request.params.put("from", String.valueOf(to - WINDOW_SECONDS));
        request.params.put("to", String.valueOf(to));
        request.params.put("query", query);
        request.headers.put("DD-API-KEY", credentials.apiKey());
        request.headers.put("DD-APPLICATION-KEY", credentials.appKey());
        request.timeout = TIMEOUT;
        try {
            HTTPResponse response = httpClient.execute(request);
            if (response.statusCode != 200) {
                logger.warn("Datadog probe returned {} for {}", response.statusCode, query);
                return null;
            }
            return JSON.fromJSON(QueryResponse.class, response.text());
        } catch (RuntimeException e) {
            logger.warn("Could not reach Datadog to probe {}: {}", query, e.getMessage());
            return null;
        }
    }

    private Credentials credentials() {
        Credentials fromEnv = readCredentials(System.getenv());
        return fromEnv != null ? fromEnv : geminiCredentials();
    }

    /** The Gemini CLI's own Datadog entry, so the probe and the run share one set of keys. */
    private Credentials geminiCredentials() {
        if (!Files.isRegularFile(GEMINI_SETTINGS)) return null;
        try {
            GeminiSettings settings = JSON.fromJSON(GeminiSettings.class,
                Files.readString(GEMINI_SETTINGS, StandardCharsets.UTF_8));
            if (settings.mcpServers == null) return null;
            McpServer datadog = settings.mcpServers.get("datadog");
            if (datadog == null || datadog.env == null) return null;
            return readCredentials(datadog.env);
        } catch (IOException | RuntimeException e) {
            logger.warn("Could not read the Datadog credentials from {}: {}", GEMINI_SETTINGS, e.getMessage());
            return null;
        }
    }

    record Credentials(String apiKey, String appKey, String site) {
    }

    public static class QueryResponse {
        @Property(name = "status")
        public String status;

        @Property(name = "series")
        public List<Series> series;

        @Property(name = "error")
        public String error;
    }

    public static class Series {
        @Property(name = "scope")
        public String scope;

        @Property(name = "expression")
        public String expression;

        /** {@code [timestamp in epoch milliseconds, value]} pairs; the value is null for an empty bucket. */
        @Property(name = "pointlist")
        public List<List<Double>> pointlist;
    }

    public static class Monitor {
        @Property(name = "id")
        public Long id;

        @Property(name = "name")
        public String name;

        @Property(name = "overall_state")
        public String overallState;
    }

    public static class Slo {
        @Property(name = "id")
        public String id;

        @Property(name = "name")
        public String name;

        @Property(name = "type")
        public String type;

        @Property(name = "thresholds")
        public List<SloThreshold> thresholds;

        /** The 28-day attainment in percent; null until the history has been read. */
        @Property(name = "sli_value")
        public Double sliValue;
    }

    public static class SloThreshold {
        @Property(name = "timeframe")
        public String timeframe;

        @Property(name = "target")
        public Double target;
    }

    public static class SloList {
        @Property(name = "data")
        public List<Slo> data = new ArrayList<>();
    }

    public static class SloHistory {
        @Property(name = "data")
        public SloHistoryData data;
    }

    public static class SloHistoryData {
        @Property(name = "overall")
        public SloOverall overall;
    }

    public static class SloOverall {
        @Property(name = "sli_value")
        public Double sliValue;
    }

    public static class GeminiSettings {
        @Property(name = "mcpServers")
        public Map<String, McpServer> mcpServers;
    }

    public static class McpServer {
        @Property(name = "env")
        public Map<String, String> env;
    }
}
