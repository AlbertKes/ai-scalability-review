package app.aiscalabilityreview.api.localreview;

import core.framework.api.json.Property;

import java.util.List;

/** Environment diagnostics (FR-10). {@code ok} is false only when a check that blocks a run failed. */
public class PreflightResponse {
    @Property(name = "ok")
    public Boolean ok;

    @Property(name = "checks")
    public List<CheckView> checks;

    public static class CheckView {
        @Property(name = "id")
        public String id;

        @Property(name = "name")
        public String name;

        @Property(name = "category")
        public String category;  // CLI | AUTH | MCP | STORAGE | REPO | DATA

        @Property(name = "status")
        public String status;  // PASS | WARN | FAIL

        @Property(name = "detail")
        public String detail;

        @Property(name = "hint")
        public String hint;
    }
}
