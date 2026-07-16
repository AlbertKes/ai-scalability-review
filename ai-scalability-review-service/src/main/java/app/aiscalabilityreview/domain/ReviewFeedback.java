package app.aiscalabilityreview.domain;

import core.framework.mongo.Collection;
import core.framework.mongo.Field;
import core.framework.mongo.Id;

import java.time.ZonedDateTime;

@Collection(name = "review_feedback")
public class ReviewFeedback {
    @Id
    public String feedbackId;

    @Field(name = "service_id")
    public String serviceId;

    @Field(name = "report_id")
    public String reportId;

    @Field(name = "confluence_page_id")
    public String confluencePageId;

    @Field(name = "confluence_comment_id")
    public String confluenceCommentId;

    @Field(name = "author")
    public String author;

    @Field(name = "dimension")
    public String dimension;

    @Field(name = "raw_text")
    public String rawText;

    @Field(name = "parsed_correction")
    public String parsedCorrection;

    @Field(name = "status")
    public String status;  // PENDING | APPLIED | DISMISSED

    @Field(name = "applied_in_job_id")
    public String appliedInJobId;

    @Field(name = "created_at")
    public ZonedDateTime createdAt;

    @Field(name = "ingested_at")
    public ZonedDateTime ingestedAt;
}