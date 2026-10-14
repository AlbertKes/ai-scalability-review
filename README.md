# ai-scalability-review

A project integrated with Datadog MCP, MySQL MCP, Azure MCP, etc. and use AI model to generate scalability review report.

## Console

`scalability-review-site-frontend/` is a local web console over this pipeline: start a review from a
form that validates every parameter before anything is created, watch it stage by stage, read the
report as HTML, compare runs of the same service, and see what each run cost in tokens and time.
See [its README](scalability-review-site-frontend/README.md) for how to run it.

Two changes the console required are worth knowing about even if you only use the API:

- **Every run writes into its own directory**, `<base>/<service>/<yyyyMMdd-HHmmss>-<jobId>`, with a
  `latest` symlink at `<base>/<service>/latest`. Runs of one service used to share a directory and
  overwrite each other's `code-context.md`, `metrics.md` and `report-snapshot.json`.
- **The trigger request is stored on the job**, so a run can be described, repeated and recovered
  after a restart. `POST /local-review/validate` returns every problem with a request at once
  without creating anything, and `POST /local-review/generate` rejects a bad request with the same
  complete list rather than the first problem it finds.

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
| `azure_resource_group` | read from the infra repository | Azure resource group holding this environment's resources |
| `azure_subscription_id` | Azure CLI profile default | subscription the Azure MCP calls run against |
| `skip_code_analysis` | `false` | reuse the `code-context.md` of the newest previous run of this service |
| `skip_validation` | `false` | skip stages 5A-5D; that is where most of a run's MCP round-trips are |
| `output_base_dir` | `reports` | where the run writes its files |
| `code_analysis_model`, `metric_collection_model`, `synthesis_model`, `validation_model` | see below | pin one stage to a different Gemini model; `validation_model` drives 5A, 5B and 5C together |

### Pipeline

| # | Stage | Model | MCP servers | Repositories | Output |
| :--- | :--- | :--- | :--- | :--- | :--- |
| 1 | code analysis | `gemini-3.5-flash` | none | app | `code-context.md` |
| 2 | infra context + MySQL table storage | none — runs in this service | none | infra | `infra-context.md`, `mysql-tables.md` |
| 3 | metric collection | `gemini-3.1-flash-lite` | datadog, azure | none | `metrics.md` |
| 4 | synthesis & scoring | `gemini-3.5-flash` | none | none | `<date>-review.md`, `report-snapshot.json` |
| 5A | validation — report self-check | `gemini-3.5-flash` | none | none | `validation-static.md` |
| 5B | validation — data re-check | `gemini-3.5-flash` | datadog, azure | infra | `validation-data.md` |
| 5C | validation — recommendation soundness | `gemini-3.5-flash` | none | app, infra | `validation-advice.md` |
| 5D | validation — report assembly | none — runs in this service | none | none | `<date>-validation.md` |

Stages 1, 2, 3 and 4 are fatal; none of the validation stages is, so a validation failure still
leaves a usable report. Every run also writes `run-stats.md` with the per-stage token and latency
breakdown.

The split exists because the review runs as an agentic loop, where the whole context is re-sent on
every tool round-trip — so a stage that holds a large context *and* makes many tool calls pays the
product of the two. Stage 3 keeps a small context and does the tool work; stage 4 holds the large
context and makes no tool calls.

### Validation is split the same way, and for the same reason

Validation runs 8 checks, and they have very different appetites:

- **5A — checks 3, 4, 5, 7** re-check the report against itself: cross-source consistency, scoring
  against the thresholds, capacity projection arithmetic, format and source attribution. Answerable
  from the report alone, so this stage starts no MCP server and is given no repository. It is also
  the only one that needs the scoring thresholds and the report format spec — 27 KB of reference
  material the other two no longer carry.
- **5B — checks 1, 2, 6** re-query Datadog and Azure and re-read the infra manifests the report
  cites, to confirm the config, the metric values, and that every `NOT_COLLECTED` was genuine.
- **5C — check 8** re-verifies the report's *advice*. Each recommendation is re-checked against the
  primary source in the application repository, and fails if its premise is contradicted there,
  rests on a `NOT_COLLECTED` metric, prescribes a number traceable to nothing, points the wrong way,
  or asks for something the code already does. It gets the application repository because
  `code-context.md` is an earlier stage's output and cannot verify itself.

5A, 5B and 5C are independent, so they run **side by side**; the group's wall clock is the slowest
of the three rather than their sum. Holding them in one session was expensive in a way the stage
table does not show: every source file check 8 opened stayed in the context and was re-sent on each
later round-trip, including the ones that were only re-counting table rows. Measured on
wonder-cart-service the single validation stage spent 6.0M input tokens over 61 tool calls in 547 s
— 64 % of the run's tokens and 51 % of its wall clock.

5D merges the three partials into `<date>-validation.md`. The summary table, the issue counts and
the overall verdict are **computed there from the findings** rather than written by a model
alongside them — no session can see all eight checks any more, and a verdict derived from the
findings cannot disagree with them. A group whose stage failed leaves no partial; its checks are
reported as not run and the verdict is held below a clean PASS, because a check that did not happen
must never read as one that passed.

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

### Azure names are not the review's `env`

The `env` in the request is a Datadog tag — `uat`, `prod` — and is never the Azure resource group:
Terraform declares the group as `name = var.env`, so `uat` is `FTIUat` and `prod` is `RFProdV2`.
Stage 2 therefore reads the group out of `<env>/app/infra/env/01-variables.tf` in the local infra
repository and passes it to the metric stage, which uses it for every Azure MCP call. Pass
`azure_resource_group` in the request to override that, e.g. for a service whose resources live in
another group.

Setting `AZURE_RESOURCE_GROUP` / `AZURE_SUBSCRIPTION_ID` in the environment also works, but note that
those two variables are read by the *scheduled* pipeline's `AzureMCPStage`, which calls Azure
Resource Manager over plain HTTP. The local review reaches Azure through the Azure MCP server the
Gemini CLI starts, where the resource group is a **tool argument** — so an environment variable alone
never reaches it, and a run with no group resolved fails every Azure call with
`Resource group '<env>' could not be found`.

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
