package app.aiscalabilityreview.api.localreview;

import core.framework.api.web.service.QueryParam;

/**
 * Everything the environment doctor needs. All fields are optional: with none of them the checks
 * cover only the machine (CLI, credentials, MCP servers, MongoDB); with them the repository and
 * data-source checks run too.
 */
public class PreflightRequest {
    @QueryParam(name = "service")
    public String service;

    @QueryParam(name = "env")
    public String env;

    @QueryParam(name = "domain")
    public String domain;

    @QueryParam(name = "local_app_repo_path")
    public String localAppRepoPath;

    @QueryParam(name = "local_infra_repo_path")
    public String localInfraRepoPath;

    @QueryParam(name = "output_base_dir")
    public String outputBaseDir;

    @QueryParam(name = "mysql_host")
    public String mysqlHost;

    @QueryParam(name = "mysql_db")
    public String mysqlDB;
}
