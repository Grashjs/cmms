# Phase 3: Signed op log and merged view

**Goal.** Changes made offline become signed, persisted ops that reach crew phones over encrypted BLE, are
verified and deduplicated there, and merge into the same view on every device. The work stops at a test harness
on the diagnostics screen; the real UI is Phase 4.

**Estimate.** 3 days. **Depends on.** Phase 2.

## Tasks

### 3.1 Pure logic: `utils/offlineOps.ts`
No SDK, Redux or React imports, so it can be unit-tested directly.
- [ ] Types from CONTRACTS §1-3: `OpBody`, payloads, `Envelope`, `MeshMessage`.
- [ ] `buildOp(kind, args, ctx) → OpBody`, where `ctx = {companyId, userId, address, lamport}`.
- [ ] `opOrder(a, b)` using the `(lamport, authorAddress)` ordering key.
- [ ] `mergedView(pack, ops) → { status, primaryUserId, tasks, timeline, handoff: {openRequest?, acceptedBy?,
      acceptedAt?}, results: Record<opId, 'APPLIED'|'CONFLICT'> }`. Implements ARCHITECTURE §5 exactly.
      Only ops without a sync result are applied; ops with a sync result appear in the timeline only.
- [ ] `parseMeshMessage(content) → Envelope[] | null`, tolerating unknown `t` values.
- [ ] `offlineOps.test.ts` (jest-expo), one case each for:
  - concurrent STATUS (worked example A);
  - an edit based on a received op doesn't conflict;
  - TASK_UPDATE compare-and-set;
  - double HANDOFF_ACCEPT (example C);
  - NOTE ordering by Lamport;
  - synced ops excluded from the overlay;
  - the same opId twice counted once.

### 3.2 Crypto glue: `utils/offlineCrypto.ts`
- [ ] `signEnvelope(protocol, body) → Envelope` and `verifyEnvelope(protocol, envelope, publicKeyB64) → OpBody |
      null`, per CONTRACTS §2.
- [ ] Verify on-device that `TextEncoder`, `atob`/`btoa` and `crypto.getRandomValues` (used by `randomId.ts`)
      exist under Hermes on RN 0.79. If one is missing, add the smallest polyfill and note it in CODEBASE_MAP §4;
      `expo-crypto` is acceptable.

### 3.3 Slice: extend `slices/offline.ts`
- [ ] State: `lamport`, `ops: Record<opId, {envelope, body, origin: 'local'|'peer', receivedFrom?, sync?: {result,
      detail}}>`, and `outbound: Record<messageId, {opIds, recipient, state, retryCount, updatedAt}>`.
- [ ] Reducers:
  - `addLocalOp`: sets `lamport = body.lamport`.
  - `receiveOps`: dedup by opId; `lamport = max(lamport, body.lamport)`.
  - `setOutbound`, `markDelivery` (driven by events), `markSynced` (used in Phase 5).
- [ ] Selector `selectMergedView(woId)` (memoized) = `mergedView(packs[woId], ops for woId)`.

### 3.4 Outbound
- [ ] Thunk `createOp(woId, kind, args)`:
  - Build the body with `lamport + 1` and `base` taken from the current merged view.
  - Sign, `addLocalOp`, then for each crew member other than me, `sendMessage({recipient, content:
    JSON.stringify({t:'atlas.ops.v1', ops:[envelope]}), priority: High})`.
  - Call `setOutbound(messageId, …)` for each send.
- [ ] Wire `message_retrying` / `message_delivered` / `message_failed` to `markDelivery` in MeshContext.
- [ ] `resendOp(opId, recipient)` sends the same envelope again (used by a "Resend" action after `failed`).

### 3.5 Inbound (in MeshContext's `message_received` handler)
- [ ] Ignore anything where `app_id !== 'atlas-cmms'` or `parseMeshMessage` fails.
- [ ] For each envelope:
  1. Parse the body.
  2. Check that the sender is in the crew of `body.workOrderId`.
  3. Look up the author's crew entry by `authorAddress` and check that `authorUserId` matches.
  4. `verifyEnvelope` with that public key.
  5. If all pass, `receiveOps`. Otherwise increment a diagnostics drop counter with the reason.
- [ ] Dispatch synchronously in the handler. redux-persist then saves it (D19).

### 3.6 Test harness on the diagnostics screen
- [ ] Buttons for the last opened work order: "Add test note", "Set status ON_HOLD", "Set status IN_PROGRESS".
- [ ] Show the raw merged view as JSON, plus the op list with delivery state.

## Exit criteria
- [ ] `npm test` passes, including `offlineOps.test.ts`.
- [ ] Offline: a note added on Alice's phone appears in Bob's merged view in under 10 s at 1 m. Alice's outbound
      entry shows `delivered`.
- [ ] Both phones offline, each changes status before receiving the other's op. They converge on the same status,
      with the same op marked CONFLICT on both.
- [ ] Kill Bob's app before Alice sends, send, relaunch Bob: the op arrives, and appears only once.
- [ ] Kill Alice's app straight after sending, relaunch: the op is still in her log and delivery still completes.
- [ ] A tampered envelope (flip one char of the body in a temporary debug path or a unit test) is rejected and
      counted.
- [ ] README status updated, and any gotchas added to CODEBASE_MAP §4.
