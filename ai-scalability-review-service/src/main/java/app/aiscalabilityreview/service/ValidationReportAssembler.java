package app.aiscalabilityreview.service;

import app.aiscalabilityreview.prompt.ValidationChecks;
import app.aiscalabilityreview.prompt.ValidationChecks.Check;
import core.framework.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
        *Validation performed by three independent sessions — one re-querying Datadog MCP and Azure MCP
        and the infra repository, one re-checking the report against itself, one re-verifying each
        recommendation against the application source — against the claims in the original report.*
        *Metric tolerances applied: ±20% for 28-day averages, ±30% for peaks (rolling window drift).*
        *Config tolerances: exact match for static config; ±10 GB or ±10% for dynamic storage values.*
        *The summary table and the verdict above are computed from the findings, not asserted beside them.*
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

        sb.append("---\n\n## Overall Validation Verdict: ")
            .append(verdict(fails.isEmpty(), warnings.isEmpty(), missing.isEmpty())).append("\n\n");
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

    /** FAIL beats WARNING beats PASS, and a group that did not run can never leave a clean PASS. */
    private String verdict(boolean noFails, boolean noWarnings, boolean nothingMissing) {
        if (!noFails) return VERDICT_FAIL;
        if (noWarnings && nothingMissing) return VERDICT_PASS;
        return VERDICT_CONDITIONAL_PASS;
    }

    private List<Finding> findings(int checkNumber, String body) {
        List<Finding> findings = new ArrayList<>();
        Matcher matcher = FINDING_PATTERN.matcher(body);
        while (matcher.find()) {
            String status = matcher.group(1);
            if (STATUS_PASS.equals(status) || "SKIPPED".equals(status)) continue;
            findings.add(new Finding(checkNumber, status, matcher.group(2).strip()));
        }
        return findings;
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

    /** One parsed check heading and where its body begins and the next heading starts. */
    private record Heading(int number, String status, int start, int bodyStart) {
    }

    private record Finding(int checkNumber, String status, String title) {
    }
}
