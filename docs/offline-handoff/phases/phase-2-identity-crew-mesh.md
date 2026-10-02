# Phase 2: Identity, crew and mesh runtime

**Goal.** Each device has a stable SDK identity tied to its Atlas user. Each phone knows which nearby devices
belong to a work order's crew, and the SDK runs as part of the app instead of only on the diagnostics screen.

**Estimate.** 3-4 days. **Depends on.** Phase 1.

## Backend

### 2.1 Schema
- [ ] Liquibase changelog `db/changelog/<date>_<epoch>_offline_handoff.xml`, included at the end of
      `db/master.xml`. Create `offline_device` exactly as in CONTRACTS §6, with its sequence. `offline_op` is
      added to the same changelog in Phase 5, or as a second changeset now; changesets are cheap.

### 2.2 Device registration and crew
- [ ] `model/OfflineDevice` (extends `CompanyAudit`), `repository/OfflineDeviceRepository`,
      `service/OfflineDeviceService`, `controller/OfflineController`, plus DTOs in `dto/offline/`.
- [ ] `POST /offline/devices` per CONTRACTS §5: upsert by address, bound to `userService.whoami(req)`, 409 on a
      foreign owner, and 32-byte key validation.
- [ ] `GET /offline/work-orders/{id}/crew` per CONTRACTS §5:
  - Check view access with the existing `checkAccessToWorkOrderId` pattern.
  - Take the company's registered devices, keep those where `workOrder.canBeEditedBy(device.user)`, and map them
    to the DTO.
- [ ] Integration test `OfflineDeviceIntegrationTest` (extends `AbstractIntegrationTest`). Cover: register, re-register
      (idempotent), foreign address gets 409, the crew contains assignees and excludes non-assigned technicians.

## Mobile

### 2.3 `contexts/MeshContext.tsx`
- [ ] Provider mounted inside the authenticated tree in `App.tsx`. Follow the initialization order in the SDK's
      `examples/demo-app/src/context/ProtocolContext.tsx`.
- [ ] Lifecycle:
  - After auth is ready: request runtime BLE permissions.
  - Create `new OfflineProtocol({appId: 'atlas-cmms', profile: 'atlas-<companyId>-<userId>', BLE only})` and call
    `start()`.
  - `stop()` on logout.
- [ ] On `identity_ready` (or `localAddress()`): store `myAddress` and `myPublicKey` in the `offline` slice.
- [ ] Registration: if online and `registeredAddress !== myAddress`, call `POST /offline/devices`. Retry whenever
      `backendReachable` becomes true.
- [ ] Services:
  - `registerService('atlas.handoff', '1', {})` after start.
  - Expose `startNearbyScan(woId)` / `stopNearbyScan()`, which call `discoverServices('atlas.handoff')` every 5 s.
- [ ] Event wiring into the `offline` slice:
  - `neighbor_discovered`/`neighbor_lost` and `service_discovered` → `nearby[address] = {lastSeen, transport, rssi}`.
  - Delivery events are wired now and used in Phase 3.
- [ ] Expose a `useMesh()` hook returning `{ready, myAddress, protocol, startNearbyScan, stopNearbyScan}`.

### 2.4 Crew in the offline pack
- [ ] When the pack is captured (Phase 1.3), also fetch `GET /offline/work-orders/{id}/crew` into
      `packs[woId].crew`.
- [ ] Selector `nearbyCrew(woId)`: crew members (excluding me) whose address was seen within the last 30 s.
- [ ] Selector `isCrewAddress(woId, address)`. This is the allowlist used by Phase 3's inbound pipeline.

### 2.5 Diagnostics
- [ ] Switch MeshDiagnosticsScreen to the shared `MeshContext` instance; no second protocol instance. Show
      `myAddress`, registration status, the crew for the last opened work order, and nearby peers marked
      crew / not crew.

## Exit criteria
- [ ] Backend integration tests pass.
- [ ] Alice and Bob each open the seeded work order online. Each phone's diagnostics screen lists the other as a
      crew member by name, and as nearby once in BLE range.
- [ ] A third device logged in as Carol (not assigned) shows as nearby but **not** crew. Optional, if a third phone
      is available.
- [ ] Relaunching keeps the same `myAddress`, and nothing is re-registered.
- [ ] Logout stops the SDK. Logging in as another user on the same phone gives a different address.
- [ ] README status updated, and any gotchas added to CODEBASE_MAP §4.
