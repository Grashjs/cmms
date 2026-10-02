# Phase 4: Handoff UX

**Goal.** A polished screen that takes a non-technical audience through demo steps 1-6 with no backend.

**Estimate.** 4-5 days. **Depends on.** Phase 3.

## Tasks

### 4.1 Entry point and navigation
- [ ] Add an "Offline handoff" item to the `work-order-details-sheet` action sheet. Show it whenever the work order
      has a pack (crew is non-empty).
- [ ] Register `OfflineHandoff: { workOrderId: number }` in `navigation/index.tsx` and `types.tsx`.
- [ ] Add English strings to `mobile/i18n` (other locales fall back). Keep the copy plain; it's shown to prospects.

### 4.2 `screens/workOrders/OfflineHandoffScreen.tsx`

Everything on the screen reads from `selectMergedView(woId)`. Sections, top to bottom:
- [ ] **Header:** work-order title and custom id; merged status chip; primary assignee; connection pill (Offline /
      Online · last synced hh:mm); "Sync now" button (wired in Phase 5, disabled until then).
- [ ] **Nearby crew:** chips from `nearbyCrew(woId)` with name and signal. Grey chips for crew members not in range.
      Call `startNearbyScan` on focus and `stopNearbyScan` on blur.
- [ ] **Handoff card**, which has three states:
  - No open request: "Request handoff" button (with an optional note).
  - Open request, viewed by its requester: "Waiting for a crew member to accept…".
  - Open request, viewed by another crew member: prominent "**Accept responsibility**" button. Also show an in-app
    modal when the request arrives while the screen is open.
  - After acceptance, both phones show "Handoff accepted by {name} · {time}".
- [ ] **Actions:** Add update (NOTE); Change status (the same four statuses as `WODetailsScreen`); Checklist.
- [ ] **Checklist:** the pack's tasks with merged values. Toggling a subtask or entering a numeric value creates a
      TASK_UPDATE. Record in CONTRACTS §1 the exact `value` strings `TasksScreen` uses per task type, and match
      them.
- [ ] **Timeline:** newest first, every op for this work order with:
  - author name and time;
  - text or a short description;
  - a "Signed ✓" marker for verified ops;
  - per recipient: Sent / Retrying (n) / Delivered / Failed (with a Resend action);
  - a Conflict badge when the merged view marks the op as a loser;
  - a sync badge (Phase 5).

### 4.3 Permission-aware UI
- [ ] Hide all actions if the current user isn't in the pack's crew; show "You are not on this work order's crew".
- [ ] Hide Accept for the requester.

### 4.4 Restart behaviour
- [ ] After relaunching offline and navigating back, the screen restores fully: timeline, delivery states, handoff
      state, and the Lamport counter (new ops get higher clocks).

## Exit criteria
- [ ] Demo steps 1-6 in DEMO_RUNBOOK run end to end on the iPhone/Android pair with the backend unreachable,
      including the Bob restart.
- [ ] Delivered and Accepted are visibly separate states.
- [ ] Retry scene: turn Bob's Bluetooth off, add a note on Alice → Retrying (n) → Bluetooth on → Delivered, with no
      user action.
- [ ] A Technician-role user who isn't on the crew sees no actions.
- [ ] README status updated, and any gotchas added to CODEBASE_MAP §4.
