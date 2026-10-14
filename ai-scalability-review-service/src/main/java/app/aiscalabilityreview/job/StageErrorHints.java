package app.aiscalabilityreview.job;

import java.util.Locale;

/**
 * Turns a raw stage error into an actionable sentence (FR-3.7).
 *
 * <p>Every entry here is a failure that has actually been hit locally and whose message does not
 * say what to do about it. The hint is advisory text shown next to the error, never a replacement
 * for it.
 */
public final class StageErrorHints {
    public static String hint(String errorMessage) {
        if (errorMessage == null) return null;
        String message = errorMessage.toLowerCase(Locale.ROOT);

        if (message.contains("resource group") && message.contains("could not be found")) {
            return "The Azure resource group is not the Datadog env: uat lives in FTIUat and prod in RFProdV2. "
                + "It is read from <env>/app/infra/env/01-variables.tf in the infra repository — check that file "
                + "exists for this env, or pass azure_resource_group in the request.";
        }
        if (message.contains("api_key_service_blocked") || message.contains("403")) {
            return "Vertex AI refused the call. Either GOOGLE_API_KEY / GEMINI_API_KEY is set in the environment "
                + "and took precedence over Application Default Credentials, or the configured project has no "
                + "access to aiplatform.googleapis.com. Run the preflight check on the Settings page.";
        }
        if (message.contains("could not load the default credentials") || message.contains("application default credentials")) {
            return "No Application Default Credentials. Run: gcloud auth application-default login";
        }
        if (message.contains("expected record, received array")) {
            return "This is the Gemini CLI 0.46 MySQL MCP incompatibility: it JSON-parses the first text block "
                + "into structuredContent, which the MCP SDK validates as an object, and the MySQL server returns "
                + "rows as an array. Table sizes are collected over JDBC in stage 2 instead; no SQL shape avoids it.";
        }
        if (message.contains("econnrefused") || message.contains("etimedout") || message.contains("timeout")
            || message.contains("enotfound")) {
            return "A data source was unreachable. Azure MCP and the UAT MySQL host both need the corporate VPN; "
                + "Datadog does not.";
        }
        if (message.contains("command not found") || message.contains("cannot run program")) {
            return "The Gemini CLI is not on PATH for this process. Start the service from a shell where "
                + "`gemini --version` works.";
        }
        if (message.contains("timed out after")) {
            return "The stage hit its own wall-clock ceiling. Narrowing app_code_paths is the biggest lever on "
                + "stage 1; skip_validation drops the whole 5A–5D group, which is where most of a run's MCP "
                + "round-trips are.";
        }
        return null;
    }

    private StageErrorHints() {
    }
}
