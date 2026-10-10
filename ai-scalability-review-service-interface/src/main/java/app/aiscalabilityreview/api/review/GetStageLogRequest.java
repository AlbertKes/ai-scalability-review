package app.aiscalabilityreview.api.review;

import core.framework.api.validate.Min;
import core.framework.api.web.service.QueryParam;

/**
 * Tails one stage's captured CLI output.
 *
 * <p>Byte-offset polling rather than a push stream: the Gemini CLI is invoked with
 * {@code --output-format json} and emits nothing on stdout until the stage ends, so the only live
 * signal is its stderr chatter, and a cursor over that file is enough to follow it.
 */
public class GetStageLogRequest {
    @QueryParam(name = "stage")
    public String stage;

    @Min(0)
    @QueryParam(name = "offset")
    public Long offset;
}
