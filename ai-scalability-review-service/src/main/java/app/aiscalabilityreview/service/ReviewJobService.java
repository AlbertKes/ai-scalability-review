package app.aiscalabilityreview.service;

import app.aiscalabilityreview.api.review.GetReviewJobResponse;
import app.aiscalabilityreview.api.review.ListReviewJobRequest;
import app.aiscalabilityreview.api.review.ListReviewJobResponse;
import app.aiscalabilityreview.api.review.ReviewJobView;
import app.aiscalabilityreview.api.review.TriggerReviewRequest;
import app.aiscalabilityreview.api.review.TriggerReviewResponse;
import app.aiscalabilityreview.domain.ReviewJob;
import app.aiscalabilityreview.domain.embedded.AIModel;
import app.aiscalabilityreview.job.ReviewJobExecutor;
import app.aiscalabilityreview.domain.AppSettings;
import app.aiscalabilityreview.service.builder.ReviewJobViewBuilder;
import core.framework.async.Executor;
import core.framework.inject.Inject;
import core.framework.util.Strings;
import core.framework.web.exception.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ReviewJobService {
    private final Logger logger = LoggerFactory.getLogger(ReviewJobService.class);
    @Inject
    ReviewService reviewService;
    @Inject
    ServiceConfigService serviceConfigService;
    @Inject
    ReviewJobExecutor reviewJobExecutor;
    @Inject
    Executor executor;
    @Inject
    ReviewJobQueryService reviewJobQueryService;
    @Inject
    ReviewJobViewBuilder reviewJobViewBuilder;
    @Inject
    AppSettingsService appSettingsService;

    public TriggerReviewResponse trigger(TriggerReviewRequest request) {
        serviceConfigService.getServiceConfig(request.serviceId);
        String jobId = reviewService.createReviewJob(request.serviceId, "MANUAL",
            request.model == null ? null : AIModel.valueOf(request.model.name()), request.note);
        executor.submit("scalability-review-" + request.serviceId, () -> {
            reviewJobExecutor.executeReview(jobId);
        });
        logger.info("Triggered review job {} for service {}", jobId, request.serviceId);
        var response = new TriggerReviewResponse();
        response.jobId = jobId;
        return response;
    }

    public GetReviewJobResponse get(String jobId) {
        ReviewJob job = reviewService.getReviewJob(jobId).orElseThrow(() -> new
            NotFoundException(Strings.format("Job not found, id = {} ", jobId)));
        var response = new GetReviewJobResponse();
        response.view = reviewJobViewBuilder.build(job);
        return response;
    }

    /**
     * One settings read serves the whole page: the budgets and the price-table version are the same
     * for every row, and re-reading them per row turns a list into a burst of identical queries.
     */
    public ListReviewJobResponse list(ListReviewJobRequest request) {
        AppSettings settings = appSettingsService.get();
        List<ReviewJob> jobs = reviewJobQueryService.list(request);
        List<ReviewJobView> views = jobs.stream().map(job -> reviewJobViewBuilder.build(job, settings)).toList();
        var response = new ListReviewJobResponse();
        response.jobs = views;
        response.total = reviewJobQueryService.count(request);
        return response;
    }
}
