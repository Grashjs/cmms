# CLAUDE.md

Working notes for this repository. Read before changing build, deployment or auth code.

## What this is

A fork of **Atlas CMMS** (upstream: `Grashjs/atlas-cmms`), a maintenance management
system, adapted for self-hosting in an FM-IT consulting context. Licensed **AGPL-3.0**:
serving a modified version over a network obliges us to offer the corresponding source,
including our changes. That is accepted and intentional — improvements are meant to go
back out publicly. **This repository is public. Never commit hostnames, IP addresses,
database credentials, container UUIDs or customer names.**

## Stack

| Part | Tech | Port | Image |
|---|---|---|---|
| `api/` | Spring Boot 3.2.3, Java 17, Liquibase, Quartz, Envers | 8080 | `cmms4fm-api` |
| `frontend/` | React (CRA + react-app-rewired), served by nginx | 3000 | `cmms4fm-frontend` |
| `docker/nginx/` | Single-domain reverse proxy | 80 | `cmms4fm-nginx` |
| — | PostgreSQL 16 | 5432 | upstream |
| — | MinIO (attachments) | 9000 | upstream |
| `mobile/` | React Native app | — | not deployed |

The nginx service is the **only** publicly reachable one. It routes `/` → frontend,
`/api/` → api, `/storage/` → minio. Single domain, so no CORS and one certificate.

## Commands

```bash
# API tests (the only automated test suite; ~3 min in CI)
cd api && mvn -B test -DargLine="--add-opens java.base/java.lang=ALL-UNNAMED"

# API package
cd api && mvn clean package -DskipTests

# Frontend
cd frontend && npm install --legacy-peer-deps && npm run build
cd frontend && npm run lint
```

Java 17 is the target. Only a **Windows** Maven wrapper is checked in (`api/mvnw.cmd`) —
there is no `api/mvnw`, so `./mvnw` fails on Linux and in containers. CI calls `mvn`
directly. On a machine without Maven on `PATH`, use `api\mvnw.cmd` from PowerShell.

## Deployment

**Images are built in CI and pulled by Coolify. Never build on the deployment server** —
the CRA webpack build alone needs 2–4 GB and will freeze a shared host.

```
push to main → .github/workflows/deploy.yml
             → builds api, frontend, nginx in parallel (GHA cache per image)
             → pushes ghcr.io/<owner>/cmms4fm-{api,frontend,nginx}:latest and :sha-<commit>
             → curls the Coolify deploy webhook
```

`docker-compose.yml` resolves `${IMAGE_TAG:-latest}`. To roll back, set `IMAGE_TAG` to a
`sha-<commit>` value in Coolify and redeploy. Required repo secrets: `COOLIFY_WEBHOOK_URL`,
`COOLIFY_API_TOKEN`.

`.github/workflows/tests.yml` runs the Maven suite separately so a test failure does not
block an image build, and a build failure is distinguishable from a test failure.

### Coolify behaviour worth knowing

Each of these cost a failed deployment. They are not documented upstream.

- **Relative bind mounts do not use the git checkout.** Coolify rewrites `./foo` to
  `/data/coolify/applications/<uuid>/foo` and Docker creates a *directory* when the source
  file is missing — which then cannot mount onto a file. This is why the nginx config is
  baked into an image instead of mounted. Do not reintroduce file bind mounts.
  Directory mounts (`./logo`, `./config`) are fine.
- **`container_name` is ignored.** Containers are named `<service>-<resource-uuid>-<id>`.
  Scripts must resolve names via `docker ps`, never hardcode.
- **Empty environment values are not passed through as compose defaults.** Upstream relies
  on `${VAR:- }` producing a single space to keep optional keys truthy for
  `runtime-env-cra`; that space does not survive Coolify. `frontend/docker-entrypoint.sh`
  fills blanks before generating `runtime-env.js`. Do not remove it.
- **No host port publishing.** Coolify's proxy routes to the container; a `ports:` entry
  bypasses TLS and collides with other stacks on the host.
- **Domains are per service.** Set the domain on `nginx` only. Setting it on `frontend`
  as well creates two Traefik routers competing for the same host rule, and bypasses the
  `/api/` and `/storage/` routes entirely. `PUBLIC_SERVER_URL` must match the domain
  exactly — `https://`, no trailing slash. A mismatch shows up as a loading UI with a
  failing login, which reads like a backend fault but is configuration.

## Conventions

- `.gitattributes` pins `*.sh`, `Dockerfile`, `*.conf` and `.env.example` to **LF**.
  Development happens on Windows and these files run inside Linux containers; a CRLF
  shebang or a stray `\r` in a config value breaks the container at runtime, not at build
  time. When adding a shell script, verify it byte-wise, not just by eyeballing output —
  command substitution strips trailing whitespace and will hide the problem.
- Alpine images use `ash`. No `$'\r'`, no bashisms in entrypoints.
- Commit messages: what broke and why, not just what changed.

### Adding a list filter

List pages post a `SearchCriteria` to `/<entity>/search`; `SpecificationBuilder` joins every
`FilterField` with **AND**. Two things follow, and both have already produced filters that
silently return nothing:

- **Enums are stored as ordinals.** `2026_01_10_1768015926_enums_type.xml` converted those
  columns to `SMALLINT`, and `WrapperSpecification.getRealValue` converts an incoming string
  to an enum only for `PRIORITY`, `STATUS` and `JS_DATE`, and only in the `in` branch. A
  filter carrying `"IMAGE"` therefore compares a string against a number. A new enum filter
  needs an `EnumName` entry plus a `case` — i.e. an API change. Ordinal storage also means a
  new enum constant may only be appended, never inserted.
- **Controllers append their own filter fields.** `FileController.search` adds
  `hidden eq false`, and `createdBy eq <own id>` for users without the view-other
  permission. A client-side filter on the same field is ANDed with that one, so it can only
  ever narrow to nothing. Check the controller before offering a field in the UI.

Search inputs are debounced with `useMemo(..., [])`, which freezes the closure. Read
`criteria` through a ref (`WorkOrders/index.tsx`, `Files/index.tsx`) — the `Parts.tsx`
version closes over `criteria` directly and only survives because that page has no other
filters.

## Security posture

Upstream targets a hosted multi-tenant product where public signup is a feature. For a
private single-tenant instance that default is wrong. Changes made here:

- **`UserService.signup` always requires an invitation** to join an existing organization.
  Upstream gated that check behind `enableInvitationViaEmail`, turning a mail-delivery
  setting into an authorization one: with mail off, a `POST /auth/signup` carrying
  `{"role":{"id":N}}` joined the organization owning role N *with that role*. Role ids are
  sequential and id 1 is the super-admin role, which is company-bound — so the most
  damaging path was also the cleanest one. `invite()` persists the invitation before it
  checks the mail flag, so invitations still work with mail disabled.
- **nginx returns 403** for `/api/auth/signup` and `/api/demo/generate-account`. This is
  now redundancy, not the primary control. To onboard someone, comment the signup line out
  for the duration — the backend holds the door on its own.
- `ALLOWED_ORGANIZATION_ADMINS` restricts who may create a *new* organization. Empty means
  anyone.

### Known upstream issue: the default super admin

`ApplicationInitializer` creates `superadmin@test.com` with the hardcoded password
`pls_change_me` on first start. Both values are in the public upstream source, and
`/auth/signin` is not covered by the nginx block — no exploit needed, just the login form.

**Do not delete this account.** The initializer checks for its *existence* on every start
and recreates it with the default password if absent. Disabling it (`enabled = false`)
survives restarts and is the correct mitigation; `CustomUserDetail.isEnabled()` is checked
during authentication, so a disabled account cannot log in. Replacing the password hash is
a useful second lock. A proper fix — refusing to boot with a default password, or
generating one — has not been implemented yet.

### Self-hosting premium unlock

Upstream gates API access (`x-api-key`), webhooks, workflows, CSV import and the usage
limits (assets, work orders, users, ...) behind two stacked checks: the company's
`SubscriptionPlan` features *and* a `LicenseEntitlement` that `LicenseService` validates
against `api.keygen.sh`. For a private single-tenant instance that is both a paywall
around AGPL source we already have and a hard dependency on a third party.

`SELF_HOSTED_UNLOCK_PREMIUM=true` (default `false`) opens both gates without any outbound
call:

- `LicenseService.getLicensingState()` short-circuits to a fully-entitled state (every
  `LicenseEntitlement`, `usersCount = MAX`) — this covers `hasEntitlement(...)` and every
  `checkUsageBasedLimit(...)` across the services.
- `ApplicationInitializer` grants the **FREE** plan all `PlanFeatures` on boot. Every
  company defaults to FREE ([`SubscriptionService`](api/src/main/java/com/grash/service/SubscriptionService.java)),
  so this unlocks the plan-gated half of the checks instance-wide.

Both halves are required together — the `x-api-key` path checks license `AND` plan in
`ApiKeyAuthFilter` and `ApiKeyService.create`. The asset/work-order REST endpoints
themselves are **not** license-gated; only `ROLE_CLIENT` + the create permission. So a
plain JWT login (`/auth/signin`) can already create assets without this flag — the flag is
for the convenient long-lived API-key path and the rest of the premium surface.

Leave it `false` on any hosted/multi-tenant deployment; it removes the paywall the
upstream billing model relies on. **Caveat:** the FREE-plan change is persisted, so turning
the flag back off does not re-lock FREE — reset its feature set by hand if you need
upstream FREE behaviour again. When syncing upstream, re-check `LicenseService`,
`ApplicationInitializer` and `ApiKeyAuthFilter`, since our changes live there.

## Open items

- Healthchecks with `condition: service_healthy` for postgres and minio. Currently only
  `depends_on`, so a cold start relies on the api container losing the race gracefully.
- Make the nginx signup block toggleable via an environment variable (`envsubst` templates
  are supported by the nginx image) instead of editing the config.
- Eliminate the default super admin password in code.

## Upstream

`dev-docs/` holds upstream documentation (TLS, LDAP, disabling users, running SQL,
backups). It describes the upstream deployment model, not ours — treat compose snippets
there as illustrative.

When syncing upstream changes, re-check the files we have diverged in. This list is what a
merge has to walk, so keep it accurate — a wrong entry wastes time, a missing one gets a
fix silently overwritten:

| Area | Files |
|---|---|
| Signup hardening | `UserService.signup` |
| Premium unlock | `LicenseService`, `ApplicationInitializer`, `application.yml` |
| Category-bound custom fields | `CustomField`, `CustomFieldService`, `CustomFieldValueService`, `CustomFieldRepository`, `AssetService.setAssetCustomFields` |
| Work order → purchase order | `PurchaseOrder`, `PurchaseOrderService`, `PurchaseOrderController`, `PurchaseOrderRepository` |
| Light sidebar | `layouts/ExtendedSidebarLayout/Sidebar/**`, `theme/schemes/*.ts` |
| File search and filters | `content/own/Files/index.tsx`, `content/own/Files/Filters/**` |
| Container plumbing | frontend `Dockerfile` + `docker-entrypoint.sh`, `docker/nginx/**`, `docker-compose.yml` |

`ApiKeyAuthFilter` is **not** in that list. It reads the license and plan gates that
`SELF_HOSTED_UNLOCK_PREMIUM` opens, so it is worth reading to understand the unlock — but
it is untouched upstream code and needs no merge attention.
