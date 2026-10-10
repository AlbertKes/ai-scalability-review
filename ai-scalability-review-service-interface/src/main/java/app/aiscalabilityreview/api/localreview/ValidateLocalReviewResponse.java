package app.aiscalabilityreview.api.localreview;

import core.framework.api.json.Property;

import java.util.List;

/**
 * Result of the pre-submit validation pass (FR-1.13).
 *
 * <p>Every rule is evaluated on every call, so the lists hold the complete set of problems rather
 * than the first one found. {@code valid} is true when there is no ERROR; warnings never block a
 * run, they describe a degradation the operator may knowingly accept.
 */
public class ValidateLocalReviewResponse {
    @Property(name = "valid")
    public Boolean valid;

    @Property(name = "errors")
    public List<IssueView> errors;

    @Property(name = "warnings")
    public List<IssueView> warnings;

    public static class IssueView {
        /** Request field this is about, in the JSON name the form uses; null for run-wide issues. */
        @Property(name = "field")
        public String field;

        @Property(name = "code")
        public String code;

        @Property(name = "severity")
        public String severity;  // ERROR | WARNING

        @Property(name = "message")
        public String message;

        @Property(name = "hint")
        public String hint;
    }
}
