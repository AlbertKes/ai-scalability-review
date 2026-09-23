package app.aiscalabilityreview.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GeminiCliServiceTest {
    private GeminiCliService service;
    private Map<String, String> env;

    @BeforeEach
    void createService() {
        service = new GeminiCliService("wonder-sandbox", "global");
        env = new HashMap<>();
    }

    @Test
    void dropsAnInheritedApiKeySoTheRunUsesApplicationDefaultCredentials() {
        env.put("GOOGLE_API_KEY", "AQ.inherited-from-the-developer-shell");
        env.put("GEMINI_API_KEY", "AI.another-one");

        service.pinVertexAuth(env);

        assertThat(env).doesNotContainKey("GOOGLE_API_KEY").doesNotContainKey("GEMINI_API_KEY");
    }

    @Test
    void pinsVertexAiOntoTheConfiguredProjectAndLocation() {
        service.pinVertexAuth(env);

        assertThat(env)
            .containsEntry("GOOGLE_GENAI_USE_VERTEXAI", "true")
            .containsEntry("GOOGLE_CLOUD_PROJECT", "wonder-sandbox")
            .containsEntry("GOOGLE_CLOUD_LOCATION", "global");
    }

    @Test
    void configurationWinsOverTheInheritedEnvironment() {
        env.put("GOOGLE_CLOUD_PROJECT", "some-other-project");
        env.put("GOOGLE_CLOUD_PROJECT_ID", "a-stale-alias");
        env.put("GOOGLE_CLOUD_LOCATION", "us-central1");

        service.pinVertexAuth(env);

        assertThat(env)
            .containsEntry("GOOGLE_CLOUD_PROJECT", "wonder-sandbox")
            .containsEntry("GOOGLE_CLOUD_LOCATION", "global")
            .doesNotContainKey("GOOGLE_CLOUD_PROJECT_ID");
    }

    @Test
    void refusesToStartWithoutAConfiguredProject() {
        assertThatThrownBy(() -> new GeminiCliService(" ", "global"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("app.gemini.cloud.project");
        assertThatThrownBy(() -> new GeminiCliService(null, "global"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void refusesToStartWithoutAConfiguredLocation() {
        assertThatThrownBy(() -> new GeminiCliService("wonder-sandbox", null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("app.gemini.cloud.location");
    }
}
