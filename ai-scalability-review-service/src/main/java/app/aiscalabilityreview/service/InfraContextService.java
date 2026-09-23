package app.aiscalabilityreview.service;

import core.framework.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Builds a single trimmed "infra context" document out of the local infra repository (AD-6383).
 *
 * <p>The previous pipeline injected whole directories into the prompt with {@code @dir}
 * ({@code kube/resource/} holds every service in the domain, {@code app/infra/env/} holds the full
 * Terraform tree). Those directories are by far the largest token contributor, and because the
 * review runs as an agentic loop the whole context is re-sent on every tool round-trip.
 *
 * <p>File selection happens here, in Java, so it costs no tokens and is deterministic: only the
 * files that actually mention this service (or, for Terraform, the node-pool / database resources
 * the review scores) end up in the document. The result is written once per run and shared by the
 * synthesis and validation stages instead of each re-reading the repository.
 *
 * <p>Selection never silently starves the review: when a content filter matches nothing the whole
 * directory is included and the fallback is logged, and every skipped or truncated file is listed
 * in the document itself.
 */
public class InfraContextService {
    private static final List<String> KUBE_EXTENSIONS = List.of(".yaml", ".yml");
    private static final List<String> TERRAFORM_EXTENSIONS = List.of(".tf", ".tfvars", ".hcl");
    /**
     * Deliberately narrow: a bare "aks" matches a third of the Terraform tree because the files
     * cross-reference the cluster, while these four only appear where node pools are declared.
     */
    private static final List<String> NODE_POOL_KEYWORDS = List.of("node_pool", "kubernetes_cluster", "vm_size", "node_count");
    private static final int MAX_FILE_CHARS = 64 * 1024;
    private static final int MAX_TOTAL_CHARS = 384 * 1024;

    private final Logger logger = LoggerFactory.getLogger(InfraContextService.class);

    /**
     * Writes the trimmed infra context to {@code outputPath}.
     *
     * @return a short summary suitable for the stage status line
     */
    public String build(InfraContextParams params) throws IOException {
        String envLower = params.env().toLowerCase(Locale.US);
        Path repo = Path.of(params.infraRepoPath()).toAbsolutePath();
        Selection selection = new Selection(repo);

        Path envDir = repo.resolve(envLower + "/app/infra/env");
        // The service's own manifests are the one input the review cannot do without, so this is the
        // only collection that falls back to the whole directory when nothing matches.
        selection.collect("Kubernetes manifests",
            repo.resolve(envLower + "/app/" + params.domain() + "/kube/resource"),
            KUBE_EXTENSIONS, List.of(params.serviceId()), true);
        selection.collect("AKS node pools (Terraform)", envDir, TERRAFORM_EXTENSIONS, NODE_POOL_KEYWORDS, false);
        if (isPresent(params.mysqlHost())) {
            List<String> needles = mysqlNeedles(params.mysqlHost());
            selection.collect("MySQL Flexible Server (Terraform)", envDir, TERRAFORM_EXTENSIONS, needles, false);
            selection.collect("MySQL database (Terraform)",
                repo.resolve(envLower + "/app/infra/mysql"), TERRAFORM_EXTENSIONS, needles, false);
        }
        if (isPresent(params.atlasCluster())) {
            selection.collect("Atlas MongoDB (Terraform)",
                repo.resolve(envLower + "/atlas"), TERRAFORM_EXTENSIONS, List.of(params.atlasCluster()), false);
        }

        String document = selection.render(params.serviceId(), envLower, repo);
        Path output = Path.of(params.outputPath()).toAbsolutePath();
        Path parent = output.getParent();
        if (parent != null) Files.createDirectories(parent);
        Files.writeString(output, document);

        String summary = Strings.format("{} files, {} chars ({} skipped)",
            selection.includedCount, document.length(), selection.skipped.size());
        logger.info("Built infra context for {}: {} -> {}", params.serviceId(), summary, output);
        return summary;
    }

    private boolean isPresent(String value) {
        return !Strings.isBlank(value) && !"N/A".equalsIgnoreCase(value);
    }

    /**
     * Terraform names a MySQL server by its logical name, not by its Azure host name, e.g. the
     * server {@code rfprodv2-flexible-wonder-db.mysql.database.azure.com} is declared in
     * {@code 16-mysql-flexible-wonder-db.tf} and {@code 02-wonder-db.tf}. Matching on both the full
     * host and the part after the environment prefix finds either spelling.
     */
    private List<String> mysqlNeedles(String host) {
        int dot = host.indexOf('.');
        String shortHost = dot > 0 ? host.substring(0, dot) : host;
        int flexible = shortHost.indexOf("flexible-");
        if (flexible < 0) return List.of(shortHost);
        return List.of(shortHost, shortHost.substring(flexible + "flexible-".length()));
    }

    /**
     * @param outputPath where the trimmed document is written; also the path the prompts reference
     */
    public record InfraContextParams(String infraRepoPath,
                                     String env,
                                     String domain,
                                     String serviceId,
                                     String mysqlHost,
                                     String atlasCluster,
                                     String outputPath) {
    }

    static final class Selection {
        private static final Logger LOGGER = LoggerFactory.getLogger(Selection.class);
        private static final String DOCUMENT_HEADER = """
            # Infrastructure Context — {} ({})

            Local infra repository root: `{}`

            This document was assembled by selecting only the infra files relevant to **{}**. Cite
            values from it as `[Source: code → <path shown in the heading>]`.

            ## Selection Summary

            """;
        private static final String NO_SKIPPED_FILES = "\nNo files were skipped or truncated.\n";
        private static final String SKIPPED_FILES_HEADER = "\n### Skipped or truncated files\n\n";
        private static final String SKIPPED_FILES_FOOTER = """

            If a configuration value you need is in one of these files, read it directly from the
            infra repository root above.
            """;

        final List<String> skipped = new ArrayList<>();
        int includedCount;

        private final Path repo;
        private final StringBuilder body = new StringBuilder(64 * 1024);
        private final List<String> sections = new ArrayList<>();
        private final Set<Path> included = new HashSet<>();
        private int totalChars;

        Selection(Path repo) {
            this.repo = repo;
        }

        void collect(String label, Path dir, List<String> extensions, List<String> keywords, boolean fallbackToAll) throws IOException {
            if (!Files.isDirectory(dir)) {
                sections.add(label + ": directory not found — `" + relative(dir) + '`');
                LOGGER.warn("Infra context: {} directory not found: {}", label, dir);
                return;
            }
            List<Path> candidates = listFiles(dir, extensions);
            List<Path> matched = filter(candidates, keywords);
            if (fallbackToAll && matched.isEmpty() && !candidates.isEmpty()) {
                LOGGER.warn("Infra context: no file under {} matched {}, including all {} files in the directory",
                    dir, keywords, candidates.size());
                matched = candidates;
                sections.add(label + ": nothing matched " + keywords + ", included all " + candidates.size() + " files");
            } else if (matched.isEmpty()) {
                LOGGER.warn("Infra context: no file under {} matched {}", dir, keywords);
                sections.add(label + ": nothing matched " + keywords + " — no file included");
                return;
            } else {
                sections.add(label + ": " + matched.size() + " of " + candidates.size() + " files included");
            }
            body.append("\n## ").append(label).append('\n');
            for (Path file : matched) {
                append(file);
            }
        }

        private List<Path> listFiles(Path dir, List<String> extensions) throws IOException {
            try (Stream<Path> walk = Files.walk(dir)) {
                return walk.filter(path -> Files.isRegularFile(path) && hasExtension(path, extensions) && !isSecret(path))
                    .sorted()
                    .toList();
            }
        }

        /** Never copy anything out of a secrets tree into a document that is handed to a model. */
        private boolean isSecret(Path path) {
            String normalized = path.toString().replace('\\', '/').toLowerCase(Locale.US);
            return normalized.contains("/secret");
        }

        private boolean hasExtension(Path path, List<String> extensions) {
            String name = fileName(path);
            return extensions.stream().anyMatch(name::endsWith);
        }

        /** {@link Path#getFileName()} is null for a root path; treat that as "no name". */
        private String fileName(Path path) {
            Path name = path.getFileName();
            return name == null ? "" : name.toString().toLowerCase(Locale.US);
        }

        private List<Path> filter(List<Path> candidates, List<String> keywords) {
            List<String> needles = keywords.stream()
                .filter(keyword -> !Strings.isBlank(keyword))
                .map(keyword -> keyword.toLowerCase(Locale.US))
                .toList();
            if (needles.isEmpty()) return candidates;
            List<Path> matched = new ArrayList<>();
            for (Path candidate : candidates) {
                if (matches(candidate, needles)) matched.add(candidate);
            }
            return matched;
        }

        private boolean matches(Path candidate, List<String> needles) {
            String name = fileName(candidate);
            if (needles.stream().anyMatch(name::contains)) return true;
            String content = read(candidate);
            if (content == null) return false;
            String lower = content.toLowerCase(Locale.US);
            return needles.stream().anyMatch(lower::contains);
        }

        private void append(Path file) {
            if (!included.add(file)) return;  // a file can match more than one collection
            String content = read(file);
            if (content == null) {
                skipped.add(relative(file) + " (unreadable)");
                return;
            }
            if (totalChars >= MAX_TOTAL_CHARS) {
                skipped.add(relative(file) + " (total size budget reached)");
                return;
            }
            String text = content;
            if (text.length() > MAX_FILE_CHARS) {
                text = text.substring(0, MAX_FILE_CHARS);
                skipped.add(relative(file) + " (truncated to " + MAX_FILE_CHARS + " chars)");
            }
            body.append("\n### `").append(relative(file)).append("`\n\n```\n").append(text).append("\n```\n");
            totalChars += text.length();
            includedCount++;
        }

        private String read(Path file) {
            try {
                return Files.readString(file);
            } catch (IOException e) {
                LOGGER.warn("Infra context: could not read {}: {}", file, e.getMessage());
                return null;
            }
        }

        private String relative(Path file) {
            return repo.getFileName() + "/" + repo.relativize(file).toString().replace('\\', '/');
        }

        String render(String serviceId, String env, Path repoPath) {
            StringBuilder document = new StringBuilder(body.length() + 2048);
            document.append(Strings.format(DOCUMENT_HEADER, serviceId, env, repoPath, serviceId));
            for (String section : sections) {
                document.append(Strings.format("- {}\n", section));
            }
            if (skipped.isEmpty()) {
                document.append(NO_SKIPPED_FILES);
            } else {
                document.append(SKIPPED_FILES_HEADER);
                for (String entry : skipped) {
                    document.append(Strings.format("- {}\n", entry));
                }
                document.append(SKIPPED_FILES_FOOTER);
            }
            document.append(body);
            return document.toString();
        }
    }
}
