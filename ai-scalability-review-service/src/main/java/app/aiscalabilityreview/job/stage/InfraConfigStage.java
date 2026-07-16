package app.aiscalabilityreview.job.stage;

import app.aiscalabilityreview.domain.ServiceConfig;
import app.aiscalabilityreview.service.GitHubService;
import core.framework.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Stage 2: Fetch Kubernetes manifests and Terraform configs from infra and k8s-gitops repos.
 *
 * Produces:
 *   - context.infraSnapshot — annotated text of K8s manifests and Terraform files
 *   - context.repoShas["infra"] and context.repoShas["k8s_gitops"]
 */
public class InfraConfigStage {
    private static final List<String> K8S_EXTENSIONS = List.of(
            ".yaml", ".yml", ".tf", ".tfvars", ".json"
    );

    private final Logger logger = LoggerFactory.getLogger(InfraConfigStage.class);

    @Inject
    GitHubService gitHubService;

    public void execute(ReviewContext context) throws Exception {
        ServiceConfig config = context.config;
        StringBuilder snapshot = new StringBuilder();

        // ---- Infra repo ----
        if (config.repoInfraUrl != null && !config.repoInfraUrl.isBlank()) {
            String branch = config.repoInfraBranch != null ? config.repoInfraBranch : "main";
            String token = gitHubService.resolveToken(config.repoInfraUrl);
            String dirPath = config.repoInfraServicePath != null ? config.repoInfraServicePath : "";

            logger.info("Fetching infra repo for service {} from {} branch {}", config.serviceId, config.repoInfraUrl, branch);

            String sha = gitHubService.getLatestCommitSha(config.repoInfraUrl, branch, token);
            if (sha != null) context.repoShas.put("infra", sha);

            String infraText = gitHubService.fetchDirectoryAsText(
                    config.repoInfraUrl, branch, dirPath, K8S_EXTENSIONS, List.of(), token);

            snapshot.append("## [Source: code → infra/").append(config.environment)
                    .append("/app/").append(config.serviceId).append("/]\n\n");
            snapshot.append(infraText);
        } else {
            snapshot.append("## Infra repo: NOT_COLLECTED (no infra repository URL configured)\n\n");
        }

        // ---- k8s-gitops repo ----
        if (config.repoK8sGitopsUrl != null && !config.repoK8sGitopsUrl.isBlank()) {
            String branch = config.repoK8sGitosBranch != null ? config.repoK8sGitosBranch : "main";
            String token = gitHubService.resolveToken(config.repoK8sGitopsUrl);
            String dirPath = config.repoK8sGitopsServicePath != null ? config.repoK8sGitopsServicePath : "";

            logger.info("Fetching k8s-gitops repo for service {} from {} branch {}", config.serviceId, config.repoK8sGitopsUrl, branch);

            String sha = gitHubService.getLatestCommitSha(config.repoK8sGitopsUrl, branch, token);
            if (sha != null) context.repoShas.put("k8s_gitops", sha);

            String gitopsText = gitHubService.fetchDirectoryAsText(
                    config.repoK8sGitopsUrl, branch, dirPath, K8S_EXTENSIONS, List.of(), token);

            snapshot.append("\n\n## [Source: code → k8s-gitops/environments/").append(config.environment)
                    .append("/apps/").append(config.serviceId).append("/]\n\n");
            snapshot.append(gitopsText);
        } else {
            snapshot.append("\n\n## k8s-gitops repo: NOT_COLLECTED (no k8s-gitops repository URL configured)\n\n");
        }

        context.infraSnapshot = snapshot.toString();
        logger.info("Infra snapshot assembled for service {}: {} chars", config.serviceId, context.infraSnapshot.length());
    }
}