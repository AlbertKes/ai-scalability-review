package app.aiscalabilityreview.service;

import app.aiscalabilityreview.prompt.ValidationChecks;
import app.aiscalabilityreview.prompt.ValidationChecks.Check;
import app.aiscalabilityreview.prompt.ValidationChecks.Dimension;
import core.framework.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Merges the three validation sessions' partials into the one report a reader opens.
 *
 * <p>The summary table, the overall verdict and the critical-issues and warnings lists used to be
 * written by the same model turn that wrote the findings. No session can see all eight checks any
 * more, so they are computed here instead — from the findings rather than alongside them. That
 * removes a whole class of defect the old report could carry: a summary row that disagreed with its
 * own section, an issue count that did not match the findings under it, or a PASS verdict over a
 * section containing a FAIL.
 *
 * <p>The verdict counts <em>dimensions</em>, not findings. A FAIL anywhere used to fail the whole
 * report, and every real run had at least one, so the verdict carried no information. A finding now
 * belongs to one of the review's five scoring dimensions, and the report FAILs only when three or
 * more of them hold a FAIL; two is a yellow ({@link #VERDICT_CONDITIONAL_PASS}), one or none is green
 * ({@link #VERDICT_PASS}). Every FAIL and WARNING is still listed in full whatever the verdict says.
 *
 * <p>A group whose session failed leaves no partial. Its checks are then reported as not run, and
 * the verdict is held below a clean PASS — a check that did not happen must never read as one that
 * passed.
 */
public class ValidationReportAssembler {
    public static final String VERDICT_PASS = "PASS";
    public static final String VERDICT_CONDITIONAL_PASS = "CONDITIONAL PASS";
    public static final String VERDICT_FAIL = "FAIL";

    private static final String STATUS_PASS = "PASS";
    private static final String STATUS_FAIL = "FAIL";
    private static final String STATUS_WARNING = "WARNING";
    private static final String NOT_RUN = "not run — its validation stage did not complete";

    /** Matches `## Check 3 — Cross-Source Metric Consistency: WARNING`, dash or em dash, any case. */
    private static final Pattern SECTION_PATTERN = Pattern.compile(
        "(?m)^##\\s*Check\\s*(\\d+)\\s*[—–-]\\s*([^:\\n]*?)\\s*:\\s*\\**\\s*(PASS|FAIL|WARNING|SKIPPED)\\b.*$");
    private static final Pattern FINDING_PATTERN = Pattern.compile(
        "(?m)^\\*\\*\\[(PASS|FAIL|WARNING|SKIPPED)]\\*\\*\\s*(.*)$");
    private static final Pattern DIMENSION_PATTERN = Pattern.compile(
        "(?mi)^\\s*[-*]?\\s*Dimension\\s*:\\s*`?([A-Za-z][A-Za-z &/]*)`?\\s*$");
    /** Failed dimensions at which the report as a whole FAILs, and at which it turns yellow. */
    private static final int FAIL_DIMENSIONS_FOR_FAIL = 3;
    private static final int FAIL_DIMENSIONS_FOR_YELLOW = 2;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final String HEADER_TEMPLATE = """
        # Validation Report: {} ({})
        **Original Report**: `{}`
        **Original Report Date**: {}
        **Validation Run Date**: {}
        **Validator**: {}

        ---

        ## Validation Summary

        | # | Check | Status | Issues |
        |---|-------|--------|--------|
        """;
    private static final String FOOTER = """

        ---
        *Validation performed by three independent sessions — one comparing the report with the collected
        Datadog and Azure data and the infra repository, one re-checking the report against itself, one re-verifying each
        recommendation against the application source — against the claims in the original report.*
        *Metric values are compared with the figures the pipeline collected over the same fixed window: equal after rounding.*
        *Config tolerances: exact match for static config.*
        *The summary table and the verdict above are computed from the findings, not asserted beside them:
        three or more failed dimensions FAIL, two are a yellow, one or none is green.*
        """;

    private final Logger logger = LoggerFactory.getLogger(ValidationReportAssembler.class);

    /**
     * Writes the merged report to {@link AssemblyParams#targetPath()} and returns its markdown.
     *
     * @param partials the per-group files in any order; a missing or unreadable one is reported as a
     *                 check that did not run rather than quietly dropped
     */
    public String assemble(AssemblyParams params, List<String> partials) {
        String markdown = merge(params, partials.stream().map(this::read).toList());
        Path target = Path.of(params.targetPath());
        try {
            Files.writeString(target, markdown);
        } catch (IOException e) {
            logger.warn("Could not write the assembled validation report to {}: {}", target, e.getMessage());
        }
        return markdown;
    }

    /**
     * The same merge with no file system behind it, for the scheduled pipeline, whose validation is
     * one model call rather than three sessions. Both pipelines therefore produce the same document,
     * and both get a verdict derived from the findings rather than asserted beside them.
     *
     * @param partials the sessions' markdown in any order; a blank one contributes no sections
     */
    public String merge(AssemblyParams params, List<String> partials) {
        Map<Integer, Section> sections = new LinkedHashMap<>();
        for (String partial : partials) {
            parseSections(partial, sections);
        }
        return render(params, sections);
    }

    /**
     * Splits one partial into its check sections. A section runs to the next check heading, so a
     * group that wrote its checks out of order or added prose between them still parses.
     */
    private void parseSections(String markdown, Map<Integer, Section> sections) {
        if (Strings.isBlank(markdown)) return;
        Matcher matcher = SECTION_PATTERN.matcher(markdown);
        List<Heading> headings = new ArrayList<>();
        while (matcher.find()) {
            headings.add(new Heading(Integer.parseInt(matcher.group(1)),
                matcher.group(3).toUpperCase(Locale.ROOT), matcher.start(), matcher.end()));
        }
        for (int i = 0; i < headings.size(); i++) {
            Heading heading = headings.get(i);
            int bodyEnd = i + 1 < headings.size() ? headings.get(i + 1).start() : markdown.length();
            sections.put(heading.number(),
                new Section(heading.number(), heading.status(), markdown.substring(heading.bodyStart(), bodyEnd).strip()));
        }
    }

    private String render(AssemblyParams params, Map<Integer, Section> sections) {
        StringBuilder sb = new StringBuilder(8192);
        sb.append(Strings.format(HEADER_TEMPLATE, params.serviceId(), params.env(), params.reviewReportPath(),
            params.reportDate(), DATE_FORMAT.format(ZonedDateTime.now()), params.model()));

        List<Finding> fails = new ArrayList<>();
        List<Finding> warnings = new ArrayList<>();
        List<Integer> missing = new ArrayList<>();
        for (Check check : ValidationChecks.ALL) {
            Section section = sections.get(check.number());
            if (section == null) missing.add(check.number());
            List<Finding> findings = section == null ? List.of() : findings(check.number(), section.body());
            findings.stream().filter(finding -> STATUS_FAIL.equals(finding.status())).forEach(fails::add);
            findings.stream().filter(finding -> STATUS_WARNING.equals(finding.status())).forEach(warnings::add);
            sb.append(summaryRow(check, section, findings.size()));
        }

        sb.append('\n');
        for (Check check : ValidationChecks.ALL) {
            sb.append("---\n\n").append(checkSection(check, sections.get(check.number()))).append("\n\n");
        }

        Set<Dimension> failedDimensions = EnumSet.noneOf(Dimension.class);
        fails.stream().map(Finding::dimension).filter(Objects::nonNull).forEach(failedDimensions::add);
        sb.append("---\n\n## Overall Validation Verdict: ")
            .append(verdict(failedDimensions.size(), missing.isEmpty())).append("\n\n");
        appendDimensions(sb, fails, warnings, failedDimensions);
        appendList(sb, "### Critical Issues", fails);
        appendList(sb, "### Warnings", warnings);
        appendNotes(sb, missing);
        sb.append(FOOTER);
        return sb.toString();
    }

    /**
     * Only FAIL and WARNING findings are counted as issues — the old reports counted whatever the
     * model felt the number should be, which is why a check could show "0 issue(s)" over a section
     * holding a FAIL.
     */
    private String summaryRow(Check check, Section section, int issues) {
        String status = section == null ? "SKIPPED" : section.status();
        // the count comes first even for a check that did not run, because ValidationReportParser
        // reads these rows and a cell that opens with prose would make the row unparseable
        String detail = section == null ? "0 issue(s) — " + NOT_RUN : issues + " issue(s)";
        return "| " + check.number() + " | " + check.name() + " | " + status + " | " + detail + " |\n";
    }

    private String checkSection(Check check, Section section) {
        if (section == null) {
            return ValidationChecks.header(check.number(), "SKIPPED") + "\n\n### Findings\n\nThis check was "
                + NOT_RUN + ", so nothing in the report was verified against it.";
        }
        return ValidationChecks.header(check.number(), section.status()) + "\n\n" + section.body();
    }

    /**
     * Three or more failed dimensions FAIL, two are yellow, one or none is green — except that a
     * group which did not run can never leave a clean PASS, because a check that did not happen must
     * not read as one that passed.
     */
    private String verdict(int failedDimensions, boolean nothingMissing) {
        if (failedDimensions >= FAIL_DIMENSIONS_FOR_FAIL) return VERDICT_FAIL;
        if (failedDimensions >= FAIL_DIMENSIONS_FOR_YELLOW || !nothingMissing) return VERDICT_CONDITIONAL_PASS;
        return VERDICT_PASS;
    }

    /** The five dimensions with their FAIL and WARNING counts, so the verdict can be audited. */
    private void appendDimensions(StringBuilder sb, List<Finding> fails, List<Finding> warnings, Set<Dimension> failed) {
        sb.append("### Dimension Results\n\n**").append(failed.size()).append(" of ").append(Dimension.values().length)
            .append(" dimensions failed** — ").append(FAIL_DIMENSIONS_FOR_FAIL).append("+ is FAIL, ")
            .append(FAIL_DIMENSIONS_FOR_YELLOW).append(" is CONDITIONAL PASS (yellow), 0–1 is PASS (green).\n\n"
                + "| Dimension | Status | FAIL | WARNING |\n|---|---|---|---|\n");
        for (Dimension dimension : Dimension.values()) {
            long failCount = fails.stream().filter(finding -> finding.dimension() == dimension).count();
            long warningCount = warnings.stream().filter(finding -> finding.dimension() == dimension).count();
            String status = failCount > 0 ? STATUS_FAIL : warningCount > 0 ? STATUS_WARNING : STATUS_PASS;
            sb.append("| ").append(dimension.label()).append(" | ").append(status).append(" | ")
                .append(failCount).append(" | ").append(warningCount).append(" |\n");
        }
        long unplaced = fails.stream().filter(finding -> finding.dimension() == null).count();
        if (unplaced > 0) {
            sb.append('\n').append(unplaced).append(" FAIL finding(s) belong to no single dimension (format, scoring math, "
                + "projection) and do not count toward the verdict; they are listed below.\n");
        }
        sb.append('\n');
    }

    private List<Finding> findings(int checkNumber, String body) {
        List<Finding> findings = new ArrayList<>();
        Matcher matcher = FINDING_PATTERN.matcher(body);
        List<FindingHead> heads = new ArrayList<>();
        while (matcher.find()) {
            heads.add(new FindingHead(matcher.group(1), matcher.start(), matcher.end(), matcher.group(2).strip()));
        }
        for (int i = 0; i < heads.size(); i++) {
            FindingHead head = heads.get(i);
            if (STATUS_PASS.equals(head.status()) || "SKIPPED".equals(head.status())) continue;
            int end = i + 1 < heads.size() ? heads.get(i + 1).start() : body.length();
            findings.add(new Finding(checkNumber, head.status(), head.title(), dimension(head.title(), body.substring(head.bodyStart(), end))));
        }
        return findings;
    }

    /**
     * The tag the model wrote, else the dimension its wording points at. A finding is left unplaced
     * only when the model explicitly called it {@code General}, because an unplaced FAIL does not
     * count toward the verdict and must not happen by accident.
     */
    private Dimension dimension(String title, String block) {
        Matcher tag = DIMENSION_PATTERN.matcher(block);
        if (tag.find()) {
            Dimension tagged = Dimension.fromTag(tag.group(1));
            if (tagged != null) return tagged;
            if (tag.group(1).strip().toLowerCase(Locale.ROOT).startsWith("general")) return null;
        }
        return Dimension.infer(title, block);
    }

    private void appendList(StringBuilder sb, String heading, List<Finding> findings) {
        if (findings.isEmpty()) return;
        sb.append(heading).append("\n\n");
        for (Finding finding : findings) {
            sb.append("- **[Check ").append(finding.checkNumber()).append("]** ").append(finding.title()).append('\n');
        }
        sb.append('\n');
    }

    private void appendNotes(StringBuilder sb, List<Integer> missing) {
        if (missing.isEmpty()) return;
        sb.append("### Validation Notes\n\n- Checks ")
            .append(missing.stream().map(String::valueOf).collect(Collectors.joining(", ")))
            .append(" did not run, so the report is unverified in those respects. The verdict above is held "
                + "below a clean PASS for that reason alone; it does not mean a problem was found.\n\n");
    }

    private String read(String path) {
        try {
            return Files.readString(Path.of(path).toAbsolutePath());
        } catch (IOException e) {
            logger.info("No validation partial at {}: {}", path, e.getMessage());
            return "";
        }
    }

    /**
     * @param reportDate the date of the report being validated, which is the run's own date
     * @param model      recorded as the validator, as the format requires
     */
    public record AssemblyParams(String serviceId,
                                 String env,
                                 String reviewReportPath,
                                 String reportDate,
                                 String model,
                                 String targetPath) {
    }

    private record Section(int number, String status, String body) {
    }

    /** One finding line: its status and title, and where the finding's own lines begin. */
    private record FindingHead(String status, int start, int bodyStart, String title) {
    }

    private record Heading(int number, String status, int start, int bodyStart) {
    }

    private record Finding(int checkNumber, String status, String title, Dimension dimension) {
    }
}
