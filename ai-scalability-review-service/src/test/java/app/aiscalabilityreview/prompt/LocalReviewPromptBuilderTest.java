package app.aiscalabilityreview.prompt;

import app.aiscalabilityreview.prompt.LocalReviewPromptBuilder.MetricCollectionParams;
import app.aiscalabilityreview.prompt.LocalReviewPromptBuilder.ReviewPromptParams;
import app.aiscalabilityreview.prompt.LocalReviewPromptBuilder.ValidationPromptParams;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LocalReviewPromptBuilderTest {
    @Test
    void codeAnalysisPromptInjectsOnlyTheRequestedPaths() {
        String prompt = LocalReviewPromptBuilder.buildCodeAnalysisPrompt("wonder-cart-service", "/repo",
            List.of("/backend/wonder-cart-service/", "backend/wonder-cart-service-interface"), "/out/code-context.md");

        assertThat(prompt).doesNotContain("{{")
            .contains("@/repo/backend/wonder-cart-service\n")
            .contains("@/repo/backend/wonder-cart-service-interface")
            .contains("/out/code-context.md");
    }

    @Test
    void codeAnalysisPromptFallsBackToTheWholeRepository() {
        String prompt = LocalReviewPromptBuilder.buildCodeAnalysisPrompt("wonder-cart-service", "/repo", null, "/out/code-context.md");

        assertThat(prompt).doesNotContain("{{").contains("@/repo");
    }

    @Test
    void metricCollectionPromptResolvesEveryPlaceholder() {
        MetricCollectionParams params = new MetricCollectionParams();
        params.serviceId = "wonder-cart-service";
        params.env = "PROD";
        params.namespace = "prod-consumer";
        params.mysqlHost = "rfprodv2-flexible-wonder-db.mysql.database.azure.com";
        params.mysqlDb = "wonder_cart";
        params.atlasCluster = null;
        params.kafkaConsumerGroups = null;
        params.outputPath = "/out/metrics.md";

        String prompt = LocalReviewPromptBuilder.buildMetricCollectionPrompt(params);

        assertThat(prompt).doesNotContain("{{")
            .contains("**Environment**: prod")
            .contains("rfprodv2-flexible-wonder-db-wonder_cart")
            .contains("/out/metrics.md")
            .contains("**Atlas MongoDB cluster**: N/A");
    }

    @Test
    void metricCollectionPromptNamesNoMysqlMcpWhenTheDatabaseIsAbsent() {
        MetricCollectionParams params = new MetricCollectionParams();
        params.serviceId = "ba-web-api";
        params.env = "prod";
        params.namespace = "prod-ba";
        params.mysqlHost = "N/A";
        params.mysqlDb = "N/A";
        params.outputPath = "/out/metrics.md";

        assertThat(LocalReviewPromptBuilder.buildMetricCollectionPrompt(params))
            .doesNotContain("{{")
            .contains("not configured for this service");
    }

    @Test
    void reviewPromptCarriesTheReferencesTheSynthesisStageNeeds() {
        String prompt = LocalReviewPromptBuilder.buildReviewPrompt(reviewParams());

        assertThat(prompt).doesNotContain("{{")
            .contains("@/out/infra-context.md")
            .contains("@/out/code-context.md")
            .contains("@/out/metrics.md")
            .contains("## Metric Scoring Reference")
            .contains("## Report Format Reference")
            .contains("## Review Coverage Checklist")
            .contains("Service Dependency Review")
            .contains("(High Priority)")
            .contains("/out/2026-09-23-review.md")
            .contains("/out/report-snapshot.json");
    }

    @Test
    void reviewPromptDoesNotPointAtThisProjectSourceTree() {
        assertThat(LocalReviewPromptBuilder.buildReviewPrompt(reviewParams()))
            .doesNotContain("ai-scalability-review-service/src/main/java")
            .doesNotContain("src/main/resources/doc/scalability-review-checkpoint.md");
    }

    @Test
    void validationPromptCarriesTheThresholdsAndFormatsItChecksAgainst() {
        ValidationPromptParams params = new ValidationPromptParams();
        params.serviceId = "wonder-cart-service";
        params.reviewReportPath = "/out/2026-09-23-review.md";
        params.infraContextPath = "/out/infra-context.md";
        params.localInfraRepoPath = "/infra";
        params.env = "prod";
        params.namespace = "prod-consumer";
        params.mysqlHost = "db-host";
        params.mysqlDb = "wonder_cart";
        params.atlasCluster = null;
        params.kafkaConsumerGroups = "wonder-cart-service";
        params.outputPath = "/out/2026-09-23-validation.md";

        String prompt = LocalReviewPromptBuilder.buildValidationPrompt(params);

        assertThat(prompt).doesNotContain("{{")
            .doesNotContain("ai-scalability-review-service/src/main/java")
            .contains("## Metric Scoring Reference")
            .contains("## Report Format Reference")
            .contains("## Validation Format Reference")
            .contains("@/out/2026-09-23-review.md")
            .contains("/out/2026-09-23-validation.md");
    }

    @Test
    void mysqlMcpNameStripsTheDomainAndHandlesAbsentValues() {
        assertThat(LocalReviewPromptBuilder.mysqlMcpName("host.mysql.database.azure.com", "order")).isEqualTo("host-order");
        assertThat(LocalReviewPromptBuilder.mysqlMcpName("host", "order")).isEqualTo("host-order");
        assertThat(LocalReviewPromptBuilder.mysqlMcpName("N/A", "order")).isNull();
        assertThat(LocalReviewPromptBuilder.mysqlMcpName("host", null)).isNull();
    }

    private ReviewPromptParams reviewParams() {
        ReviewPromptParams params = new ReviewPromptParams();
        params.serviceId = "wonder-cart-service";
        params.codeContextPath = "/out/code-context.md";
        params.infraContextPath = "/out/infra-context.md";
        params.metricsPath = "/out/metrics.md";
        params.env = "prod";
        params.namespace = "prod-consumer";
        params.mysqlHost = "db-host";
        params.mysqlDb = "wonder_cart";
        params.atlasCluster = null;
        params.hpaType = "HPA";
        params.kafkaConsumerGroups = "wonder-cart-service";
        params.outputPath = "/out/2026-09-23-review.md";
        params.snapshotPath = "/out/report-snapshot.json";
        return params;
    }
}
