package app.aiscalabilityreview.service;

import core.framework.util.Strings;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Reads the commit and the working-tree state of a local repository.
 *
 * <p>Both go into the run's fingerprint: without them, two runs of the same service cannot be told
 * apart as "the service changed" versus "the pipeline changed", and a dirty infra tree silently
 * changes what stages 2 and 5 read.
 */
public class GitRepoInspector {
    private static final int TIMEOUT_SECONDS = 10;

    public boolean isGitRepo(String repoPath) {
        if (Strings.isBlank(repoPath)) return false;
        return Files.isDirectory(Path.of(repoPath, ".git")) || ProcessRunner.run(
            List.of("git", "rev-parse", "--is-inside-work-tree"), repoPath, TIMEOUT_SECONDS) != null;
    }

    /** @return the short commit SHA, or null when the path is not a readable repository */
    public String sha(String repoPath) {
        if (Strings.isBlank(repoPath)) return null;
        return ProcessRunner.run(List.of("git", "rev-parse", "--short", "HEAD"), repoPath, TIMEOUT_SECONDS);
    }

    /** @return true when the working tree has uncommitted changes, false when clean or unknown */
    public boolean isDirty(String repoPath) {
        if (Strings.isBlank(repoPath)) return false;
        String status = ProcessRunner.run(List.of("git", "status", "--porcelain"), repoPath, TIMEOUT_SECONDS);
        return status != null && !status.isEmpty();
    }
}
