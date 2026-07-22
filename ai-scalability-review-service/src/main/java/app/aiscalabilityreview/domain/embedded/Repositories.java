package app.aiscalabilityreview.domain.embedded;

import core.framework.api.validate.NotBlank;
import core.framework.api.validate.NotNull;
import core.framework.mongo.Field;

import java.util.List;

public class Repositories {
    @NotNull
    @Field(name = "app")
    public RepoConfig app;

    @Field(name = "infra")
    public RepoConfig infra;

    @Field(name = "k8s_gitops")
    public RepoConfig k8sGitops;

    public static class RepoConfig {
        @NotNull
        @NotBlank
        @Field(name = "url")
        public String url;

        @NotNull
        @NotBlank
        @Field(name = "branch")
        public String branch;

        @Field(name = "service_path")
        public String servicePath;

        @Field(name = "include_paths")
        public List<String> includePaths;

        @Field(name = "exclude_paths")
        public List<String> excludePaths;
    }
}
