package app.aiscalabilityreview.web;

import app.aiscalabilityreview.domain.ReviewJob;
import app.aiscalabilityreview.service.ArtifactService;
import app.aiscalabilityreview.service.ReviewService;
import core.framework.http.ContentType;
import core.framework.inject.Inject;
import core.framework.log.ActionLogContext;
import core.framework.web.Request;
import core.framework.web.Response;
import core.framework.web.exception.NotFoundException;

import java.nio.file.Path;

/**
 * Serves artifact bytes.
 *
 * <p>Raw routes rather than typed API methods because the payload is a file, not a bean. Every path
 * is resolved by {@link ArtifactService} inside the run's own directory: the console has no login
 * precisely because it is bound to localhost, which is also why a path parameter must never be able
 * to address something outside the run.
 */
public class ArtifactController {
    @Inject
    ReviewService reviewService;
    @Inject
    ArtifactService artifactService;

    public Response download(Request request) {
        ReviewJob job = job(request.pathParam("jobId"));
        String name = request.pathParam("name");
        Path path = artifactService.resolve(job, name);
        return Response.file(path).contentType(contentType(name));
    }

    public Response archive(Request request) {
        ReviewJob job = job(request.pathParam("jobId"));
        byte[] zip = artifactService.zip(job);
        return Response.bytes(zip)
            .contentType(ContentType.APPLICATION_OCTET_STREAM)
            .header("Content-Disposition", "attachment; filename=\"" + job.serviceId + "-" + job.jobId + ".zip\"");
    }

    private ContentType contentType(String name) {
        if (name.endsWith(".json")) return ContentType.APPLICATION_JSON;
        if (name.endsWith(".md") || name.endsWith(".log")) return ContentType.TEXT_PLAIN;
        return ContentType.APPLICATION_OCTET_STREAM;
    }

    private ReviewJob job(String jobId) {
        ActionLogContext.put("review_job_id", jobId);
        return reviewService.getReviewJob(jobId).orElseThrow(() -> new NotFoundException("Run not found: " + jobId));
    }
}
