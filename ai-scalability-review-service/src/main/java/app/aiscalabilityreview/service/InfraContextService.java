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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Builds a single trimmed "infra context" document out of the local infra repository (AD-6383).
 *
 * <p>The previous pipeline injected whole directories into the prompt with {@code @dir}
 * ({@code kube/resource/} holds every service in the domain, {@code app/infra/env/} holds the full
 * Terraform tree). Those directories are by far the largest token contributor, and because the
 * review runs as an agentic loop the whole context is re-sent on every tool round-trip. This
 * document is read by both the synthesis and the validation stage, so every byte here is paid for
 * twice over.
 *
 * <p>File selection happens here, in Java, so it costs no tokens and is deterministic. Selecting
 * whole files is not enough on its own: the shared Kubernetes manifests hold every service in the
 * domain, so a file that matches this service is mostly other services' configuration. Matching
 * therefore works at two levels — which files to include, and which part of them:
 *
 * <ul>
 *   <li>Kubernetes YAML is split on its document separator and only the documents naming this
 *       service are kept. A manifest that belongs entirely to this service is untouched; the
 *       domain's shared HPA file drops from 23 documents to the one that matters.</li>
 *   <li>Terraform node-pool keywords match only where a resource is <em>declared</em>, not where one
 *       is referenced — the workload-identity file mentions the cluster three times without
 *       declaring a single node pool.</li>
 *   <li>The MySQL server name is matched precisely before it is matched loosely, so a sibling
 *       server whose name merely contains this one's suffix is not dragged in.</li>
 * </ul>
 *
 * <p>Selection never silently starves the review: every narrowing step falls back to the wider
 * result when it would otherwise match nothing, the fallback is logged, and every skipped or
 * trimmed file is listed in the document itself.
 */
public class InfraContextService {
    private static final List<String> KUBE_EXTENSIONS = List.of(".yaml", ".yml");
    private static final List<String> TERRAFORM_EXTENSIONS = List.of(".tf", ".tfvars", ".hcl");
    /**
     * Deliberately narrow: a bare "aks" matches a third of the Terraform tree because the files
     * cross-reference the cluster, while these four only appear where node pools are declared —
     * and {@link MatchMode#DECLARATION} makes sure it is a declaration rather than a reference.
     */
    private static final List<String> NODE_POOL_KEYWORDS = List.of("node_pool", "kubernetes_cluster", "vm_size", "node_count");
    /** {@code variable "env" { type = string  default = "FTIUat" }} — the Azure name of the environment. */
    private static final Pattern ENV_VARIABLE_DEFAULT =
        Pattern.compile("variable\\s+\"env\"\\s*\\{[^}]*?default\\s*=\\s*\"([^\"]+)\"", Pattern.DOTALL);
    /**
     * Values that mean "this service has none of that". {@code null} and {@code none} are here
     * because a caller that stringifies an absent field sends the four characters {@code null},
     * which then matches as a substring — that is how an entire Atlas tree once joined the context
     * of a service with no MongoDB at all.
     */
    private static final Set<String> ABSENT_VALUES = Set.of("n/a", "null", "none", "-");
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
        selection.collect(new Collection("Kubernetes manifests",
            repo.resolve(envLower + "/app/" + params.domain() + "/kube/resource"),
            KUBE_EXTENSIONS, List.of(params.serviceId()), MatchMode.ANYWHERE, true));
        selection.collect(new Collection("AKS node pools (Terraform)", envDir,
            TERRAFORM_EXTENSIONS, NODE_POOL_KEYWORDS, MatchMode.DECLARATION, false));
        if (isPresent(params.mysqlHost())) {
            collectMysql(selection, params.mysqlHost(), envDir, repo.resolve(envLower + "/app/infra/mysql"));
        }
        if (isPresent(params.atlasCluster())) {
            selection.collect(new Collection("Atlas MongoDB (Terraform)", repo.resolve(envLower + "/atlas"),
                TERRAFORM_EXTENSIONS, List.of(params.atlasCluster()), MatchMode.ANYWHERE, false));
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

    /**
     * Terraform spells a MySQL server's name with an interpolated environment prefix
     * ({@code name = "${lower(var.env)}-flexible-consumer-db"}), so the host from the request never
     * appears literally and the suffix has to be matched instead. Matching the bare suffix alone is
     * too loose — {@code consumer-db} is also a substring of {@code ba-consumer-db}, a different
     * server — so the suffix is tried with its {@code flexible-} prefix first and bare only if that
     * found nothing, which keeps the documented fallback for repositories that name the file
     * {@code 02-wonder-db.tf}.
     */
    private void collectMysql(Selection selection, String host, Path envDir, Path mysqlDir) throws IOException {
        List<String> precise = preciseMysqlNeedles(host);
        List<String> loose = looseMysqlNeedles(host);
        String serverLabel = "MySQL Flexible Server (Terraform)";
        String databaseLabel = "MySQL database (Terraform)";

        if (!selection.collect(new Collection(serverLabel, envDir, TERRAFORM_EXTENSIONS, precise, MatchMode.ANYWHERE, false))
            && !loose.isEmpty()) {
            selection.collect(new Collection(serverLabel, envDir, TERRAFORM_EXTENSIONS, loose, MatchMode.ANYWHERE, false));
        }
        if (!selection.collect(new Collection(databaseLabel, mysqlDir, TERRAFORM_EXTENSIONS, precise, MatchMode.ANYWHERE, false))
            && !loose.isEmpty()) {
            selection.collect(new Collection(databaseLabel, mysqlDir, TERRAFORM_EXTENSIONS, loose, MatchMode.ANYWHERE, false));
        }
    }

    /**
     * Azure resource group that hosts this environment, read from the infra repository.
     *
     * <p>It cannot be derived from the review's {@code env}: that value is a Datadog tag — {@code uat},
     * {@code prod} — while the Azure name is {@code FTIUat} or {@code RFProdV2}. Terraform declares
     * the group as {@code name = var.env} in {@code 02-provider.tf}, so the group name is the default
     * of the {@code env} variable in {@code <env>/app/infra/env/01-variables.tf}. Returns null when
     * that file or variable is absent, which the prompt turns into a discovery call rather than a
     * guess — a guessed group is what produced "Resource group 'uat' could not be found".
     */
    public String azureResourceGroup(String infraRepoPath, String env) {
        if (Strings.isBlank(infraRepoPath) || Strings.isBlank(env)) return null;
        Path variables = Path.of(infraRepoPath).toAbsolutePath()
            .resolve(env.toLowerCase(Locale.US) + "/app/infra/env/01-variables.tf");
        if (!Files.exists(variables)) {
            logger.warn("No {} in the infra repository; the Azure resource group stays unresolved", variables);
            return null;
        }
        try {
            Matcher matcher = ENV_VARIABLE_DEFAULT.matcher(Files.readString(variables));
            if (matcher.find()) return matcher.group(1);
            logger.warn("No `variable \"env\"` default in {}; the Azure resource group stays unresolved", variables);
        } catch (IOException e) {
            logger.warn("Could not read {}: {}", variables, e.getMessage());
        }
        return null;
    }

    private boolean isPresent(String value) {
        return !Strings.isBlank(value) && !ABSENT_VALUES.contains(value.trim().toLowerCase(Locale.US));
    }

    /** The host as written, plus its {@code flexible-} suffix, which is what Terraform interpolates. */
    private List<String> preciseMysqlNeedles(String host) {
        String shortHost = shortHost(host);
        int flexible = shortHost.indexOf("flexible-");
        if (flexible < 0) return List.of(shortHost);
        return List.of(shortHost, shortHost.substring(flexible));
    }

    /** The bare database suffix — loose enough to also match a sibling server, so used only last. */
    private List<String> looseMysqlNeedles(String host) {
        String shortHost = shortHost(host);
        int flexible = shortHost.indexOf("flexible-");
        if (flexible < 0) return List.of();
        return List.of(shortHost.substring(flexible + "flexible-".length()));
    }

    private String shortHost(String host) {
        int dot = host.indexOf('.');
        return dot > 0 ? host.substring(0, dot) : host;
    }

    /** How a keyword has to appear in a file for the file to count as a match. */
    enum MatchMode {
        /** Anywhere in the name or the body. */
        ANYWHERE,
        /** Only on a Terraform {@code resource "…"} or {@code data "…"} line, never a reference. */
        DECLARATION
    }

    /**
     * @param fallbackToAll include the whole directory when no file matches, rather than nothing
     */
    record Collection(String label,
                      Path dir,
                      List<String> extensions,
                      List<String> keywords,
                      MatchMode mode,
                      boolean fallbackToAll) {
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

            This document was assembled by selecting only the infra files relevant to **{}**, and from
            the shared ones only the parts that name it. Cite values from it as
            `[Source: code → <path shown in the heading>]`.

            ## Selection Summary

            """;
        private static final String NO_SKIPPED_FILES = "\nNo files were skipped or trimmed.\n";
        private static final String SKIPPED_FILES_HEADER = "\n### Skipped or trimmed files\n\n";
        private static final String SKIPPED_FILES_FOOTER = """

            If a configuration value you need is in one of these files, read it directly from the
            infra repository root above.
            """;
        private static final Pattern YAML_DOCUMENT_SEPARATOR = Pattern.compile("(?m)^---\\s*$");

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

        /** @return true when the collection contributed at least one file */
        boolean collect(Collection collection) throws IOException {
            if (!Files.isDirectory(collection.dir())) {
                sections.add(collection.label() + ": directory not found — `" + relative(collection.dir()) + '`');
                LOGGER.warn("Infra context: {} directory not found: {}", collection.label(), collection.dir());
                return false;
            }
            List<Path> candidates = listFiles(collection.dir(), collection.extensions());
            List<Path> matched = filter(candidates, collection.keywords(), collection.mode());
            if (collection.fallbackToAll() && matched.isEmpty() && !candidates.isEmpty()) {
                LOGGER.warn("Infra context: no file under {} matched {}, including all {} files in the directory",
                    collection.dir(), collection.keywords(), candidates.size());
                matched = candidates;
                sections.add(collection.label() + ": nothing matched " + collection.keywords()
                    + ", included all " + candidates.size() + " files");
            } else if (matched.isEmpty()) {
                LOGGER.info("Infra context: no file under {} matched {}", collection.dir(), collection.keywords());
                return false;
            } else {
                sections.add(collection.label() + ": " + matched.size() + " of " + candidates.size() + " files included");
            }
            body.append("\n## ").append(collection.label()).append('\n');
            for (Path file : matched) {
                append(file, collection.keywords());
            }
            return true;
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

        private List<Path> filter(List<Path> candidates, List<String> keywords, MatchMode mode) {
            List<String> needles = needles(keywords);
            if (needles.isEmpty()) return candidates;
            List<Path> matched = new ArrayList<>();
            for (Path candidate : candidates) {
                if (matches(candidate, needles, mode)) matched.add(candidate);
            }
            return matched;
        }

        private List<String> needles(List<String> keywords) {
            return keywords.stream()
                .filter(keyword -> !Strings.isBlank(keyword))
                .map(keyword -> keyword.toLowerCase(Locale.US))
                .toList();
        }

        private boolean matches(Path candidate, List<String> needles, MatchMode mode) {
            String name = fileName(candidate);
            if (needles.stream().anyMatch(name::contains)) return true;
            String content = read(candidate);
            if (content == null) return false;
            String lower = content.toLowerCase(Locale.US);
            if (mode == MatchMode.DECLARATION) return declaresAny(lower, needles);
            return needles.stream().anyMatch(lower::contains);
        }

        /**
         * True when a needle appears on a Terraform declaration line. A file that only references a
         * resource declared elsewhere — {@code issuer = azurerm_kubernetes_cluster.aks_v2.oidc_issuer_url}
         * — is not about node pools, and including it costs 16 KB for nothing.
         */
        private boolean declaresAny(String lowerContent, List<String> needles) {
            for (String line : lowerContent.split("\n")) {
                String trimmed = line.stripLeading();
                if (!trimmed.startsWith("resource ") && !trimmed.startsWith("data ")) continue;
                if (needles.stream().anyMatch(trimmed::contains)) return true;
            }
            return false;
        }

        private void append(Path file, List<String> keywords) {
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
            String text = trimYamlDocuments(file, content, needles(keywords));
            if (text.length() > MAX_FILE_CHARS) {
                text = text.substring(0, MAX_FILE_CHARS);
                skipped.add(relative(file) + " (truncated to " + MAX_FILE_CHARS + " chars)");
            }
            body.append("\n### `").append(relative(file)).append("`\n\n```\n").append(text).append("\n```\n");
            totalChars += text.length();
            includedCount++;
        }

        /**
         * Keeps only the YAML documents that name the service.
         *
         * <p>A Kubernetes document is self-contained, so dropping the neighbours loses nothing — and
         * the neighbours are the point: the domain's shared HPA manifest holds 23 documents of which
         * one belongs to the service under review. A file whose documents all match, which is the
         * case for the service's own manifests, comes through untouched.
         */
        private String trimYamlDocuments(Path file, String content, List<String> needles) {
            if (needles.isEmpty() || !hasExtension(file, KUBE_EXTENSIONS)) return content;
            String[] documents = YAML_DOCUMENT_SEPARATOR.split(content);
            if (documents.length < 2) return content;

            List<String> kept = new ArrayList<>();
            for (String document : documents) {
                String lower = document.toLowerCase(Locale.US);
                if (needles.stream().anyMatch(lower::contains)) kept.add(document.strip());
            }
            // Nothing matched means the file was selected by its name alone; keep it whole rather
            // than hand the review an empty block.
            if (kept.isEmpty() || kept.size() == documents.length) return content;

            skipped.add(relative(file) + " (kept " + kept.size() + " of " + documents.length
                + " YAML documents — the rest belong to other services)");
            return String.join("\n---\n", kept);
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
