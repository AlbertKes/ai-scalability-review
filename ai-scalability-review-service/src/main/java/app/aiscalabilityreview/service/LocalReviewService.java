package app.aiscalabilityreview.service;

import app.aiscalabilityreview.api.localreview.GenerateLocalReviewRequest;
import app.aiscalabilityreview.api.localreview.GenerateLocalReviewResponse;
import app.aiscalabilityreview.job.LocalReviewJobExecutor;
import core.framework.async.Executor;
import core.framework.inject.Inject;
import core.framework.web.exception.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public class LocalReviewService {
    private final Logger logger = LoggerFactory.getLogger(LocalReviewService.class);
    @Inject
    ReviewService reviewService;
    @Inject
    LocalReviewJobExecutor localReviewJobExecutor;
    @Inject
    Executor executor;

    public GenerateLocalReviewResponse generate(GenerateLocalReviewRequest request) {
        validateLocalPaths(request);

        String jobId = reviewService.createLocalReviewJob(request.service, request.operator);
        String outputDir = LocalReviewJobExecutor.resolveOutputDir(request);

        localReviewJobExecutor.registerRequest(jobId, request);
        executor.submit("local-review-" + request.service, () -> {
            localReviewJobExecutor.executeLocalReview(jobId);
        });

        logger.info("Triggered local review job {} for service {}, outputDir={}", jobId, request.service, outputDir);

        GenerateLocalReviewResponse response = new GenerateLocalReviewResponse();
        response.jobId = jobId;
        response.outputDir = Path.of(outputDir).toAbsolutePath().toString();
        return response;
    }

    private void validateLocalPaths(GenerateLocalReviewRequest request) {
        Path appRepo = Path.of(request.localAppRepoPath);
        if (!appRepo.isAbsolute()) {
            throw new BadRequestException("local_app_repo_path must be an absolute path, got: " + request.localAppRepoPath);
        }
        if (!appRepo.toFile().exists()) {
            throw new BadRequestException("local_app_repo_path does not exist: " + request.localAppRepoPath);
        }

        Path infraRepo = Path.of(request.localInfraRepoPath);
        if (!infraRepo.isAbsolute()) {
            throw new BadRequestException("local_infra_repo_path must be an absolute path, got: " + request.localInfraRepoPath);
        }
        if (!infraRepo.toFile().exists()) {
            throw new BadRequestException("local_infra_repo_path does not exist: " + request.localInfraRepoPath);
        }
    }
}
