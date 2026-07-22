package app.aiscalabilityreview.job.stage;

import app.aiscalabilityreview.domain.ServiceConfig;
import app.aiscalabilityreview.prompt.CodeAnalysisTaskPrompt;
import app.aiscalabilityreview.service.AnthropicService;
import app.aiscalabilityreview.service.AuditLogService;
import app.aiscalabilityreview.service.GitHubService;
import core.framework.inject.Inject;
import core.framework.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stage 1: Fetch application source code from GitHub and run code analysis with Claude.
 * <p>
 * Produces:
 * - context.appCodeContent  — concatenated source files
 * - context.codeContextDocument — AI-generated business context summary
 * - context.repoShas["app"] — commit SHA of the app repo
 */
public class CodeFetchStage {
    private static final int MAX_TOKENS = 8192;
    private static final double TEMPERATURE = 0.2;
    private final Logger logger = LoggerFactory.getLogger(CodeFetchStage.class);
    @Inject
    GitHubService gitHubService;
    @Inject
    AnthropicService anthropicService;
    @Inject
    AuditLogService auditLogService;

    public void execute(ReviewContext context) throws Exception {
        ServiceConfig config = context.config;
        String url = config.repositories.app.url;
        if (Strings.isBlank(url)) {
            logger.warn("No app repository URL configured for service {}", config.serviceId);
            context.appCodeContent = "NOT_COLLECTED: No app repository URL configured.";
            context.codeContextDocument = "Code context: not available";
            return;
        }
        String branch = config.repositories.app.branch != null ? config.repositories.app.branch : "main";
        String token = gitHubService.resolveToken(url);
        logger.info("Fetching app repo code for service {} from {} branch {}", config.serviceId, url, branch);
        // Get the latest commit SHA
        String sha = gitHubService.getLatestCommitSha(url, branch, token);
        if (sha != null) {
            context.repoShas.put("app", sha);
        }
        // Fetch source files
        String dirPath = config.repositories.app.servicePath != null ? config.repositories.app.servicePath : "";
        String codeText = gitHubService.fetchDirectoryAsText(
            url, branch, dirPath, config.repositories.app.includePaths, config.repositories.app.excludePaths, token);

        context.appCodeContent = codeText;
        logger.info("Fetched {} chars of code for service {}", codeText.length(), config.serviceId);
        generate(context, config, codeText);
    }

    private void generate(ReviewContext context, ServiceConfig config, String codeText) {
        // Build code analysis prompt
        String systemPrompt = CodeAnalysisTaskPrompt.CONTENT
            .replace("{{SERVICE}}", config.serviceId);
        String userPrompt = "Here is the source code for **" + config.serviceId + "**:\n\n" + codeText;
        long startMs = System.currentTimeMillis();
        try {
            AnthropicService.GeneratedContent result = anthropicService.generate(
                context.job.aiModel, systemPrompt, userPrompt, MAX_TOKENS, TEMPERATURE);

            context.codeContextDocument = result.text();
            long durationMs = System.currentTimeMillis() - startMs;

            auditLogService.log(new AuditLogService.AuditLogParam(
                context.job.jobId, config.serviceId,
                "CODE_ANALYSIS_AI_CALL", "CodeFetchStage",
                "anthropic/" + context.job.aiModel,
                "Code analysis for " + config.serviceId + " (" + codeText.length() + " chars input)",
                result.inputTokens(),
                200, durationMs,
                "Generated " + result.text().length() + " chars code context",
                null, true));

            logger.info("Code analysis complete for service {}: inputTokens={}, outputTokens={}",
                config.serviceId, result.inputTokens(), result.outputTokens());

        } catch (Exception e) {
            long durationMs = System.currentTimeMillis() - startMs;
            String errorMsg = e.getMessage();
            auditLogService.log(new AuditLogService.AuditLogParam(
                context.job.jobId, config.serviceId,
                "CODE_ANALYSIS_AI_CALL", "CodeFetchStage",
                "anthropic/" + context.job.aiModel,
                "Code analysis for " + config.serviceId,
                null, 500, durationMs, null, errorMsg, false));

            logger.error("Code analysis AI call failed for service {}: {}", config.serviceId, errorMsg);
            context.codeContextDocument = "Code context: not available (AI call failed: " + errorMsg + ")";
            // Non-fatal: proceed with empty code context
        }
    }
}