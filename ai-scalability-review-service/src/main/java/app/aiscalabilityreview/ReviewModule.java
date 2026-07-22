package app.aiscalabilityreview;

import app.aiscalabilityreview.api.LocalReviewWebService;
import app.aiscalabilityreview.api.ReviewWebService;
import app.aiscalabilityreview.api.ServiceConfigWebService;
import app.aiscalabilityreview.job.ConfluenceFeedbackSyncJob;
import app.aiscalabilityreview.job.LocalReviewJobExecutor;
import app.aiscalabilityreview.job.ReviewJobExecutor;
import app.aiscalabilityreview.job.ScheduledScalabilityReviewJob;
import app.aiscalabilityreview.job.stage.AIScoringStage;
import app.aiscalabilityreview.job.stage.AzureMCPStage;
import app.aiscalabilityreview.job.stage.CodeFetchStage;
import app.aiscalabilityreview.job.stage.DatadogMetricsStage;
import app.aiscalabilityreview.job.stage.InfraConfigStage;
import app.aiscalabilityreview.job.stage.MySQLTableStage;
import app.aiscalabilityreview.job.stage.ReportPublishStage;
import app.aiscalabilityreview.service.AnthropicService;
import app.aiscalabilityreview.service.AuditLogService;
import app.aiscalabilityreview.service.AzureService;
import app.aiscalabilityreview.service.ConfluenceService;
import app.aiscalabilityreview.service.DatadogService;
import app.aiscalabilityreview.service.GeminiCliService;
import app.aiscalabilityreview.service.GitHubService;
import app.aiscalabilityreview.service.LocalReviewService;
import app.aiscalabilityreview.service.ReviewJobService;
import app.aiscalabilityreview.service.ReviewReportService;
import app.aiscalabilityreview.service.ReviewService;
import app.aiscalabilityreview.service.ServiceConfigService;
import app.aiscalabilityreview.web.LocalReviewWebServiceImpl;
import app.aiscalabilityreview.web.ReviewWebServiceImpl;
import app.aiscalabilityreview.web.ServiceConfigWebServiceImpl;
import core.framework.module.Module;

import java.time.Duration;

public class ReviewModule extends Module {
    @Override
    protected void initialize() {
        bindServices();
        bindStageExecutors();
        bindSchedule();
        bindAPIServices();
    }

    private void bindServices() {
        bind(ServiceConfigService.class);
        bind(ReviewService.class);
        bind(AnthropicService.class);
        bind(GitHubService.class);
        bind(DatadogService.class);
        bind(AzureService.class);
        bind(ConfluenceService.class);
        bind(AuditLogService.class);
        bind(ReviewReportService.class);
        bind(GeminiCliService.class);
    }

    private void bindStageExecutors() {
        bind(CodeFetchStage.class);
        bind(InfraConfigStage.class);
        bind(AzureMCPStage.class);
        bind(DatadogMetricsStage.class);
        bind(MySQLTableStage.class);
        bind(AIScoringStage.class);
        bind(ReportPublishStage.class);
        bind(ReviewJobExecutor.class);
        bind(ReviewJobService.class);
        bind(LocalReviewJobExecutor.class);
        bind(LocalReviewService.class);
    }

    private void bindSchedule() {
        schedule().fixedRate("scheduled-scalability-review-job", bind(ScheduledScalabilityReviewJob.class), Duration.ofHours(1));
        schedule().fixedRate("confluence-feedback-sync-job", bind(ConfluenceFeedbackSyncJob.class), Duration.ofHours(6));
    }

    private void bindAPIServices() {
        api().service(ServiceConfigWebService.class, bind(ServiceConfigWebServiceImpl.class));
        api().service(ReviewWebService.class, bind(ReviewWebServiceImpl.class));
        api().service(LocalReviewWebService.class, bind(LocalReviewWebServiceImpl.class));
    }
}