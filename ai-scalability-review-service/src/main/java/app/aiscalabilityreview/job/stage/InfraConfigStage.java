package app.aiscalabilityreview.job.stage;

import app.aiscalabilityreview.domain.ServiceConfig;
import app.aiscalabilityreview.service.GitHubService;
import core.framework.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Stage 2: Fetch Kubernetes manifests and Terraform configs from infra and k8s-gitops repos.
 * <p>
 * Produces:
 * - context.infraSnapshot — annotated text of K8s manifests and Terraform files
 * - context.repoShas["infra"] and context.repoShas["k8s_gitops"]
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
        StringBuilder snapshot = new StringBuilder(4096);

        // ---- Infra repo ----
        if (config.repositories.infra != null) {
            String branch = config.repositories.infra.branch != null ? config.repositories.infra.branch : "main";
            String infraURL = config.repositories.infra.url;
            String token = gitHubService.resolveToken(infraURL);
            String dirPath = config.repositories.infra.servicePath != null ? config.repositories.infra.servicePath : "";

            logger.info("Fetching infra repo for service {} from {} branch {}", config.serviceId, infraURL, branch);

            String sha = gitHubService.getLatestCommitSha(infraURL, branch, token);
            if (sha != null) context.repoShas.put("infra", sha);

            String infraText = gitHubService.fetchDirectoryAsText(
                infraURL, branch, dirPath, K8S_EXTENSIONS, List.of(), token);

            snapshot.append("## [Source: code → infra/").append(config.runtime.environment)
                .append("/app/").append(config.serviceId).append("/]\n\n")
                .append(infraText);
        } else {
            snapshot.append("## Infra repo: NOT_COLLECTED (no infra repository URL configured)\n\n");
        }

        // ---- k8s-gitops repo ----
        if (config.repositories.k8sGitops != null) {
            String branch = config.repositories.k8sGitops.branch != null ? config.repositories.k8sGitops.branch : "main";
            String k8sGitopsURL = config.repositories.k8sGitops.url;
            String token = gitHubService.resolveToken(k8sGitopsURL);
            String dirPath = config.repositories.k8sGitops.servicePath != null ? config.repositories.k8sGitops.servicePath : "";

            logger.info("Fetching k8s-gitops repo for service {} from {} branch {}", config.serviceId, k8sGitopsURL, branch);

            String sha = gitHubService.getLatestCommitSha(k8sGitopsURL, branch, token);
            if (sha != null) context.repoShas.put("k8s_gitops", sha);

            String gitopsText = gitHubService.fetchDirectoryAsText(
                k8sGitopsURL, branch, dirPath, K8S_EXTENSIONS, List.of(), token);

            snapshot.append("\n\n## [Source: code → k8s-gitops/environments/").append(config.runtime.environment)
                .append("/apps/").append(config.serviceId).append("/]\n\n")
                .append(gitopsText);
        } else {
            snapshot.append("\n\n## k8s-gitops repo: NOT_COLLECTED (no k8s-gitops repository URL configured)\n\n");
        }

        context.infraSnapshot = snapshot.toString();
        logger.info("Infra snapshot assembled for service {}: {} chars", config.serviceId, context.infraSnapshot.length());
    }
}