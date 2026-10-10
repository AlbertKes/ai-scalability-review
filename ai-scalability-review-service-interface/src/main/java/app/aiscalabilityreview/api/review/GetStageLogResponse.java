package app.aiscalabilityreview.api.review;

import core.framework.api.json.Property;

public class GetStageLogResponse {
    @Property(name = "stage")
    public String stage;

    @Property(name = "content")
    public String content;

    @Property(name = "next_offset")
    public Long nextOffset;

    @Property(name = "size")
    public Long size;

    /** True once the stage has finished, so the client can stop polling. */
    @Property(name = "complete")
    public Boolean complete;
}
