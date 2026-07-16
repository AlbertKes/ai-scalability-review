package app.aiscalabilityreview.prompt;

public class ComparisonPrompt {
    /**
     * Comparison prompt for comparing two scalability review reports.
     * Placeholders: {SERVICE}, {PERIOD_A}, {PERIOD_B}, {REPORT_A}, {REPORT_B}
     */
    public static final String CONTENT = """
        You are comparing two scalability review reports for {SERVICE}.
        
        **Report A** (period: {PERIOD_A}):
        {REPORT_A}
        
        ---
        
        **Report B** (period: {PERIOD_B}):
        {REPORT_B}
        
        ---
        
        ## Instructions
        
        Perform a structured comparison across all 5 scalability dimensions:
        1. Traffic & Throughput
        2. Latency & Thread Concurrency
        3. Error Rates & Stability
        4. Resource Saturation
        5. Persistence Layer
        
        For each dimension, produce a `DimensionDiff` containing:
        - **score_a**: the score from Report A (GREEN / YELLOW / RED)
        - **score_b**: the score from Report B (GREEN / YELLOW / RED)
        - **change**: one of IMPROVED, DEGRADED, or UNCHANGED
        - **key_changes**: bullet list of the most significant metric changes (up to 3)
        
        Then produce an **overall_trajectory** (IMPROVING / STABLE / DEGRADING) based on the
        aggregate of all dimension changes.
        
        Finally, write a **summary_text** of 3–5 sentences describing the most important
        scalability changes between the two periods, citing specific metric deltas where available.
        
        ## Output Format
        
        Respond with a JSON object matching this schema:
        ```json
        {
          "overall_trajectory": "IMPROVING|STABLE|DEGRADING",
          "summary_text": "...",
          "dimension_diffs": [
            {
              "dimension": "Traffic & Throughput",
              "score_a": "GREEN|YELLOW|RED",
              "score_b": "GREEN|YELLOW|RED",
              "change": "IMPROVED|DEGRADED|UNCHANGED",
              "key_changes": ["...", "..."]
            }
          ]
        }
        ```
        
        Do not invent metric values. Base all observations on what is explicitly stated in the two reports.
        """;
}
