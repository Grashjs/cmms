# CLAUDE.md

This repo is a fork of **Atlas CMMS**:

| Dir | Contents |
|---|---|
| `api/` | Spring Boot backend |
| `frontend/` | React web app |
| `mobile/` | Expo / React Native app |
| `home/` | Marketing site; untouched |

We are building **Offline Work-Order Handoff**, a prospect demo. It integrates the Offline Protocol SDK (the sibling
repo `/Users/mizanxali/offline-protocol-sdk`) into the mobile app, so two technicians (iPhone ↔ Android over BLE)
can hand off a work order with no backend. The offline history then reconciles into Atlas without duplicates.

## Source of truth: `docs/offline-handoff/`

Read these before writing code, in this order:

1. `README.md`: goal, demo story, scope, **phase status**
2. `DECISIONS.md`: settled decisions (D1-D24). Don't reopen them. To change one, add a superseding entry.
3. `ARCHITECTURE.md`: components, trust model, merge algorithm (§5), sync pipeline (§6)
4. `CONTRACTS.md`: op, envelope, mesh, REST and DB formats shared by mobile and API
5. `CODEBASE_MAP.md`: where things live in Atlas and the SDK, with line references, plus gotchas
6. `phases/phase-N-*.md`: the phase being executed

## Working rules

- **Phase discipline.**
  - Execute phases in order and keep to the current phase's task list; note anything else as a follow-up instead
    of building it.
  - A phase is done when every exit criterion in its file is checked.
  - Tick boxes in the phase file and update the README status table in the same commit as the work.
- **Contracts first.** Any change to a wire, REST or DB format is made in `CONTRACTS.md` in the same change as the
  code, on both sides.
- **Write down what you learn.** Record surprising SDK or Atlas behaviour in `CODEBASE_MAP.md` §4 as
  `- [Phase N] fact. Consequence.`
- **Match the surrounding code.** Use Atlas's existing patterns: layered packages, RTK thunks, `utils/api.ts`,
  `SheetManager` sheets, i18n strings. Keep the pure op logic (`mobile/utils/offlineOps.ts`) free of
  SDK, Redux and React imports.
- **Tests.** Each non-trivial piece leaves one runnable check behind:
  - mobile: jest-expo, in `utils/offlineOps.test.ts`;
  - API: Testcontainers integration tests extending `integration/AbstractIntegrationTest`.

## Repo conventions

### API (`api/`)
- Java 17, Spring Boot 3.5, system `mvn` (there's no wrapper).
- Code goes under `api/src/main/java/com/grash/` in `controller`, `service`, `model`, `repository`, `dto`, `mapper`.
  Offline classes use the `Offline` prefix in those packages; DTOs go in `dto/offline/`.
- **Schema changes only through Liquibase.**
  - New file in `api/src/main/resources/db/changelog/`, named `YYYY_MM_DD_<epoch>_<name>.xml`, appended to
    `db/master.xml`.
  - Follow `db/Agents.md`: FK `onDelete` cascade (or set null), and a sequence with increment 50 for
    `CompanyAudit` tables.
  - `ddl-auto: validate`, so entities and changelogs must match exactly.
- Multi-tenancy comes from `CompanyAudit` plus the SecurityContext. Code acting as another user must set and then
  restore the SecurityContext (see `DemoController` L77-84).

### Mobile (`mobile/`)
- Expo 53, RN 0.79, **old architecture**, dev client. Expo Go can't run the SDK.
- `android/` and `ios/` are committed but generated.
  - Native config changes go through `app.config.ts` or `mobile/plugins/`, followed by `npx expo prebuild`.
    Commit the regenerated native folders.
  - Never hand-edit Info.plist or AndroidManifest.
- The whole Redux store is persisted by redux-persist. New state goes in `slices/offline.ts`.

### SDK (sibling repo)
- Linked as `file:../../offline-protocol-sdk/bindings/react-native` (D6). Metro is configured to watch it.
- After changing the SDK's TypeScript, run `npm run build` in `bindings/react-native`.
- After changing its Rust, run `npm run build:uniffi:ios` / `build:uniffi:android`, then rebuild the app.
- SDK fixes are committed in the SDK repo on branch `fix/ble-reliability`, and noted in `CODEBASE_MAP.md` §4.
- Nothing in the SDK repo mentions Atlas: branch names, commit messages, code and comments stay product-neutral.

## Commands

Verified in Phase 0. The laptop's LAN IP was `192.168.0.133`; replace it if yours differs (`ipconfig getifaddr en0`).
Docker is OrbStack: if `docker` can't connect, run `orb start`.

```bash
# Full stack, with the API built from local source (first build ~10 min; incremental API rebuild ~45 s)
docker compose -f docker-compose.yml -f docker-compose.offline-demo.yml up -d --build
# Backend inner loop after editing api/: rebuild and restart only the API
docker compose -f docker-compose.yml -f docker-compose.offline-demo.yml up -d --build api
# Web UI: http://192.168.0.133:3000   API (via nginx): http://192.168.0.133:3000/api/

# API integration tests (Homebrew mvn defaults to JDK 27, which breaks Lombok, so pin 17)
cd api && JAVA_HOME=$(/usr/libexec/java_home -v 17) mvn test -Dtest='Offline*IntegrationTest'

# Mobile one-time setup: placeholder Firebase config (gitignored files)
scripts/offline-demo/firebase-stub.sh

# Mobile env for every prebuild/build (iOS is signed by the Offline Protocol team)
export API_URL=http://192.168.0.133:3000/api/ APPLE_TEAM_ID=4NZGXQRKBG IOS_BUNDLE_ID=com.offlineprotocol.atlasdemo
export ANDROID_NDK_HOME=$HOME/Library/Android/sdk/ndk/27.1.12297006

# Prebuild (only after app.config.ts / plugins changes), then revert upstream drift (CODEBASE_MAP §4)
cd mobile && npx expo prebuild --no-install && (cd ios && pod install)
cd mobile && git checkout android/app/src/main/res/values/styles.xml ios/AtlasCMMS/AtlasCMMS.entitlements \
  ios/AtlasCMMS/Supporting/Expo.plist && sed -i '' 's#Allow Atlas to access camera.#Allow Atlas to use the camera to scan.#' \
  ios/AtlasCMMS/Info.plist

# Metro (both dev clients load from it)
cd mobile && npx expo start --dev-client --lan

# Android dev build (--device takes the `adb devices -l` model name, not the serial)
cd mobile && npx expo run:android --device SM_M176B --no-bundler

# iOS dev build (expo run:ios is broken on Xcode 27, so use xcodebuild + devicectl)
cd mobile/ios && xcodebuild -workspace AtlasCMMS.xcworkspace -scheme AtlasCMMS -configuration Debug \
  -destination id=00008140-0004453A212B001C -derivedDataPath /tmp/atlas-dd -allowProvisioningUpdates build
xcrun devicectl device install app --device 00008140-0004453A212B001C \
  /tmp/atlas-dd/Build/Products/Debug-iphoneos/AtlasCMMS.app
xcrun devicectl device process launch --device 00008140-0004453A212B001C --terminate-existing \
  --payload-url "exp+atlas-cmms://expo-development-client/?url=http%3A%2F%2F192.168.0.133%3A8081" \
  com.offlineprotocol.atlasdemo

# Mobile tests
cd mobile && npm test -- --watchAll=false

# Device logs: Android from logcat (the JS console detaches from Metro after a lock or force-quit)
adb logcat | grep ReactNativeJS

# SDK (from /Users/mizanxali/offline-protocol-sdk/bindings/react-native)
npm run build                                  # TypeScript
ANDROID_NDK_HOME=... npm run build:uniffi:all  # Rust only; Swift/Kotlin changes need just an app rebuild

# Demo data (fresh DB: seed once; reset before each run)
API_URL=http://192.168.0.133:3000/api node scripts/offline-demo/seed.mjs
scripts/offline-demo/reset.sh
```

## Git
- Work on branch `feat/offline-handoff`. Keep commits per phase task where practical, e.g.
  `feat(offline): phase 2 - device registration endpoint`.
