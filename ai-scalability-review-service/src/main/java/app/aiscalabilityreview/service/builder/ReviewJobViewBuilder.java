package app.aiscalabilityreview.service.builder;

import app.aiscalabilityreview.api.review.ReviewJobView;
import app.aiscalabilityreview.api.review.ReviewJobView.FingerprintView;
import app.aiscalabilityreview.api.review.ReviewJobView.RunTotalsView;
import app.aiscalabilityreview.api.review.ReviewJobView.StageView;
import app.aiscalabilityreview.api.serviceconfig.embedded.AIModelView;
import app.aiscalabilityreview.domain.AppSettings;
import app.aiscalabilityreview.domain.ReviewJob;
import app.aiscalabilityreview.domain.ReviewJob.StageStatus;
import app.aiscalabilityreview.job.LocalReviewRunStats;
import app.aiscalabilityreview.job.LocalReviewStages;
import app.aiscalabilityreview.job.StageErrorHints;
import app.aiscalabilityreview.service.AppSettingsService;
import core.framework.inject.Inject;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Maps a stored run onto the shape the console renders. */
public class ReviewJobViewBuilder {
    @Inject
    AppSettingsService appSettingsService;

    public ReviewJobView build(ReviewJob job) {
        return build(job, appSettingsService.get());
    }

    /** Overload for list rendering, so one settings read serves a whole page. */
    public ReviewJobView build(ReviewJob job, AppSettings settings) {
        ReviewJobView view = new ReviewJobView();
        view.jobId = job.jobId;
        view.serviceId = job.serviceId;
        view.status = job.status;
        view.currentStage = job.currentStage;
        view.aiModel = job.aiModel == null ? null : AIModelView.valueOf(job.aiModel.name());
        view.triggerType = job.triggerType;
        view.triggeredBy = job.triggeredBy;
        view.note = job.note;
        view.labels = job.labels;
        view.baseline = job.baseline;
        view.startedAt = job.startedAt;
        view.completedAt = job.completedAt;
        view.cancelledAt = job.cancelledAt;
        view.cancelledBy = job.cancelledBy;
        view.reportId = job.reportId;
        view.errorMessage = job.errorMessage;
        view.env = job.env;
        view.namespace = job.namespace;
        view.domain = job.domain;
        view.outputDir = job.outputDir;
        view.request = LocalReviewRequestMapper.toView(job.request);
        view.acknowledgedWarnings = job.acknowledgedWarnings;
        view.overallScore = job.overallScore;
        view.validationVerdict = job.validationVerdict;
        view.validationResultId = job.validationResultId;
        view.totals = totals(job, settings);
        view.fingerprint = fingerprint(job);
        view.stages = stages(job);
        return view;
    }

    private RunTotalsView totals(ReviewJob job, AppSettings settings) {
        RunTotalsView totals = new RunTotalsView();
        totals.inputTokens = job.totalInputTokens;
        totals.outputTokens = job.totalOutputTokens;
        totals.cachedTokens = job.totalCachedTokens;
        totals.totalTokens = job.totalTokens;
        totals.durationMs = job.totalDurationMs;
        totals.mcpToolCalls = job.totalMcpToolCalls;
        totals.estimatedCostUsd = job.estimatedCostUsd;
        totals.withinBudget = job.withinBudget;
        long tokenBudget = settings.tokenBudget == null ? LocalReviewRunStats.TOKEN_BUDGET : settings.tokenBudget;
        long durationBudget = settings.durationBudgetMs == null
            ? LocalReviewRunStats.DURATION_BUDGET_MS : settings.durationBudgetMs;
        totals.tokenBudget = tokenBudget;
        totals.durationBudgetMs = durationBudget;
        totals.priceTableVersion = settings.priceTableVersion;
        if (job.totalInputTokens != null && job.totalCachedTokens != null && job.totalInputTokens > 0) {
            double ratio = Math.round(job.totalCachedTokens * 1000d / job.totalInputTokens) / 1000d;
            totals.cacheHitRatio = ratio;
        }
        return totals;
    }

    private FingerprintView fingerprint(ReviewJob job) {
        FingerprintView fingerprint = new FingerprintView();
        fingerprint.cliVersion = job.cliVersion;
        fingerprint.promptVersion = job.promptVersion;
        fingerprint.repoShaApp = job.repoShaApp;
        fingerprint.repoShaInfra = job.repoShaInfra;
        fingerprint.repoDirtyApp = job.repoDirtyApp;
        fingerprint.repoDirtyInfra = job.repoDirtyInfra;
        fingerprint.azureResourceGroup = job.azureResourceGroup;
        return fingerprint;
    }

    /**
     * Always returns all five stages in pipeline order, including the ones that have not started —
     * a timeline that grows a row at a time cannot show what is still to come.
     */
    private List<StageView> stages(ReviewJob job) {
        Set<String> names = new LinkedHashSet<>(LocalReviewStages.ORDER);
        if (job.stageStatuses != null) names.addAll(job.stageStatuses.keySet());
        List<StageView> stages = new ArrayList<>(names.size());
        for (String name : names) {
            StageStatus status = job.stageStatuses == null ? null : job.stageStatuses.get(name);
            stages.add(toView(name, status));
        }
        return stages;
    }

    private StageView toView(String name, StageStatus status) {
        StageView view = new StageView();
        view.name = name;
        if (status == null) {
            view.status = ReviewJob.STATUS_PENDING;
            return view;
        }
        view.status = status.status;
        view.startedAt = status.startedAt;
        view.completedAt = status.completedAt;
        view.durationMs = status.durationMs;
        view.error = status.errorMessage;
        view.errorHint = StageErrorHints.hint(status.errorMessage);
        view.outputSummary = status.outputSummary;
        view.model = status.model;
        view.inputTokens = status.inputTokens;
        view.outputTokens = status.outputTokens;
        view.cachedTokens = status.cachedTokens;
        view.totalTokens = status.totalTokens;
        view.apiRequests = status.apiRequests;
        view.toolCalls = status.toolCalls;
        view.mcpToolCalls = status.mcpToolCalls;
        view.estimatedCostUsd = status.estimatedCostUsd;
        view.artifacts = status.artifacts;
        view.logFile = status.logFile;
        view.sessionId = status.sessionId;
        return view;
    }
}
