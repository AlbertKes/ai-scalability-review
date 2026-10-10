package app.aiscalabilityreview.api.review;

import core.framework.api.json.Property;

import java.time.ZonedDateTime;
import java.util.List;

public class ListFindingFeedbackResponse {
    @Property(name = "feedback")
    public List<FindingFeedbackView> feedback;

    public static class FindingFeedbackView {
        @Property(name = "feedback_id")
        public String feedbackId;

        @Property(name = "report_id")
        public String reportId;

        @Property(name = "job_id")
        public String jobId;

        @Property(name = "finding_key")
        public String findingKey;

        @Property(name = "finding_title")
        public String findingTitle;

        @Property(name = "verdict")
        public String verdict;

        @Property(name = "comment")
        public String comment;

        @Property(name = "author")
        public String author;

        @Property(name = "created_at")
        public ZonedDateTime createdAt;
    }
}
