package app.aiscalabilityreview.api.review;

import core.framework.api.json.Property;
import core.framework.api.validate.NotBlank;
import core.framework.api.validate.NotNull;

/** A reader's verdict on one finding (FR-6.10) — the ground truth for measuring report quality. */
public class SaveFindingFeedbackRequest {
    @Property(name = "job_id")
    public String jobId;

    @NotNull
    @NotBlank
    @Property(name = "finding_key")
    public String findingKey;

    @Property(name = "finding_title")
    public String findingTitle;

    @NotNull
    @NotBlank
    @Property(name = "verdict")
    public String verdict;  // CONFIRMED | FALSE_POSITIVE | WONT_FIX

    @Property(name = "comment")
    public String comment;

    @NotNull
    @NotBlank
    @Property(name = "author")
    public String author;
}
