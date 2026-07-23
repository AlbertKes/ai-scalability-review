package app.aiscalabilityreview.prompt;

import core.framework.util.Strings;

import java.util.Locale;

/**
 * Builds Gemini CLI prompts for the three local review stages.
 * Stage 1 reuses ReviewTaskPrompt (full 9-step review) with a local-file preamble
 * that maps @file references to the local infra/app repo paths.
 * Stage 2 reuses ValidateReportTaskPrompt (full 7-check validation).
 */
public class LocalReviewPromptBuilder {

    /**
     * Stage 0 — Code analysis prompt.
     */
    public static String buildCodeAnalysisPrompt(String serviceId, String localAppRepoPath) {
        return CodeAnalysisTaskPrompt.CONTENT.replace("{{SERVICE}}", serviceId)
            + "\n\n## Source Code\n\nThe following files are from the **"
            + serviceId + "** application repository. Analyze them per the instructions above.\n\n@"
            + localAppRepoPath + '\n';
    }

    /**
     * Stage 1 — Full scalability review reusing ReviewTaskPrompt (9 steps) with local @file preamble.
     */
    public static String buildReviewPrompt(ReviewPromptParams p) {
        ResolvedReviewParams r = new ResolvedReviewParams(p);
        StringBuilder sb = new StringBuilder(32768);
        appendLocalFileReferences(sb, r);
        sb.append(resolveReviewTaskPlaceholders(r));
        appendScoringAndFormatRefs(sb);
        return sb.toString();
    }

    /**
     * Stage 2 — Full 7-check validation reusing ValidateReportTaskPrompt with local file references.
     */
    public static String buildValidationPrompt(ValidationPromptParams p) {
        String envLower = p.env.toLowerCase(Locale.US);
        StringBuilder sb = new StringBuilder(8192);
        sb.append(resolveValidationPlaceholders(p, envLower));
        appendValidationFileRefs(sb, p);
        sb.append("\n\n## Validation Format Reference\n\n").append(ValidationFormatPrompt.CONTENT)
          .append("\n\nNow produce the complete validation report.\n");
        return sb.toString();
    }

    // --- Stage 1 helpers ---

    private static void appendLocalFileReferences(StringBuilder sb, ResolvedReviewParams r) {
        sb.append("## Attached Local Files\n\nRead the following local files and directories before executing the task steps below.\n\n**Business context (code analysis output)**:\n@")
            .append(r.codeContextPath)
            .append("\n\n**Kubernetes manifests**:\n@")
            .append(r.localInfraRepoPath).append('/').append(r.envLower)
            .append("/app/").append(r.domain)
            .append("/kube/resource/\n\n**AKS node pools / infra Terraform**:\n@")
            .append(r.localInfraRepoPath).append('/').append(r.envLower)
            .append("/app/infra/env/\n\n");
        appendOptionalInfraRefs(sb, r);
        sb.append("---\n\n");
    }

    private static void appendOptionalInfraRefs(StringBuilder sb, ResolvedReviewParams r) {
        if (!"N/A".equals(r.mysqlHost)) {
            sb.append("**MySQL Terraform**:\n@")
                .append(r.localInfraRepoPath).append('/').append(r.envLower)
                .append("/app/infra/mysql/\n\n");
        }
        if (!"N/A".equals(r.atlasCluster)) {
            sb.append("**Atlas MongoDB Terraform**:\n@")
                .append(r.localInfraRepoPath).append('/').append(r.envLower)
                .append("/atlas/\n\n");
        }
    }

    private static String resolveReviewTaskPlaceholders(ResolvedReviewParams r) {
        return ReviewTaskPrompt.CONTENT
            .replace("{{SERVICE}}", r.serviceId)
            .replace("{{ENV}}", r.envLower)
            .replace("{{NAMESPACE}}", r.namespace)
            .replace("{{DOMAIN}}", r.domain)
            .replace("{{MYSQL_HOST}}", r.mysqlHost)
            .replace("{{MYSQL_DB}}", r.mysqlDb)
            .replace("{{ATLAS_CLUSTER}}", r.atlasCluster)
            .replace("{{HPA_TYPE}}", r.hpaType)
            .replace("{{KAFKA_CONSUMER_GROUPS}}", r.kafkaGroups);
    }

    private static void appendScoringAndFormatRefs(StringBuilder sb) {
        sb.append("\n\n## Metric Scoring Reference\n\n").append(MetricScorePrompt.CONTENT)
            .append("\n\n## Report Format Reference\n\n").append(ReportFormatPrompt.CONTENT)
            .append("\n\nNow produce the complete scalability review report.\n");
    }

    // --- Stage 2 helpers ---

    private static String resolveValidationPlaceholders(ValidationPromptParams p, String envLower) {
        return ValidateReportTaskPrompt.CONTENT
            .replace("{{REPORT_FILE}}", p.reviewReportPath)
            .replace("{{SERVICE}}", p.serviceId)
            .replace("{{ENV}}", envLower)
            .replace("{{NAMESPACE}}", p.namespace)
            .replace("{{MYSQL_HOST}}", nvl(p.mysqlHost, "N/A"))
            .replace("{{MYSQL_DB}}", nvl(p.mysqlDb, "N/A"))
            .replace("{{ATLAS_CLUSTER}}", nvl(p.atlasCluster, "N/A"))
            .replace("{{KAFKA_CONSUMER_GROUPS}}", nvl(p.kafkaConsumerGroups, "N/A"));
    }

    private static void appendValidationFileRefs(StringBuilder sb, ValidationPromptParams p) {
        sb.append("\n\n## Attached Local Files\n\n**Review report to validate**:\n@")
            .append(p.reviewReportPath).append('\n');
        if (!Strings.isBlank(p.localInfraRepoPath)) {
            sb.append("\n**Infra repository root** (for re-reading source-annotated config files):\nLocal path: `")
                .append(p.localInfraRepoPath)
                .append("`\n\nWhen the report cites `[Source: code → infra/...]`, the full path is `")
                .append(p.localInfraRepoPath)
                .append("/<relative-path>`. Use `@")
                .append(p.localInfraRepoPath)
                .append("/<relative-path>` to read those files.\n");
        }
    }

    private static String nvl(String value, String defaultValue) {
        return Strings.isBlank(value) ? defaultValue : value;
    }

    public static class ReviewPromptParams {
        public String serviceId;
        public String localInfraRepoPath;
        public String codeContextPath;
        public String env;
        public String namespace;
        public String domain;
        public String mysqlHost;
        public String mysqlDb;
        public String atlasCluster;
        public String hpaType;
        public String kafkaConsumerGroups;
    }

    public static class ValidationPromptParams {
        public String serviceId;
        public String reviewReportPath;
        public String localInfraRepoPath;
        public String env;
        public String namespace;
        public String mysqlHost;
        public String mysqlDb;
        public String atlasCluster;
        public String kafkaConsumerGroups;
    }

    private static class ResolvedReviewParams {
        final String serviceId;
        final String envLower;
        final String namespace;
        final String domain;
        final String localInfraRepoPath;
        final String codeContextPath;
        final String mysqlHost;
        final String mysqlDb;
        final String atlasCluster;
        final String hpaType;
        final String kafkaGroups;

        ResolvedReviewParams(ReviewPromptParams p) {
            serviceId = p.serviceId;
            envLower = p.env.toLowerCase(Locale.US);
            namespace = p.namespace;
            domain = p.domain;
            localInfraRepoPath = p.localInfraRepoPath;
            codeContextPath = p.codeContextPath;
            mysqlHost = nvl(p.mysqlHost, "N/A");
            mysqlDb = nvl(p.mysqlDb, "N/A");
            atlasCluster = nvl(p.atlasCluster, "N/A");
            hpaType = nvl(p.hpaType, "none");
            kafkaGroups = nvl(p.kafkaConsumerGroups, "N/A");
        }
    }
}
