package app.aiscalabilityreview.service;

import app.aiscalabilityreview.service.GeminiCliOutput.ModelStats;
import app.aiscalabilityreview.service.GeminiCliOutput.TokenStats;
import app.aiscalabilityreview.service.GeminiCliOutput.ToolCallStats;
import core.framework.json.JSON;
import core.framework.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Runs the local Gemini CLI as a subprocess with a given prompt.
 * The prompt is written to a temporary file and passed via "-p @promptFile" so the Gemini CLI
 * expands all @file references (source repos, infra configs) before sending to the model.
 * Runs from a neutral temp directory to prevent Gemini from treating the current project
 * as its workspace. Each invocation uses a unique session ID to avoid inheriting prior context.
 * Callers must pass includeDirs to whitelist any paths the prompt references via @file.
 *
 * <p>Two cost/latency controls are exposed per invocation (AD-6383):
 * <ul>
 *   <li>{@code allowedMcpServers} — only these MCP servers from the local Gemini CLI setup are
 *       started and have their tool schemas injected into the model context. Passing
 *       {@link #MCP_DISABLED} keeps every configured server out of the run, which removes both the
 *       npx start-up latency and the tool-schema tokens from stages that need no live data.</li>
 *   <li>{@code timeoutMinutes} — per-stage rather than one global ceiling.</li>
 * </ul>
 *
 * <p>The CLI is invoked with {@code --output-format json} so each run reports its own token usage
 * and tool-call counts; those are returned in {@link GeminiRunResult} for per-stage accounting.
 * That same flag is why the console cannot show progress inside a stage: nothing reaches stdout
 * until the stage ends, so the only live signal is the stderr stream, which
 * {@link GeminiRunContext#logFile()} redirects to a file the console can tail.
 *
 * <p>Authentication is pinned to Vertex AI with Application Default Credentials rather than
 * inherited from the shell — see {@link #pinVertexAuth}.
 */
public class GeminiCliService {
    /**
     * Sentinel server name passed as the MCP allowlist when a stage needs no MCP server at all.
     * The CLI only blocks servers when the allowlist is non-empty, so a name that matches nothing
     * is how "allow none" is expressed.
     */
    public static final String MCP_DISABLED = "__disabled__";
    private static final String MCP_TOOL_NAME_PREFIX = "mcp_";
    private static final int ERROR_OUTPUT_LIMIT = 4000;
    private static final int VERSION_TIMEOUT_SECONDS = 15;

    private static final String GOOGLE_API_KEY = "GOOGLE_API_KEY";
    private static final String GEMINI_API_KEY = "GEMINI_API_KEY";
    private static final String USE_VERTEX_AI = "GOOGLE_GENAI_USE_VERTEXAI";
    private static final String CLOUD_PROJECT = "GOOGLE_CLOUD_PROJECT";
    private static final String CLOUD_PROJECT_ID = "GOOGLE_CLOUD_PROJECT_ID";
    private static final String CLOUD_LOCATION = "GOOGLE_CLOUD_LOCATION";

    private final Logger logger = LoggerFactory.getLogger(GeminiCliService.class);
    private final String cloudProject;
    private final String cloudLocation;

    /**
     * @param cloudProject  {@code app.gemini.cloud.project} — the Google Cloud project hosting Vertex AI
     * @param cloudLocation {@code app.gemini.cloud.location} — the Vertex AI region, e.g. {@code global}
     */
    public GeminiCliService(String cloudProject, String cloudLocation) {
        if (Strings.isBlank(cloudProject)) {
            throw new IllegalArgumentException("app.gemini.cloud.project must be configured with the Google Cloud project hosting Vertex AI");
        }
        if (Strings.isBlank(cloudLocation)) {
            throw new IllegalArgumentException("app.gemini.cloud.location must be configured, e.g. global");
        }
        this.cloudProject = cloudProject;
        this.cloudLocation = cloudLocation;
    }

    public String cloudProject() {
        return cloudProject;
    }

    public String cloudLocation() {
        return cloudLocation;
    }

    /** @return the installed CLI version, or null when the CLI is not on PATH */
    public String version() {
        return ProcessRunner.run(List.of("gemini", "--version"), null, VERSION_TIMEOUT_SECONDS);
    }

    public GeminiRunResult run(GeminiRunRequest request) throws IOException, InterruptedException {
        return run(request, GeminiRunContext.none());
    }

    public GeminiRunResult run(GeminiRunRequest request, GeminiRunContext context) throws IOException, InterruptedException {
        Path workDir = Files.createTempDirectory("gemini-workdir-");
        Path promptFile = workDir.resolve("prompt.md");
        boolean keepLog = !Strings.isBlank(context.logFile());
        Path stderrFile = keepLog ? Path.of(context.logFile()) : workDir.resolve("stderr.log");
        String sessionId = UUID.randomUUID().toString();
        try {
            Files.writeString(promptFile, request.prompt());
            logger.info("Running Gemini CLI: model={}, promptChars={}, includeDirs={}, mcpServers={}, timeoutMinutes={}",
                request.model(), request.prompt().length(), request.includeDirs(), request.allowedMcpServers(), request.timeoutMinutes());

            ProcessBuilder pb = new ProcessBuilder(command(request, promptFile, sessionId));
            pb.directory(workDir.toFile());
            pb.redirectError(stderrFile.toFile());
            pinVertexAuth(pb.environment());

            long startMs = System.currentTimeMillis();
            Process process = pb.start();
            if (context.processListener() != null) context.processListener().accept(process);
            byte[] stdoutBytes = process.getInputStream().readAllBytes();
            boolean finished = process.waitFor(request.timeoutMinutes(), TimeUnit.MINUTES);
            if (!finished) {
                process.destroyForcibly();
                throw new IllegalStateException("Gemini CLI timed out after " + request.timeoutMinutes() + " minutes");
            }
            long durationMs = System.currentTimeMillis() - startMs;

            String stdout = new String(stdoutBytes, StandardCharsets.UTF_8).trim();
            int exitCode = process.exitValue();
            if (exitCode != 0) {
                throw new IllegalStateException("Gemini CLI failed (exit " + exitCode + "): "
                    + errorMessage(stdout, Files.readString(stderrFile, StandardCharsets.UTF_8)));
            }

            GeminiRunResult result = parseResult(request.model(), stdout, durationMs, sessionId);
            logger.info("Gemini CLI completed: model={}, durationMs={}, inputTokens={}, outputTokens={}, cachedTokens={}, apiRequests={}, toolCalls={}, mcpToolCalls={}, responseChars={}",
                result.model(), result.durationMs(), result.inputTokens(), result.outputTokens(), result.cachedTokens(),
                result.apiRequests(), result.toolCalls(), result.mcpToolCalls(), result.response().length());
            return result;
        } finally {
            deleteQuietly(promptFile);
            if (!keepLog) deleteQuietly(stderrFile);
            deleteQuietly(workDir);
        }
    }

    /**
     * Pins the subprocess onto Vertex AI with Application Default Credentials.
     *
     * <p>The CLI resolves credentials with the API key winning over ADC: when {@code GOOGLE_API_KEY}
     * or {@code GEMINI_API_KEY} is set it switches to Vertex express mode, sends the key as
     * {@code x-goog-api-key}, and never builds a {@code GoogleAuth} client. The request is then
     * billed to — and authorized against — whatever project owns that key, not the project this
     * service is configured for, which surfaces as an opaque {@code 403 API_KEY_SERVICE_BLOCKED}.
     * A key exported from a developer's shell profile is inherited by this process and silently
     * takes over the run, so both variables are removed from the child environment.
     *
     * <p>The project and location come from configuration rather than the inherited environment, so
     * a run always targets the configured project. Both are set explicitly because the CLI only
     * takes the Vertex path when it has <em>both</em>; with the location missing it falls through to
     * the public Gemini API with no credentials at all. {@code GOOGLE_CLOUD_PROJECT_ID} is cleared
     * so an inherited alias cannot disagree with the configured project.
     *
     * @param env the child process environment, pre-populated by {@link ProcessBuilder} with a copy
     *            of this process's environment
     */
    void pinVertexAuth(Map<String, String> env) {
        String removedKey = env.remove(GOOGLE_API_KEY) != null ? GOOGLE_API_KEY : null;
        if (env.remove(GEMINI_API_KEY) != null) {
            removedKey = removedKey == null ? GEMINI_API_KEY : removedKey + " and " + GEMINI_API_KEY;
        }
        if (removedKey != null) {
            logger.info("Ignoring {} from the environment; the local review authenticates to Vertex AI with Application Default Credentials", removedKey);
        }

        env.remove(CLOUD_PROJECT_ID);
        env.put(CLOUD_PROJECT, cloudProject);
        env.put(CLOUD_LOCATION, cloudLocation);
        env.put(USE_VERTEX_AI, "true");
        logger.info("Gemini CLI auth: Vertex AI via Application Default Credentials, project={}, location={}", cloudProject, cloudLocation);
    }

    private List<String> command(GeminiRunRequest request, Path promptFile, String sessionId) {
        List<String> baseArgs = List.of(
            "gemini", "--model", request.model(), "--yolo", "--skip-trust",
            "--output-format", "json",
            "--session-id", sessionId);
        List<String> dirArgs = request.includeDirs().stream()
            .flatMap(dir -> Stream.of("--include-directories", dir))
            .toList();
        List<String> mcpArgs = request.allowedMcpServers().stream()
            .flatMap(server -> Stream.of("--allowed-mcp-server-names", server))
            .toList();
        List<String> promptArg = List.of("-p", "@" + promptFile.toAbsolutePath());

        List<String> command = new ArrayList<>(baseArgs.size() + dirArgs.size() + mcpArgs.size() + promptArg.size());
        command.addAll(baseArgs);
        command.addAll(dirArgs);
        command.addAll(mcpArgs);
        command.addAll(promptArg);
        return command;
    }

    /**
     * Parses the {@code --output-format json} envelope. Falls back to treating stdout as plain text
     * so a CLI version that stops emitting the envelope degrades to the previous behaviour instead
     * of failing the stage.
     */
    private GeminiRunResult parseResult(String model, String stdout, long durationMs, String sessionId) {
        GeminiCliOutput output = parseOutput(stdout);
        if (output == null) {
            logger.warn("Gemini CLI did not return a JSON envelope; falling back to raw stdout and zero token stats");
            return new GeminiRunResult(model, stdout, durationMs, new TokenUsage(0, 0, 0), new CallCounts(0, 0, 0), sessionId, null);
        }
        TokenUsage tokens = tokenUsage(output);
        CallCounts calls = callCounts(output);
        String response = output.response == null ? "" : output.response.trim();
        String resolvedSessionId = output.sessionId == null ? sessionId : output.sessionId;
        String rawStats = output.stats == null ? null : JSON.toJSON(output.stats);
        return new GeminiRunResult(model, response, durationMs, tokens, calls, resolvedSessionId, rawStats);
    }

    private TokenUsage tokenUsage(GeminiCliOutput output) {
        long inputTokens = 0;
        long outputTokens = 0;
        long cachedTokens = 0;
        if (output.stats != null && output.stats.models != null) {
            for (Map.Entry<String, ModelStats> entry : output.stats.models.entrySet()) {
                ModelStats stats = entry.getValue();
                if (stats == null || stats.tokens == null) continue;
                inputTokens += inputTokens(stats.tokens);
                outputTokens += outputTokens(stats.tokens);
                cachedTokens += value(stats.tokens.cached);
            }
        }
        return new TokenUsage(inputTokens, outputTokens, cachedTokens);
    }

    private CallCounts callCounts(GeminiCliOutput output) {
        int apiRequests = 0;
        if (output.stats != null && output.stats.models != null) {
            for (Map.Entry<String, ModelStats> entry : output.stats.models.entrySet()) {
                ModelStats stats = entry.getValue();
                if (stats != null && stats.api != null) apiRequests += (int) value(stats.api.totalRequests);
            }
        }
        int toolCalls = 0;
        int mcpToolCalls = 0;
        if (output.stats != null && output.stats.tools != null) {
            toolCalls = (int) value(output.stats.tools.totalCalls);
            mcpToolCalls = mcpToolCalls(output.stats.tools.byName);
        }
        return new CallCounts(apiRequests, toolCalls, mcpToolCalls);
    }

    /** Prefers the explicit prompt count, and derives it from the total when the CLI omits it. */
    private long inputTokens(TokenStats tokens) {
        long prompt = value(tokens.prompt);
        if (prompt > 0) return prompt;
        long derived = value(tokens.total) - outputTokens(tokens);
        return Math.max(derived, value(tokens.input));
    }

    /** Thinking tokens are billed as output, so they belong in the output count. */
    private long outputTokens(TokenStats tokens) {
        return value(tokens.candidates) + value(tokens.thoughts);
    }

    private int mcpToolCalls(Map<String, ToolCallStats> byName) {
        if (byName == null) return 0;
        int count = 0;
        for (Map.Entry<String, ToolCallStats> entry : byName.entrySet()) {
            if (entry.getKey() != null && entry.getKey().startsWith(MCP_TOOL_NAME_PREFIX) && entry.getValue() != null) {
                count += (int) value(entry.getValue().count);
            }
        }
        return count;
    }

    private GeminiCliOutput parseOutput(String stdout) {
        int start = stdout.indexOf('{');
        int end = stdout.lastIndexOf('}');
        if (start < 0 || end <= start) return null;
        try {
            return JSON.fromJSON(GeminiCliOutput.class, stdout.substring(start, end + 1));
        } catch (RuntimeException e) {
            logger.warn("Could not parse Gemini CLI JSON output: {}", e.getMessage());
            return null;
        }
    }

    /** Prefers the structured error from the JSON envelope, falling back to the tail of stderr. */
    private String errorMessage(String stdout, String stderr) {
        GeminiCliOutput output = parseOutput(stdout);
        if (output != null && output.error != null && output.error.message != null) {
            return truncate(output.error.message);
        }
        return truncate(stderr.trim());
    }

    private String truncate(String text) {
        if (text.length() <= ERROR_OUTPUT_LIMIT) return text;
        return text.substring(0, ERROR_OUTPUT_LIMIT) + "... (truncated)";
    }

    private long value(Long raw) {
        return raw == null ? 0 : raw;
    }

    private void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // best-effort cleanup
        }
    }

    /**
     * @param prompt            full prompt text, @file references included
     * @param model             Gemini CLI model id, see {@link GeminiModels}
     * @param includeDirs       directories the prompt may read via @file
     * @param allowedMcpServers MCP servers this stage may use, or {@link #MCP_DISABLED} for none
     * @param timeoutMinutes    wall-clock ceiling for this stage
     */
    public record GeminiRunRequest(String prompt,
                                   String model,
                                   List<String> includeDirs,
                                   List<String> allowedMcpServers,
                                   int timeoutMinutes) {
    }

    /**
     * Side channels around one invocation.
     *
     * @param logFile         where to keep the CLI's stderr so the console can tail it; null sends it
     *                        to a temporary file that is deleted when the stage ends
     * @param processListener receives the subprocess as soon as it starts, so a cancel can destroy it
     */
    public record GeminiRunContext(String logFile, Consumer<Process> processListener) {
        public static GeminiRunContext none() {
            return new GeminiRunContext(null, null);
        }
    }

    /** @param cachedTokens part of inputTokens that was served from cache, and priced differently */
    public record TokenUsage(long inputTokens, long outputTokens, long cachedTokens) {
    }

    public record CallCounts(int apiRequests, int toolCalls, int mcpToolCalls) {
    }

    /** @param rawStatsJson the CLI's own {@code stats} object, kept verbatim for provenance */
    public record GeminiRunResult(String model,
                                  String response,
                                  long durationMs,
                                  TokenUsage tokens,
                                  CallCounts calls,
                                  String sessionId,
                                  String rawStatsJson) {
        public long inputTokens() {
            return tokens.inputTokens();
        }

        public long outputTokens() {
            return tokens.outputTokens();
        }

        public long cachedTokens() {
            return tokens.cachedTokens();
        }

        public int apiRequests() {
            return calls.apiRequests();
        }

        public int toolCalls() {
            return calls.toolCalls();
        }

        public int mcpToolCalls() {
            return calls.mcpToolCalls();
        }

        public long totalTokens() {
            return tokens.inputTokens() + tokens.outputTokens();
        }
    }
}
