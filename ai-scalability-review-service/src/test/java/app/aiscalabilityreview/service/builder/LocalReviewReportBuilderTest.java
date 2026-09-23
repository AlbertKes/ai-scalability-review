package app.aiscalabilityreview.service.builder;

import app.aiscalabilityreview.domain.ReviewReport;
import app.aiscalabilityreview.domain.embedded.AIModel;
import app.aiscalabilityreview.service.GeminiModels;
import app.aiscalabilityreview.service.builder.LocalReviewReportBuilder.LocalReportParams;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LocalReviewReportBuilderTest {
    private static final String SNAPSHOT = """
        {
          "traffic_score": "GREEN",
          "latency_score": "YELLOW",
          "errors_score": "GREEN",
          "resources_score": "GREEN",
          "persistence_score": "RED",
          "overall_score": "RED",
          "avg_rps": 41.7,
          "p99_latency_ms": 820.5,
          "error_rate_pct": 0.12,
          "cpu_avg_pct": 23.4,
          "mem_avg_pct": 48.1,
          "mysql_connection_pct": 78.9,
          "mysql_replication_lag_s": 1.5
        }
        """;

    private static final String MARKDOWN = """
        ## 4. Scalability Assessment & Scoring

        ### Dimension 1 — Traffic & Throughput: GREEN
        ### Dimension 2 — Latency & Thread Concurrency: YELLOW
        ### Dimension 3 — Error Rates & Stability: GREEN
        ### Dimension 4 — Resource Saturation: GREEN
        ### Dimension 5 — Persistence Layer: RED

        ## 6. Recommendations & Action Items

        ### 1. Raise max_connections (High Priority)
        ### 2. Add an index on cart_items.owner_id (High Priority)
        ### 3. Tighten the outbound timeout (Medium Priority)
        ### 4. Document the scaling justification (Low Priority)
        """;

    @Test
    void takesScoresAndMetricsFromTheSnapshot() {
        ReviewReport report = LocalReviewReportBuilder.reviewReport(new LocalReportParams(
            "job-1", "wonder-cart-service", MARKDOWN, "/out/2026-09-23-review.md", SNAPSHOT, GeminiModels.FLASH));

        assertThat(report.trafficScore).isEqualTo("GREEN");
        assertThat(report.latencyScore).isEqualTo("YELLOW");
        assertThat(report.persistenceScore).isEqualTo("RED");
        assertThat(report.overallScore).isEqualTo("RED");
        assertThat(report.avgRps).isEqualTo(41.7);
        assertThat(report.p99LatencyMs).isEqualTo(820.5);
        assertThat(report.mysqlConnectionPct).isEqualTo(78.9);
        assertThat(report.aiModel).isEqualTo(AIModel.GEMINI_3_5_FLASH);
        assertThat(report.confluencePageUrl).isEqualTo("file:///out/2026-09-23-review.md");
    }

    @Test
    void countsRecommendationsByPriorityMarker() {
        ReviewReport report = LocalReviewReportBuilder.reviewReport(new LocalReportParams(
            "job-1", "wonder-cart-service", MARKDOWN, "/out/review.md", SNAPSHOT, GeminiModels.FLASH));

        assertThat(report.recommendationsHigh).isEqualTo(2);
        assertThat(report.recommendationsMedium).isEqualTo(1);
        assertThat(report.recommendationsLow).isEqualTo(1);
    }

    @Test
    void fallsBackToTheDimensionHeadingsWhenTheSnapshotIsMissing() {
        ReviewReport report = LocalReviewReportBuilder.reviewReport(new LocalReportParams(
            "job-1", "wonder-cart-service", MARKDOWN, "/out/review.md", null, GeminiModels.FLASH_LITE));

        assertThat(report.trafficScore).isEqualTo("GREEN");
        assertThat(report.latencyScore).isEqualTo("YELLOW");
        assertThat(report.persistenceScore).isEqualTo("RED");
        assertThat(report.overallScore).isEqualTo("RED");
        assertThat(report.avgRps).isNull();
        assertThat(report.aiModel).isEqualTo(AIModel.GEMINI_3_1_FLASH_LITE);
    }

    @Test
    void reportsUnknownRatherThanGreenWhenNoScoreCouldBeRead() {
        ReviewReport report = LocalReviewReportBuilder.reviewReport(new LocalReportParams(
            "job-1", "wonder-cart-service", "# Scalability Review: wonder-cart-service (prod)",
            "/out/review.md", null, GeminiModels.FLASH));

        assertThat(report.trafficScore).isEqualTo(LocalReviewReportBuilder.UNKNOWN);
        assertThat(report.overallScore).isEqualTo(LocalReviewReportBuilder.UNKNOWN);
        assertThat(report.recommendationsHigh).isZero();
    }

    @Test
    void ignoresAnUnparseableSnapshotInsteadOfFailingTheRun() {
        ReviewReport report = LocalReviewReportBuilder.reviewReport(new LocalReportParams(
            "job-1", "wonder-cart-service", MARKDOWN, "/out/review.md", "{not json", GeminiModels.FLASH));

        assertThat(report.trafficScore).isEqualTo("GREEN");
        assertThat(report.avgRps).isNull();
    }
}
