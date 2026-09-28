# ai-scalability-review

A project integrated with Datadog MCP, MySQL MCP, Azure MCP, etc. and use AI model to generate scalability review report.

## Local one-click review

`POST /local-review/generate` runs a full review on the developer's machine through the local Gemini
CLI, against locally cloned application and infra repositories.

```json
{
  "service": "wonder-cart-service",
  "local_app_repo_path": "/Users/me/IdeaProjects/03Wonder/wonder-consumer-project",
  "local_infra_repo_path": "/Users/me/IdeaProjects/03Wonder/infra",
  "env": "prod",
  "namespace": "prod-consumer",
  "domain": "consumer",
  "mysql_host": "rfprodv2-flexible-wonder-db.mysql.database.azure.com",
  "mysql_db": "wonder_cart",
  "atlas_cluster": null,
  "hpa_type": "HPA",
  "kafka_consumer_groups": "wonder-cart-service",
  "app_code_paths": ["backend/wonder-cart-service", "backend/wonder-cart-service-interface"],
  "operator": "albertke"
}
```

The response returns a `job_id` and the `output_dir`. Poll `GET /review/job/:jobId` for stage
status, which now includes the per-stage model and token counts.

Optional fields:

| Field | Default | Purpose |
| :--- | :--- | :--- |
| `app_code_paths` | whole repository | repo-relative modules to analyze; the single biggest lever on stage 1 cost |
| `skip_code_analysis` | `false` | reuse the existing `code-context.md` |
| `skip_validation` | `false` | skip stage 5; roughly halves the MCP round-trips of a run |
| `output_base_dir` | `reports` | where the run writes its files |
| `code_analysis_model`, `metric_collection_model`, `synthesis_model`, `validation_model` | see below | pin one stage to a different Gemini model |

### Pipeline

| # | Stage | Model | MCP servers | Output |
| :--- | :--- | :--- | :--- | :--- |
| 1 | code analysis | `gemini-3.5-flash` | none | `code-context.md` |
| 2 | infra context + MySQL table storage | none — runs in this service | none | `infra-context.md`, `mysql-tables.md` |
| 3 | metric collection | `gemini-3.1-flash-lite` | datadog, azure | `metrics.md` |
| 4 | synthesis & scoring | `gemini-3.5-flash` | none | `<date>-review.md`, `report-snapshot.json` |
| 5 | validation | `gemini-3.5-flash` | datadog, azure | `<date>-validation.md` |

Stages 1, 2, 3 and 4 are fatal; validation is not, so a validation failure still leaves a usable
report. Validation runs 8 checks. Seven of them re-verify the report's *data* — config, metric
fidelity, cross-source consistency, scoring, projection math, `NOT_COLLECTED` authenticity, format.
The eighth re-verifies its *advice*: each recommendation is re-checked against the primary source in
the application and infra repositories, and fails if its premise is contradicted there, rests on a
`NOT_COLLECTED` metric, prescribes a number traceable to nothing, points the wrong way, or asks for
something the code already does. That is why stage 5 is given the application repository as well as
the infra one — `code-context.md` is an earlier stage's output and cannot verify itself. Every run also writes `run-stats.md` with the per-stage token and latency breakdown.

The split exists because the review runs as an agentic loop, where the whole context is re-sent on
every tool round-trip — so a stage that holds a large context *and* makes many tool calls pays the
product of the two. Stage 3 keeps a small context and does the tool work; stage 4 holds the large
context and makes no tool calls.

Stage 3's Datadog queries are all `.rollup(<aggregator>, 86400)` (or `3600` for peak RPS) over a
day-aligned 28-day window. That is what makes the per-day and per-week figures derivable at all, and
it also keeps each response to 28 points instead of several thousand — the same correctness fix pays
for itself in tokens.

### Prerequisites

The Gemini CLI must be on `PATH` and its MCP servers configured in `~/.gemini/settings.json` under
the names `datadog` and `azure`. Each stage is started with only the servers it needs, so a server
configured under a different name is simply not available to the run.

MySQL table storage is **not** collected through an MCP server. Gemini CLI 0.46 JSON-parses the
first text block of every MCP response into `structuredContent`, which the MCP SDK then validates
against `record(string, unknown)`; `@benborla29/mcp-server-mysql` returns result rows as a JSON
array, so every row-returning query fails client-side with
`Invalid input: expected record, received array`. Stage 2 therefore runs the one fixed
`information_schema` query itself over JDBC. It takes the host, port, user and password from
`MYSQL_JDBC_HOST` / `MYSQL_PORT` / `MYSQL_USER` / `MYSQL_PASSWORD` when they are set, and otherwise
from the `<mysql-host-short-name>-<mysql-db>` entry still present in `~/.gemini/settings.json` (e.g.
`rfprodv2-flexible-wonder-db-wonder_cart`) — that entry carries the routable host, which the Azure
resource name in the request does not. A database that needs the VPN degrades to a `NOT_COLLECTED:`
line rather than failing the run.

Authentication is **pinned to Vertex AI with Application Default Credentials** and is not inherited
from the shell. `GOOGLE_API_KEY` / `GEMINI_API_KEY` are stripped from the CLI subprocess
environment: the CLI gives an API key precedence over ADC and switches to Vertex express mode, which
authorizes against whatever project owns the key rather than the one configured here — a key left in
a shell profile would silently take over the run and fail with `403 API_KEY_SERVICE_BLOCKED`.

The target project and region come from `app.properties`, not from the shell:

```properties
app.gemini.cloud.location=global
app.gemini.cloud.project=wonder-sandbox
```

Either can be overridden without a rebuild, by environment variable
(`APP_GEMINI_CLOUD_PROJECT`, `APP_GEMINI_CLOUD_LOCATION`) or system property
(`-Dapp.gemini.cloud.project=...`). Both are required — the app refuses to start without them,
because with no location the CLI silently falls off the Vertex path onto the public Gemini API.

The only thing left to set up locally is the credential itself:

```bash
gcloud auth application-default login
```

### Cost and latency budget

Target for one service review, enforced as a warning rather than a hard failure (see
`LocalReviewRunStats`):

| Budget | Target |
| :--- | :--- |
| Total tokens (input + output, all stages) | 1,500,000 |
| Wall-clock time | 15 minutes |

A run that exceeds either target logs a warning and records `Within budget: NO` in `run-stats.md`.
A genuinely large service may legitimately cost more; the point of the budget is to make a
regression visible.
