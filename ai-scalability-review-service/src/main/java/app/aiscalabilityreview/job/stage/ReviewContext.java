package app.aiscalabilityreview.job.stage;

import app.aiscalabilityreview.domain.ReviewJob;
import app.aiscalabilityreview.domain.ServiceConfig;

import java.util.HashMap;
import java.util.Map;

/**
 * Data holder passed between pipeline stages during a review run.
 * Accumulates all collected data: code, infra configs, metrics, AI outputs.
 */
public class ReviewContext {
    /** The service configuration being reviewed. */
    public ServiceConfig config;

    /** The review job being executed. */
    public ReviewJob job;

    /** Concatenated application source files fetched from GitHub. */
    public String appCodeContent;

    /** AI-generated business context document from the code analysis stage. */
    public String codeContextDocument;

    /** K8s manifests and Terraform config files as annotated text. */
    public String infraSnapshot;

    /** Azure Resource Manager / Azure Monitor responses as annotated text. */
    public String azureConfigData;

    /** Datadog metrics queries and results as annotated text. */
    public String datadogMetricsData;

    /** MySQL information_schema.tables results as annotated text. */
    public String mysqlTableData;

    /** The final AI-generated scalability review report in Markdown format. */
    public String reportMarkdown;

    /** Commit SHAs for each repository at the time of the review. Keys: "app", "infra", "k8s_gitops". */
    public Map<String, String> repoShas = new HashMap<>();
}