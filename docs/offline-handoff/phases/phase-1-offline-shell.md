# Phase 1: Offline-capable app shell

**Goal.** The Atlas app opens and stays usable with no backend: the user stays logged in, cached work orders still
render, an Offline banner tells the truth, and work-order snapshots (offline packs) are captured while online.

**Estimate.** 2 days. **Depends on.** Phase 0 (go).

## Tasks

### 1.1 Cached-login fallback (D16), in `contexts/AuthContext.tsx`
- [ ] After `getInfos` succeeds, save `user`, `company` and `companySettings` JSON to AsyncStorage (one key,
      `offlineAuthCache`).
- [ ] In the `getInfos` catch: if the error is a network error (reuse `isNetworkError` from `utils/api.ts`) **and**
      a cache and an access token exist, initialize as authenticated from the cache. Only an HTTP 401/403 or a
      missing cache should lead to the logged-out state.
- [ ] Clear the cache on logout.
- [ ] Check that `hasEditPermission` and the other permission helpers work from the cached user.

### 1.2 Backend reachability (D22)
- [ ] Create `slices/offline.ts` with an initial state of `backendReachable` and `lastSyncAt`. Later phases extend
      it. Register it in the root reducer so redux-persist picks it up.
- [ ] `utils/api.ts`: set `backendReachable=false` on a network error and `true` on any successful response. Avoid
      a circular import; dispatch through the store module or a small setter.
- [ ] NetInfo listener (in `App.tsx` or a small hook): when it reports connected, probe `GET auth/me`.
- [ ] Offline banner component, shown app-wide when `!backendReachable`: "Offline: changes are saved on this
      device".

### 1.3 Offline pack capture (D17)
- [ ] `offline.packs[woId] = {workOrder, tasks, crew: [], fetchedAt}`. Crew stays empty until Phase 2.
- [ ] When `WODetailsScreen` loads the work order and tasks successfully, dispatch `savePack`, reusing the payloads
      it already fetches. No extra requests.
- [ ] Make sure `WODetailsScreen` and `TasksScreen` render offline from cached state without error loops or a
      logout. Existing edit actions may fail with a snackbar offline; offline editing arrives with the Phase 4 screen.

## Files
`contexts/AuthContext.tsx`, `utils/api.ts`, `slices/offline.ts` (new), `store/index.ts` (root reducer),
`App.tsx` or `hooks/useBackendReachability.ts` (new), a banner component in `components/`, `WODetailsScreen.tsx`
(one dispatch).

## Exit criteria
- [ ] Log in online, open the seeded work order, enable airplane mode, force-quit, relaunch: still logged in, the
      work order and tasks render, and the banner shows.
- [ ] Disable airplane mode: the banner disappears within 5 s with no relaunch needed.
- [ ] Bad credentials and an expired refresh token still log out as before.
- [ ] `offline.packs` contains the seeded work order after opening it.
- [ ] README status updated, and any gotchas added to CODEBASE_MAP §4.
