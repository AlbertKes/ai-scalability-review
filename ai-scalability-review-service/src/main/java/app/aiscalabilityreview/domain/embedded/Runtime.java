package app.aiscalabilityreview.domain.embedded;

import core.framework.api.validate.NotBlank;
import core.framework.api.validate.NotNull;
import core.framework.mongo.Field;

import java.util.List;

public class Runtime {
    @NotNull
    @Field(name = "environment")
    public Environment environment;

    @NotNull
    @NotBlank
    @Field(name = "namespace")
    public String namespace;

    @NotNull
    @Field(name = "deployments")
    public List<DeploymentConfig> deployments;

    @NotNull
    @Field(name = "hpa_type")
    public HPAType hpaType;

    @Field(name = "kafka_consumer_groups")
    public List<String> kafkaConsumerGroups;

    @Field(name = "mysql_host")
    public String mysqlHost;

    @Field(name = "mysql_db")
    public String mysqlDB;

    @Field(name = "atlas_cluster")
    public String atlasCluster;

    @Field(name = "redis_enabled")
    public Boolean redisEnabled;

    @NotNull
    @Field(name = "domain")
    public String domain;

    public static class DeploymentConfig {
        @NotNull
        @NotBlank
        @Field(name = "name")
        public String name;

        @Field(name = "type")
        public String type;
    }
}
