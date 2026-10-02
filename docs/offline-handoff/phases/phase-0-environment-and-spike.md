# Phase 0: Environment, seed and BLE spike (go/no-go)

**Goal.** Prove that a self-hosted Atlas built from local source, plus the locally built SDK, runs inside the Atlas
mobile app on one iPhone and one Android phone, and that those phones exchange encrypted messages over BLE
reliably. If they can't, stop and fix the SDK before building anything else.

**Estimate.** 3 days. **Depends on.** Nothing.

## Prerequisites (machine and accounts)

- [x] Docker Desktop, JDK 17, Maven, Node 20+
- [x] Rust (rustup) with iOS and Android targets, the Android NDK (`ANDROID_NDK_HOME`), and
      `cargo install uniffi --version 0.30.0 --features cli --locked`
- [x] Xcode with an Apple developer team that can sign dev builds for the demo iPhone
- [x] Android Studio and SDK, with the demo Android phone in developer mode
- [x] Firebase config env vars available for `app.config.ts` (or a documented stub; see Notes)
- [x] Laptop and both phones on the same Wi-Fi for the online parts. Note the laptop's LAN IP.

## Tasks

### 0.1 Branches
- [x] Create branch `feat/offline-handoff` in `cmms`. SDK fixes go on branch `atlas-demo` in the SDK repo.

### 0.2 Backend from local source
- [x] Add `docker-compose.offline-demo.yml`, an override that builds `api` from `./api`:
      `services: api: build: ./api, image: atlas-api-local`.
- [x] Create `.env` from `.env.example`. Set `PUBLIC_SERVER_URL=http://<LAN-IP>:3000` and leave
      `INVITATION_VIA_EMAIL=false`.
- [x] `docker compose -f docker-compose.yml -f docker-compose.offline-demo.yml up -d --build`. Confirm
      `http://<LAN-IP>:3000` serves the web UI and that you can log in.
- [x] Choose the fast inner loop: either keep postgres and minio in compose with the API running through `mvn
      spring-boot:run` (the dev profile pointed at compose Postgres), or rebuild the image. Write the exact
      commands into the root `CLAUDE.md` "Commands" section.

### 0.3 Seed script
- [x] `scripts/offline-demo/seed.mjs`: plain Node 20 `fetch`, no dependencies, idempotent where practical.
      It calls the REST API to create:
  - Company *Northwind Facilities*, admin `admin@northwind.test`.
  - Technicians `alice@northwind.test` (Android) and `bob@northwind.test` (iPhone) with the default
    Technician role (no `editOther` on work orders), plus an optional `carol@northwind.test` for the
    permissions scene. All use one demo password.
  - Location *Plant 1* and asset *Chiller CH-2*.
  - Work order *CH-2 quarterly inspection*:
    - `primaryUser = Alice`, `assignedTo = [Alice, Bob]`, `requiredSignature = false`, status OPEN.
    - 4 tasks: 3 checklist subtasks and 1 numeric reading ("Discharge pressure, psi").
- [x] First verify how invited users join when `INVITATION_VIA_EMAIL=false` (look at `UserService.invite` /
      signup), and check the free-tier user limit (`UserService.checkUsageBasedLimit`). If inviting through the
      API isn't workable, seed users with `scripts/offline-demo/seed-users.sql` and say why in a comment.
- [x] `scripts/offline-demo/reset.sh`: return the work order to its seeded state (status OPEN, primary Alice,
      tasks cleared, offline comments deleted) and truncate `offline_op`. The tables come in later phases, so
      extend this script as they land.
- [x] Record the credentials and work order id in `DEMO_RUNBOOK.md`.

### 0.4 Build the SDK locally
- [x] In `offline-protocol-sdk/bindings/react-native`: `npm install && npm run build && npm run
      build:uniffi:all`.
- [x] Confirm that the iOS XCFramework (the podspec expects it under `ios/libs/`) and the Android `.so` files for
      every ABI were produced. Record the exact output paths in CODEBASE_MAP §3.

### 0.5 Link the SDK into the mobile app
- [x] `mobile/package.json`: `"@offline-protocol/mesh-sdk": "file:../../offline-protocol-sdk/bindings/react-native"`,
      then `npm install`.
- [x] `mobile/metro.config.js`, on top of `getSentryExpoConfig`:
  - Add the SDK package dir to `watchFolders`.
  - Map `@offline-protocol/mesh-sdk` in `extraNodeModules`.
  - Pin `react` and `react-native` to `mobile/node_modules`.
  - Add the SDK's own `node_modules/react` and `node_modules/react-native` to `blockList`.
  - Mirror `examples/demo-app/metro.config.js`.
- [x] `app.config.ts`: iOS `infoPlist.NSBluetoothAlwaysUsageDescription` ("Atlas uses Bluetooth to hand off work
      orders to nearby technicians when offline."). Android: make sure the SDK manifest's BLE permissions merge,
      and add `ACCESS_FINE_LOCATION` for Android ≤ 11 if the SDK docs require it.
- [x] `npx expo prebuild` (without `--clean`, unless it's needed), then `cd ios && pod install`. Review the native
      diff and commit it.
- [x] Build to both phones: `npx expo run:android --device` and `npx expo run:ios --device`, with
      `API_URL=http://<LAN-IP>:3000/api/`.

### 0.6 Spike: Mesh Diagnostics screen
- [x] `screens/MeshDiagnosticsScreen.tsx`, reachable from Settings/More and kept as a dev tool. It shows:
  - Buttons: request permissions, `start`, `stop`.
  - `localAddress()` and transport state.
  - A neighbours list (`neighbor_discovered`/`neighbor_lost`).
  - "Send test" to a selected neighbour.
  - A log of `message_received` / `message_delivered` / `message_retrying` / `message_failed`.
- [x] SDK config per D20, BLE only (D1), with a temporary hard-coded profile `atlas-spike`.

### 0.7 Measure (go/no-go)

Record results in the table below.
- [x] Discovery time with phones 1 m apart, in both directions.
- [x] 20 messages Android→iOS and 20 iOS→Android. Count delivered, and note median and p95 latency.
- [x] Retry: receiver's Bluetooth off, send, wait 30 s, Bluetooth on. Delivered?
- [x] Restart: kill the sender with a message pending, relaunch. Delivered without a resend?
- [x] At a 5 m distance, and with screens locked for 30 s (to learn the background behaviour).
- [x] `encrypted: true` on received messages.

| Measurement | Result | Notes |
|---|---|---|
| Discovery A→B / B→A | Android found iPhone in 6.2 s; iPhone found Android in 2.3 s | Clean run, after uninstalling the other SDK demo apps from both phones. With them installed: 4 s / 6.7 s. |
| Delivery 20/20 each way | 20/20 Android→iOS and 20/20 iOS→Android, at 1 m and again at 5 m | All received with `encrypted: true`, 0 retries, 0 failures |
| Latency median / p95 | Android→iOS 478 / 515 ms (1 m), 477 / 546 ms (5 m). iOS→Android 215 / 278 ms (both runs) | Send → SDK delivery ACK, measured on the sender, one message per second. An earlier run (other SDK apps present) had one ~5 s stall. |
| Retry after BT off/on | Delivered after BT on: 3 retries, 43 s end to end | **Needed SDK fixes** on `atlas-demo` (a54b383d iOS, 36415b01 Android); see CODEBASE_MAP §4. Before them, the iPhone never sent again after a power-cycle. |
| Delivery after sender restart | Delivered, no resend, both directions | Messages pending when the app was force-quit showed "delivered (from previous run)" within about 1 s of relaunch + Start |
| 5 m, screens locked | 20/20 each way after 30 s locked, no neighbour loss | iOS's JS console detaches from Metro while locked; delivery confirmed from Android's logs and the iPhone's on-screen stats |

**Result: GO.** Every criterion is met (≥ 19/20 each way, discovery < 15 s, retry and restart deliver, encrypted).
Three SDK bugs found and fixed in the SDK repo on `atlas-demo`: iOS stale GATT service after power-off, iOS
`bleStatusChanged` never re-sent after power-on, and Android false `neighbor_lost` on a rotated iOS address.

Notes for later phases:
- Demo phones must have no other Offline Protocol SDK apps installed (runbook). They publish the same GATT
  service, and peers bind to the wrong identity.
- Only the iPhone's Bluetooth was power-cycled. Phase 6 should repeat the retry case with Android's Bluetooth
  toggled too.

## Exit criteria
- [x] The Atlas web UI and API, built from local source, run through compose. Commands are in the root `CLAUDE.md`.
- [x] Seed and reset scripts work from a fresh database.
- [x] Both phones run a dev build of Atlas with the locally linked SDK.
- [x] **Go:** at least 19/20 delivered each way, discovery under 15 s, retry and restart cases deliver, and
      messages are encrypted. Or **no-go** is recorded with the SDK issue(s) filed and the fix plan agreed before
      Phase 1.
- [x] Gotchas added to CODEBASE_MAP §4. README phase status updated.

## Notes
- Firebase: if the build fails without Firebase files, prefer real dev config. Otherwise gate the Firebase plugin
  in `app.config.ts` behind an env var. Don't delete it.
- If the native SDK build blocks progress for more than half a day, fall back to the prebuilt
  `offline-protocol-mesh-sdk-0.27.0.tgz` (D6 fallback) and keep going.
