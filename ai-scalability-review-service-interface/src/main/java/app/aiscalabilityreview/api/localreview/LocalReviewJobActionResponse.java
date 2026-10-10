package app.aiscalabilityreview.api.localreview;

import core.framework.api.json.Property;

/** Outcome of a cancel / delete / update action on one run. */
public class LocalReviewJobActionResponse {
    @Property(name = "job_id")
    public String jobId;

    @Property(name = "status")
    public String status;

    @Property(name = "message")
    public String message;
}
