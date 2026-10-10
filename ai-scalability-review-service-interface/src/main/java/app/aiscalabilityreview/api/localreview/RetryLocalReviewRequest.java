package app.aiscalabilityreview.api.localreview;

import core.framework.api.json.Property;
import core.framework.api.validate.NotBlank;
import core.framework.api.validate.NotNull;

/**
 * Re-runs an existing run, optionally from a later stage, reusing the artifacts of the stages before
 * it. A null {@code from_stage} means re-run everything.
 */
public class RetryLocalReviewRequest {
    /** STAGE_1_CODE_ANALYSIS … STAGE_5_VALIDATION, or null to start from the beginning. */
    @Property(name = "from_stage")
    public String fromStage;

    @NotNull
    @NotBlank
    @Property(name = "operator")
    public String operator;
}
