# Phase 5: Backend reconciliation

**Goal.** When connectivity returns, either phone (or both) uploads the op log. Atlas applies each op exactly once,
attributed to its author, under the server-wins policy. The web UI then shows a clean, duplicate-free record.

**Estimate.** 4 days. **Depends on.** Phase 4. The backend half can start in parallel with Phase 4 once Phase 3 is
done.

## Backend

### 5.1 Schema
- [ ] Add the `offline_op` changeset per CONTRACTS §6, including the unique constraint on `op_id`.

### 5.2 `service/OfflineSyncService`
Implements ARCHITECTURE §6 exactly.
- [ ] `sync(List<EnvelopeDTO>, User uploader) → List<OpResultDTO>`.
  - Parse all bodies and sort by `(lamport, authorAddress)`. Return results in the original input order.
  - Process each op in its own transaction (`TransactionTemplate`).
- [ ] Dedup: insert the `offline_op` row first. On `DataIntegrityViolationException` (or an existing row), return
      DUPLICATE with the stored result as `detail`.
- [ ] Verification: device lookup by address, owner equals `authorUserId`, Ed25519 verify (CONTRACTS §2),
      `canBeEditedBy(author)`. On failure, store REJECTED with the reason.
- [ ] Apply as the author: temporarily set the SecurityContext to the author, as DemoController L77-84 does, and
      restore it in `finally`.

  | Op | Apply |
  |---|---|
  | NOTE / HANDOFF_REQUEST | `CommentService.create(dto, author)` with the CONTRACTS §7 template |
  | STATUS | Compare-and-set on `workOrder.status == base`, then `WorkOrderService.changeStatus(dto{status: to}, id, author, "OFFLINE")` |
  | TASK_UPDATE | Compare-and-set on `task.<field> == base` (null-safe), then `taskService.update(taskId, patchDto)` |
  | HANDOFF_ACCEPT | Compare-and-set on `primaryUser.id == basePrimaryUserId`, then `WorkOrderService.patch` with `primaryUser = author` and `assignedTo` ∪ {requester, author}, plus the accept comment |
  | Any compare-and-set failure | CONFLICT, plus the conflict comment as the author |

- [ ] Append " · synced by {uploader}" to comments when the uploader isn't the author.

### 5.3 Endpoint
- [ ] `POST /offline/ops` in `OfflineController`, per CONTRACTS §5 (maximum 200 ops, 200 OK with per-op results).

### 5.4 Integration test `OfflineSyncIntegrationTest`
Generate an Ed25519 keypair in the test with the JDK, register a device, and build envelopes.
- [ ] A NOTE becomes APPLIED and exactly one comment exists, authored by the author.
- [ ] Uploading the same batch again gives DUPLICATE for every op, and the comment count is unchanged.
- [ ] Two different uploaders send the same op: one APPLIED, one DUPLICATE.
- [ ] STATUS with a stale base gives CONFLICT, the status is unchanged, and a conflict comment exists.
- [ ] Concurrent STATUS ops (example A) in one batch: the first applies, the second conflicts.
- [ ] HANDOFF_ACCEPT: primary becomes the acceptor and the requester stays in `assignedTo`. A second accept
      conflicts.
- [ ] A bad signature and a non-crew author (Carol) are REJECTED, and a repeat upload returns the same result.

## Mobile

### 5.5 Sync engine (thunk in `slices/offline.ts`)
- [ ] `syncOffline()`: single-flight. Collect ops with no `sync` result (both local and peer ops), POST them in
      chunks of 200, then `markSynced(results)`.
- [ ] Then refresh every touched work order: refetch the work order, tasks, crew and comments, and replace the
      pack. Ops that now have results drop out of the overlay.
- [ ] Triggers: `backendReachable` going false→true (from Phase 1), and the Sync now button.
- [ ] UI: sync badges in the timeline (Synced / Already synced / Conflict: server kept X / Rejected), the
      header's last-synced time, and a toast summary such as "4 synced · 1 conflict".

## Exit criteria
- [ ] Integration tests pass.
- [ ] Demo step 7: after the offline session, turning off airplane mode on Bob first and then Alice gives:
  - in the web UI, exactly one comment per NOTE and HANDOFF op, in the right order and attributed to the right
    authors;
  - Bob as primary, with Alice still assigned;
  - the task values and status matching the merged view;
  - Alice's sync reporting "already synced" for the ops Bob uploaded.
- [ ] Web edit during the offline session (example B) ends as a server-wins conflict, shown on both phones and as a
      web comment.
- [ ] Notifications and webhooks fire once per applied op, not per upload.
- [ ] `reset.sh` also clears `offline_op` and offline comments.
- [ ] README status updated, and any gotchas added to CODEBASE_MAP §4.
