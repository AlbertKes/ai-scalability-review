package app.aiscalabilityreview.job;

import app.aiscalabilityreview.service.GeminiCliService.GeminiRunResult;
import core.framework.util.Strings;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

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
        **Stage time**: {} s (sum of the stages; they overlap, so this exceeds the wall clock)
        **Wall clock**: {} s (budget {} s)
        **MCP tool calls**: {}
        **Within budget**: {}
        """;

    /**
     * Written from more than one thread: code analysis runs alongside the infra and metric stages,
     * and both report into this. Copy-on-write is the right shape for five writes and one read.
     */
    private final List<StageStat> stages = new CopyOnWriteArrayList<>();

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

    /** Sum of the stage durations. Exceeds the wall clock whenever stages overlap. */
    public long totalDurationMs() {
        return stages.stream().mapToLong(stage -> stage.durationMs).sum();
    }

    public int totalMcpToolCalls() {
        return stages.stream().mapToInt(stage -> stage.mcpToolCalls).sum();
    }

    /**
     * Budget is judged on the wall clock, not on the sum of the stage durations: those overlap now
     * that code analysis runs beside the infra and metric stages, and judging a run against a total
     * no clock ever showed would make the budget meaningless.
     */
    public boolean withinBudget(long wallClockMs, long tokenBudget, long durationBudgetMs) {
        return wallClockMs <= durationBudgetMs && totalTokens() <= tokenBudget;
    }

    public String summaryLine(long wallClockMs, long tokenBudget, long durationBudgetMs) {
        return Strings.format("totalTokens={}, budget={}, wallClockMs={}, budgetMs={}, mcpToolCalls={}, withinBudget={}",
            totalTokens(), tokenBudget, wallClockMs, durationBudgetMs, totalMcpToolCalls(),
            withinBudget(wallClockMs, tokenBudget, durationBudgetMs));
    }

    /** Rows follow the pipeline order, not the order the stages happened to finish in. */
    public String render(String serviceId, String jobId, long wallClockMs, long tokenBudget, long durationBudgetMs) {
        StringBuilder sb = new StringBuilder(2048);
        sb.append(Strings.format(HEADER_TEMPLATE, serviceId, jobId));
        stages.stream().sorted(Comparator.comparingInt(stage -> pipelineOrder(stage.stage))).forEach(stage ->
            sb.append(Strings.format(ROW_TEMPLATE, stage.stage, stage.model, stage.durationMs / 1000,
                stage.inputTokens, stage.outputTokens, stage.cachedTokens, stage.toolCalls, stage.mcpToolCalls)));
        sb.append(Strings.format(FOOTER_TEMPLATE, totalTokens(), tokenBudget, totalDurationMs() / 1000,
            wallClockMs / 1000, durationBudgetMs / 1000, totalMcpToolCalls(),
            withinBudget(wallClockMs, tokenBudget, durationBudgetMs) ? "yes" : "NO"));
        return sb.toString();
    }

    private int pipelineOrder(String stage) {
        int index = LocalReviewStages.indexOf(stage);
        return index < 0 ? Integer.MAX_VALUE : index;
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
