package app.aiscalabilityreview.domain;

import core.framework.mongo.Collection;
import core.framework.mongo.Field;
import core.framework.mongo.Id;

import java.time.ZonedDateTime;

@Collection(name = "model_configs")
public class ModelConfig {
    @Id
    public String modelId;

    @Field(name = "provider")
    public String provider;  // ANTHROPIC | OPENAI | GEMINI

    @Field(name = "api_endpoint")
    public String apiEndpoint;

    @Field(name = "api_key_secret_ref")
    public String apiKeySecretRef;

    @Field(name = "max_tokens")
    public Integer maxTokens;

    @Field(name = "temperature")
    public Double temperature;

    @Field(name = "enabled")
    public Boolean enabled;

    @Field(name = "rate_limit_rpm")
    public Integer rateLimitRpm;

    @Field(name = "cost_per_input_token_usd")
    public Double costPerInputTokenUsd;

    @Field(name = "cost_per_output_token_usd")
    public Double costPerOutputTokenUsd;

    @Field(name = "updated_at")
    public ZonedDateTime updatedAt;
}