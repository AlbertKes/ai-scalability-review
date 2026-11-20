package app.aiscalabilityreview.service;

import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** Reads a Gemini CLI process's stdout under a deadline. */
final class GeminiProcessOutput {
    private static final int DRAIN_SECONDS = 30;

    /**
     * Reads the CLI's stdout while enforcing the stage timeout.
     *
     * <p>The stream used to be read to its end before the timeout was ever consulted. A blocking
     * read returns only when the process closes stdout, so the timeout could never fire: a stage
     * with a 15-minute limit ran 21.8 minutes and was reported as a success. Reading on a separate
     * thread leaves this one free to wait with a deadline, and to kill the process tree when it
     * passes.
     *
     * @throws IllegalStateException when the process is still running at the deadline
     */
    static byte[] await(Process process, long timeout, TimeUnit unit) throws IOException, InterruptedException {
        CompletableFuture<byte[]> stdout = new CompletableFuture<>();
        Thread reader = new Thread(() -> {
            try {
                stdout.complete(process.getInputStream().readAllBytes());
            } catch (IOException e) {
                stdout.completeExceptionally(e);
            }
        }, "gemini-stdout");
        reader.setDaemon(true);
        reader.start();
        if (!process.waitFor(timeout, unit)) {
            ProcessRunner.destroyTree(process);
            reader.interrupt();
            throw new IllegalStateException("Gemini CLI timed out after " + timeout + " " + unit.name().toLowerCase(Locale.ROOT));
        }
        try {
            return stdout.get(DRAIN_SECONDS, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            throw new IOException("Could not read the Gemini CLI output", e);
        } catch (TimeoutException e) {
            throw new IOException("The Gemini CLI exited but its output stream did not close", e);
        }
    }

    private GeminiProcessOutput() {
    }
}
