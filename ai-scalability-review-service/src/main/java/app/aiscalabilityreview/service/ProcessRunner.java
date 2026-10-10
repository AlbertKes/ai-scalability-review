package app.aiscalabilityreview.service;

import core.framework.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Runs a short external command and returns its trimmed stdout.
 *
 * <p>Used by the preflight checks and the reproducibility fingerprint — {@code git rev-parse},
 * {@code gemini --version} — where the answer is one line and a failure simply means "unknown"
 * rather than an error worth propagating.
 */
public final class ProcessRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessRunner.class);
    private static final int OUTPUT_LIMIT = 2000;

    /** @return the command's stdout, or null when it is missing, fails or times out */
    public static String run(List<String> command, String workingDir, int timeoutSeconds) {
        ProcessBuilder builder = new ProcessBuilder(command);
        if (!Strings.isBlank(workingDir)) builder.directory(new File(workingDir));
        builder.redirectErrorStream(true);
        Process process = null;
        try {
            process = builder.start();
            String output = readOutput(process);
            if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return null;
            }
            if (process.exitValue() != 0) return null;
            return output.trim();
        } catch (IOException e) {
            LOGGER.debug("Command not available: {}", command.get(0));
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();   // start() already returned, so there is always a process here
            return null;
        }
    }

    private static String readOutput(Process process) throws IOException {
        try (InputStream stream = process.getInputStream()) {
            return new String(stream.readNBytes(OUTPUT_LIMIT), StandardCharsets.UTF_8);
        }
    }

    private ProcessRunner() {
    }
}
