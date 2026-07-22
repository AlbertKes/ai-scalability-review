package app.aiscalabilityreview.job;

import app.aiscalabilityreview.domain.ReviewJob;
import app.aiscalabilityreview.domain.ReviewJob.StageStatus;
import app.aiscalabilityreview.domain.ServiceConfig;
import app.aiscalabilityreview.exception.FatalStageException;
import app.aiscalabilityreview.job.stage.AIScoringStage;
import app.aiscalabilityreview.job.stage.AzureMCPStage;
import app.aiscalabilityreview.job.stage.CodeFetchStage;
import app.aiscalabilityreview.job.stage.DatadogMetricsStage;
import app.aiscalabilityreview.job.stage.InfraConfigStage;
import app.aiscalabilityreview.job.stage.MySQLTableStage;
import app.aiscalabilityreview.job.stage.ReportPublishStage;
import app.aiscalabilityreview.job.stage.ReviewContext;
import app.aiscalabilityreview.service.ReviewService;
import app.aiscalabilityreview.service.ServiceConfigService;
import core.framework.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;

/**
 * Orchestrates the sequential execution of all review pipeline stages for a given job.
 * Each stage is executed in order; stage failures are caught and logged but do not
 * necessarily abort the pipeline (code/infra stages are non-fatal, AI stage is fatal).
 */
public class ReviewJobExecutor {
    private final Logger logger = LoggerFactory.getLogger(ReviewJobExecutor.class);
    @Inject
    ReviewService reviewService;
    @Inject
    ServiceConfigService serviceConfigService;
    @Inject
    CodeFetchStage codeFetchStage;
    @Inject
    InfraConfigStage infraConfigStage;
    @Inject
    AzureMCPStage azureMcpStage;
    @Inject
    DatadogMetricsStage datadogMetricsStage;
    @Inject
    MySQLTableStage mySQLTableStage;
    @Inject
    AIScoringStage aiScoringStage;
    @Inject
    ReportPublishStage reportPublishStage;

    /**
     * Execute a complete review pipeline for the specified job ID.
     * Marks the job RUNNING, executes stages in sequence, then marks COMPLETED or FAILED.
     *
     * @param jobId the ID of the ReviewJob to execute
     */
    public void executeReview(String jobId) {
        ReviewJob job = reviewService.getReviewJob(jobId)
                .orElseThrow(() -> new IllegalArgumentException("ReviewJob not found: " + jobId));

        ServiceConfig config = serviceConfigService.getServiceConfig(job.serviceId);

        ReviewContext context = new ReviewContext();
        context.job = job;
        context.config = config;

        reviewService.updateJobStatus(jobId, "RUNNING", "CODE_FETCH");
        logger.info("Starting review pipeline for job {} (service {})", jobId, job.serviceId);

        try {
            // Stage 1: Fetch and analyse application code
            runStage(context, "CODE_FETCH", () -> codeFetchStage.execute(context), false);

            // Stage 2: Fetch infrastructure configuration
            runStage(context, "INFRA_CONFIG", () -> infraConfigStage.execute(context), false);

            // Stage 3: Azure MCP resource config
            runStage(context, "AZURE_MCP", () -> azureMcpStage.execute(context), false);

            // Stage 4: Datadog metrics
            runStage(context, "DATADOG_METRICS", () -> datadogMetricsStage.execute(context), false);

            // Stage 5: MySQL table sizes (optional)
            runStage(context, "MYSQL_TABLES", () -> mySQLTableStage.execute(context), false);

            // Stage 6: AI scoring — this one is fatal if it fails
            runStage(context, "AI_SCORING", () -> aiScoringStage.execute(context), true);

            // Stage 7: Publish to Confluence
            runStage(context, "REPORT_PUBLISH", () -> reportPublishStage.execute(context), false);

            // Job was marked COMPLETED by AIScoringStage (which saves the reportId)
            logger.info("Review pipeline completed for job {} (service {})", jobId, job.serviceId);

        } catch (FatalStageException e) {
            logger.error("Fatal stage failure in job {} at stage {}: {}", jobId, e.stageName, e.getMessage());
            reviewService.failJob(jobId, "Stage " + e.stageName + " failed: " + e.getCause().getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error in review pipeline for job {}: {}", jobId, e.getMessage(), e);
            reviewService.failJob(jobId, "Unexpected error: " + e.getMessage());
        }
    }

    private void runStage(ReviewContext context, String stageName, StageAction action, boolean fatal) {
        reviewService.updateJobStatus(context.job.jobId, "RUNNING", stageName);

        StageStatus stageStatus = new StageStatus();
        stageStatus.status = "RUNNING";
        stageStatus.startedAt = ZonedDateTime.now();
        reviewService.updateJobStageStatus(context.job.jobId, stageName, stageStatus);

        long startMs = System.currentTimeMillis();
        try {
            action.execute();

            long durationMs = System.currentTimeMillis() - startMs;
            stageStatus.status = "COMPLETED";
            stageStatus.completedAt = ZonedDateTime.now();
            stageStatus.durationMs = durationMs;
            reviewService.updateJobStageStatus(context.job.jobId, stageName, stageStatus);

            logger.info("Stage {} completed in {}ms for job {}", stageName, durationMs, context.job.jobId);

        } catch (Exception e) {
            long durationMs = System.currentTimeMillis() - startMs;
            stageStatus.status = "FAILED";
            stageStatus.completedAt = ZonedDateTime.now();
            stageStatus.durationMs = durationMs;
            stageStatus.errorMessage = e.getMessage();
            reviewService.updateJobStageStatus(context.job.jobId, stageName, stageStatus);

            logger.error("Stage {} failed after {}ms for job {}: {}", stageName, durationMs, context.job.jobId, e.getMessage());

            if (fatal) {
                throw new FatalStageException(stageName, e);
            }
            // Non-fatal: log and continue
        }
    }

    @FunctionalInterface
    private interface StageAction {
        void execute() throws Exception;
    }
}