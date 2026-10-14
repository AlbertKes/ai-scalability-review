package app.aiscalabilityreview.prompt;

/**
 * Check 8 of the validation — the one that re-verifies the report's <em>advice</em> rather than its
 * data.
 *
 * <p>Held apart from the other checks because it is a different kind of check: checks 1 to 7 ask
 * whether the numbers are right, this one asks whether the recommendations built on them are sound,
 * and it is the only check that reads the application source. That last property is why it now runs
 * in a session of its own — the files it opens used to stay in the context of every later
 * round-trip, including the ones that were only re-reading the report's own markdown.
 */
public final class RecommendationSoundnessPrompt {
    public static final String CONTENT;

    /** The two repository roots this check reads, and the rule that keeps it from searching them. */
    public static final String FILE_REFS;

    static {
        CONTENT = """
        ## Check 8 — Recommendation Soundness

        Checks 1–7 verify that the report's *data* is right. This check verifies that its **advice**
        is right, which is what a reader actually acts on. Take every entry of
        "Recommendations & Action Items" **and** every numbered bullet of the Executive Summary's
        critical-findings list — they restate the same claims, so a false finding has to be flagged in
        both places.

        Do not accept a recommendation because it sounds sensible. Each one is a factual claim about
        this codebase plus a proposed change; verify both.

        **Procedure** — run all six tests below on each recommendation, in order:

        **8a — Premise.** Re-verify the **Finding** against the primary source, not against the
        report. A finding annotated `[Source: context → code-context.md]` is *not* verified by
        reading `code-context.md`: that file is itself model output from an earlier stage, so it can
        carry the same error. Read the actual file — the class, the migration SQL, the module
        registration, the property file — and quote what it says.

        This service's own code lives under these roots, and the infra under `{{INFRA_REPO_PATH}}`:
        {{APP_SOURCE_ROOTS}}

        Open files directly; do not search for them. `code-context.md` and the report both cite the
        path they drew from, so use that citation as the index of what to open — then verify what
        the file actually contains. Globbing or grepping your way to a path you were already handed
        is the most expensive thing this stage does: every tool call re-sends the whole context.
        The roots above are where to look first, not a boundary — read anything under
        `{{APP_REPO_PATH}}` the report cites.

        If a primary source cannot be read at all, say so and mark that recommendation
        **WARNING — primary source unreadable**, naming the path and the error. Never record PASS
        for a recommendation you were unable to check: a check that silently passes because it
        could not look is worse than no check, and it is indistinguishable from a real PASS in the
        summary table. Premise contradicted by the source → **FAIL**. Premise true but its
        stated severity overstated or understated → **WARNING**, and say which way.

        **8b — Evidence.** If the finding rests on a metric the report itself marks
        `NOT_COLLECTED`, the recommendation is unfounded → **FAIL**. The only sound recommendation
        that can be built on a missing metric is "collect it" — a recommendation that instead asserts
        a risk, or prescribes a change sized to the number it does not have, is asserting something
        it cannot know.

        **8c — Already done.** Check whether the proposed **Action** is already implemented. Search
        the repository for the mechanism the action asks for — the config call, the annotation, the
        property, the Redis key, the index, the Kafka publish — before accepting that it is missing.
        Action wholly present already → **FAIL** (the recommendation is noise that costs a reviewer
        real time). Action partly present, so that only a narrower change remains → **WARNING**, and
        state the part that is already covered.

        **8d — Every number traceable.** Each numeric target in the Action (a timeout in ms, a
        connection limit, a batch size, a replica count, a threshold) must come from either a
        collected metric, an existing value in this codebase, or a documented platform default, and
        the recommendation must say which. A target that appears from nowhere → **FAIL**. Prefer a
        number the codebase already uses for the same purpose: if the sibling clients in the same
        module use 5000 ms, a recommendation of 5000 ms is traceable and one of 2000 ms is not.

        **8e — Direction.** Confirm the change moves the metric the right way. Re-read the current
        value of whatever the action proposes to set, and compare. A proposed ceiling that is
        **below** the value already in force is a regression dressed as a fix → **FAIL**. This is the
        common failure for capacity limits, where the platform default is far larger than the number
        the report guesses at.

        **8f — Scope.** Confirm the metric that motivates the recommendation is actually scoped to
        this service. A shared-resource metric — `azure.dbformysql_flexibleservers.*` is per
        **server**, not per application, and one flexible server hosts every service in the domain —
        describes all of its tenants at once. Attributing it to this service alone, or sizing this
        service's change by it, → **WARNING** at minimum; **FAIL** if the recommendation's whole
        premise is that this service caused the number.

        Finally, for each recommendation that survives, check **proportionality**: the
        `(High Priority)` / `(Medium Priority)` / `(Low Priority)` marker must match the strength of
        the verified evidence. A High marked on an unverified or shared-resource premise →
        **WARNING**.

        **Report one finding per recommendation**, naming it by number and title, with `Reported:` =
        the finding as the report states it and `Actual:` = what the primary source says, sourced. A
        recommendation that passes all six tests is a **PASS** finding — still list it, so the reader
        can tell verified advice from unchecked advice.

        A recommendation that is factually false or already implemented is worse than a missing one,
        because it spends engineering time and discredits the rest of the report. Prefer FAIL over
        WARNING when the source contradicts the finding.

        ---
            """;

        FILE_REFS = """


        ## Attached Local Files

        **Review report to validate**:
        @{{REPORT_FILE}}

        **Application repository root**:
        Local path: `{{APP_REPO_PATH}}`

        **Infra repository root**:
        Local path: `{{INFRA_REPO_PATH}}`

        A claim annotated `[Source: context -> code-context.md]` was written by an earlier model stage
        and is not evidence of itself, so re-read the class, migration SQL, module registration or
        property file under these roots before accepting or rejecting it. Open the path the report
        cites rather than searching for it — the repository is large, every tool call re-sends the
        whole context, and a whole-directory read would cost more than the check is worth.
            """;
    }

    private RecommendationSoundnessPrompt() {
    }
}
