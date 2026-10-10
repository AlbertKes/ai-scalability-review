package app.aiscalabilityreview.domain.embedded;

import core.framework.mongo.Field;

/** Per-trigger notification switches; every one defaults to on except the outbound integrations. */
public class NotificationPrefs {
    @Field(name = "on_completed")
    public Boolean onCompleted;

    @Field(name = "on_failed")
    public Boolean onFailed;

    @Field(name = "on_cancelled")
    public Boolean onCancelled;

    @Field(name = "on_stage_failed")
    public Boolean onStageFailed;

    @Field(name = "on_over_budget")
    public Boolean onOverBudget;

    @Field(name = "on_validation_failed")
    public Boolean onValidationFailed;

    @Field(name = "on_red_score")
    public Boolean onRedScore;

    @Field(name = "on_stale")
    public Boolean onStale;

    @Field(name = "browser_enabled")
    public Boolean browserEnabled;

    @Field(name = "slack_enabled")
    public Boolean slackEnabled;
}
