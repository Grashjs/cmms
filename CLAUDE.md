# CLAUDE.md

Working notes for this repository. Read before changing build, deployment or auth code.

Design documentation for individual subsystems lives in [`docs/`](docs/README.md):
[`docs/reporting.md`](docs/reporting.md) for the `rpt_*` reporting views, saved list views and
filtered exports — read it before changing anything under `controller/analytics/`, the `rpt_*`
views, or the CSV export; [`docs/terminology-de.md`](docs/terminology-de.md) for the partly
finished German wording migration, before editing `de.ts`; [`docs/TECHNICAL_DEBT_REMEDIATION.md`](docs/TECHNICAL_DEBT_REMEDIATION.md) for the frontend dependency backlog — read it before any dependency upgrade, it records how deep each package sits and which one blocks React 18;
[`docs/custom-field-categories.md`](docs/custom-field-categories.md) for custom fields bound to
asset categories — read it before touching `CustomFieldValueService`, which deliberately
*discards* a value for the wrong category instead of refusing the request, and before deciding
anything about `mobile/`.

## What this is

A fork of **Atlas CMMS** (upstream: `Grashjs/cmms`, renamed from `atlas-cmms`), a maintenance management
system, adapted for self-hosting in an FM-IT consulting context. Licensed **AGPL-3.0**:
serving a modified version over a network obliges us to offer the corresponding source,
including our changes. That is accepted and intentional — improvements are meant to go
back out publicly. **This repository is public. Never commit hostnames, IP addresses,
database credentials, container UUIDs or customer names.**

## Stack

| Part | Tech | Port | Image |
|---|---|---|---|
| `api/` | Spring Boot 3.2.3, Java 17, Liquibase, Quartz, Envers | 8080 | `cmms4fm-api` |
| `frontend/` | React 17 + Vite, served by nginx | 3000 | `cmms4fm-frontend` |
| `docker/nginx/` | Single-domain reverse proxy | 80 | `cmms4fm-nginx` |
| — | PostgreSQL 16 | 5432 | upstream |
| — | MinIO (attachments) | 9000 | upstream |
| `mobile/` | React Native app (Expo 53 / RN 0.79) | — | not built here, **unmodified upstream** |

The nginx service is the **only** publicly reachable one. It routes `/` → frontend,
`/api/` → api, `/storage/` → minio. Single domain, so no CORS and one certificate.

## Commands

```bash
# API tests (the only automated test suite; ~3 min in CI)
cd api && mvn -B test -DargLine="--add-opens java.base/java.lang=ALL-UNNAMED"

# API package
cd api && mvn clean package -DskipTests

# Frontend
cd frontend && npm install --legacy-peer-deps && npm run build   # vite build, ~1 min
cd frontend && npm run lint
cd frontend && npm start                                       # vite dev server on 3000
```

**`mvn compile` is not a check.** Without `clean` the compiler plugin's incremental pass can
leave an edited file untranslated and still exit 0 — a type error that fails CI looks green
locally. The image build runs `mvn clean package -DskipTests`; use that, or nothing.

**The template tests fail locally on a machine whose system locale is not English, and that
is the machine's fault, not the code's.** `AbstractTemplateTest` renders with
`Locale.ENGLISH`, but there is no `mailMessages_en.properties` — only the base bundle. Java's
`ResourceBundle` fills that gap by falling back to the *JVM default locale* before it falls
back to the base bundle, so on a German workstation every one of the eighteen tests in
`MainLayoutConsumerTemplatesTest`, `MainLayoutTemplateTest` and `WorkOrderReportTemplateTest`
renders German text and fails its first `assertTrue(html.contains(...))`. CI runs in English
and is green. They all fail identically and at the first assertion, which makes them look like
one broken shared fragment — they are not. Run them with `-Duser.language=en -Duser.country=US`
to see them pass, or ignore exactly those eighteen. Making them locale-independent would mean
`setFallbackToSystemLocale(false)` on the message source, which is upstream's file and a good
candidate to contribute back.

**Local Maven tests are usable again.** The old note that the suite cannot run above JDK 22
(Byte Buddy refusing newer class files) no longer holds — after the 2026-08-26 upstream sync
the full suite runs on JDK 25. Only the five `*IntegrationTest` classes still error, and only
because Testcontainers needs a running Docker.

**The frontend build type-checks again, and `mvn compile`'s lesson applies here too.**
`npm run build` is `tsc --noEmit && vite build`: Vite itself never looks at types — esbuild
strips them — so the compiler runs in front of it and a type error stops the build before the
bundler starts. `npm run typecheck` runs the same check alone.

This was broken for a long time and worth knowing about, because the failure mode was silent.
TypeScript 4.7.3 could not *parse* the i18next type definitions and died with 88 syntax errors
inside `node_modules` before reaching `src/` — so an empty error list for `src/` meant the
check had not run, not that it passed. TypeScript 5.9.3 reads them fine. Turning the check
back on surfaced 34 real errors that had accumulated behind it; they are fixed, and the wiring
was verified by planting a type error and confirming the build fails with exit 2 without Vite
running.

## Deployment

**Images are built in CI and pulled by Coolify. Never build on the deployment server** —
the frontend build is the memory-hungry step and will freeze a shared host.

```
push to main → .github/workflows/deploy.yml
             → builds api, frontend, nginx in parallel (GHA cache per image)
             → pushes ghcr.io/<owner>/cmms4fm-{api,frontend,nginx}:latest and :sha-<commit>
             → curls the Coolify deploy webhook
```

`docker-compose.yml` resolves `${IMAGE_TAG:-latest}`. To roll back, set `IMAGE_TAG` to a
`sha-<commit>` value in Coolify and redeploy — note the tag carries the **full** commit sha,
not the short one. Required repo secrets: `COOLIFY_WEBHOOK_URL`, `COOLIFY_API_TOKEN`.

**The builder image's Node version is coupled to Vite and nothing enforces it.** `vite@8`
declares `engines.node "^20.19.0 || >=22.12.0"`; the frontend Dockerfile was on `node:21.6.1`,
which satisfies neither, and the image build failed while every local build stayed green —
developers here run a much newer Node, and npm only *warns* on an engines mismatch instead of
refusing to install. So the mismatch is invisible until CI, and it looks like the application
broke rather than the toolchain. When bumping Vite, read `engines` and check the `FROM` line.

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
  Despite the name, `runtime-env-cra` survived the move off Create React App untouched: it
  only reads the key list from `.env` and writes `window.__RUNTIME_CONFIG__` into a JS file
  in the served directory, which has nothing to do with the bundler. `index.html` loads it
  with `defer` *before* the app's module script, and both being deferred means they run in
  document order — so the config is in place before the app reads it. Keep that order.
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

Two more traps in the same area:

- **`query.distinct(true)` is not a safe fix for the duplicate rows** an `inm` (many-to-many)
  filter produces. Work orders sort by `asset.name`, `location.name`, `category.name` and
  `primaryUser.firstName`; Postgres rejects `SELECT DISTINCT` with an `ORDER BY` expression
  that is not in the select list, so the page 500s as soon as both are active. Use an
  IN-subquery on the root id instead of a join if this ever needs fixing.
- **`File`'s to-many sides are not all wired to the table that holds the data.**
  `File.assets` mirrors `Asset.files` onto `t_asset_file_associations` and works.
  `File.workOrders` originally mapped `T_WorkOrder_File_Associations`, but attachments live
  in `work_order_files` — the default name Hibernate derives because `WorkOrderBase.files`
  declares no `@JoinTable`. Both tables exist, so `ddl-auto: validate` passes and the wrong
  one simply stays empty. `File.parts`, `File.locations` and `File.Requests` have not been
  checked; `request_files` next to `t_request_file_associations` suggests the same split.

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

### "Wrong credentials" used to mean anything at all

For minutes after every deploy the login form claimed the password was wrong. Three layers
each turned "the api is not ready" into an authentication verdict, and all three are fixed —
if a login failure ever looks implausible again, re-check them in this order:

- nginx and the frontend are static and serve the login form within milliseconds. The api
  needs Liquibase, Hibernate and Quartz — tens of seconds. `depends_on` alone waits for the
  container to *start*, not to be *ready*, so the api could also outrun postgres. Postgres now
  has a `pg_isready` healthcheck the api waits on, and the api has one on
  `/actuator/health/readiness`.
- `UserService.signin` caught `AuthenticationException` and answered 403 "Invalid
  credentials". `InternalAuthenticationServiceException` extends it, and Spring wraps
  *anything* that fails while loading the user in it — including an unreachable database. It
  is now caught first and answers 503.
- `utils/api.ts` threw away the HTTP status, and `LoginJWT` mapped every rejection to
  `wrong_credentials`. The status now travels on `ApiError`; use `isServerUnavailable(err)`
  rather than assuming a failed call means the user got something wrong.

Healthcheck note: probe `/actuator/health/readiness`, never plain `/actuator/health`. The
aggregate includes the mail indicator, which defaults to on (`ENABLE_MAIL_HEALTH_CHECK`) and
reports DOWN without SMTP configured — a probe on it never turns green. The readiness group
is pinned to `readinessState,db` in `application.yml` for exactly this reason.

### Known upstream issue: the default super admin

`ApplicationInitializer` creates `superadmin@test.com` with the hardcoded password
`pls_change_me` on first start. Both values are in the public upstream source, and
`/auth/signin` is not covered by the nginx block — no exploit needed, just the login form.

**Status: mitigated in the database, not in code.** The account on the deployed instance is
disabled (`enabled = false`), which is the correct mitigation — `CustomUserDetail.isEnabled()`
is checked during authentication, so it cannot log in, and the account still exists so the
initializer leaves it alone. Replacing the password hash is a useful second lock. But the fix
lives in the data, not the source: **any environment that starts against a fresh or restored
pre-mitigation database is vulnerable again from the first boot**, silently and with no log
line saying so. Re-check after every restore, every new environment and every database reset:

```sql
SELECT id, email, enabled FROM users WHERE email = 'superadmin@test.com';
-- expected: enabled = false. If it is true, or the row is missing, act.
```

**Do not delete this account.** The recreation guard is not "does this email exist" but
`userService.findByCompany(<super-admin company>).isEmpty()` — the initializer recreates the
account with the default password whenever the super-admin role's company has no users at all.
Two consequences: deleting the account brings it straight back with `pls_change_me`, and if
another user happens to sit in that company, deleting it does *not* bring it back and the
instance is left with no super admin at all.

**The code-level fix is in.** `getSuperAdminSignupRequest` no longer hardcodes
`pls_change_me`; it generates a random password and logs it once at WARN. Logging it is
deliberate — without it a fresh instance has no way in at all — but it means the credential
appears in the boot log of the very first start and nowhere else. A fresh database is
therefore no longer reachable from the public source, and the instruction above still applies
to whoever reads that log: sign in once, change it, disable the account, do not delete it.

`dev-docs/SuperAdmin password update guide.md` describes signing in with the default password
in order to change it. That path does not work while the account is disabled, which is intended.

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

- **The Coolify deploy webhook has been failing, so nothing has auto-deployed.** All three
  images build and reach GHCR; the `Trigger Coolify` job then fails on its single `curl -fsS`.
  The instance is up and the endpoint is fine — `GET /api/v1/deploy` without credentials
  answers 401 where an invented path answers 404 — which points at `COOLIFY_API_TOKEN` being
  unset, wrong or expired rather than at a stale URL. Note the workflow omits the
  `Authorization` header entirely when the secret is empty, which produces exactly this 401.
  Until it is fixed, deploying means clicking Deploy in Coolify by hand; `:latest` already
  carries the newest build.
- Make the nginx signup block toggleable via an environment variable (`envsubst` templates
  are supported by the nginx image) instead of editing the config.
- **The api healthcheck has never once been green.** It probes
  `/actuator/health/readiness`, but `WebSecurityConfig` does not permit that path, so
  Spring Security answers 403 and the probe fails from the first second of every container's
  life (`FailingStreak` in the thousands). Nothing depends on the check, so it gates nothing —
  which is exactly why it went unnoticed. The fix is one line in the permit list:
  `.requestMatchers("/actuator/health/readiness", "/actuator/health/liveness").permitAll()`.
  Worth doing: a signal that is permanently red trains everyone to ignore it, and on this
  codebase misleading health and error signals have already cost hours (see "Wrong
  credentials" above).
- **`POST /work-orders/search` returns the whole company** for a user who has no work-order
  view permission at all. `WorkOrderService.getSearchCriteria` only ever narrows: it adds the
  company filter, then adds the own-records filter *inside* an `if (viewPermissions contains
  WORK_ORDERS)`, so lacking that permission means no narrowing rather than no access.
  Upstream behaviour, found while building the filtered export. `AssetService.getSearchCriteria`
  gets this right (it throws), which is the shape the fix should take. Reporting-specific
  detail in [`docs/reporting.md`](docs/reporting.md#5-stage-1--filtered-export).

## Upstream

`dev-docs/` holds upstream documentation (TLS, LDAP, disabling users, running SQL,
backups). It describes the upstream deployment model, not ours — treat compose snippets
there as illustrative.

**The upstream repository was renamed** from `Grashjs/atlas-cmms` to `Grashjs/cmms`; the old
path 404s. The remote is now configured as `upstream`, so `git fetch upstream` works.

**Upstream is alive and fast — measured 2026-08-26, fork point 2026-08-02:** 264 commits
ahead, against 55 of ours. That is roughly eleven a day, so the gap is not a backlog that
sits still. What matters is where they land:

| | commits |
|---|---:|
| touching `api/` | 186 |
| touching `frontend/` | 45 |
| touching a file this fork has diverged in | ~10 |

The divergence list below is therefore cheaper to walk than its length suggests — the bulk of
upstream's work is backend, where this fork barely differs.

**Upstream modernises the backend and not the frontend.** They are on Liquibase 5, Thymeleaf 6,
JWT 0.13, google-cloud-storage 2.64 while this fork sits on 4.22 / 5 / 0.11 / 2.0.1 — so a sync
delivers dependency currency that would otherwise be hand-work. Their frontend, meanwhile, is
still React 17.0.2, MUI 5.8.2, TypeScript 4.7.3, react-scripts and `@mui/styles`: unchanged
since the fork point. **Anyone considering React 18/19 or a MUI major here should weigh that
first** — upstream has 45 frontend commits in the same window, and moving ahead of them turns
the one cheap area of the merge into the expensive one.

They have also *removed* `firebase`, `axios` and `jsonwebtoken` from the frontend, which
between them account for one of the two remaining critical findings and six high ones. Syncing
clears those without a single risky major upgrade.

When syncing upstream changes, re-check the files we have diverged in. This list is what a
merge has to walk, so keep it accurate — a wrong entry wastes time, a missing one gets a
fix silently overwritten:

| Area | Files |
|---|---|
| Signup hardening | `UserService.signup` |
| Default super admin password | `ApplicationInitializer.getSuperAdminSignupRequest` |
| Premium unlock | `LicenseService`, `ApplicationInitializer`, `application.yml` |
| Category-bound custom fields | `CustomField`, `CustomFieldService`, `CustomFieldValueService`, `CustomFieldRepository`, `AssetService.setAssetCustomFields` |
| Work order → purchase order | `PurchaseOrder`, `PurchaseOrderService`, `PurchaseOrderController`, `PurchaseOrderRepository` |
| Light sidebar | `layouts/ExtendedSidebarLayout/Sidebar/**`, `theme/schemes/*.ts` |
| Sidebar order and labels | `Sidebar/SidebarMenu/items.ts` (order, no two-child dropdowns, `activePath`), `SidebarMenu/index.tsx`, `i18n/translations/de.ts` |
| Branding | `components/LogoSign` (caption under the mark), `public/favicon*`, `public/static/images/logo/**`, `frontend/scripts/build-logo-assets.ps1`, `docs/logo_v3.png`, `docs/fav_fm_v2..png` |
| Reporting: column registry | `CsvFileGenerator` (work-order and asset writers now delegate), `utils/csv/**`, `CsvFileGeneratorTest` (constructs the generator instead of `@InjectMocks`) |
| Reporting: filtered export | `ExportController`, `AsyncExportService`, `WorkOrderService.findForExport`, `AssetService.findForExport` |
| Reporting: shared asset scoping | `AssetService.getSearchCriteria` (extracted out of `AssetController.search`) |
| Reporting: export headers | `messages.properties`, `messages_de_DE.properties` (appended keys) |
| Saved views | `SavedView*` (new files), `frontend` work-order and asset list pages, `hooks/useTableState.ts`, `hooks/useExport.ts` |
| File search and filters | `content/own/Files/index.tsx`, `content/own/Files/Filters/**` |
| File→asset/work-order links | `File` (`workOrders` join table), `FileShowDTO`, `FileMapper` |
| Build tooling (frontend) | **Upstream is still Create React App; this fork is not.** `frontend/vite.config.ts` (new), `frontend/index.html` (moved out of `public/`), `frontend/package.json` scripts, `src/config.ts` + `src/serviceWorker.ts` (`import.meta.env` instead of `process.env`), `src/vite-env.d.ts`. Deleted here: `config-overrides.js`, `src/react-app-env.d.ts`. An upstream change touching the build, `public/index.html` or `REACT_APP_*` needs translating, not merging |
| Container plumbing | frontend `Dockerfile` + `docker-entrypoint.sh`, `docker/nginx/**`, `docker-compose.yml` |

`ApiKeyAuthFilter` is **not** in that list. It reads the license and plan gates that
`SELF_HOSTED_UNLOCK_PREMIUM` opens, so it is worth reading to understand the unlock — but
it is untouched upstream code and needs no merge attention.
