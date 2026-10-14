package app.aiscalabilityreview;

import app.aiscalabilityreview.api.LocalReviewWebService;
import app.aiscalabilityreview.api.NotificationWebService;
import app.aiscalabilityreview.api.ReviewWebService;
import app.aiscalabilityreview.api.ServiceConfigWebService;
import app.aiscalabilityreview.api.SettingsWebService;
import app.aiscalabilityreview.job.ConfluenceFeedbackSyncJob;
import app.aiscalabilityreview.job.LocalReviewJobExecutor;
import app.aiscalabilityreview.job.LocalReviewRunFinalizer;
import app.aiscalabilityreview.job.LocalReviewRunPreparer;
import app.aiscalabilityreview.job.LocalReviewRunRegistry;
import app.aiscalabilityreview.job.LocalReviewStageRunner;
import app.aiscalabilityreview.job.LocalReviewValidationRunner;
import app.aiscalabilityreview.job.StaleRunDetector;
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
import app.aiscalabilityreview.service.AppSettingsService;
import app.aiscalabilityreview.service.ArtifactService;
import app.aiscalabilityreview.service.AuditLogService;
import app.aiscalabilityreview.service.AzureService;
import app.aiscalabilityreview.service.ConfluenceService;
import app.aiscalabilityreview.service.CostService;
import app.aiscalabilityreview.service.DatadogMetricProbe;
import app.aiscalabilityreview.service.DatadogService;
import app.aiscalabilityreview.service.GeminiCliService;
import app.aiscalabilityreview.service.FindingFeedbackService;
import app.aiscalabilityreview.service.GitHubService;
import app.aiscalabilityreview.service.GitRepoInspector;
import app.aiscalabilityreview.service.InfraContextService;
import app.aiscalabilityreview.service.MySQLTableStorageService;
import app.aiscalabilityreview.service.LocalReviewService;
import app.aiscalabilityreview.service.LocalReviewValidator;
import app.aiscalabilityreview.service.NotificationService;
import app.aiscalabilityreview.service.PreflightService;
import app.aiscalabilityreview.service.PresetService;
import app.aiscalabilityreview.service.ReviewJobQueryService;
import app.aiscalabilityreview.service.ReviewJobService;
import app.aiscalabilityreview.service.ReviewReportService;
import app.aiscalabilityreview.service.ValidationReportAssembler;
import app.aiscalabilityreview.service.ReviewService;
import app.aiscalabilityreview.service.RunComparisonService;
import app.aiscalabilityreview.service.ServiceConfigService;
import app.aiscalabilityreview.service.ServiceSummaryService;
import app.aiscalabilityreview.service.UsageAnalyticsService;
import app.aiscalabilityreview.service.builder.ReviewJobViewBuilder;
import app.aiscalabilityreview.web.ArtifactController;
import app.aiscalabilityreview.web.LocalReviewWebServiceImpl;
import app.aiscalabilityreview.web.NotificationWebServiceImpl;
import app.aiscalabilityreview.web.ReviewWebServiceImpl;
import app.aiscalabilityreview.web.ServiceConfigWebServiceImpl;
import app.aiscalabilityreview.web.SettingsWebServiceImpl;
import core.framework.http.HTTPMethod;
import core.framework.module.Module;

import java.time.Duration;

public class ReviewModule extends Module {
    @Override
    protected void initialize() {
        bindServices();
        bindStageExecutors();
        bindSchedule();
        bindAPIServices();
        bindArtifactRoutes();
        bindStartupChecks();
    }

    private void bindServices() {
        // bound first: the validator and the stale-run check both resolve it at bind time
        bind(LocalReviewRunRegistry.class);
        bind(ServiceConfigService.class);
        bind(ReviewService.class);
        bind(AnthropicService.class);
        bind(GitHubService.class);
        bind(DatadogService.class);
        bind(AzureService.class);
        bind(ConfluenceService.class);
        bind(AuditLogService.class);
        bind(ValidationReportAssembler.class);
        bind(ReviewReportService.class);
        bind(new GeminiCliService(requiredProperty("app.gemini.cloud.project"), requiredProperty("app.gemini.cloud.location")));
        bind(InfraContextService.class);
        bind(MySQLTableStorageService.class);
        bind(AppSettingsService.class);
        bind(CostService.class);
        bind(NotificationService.class);
        bind(GitRepoInspector.class);
        bind(DatadogMetricProbe.class);
        bind(PreflightService.class);
        bind(ArtifactService.class);
        bind(ReviewJobQueryService.class);
        bind(ReviewJobViewBuilder.class);
        bind(RunComparisonService.class);
        bind(UsageAnalyticsService.class);
        bind(ServiceSummaryService.class);
        bind(PresetService.class);
        bind(FindingFeedbackService.class);
        bind(LocalReviewValidator.class);
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
        bind(LocalReviewStageRunner.class);
        bind(LocalReviewRunPreparer.class);
        bind(LocalReviewRunFinalizer.class);
        bind(LocalReviewValidationRunner.class);
        bind(LocalReviewJobExecutor.class);
        bind(LocalReviewService.class);
        bind(StaleRunDetector.class);
    }

    private void bindSchedule() {
        schedule().fixedRate("scheduled-scalability-review-job", bind(ScheduledScalabilityReviewJob.class), Duration.ofHours(1));
        schedule().fixedRate("confluence-feedback-sync-job", bind(ConfluenceFeedbackSyncJob.class), Duration.ofHours(6));
    }

    private void bindAPIServices() {
        api().service(ServiceConfigWebService.class, bind(ServiceConfigWebServiceImpl.class));
        api().service(ReviewWebService.class, bind(ReviewWebServiceImpl.class));
        api().service(LocalReviewWebService.class, bind(LocalReviewWebServiceImpl.class));
        api().service(NotificationWebService.class, bind(NotificationWebServiceImpl.class));
        api().service(SettingsWebService.class, bind(SettingsWebServiceImpl.class));
    }

    /**
     * Artifact bytes are served by plain routes rather than by the typed API: the payload is a file,
     * not a bean, and the console needs to open a markdown artifact in a tab.
     */
    private void bindArtifactRoutes() {
        ArtifactController controller = bind(ArtifactController.class);
        http().route(HTTPMethod.GET, "/review/job/:jobId/artifact/:name", controller::download);
        http().route(HTTPMethod.GET, "/review/job/:jobId/artifacts.zip", controller::archive);
    }

    /**
     * A job left in flight by a restart has nothing running behind it; saying so at startup is what
     * keeps the run list honest instead of showing a spinner that never resolves.
     */
    private void bindStartupChecks() {
        StaleRunDetector detector = bean(StaleRunDetector.class);
        onStartup(detector::markAbandonedRuns);
    }
}