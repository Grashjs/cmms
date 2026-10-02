# Phase 6: Concurrency scene, hardening and rehearsal

**Goal.** The demo runs the same way every time in front of a prospect, with a recovery path for each thing that
can go wrong.

**Estimate.** 3 days. **Depends on.** Phase 5.

## Tasks

### 6.1 Concurrency scene
- [ ] Script the concurrent-status scene (ARCHITECTURE §5 examples A and B) into DEMO_RUNBOOK with exact taps.
- [ ] Make the conflict easy to read on screen: the losing op shows "Conflict: Alice's ON_HOLD came first", and the
      merged status chip briefly highlights when it changes.

### 6.2 Hardening
- [ ] Run each failure mode in ARCHITECTURE §10 once on real devices. Fix anything that deviates, or document it.
- [ ] Bluetooth permission denied or Bluetooth off: show a clear blocking state with a Settings link.
- [ ] Keep the screen awake while OfflineHandoffScreen is focused, because iOS BLE needs the app in the foreground.
      Use `expo-keep-awake` if it's already a dependency, otherwise the smallest option, and note it here.
- [ ] Throttle timeline re-renders and check there's no jank with 50 ops.
- [ ] Remove the Phase 3 test-harness buttons from Diagnostics, or hide them behind a dev flag.

### 6.3 Demo operations
- [ ] Make sure `scripts/offline-demo/reset.sh` returns everything to the opening state in under 30 s: the backend,
      plus instructions to clear app data on both phones (or an in-app "Reset offline data" action in
      Diagnostics).
- [ ] Finalize DEMO_RUNBOOK: pre-flight checklist, talk track per step, timings, recovery table.
- [ ] Rehearse the full script three times from reset, and record the timings. Record one clean run as a backup
      video.

## Exit criteria
- [ ] Three consecutive full rehearsals pass with no unscripted intervention.
- [ ] Every row of the runbook recovery table has been tried at least once.
- [ ] The backup video is recorded and its location noted in DEMO_RUNBOOK.
- [ ] README status shows all phases done.
