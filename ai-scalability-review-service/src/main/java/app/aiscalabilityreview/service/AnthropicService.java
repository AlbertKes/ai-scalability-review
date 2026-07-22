package app.aiscalabilityreview.service;

import app.aiscalabilityreview.domain.embedded.AIModel;
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
import java.util.List;

public class AnthropicService {
    private static final String ANTHROPIC_API_URL = "https://api.anthropic.com/v1/messages";
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    private final Logger logger = LoggerFactory.getLogger(AnthropicService.class);

    @Inject
    HTTPClient httpClient;

    /**
     * Send a prompt to the Anthropic Claude API and return the generated content.
     *
     * @param aiModel      AI model
     * @param systemPrompt system prompt text
     * @param userPrompt   user message text
     * @param maxTokens    maximum tokens to generate
     * @param temperature  sampling temperature (0.0 – 1.0)
     * @return GeneratedContent with response text and token usage
     */
    public GeneratedContent generate(AIModel aiModel, String systemPrompt, String userPrompt, int maxTokens, double temperature) {
        String apiKey = System.getenv("ANTHROPIC_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("ANTHROPIC_API_KEY environment variable is not set");
        }

        AnthropicRequest requestBody = new AnthropicRequest();
        requestBody.model = aiModelString(aiModel);
        requestBody.maxTokens = maxTokens;
        requestBody.temperature = temperature;
        requestBody.system = systemPrompt;

        AnthropicMessage userMessage = new AnthropicMessage();
        userMessage.role = "user";
        AnthropicContent content = new AnthropicContent();
        content.type = "text";
        content.text = userPrompt;
        userMessage.content = List.of(content);
        requestBody.messages = List.of(userMessage);

        byte[] bodyBytes = JSON.toJSON(requestBody).getBytes(StandardCharsets.UTF_8);

        HTTPRequest httpRequest = new HTTPRequest(HTTPMethod.POST, ANTHROPIC_API_URL);
        httpRequest.headers.put("x-api-key", apiKey);
        httpRequest.headers.put("anthropic-version", ANTHROPIC_VERSION);
        httpRequest.headers.put("content-type", "application/json");
        httpRequest.body = bodyBytes;

        long startMs = System.currentTimeMillis();
        HTTPResponse response = httpClient.execute(httpRequest);
        long durationMs = System.currentTimeMillis() - startMs;

        if (response.statusCode != 200) {
            String errorBody = response.body == null ? "(empty)" : new String(response.body, StandardCharsets.UTF_8);
            logger.error("Anthropic API error: status={}, body={}", response.statusCode, errorBody);
            throw new RuntimeException("Anthropic API returned status " + response.statusCode + ": " + errorBody);
        }

        String responseJson = new String(response.body, StandardCharsets.UTF_8);
        AnthropicResponse anthropicResponse = JSON.fromJSON(AnthropicResponse.class, responseJson);

        if (anthropicResponse.content == null || anthropicResponse.content.isEmpty()) {
            throw new RuntimeException("Anthropic API returned empty content");
        }

        String text = anthropicResponse.content.get(0).text;
        int inputTokens = anthropicResponse.usage != null ? anthropicResponse.usage.inputTokens : 0;
        int outputTokens = anthropicResponse.usage != null ? anthropicResponse.usage.outputTokens : 0;

        // Rough cost estimate: claude-sonnet pricing ~$3/$15 per 1M tokens input/output
        double estimatedCostUsd = (inputTokens * 3.0 + outputTokens * 15.0) / 1_000_000.0;

        logger.info("Anthropic API call completed: model={}, inputTokens={}, outputTokens={}, durationMs={}, estimatedCostUsd={}",
            aiModelString(aiModel), inputTokens, outputTokens, durationMs, estimatedCostUsd);

        return new GeneratedContent(text, inputTokens, outputTokens, estimatedCostUsd);
    }

    private String aiModelString(AIModel aiModel) {
        if (aiModel == null) return null;
        return switch (aiModel) {
            case CLAUDE_SONNET_4_6 -> "claude-sonnet-4-6";
            case GEMINI_2_5_PRO -> "gemini-2-5-pro";
        };
    }

    public static class AnthropicRequest {
        @Property(name = "model")
        public String model;

        @Property(name = "max_tokens")
        public Integer maxTokens;

        @Property(name = "temperature")
        public Double temperature;

        @Property(name = "system")
        public String system;

        @Property(name = "messages")
        public List<AnthropicMessage> messages;
    }

    public static class AnthropicMessage {
        @Property(name = "role")
        public String role;

        @Property(name = "content")
        public List<AnthropicContent> content;
    }

    public static class AnthropicContent {
        @Property(name = "type")
        public String type;

        @Property(name = "text")
        public String text;
    }

    public static class AnthropicResponse {
        @Property(name = "id")
        public String id;

        @Property(name = "type")
        public String type;

        @Property(name = "role")
        public String role;

        @Property(name = "content")
        public List<AnthropicContent> content;

        @Property(name = "model")
        public String model;

        @Property(name = "stop_reason")
        public String stopReason;

        @Property(name = "usage")
        public AnthropicUsage usage;
    }

    public static class AnthropicUsage {
        @Property(name = "input_tokens")
        public Integer inputTokens;

        @Property(name = "output_tokens")
        public Integer outputTokens;
    }

    public record GeneratedContent(String text, int inputTokens, int outputTokens, double estimatedCostUsd) {
    }
}