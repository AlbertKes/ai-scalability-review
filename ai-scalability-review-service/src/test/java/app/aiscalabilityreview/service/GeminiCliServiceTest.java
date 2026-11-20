package app.aiscalabilityreview.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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
    void narrowsTheDatadogServerToTheReviewToolsAndKeepsItReadOnly() {
        env.put("DD_ALLOW_WRITE", "true");

        service.scopeDatadogMcp(env);

        assertThat(env)
            .containsEntry("DD_TOOLS", "metrics,monitors")
            .containsEntry("DD_ALLOW_WRITE", "false");
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

    /** The timeout used to be read only after stdout closed, so it never fired on a process that was still running. */
    @Test
    void killsAProcessThatOutlivesTheStageTimeout() throws IOException {
        Process process = new ProcessBuilder("sleep", "30").start();
        long started = System.nanoTime();

        assertThatThrownBy(() -> GeminiProcessOutput.await(process, 300, TimeUnit.MILLISECONDS))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("timed out");
        assertThat(TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - started)).isLessThan(10);
        assertThat(process.isAlive()).isFalse();
    }

    @Test
    void returnsTheOutputOfAProcessThatFinishesInTime() throws Exception {
        Process process = new ProcessBuilder("echo", "hello").start();

        assertThat(new String(GeminiProcessOutput.await(process, 10, TimeUnit.SECONDS), StandardCharsets.UTF_8)).isEqualTo("hello\n");
    }

    @Test
    void stageTimeoutIsThirtyMinutes() {
        assertThat(GeminiCliService.STAGE_TIMEOUT_MINUTES).isEqualTo(30);
    }
}
