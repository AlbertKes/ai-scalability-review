package app.aiscalabilityreview.api.review;

import core.framework.api.json.Property;

import java.time.ZonedDateTime;
import java.util.List;

/** One row per service that has either a config or at least one run (FR-9.1, FR-9.4). */
public class ListServiceSummaryResponse {
    @Property(name = "services")
    public List<ServiceSummaryView> services;

    public static class ServiceSummaryView {
        @Property(name = "service_id")
        public String serviceId;

        @Property(name = "display_name")
        public String displayName;

        @Property(name = "team")
        public String team;

        @Property(name = "tier")
        public String tier;

        @Property(name = "has_config")
        public Boolean hasConfig;

        @Property(name = "run_count")
        public Integer runCount;

        @Property(name = "last_run_at")
        public ZonedDateTime lastRunAt;

        @Property(name = "last_job_id")
        public String lastJobId;

        @Property(name = "last_status")
        public String lastStatus;

        @Property(name = "last_overall_score")
        public String lastOverallScore;

        @Property(name = "last_validation_verdict")
        public String lastValidationVerdict;

        @Property(name = "last_env")
        public String lastEnv;

        @Property(name = "baseline_job_id")
        public String baselineJobId;

        @Property(name = "tokens_30d")
        public Long tokens30d;

        @Property(name = "cost_30d_usd")
        public Double cost30dUsd;

        /** Days since the last completed run; null when there has never been one. */
        @Property(name = "days_since_last_run")
        public Integer daysSinceLastRun;
    }
}
