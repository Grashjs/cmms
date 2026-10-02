# Architecture

## 1. System overview

```
┌──────────────── Phone A (Android, Alice) ───────────────┐        ┌──────── Phone B (iPhone, Bob) ────────┐
│ Screens: WODetails ─▶ OfflineHandoffScreen, Diagnostics │        │            (same app)                 │
│ MeshContext (SDK lifecycle, events → offline slice)     │◀─BLE──▶│                                       │
│ offline slice: ops · lamport · packs · outbound · nearby│  MLS   │                                       │
│ utils/offlineOps.ts (pure: create/verify/merge view)    │  1:1   │                                       │
│ sync engine (upload unsynced ops, refresh packs)        │        │                                       │
│ @offline-protocol/mesh-sdk (file: link, BLE only)       │        │                                       │
└───────────────┬─────────────────────────────────────────┘        └──────────────────┬────────────────────┘
                │ HTTPS (only when backend reachable)                                 │
                ▼                                                                     ▼
┌──────────────────────── Atlas API (Spring Boot, built from local source) ─────────────────────────────────┐
│ OfflineController   POST /offline/devices · GET /offline/work-orders/{id}/crew · POST /offline/ops        │
│ OfflineDeviceService  ─ offline_device table (user ↔ address ↔ Ed25519 public key)                       │
│ OfflineSyncService    ─ offline_op table (unique op_id) → verify → authorize → CAS → apply via:          │
│                         CommentService.create · WorkOrderService.changeStatus/patch · TaskService.update │
│ Existing: Envers audit, notifications, webhooks, workflows fire as for any normal edit                   │
└────────────────────────────────────────────────────────────────────────────────────────────────────────────┘
                ▲
                │ Atlas web UI (unchanged): comments, status, primary user, tasks show the reconciled result
```

## 2. Components

### Mobile (`mobile/`)

| Component | File(s) | Responsibility |
|---|---|---|
| Cached-login fallback | `contexts/AuthContext.tsx` | Caches the user, company and company settings. When `getInfos` fails with a network error, authenticates from that cache (D16). |
| Reachability | `utils/api.ts`, `slices/offline.ts` | Sets `backendReachable` from the outcome of each request. NetInfo changes trigger a probe (D22). |
| `MeshContext` | `contexts/MeshContext.tsx` | Starts and stops the SDK after login; requests permissions; registers the device; registers and polls the `atlas.handoff` service. Feeds SDK events into the `offline` slice and runs the inbound pipeline. |
| `offline` slice | `slices/offline.ts` | Holds Lamport counter, ops, offline packs, outbound delivery state, nearby peers, sync state. Persisted through redux-persist (D19). |
| Op logic | `utils/offlineOps.ts` (+ `offlineOps.test.ts`) | Pure functions with no SDK or Redux imports: building op bodies, ordering, the merged view (merge algorithm in §5). |
| Sync engine | `slices/offline.ts` thunks | Uploads unsynced ops, records the results, refreshes packs. |
| UI | `screens/workOrders/OfflineHandoffScreen.tsx`, `screens/MeshDiagnosticsScreen.tsx` | Handoff flow (Phase 4). Diagnostics screen from the Phase 0 spike, kept as a dev tool. |

### Backend (`api/src/main/java/com/grash/`)

| Component | Responsibility |
|---|---|
| `model/OfflineDevice`, `repository/OfflineDeviceRepository` | Links an Atlas user to an SDK address and Ed25519 public key |
| `model/OfflineOp`, `repository/OfflineOpRepository` | One row per accepted op id. Provides idempotency and stores the result. |
| `service/OfflineDeviceService` | Registers devices and computes each work order's crew |
| `service/OfflineSyncService` | Per-op pipeline (§6) |
| `controller/OfflineController` | The three endpoints in CONTRACTS.md |

### SDK (sibling repo)

The SDK is used as-is: BLE transport, MLS 1:1 encryption, outbox with retry/park/persistence, message-id dedup,
service discovery, `signData`/`verifySignature`. Any SDK fix goes into the SDK repo on its own branch and is
noted in CODEBASE_MAP.md.

## 3. Identity and trust model

| Question | Answer |
|---|---|
| Who is this device? | The SDK address `off1…`, derived from the Ed25519 identity key the SDK generates per `profile`. Stable across restarts. |
| Which Atlas user owns that address? | The user who registered it through `POST /offline/devices` with a valid JWT. The server binds it to `whoami`, and the client cannot choose the user. |
| How does a phone learn a peer's identity offline? | From the crew snapshot fetched online: `{userId, name, address, publicKey}` for every crew member with a registered device. |
| May this message be processed? | Only if the sender address is in the crew snapshot of the op's work order. Otherwise it is dropped silently (and logged in Diagnostics). |
| Did this author really make this op? | Ed25519 signature over the body bytes, verified against the author's public key from the crew snapshot. |
| May this author make this change? | Offline: the author is in the crew snapshot. At sync: `WorkOrder.canBeEditedBy(author)` on live data, which is authoritative. |
| Is it confidential? | MLS 1:1 encryption with fail-closed `requireEncryption`. Discovery messages are plaintext and carry nothing sensitive (D11). |

Known limits, to state honestly when prospects ask:
- The crew is a snapshot, so a user removed from the work order while devices are offline is still trusted until
  they reconnect.
- The SDK's own delivery receipts are unsigned (the SDK documents this). Human acceptance, by contrast, is a
  signed op.
- A stolen, unlocked phone can act as its user. The identity key sits in Keychain or EncryptedSharedPreferences.

## 4. Data lifecycle

```
online:  open WO ─▶ fetch WO + tasks + crew ─▶ packs[woId] = {workOrder, tasks, crew, fetchedAt}
                └▶ ensure device registered (once per address)

offline: user action ─▶ body = {opId, lamport: ++L, base…, payload}
                    ─▶ sig = signData(utf8(JSON.stringify(body)))
                    ─▶ ops[opId] = {envelope, body, origin: 'local'}            (persisted)
                    ─▶ for each crew member ≠ me: sdk.sendMessage({recipient: address, content: meshMessage})
                         outbound[messageId] = {opIds, recipient, state: 'sent'}

peer:    message_received ─▶ parse ─▶ sender ∈ crew? ─▶ verify sig with author's key ─▶ author ∈ crew?
                          ─▶ ops[opId] exists? skip : store                    (persisted)
                          ─▶ L = max(L, body.lamport)

view:    mergedView(pack, ops where syncResult is unset)  — recomputed on every change (§5)

online again: sync ─▶ POST /offline/ops (every op with no terminal result, including peers' ops)
              ─▶ store results ─▶ refetch pack + comments ─▶ ops with a result leave the overlay
```

## 5. Merge algorithm (D13)

A single algorithm runs in two places: `mergedView` on the device and `OfflineSyncService` on the server.

```
sort ops by (lamport asc, authorAddress asc)
state := pack snapshot (status, primaryUserId, tasks[id].{value, notes})
for op in ops:
  NOTE, HANDOFF_REQUEST       → append to timeline. Always applies.
  STATUS        (base, to)    → state.status == base ? set : CONFLICT
  TASK_UPDATE   (task, field, base, to) → state.tasks[task][field] == base ? set : CONFLICT
  HANDOFF_ACCEPT(basePrimaryUserId) → state.primaryUserId == basePrimaryUserId
                                      ? primaryUserId := author; mark request accepted
                                      : CONFLICT
```

The device-side **base value** is the merged view's value at the moment the user acts, not the raw pack value.
So an edit made after receiving a peer's op is based on that op and does not conflict with it. That is ordinary
causality.

### Worked examples

**A. Concurrent status change, both phones offline.**
- Alice's status op: lamport 5, base OPEN, to ON_HOLD. Bob's: lamport 6, base OPEN, to IN_PROGRESS. Neither had
  seen the other's op.
- Once both ops are on both phones:
  - Alice's op applies (OPEN == OPEN), so the status becomes ON_HOLD.
  - Bob's op conflicts (OPEN ≠ ON_HOLD).
- Both phones show ON_HOLD, and Bob's op carries a Conflict badge. Bob's phone briefly showed IN_PROGRESS until
  Alice's op arrived, and that flip is the visible part of the demo.
- At sync, the server sees the same order, gets the same outcome, and writes a conflict comment for Bob's op.

**B. A web admin edits while the phones are offline (server wins, D4).**
- The admin sets the status to COMPLETE on the web.
- Alice's offline op had base OPEN. At sync, COMPLETE ≠ OPEN, so the result is CONFLICT and a comment is written.
- After the refresh, the phones show COMPLETE and Alice's op shows Conflict.

**C. Double accept (three devices).**
- Bob accepts with basePrimaryUserId = Alice (lamport 9). Carol does the same at lamport 10.
- Bob's accept applies, so the primary user becomes Bob. Carol's conflicts (Alice ≠ Bob).
- This is "first accept wins" with no special-case code.

**D. Partition.**
- Only Bob's op reaches the server first, and it applies.
- Alice uploads later. Her op has base OPEN, but the server status has changed, so it conflicts.
- The server outcome is authoritative and both phones adopt it after refreshing. This is the documented
  tiebreak for partitions: arrival order at the server.

## 6. Backend sync pipeline (per op, each in its own transaction)

```
1. parse envelope; body.v == 1; body.companyId == uploader's company           else REJECTED(bad_envelope)
2. INSERT offline_op(op_id …) — unique violation → return stored result as DUPLICATE
3. author device = offline_device by body.authorAddress; must belong to body.authorUserId
                                                                               else REJECTED(unknown_device)
4. Ed25519 verify(publicKey, utf8(body), sig)                                  else REJECTED(bad_signature)
5. workOrder.canBeEditedBy(author)                                             else REJECTED(forbidden)
6. set SecurityContext to author (as DemoController does) and apply via existing services with CAS (§5)
   → APPLIED | CONFLICT (+ conflict comment)
7. update offline_op.result/detail; restore SecurityContext
```

- The ops in a batch are sorted by `(lamport, authorAddress)` before processing.
- A REJECTED op is still stored, so a retry returns the same answer and nothing gets re-evaluated.
- Each op runs in its own transaction (`TransactionTemplate` or `REQUIRES_NEW`), so one failure doesn't roll back
  the rest of the batch.
- Two phones syncing the same op at the same instant: one insert wins the unique constraint. The other catches
  `DataIntegrityViolationException`, re-reads the row, and returns DUPLICATE.

## 7. Delivery and acknowledgement states

There is one state per (op batch, recipient), driven by SDK events:

```
sent ──message_retrying──▶ retrying(n) ──message_delivered──▶ delivered
  │                              └────────message_failed────▶ failed  (terminal; "Resend" re-sends the same envelopes)
  └──────────────message_delivered──────────────────────────▶ delivered
```

- **Delivered** is a machine receipt: the SDK's end-to-end ACK from the recipient's device.
- **Accepted** is a human acknowledgement. It is a signed HANDOFF_ACCEPT op, shown as "Accepted by Bob · time".
- The UI always shows these two separately; the demo depends on that distinction.
- A resend after a failure reuses the same envelope (same opId). The SDK gives it a new message id, so the
  receiver deduplicates by opId.

## 8. Persistence and restart

| What | Where | Survives app kill |
|---|---|---|
| Outbound SDK queue, SDK dedup set, MLS state, identity key | SDK storage + Keychain / EncryptedSharedPreferences | Yes (SDK) |
| Ops, Lamport counter, packs, outbound state, sync results | `offline` slice → redux-persist → AsyncStorage | Yes (D19) |
| Logged-in user, company, settings | AsyncStorage cache (D16) | Yes |
| Nearby peers | `offline` slice. Entries older than 30 s are treated as gone. | Persisted but stale. Rebuilt by discovery. |

On relaunch, `MeshContext` starts the SDK again with the same profile. The SDK re-drives its outbox, and pending
deliveries carry on under the same message ids.

## 9. Connectivity and sync triggers

- `backendReachable` becomes false when a request fails with a network error, and true when any request succeeds.
- When NetInfo reports a change to connected, the app probes `GET auth/me`, which updates reachability.
- Sync runs on a false→true transition and when the user taps **Sync now**.
- Sync is single-flight: one run at a time.
- Tokens: access tokens last 30 minutes and refresh tokens 7 days. The existing 401 → refresh path in
  `utils/api.ts` handles reconnecting. A demo never runs offline for 7 days.

## 10. Failure modes

| Failure | Behaviour |
|---|---|
| Peer out of range | SDK retries, then parks; the UI shows Retrying (n). Delivered once the peer is back in range. |
| App killed with ops in flight | The SDK outbox persists and re-drives after relaunch. Ops persist in the slice. |
| The same op arrives twice (SDK resend, or a manual resend with a new message id) | Dropped by the opId check in `receiveOps` |
| Message from a non-crew or unregistered device | Dropped; a counter shows in Diagnostics |
| Bad signature | Dropped on the device; REJECTED on the server |
| Both phones upload the same ops | One APPLIED, the other DUPLICATE. Exactly one comment per note. |
| Server changed the field while offline | CONFLICT plus a conflict comment (server wins) |
| Crew changed on the server while offline | Not seen until reconnect; the server re-checks at sync (D10) |
| BLE permission denied | MeshContext shows a blocking prompt with a link to Settings |
