package app.aiscalabilityreview.domain;

import core.framework.mongo.Collection;
import core.framework.mongo.Field;
import core.framework.mongo.Id;

import java.time.ZonedDateTime;

@Collection(name = "review_reports")
public class ReviewReport {
    @Id
    public String reportId;

    @Field(name = "job_id")
    public String jobId;

    @Field(name = "service_id")
    public String serviceId;

    @Field(name = "period_label")
    public String periodLabel;

    @Field(name = "ai_model")
    public String aiModel;

    @Field(name = "report_markdown")
    public String reportMarkdown;

    @Field(name = "confluence_page_id")
    public String confluencePageId;

    @Field(name = "confluence_page_url")
    public String confluencePageUrl;

    // Scores
    @Field(name = "traffic_score")
    public String trafficScore;

    @Field(name = "latency_score")
    public String latencyScore;

    @Field(name = "errors_score")
    public String errorsScore;

    @Field(name = "resources_score")
    public String resourcesScore;

    @Field(name = "persistence_score")
    public String persistenceScore;

    @Field(name = "overall_score")
    public String overallScore;

    // Key metrics snapshot
    @Field(name = "avg_rps")
    public Double avgRps;

    @Field(name = "p99_latency_ms")
    public Double p99LatencyMs;

    @Field(name = "error_rate_pct")
    public Double errorRatePct;

    @Field(name = "cpu_avg_pct")
    public Double cpuAvgPct;

    @Field(name = "mem_avg_pct")
    public Double memAvgPct;

    @Field(name = "mysql_connection_pct")
    public Double mysqlConnectionPct;

    @Field(name = "mysql_replication_lag_s")
    public Double mysqlReplicationLagS;

    // Recommendation counts
    @Field(name = "recommendations_high")
    public Integer recommendationsHigh;

    @Field(name = "recommendations_medium")
    public Integer recommendationsMedium;

    @Field(name = "recommendations_low")
    public Integer recommendationsLow;

    @Field(name = "generated_at")
    public ZonedDateTime generatedAt;

    // Repo commit SHAs at time of review
    @Field(name = "repo_sha_app")
    public String repoShaApp;

    @Field(name = "repo_sha_infra")
    public String repoShaInfra;

    @Field(name = "repo_sha_k8s_gitops")
    public String repoShaK8sGitops;
}