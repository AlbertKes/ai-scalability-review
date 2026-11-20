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
            .contains("/out/metrics.md");
    }

    /** A resolved resource group must reach the prompt verbatim; env must never be offered as one. */
    @Test
    void metricCollectionPromptCarriesTheResolvedAzureNames() {
        MetricCollectionParams params = metricParams();
        params.azureResourceGroup = "FTIUat";
        params.azureSubscriptionId = "33a0344b-7e99-4718-938e-cb85df661a8c";

        assertThat(LocalReviewPromptBuilder.buildMetricCollectionPrompt(params))
            .doesNotContain("{{")
            .contains("**Resource group**: `FTIUat`")
            .contains("**Subscription**: `33a0344b-7e99-4718-938e-cb85df661a8c`")
            .contains("mysql_server_param_get")
            .contains("aks_nodepool_get")
            .contains("mysql_server_config_get")
            .contains("aks_cluster_get")
            // the invented names are named only as examples of what fails
            .contains("like `get_mysql_flexible_server` or `get_aks_node_pools` will simply fail");
    }

    /** Unresolved names must read as "discover it", never leave the model to guess from env. */
    @Test
    void metricCollectionPromptAsksForDiscoveryWhenTheAzureNamesAreUnresolved() {
        MetricCollectionParams params = metricParams();
        params.azureResourceGroup = null;
        params.azureSubscriptionId = null;

        assertThat(LocalReviewPromptBuilder.buildMetricCollectionPrompt(params))
            .doesNotContain("{{")
            .contains("**Resource group**: `unknown`")
            .contains("discover it, do not guess")
            .contains("is a Datadog tag value, not an Azure name");
    }

    /** The executor sets only what the Azure session needs; any other field being null must not break the prompt. */
    @Test
    void metricCollectionPromptBuildsFromTheFieldsTheExecutorSets() {
        MetricCollectionParams params = new MetricCollectionParams();
        params.serviceId = "wonder-cart-service";
        params.env = "prod";
        params.namespace = "prod-consumer";
        params.mysqlHost = "db";
        params.outputPath = "/out/azure-live.md";

        assertThat(LocalReviewPromptBuilder.buildMetricCollectionPrompt(params)).doesNotContain("{{").contains("/out/azure-live.md");
    }

    /** Datadog is read by the pipeline over REST, so the Azure session must not be told to query it. */
    @Test
    void metricCollectionPromptIsAzureOnly() {
        assertThat(LocalReviewPromptBuilder.buildMetricCollectionPrompt(metricParams()))
            .doesNotContain("{{")
            .doesNotContain("query-metrics")
            .doesNotContain("get-monitors")
            .doesNotContain("rollup(")
            .contains("mysql_server_param_get")
            .contains("{ \"mysql_max_connections\": null }");
    }

    @Test
    void scoringReferenceTreatsSharedMetricsAsRemindersAndIgnoresCircuitBreakers() {
        assertThat(MetricScorePrompt.CONTENT)
            .contains("Shared MySQL server — reminders, not scored")
            .contains("≥ 50 GB (**warning only — never RED**)")
            .contains("≤ 1,000 | 1,000 – 10,000 | > 10,000")
            .doesNotContain("| **Circuit breakers on critical paths**")
            .doesNotContain("| **Outbound HTTP client timeout configured**")
            .contains("tables > 1 GB")
            .doesNotContain("| **Replication lag — 28-day average**");
    }

    @Test
    void validationDataChecksCompareWithTheCollectedFilesAndQueryNothing() {
        assertThat(ValidationDataChecksPrompt.CONTENT)
            .contains("{{METRICS_PATH}}")
            .contains("{{AZURE_LIVE_PATH}}")
            .contains("Collection Gaps")
            .doesNotContain("query-metrics")
            .doesNotContain("list-slos")
            .doesNotContain("re-run the identical tool call");
    }

    @Test
    void recommendationSoundnessSkipsTheTwoOutOfScopeRecommendations() {
        assertThat(RecommendationSoundnessPrompt.CONTENT)
            .contains("explicit outbound HTTP client")
            .contains("distributed locking for periodic background Kafka message handlers")
            .contains("out of scope for now");
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

    /**
     * The static group is the only one that needs the scoring thresholds and the report format, and
     * it must be able to decide its checks without a data source or a repository.
     */
    @Test
    void staticValidationPromptCarriesTheThresholdsAndFormatItChecksAgainst() {
        String prompt = LocalReviewPromptBuilder.buildStaticValidationPrompt(validationParams());

        assertThat(prompt).doesNotContain("{{")
            .doesNotContain("ai-scalability-review-service/src/main/java")
            .contains("## Metric Scoring Reference")
            .contains("## Report Format Reference")
            .contains("@/out/2026-09-23-review.md")
            .contains("Check 3 — Cross-Source Metric Consistency")
            .contains("Check 7 — Format & Attribution Completeness")
            .contains("/out/validation-static.md");
    }

    /**
     * Keeping the 27 KB of scoring thresholds and report format out of the group that makes the tool
     * calls is most of what the split buys: in an agentic loop every byte of it would be re-sent on
     * each round-trip.
     */
    @Test
    void dataValidationPromptCarriesNeitherTheThresholdsNorTheReportFormat() {
        String prompt = LocalReviewPromptBuilder.buildDataValidationPrompt(validationParams());

        assertThat(prompt).doesNotContain("{{")
            .doesNotContain("## Metric Scoring Reference")
            .doesNotContain("## Report Format Reference")
            .doesNotContain("Check 8 — Recommendation Soundness")
            .contains("Check 1 — Config Accuracy")
            .contains("Check 2 — Metric Numerical Fidelity")
            .contains("Check 6 — NOT_COLLECTED Authenticity")
            .contains("/out/mysql-tables.md")
            .contains("@/out/metrics.md")
            .contains("@/out/azure-live.md")
            .contains("/infra")
            .contains("/out/validation-data.md");
    }

    /**
     * Check 8 re-verifies each recommendation against the primary source, so its group is the one
     * pointed at the application repository — and the only one, now that it no longer shares a
     * context with the checks that just re-read the report.
     */
    @Test
    void adviceValidationPromptChecksRecommendationSoundnessAgainstBothRepositories() {
        String prompt = LocalReviewPromptBuilder.buildAdviceValidationPrompt(validationParams());

        assertThat(prompt).doesNotContain("{{")
            .doesNotContain("Check 1 — Config Accuracy")
            .contains("Check 8 — Recommendation Soundness")
            .contains("/app-repo")
            .contains("/infra")
            .contains("/out/validation-advice.md");
    }

    /** No group may write the summary table or the verdict; the pipeline computes both. */
    @Test
    void everyValidationGroupIsToldToWriteItsCheckSectionsAndNothingElse() {
        ValidationPromptParams params = validationParams();

        assertThat(List.of(LocalReviewPromptBuilder.buildStaticValidationPrompt(params),
            LocalReviewPromptBuilder.buildDataValidationPrompt(params),
            LocalReviewPromptBuilder.buildAdviceValidationPrompt(params)))
            .allSatisfy(prompt -> assertThat(prompt)
                .contains("no summary table, no overall verdict")
                .contains("## Check "));
    }

    /** A validation run triggered without the application repository must still resolve every placeholder. */
    @Test
    void validationPromptToleratesAMissingApplicationRepository() {
        ValidationPromptParams params = validationParams();
        params.localAppRepoPath = null;

        assertThat(LocalReviewPromptBuilder.buildAdviceValidationPrompt(params))
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

    private MetricCollectionParams metricParams() {
        MetricCollectionParams params = new MetricCollectionParams();
        params.serviceId = "wonder-cart-service";
        params.env = "uat";
        params.namespace = "uat-consumer";
        params.mysqlHost = "N/A";
        params.mysqlDb = "N/A";
        params.mysqlTablesPath = "/out/mysql-tables.md";
        params.outputPath = "/out/metrics.md";
        return params;
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
        params.metricsPath = "/out/metrics.md";
        params.azureLivePath = "/out/azure-live.md";
        params.staticOutputPath = "/out/validation-static.md";
        params.dataOutputPath = "/out/validation-data.md";
        params.adviceOutputPath = "/out/validation-advice.md";
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
