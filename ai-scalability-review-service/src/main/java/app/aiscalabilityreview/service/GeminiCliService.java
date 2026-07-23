package app.aiscalabilityreview.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * Runs the local Gemini CLI as a subprocess with a given prompt.
 * The prompt is written to a temporary file and passed via "-p @promptFile" so the Gemini CLI
 * expands all @file references (source repos, infra configs) before sending to the model.
 * Runs from a neutral temp directory to prevent Gemini from treating the current project
 * as its workspace. Each invocation uses a unique session ID to avoid inheriting prior context.
 * Callers must pass includeDirs to whitelist any paths the prompt references via @file.
 * MCP servers (Datadog, Azure, MySQL) must be pre-configured in the local Gemini CLI setup.
 */
public class GeminiCliService {
    private static final int TIMEOUT_MINUTES = 30;
    private final Logger logger = LoggerFactory.getLogger(GeminiCliService.class);

    public String run(String prompt, String model, List<String> includeDirs) throws IOException, InterruptedException {
        Path workDir = Files.createTempDirectory("gemini-workdir-");
        Path promptFile = workDir.resolve("prompt.md");
        Path stderrFile = workDir.resolve("stderr.log");
        try {
            Files.writeString(promptFile, prompt);
            logger.info("Running Gemini CLI: model={}, promptLength={}, includeDirs={}", model, prompt.length(), includeDirs);

            List<String> baseArgs = List.of(
                "gemini", "--model", model, "--yolo", "--skip-trust",
                "--output-format", "text",
                "--session-id", UUID.randomUUID().toString());
            List<String> dirArgs = includeDirs.stream()
                .flatMap(dir -> Stream.of("--include-directories", dir))
                .toList();
            List<String> promptArg = List.of("-p", "@" + promptFile.toAbsolutePath());
            List<String> command = new ArrayList<>(baseArgs.size() + dirArgs.size() + promptArg.size());
            command.addAll(baseArgs);
            command.addAll(dirArgs);
            command.addAll(promptArg);

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(workDir.toFile());
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
            deleteQuietly(workDir);
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
