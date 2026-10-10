package app.aiscalabilityreview.domain;

import app.aiscalabilityreview.domain.embedded.LocalReviewRequestSnapshot;
import core.framework.mongo.Collection;
import core.framework.mongo.Field;
import core.framework.mongo.Id;

import java.time.ZonedDateTime;

/** A named, reusable set of trigger parameters for one service and environment. */
@Collection(name = "review_presets")
public class ReviewPreset {
    @Id
    public String presetId;

    @Field(name = "service_id")
    public String serviceId;

    @Field(name = "env")
    public String env;

    @Field(name = "name")
    public String name;

    @Field(name = "request")
    public LocalReviewRequestSnapshot request;

    @Field(name = "is_default")
    public Boolean isDefault;

    @Field(name = "created_by")
    public String createdBy;

    @Field(name = "created_at")
    public ZonedDateTime createdAt;

    @Field(name = "updated_at")
    public ZonedDateTime updatedAt;
}
