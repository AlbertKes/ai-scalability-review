package app.aiscalabilityreview.domain;

import app.aiscalabilityreview.domain.embedded.Repositories;
import app.aiscalabilityreview.domain.embedded.ReviewConfig;
import app.aiscalabilityreview.domain.embedded.Runtime;
import app.aiscalabilityreview.domain.embedded.ServiceTier;
import core.framework.api.validate.NotBlank;
import core.framework.api.validate.NotNull;
import core.framework.mongo.Collection;
import core.framework.mongo.Field;
import core.framework.mongo.Id;

import java.time.ZonedDateTime;

@Collection(name = "service_configs")
public class ServiceConfig {
    @Id
    public String serviceId;

    @NotNull
    @NotBlank
    @Field(name = "display_name")
    public String displayName;

    @NotNull
    @NotBlank
    @Field(name = "team")
    public String team;

    @NotNull
    @Field(name = "tier")
    public ServiceTier tier;

    @NotNull
    @Field(name = "repositories")
    public Repositories repositories;

    @NotNull
    @Field(name = "review_config")
    public ReviewConfig reviewConfig;

    @NotNull
    @Field(name = "runtime")
    public Runtime runtime;

    @NotNull
    @Field(name = "enabled")
    public Boolean enabled;

    @NotNull
    @NotBlank
    @Field(name = "created_by")
    public String createdBy;

    @NotNull
    @Field(name = "created_time")
    public ZonedDateTime createdTime;

    @NotNull
    @NotBlank
    @Field(name = "updated_by")
    public String updatedBy;

    @NotNull
    @Field(name = "updated_time")
    public ZonedDateTime updatedTime;
}