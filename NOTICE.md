# NOTICE — Fierabrás CMMS (community fork of Atlas CMMS)

This NOTICE documents modifications made to the Atlas CMMS source code and is
provided to comply with the terms of the GNU Affero General Public License
v3.0 (AGPLv3) under which this fork is distributed.

## Origin and original copyright

This software is a **fork of Atlas CMMS** maintained at
<https://github.com/Grashjs/cmms> by INTELLOOP LLC. All original copyright,
trademark, and other notices are preserved. The original project is
dual-licensed (AGPLv3 and a separate Commercial License Agreement);
this fork adopts **only** the GNU AGPLv3 provided in [`LICENSE`](./LICENSE).
The original Commercial License Agreement in [`COMMERCIAL_LICENSE.MD`](./COMMERCIAL_LICENSE.MD)
is kept verbatim for attribution and traceability and does **not** apply to
this fork.

## Status

- **Unofficial** community fork. Not affiliated with, endorsed by, or
  sponsored by INTELLOOP LLC or Grashjs.
- **Date of modification:** 2026-08.
- **License of the fork:** AGPL-3.0-only.

## Summary of changes

1. Added a local, deterministic AGPL licensing policy in
   `api/src/main/java/com/grash/service/LicenseService.java` that grants every
   `LicenseEntitlement` without contacting Keygen or requiring a license key/file.
2. In `api/src/main/java/com/grash/ApplicationInitializer.java`, every persisted
   `SubscriptionPlan` is given all `PlanFeatures` (idempotent upsert), and existing
   subscriptions are normalized to remove commercial seat limits and degradation.
3. In `api/src/main/java/com/grash/service/SubscriptionService.java`,
   `resetToFreePlan` now delegates to an AGPL policy that does not degrade
   subscriptions, preserves the historical plan and `paddleSubscriptionId`,
   and removes pending Quartz degradation jobs.
4. Added a public AGPLv3 corresponding-source endpoint
   `api/src/main/java/com/grash/controller/SourceController.java`
   (`GET /source`, exposed as `/api/source`), permitted in
   `WebSecurityConfig.java` without relaxing any other security rule.
5. Added `SOURCE_CODE_URL` / `SOURCE_REVISION` configuration
   (`application.yml`, `docker-compose.yml`, `.env.example`, `frontend/.env.example`).
6. Added a visible "Código fuente — AGPLv3" link in the frontend
   (`frontend/src/config.ts`, the login screen
   `content/pages/Auth/Login/Cover/index.tsx`, and the authenticated sidebar
   footer `ExtendedSidebarLayout/Sidebar/SidebarFooter/index.tsx`).
7. `docker-compose.yml` now builds local `fierabras/cmms-api:local` and
   `fierabras/cmms-frontend:local` images from this fork's source.
8. Added a fork section in `README.MD` and this `NOTICE.md`.
9. Added contract tests
   (`api/src/test/java/com/grash/service/LicenseServiceTest.java`,
   `api/src/test/java/com/grash/ApplicationInitializerTest.java`, and the
   `ResetToFreePlan` / `ApplyAgplSubscriptionPolicy` / `NormalizeExistingSubscriptions`
   sections of `SubscriptionServiceTest.java`).

## Corresponding Source (AGPLv3 §13)

The exact Corresponding Source for the deployed revision is offered to users
interacting with this software remotely:

- `SOURCE_CODE_URL`: public repository URL of this fork.
- `SOURCE_REVISION`: exact deployed commit or tag.
- A visible link labeled "Código fuente — AGPLv3" appears both on the
  unauthenticated login screen and in the authenticated app layout.
- A public endpoint `GET /api/source` returns
  `{ "license": "AGPL-3.0-only", "sourceCodeUrl", "revision", "correspondingSourceUrl" }`.

## Trademarks

"Atlas CMMS", the Atlas CMMS logo, and related identifiers are trademarks of
their respective owners and are **not** licensed under the AGPLv3. This fork
retains them only as historical attribution and operational references until
a distinguishable identity replaces them. The fork is not presented as an
official product of INTELLOOP LLC or Grashjs.

## No warranty

This program is free software: you can redistribute it and/or modify it under
the terms of the GNU Affero General Public License as published by the Free
Software Foundation, either version 3 of the License, or (at your option) any
later version.

This program is distributed in the hope that it will be useful, but WITHOUT
ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
FOR A PARTICULAR PURPOSE. See [`LICENSE`](./LICENSE) for the full text and
the complete disclaimer of warranty.