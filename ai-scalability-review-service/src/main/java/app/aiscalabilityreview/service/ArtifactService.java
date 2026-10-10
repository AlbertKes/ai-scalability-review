package app.aiscalabilityreview.service;

import app.aiscalabilityreview.api.review.GetStageLogResponse;
import app.aiscalabilityreview.api.review.ListArtifactResponse;
import app.aiscalabilityreview.api.review.ListArtifactResponse.ArtifactView;
import app.aiscalabilityreview.domain.ReviewJob;
import app.aiscalabilityreview.domain.ReviewJob.StageStatus;
import app.aiscalabilityreview.job.LocalReviewOutputPaths;
import app.aiscalabilityreview.job.LocalReviewStages;
import core.framework.util.Strings;
import core.framework.web.exception.BadRequestException;
import core.framework.web.exception.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Serves the files a run produced.
 *
 * <p>Every read resolves inside the run's own directory and is rejected if it escapes it. The site
 * is bound to localhost and has no login, which is exactly why a path parameter must not be able to
 * address arbitrary files: the blast radius of a mistake here is the developer's whole disk.
 */
public class ArtifactService {
    private static final long LOG_CHUNK_LIMIT = 64 * 1024L;
    private final Logger logger = LoggerFactory.getLogger(ArtifactService.class);

    public ListArtifactResponse list(ReviewJob job) {
        ListArtifactResponse response = new ListArtifactResponse();
        response.outputDir = job.outputDir;
        response.artifacts = new ArrayList<>();
        if (Strings.isBlank(job.outputDir)) return response;
        Path dir = Path.of(job.outputDir);
        if (!Files.isDirectory(dir)) return response;

        Map<String, String> producers = producers(job);
        try (Stream<Path> files = Files.list(dir)) {
            response.artifacts = files.filter(Files::isRegularFile)
                .sorted(Comparator.comparing(path -> String.valueOf(path.getFileName())))
                .map(path -> toView(path, producers))
                .toList();
        } catch (IOException e) {
            logger.warn("Could not list artifacts in {}: {}", dir, e.getMessage());
        }
        return response;
    }

    /** @throws BadRequestException when the name escapes the run directory */
    public Path resolve(ReviewJob job, String name) {
        if (Strings.isBlank(job.outputDir)) throw new NotFoundException("This run has no output directory");
        Path dir = Path.of(job.outputDir).toAbsolutePath().normalize();
        Path target = dir.resolve(name).toAbsolutePath().normalize();
        if (!target.startsWith(dir)) {
            throw new BadRequestException("Artifact path escapes the run directory: " + name);
        }
        if (!Files.isRegularFile(target)) throw new NotFoundException("Artifact not found: " + name);
        return target;
    }

    public byte[] zip(ReviewJob job) {
        if (Strings.isBlank(job.outputDir)) throw new NotFoundException("This run has no output directory");
        Path dir = Path.of(job.outputDir).toAbsolutePath().normalize();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(buffer, StandardCharsets.UTF_8);
             Stream<Path> files = Files.walk(dir)) {
            files.filter(Files::isRegularFile).forEach(path -> addEntry(zip, dir, path));
        } catch (IOException e) {
            throw new UncheckedIOException("Could not package the run directory", e);
        }
        return buffer.toByteArray();
    }

    /**
     * Tails a stage's captured CLI output from a byte offset.
     *
     * <p>Polled rather than pushed: the CLI runs with {@code --output-format json} and writes
     * nothing to stdout until the stage ends, so its stderr file is the only live signal there is,
     * and a cursor over that file follows it without a second transport to maintain.
     */
    public GetStageLogResponse stageLog(ReviewJob job, String stage, Long offset) {
        GetStageLogResponse response = new GetStageLogResponse();
        response.stage = stage;
        response.content = "";
        long cursor = offset == null ? 0L : offset;
        response.nextOffset = cursor;
        response.size = 0L;
        response.complete = Boolean.TRUE;
        if (Strings.isBlank(stage) || Strings.isBlank(job.outputDir)) return response;

        StageStatus status = job.stageStatuses == null ? null : job.stageStatuses.get(stage);
        String logFile = status == null ? LocalReviewOutputPaths.stageLogName(stage) : status.logFile;
        if (logFile == null) logFile = LocalReviewOutputPaths.stageLogName(stage);
        response.complete = status == null || !ReviewJob.STATUS_RUNNING.equals(status.status);

        Path path = Path.of(job.outputDir, logFile);
        if (!Files.isRegularFile(path)) return response;
        try {
            long size = Files.size(path);
            response.size = size;
            long from = Math.min(cursor, size);
            long length = Math.min(size - from, LOG_CHUNK_LIMIT);
            byte[] chunk = readChunk(path, from, (int) length);
            response.content = new String(chunk, StandardCharsets.UTF_8);
            long next = from + chunk.length;
            response.nextOffset = next;
        } catch (IOException e) {
            logger.warn("Could not read the stage log {}: {}", path, e.getMessage());
        }
        return response;
    }

    /** Removes a run's whole directory. Confined to the stored output directory, never a parent. */
    public void deleteRunDir(ReviewJob job) {
        if (Strings.isBlank(job.outputDir)) return;
        Path dir = Path.of(job.outputDir).toAbsolutePath().normalize();
        if (!Files.isDirectory(dir)) return;
        try (Stream<Path> files = Files.walk(dir)) {
            files.sorted(Comparator.reverseOrder()).forEach(this::deleteQuietly);
        } catch (IOException e) {
            logger.warn("Could not delete the run directory {}: {}", dir, e.getMessage());
        }
    }

    private void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            logger.warn("Could not delete {}: {}", path, e.getMessage());
        }
    }

    private byte[] readChunk(Path path, long from, int length) throws IOException {
        if (length <= 0) return new byte[0];
        try (SeekableByteChannel channel = Files.newByteChannel(path)) {
            channel.position(from);
            ByteBuffer buffer = ByteBuffer.allocate(length);
            int read = channel.read(buffer);
            if (read <= 0) return new byte[0];
            byte[] bytes = new byte[read];
            buffer.flip();
            buffer.get(bytes);
            return bytes;
        }
    }

    private void addEntry(ZipOutputStream zip, Path dir, Path path) {
        try {
            zip.putNextEntry(new ZipEntry(dir.relativize(path).toString()));
            copy(path, zip);
            zip.closeEntry();
        } catch (IOException e) {
            logger.warn("Could not add {} to the archive: {}", path, e.getMessage());
        }
    }

    private void copy(Path path, OutputStream target) throws IOException {
        try (InputStream input = Files.newInputStream(path)) {
            input.transferTo(target);
        }
    }

    private Map<String, String> producers(ReviewJob job) {
        Map<String, String> producers = new HashMap<>();
        if (job.stageStatuses == null) return producers;
        job.stageStatuses.forEach((stage, status) -> {
            if (status == null) return;
            if (status.artifacts != null) status.artifacts.forEach(artifact -> producers.put(artifact, stage));
            if (status.logFile != null) producers.put(status.logFile, stage);
        });
        return producers;
    }

    private ArtifactView toView(Path path, Map<String, String> producers) {
        String name = String.valueOf(path.getFileName());
        ArtifactView view = new ArtifactView();
        view.name = name;
        view.stage = producers.getOrDefault(name, LocalReviewStages.producerOf(name));
        view.kind = kind(name);
        try {
            view.sizeBytes = Files.size(path);
            view.modifiedAt = ZonedDateTime.ofInstant(Files.getLastModifiedTime(path).toInstant(), ZoneId.systemDefault());
        } catch (IOException e) {
            logger.debug("Could not stat {}: {}", path, e.getMessage());
        }
        return view;
    }

    private String kind(String name) {
        if (name.endsWith(".md")) return "MARKDOWN";
        if (name.endsWith(".json")) return "JSON";
        if (name.endsWith(".log")) return "LOG";
        return "OTHER";
    }
}
