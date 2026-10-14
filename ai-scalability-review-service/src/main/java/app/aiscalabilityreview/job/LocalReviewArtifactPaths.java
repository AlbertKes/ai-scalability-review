package app.aiscalabilityreview.job;

/**
 * Absolute paths of the files one run writes, all inside {@link #runDir()}.
 *
 * @param codeContext       stage 1 output, or a previous run's file when code analysis was skipped
 * @param reviewReport      the report markdown, date-stamped inside this run's own directory
 * @param validationStatic  checks 3, 4, 5, 7 — stage 5A's partial
 * @param validationData    checks 1, 2, 6 — stage 5B's partial
 * @param validationAdvice  check 8 — stage 5C's partial
 * @param validationReport  the three partials merged into one report by stage 5D
 */
public record LocalReviewArtifactPaths(String runDir,
                                       String codeContext,
                                       String infraContext,
                                       String metrics,
                                       String mysqlTables,
                                       String reviewReport,
                                       String validationStatic,
                                       String validationData,
                                       String validationAdvice,
                                       String validationReport,
                                       String reportSnapshot) {
}
