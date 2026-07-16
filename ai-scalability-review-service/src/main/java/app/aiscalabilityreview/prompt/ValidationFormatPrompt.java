package app.aiscalabilityreview.prompt;

public class ValidationFormatPrompt {
    /**
     * Validation report format specification (validation-format.md).
     */
    public static final String CONTENT = """
        # Validation Report Format
        
        This file defines the mandatory output format for scalability review validation reports.
        
        **File naming**: `scalability/ai/reports/<service>/<date>/<service>-validation-<report-YYYY-MM-DD>-run-<run-YYYY-MM-DD>.md`
        
        Example: `scalability/ai/reports/wonder-cart-service/gemini/wonder-cart-service-validation-2026-07-08-run-2026-07-10.md`
        
        ---
        
        ## Section Order (mandatory)
        
        1. Validation Header
        2. Validation Summary Table
        3. Check Results (all 7 checks, in order)
        4. Overall Verdict
        5. Footer
        
        ---
        
        ## 1. Validation Header
        
        ```markdown
        # Validation Report: <service> (<env>)
        **Original Report**: `<REPORT_FILE path>`
        **Original Report Date**: <YYYY-MM-DD>
        **Validation Run Date**: <YYYY-MM-DD>
        **Validator**: <AI model name or engineer name>
        ---
        ```
        
        ---
        
        ## 2. Validation Summary Table
        
        A single table summarizing the result of all 7 checks. Always include all 7 rows.
        
        ```markdown
        ## Validation Summary
        
        | # | Check | Status | Issues |
        |---|-------|--------|--------|
        | 1 | Config Accuracy | PASS / FAIL / WARNING / SKIPPED | <n> issue(s) |
        | 2 | Metric Numerical Fidelity | PASS / FAIL / WARNING / SKIPPED | <n> issue(s) |
        | 3 | Cross-Source Metric Consistency | PASS / FAIL / WARNING / SKIPPED | <n> issue(s) |
        | 4 | Scoring Accuracy | PASS / FAIL / WARNING / SKIPPED | <n> issue(s) |
        | 5 | Capacity Projection Math | PASS / FAIL / WARNING / SKIPPED | <n> issue(s) |
        | 6 | NOT_COLLECTED Authenticity | PASS / FAIL / WARNING / SKIPPED | <n> issue(s) |
        | 7 | Format & Attribution Completeness | PASS / FAIL / WARNING / SKIPPED | <n> issue(s) |
        ```
        
        **Row-level status rules**:
        - A check is **PASS** if all its individual findings are PASS (or the check was fully SKIPPED).
        - A check is **WARNING** if it has at least one WARNING and no FAIL findings.
        - A check is **FAIL** if it has at least one FAIL finding.
        - A check is **SKIPPED** only if the entire check is not applicable (e.g., MySQL checks when MYSQL_HOST is N/A).
        
        ---
        
        ## 3. Check Results
        
        Output one section per check in the fixed order below. Always include all 7 sections.
        Use "Status: SKIPPED — <reason>" for inapplicable checks (e.g., MySQL checks when MYSQL_HOST is N/A).
        
        ```markdown
        ## Check 1 — Config Accuracy: <PASS | FAIL | WARNING | SKIPPED>
        
        ### Findings
        
        **[PASS]** CPU requests (K8s deployment)
          - Reported: `500m`
          - Actual: `500m` [Source: code → k8s-gitops/environments/prod/apps/wonder/deployment.yaml:L42]
          - Detail: Exact match.
        
        **[FAIL]** HPA maxReplicas
          - Reported: `20`
          - Actual: `15` [Source: code → k8s-gitops/environments/prod/apps/wonder/hpa.yaml:L18]
          - Detail: Report overstates maxReplicas by 5. Likely copied from an older manifest version.
        
        **[WARNING]** MySQL storage used
          - Reported: `312 GB`
          - Actual: `329 GB` [Source: Azure MCP → get_metric(resource=wonder-cart-mysql-prod, metric=storage_used)]
          - Detail: 5.4% increase since report generation (within 10% tolerance for growing storage).
        
        ### Summary
        <1–2 sentences summarizing the overall config accuracy result.>
        ```
        
        *(Repeat this structure for all 7 checks.)*
        
        ### Check section headers
        
        Use these exact headers in order:
        
        ```
        ## Check 1 — Config Accuracy: <STATUS>
        ## Check 2 — Metric Numerical Fidelity: <STATUS>
        ## Check 3 — Cross-Source Metric Consistency: <STATUS>
        ## Check 4 — Scoring Accuracy: <STATUS>
        ## Check 5 — Capacity Projection Math: <STATUS>
        ## Check 6 — NOT_COLLECTED Authenticity: <STATUS>
        ## Check 7 — Format & Attribution Completeness: <STATUS>
        ```
        
        ---
        
        ## 4. Overall Verdict
        
        ```markdown
        ## Overall Validation Verdict: <PASS | CONDITIONAL PASS | FAIL>
        ```
        
        **Verdict rules**:
        - **PASS** — No FAIL findings across all checks; at most minor WARNINGs. Report can be trusted as-is.
        - **CONDITIONAL PASS** — Has WARNINGs but no FAILs. Report is usable; reviewer should note caveats below.
        - **FAIL** — At least one FAIL finding. Report contains inaccuracies that must be corrected before use.
        
        ```markdown
        ### Critical Issues
        *(List all FAIL findings in brief. Omit this section if verdict is PASS.)*
        
        - **[Check 1]** HPA maxReplicas overstated: report says 20, actual is 15.
        - **[Check 4]** Dimension 5 score: report says YELLOW, but cited metrics compute to RED
          (MySQL connection avg 78% > 75% RED threshold).
        
        ### Warnings
        *(List all WARNING findings in brief. Omit this section if there are no warnings.)*
        
        - **[Check 2]** Peak RPS differs by 22% (report: 340 RPS, current: 278 RPS); within tolerance
          but notable given the 28-day window rolled.
        - **[Check 5]** +4Q growth factor rounding: report 1.612×, computed 1.608× (0.2% off).
        
        ### Validation Notes
        *(General observations about data source availability, temporal context, or review quality.)*
        ```
        
        ---
        
        ## 5. Footer
        
        ```markdown
        ---
        *Validation performed by independently re-querying Datadog MCP, Azure MCP, MySQL MCP, and
        code repositories against the claims in the original report.*
        *Metric tolerances applied: ±20% for 28-day averages, ±30% for peaks (rolling window drift).*
        *Config tolerances: exact match for static config; ±10 GB or ±10% for dynamic storage values.*
        ```
        
        ---
        
        ## Drift-Prevention Checklist for Validators
        
        Before finalizing the validation report, confirm:
        
        - [ ] All 7 check sections are present in the correct order
        - [ ] Validation Summary table has exactly 7 rows
        - [ ] Every finding includes: Reported value, Actual value with `[Source: ...]`, and Detail
        - [ ] No finding is left without a STATUS tag
        - [ ] Overall Verdict matches the highest-severity finding (FAIL > WARNING > PASS)
        - [ ] Critical Issues section lists every FAIL; Warnings section lists every WARNING
        - [ ] SKIPPED findings have a clear reason stated
        - [ ] Temporal context is noted when report is >30 days old (affects Check 2 and Check 6)
        """;
}
