package app.aiscalabilityreview.service;

import app.aiscalabilityreview.api.localreview.PreflightRequest;
import app.aiscalabilityreview.api.localreview.PreflightResponse;
import app.aiscalabilityreview.api.localreview.PreflightResponse.CheckView;
import app.aiscalabilityreview.domain.ReviewJob;
import app.aiscalabilityreview.prompt.LocalReviewPromptBuilder;
import com.mongodb.client.model.Filters;
import core.framework.inject.Inject;
import core.framework.mongo.MongoCollection;
import core.framework.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * The environment doctor (FR-10).
 *
 * <p>Most local review failures are environmental rather than anything to do with the service under
 * review — a missing CLI, an expired credential, an MCP server configured under another name, a VPN
 * that is not up. Each check therefore carries the remediation, not just the symptom.
 */
public class PreflightService {
    public static final String PASS = "PASS";
    public static final String WARN = "WARN";
    public static final String FAIL = "FAIL";

    private static final String CATEGORY_CLI = "CLI";
    private static final String CATEGORY_AUTH = "AUTH";
    private static final String CATEGORY_MCP = "MCP";
    private static final String CATEGORY_STORAGE = "STORAGE";
    private static final String CATEGORY_REPO = "REPO";
    private static final String CATEGORY_DATA = "DATA";

    private static final Path GEMINI_SETTINGS = Path.of(System.getProperty("user.home"), ".gemini", "settings.json");
    private static final Path ADC_FILE = Path.of(System.getProperty("user.home"),
        ".config", "gcloud", "application_default_credentials.json");

    private final Logger logger = LoggerFactory.getLogger(PreflightService.class);

    @Inject
    GeminiCliService geminiCliService;
    @Inject
    GitRepoInspector gitRepoInspector;
    @Inject
    AppSettingsService appSettingsService;
    @Inject
    MongoCollection<ReviewJob> reviewJobCollection;

    public PreflightResponse check(PreflightRequest request) {
        List<CheckView> checks = new ArrayList<>(machineChecks());
        checks.addAll(storageChecks(request.outputBaseDir));
        checks.addAll(repoChecks(request));
        checks.addAll(dataChecks(request));

        PreflightResponse response = new PreflightResponse();
        response.checks = checks;
        response.ok = checks.stream().noneMatch(check -> FAIL.equals(check.status));
        return response;
    }

    /** Checks that depend only on this machine, so they apply to every run. */
    public List<CheckView> machineChecks() {
        List<CheckView> checks = new ArrayList<>();
        String version = geminiCliService.version();
        checks.add(version == null
            ? view("gemini-cli", "Gemini CLI on PATH", CATEGORY_CLI, FAIL, "not found",
                "Start the service from a shell where `gemini --version` works; the run shells out to it.")
            : view("gemini-cli", "Gemini CLI on PATH", CATEGORY_CLI, PASS, version, null));

        boolean adc = Files.isRegularFile(ADC_FILE);
        checks.add(view("adc", "Application Default Credentials", CATEGORY_AUTH, adc ? PASS : FAIL,
            adc ? ADC_FILE.toString() : "no credentials file",
            adc ? null : "Run: gcloud auth application-default login"));

        String apiKey = envNameInUse("GOOGLE_API_KEY", "GEMINI_API_KEY");
        checks.add(view("api-key-absent", "No inherited Gemini API key", CATEGORY_AUTH,
            apiKey == null ? PASS : WARN,
            apiKey == null ? "neither GOOGLE_API_KEY nor GEMINI_API_KEY is set" : apiKey + " is set",
            apiKey == null ? null
                : "The run strips it from the CLI subprocess, because an API key takes precedence over ADC and "
                + "redirects the call to whatever project owns the key. Remove it from your shell profile to "
                + "avoid the confusion."));

        checks.add(view("vertex-target", "Vertex AI project and location", CATEGORY_AUTH, PASS,
            geminiCliService.cloudProject() + " / " + geminiCliService.cloudLocation(), null));

        String settings = readGeminiSettings();
        checks.add(mcpCheck(settings, "datadog"));
        checks.add(mcpCheck(settings, "azure"));

        checks.add(mongoCheck());
        return checks;
    }

    public List<CheckView> storageChecks(String outputBaseDir) {
        String base = Strings.isBlank(outputBaseDir) ? appSettingsService.get().outputBaseDir : outputBaseDir;
        if (Strings.isBlank(base)) base = "reports";
        Path path = Path.of(base).toAbsolutePath();
        boolean writable = isWritable(path);
        return List.of(view("output-dir", "Output directory writable", CATEGORY_STORAGE,
            writable ? PASS : FAIL, path.toString(),
            writable ? null : "Create it or point output_base_dir somewhere this process can write."));
    }

    public List<CheckView> repoChecks(PreflightRequest request) {
        List<CheckView> checks = new ArrayList<>();
        addRepoCheck(checks, "app-repo", "Application repository", request.localAppRepoPath);
        addRepoCheck(checks, "infra-repo", "Infra repository", request.localInfraRepoPath);
        if (!Strings.isBlank(request.localInfraRepoPath) && !Strings.isBlank(request.env)) {
            checks.add(envVariablesCheck(request));
        }
        if (!Strings.isBlank(request.localInfraRepoPath) && !Strings.isBlank(request.env)
            && !Strings.isBlank(request.domain)) {
            Path domainDir = Path.of(request.localInfraRepoPath, request.env, "app", request.domain);
            boolean exists = Files.isDirectory(domainDir);
            checks.add(view("infra-domain", "Infra domain directory", CATEGORY_REPO, exists ? PASS : WARN,
                domainDir.toString(),
                exists ? null : "Stage 2 finds no manifests under this path, so the infra context will be empty."));
        }
        return checks;
    }

    public List<CheckView> dataChecks(PreflightRequest request) {
        List<CheckView> checks = new ArrayList<>();
        if (Strings.isBlank(request.mysqlHost)) return checks;
        String user = envNameInUse("MYSQL_USER");
        String password = envNameInUse("MYSQL_PASSWORD");
        String settingsEntry = LocalReviewPromptBuilder.mysqlConnectionName(request.mysqlHost, request.mysqlDB);
        boolean fromEnv = user != null && password != null;
        boolean fromSettings = settingsEntry != null && readGeminiSettings().contains("\"" + settingsEntry + "\"");
        String status = fromEnv || fromSettings ? PASS : WARN;
        String detail = fromEnv ? "MYSQL_USER / MYSQL_PASSWORD from the environment"
            : fromSettings ? "the " + settingsEntry + " entry in ~/.gemini/settings.json"
            : "no credentials found";
        checks.add(view("mysql-credentials", "MySQL table storage credentials", CATEGORY_DATA, status, detail,
            PASS.equals(status) ? null
                : "Table sizes degrade to a NOT_COLLECTED line. Set MYSQL_USER / MYSQL_PASSWORD, or add the "
                + "<mysql-host-short>-<db> entry to ~/.gemini/settings.json. The host also needs the VPN."));
        return checks;
    }

    private CheckView envVariablesCheck(PreflightRequest request) {
        Path variables = Path.of(request.localInfraRepoPath, request.env, "app", "infra", "env", "01-variables.tf");
        boolean exists = Files.isRegularFile(variables);
        return view("infra-env-variables", "Azure resource group source", CATEGORY_REPO, exists ? PASS : WARN,
            variables.toString(),
            exists ? null
                : "This file is where the Azure resource group is read from — the env is a Datadog tag, not the "
                + "group name (uat lives in FTIUat). Without it every Azure call fails with "
                + "\"Resource group '" + request.env + "' could not be found\" unless azure_resource_group is passed.");
    }

    private void addRepoCheck(List<CheckView> checks, String id, String name, String repoPath) {
        if (Strings.isBlank(repoPath)) return;
        Path path = Path.of(repoPath);
        if (!Files.isDirectory(path)) {
            checks.add(view(id, name, CATEGORY_REPO, FAIL, repoPath + " does not exist",
                "Clone it locally and give the absolute path."));
            return;
        }
        if (!gitRepoInspector.isGitRepo(repoPath)) {
            checks.add(view(id, name, CATEGORY_REPO, WARN, repoPath + " is not a git repository",
                "The run still works, but it records no commit, so two runs cannot be told apart."));
            return;
        }
        String sha = gitRepoInspector.sha(repoPath);
        boolean dirty = gitRepoInspector.isDirty(repoPath);
        checks.add(view(id, name, CATEGORY_REPO, dirty ? WARN : PASS,
            repoPath + " @ " + (sha == null ? "unknown" : sha) + (dirty ? " (uncommitted changes)" : ""),
            dirty ? "Uncommitted changes are what the run will read, and they are not reproducible later." : null));
    }

    private CheckView mcpCheck(String settings, String server) {
        boolean configured = Pattern.compile("\"" + Pattern.quote(server) + "\"\\s*:").matcher(settings).find();
        return view("mcp-" + server, server + " MCP server", CATEGORY_MCP, configured ? PASS : WARN,
            configured ? "configured in ~/.gemini/settings.json" : "not configured",
            configured ? null
                : "Each stage starts only the servers it names, so a server configured under a different name is "
                + "simply unavailable and its metrics come back as NOT_COLLECTED.");
    }

    private CheckView mongoCheck() {
        try {
            reviewJobCollection.count(Filters.empty());
            return view("mongodb", "MongoDB reachable", CATEGORY_STORAGE, PASS, "connected", null);
        } catch (RuntimeException e) {
            return view("mongodb", "MongoDB reachable", CATEGORY_STORAGE, FAIL, e.getMessage(),
                "Start MongoDB; the console reads every run from it.");
        }
    }

    private boolean isWritable(Path path) {
        try {
            Files.createDirectories(path);
            return Files.isWritable(path);
        } catch (IOException e) {
            logger.debug("Output directory {} is not usable: {}", path, e.getMessage());
            return false;
        }
    }

    private String readGeminiSettings() {
        try {
            return Files.isRegularFile(GEMINI_SETTINGS) ? Files.readString(GEMINI_SETTINGS, StandardCharsets.UTF_8) : "";
        } catch (IOException e) {
            logger.debug("Could not read {}: {}", GEMINI_SETTINGS, e.getMessage());
            return "";
        }
    }

    private String envNameInUse(String... names) {
        for (String name : names) {
            if (!Strings.isBlank(System.getenv(name))) return name;
        }
        return null;
    }

    private CheckView view(String id, String name, String category, String status, String detail, String hint) {
        CheckView view = new CheckView();
        view.id = id;
        view.name = name;
        view.category = category;
        view.status = status;
        view.detail = detail;
        view.hint = hint;
        return view;
    }
}
