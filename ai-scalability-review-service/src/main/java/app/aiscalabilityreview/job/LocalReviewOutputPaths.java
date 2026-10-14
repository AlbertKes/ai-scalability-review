package app.aiscalabilityreview.job;

import core.framework.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Resolves where one run writes its files.
 *
 * <p>Every run gets its own directory, {@code <base>/<service>/<yyyyMMdd-HHmmss>-<jobId prefix>}.
 * The pipeline used to write all runs of a service into {@code <base>/<service>}, where
 * {@code code-context.md}, {@code metrics.md}, {@code infra-context.md},
 * {@code report-snapshot.json} and {@code run-stats.md} have fixed names — so the second run of a
 * service destroyed the first one's evidence, and even the date-stamped report was overwritten by a
 * second run on the same day. Managing multiple runs is impossible on top of that layout, so the
 * layout changed first.
 */
public final class LocalReviewOutputPaths {
    public static final String CODE_CONTEXT = "code-context.md";
    public static final String INFRA_CONTEXT = "infra-context.md";
    public static final String METRICS = "metrics.md";
    public static final String MYSQL_TABLES = "mysql-tables.md";
    public static final String REPORT_SNAPSHOT = "report-snapshot.json";
    /** One per validation session; the merged `<date>-validation.md` is assembled from the three. */
    public static final String VALIDATION_STATIC = "validation-static.md";
    public static final String VALIDATION_DATA = "validation-data.md";
    public static final String VALIDATION_ADVICE = "validation-advice.md";
    public static final String RUN_STATS = "run-stats.md";
    public static final String RUN_MANIFEST = "run.json";
    /** Points at the newest run of a service, so the existing command-line workflow still works. */
    public static final String LATEST_LINK = "latest";

    private static final DateTimeFormatter RUN_DIR_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final Logger LOGGER = LoggerFactory.getLogger(LocalReviewOutputPaths.class);
    private static final int JOB_ID_PREFIX_LENGTH = 8;

    public static String serviceDir(String baseDir, String service) {
        return baseDir + "/" + service;
    }

    public static String runDir(String baseDir, String service, ZonedDateTime startedAt, String jobId) {
        String prefix = jobId.length() > JOB_ID_PREFIX_LENGTH ? jobId.substring(0, JOB_ID_PREFIX_LENGTH) : jobId;
        return serviceDir(baseDir, service) + "/" + RUN_DIR_FORMAT.format(startedAt) + "-" + prefix;
    }

    public static String stageLogName(String stageName) {
        return stageName.toLowerCase(Locale.ROOT).replace('_', '-') + ".log";
    }

    /**
     * Code context of the most recent run of this service that produced one.
     *
     * <p>{@code skip_code_analysis} used to mean "reuse the file at the fixed path"; with per-run
     * directories there is no fixed path, so the newest previous run supplies it. Returns null when
     * no previous run has one, which the caller turns into an explicit placeholder rather than a
     * silently empty context.
     */
    public static String latestCodeContext(String baseDir, String service, String currentRunDir) {
        Path serviceDir = Path.of(serviceDir(baseDir, service));
        if (!Files.isDirectory(serviceDir)) return null;
        try (Stream<Path> dirs = Files.list(serviceDir)) {
            String current = Path.of(currentRunDir).toAbsolutePath().toString();
            List<Path> candidates = dirs
                .filter(dir -> Files.isDirectory(dir)
                    && !dir.toAbsolutePath().toString().equals(current)
                    && Files.isRegularFile(dir.resolve(CODE_CONTEXT)))
                .sorted(Comparator.comparing((Path dir) -> String.valueOf(dir.getFileName())).reversed())
                .toList();
            if (candidates.isEmpty()) return null;
            return candidates.get(0).resolve(CODE_CONTEXT).toAbsolutePath().toString();
        } catch (IOException e) {
            LOGGER.warn("Could not scan {} for a previous code context: {}", serviceDir, e.getMessage());
            return null;
        }
    }

    /**
     * Re-points {@code <base>/<service>/latest} at this run. Best effort — a filesystem without
     * symlink support simply leaves the per-run directories, which are the authoritative layout.
     */
    public static void updateLatestLink(String baseDir, String service, String runDir) {
        Path link = Path.of(serviceDir(baseDir, service), LATEST_LINK);
        try {
            Files.deleteIfExists(link);
            Files.createSymbolicLink(link, Path.of(runDir).toAbsolutePath());
        } catch (IOException | UnsupportedOperationException e) {
            LOGGER.debug("Could not update the latest link at {}: {}", link, e.getMessage());
        }
    }

    public static boolean isBlankPath(String path) {
        return Strings.isBlank(path);
    }

    private LocalReviewOutputPaths() {
    }
}
