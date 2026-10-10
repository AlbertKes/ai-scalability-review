package app.aiscalabilityreview.api.review;

import core.framework.api.validate.Min;
import core.framework.api.validate.NotNull;
import core.framework.api.web.service.QueryParam;

import java.time.ZonedDateTime;

/**
 * Filters for the run list (FR-2.2, FR-2.3).
 *
 * <p>{@code service_id} is optional so the console can show every run across every service; it used
 * to be mandatory, which made a global list impossible. Multi-value filters are comma-separated
 * because core-ng query parameters bind single scalars only.
 */
public class ListReviewJobRequest {
    @QueryParam(name = "service_id")
    public String serviceId;

    /** Comma-separated: PENDING,QUEUED,RUNNING,COMPLETED,FAILED,CANCELLED,STALE. */
    @QueryParam(name = "status")
    public String status;

    @QueryParam(name = "env")
    public String env;

    @QueryParam(name = "operator")
    public String operator;

    @QueryParam(name = "trigger_type")
    public String triggerType;

    /** Matches service id, job id, operator, note and label; see also {@code search_reports}. */
    @QueryParam(name = "keyword")
    public String keyword;

    /** Extends {@code keyword} to the body of the generated report (FR-2.5). */
    @QueryParam(name = "search_reports")
    public Boolean searchReports;

    @QueryParam(name = "from")
    public ZonedDateTime from;

    @QueryParam(name = "to")
    public ZonedDateTime to;

    /** Comma-separated: GREEN,YELLOW,RED,UNKNOWN. */
    @QueryParam(name = "overall_score")
    public String overallScore;

    /** Comma-separated: PASS,CONDITIONAL_PASS,FAIL. */
    @QueryParam(name = "validation_verdict")
    public String validationVerdict;

    @QueryParam(name = "within_budget")
    public Boolean withinBudget;

    @QueryParam(name = "has_failed_stage")
    public Boolean hasFailedStage;

    @QueryParam(name = "model")
    public String model;

    @QueryParam(name = "label")
    public String label;

    /** started_at | duration | tokens | cost, optionally suffixed with {@code :asc}. */
    @QueryParam(name = "sort")
    public String sort;

    @NotNull
    @Min(1)
    @QueryParam(name = "limit")
    public Integer limit;

    @NotNull
    @Min(0)
    @QueryParam(name = "skip")
    public Integer skip;
}
