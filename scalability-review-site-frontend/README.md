# Scalability Review Console

A local web console for the one-click scalability review. It turns the pipeline from an API call
into something you can use: start a review from a form, watch it stage by stage, read the report as
HTML, compare it against the last run of the same service, and see exactly what it cost in tokens,
money and wall-clock time.

Everything runs on your own machine against your own MongoDB and your own Gemini CLI. There is no
login, and the dev server binds to `127.0.0.1` on purpose — nothing here should be reachable from
the network.

The pipeline itself is documented in [`../README.md`](../README.md).

---

## Running it locally

### 1. Prerequisites

| | |
| :--- | :--- |
| **MongoDB** on `localhost:27017` | `docker run -d --rm --name review-mongo -p 27017:27017 mongo:7` |
| **Gemini CLI** on `PATH` | `gemini --version` must work in the shell that starts the backend |
| **Vertex AI credentials** | `gcloud auth application-default login` |
| **Node 20+ and yarn** | the frontend was built and verified on Node 26 / yarn 1.22 |
| **Azure Artifacts token** | `@wonder/core-fe` comes from the private registry; see below |

The console starts and is useful without the Gemini CLI or the credentials — you simply cannot
start a review. The **Settings → Environment** panel tells you exactly what is missing and what to
do about it.

### 2. Start the backend

From the repository root:

```bash
./gradlew :ai-scalability-review-service:run
```

It listens on `http://localhost:8080`. (`sys.http.listen=8080` is set in `sys.properties`; without
it core-ng would serve HTTPS on 8443 with a self-signed certificate, which the dev proxy cannot use.)

### 3. Start the frontend

```bash
cd scalability-review-site-frontend
export BASE64_ARTIFACT_ACCESSTOKEN=$(printf '%s' "$ARTIFACT_ACCESSTOKEN" | base64)   # once per shell
yarn install
yarn dev
```

Open **http://127.0.0.1:7480**.

`/review`, `/local-review`, `/notification`, `/settings`, `/preset` and `/service-config` are
proxied to `http://localhost:8080`, so the browser stays on one origin and the backend needs no CORS
configuration. Point the proxy somewhere else by putting `VITE_API_BASE` in `.env.local`.

### 4. Before the first review

Open **Settings** and fill in:

- **Default operator** — pre-fills the trigger form.
- **Model prices** — the console ships with the table empty. Token counts are exact everywhere, but
  every dollar figure stays blank until you enter the per-million rates, because there is no
  authoritative rate card in this repository and an invented number presented as a cost is worse
  than a blank. Cached input has its own column: it is usually most of the input, so folding it into
  the input rate overstates a run by a multiple.
- **Budgets** — the documented 1,500,000-token target is a pilot figure that real runs already
  exceed. Set it from your own baseline, otherwise the "over budget" badge appears on every run and
  stops meaning anything.

---

## What the console does

### Starting a review

The trigger form covers every field of the request, grouped into Target, Repositories, Data sources,
Run options and Models (with the per-stage defaults shown).

**Validation runs before anything is created, and reports every problem at once.** Two bad repository
paths produce two errors, not one after the other; an unknown model id or a non-existent
`app_code_paths` entry is caught here rather than minutes into the run inside the CLI subprocess.
Errors block the submit; warnings — a data source that will come back as `NOT_COLLECTED`, a stage you
chose to skip — do not, but you have to tick *run anyway*, and that acknowledgement is stored with
the run so a later reader knows it started with known gaps. Fields with problems are flagged inline,
their section auto-expands, and the banner at the top links to each one.

The form also shows an **environment preflight** (Gemini CLI, credentials, MCP servers, repositories,
MongoDB) and a **cost estimate derived from this service's own previous runs**, counting only the
stages this run will actually execute.

Parameter sets can be saved as **presets** per service and environment.

### Watching it run

The run page shows all five stages in pipeline order, including the ones that have not started yet,
each with its status, duration, model, token split, MCP call count and estimated cost, plus the
artifacts it wrote as soon as they land on disk.

A stage's captured CLI output can be tailed live. Note the hard limit: the CLI is invoked with
`--output-format json` and emits nothing on stdout until a stage ends, so the only live signal is
its stderr, and stage granularity is the best fidelity there is. An elapsed timer in the header is
what tells you a seven-minute stage is alive rather than stuck.

A failed stage shows its error together with a known-cause hint — the Azure resource group is not the
`env`, an inherited `GOOGLE_API_KEY` took over the run, Azure MCP needs the VPN, and so on. A run can
be **cancelled** (the subprocess is destroyed) or **re-run from any stage**, reusing the artifacts of
the stages before it.

### Reading the report

The report renders as HTML with a sticky table of contents, a score header and a recommendation
summary. Raw HTML in the markdown is never rendered — `rehype-raw` is deliberately not installed, so
the model's output cannot inject markup into your browser.

`[Source: …]` attributions are shown as muted chips and can be switched off for a clean read; they
are decorated in the rendered tree rather than rewritten in the markdown, so the text stays byte for
byte what the pipeline produced. The report can be exported as self-contained HTML, as markdown, or
printed.

An unscored dimension shows `UNKNOWN`, never green. If the collected metrics contain `NOT_COLLECTED`
markers, a banner says how many and why — a metric that could not be collected is not a zero.

### Validation, provenance and multiple runs

The **Validation** tab shows stage 5's verdict and its eight checks; the verdict also appears as a
column in the run list, so you can see at a glance whether a report passed its own checks.

The **Provenance** tab records what produced the report: Gemini CLI version, prompt version, both
repository commits and whether they were dirty, and the resolved Azure resource group. Without those,
two runs of the same service cannot be told apart as "the service changed" versus "the pipeline
changed".

Every run writes into its own directory, `<base>/<service>/<yyyyMMdd-HHmmss>-<jobId>`, with a
`latest` symlink for the existing command-line workflow. Runs of one service can be compared side by
side: score changes, key metric deltas, recommendation counts, cost and latency, and a deterministic
line-level diff of the infrastructure each run actually read.

### Cost, usage and notifications

Per run: total and per-stage tokens (input, output, cached), API requests, tool calls, MCP calls,
duration, estimated cost and budget status. Across runs: spend grouped by stage, model, service or
day, the most expensive runs, month-to-date cost against an optional ceiling, and tokens-per-MCP-call
— the number that exposes the agentic-loop cost shape the stage split exists to avoid.

The bell in the top-right carries an unread count that survives a reload, because a review outlives
the tab that started it. It fires on completion, failure and cancellation, and optionally on a failed
stage, a budget overrun, a `FAIL` verdict, a `RED` score or a run that went stale. Desktop
notifications and a Slack webhook are opt-in in Settings.

---

## Project layout

```
src/
  index.tsx             bootstrap
  conf/config.ts        env-derived config
  constants/review.ts   stage names, statuses, score colours, model ids
  route/                route table and menu
  type/api.ts           backend contract (generated — see below)
  type/state.ts         RootState, one slice per page
  service/              one class per backend web service
  components/           StatusTag, ScoreBadge, Markdown, Message
  utils/                formatters, markdown helpers, csv, hooks
  page/<Name>/          index.ts · module.ts · page.tsx · state.ts · type.ts
```

One page is one core-fe module backed by one slice of `RootState`. Adding a page means four
registrations — `src/type/state.ts`, the `routes` array, the `menus` array if it needs a nav entry,
and the directory itself. Keep the directory name, the state key and the route path identical.

### `type/api.ts` is generated

It mirrors `ai-scalability-review-service-interface`. Regenerate it against a running backend rather
than editing it by hand:

```bash
npx @wonder/core-fe-cli api --api-url http://localhost:8080/_sys/api
```

## Commands

```bash
yarn dev         # dev server on 127.0.0.1:7480
yarn typecheck   # tsc --noEmit
yarn lint        # eslint
yarn format      # prettier
yarn build       # production build into build/dist
```

`yarn build` does not run `typecheck`, so run both after a change.

## Code style

Prettier is configured; the two counter-intuitive settings are `bracketSpacing: false`
(`import {x} from "y"`) and `arrowParens: "avoid"` (`s => s.trim()`). Tab width 2, print width 120.
ESLint enforces `react/jsx-boolean-value: never`, `object-shorthand` and `prefer-const`.

TypeScript `strict` is off, matching the sibling frontends — the `| null` annotations are
descriptive, not enforced, so check for null explicitly.

## Known limits

- **No progress inside a stage.** `--output-format json` means the CLI reports nothing until a stage
  finishes. Stage status, the elapsed timer and the stderr tail are the whole picture.
- **Costs are estimates** computed from the price table you enter, labelled with the table version.
- **Live updates are polled**, not pushed: 3 s on an open run, 10 s for notifications, and the run
  list only refreshes while something is actually running.
- **No authentication.** Bind it to localhost and keep it there.
