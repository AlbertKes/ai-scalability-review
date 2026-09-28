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
        params.mysqlTablesPath = "/out/mysql-tables.md";
        params.outputPath = "/out/metrics.md";

        String prompt = LocalReviewPromptBuilder.buildMetricCollectionPrompt(params);

        assertThat(prompt).doesNotContain("{{")
            .contains("**Environment**: prod")
            .contains("rfprodv2-flexible-wonder-db.mysql.database.azure.com")
            .contains("/out/metrics.md")
            .contains("**Atlas MongoDB cluster**: N/A");
    }

    /**
     * The table sizes are collected by the pipeline, not by an MCP server, so the stage must be told
     * to read the file rather than to look for a MySQL tool — see MySQLTableStorageService.
     */
    @Test
    void metricCollectionPromptPointsAtTheLocallyCollectedMysqlTables() {
        MetricCollectionParams params = new MetricCollectionParams();
        params.serviceId = "ba-web-api";
        params.env = "prod";
        params.namespace = "prod-ba";
        params.mysqlHost = "N/A";
        params.mysqlDb = "N/A";
        params.mysqlTablesPath = "/out/mysql-tables.md";
        params.outputPath = "/out/metrics.md";

        assertThat(LocalReviewPromptBuilder.buildMetricCollectionPrompt(params))
            .doesNotContain("{{")
            .contains("/out/mysql-tables.md")
            .contains("Do not query MySQL and do not look for a MySQL MCP server");
    }

    /** Every per-window figure has to be rolled up, both to be derivable and to keep the response small. */
    @Test
    void metricCollectionPromptRollsUpTheDatadogQueries() {
        MetricCollectionParams params = new MetricCollectionParams();
        params.serviceId = "wonder-cart-service";
        params.env = "uat";
        params.namespace = "uat-consumer";
        params.mysqlHost = "N/A";
        params.mysqlDb = "N/A";
        params.mysqlTablesPath = "/out/mysql-tables.md";
        params.outputPath = "/out/metrics.md";

        assertThat(LocalReviewPromptBuilder.buildMetricCollectionPrompt(params))
            .contains(".as_count().rollup(sum, 86400)")
            .contains(".as_count().rollup(sum, 3600)")
            .contains("`to - 2419200`")
            .contains("epoch milliseconds")
            .contains("get_monitors")
            .doesNotContain("list_monitors");
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
        String prompt = LocalReviewPromptBuilder.buildValidationPrompt(validationParams());

        assertThat(prompt).doesNotContain("{{")
            .doesNotContain("ai-scalability-review-service/src/main/java")
            .contains("## Metric Scoring Reference")
            .contains("## Report Format Reference")
            .contains("## Validation Format Reference")
            .contains("@/out/2026-09-23-review.md")
            .contains("/out/mysql-tables.md")
            .contains("/out/2026-09-23-validation.md");
    }

    /**
     * Check 8 re-verifies each recommendation against the primary source, so the validation stage has
     * to be pointed at the application repository as well as the infra one.
     */
    @Test
    void validationPromptChecksRecommendationSoundnessAgainstBothRepositories() {
        ValidationPromptParams params = validationParams();

        assertThat(LocalReviewPromptBuilder.buildValidationPrompt(params))
            .doesNotContain("{{")
            .contains("Check 8 — Recommendation Soundness")
            .contains("Execute all 8 checks")
            .contains("/app-repo")
            .contains("/infra");
    }

    /** A validation run triggered without the application repository must still resolve every placeholder. */
    @Test
    void validationPromptToleratesAMissingApplicationRepository() {
        ValidationPromptParams params = validationParams();
        params.localAppRepoPath = null;

        assertThat(LocalReviewPromptBuilder.buildValidationPrompt(params))
            .doesNotContain("{{")
            .contains("not provided");
    }

    /** The synthesis stage has to be told the rules Check 8 will hold its recommendations to. */
    @Test
    void reviewPromptCarriesTheRecommendationSoundnessRules() {
        assertThat(LocalReviewPromptBuilder.buildReviewPrompt(reviewParams()))
            .contains("Cite the primary source, not the context file")
            .contains("No recommendation built on a metric that was not collected")
            .contains("Do not recommend what already exists");
    }

    @Test
    void mysqlConnectionNameStripsTheDomainAndHandlesAbsentValues() {
        assertThat(LocalReviewPromptBuilder.mysqlConnectionName("host.mysql.database.azure.com", "order")).isEqualTo("host-order");
        assertThat(LocalReviewPromptBuilder.mysqlConnectionName("host", "order")).isEqualTo("host-order");
        assertThat(LocalReviewPromptBuilder.mysqlConnectionName("N/A", "order")).isNull();
        assertThat(LocalReviewPromptBuilder.mysqlConnectionName("host", null)).isNull();
    }

    private ValidationPromptParams validationParams() {
        ValidationPromptParams params = new ValidationPromptParams();
        params.serviceId = "wonder-cart-service";
        params.reviewReportPath = "/out/2026-09-23-review.md";
        params.infraContextPath = "/out/infra-context.md";
        params.localInfraRepoPath = "/infra";
        params.localAppRepoPath = "/app-repo";
        params.env = "prod";
        params.namespace = "prod-consumer";
        params.mysqlHost = "db-host";
        params.mysqlDb = "wonder_cart";
        params.atlasCluster = null;
        params.kafkaConsumerGroups = "wonder-cart-service";
        params.mysqlTablesPath = "/out/mysql-tables.md";
        params.outputPath = "/out/2026-09-23-validation.md";
        return params;
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
