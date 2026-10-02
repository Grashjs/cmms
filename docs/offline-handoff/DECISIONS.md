# Decisions

These decisions are settled; don't reopen them during implementation. To change one, append a new entry that
supersedes it ("D25 supersedes D7: …") and update every affected doc in the same change.

Each entry has a source:
- **User:** decided by the product owner.
- **Design:** decided during design and accepted along with the plan.

## Platform and SDK

**D1. iOS and Android over BLE only.** *(User)*
The demo pairs an iPhone with an Android phone. The SDK's Android Wi-Fi Direct and iOS local-network transports
can't talk to each other, so only `ble` is enabled. Wi-Fi Direct, internet, Nostr and Reticulum are all off.
- Consequence: throughput is BLE-class, so messages stay small (text ops only).
- Consequence: both apps stay in the foreground during the demo.

**D2. The SDK's DataStore (Loro CRDT) is not used.** *(User)*
Shared state is an app-level, signed, append-only op log. It maps one-to-one onto backend replay and makes the
conflict policy easy to explain.

**D6. The SDK is consumed through a local `file:` link to `/Users/mizanxali/offline-protocol-sdk/bindings/react-native`.** *(User)*
- SDK bugs found during the build are fixed in the SDK repo straight away.
- Native libraries must be built locally, which needs Rust, the Android NDK and uniffi-bindgen 0.30.0.
- Builds are local only (`expo run:*`). EAS cloud builds are out of scope because the link points outside the repo.
- Fallback if the native build blocks Phase 0: install the prebuilt `offline-protocol-mesh-sdk-0.27.0.tgz`.

**D7. Ops travel as 1:1 MLS-encrypted messages, one per crew member.** *(Design)*
SDK groups are not used. Two or three technicians don't need group key management.

**D20. SDK configuration.** *(Design)*
- `appId: 'atlas-cmms'`.
- `profile: 'atlas-<companyId>-<userId>'`, so each Atlas user on a device gets a stable SDK identity of their own.
- Encryption uses the SDK defaults: enabled, `requireEncryption: true`, `autoKeyExchange: true`, `storePending: true`.

## Identity and permissions

**D8. The device's SDK address is bound to the Atlas user through authenticated registration.** *(Design)*
- While online, the app posts its `localAddress()` and identity public key to `POST /offline/devices`, over HTTPS with its JWT.
- While online, the app fetches each work order's crew (`GET /offline/work-orders/{id}/crew`).
- Offline, the crew snapshot is the allowlist. Messages from addresses outside it are dropped.
- No certificates or tokens are minted: the roster arrives from the trusted server over TLS.

**D9. Every op is signed with the SDK identity key (Ed25519, `signData`).** *(Design)*
- The signature covers the exact UTF-8 bytes of the serialized body string, so neither side needs JSON
  canonicalization.
- Peers verify with the crew snapshot's public key. The backend verifies with the registered key.
- A crew member can upload another member's ops, and attribution to the author still can't be forged.

**D10. Offline authorization means the author is in the work order's crew snapshot.** *(Design)*
The backend re-checks `WorkOrder.canBeEditedBy(author)` at sync time, and that check is authoritative. Crew
changes made while a phone is offline are only seen after it reconnects (known limitation).

**D24. Any crew member except the requester may accept an open handoff request.** *(Design)*
Only one accept can win. The conflict rule (D13) enforces this through `basePrimaryUserId`.

## Product behaviour

**D3. Accepting a handoff changes the assignment.** *(User)*
- The acceptor becomes the work order's `primaryUser`.
- The requester stays in (or is added to) `assignedTo`.
- A handoff comment records who, when and from whom.

**D4. On a conflict, the server wins.** *(User)*
- If an offline field change reaches the backend and the server value differs from the value the device based
  its change on, the change is not applied.
- A conflict comment is written instead, and both devices show the op as Conflict after sync.

**D5. Op scope.** *(User)*
- In scope: `NOTE`, `STATUS`, `TASK_UPDATE` (task `value` and `notes`), `HANDOFF_REQUEST`, `HANDOFF_ACCEPT`.
- Out of scope: labor, parts, photos, creating work orders, and editing or deleting comments.

**D13. Merge policy: compare-and-set in a total order, identical on device and server.** *(Design)*
- Ops are ordered by `(lamport, authorAddress)`.
- Field ops (STATUS, TASK_UPDATE, HANDOFF_ACCEPT) carry the value they were based on. An op applies only if the
  current value still equals its base value. Otherwise it is a Conflict, and the earlier op keeps its value.
- NOTE and HANDOFF_REQUEST never conflict.
- Before sync, devices converge with each other. After sync, the server's outcome is authoritative. Between
  partitioned devices, the order in which ops arrive at the server breaks the tie.
- ARCHITECTURE.md §5 has worked examples.

**D14. Any crew member may upload any op.** *(Design)*
- The backend deduplicates on `op_id` with a unique constraint and returns the stored result for repeats.
- Each op is applied under the author's security context, so the comment author, Envers revision and `createdBy`
  are the author. The uploader is recorded in `offline_op.uploaded_by`.

**D15. In the web UI, the reconciled record is shown through comments and work-order fields.** *(Design)*
The web History tab is licence-gated (`WORK_ORDER_HISTORY` entitlement) and is empty when self-hosted without a
licence. No web UI changes are made.

**D21. Server timestamps are not altered.** *(Design)*
The device's `occurredAt` appears in the comment text. `DateAudit` and `completedOn` stay server-set.

**D17. The offline pack is captured when Work Order Details is opened online.** *(Design)*
The pack holds the work order, its tasks and its crew. There is no background prefetch; the demo opens the work
order in step 1.

## App and infrastructure

**D11. Service discovery is used only for presence ("Nearby crew").** *(Design)*
- The app registers service `atlas.handoff` version `1` with empty capabilities.
- Discovery messages are signed plaintext, so they carry no work-order or company data.
- Content only ever travels in encrypted 1:1 messages.

**D12. Sending model: when an op is created, send it straight to every other crew member.** *(Design)*
The SDK's outbox handles queuing, retries, parking for unreachable peers and restarts (7-day lifetime). There is
no gossip or anti-entropy protocol. Revisit this only if relaying through a third device becomes a demo goal.

**D16. Opening the app offline uses the cached user and company.** *(Design)*
- On a network error (as opposed to a 401), `AuthContext` authenticates from the cached user and company JSON.
- Permissions keep working because they read `state.user`.

**D18. Backend code follows Atlas's layered layout with an `Offline` prefix.** *(Design)*
- Classes go in `controller/`, `service/`, `model/`, `repository/` and `dto/`.
- All schema changes go in one Liquibase changelog.

**D19. Device persistence is a new `offline` Redux slice, saved by the app's existing redux-persist setup.** *(Design)*
- Ceiling: the SDK acknowledges delivery before our handler runs, and redux-persist writes asynchronously. A crash
  in that small window can lose one received op. The sender still holds it and it reaches the backend from there.
- Upgrade path: write-through storage keyed by op id.

**D22. "Backend reachable" means the last API call succeeded, not just that NetInfo reports a connection.** *(Design)*
- The demo disconnects with airplane mode and then Bluetooth back on.
- Sync triggers when reachability returns, plus a manual **Sync now** button.

**D23. Development and demo infrastructure.** *(Design)*
- Postgres, MinIO, nginx and the frontend run from docker compose.
- The API is built from local source using a compose override.
- Phones reach the API at `http://<laptop-LAN-IP>:3000/api/` through nginx. Cleartext HTTP is already allowed on
  both platforms.
