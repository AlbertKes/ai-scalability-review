package app.aiscalabilityreview.service;

import core.framework.api.json.Property;

import java.util.Map;

/**
 * The {@code --output-format json} envelope of the Gemini CLI.
 *
 * <p>{@code stats} is the raw {@code SessionMetrics} the CLI reports for the session, and is the
 * only source of per-stage token and tool-call numbers — the run accounting is built on it, so the
 * shape is mirrored field for field rather than summarised.
 */
public class GeminiCliOutput {
    @Property(name = "session_id")
    public String sessionId;

    @Property(name = "response")
    public String response;

    @Property(name = "stats")
    public CliStats stats;

    @Property(name = "error")
    public CliError error;

    public static class CliError {
        @Property(name = "type")
        public String type;

        @Property(name = "message")
        public String message;

        @Property(name = "code")
        public Integer code;
    }

    public static class CliStats {
        @Property(name = "models")
        public Map<String, ModelStats> models;

        @Property(name = "tools")
        public ToolStats tools;
    }

    public static class ModelStats {
        @Property(name = "api")
        public ApiStats api;

        @Property(name = "tokens")
        public TokenStats tokens;
    }

    public static class ApiStats {
        @Property(name = "totalRequests")
        public Long totalRequests;

        @Property(name = "totalErrors")
        public Long totalErrors;

        @Property(name = "totalLatencyMs")
        public Long totalLatencyMs;
    }

    public static class TokenStats {
        @Property(name = "input")
        public Long input;

        @Property(name = "prompt")
        public Long prompt;

        @Property(name = "candidates")
        public Long candidates;

        @Property(name = "total")
        public Long total;

        @Property(name = "cached")
        public Long cached;

        @Property(name = "thoughts")
        public Long thoughts;

        @Property(name = "tool")
        public Long tool;
    }

    public static class ToolStats {
        @Property(name = "totalCalls")
        public Long totalCalls;

        @Property(name = "totalSuccess")
        public Long totalSuccess;

        @Property(name = "totalFail")
        public Long totalFail;

        @Property(name = "totalDurationMs")
        public Long totalDurationMs;

        @Property(name = "byName")
        public Map<String, ToolCallStats> byName;
    }

    public static class ToolCallStats {
        @Property(name = "count")
        public Long count;

        @Property(name = "success")
        public Long success;

        @Property(name = "fail")
        public Long fail;

        @Property(name = "durationMs")
        public Long durationMs;
    }
}
