package app.aiscalabilityreview.service;

import core.framework.util.Strings;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * How much the model may think before it answers, for one Gemini CLI session.
 *
 * <p>Gemini 3 models think at level HIGH unless told otherwise, and thinking tokens are output
 * tokens: they are generated serially at roughly 100–150 per second. A measured run of wonder-cart-service
 * spent 71K output tokens in synthesis, of which the report itself was about 8K, so most of the stage's
 * 516 s was the model thinking. The stages here mostly transcribe, compare and apply written rules, which
 * do not need that depth, so each session is given a level.
 *
 * <p>The CLI reads {@code modelConfigs.customOverrides} from {@code .gemini/settings.json} in its working
 * directory, so the level is written there for the one session rather than into the developer's own
 * settings. (A system settings file would also work, but the CLI refuses it unless root owns it.)
 * Measured on {@code gemini-3.8-flash}: LOW produced 0 thinking tokens and MEDIUM about as many as the default.
 */
public enum GeminiThinking {
    LOW("LOW"),
    MEDIUM("MEDIUM");

    private final String level;

    GeminiThinking(String level) {
        this.level = level;
    }

    /** Writes the override into {@code workDir/.gemini/settings.json}; the CLI picks it up from its cwd. */
    public void writeSettings(Path workDir, String model) throws IOException {
        Path dir = workDir.resolve(".gemini");
        Files.createDirectories(dir);
        Files.writeString(dir.resolve("settings.json"), Strings.format("""
            {"modelConfigs":{"customOverrides":[{"match":{"model":"{}"},"modelConfig":{"generateContentConfig":{"thinkingConfig":{"thinkingLevel":"{}"}}}}]}}
            """, model, level), StandardCharsets.UTF_8);
    }
}
