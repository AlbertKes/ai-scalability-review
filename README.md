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
| 2 | infra context | none — runs in this service | none | `infra-context.md` |
| 3 | metric collection | `gemini-3.1-flash-lite` | datadog, azure, `<host>-<db>` | `metrics.md` |
| 4 | synthesis & scoring | `gemini-3.5-flash` | none | `<date>-review.md`, `report-snapshot.json` |
| 5 | validation | `gemini-3.5-flash` | datadog, azure, `<host>-<db>` | `<date>-validation.md` |

Stages 1, 2, 3 and 4 are fatal; validation is not, so a validation failure still leaves a usable
report. Every run also writes `run-stats.md` with the per-stage token and latency breakdown.

The split exists because the review runs as an agentic loop, where the whole context is re-sent on
every tool round-trip — so a stage that holds a large context *and* makes many tool calls pays the
product of the two. Stage 3 keeps a small context and does the tool work; stage 4 holds the large
context and makes no tool calls.

### Prerequisites

The Gemini CLI must be on `PATH` and its MCP servers configured in `~/.gemini/settings.json` under
the names `datadog`, `azure`, and `<mysql-host-short-name>-<mysql-db>` (e.g.
`rfprodv2-flexible-wonder-db-wonder_cart`). Each stage is started with only the servers it needs, so
a server configured under a different name is simply not available to the run.

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
