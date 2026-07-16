package app.aiscalabilityreview.service;

import core.framework.http.HTTPClient;
import core.framework.http.HTTPMethod;
import core.framework.http.HTTPRequest;
import core.framework.http.HTTPResponse;
import core.framework.inject.Inject;
import core.framework.json.JSON;
import core.framework.api.json.Property;
import core.framework.util.Types;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Fetches code and configuration files from GitHub repositories
 * using the GitHub REST API (Contents API and Trees API).
 */
public class GitHubService {
    private static final String GITHUB_API_BASE = "https://api.github.com";

    // File extensions considered source/config files worth including
    private static final List<String> DEFAULT_INCLUDE_EXTENSIONS = List.of(
            ".java", ".kt", ".py", ".go", ".ts", ".js",
            ".yaml", ".yml", ".tf", ".json", ".properties", ".xml"
    );

    // Directories to skip by default
    private static final List<String> DEFAULT_EXCLUDE_DIRS = List.of(
            ".git", "node_modules", "build", "target", ".gradle", "dist", "__pycache__"
    );

    private final Logger logger = LoggerFactory.getLogger(GitHubService.class);

    @Inject
    HTTPClient httpClient;

    /**
     * Fetch the raw content of a single file from a GitHub repository.
     *
     * @param repoUrl  HTTPS or SSH URL of the repository
     * @param branch   branch name
     * @param filePath path within the repository
     * @param token    GitHub personal access token
     * @return file content as a String, or null if not found
     */
    public String fetchFileContent(String repoUrl, String branch, String filePath, String token) {
        String[] ownerRepo = extractOwnerRepo(repoUrl);
        String url = GITHUB_API_BASE + "/repos/" + ownerRepo[0] + "/" + ownerRepo[1]
                + "/contents/" + filePath + "?ref=" + branch;

        HTTPRequest request = new HTTPRequest(HTTPMethod.GET, url);
        addAuthHeaders(request, token);

        HTTPResponse response = httpClient.execute(request);
        if (response.statusCode == 404) {
            return null;
        }
        if (response.statusCode != 200) {
            throw new RuntimeException("GitHub API error fetching " + filePath + ": " + response.statusCode);
        }

        GitHubContentResponse content = JSON.fromJSON(GitHubContentResponse.class,
                new String(response.body, StandardCharsets.UTF_8));

        if (!"base64".equals(content.encoding)) {
            return content.content;
        }

        // GitHub returns base64-encoded content with newlines
        String cleaned = content.content.replace("\n", "").replace("\r", "");
        return new String(Base64.getDecoder().decode(cleaned), StandardCharsets.UTF_8);
    }

    /**
     * List all file paths in a directory of a GitHub repository (non-recursive).
     *
     * @param repoUrl HTTPS or SSH URL
     * @param branch  branch name
     * @param dirPath directory path within repo
     * @param token   GitHub token
     * @return list of file paths (relative to repo root)
     */
    public List<String> listFiles(String repoUrl, String branch, String dirPath, String token) {
        String[] ownerRepo = extractOwnerRepo(repoUrl);
        String url = GITHUB_API_BASE + "/repos/" + ownerRepo[0] + "/" + ownerRepo[1]
                + "/contents/" + dirPath + "?ref=" + branch;

        HTTPRequest request = new HTTPRequest(HTTPMethod.GET, url);
        addAuthHeaders(request, token);

        HTTPResponse response = httpClient.execute(request);
        if (response.statusCode == 404) {
            return List.of();
        }
        if (response.statusCode != 200) {
            throw new RuntimeException("GitHub API error listing " + dirPath + ": " + response.statusCode);
        }

        List<GitHubTreeItem> items = JSON.fromJSON(Types.list(GitHubTreeItem.class),
                new String(response.body, StandardCharsets.UTF_8));

        List<String> paths = new ArrayList<>();
        for (GitHubTreeItem item : items) {
            if ("file".equals(item.type)) {
                paths.add(item.path);
            }
        }
        return paths;
    }

    /**
     * Recursively fetch all files in a directory and concatenate their content
     * into a single string with file-path headers.
     *
     * @param repoUrl      HTTPS or SSH URL
     * @param branch       branch name
     * @param dirPath      directory path within repo (empty string for root)
     * @param includePaths if non-empty, only include files matching these path prefixes/extensions
     * @param excludePaths if non-empty, skip files matching these path prefixes
     * @param token        GitHub token
     * @return concatenated file contents
     */
    public String fetchDirectoryAsText(String repoUrl, String branch, String dirPath,
                                       List<String> includePaths, List<String> excludePaths,
                                       String token) {
        String[] ownerRepo = extractOwnerRepo(repoUrl);
        // Use git trees API with recursive=1 for efficiency
        String treeUrl = GITHUB_API_BASE + "/repos/" + ownerRepo[0] + "/" + ownerRepo[1]
                + "/git/trees/" + branch + "?recursive=1";
        HTTPRequest treeRequest = new HTTPRequest(HTTPMethod.GET, treeUrl);
        addAuthHeaders(treeRequest, token);
        HTTPResponse treeResponse = httpClient.execute(treeRequest);
        if (treeResponse.statusCode != 200) {
            throw new RuntimeException("GitHub tree API error for " + repoUrl + ": " + treeResponse.statusCode);
        }
        GitHubTreeResponse treeResult = JSON.fromJSON(GitHubTreeResponse.class, new String(treeResponse.body, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        int fileCount = 0;
        for (GitHubTreeEntry entry : treeResult.tree) {
            if (!"blob".equals(entry.type)) continue;
            String path = entry.path;
            // Filter by dirPath prefix
            if (!dirPath.isEmpty() && !path.startsWith(dirPath)) continue;
            // Skip default excluded directories
            boolean excluded = false;
            for (String excDir : DEFAULT_EXCLUDE_DIRS) {
                if (path.startsWith(excDir + "/") || path.contains("/" + excDir + "/")) {
                    excluded = true;
                    break;
                }
            }
            if (excluded) continue;
            // Apply custom exclude paths
            if (excludePaths != null) {
                for (String excPath : excludePaths) {
                    if (path.startsWith(excPath)) {
                        excluded = true;
                        break;
                    }
                }
            }
            if (excluded) continue;

            // Apply include paths filter
            if (includePaths != null && !includePaths.isEmpty()) {
                boolean included = false;
                for (String incPath : includePaths) {
                    if (path.startsWith(incPath) || path.endsWith(incPath)) {
                        included = true;
                        break;
                    }
                }
                if (!included) continue;
            } else {
                // Default: only include known source/config extensions
                boolean hasKnownExt = false;
                for (String ext : DEFAULT_INCLUDE_EXTENSIONS) {
                    if (path.endsWith(ext)) {
                        hasKnownExt = true;
                        break;
                    }
                }
                if (!hasKnownExt) continue;
            }

            String content = fetchFileContent(repoUrl, branch, path, token);
            if (content == null) continue;

            sb.append("\n\n--- [Source: code → ").append(path).append("] ---\n");
            sb.append(content);
            fileCount++;

            // Safety limit: stop at 200 files to avoid huge payloads
            if (fileCount >= 200) {
                sb.append("\n\n[Truncated: reached 200-file limit]");
                break;
            }
        }

        logger.info("Fetched {} files from {}/{}", fileCount, repoUrl, dirPath);
        return sb.toString();
    }

    /**
     * Get the latest commit SHA on a branch.
     *
     * @param repoUrl HTTPS or SSH URL
     * @param branch  branch name
     * @param token   GitHub token
     * @return commit SHA string
     */
    public String getLatestCommitSha(String repoUrl, String branch, String token) {
        String[] ownerRepo = extractOwnerRepo(repoUrl);
        String url = GITHUB_API_BASE + "/repos/" + ownerRepo[0] + "/" + ownerRepo[1]
                + "/commits/" + branch;

        HTTPRequest request = new HTTPRequest(HTTPMethod.GET, url);
        addAuthHeaders(request, token);

        HTTPResponse response = httpClient.execute(request);
        if (response.statusCode != 200) {
            logger.warn("Could not get commit SHA for {}/{}: {}", repoUrl, branch, response.statusCode);
            return null;
        }

        GitHubCommitResponse commit = JSON.fromJSON(GitHubCommitResponse.class,
                new String(response.body, StandardCharsets.UTF_8));
        return commit.sha;
    }

    /**
     * Select the appropriate GitHub token based on repository URL pattern.
     * Falls back to GITHUB_TOKEN if no specific token is found.
     */
    public String resolveToken(String repoUrl) {
        if (repoUrl != null && repoUrl.contains("infra")) {
            String token = System.getenv("GITHUB_INFRA_TOKEN");
            if (token != null && !token.isBlank()) return token;
        }
        if (repoUrl != null && repoUrl.contains("k8s-gitops")) {
            String token = System.getenv("GITHUB_K8S_GITOPS_TOKEN");
            if (token != null && !token.isBlank()) return token;
        }
        String appToken = System.getenv("GITHUB_APP_TOKEN");
        if (appToken != null && !appToken.isBlank()) return appToken;
        String token = System.getenv("GITHUB_TOKEN");
        if (token != null && !token.isBlank()) return token;
        throw new IllegalStateException("No GitHub token configured. Set GITHUB_TOKEN, GITHUB_APP_TOKEN, GITHUB_INFRA_TOKEN, or GITHUB_K8S_GITOPS_TOKEN.");
    }

    private void addAuthHeaders(HTTPRequest request, String token) {
        request.headers.put("Authorization", "Bearer " + token);
        request.headers.put("Accept", "application/vnd.github+json");
        request.headers.put("X-GitHub-Api-Version", "2022-11-28");
    }

    /**
     * Parse owner and repo name from HTTPS or SSH GitHub URL.
     * Handles:
     *   https://github.com/org/repo
     *   https://github.com/org/repo.git
     *   git@github.com:org/repo.git
     */
    String[] extractOwnerRepo(String repoUrl) {
        String url = repoUrl.trim();
        if (url.endsWith(".git")) {
            url = url.substring(0, url.length() - 4);
        }
        if (url.startsWith("git@")) {
            // git@github.com:org/repo
            int colonIdx = url.indexOf(':');
            String path = url.substring(colonIdx + 1);
            String[] parts = path.split("/", 2);
            return new String[]{parts[0], parts[1]};
        }
        // https://github.com/org/repo
        String[] parts = url.split("/");
        return new String[]{parts[parts.length - 2], parts[parts.length - 1]};
    }

    // ---- Inner classes for GitHub API responses ----

    public static class GitHubContentResponse {
        @Property(name = "name")
        public String name;

        @Property(name = "path")
        public String path;

        @Property(name = "type")
        public String type;

        @Property(name = "encoding")
        public String encoding;

        @Property(name = "content")
        public String content;

        @Property(name = "sha")
        public String sha;
    }

    public static class GitHubTreeItem {
        @Property(name = "name")
        public String name;

        @Property(name = "path")
        public String path;

        @Property(name = "type")
        public String type;

        @Property(name = "sha")
        public String sha;
    }

    public static class GitHubTreeResponse {
        @Property(name = "sha")
        public String sha;

        @Property(name = "url")
        public String url;

        @Property(name = "tree")
        public List<GitHubTreeEntry> tree;

        @Property(name = "truncated")
        public Boolean truncated;
    }

    public static class GitHubTreeEntry {
        @Property(name = "path")
        public String path;

        @Property(name = "mode")
        public String mode;

        @Property(name = "type")
        public String type;

        @Property(name = "sha")
        public String sha;

        @Property(name = "size")
        public Integer size;
    }

    public static class GitHubCommitResponse {
        @Property(name = "sha")
        public String sha;

        @Property(name = "url")
        public String url;
    }
}