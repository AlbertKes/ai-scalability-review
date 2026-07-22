package app.aiscalabilityreview.domain.embedded;

import core.framework.api.validate.NotBlank;
import core.framework.api.validate.NotNull;
import core.framework.mongo.Field;

public class ReviewConfig {
    @NotNull
    @NotBlank
    @Field(name = "schedule")
    public String schedule;

    @NotNull
    @Field(name = "ai_model")
    public AIModel aiModel;

    @NotNull
    @Field(name = "metric_lookback_days")
    public Integer metricLookbackDays = 30;

    @Field(name = "confluence_space_key")
    public String confluenceSpaceKey;

    @Field(name = "confluence_parent_page_title")
    public String confluenceParentPageTitle;

    @Field(name = "confluence_parent_page_id")
    public String confluenceParentPageId;
}
