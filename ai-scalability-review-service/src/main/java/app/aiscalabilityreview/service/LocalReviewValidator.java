package app.aiscalabilityreview.service;

import app.aiscalabilityreview.api.localreview.GenerateLocalReviewRequest;
import app.aiscalabilityreview.api.localreview.PreflightResponse.CheckView;
import app.aiscalabilityreview.job.LocalReviewOutputPaths;
import app.aiscalabilityreview.job.LocalReviewRunRegistry;
import core.framework.inject.Inject;
import core.framework.util.Strings;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Locale;
import java.util.List;

/**
 * The single rule set behind both {@code POST /local-review/validate} and the submit itself
 * (FR-1.13).
 *
 * <p>Two properties matter more than the individual rules. Every rule runs on every call, so the
 * caller gets the complete set of problems and fixes one list instead of discovering the next
 * problem after each retry — the path checks used to abort on the first bad path. And nothing is
 * created while a blocking error stands: validation happens before the job document exists, so a
 * rejected request leaves no run, no directory and no notification behind.
 *
 * <p>Errors block. Warnings do not: they describe a degradation the run can legitimately absorb —
 * a source that will come back as {@code NOT_COLLECTED}, a stage the operator chose to skip — and
 * the console asks for an explicit acknowledgement instead of refusing.
 */
public class LocalReviewValidator {
    public static final String SEVERITY_ERROR = "ERROR";
    public static final String SEVERITY_WARNING = "WARNING";

    @Inject
    AppSettingsService appSettingsService;
    @Inject
    LocalReviewRunRegistry runRegistry;
    @Inject
    PreflightService preflightService;

    public List<Issue> validate(GenerateLocalReviewRequest request) {
        List<Issue> issues = new ArrayList<>();
        checkRequiredFields(request, issues);
        checkRepoPaths(request, issues);
        checkCodePaths(request, issues);
        checkOutputDir(request, issues);
        checkCrossFields(request, issues);
        checkModels(request, issues);
        checkInfraRepoContent(request, issues);
        checkRunOptions(request, issues);
        checkConcurrency(request, issues);
        checkEnvironment(issues);
        return issues;
    }

    public List<Issue> errors(List<Issue> issues) {
        return issues.stream().filter(issue -> SEVERITY_ERROR.equals(issue.severity())).toList();
    }

    public List<Issue> warnings(List<Issue> issues) {
        return issues.stream().filter(issue -> SEVERITY_WARNING.equals(issue.severity())).toList();
    }

    private void checkRequiredFields(GenerateLocalReviewRequest request, List<Issue> issues) {
        required(issues, "service", request.service, "The service being reviewed.");
        required(issues, "local_app_repo_path", request.localAppRepoPath, "Absolute path to the cloned application repository.");
        required(issues, "local_infra_repo_path", request.localInfraRepoPath, "Absolute path to the cloned infra repository.");
        required(issues, "env", request.env, "The Datadog environment tag, e.g. uat or prod.");
        required(issues, "namespace", request.namespace, "The Kubernetes namespace, e.g. uat-consumer.");
        required(issues, "domain", request.domain, "The infra repository domain directory, e.g. consumer.");
        required(issues, "operator", request.operator, "Who is running this review.");
    }

    private void checkRepoPaths(GenerateLocalReviewRequest request, List<Issue> issues) {
        checkRepoPath(issues, "local_app_repo_path", request.localAppRepoPath);
        checkRepoPath(issues, "local_infra_repo_path", request.localInfraRepoPath);
    }

    /** Both repositories are checked independently, so two bad paths produce two problems, not one. */
    private void checkRepoPath(List<Issue> issues, String field, String value) {
        if (Strings.isBlank(value)) return;
        Path path = Path.of(value);
        if (!path.isAbsolute()) {
            issues.add(error(field, "PATH_NOT_ABSOLUTE", value + " is not an absolute path.",
                "Give the full path, e.g. /Users/you/IdeaProjects/03Wonder/infra"));
            return;
        }
        if (!Files.exists(path)) {
            issues.add(error(field, "PATH_NOT_FOUND", value + " does not exist.", "Clone the repository first."));
            return;
        }
        if (!Files.isDirectory(path)) {
            issues.add(error(field, "PATH_NOT_DIRECTORY", value + " is not a directory.", null));
        }
    }

    private void checkCodePaths(GenerateLocalReviewRequest request, List<Issue> issues) {
        if (request.appCodePaths == null || request.appCodePaths.isEmpty()) {
            issues.add(warning("app_code_paths", "CODE_PATHS_EMPTY",
                "No app_code_paths given, so stage 1 reads the whole repository.",
                "This is the single biggest lever on stage 1 cost: on wonder-cart-service, naming the modules "
                + "cut the injected context from 807 KB to 111 KB."));
            return;
        }
        for (int i = 0; i < request.appCodePaths.size(); i++) {
            String codePath = request.appCodePaths.get(i);
            String field = "app_code_paths[" + i + "]";
            if (Strings.isBlank(codePath)) {
                issues.add(error(field, "CODE_PATH_BLANK", "Empty entry.", "Remove it or fill it in."));
            } else if (codePath.startsWith("/") || codePath.contains("..")) {
                issues.add(error(field, "CODE_PATH_NOT_RELATIVE", codePath + " must be relative to the repository root.",
                    "e.g. backend/wonder-cart-service"));
            } else if (!Strings.isBlank(request.localAppRepoPath)
                && !Files.isDirectory(Path.of(request.localAppRepoPath, codePath))) {
                issues.add(error(field, "CODE_PATH_NOT_FOUND", codePath + " does not exist in the application repository.",
                    "Check the module path against the repository layout."));
            }
        }
    }

    private void checkOutputDir(GenerateLocalReviewRequest request, List<Issue> issues) {
        String base = Strings.isBlank(request.outputBaseDir) ? appSettingsService.get().outputBaseDir : request.outputBaseDir;
        if (Strings.isBlank(base)) base = "reports";
        for (CheckView check : preflightService.storageChecks(base)) {
            if (PreflightService.FAIL.equals(check.status)) {
                issues.add(error("output_base_dir", "OUTPUT_DIR_NOT_WRITABLE",
                    check.detail + " is not writable.", check.hint));
            }
        }
    }

    private void checkCrossFields(GenerateLocalReviewRequest request, List<Issue> issues) {
        boolean hasHost = !Strings.isBlank(request.mysqlHost);
        boolean hasDb = !Strings.isBlank(request.mysqlDB);
        if (hasHost && !hasDb) {
            issues.add(error("mysql_db", "MYSQL_DB_REQUIRED", "mysql_db is required when mysql_host is set.",
                "Stage 2 queries information_schema for this database."));
        }
        if (!hasHost && hasDb) {
            issues.add(error("mysql_host", "MYSQL_HOST_REQUIRED", "mysql_host is required when mysql_db is set.", null));
        }
        if (!Strings.isBlank(request.hpaType) && !"HPA".equals(request.hpaType) && !"none".equals(request.hpaType)) {
            issues.add(error("hpa_type", "HPA_TYPE_INVALID", request.hpaType + " is not a valid hpa_type.",
                "Use HPA or none."));
        }
        if (!Strings.isBlank(request.kafkaConsumerGroups)) {
            for (String group : request.kafkaConsumerGroups.split(",", -1)) {
                if (group.isBlank()) {
                    issues.add(error("kafka_consumer_groups", "KAFKA_GROUPS_MALFORMED",
                        "kafka_consumer_groups has an empty entry.", "Comma-separated group ids, no trailing comma."));
                    break;
                }
            }
        }
        if (!hasHost && Strings.isBlank(request.atlasCluster)) {
            issues.add(warning("mysql_host", "NO_PERSISTENCE_SOURCE",
                "Neither a MySQL host nor an Atlas cluster is set.",
                "The persistence dimension will be scored with no data behind it."));
        }
    }

    private void checkModels(GenerateLocalReviewRequest request, List<Issue> issues) {
        checkModel(issues, "code_analysis_model", request.codeAnalysisModel);
        checkModel(issues, "metric_collection_model", request.metricCollectionModel);
        checkModel(issues, "synthesis_model", request.synthesisModel);
        checkModel(issues, "validation_model", request.validationModel);
    }

    private void checkModel(List<Issue> issues, String field, String model) {
        if (Strings.isBlank(model) || GeminiModels.VALID_MODELS.contains(model)) return;
        issues.add(error(field, "MODEL_UNKNOWN", model + " is not a model id this Gemini CLI accepts.",
            "Valid ids: " + String.join(", ", GeminiModels.VALID_MODELS.stream().sorted().toList())));
    }

    private void checkInfraRepoContent(GenerateLocalReviewRequest request, List<Issue> issues) {
        if (Strings.isBlank(request.localInfraRepoPath) || Strings.isBlank(request.env)) return;
        Path envDir = Path.of(request.localInfraRepoPath, request.env);
        if (!Files.isDirectory(envDir)) {
            issues.add(error("env", "ENV_DIR_NOT_FOUND", envDir + " does not exist in the infra repository.",
                "The env is a directory name in the infra repository as well as a Datadog tag."));
            return;
        }
        Path variables = envDir.resolve(Path.of("app", "infra", "env", "01-variables.tf"));
        if (!Files.isRegularFile(variables) && Strings.isBlank(request.azureResourceGroup)) {
            issues.add(warning("azure_resource_group", "AZURE_GROUP_UNRESOLVED",
                "The Azure resource group cannot be read from " + variables + ".",
                "The group is not the env: uat lives in FTIUat, prod in RFProdV2. Without it every Azure call "
                + "fails with \"Resource group '" + request.env + "' could not be found\"."));
        }
        if (!Strings.isBlank(request.domain)
            && !Files.isDirectory(envDir.resolve(Path.of("app", request.domain)))) {
            issues.add(warning("domain", "DOMAIN_DIR_NOT_FOUND",
                "No " + request.domain + " directory under " + envDir + "/app.",
                "Stage 2 will find no Kubernetes manifests, so the infra context will be close to empty."));
        }
    }

    private void checkRunOptions(GenerateLocalReviewRequest request, List<Issue> issues) {
        if (Boolean.TRUE.equals(request.skipCodeAnalysis)) {
            String base = Strings.isBlank(request.outputBaseDir) ? appSettingsService.get().outputBaseDir : request.outputBaseDir;
            String previous = Strings.isBlank(base) || Strings.isBlank(request.service)
                ? null : LocalReviewOutputPaths.latestCodeContext(base, request.service, "");
            if (previous == null) {
                issues.add(warning("skip_code_analysis", "NO_CODE_CONTEXT_TO_REUSE",
                    "No previous run of this service left a code-context.md to reuse.",
                    "Synthesis will run against a placeholder saying the code context is unavailable."));
            }
        }
        if (Boolean.TRUE.equals(request.skipValidation)) {
            issues.add(warning("skip_validation", "VALIDATION_SKIPPED",
                "Validation is skipped, so the report carries no verdict.",
                "It drops stages 5A to 5D, which is where most of a run's MCP round-trips are; the eight "
                + "checks are what make the report checkable rather than merely plausible."));
        }
    }

    private void checkConcurrency(GenerateLocalReviewRequest request, List<Issue> issues) {
        if (Strings.isBlank(request.service)) return;
        if (runRegistry.hasRunFor(request.service)) {
            issues.add(error("service", "RUN_ALREADY_IN_FLIGHT",
                "A review of " + request.service + " is already running.",
                "Wait for it, or cancel it from the run list."));
        }
    }

    /** Folds the machine-level preflight results in, so the operator sees one list rather than two. */
    private void checkEnvironment(List<Issue> issues) {
        for (CheckView check : preflightService.machineChecks()) {
            if (PreflightService.FAIL.equals(check.status)) {
                issues.add(error(null, "PREFLIGHT_" + check.id.toUpperCase(Locale.ROOT).replace('-', '_'),
                    check.name + ": " + check.detail, check.hint));
            } else if (PreflightService.WARN.equals(check.status)) {
                issues.add(warning(null, "PREFLIGHT_" + check.id.toUpperCase(Locale.ROOT).replace('-', '_'),
                    check.name + ": " + check.detail, check.hint));
            }
        }
    }

    private void required(List<Issue> issues, String field, String value, String hint) {
        if (Strings.isBlank(value)) {
            issues.add(error(field, "REQUIRED", field + " is required.", hint));
        }
    }

    private Issue error(String field, String code, String message, String hint) {
        return new Issue(field, code, SEVERITY_ERROR, message, hint);
    }

    private Issue warning(String field, String code, String message, String hint) {
        return new Issue(field, code, SEVERITY_WARNING, message, hint);
    }

    /**
     * @param field the request field in the JSON name the form uses, or null for a run-wide issue
     * @param hint  what to do about it; the message alone is rarely enough to act on
     */
    public record Issue(String field, String code, String severity, String message, String hint) {
    }
}
