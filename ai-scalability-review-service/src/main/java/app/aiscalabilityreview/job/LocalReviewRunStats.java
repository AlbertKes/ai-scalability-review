package app.aiscalabilityreview.job;

import app.aiscalabilityreview.service.GeminiCliService.GeminiRunResult;
import core.framework.util.Strings;

import java.util.ArrayList;
import java.util.List;

/**
 * Per-run cost and latency accounting for a local review (AD-6383).
 *
 * <p>One instance per job. It collects what each stage spent, compares the run against the
 * documented budget, and renders a markdown summary that is written next to the report so a run can
 * be compared against the baseline without going through MongoDB.
 *
 * <p>The budget below is the target for a single service review of the current pilot shape. It is a
 * target, not a hard limit: exceeding it logs a warning and shows up in the summary rather than
 * failing the run, because a genuinely large service may legitimately cost more.
 */
public class LocalReviewRunStats {
    /** Target total token spend (input + output, all stages) for one report. */
    public static final long TOKEN_BUDGET = 1_500_000L;
    /** Target wall-clock time for one report, in milliseconds. */
    public static final long DURATION_BUDGET_MS = 15L * 60 * 1000;

    private static final String HEADER_TEMPLATE = """
        # Run Cost & Latency — {}

        **Job**: {}

        | Stage | Model | Duration (s) | Input Tokens | Output Tokens | Cached | Tool Calls | MCP Calls |
        | :--- | :--- | ---: | ---: | ---: | ---: | ---: | ---: |
        """;
    private static final String ROW_TEMPLATE = "| {} | {} | {} | {} | {} | {} | {} | {} |\n";
    private static final String FOOTER_TEMPLATE = """

        **Total tokens**: {} (budget {})
        **Total duration**: {} s (budget {} s)
        **MCP tool calls**: {}
        **Within budget**: {}
        """;

    private final List<StageStat> stages = new ArrayList<>();

    public void record(String stage, GeminiRunResult result) {
        stages.add(new StageStat(stage, result.model(), result.durationMs(), result.inputTokens(),
            result.outputTokens(), result.cachedTokens(), result.toolCalls(), result.mcpToolCalls()));
    }

    /** Records a stage that runs entirely in this service and spends no tokens. */
    public void recordLocal(String stage, long durationMs) {
        stages.add(new StageStat(stage, "none (local)", durationMs, 0, 0, 0, 0, 0));
    }

    public long totalTokens() {
        return stages.stream().mapToLong(stage -> stage.inputTokens + stage.outputTokens).sum();
    }

    public long totalDurationMs() {
        return stages.stream().mapToLong(stage -> stage.durationMs).sum();
    }

    public int totalMcpToolCalls() {
        return stages.stream().mapToInt(stage -> stage.mcpToolCalls).sum();
    }

    public boolean withinBudget() {
        return totalTokens() <= TOKEN_BUDGET && totalDurationMs() <= DURATION_BUDGET_MS;
    }

    public String summaryLine() {
        return Strings.format("totalTokens={}, budget={}, totalDurationMs={}, budgetMs={}, mcpToolCalls={}, withinBudget={}",
            totalTokens(), TOKEN_BUDGET, totalDurationMs(), DURATION_BUDGET_MS, totalMcpToolCalls(), withinBudget());
    }

    public String render(String serviceId, String jobId) {
        StringBuilder sb = new StringBuilder(2048);
        sb.append(Strings.format(HEADER_TEMPLATE, serviceId, jobId));
        for (StageStat stage : stages) {
            sb.append(Strings.format(ROW_TEMPLATE, stage.stage, stage.model, stage.durationMs / 1000,
                stage.inputTokens, stage.outputTokens, stage.cachedTokens, stage.toolCalls, stage.mcpToolCalls));
        }
        sb.append(Strings.format(FOOTER_TEMPLATE, totalTokens(), TOKEN_BUDGET, totalDurationMs() / 1000,
            DURATION_BUDGET_MS / 1000, totalMcpToolCalls(), withinBudget() ? "yes" : "NO"));
        return sb.toString();
    }

    private record StageStat(String stage,
                             String model,
                             long durationMs,
                             long inputTokens,
                             long outputTokens,
                             long cachedTokens,
                             int toolCalls,
                             int mcpToolCalls) {
    }
}
