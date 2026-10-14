package app.aiscalabilityreview.service;

import core.framework.api.json.Property;
import core.framework.http.HTTPClient;
import core.framework.http.HTTPMethod;
import core.framework.http.HTTPRequest;
import core.framework.http.HTTPResponse;
import core.framework.inject.Inject;
import core.framework.json.JSON;
import core.framework.util.Strings;
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
    private static final long WINDOW_SECONDS = 7L * 24 * 3600;
    private static final Duration TIMEOUT = Duration.ofSeconds(15);

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
        String apiKey = System.getenv("DATADOG_API_KEY");
        String appKey = System.getenv("DATADOG_APP_KEY");
        if (!Strings.isBlank(apiKey) && !Strings.isBlank(appKey)) {
            String site = System.getenv("DATADOG_SITE");
            return new Credentials(apiKey, appKey, Strings.isBlank(site) ? DEFAULT_SITE : site);
        }
        return geminiCredentials();
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
            String apiKey = datadog.env.get("DATADOG_API_KEY");
            String appKey = datadog.env.get("DATADOG_APP_KEY");
            if (Strings.isBlank(apiKey) || Strings.isBlank(appKey)) return null;
            String site = datadog.env.get("DATADOG_SITE");
            return new Credentials(apiKey, appKey, Strings.isBlank(site) ? DEFAULT_SITE : site);
        } catch (IOException | RuntimeException e) {
            logger.warn("Could not read the Datadog credentials from {}: {}", GEMINI_SETTINGS, e.getMessage());
            return null;
        }
    }

    private record Credentials(String apiKey, String appKey, String site) {
    }

    public static class QueryResponse {
        @Property(name = "status")
        public String status;

        @Property(name = "series")
        public List<Series> series;
    }

    public static class Series {
        @Property(name = "scope")
        public String scope;
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
