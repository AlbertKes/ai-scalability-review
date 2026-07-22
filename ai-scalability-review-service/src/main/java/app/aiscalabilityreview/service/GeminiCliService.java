package app.aiscalabilityreview.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/**
 * Runs the local Gemini CLI as a subprocess with a given prompt.
 * The prompt is written to a temporary file and passed via "-p @promptFile" so the Gemini CLI
 * expands all @file references (source repos, infra configs) before sending to the model.
 * MCP servers (Datadog, Azure, MySQL) must be pre-configured in the local Gemini CLI setup.
 */
public class GeminiCliService {
    private static final String DEFAULT_MODEL = "gemini-2.5-pro";
    private static final int TIMEOUT_MINUTES = 30;
    private final Logger logger = LoggerFactory.getLogger(GeminiCliService.class);

    public String run(String prompt) throws IOException, InterruptedException {
        return run(prompt, DEFAULT_MODEL);
    }

    public String run(String prompt, String model) throws IOException, InterruptedException {
        Path promptFile = Files.createTempFile("gemini-prompt-", ".md");
        Path stderrFile = Files.createTempFile("gemini-stderr-", ".log");
        try {
            Files.writeString(promptFile, prompt);
            logger.info("Running Gemini CLI: model={}, promptLength={}", model, prompt.length());

            // Pass prompt via "-p @file" so the CLI expands all @file references in the prompt.
            // stdin redirect does not trigger @file expansion in non-interactive mode.
            ProcessBuilder pb = new ProcessBuilder(
                "gemini", "--model", model, "--yolo", "-p", "@" + promptFile.toAbsolutePath());
            pb.redirectError(stderrFile.toFile());

            Process process = pb.start();
            byte[] stdoutBytes = process.getInputStream().readAllBytes();

            boolean finished = process.waitFor(TIMEOUT_MINUTES, TimeUnit.MINUTES);
            if (!finished) {
                process.destroyForcibly();
                throw new RuntimeException("Gemini CLI timed out after " + TIMEOUT_MINUTES + " minutes");
            }

            int exitCode = process.exitValue();
            String output = new String(stdoutBytes, StandardCharsets.UTF_8).trim();
            if (exitCode != 0) {
                String errorOutput = Files.readString(stderrFile, StandardCharsets.UTF_8).trim();
                logger.error("Gemini CLI failed: exitCode={}, stderr={}", exitCode, errorOutput);
                throw new RuntimeException("Gemini CLI failed (exit " + exitCode + "): " + errorOutput);
            }

            logger.info("Gemini CLI completed: outputLength={}", output.length());
            return output;
        } finally {
            deleteQuietly(promptFile);
            deleteQuietly(stderrFile);
        }
    }

    private void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // best-effort cleanup
        }
    }
}
