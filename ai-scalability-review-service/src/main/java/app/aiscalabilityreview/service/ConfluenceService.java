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
import java.util.Base64;
import java.util.List;
import java.util.Optional;

/**
 * Creates and updates Confluence pages using the Confluence REST API v1.
 * Reads CONFLUENCE_URL, CONFLUENCE_USERNAME, CONFLUENCE_API_TOKEN from environment.
 */
public class ConfluenceService {
    private final Logger logger = LoggerFactory.getLogger(ConfluenceService.class);

    @Inject
    HTTPClient httpClient;

    /**
     * Create a new Confluence page or update it if a page with the same title already exists.
     *
     * @param spaceKey      Confluence space key, e.g. "ENG"
     * @param parentPageId  ID of the parent page (can be null to use root)
     * @param title         page title
     * @param markdownContent markdown content to convert and publish
     * @return public URL of the created/updated page
     */
    public String createOrUpdatePage(String spaceKey, String parentPageId, String title, String markdownContent) {
        String baseUrl = requireEnv("CONFLUENCE_URL");
        String htmlContent = markdownToConfluenceHtml(markdownContent);

        Optional<String> existingPageId = getPageIdByTitle(spaceKey, title);
        if (existingPageId.isPresent()) {
            return updatePage(existingPageId.get(), title, htmlContent, baseUrl);
        } else {
            return createPage(spaceKey, parentPageId, title, htmlContent, baseUrl);
        }
    }

    /**
     * Find a Confluence page ID by its title within a space.
     *
     * @param spaceKey Confluence space key
     * @param title    page title to search for
     * @return Optional containing the page ID if found, empty otherwise
     */
    public Optional<String> getPageIdByTitle(String spaceKey, String title) {
        String baseUrl = requireEnv("CONFLUENCE_URL");
        try {
            String encodedTitle = java.net.URLEncoder.encode(title, StandardCharsets.UTF_8);
            String url = baseUrl + "/rest/api/content?spaceKey=" + spaceKey
                    + "&title=" + encodedTitle + "&expand=version";

            HTTPRequest request = new HTTPRequest(HTTPMethod.GET, url);
            addAuthHeaders(request);

            HTTPResponse response = httpClient.execute(request);
            if (response.statusCode != 200) {
                return Optional.empty();
            }

            ConfluenceSearchResponse searchResult = JSON.fromJSON(ConfluenceSearchResponse.class,
                    new String(response.body, StandardCharsets.UTF_8));

            if (searchResult.results != null && !searchResult.results.isEmpty()) {
                return Optional.of(searchResult.results.get(0).id);
            }
            return Optional.empty();
        } catch (Exception e) {
            logger.warn("Failed to search for Confluence page '{}': {}", title, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Get comments on a Confluence page.
     *
     * @param pageId Confluence page ID
     * @return list of ConfluenceComment objects
     */
    public List<ConfluenceComment> getPageComments(String pageId) {
        String baseUrl = requireEnv("CONFLUENCE_URL");
        String url = baseUrl + "/rest/api/content/" + pageId + "/child/comment?expand=body.storage,version,metadata";

        HTTPRequest request = new HTTPRequest(HTTPMethod.GET, url);
        addAuthHeaders(request);

        HTTPResponse response = httpClient.execute(request);
        if (response.statusCode != 200) {
            logger.warn("Failed to fetch comments for page {}: {}", pageId, response.statusCode);
            return List.of();
        }

        ConfluenceSearchResponse searchResult = JSON.fromJSON(ConfluenceSearchResponse.class,
                new String(response.body, StandardCharsets.UTF_8));

        if (searchResult.results == null) return List.of();

        return searchResult.results.stream()
                .map(r -> {
                    ConfluenceComment comment = new ConfluenceComment();
                    comment.id = r.id;
                    comment.title = r.title;
                    if (r.body != null && r.body.storage != null) {
                        comment.bodyText = r.body.storage.value;
                    }
                    if (r.version != null) {
                        comment.author = r.version.by != null ? r.version.by.displayName : null;
                    }
                    return comment;
                })
                .toList();
    }

    private String createPage(String spaceKey, String parentPageId, String title, String htmlContent, String baseUrl) {
        ConfluencePageRequest body = buildPageRequest(spaceKey, parentPageId, title, htmlContent, null, 1);
        byte[] bodyBytes = JSON.toJSON(body).getBytes(StandardCharsets.UTF_8);

        String url = baseUrl + "/rest/api/content";
        HTTPRequest request = new HTTPRequest(HTTPMethod.POST, url);
        addAuthHeaders(request);
        request.headers.put("Content-Type", "application/json");
        request.body = bodyBytes;

        HTTPResponse response = httpClient.execute(request);
        if (response.statusCode != 200 && response.statusCode != 201) {
            String responseBody = response.body == null ? "(empty)" : new String(response.body, StandardCharsets.UTF_8);
            throw new RuntimeException("Confluence create page failed: " + response.statusCode + " — " + responseBody);
        }

        ConfluencePageResponse pageResponse = JSON.fromJSON(ConfluencePageResponse.class,
                new String(response.body, StandardCharsets.UTF_8));

        String pageUrl = baseUrl + "/wiki/spaces/" + spaceKey + "/pages/" + pageResponse.id;
        logger.info("Created Confluence page '{}' with ID {}: {}", title, pageResponse.id, pageUrl);
        return pageUrl;
    }

    private String updatePage(String pageId, String title, String htmlContent, String baseUrl) {
        int currentVersion = getPageVersion(pageId, baseUrl);

        ConfluencePageRequest body = buildPageRequest(null, null, title, htmlContent, pageId, currentVersion + 1);
        byte[] bodyBytes = JSON.toJSON(body).getBytes(StandardCharsets.UTF_8);

        String url = baseUrl + "/rest/api/content/" + pageId;
        HTTPRequest request = new HTTPRequest(HTTPMethod.PUT, url);
        addAuthHeaders(request);
        request.headers.put("Content-Type", "application/json");
        request.body = bodyBytes;

        HTTPResponse response = httpClient.execute(request);
        if (response.statusCode != 200) {
            String responseBody = response.body == null ? "(empty)" : new String(response.body, StandardCharsets.UTF_8);
            throw new RuntimeException("Confluence update page failed: " + response.statusCode + " — " + responseBody);
        }

        ConfluencePageResponse pageResponse = JSON.fromJSON(ConfluencePageResponse.class,
                new String(response.body, StandardCharsets.UTF_8));

        String spaceKey = pageResponse.space != null ? pageResponse.space.key : "";
        String pageUrl = baseUrl + "/wiki/spaces/" + spaceKey + "/pages/" + pageId;
        logger.info("Updated Confluence page '{}' (ID {}): {}", title, pageId, pageUrl);
        return pageUrl;
    }

    private int getPageVersion(String pageId, String baseUrl) {
        String url = baseUrl + "/rest/api/content/" + pageId + "?expand=version";
        HTTPRequest request = new HTTPRequest(HTTPMethod.GET, url);
        addAuthHeaders(request);

        HTTPResponse response = httpClient.execute(request);
        if (response.statusCode != 200) return 1;

        ConfluencePageResponse page = JSON.fromJSON(ConfluencePageResponse.class,
                new String(response.body, StandardCharsets.UTF_8));
        return page.version != null ? page.version.number : 1;
    }

    private ConfluencePageRequest buildPageRequest(String spaceKey, String parentPageId,
                                                   String title, String htmlContent,
                                                   String pageId, int version) {
        ConfluencePageRequest req = new ConfluencePageRequest();
        req.id = pageId;
        req.type = "page";
        req.title = title;

        if (spaceKey != null) {
            req.space = new ConfluencePageRequest.Space();
            req.space.key = spaceKey;
        }

        req.body = new ConfluencePageRequest.Body();
        req.body.storage = new ConfluencePageRequest.Storage();
        req.body.storage.value = htmlContent;
        req.body.storage.representation = "storage";

        req.version = new ConfluencePageRequest.Version();
        req.version.number = version;

        if (parentPageId != null) {
            ConfluencePageRequest.Ancestor ancestor = new ConfluencePageRequest.Ancestor();
            ancestor.id = parentPageId;
            req.ancestors = List.of(ancestor);
        }

        return req;
    }

    private void addAuthHeaders(HTTPRequest request) {
        String username = requireEnv("CONFLUENCE_USERNAME");
        String apiToken = requireEnv("CONFLUENCE_API_TOKEN");
        String credentials = Base64.getEncoder().encodeToString(
                (username + ":" + apiToken).getBytes(StandardCharsets.UTF_8));
        request.headers.put("Authorization", "Basic " + credentials);
        request.headers.put("Accept", "application/json");
    }

    private String requireEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Environment variable " + name + " is not set");
        }
        return value;
    }

    /**
     * Convert Markdown to Confluence storage format HTML.
     * This is a minimal implementation; for production use a proper Markdown parser.
     */
    String markdownToConfluenceHtml(String markdown) {
        if (markdown == null) return "";
        // Minimal conversion: wrap in a preformatted block via Confluence macro
        // In a real implementation use commonmark or flexmark library
        return "<ac:structured-macro ac:name=\"code\"><ac:parameter ac:name=\"language\">markdown</ac:parameter>"
                + "<ac:plain-text-body><![CDATA[" + markdown + "]]></ac:plain-text-body></ac:structured-macro>";
    }

    public static class ConfluenceComment {
        public String id;
        public String title;
        public String bodyText;
        public String author;
    }

    public static class ConfluenceSearchResponse {
        @Property(name = "results")
        public List<ConfluenceResult> results;

        @Property(name = "size")
        public Integer size;
    }

    public static class ConfluenceResult {
        @Property(name = "id")
        public String id;

        @Property(name = "type")
        public String type;

        @Property(name = "title")
        public String title;

        @Property(name = "body")
        public ConfluenceBody body;

        @Property(name = "version")
        public ConfluenceVersion version;

        @Property(name = "space")
        public ConfluenceSpace space;
    }

    public static class ConfluenceBody {
        @Property(name = "storage")
        public ConfluenceStorage storage;
    }

    public static class ConfluenceStorage {
        @Property(name = "value")
        public String value;

        @Property(name = "representation")
        public String representation;
    }

    public static class ConfluenceVersion {
        @Property(name = "number")
        public Integer number;

        @Property(name = "by")
        public ConfluenceUser by;
    }

    public static class ConfluenceUser {
        @Property(name = "displayName")
        public String displayName;
    }

    public static class ConfluenceSpace {
        @Property(name = "key")
        public String key;
    }

    public static class ConfluencePageRequest {
        @Property(name = "id")
        public String id;

        @Property(name = "type")
        public String type;

        @Property(name = "title")
        public String title;

        @Property(name = "space")
        public Space space;

        @Property(name = "body")
        public Body body;

        @Property(name = "version")
        public Version version;

        @Property(name = "ancestors")
        public List<Ancestor> ancestors;

        public static class Space {
            @Property(name = "key")
            public String key;
        }

        public static class Body {
            @Property(name = "storage")
            public Storage storage;
        }

        public static class Storage {
            @Property(name = "value")
            public String value;

            @Property(name = "representation")
            public String representation;
        }

        public static class Version {
            @Property(name = "number")
            public Integer number;
        }

        public static class Ancestor {
            @Property(name = "id")
            public String id;
        }
    }

    public static class ConfluencePageResponse {
        @Property(name = "id")
        public String id;

        @Property(name = "type")
        public String type;

        @Property(name = "title")
        public String title;

        @Property(name = "version")
        public ConfluenceVersion version;

        @Property(name = "space")
        public ConfluenceSpace space;
    }
}