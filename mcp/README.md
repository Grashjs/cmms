# cmms4fm-mcp

An MCP server over the CMMS REST API. Design and reasoning: [`docs/mcp-server-konzept.md`](../docs/mcp-server-konzept.md).

It is a **stateless proxy**. It holds no database, no session, no cache, and above all no
identity: every call carries the caller's own `x-api-key` to `/api`, where `ApiKeyAuthFilter`
resolves it to a user and applies that user's role, permissions and company. The server adds
no privilege and cannot exceed the key it was handed. Compromise it and you gain exactly
what that key already allowed.

## What it does, in one paragraph

At boot it fetches the OpenAPI document from the running CMMS and derives one tool per
endpoint — 348 of them from the current document. A **profile** decides which of those a
client actually sees, because a model handed 348 tools chooses badly. About thirty core
endpoints carry hand-written names and descriptions (`search_assets`, `get_asset`,
`search_work_orders`); the rest get names and descriptions derived from the document. A
discovery tool, `list_capabilities`, searches the whole surface including the hidden part, so
an agent can report "this exists but is not enabled here" instead of concluding the CMMS
cannot do it.

## Before it works at all

`SELF_HOSTED_UNLOCK_PREMIUM=true` must be set on the `api` service. Upstream gates the entire
`x-api-key` path behind the `API_ACCESS` entitlement *and* the plan feature, both of which
that flag opens (CLAUDE.md, "Self-hosting premium unlock"). Without it every tool call answers
`403 {"message":"Access denied"}` and nothing anywhere says why — which is why this server
attaches that hint to any 403 whose message is exactly `Access denied`.

To check without credentials, ask the API itself — `/license/state` is `permitAll`:

```bash
curl -s https://<domain>/api/license/state | grep -o 'API_ACCESS'
```

An answer means the gate is open. On the deployed instance it already is.

Then create an API key in the CMMS under **Settings → Integrations → API Keys**, for a user
whose permissions are the ones this client should have. That user is the real security
boundary; the profile below is only about what the model can see.

## Running it

```bash
npm ci
npm test          # compiles with strict tsc, then runs the suite
npm run build && npm start
```

Local run against the deployed instance, read-only, over stdio — the shape a desktop client
uses:

```bash
CMMS_BASE_URL=https://cmms.example.com/api \
CMMS_API_KEY=<your key> \
MCP_TRANSPORT=stdio \
PROFILE=core-readonly \
node dist/src/index.js
```

For a client entry (Claude Desktop, MCP Inspector), point `command` at `node` and `args` at
`dist/src/index.js`, and put the variables above in that client's `env` block.

In the deployed stack the server runs as the `mcp` service behind nginx at `/mcp`, so a remote
client connects to `https://<domain>/mcp` over Streamable HTTP and sends its key as an
`x-api-key` header.

## Configuration

Every setting, with its default, is in [`.env.example`](.env.example). The four that decide
how the server behaves:

| Variable | Effect |
|---|---|
| `PROFILE` | Which tools are visible. `readonly` (default), `core`, `core-readonly`, `assets`, `workorders`, `full` |
| `READ_ONLY` | Hides every writing tool whatever the profile says — the last word, it outranks `TOOLS_ALLOW` |
| `AUTH_MODE` | `passthrough` (each client brings its own key) or `service` (the server holds one) |
| `RATE_LIMIT` | Tool calls per minute per key. `0` disables it |

`TOOLS_ALLOW` and `TOOLS_DENY` take comma-separated globs (`get_*`, `*_assets_*`) on top of
the profile; a deny always wins.

### Choosing a profile

- **A client that processes input nobody vouched for** — an uploaded PDF, an incoming email:
  `PROFILE=core-readonly` **and** a key belonging to a read-only user. Two independent locks,
  because prompt injection through a document has to fail even if one of them is wrong. This
  is the rule [`ki-meldungs-triage.md`](../docs/ki-meldungs-triage.md) sets and the concept
  adopts: untrusted input must not be able to write, and writing stays something a person
  triggers in the frontend.
- **An assistant for a person doing maintenance work**: `PROFILE=core` or `workorders`, with a
  key belonging to that person, so the CMMS's own audit trail records who did it.
- **`PROFILE=full`** exists for exploration. It is 348 tools; expect worse tool selection, not
  better coverage.

## What it deliberately does not do

- **No business logic.** A tool call is "call this endpoint cleanly" and nothing else. No
  validation, no computation, no multi-step composition. The CMMS decides.
- **No LLM.** The server never calls a model and never sends anything outward. Whether FM
  data may cross a boundary is the client's decision and stays visible as such.
- **No second permission system.** No roles of its own, no tenant logic. Profiles govern
  *visibility*; permission is the CMMS's answer alone.

## Design decisions the code makes, and why

These are the places where the implementation had to choose, and each cost something to find
out. They are documented at the top of the file that owns them.

**Tool names come from method plus path, never from `operationId`**
([`src/tools/naming.ts`](src/tools/naming.ts)). springdoc names operations after the Java
method and disambiguates collisions with a scan-order counter, so this document calls
`POST /assets/search` → `search_15` and `PATCH /assets/{id}` → `patch_40`. That is unusable
for a model *and* unstable: adding one controller upstream renumbers unrelated tools and
silently breaks every client allowlist. `post_assets_search` changes only when the endpoint
does.

**The read/write split cannot come from the HTTP method**
([`src/tools/describe.ts`](src/tools/describe.ts)). Every list endpoint in this API is a
**POST** carrying a filter body, and so is every analytics query. Classify those as writing
and `PROFILE=readonly` can no longer find anything, which makes the read-only deployment
useless rather than safe. The read-only POST shapes are therefore an explicit list; anything
not on it counts as writing.

**Curation is mandatory, not polish** ([`src/tools/curated.ts`](src/tools/curated.ts)). The
document describes **6** of its 373 operations — all of them webhook endpoints plus one
histogram. The other 367 arrive with no summary and no description, so generated tools would
tell a model the HTTP method and nothing more.

**The document is loaded at runtime, not generated into code**
([`src/openapi/loader.ts`](src/openapi/loader.ts)). This is what makes "a new endpoint
upstream becomes a tool" true without a code-generation step that can drift, and it is why
readiness means "the document is loaded". `SPEC_REFRESH_MINUTES` re-reads it without a
redeploy. It also means the tool set depends on a running CMMS — hence `SPEC_FILE` as a
fallback.

**The document lives at the group URL.** `/v3/api-docs` answers **404** on this API:
`springdoc.enable-default-api-docs` is false in `application.yml`, so only the named group
`/v3/api-docs/atlas-cmms` serves it. This looks exactly like the API being down.

**Input schemas are inlined with two guards** ([`src/openapi/schema.ts`](src/openapi/schema.ts)).
OpenAPI 3.1 schemas already *are* JSON Schema, so no dialect translation is needed — but
entity DTOs reference each other, and `PreventiveMaintenancePostDTO` inlines to about 130 KB.
Cycles become untyped objects; anything past `MAX_SCHEMA_CHARS` is pruned to its top level,
with the full definition still readable through `cmms://schema/{name}`.

**Failures are translated, not forwarded** ([`src/cmms/errors.ts`](src/cmms/errors.ts)). Each
result carries a `kind`, a `retryable` verdict and an `advice` line, because an agent that
sees only "something went wrong" retries a 403 forever and gives up on a 503. A 500 with a
message is a business refusal in this codebase (usage limits are thrown as bare
`RuntimeException`s), so its text survives and it is marked not retryable.

**Some endpoints are never offered** ([`src/tools/describe.ts`](src/tools/describe.ts)):
`/auth/**`, `/subscriptions/**`, `/subscription-plans*`, `/notifications/push-token`. Not a
permission decision — they stay reachable over REST — but an agent has no business issuing
credentials, ending sessions or changing billing, and offering them only creates a path for an
injected instruction to reach them. File-upload endpoints are excluded too, because binary
content cannot travel in a JSON tool call; `list_capabilities` reports the reason rather than
letting them vanish.

**The low-level SDK `Server`, not `McpServer`** ([`src/server.ts`](src/server.ts)).
`registerTool` wants Zod schemas; ours are JSON Schema derived at runtime. Converting to Zod
and back would be a lossy round trip to produce what we already have.

**nginx resolves the service lazily** ([`docker/nginx/nginx.conf`](../docker/nginx/nginx.conf)).
An `upstream` block is resolved once at startup and nginx refuses to start with "host not
found in upstream" — a stopped `mcp` service would black out the whole domain. A variable in
`proxy_pass` defers the lookup, so `/mcp` answers 502 and everything else keeps serving.

## Audit

One line per tool call on **stderr** (stdout is the protocol on stdio): time, tool, endpoint,
HTTP status, failure kind, duration, and a 12-character hash of the key. Never the key,
never the arguments, never the response. That answers "who called what and how did it end",
which is the question an audit is for, without turning the log into a copy of the data.

## Tests

`npm test` compiles with `strict` and runs the suite; both the image build and CI run the
same thing. Most assertions run against
[`test/fixtures/api-docs.json`](test/fixtures/api-docs.json) — the real document, saved
verbatim — because the things that break here are properties of the real document (generated
`operationId`s, POST-based search, absent descriptions) that no hand-written stub would
reproduce. [`test/e2e.test.ts`](test/e2e.test.ts) drives an MCP client against the server
against a stand-in CMMS and checks the parts unit tests cannot: that the key survives the
trip, that a 403 comes back structured, that a hidden tool stays uncallable, that the rate
limit bites, and that the audit line contains neither the key nor the data.

Refreshing the fixture after an upstream sync that changed the API:

```bash
curl -s https://<domain>/api/v3/api-docs/atlas-cmms > mcp/test/fixtures/api-docs.json
npm test    # a changed count or a renamed endpoint fails here, which is the point
```
