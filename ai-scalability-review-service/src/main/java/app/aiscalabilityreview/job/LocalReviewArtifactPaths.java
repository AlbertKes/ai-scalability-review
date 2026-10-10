package app.aiscalabilityreview.job;

/**
 * Absolute paths of the files one run writes, all inside {@link #runDir()}.
 *
 * @param codeContext      stage 1 output, or a previous run's file when code analysis was skipped
 * @param reviewReport     the report markdown, date-stamped inside this run's own directory
 * @param validationReport stage 5 output
 */
public record LocalReviewArtifactPaths(String runDir,
                                       String codeContext,
                                       String infraContext,
                                       String metrics,
                                       String mysqlTables,
                                       String reviewReport,
                                       String validationReport,
                                       String reportSnapshot) {
}
