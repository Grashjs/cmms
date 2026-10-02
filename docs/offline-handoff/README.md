# Offline Work-Order Handoff

A prospect-facing demo that integrates the **Offline Protocol SDK** into the **Atlas CMMS** mobile app. Two
technicians hand off a work order face to face with no backend, over encrypted Bluetooth LE between an iPhone and
an Android phone. When connectivity returns, the work order reconciles into Atlas without duplicate records.

This folder is the source of truth for the build. If code and docs disagree, fix whichever one is wrong in the
same change.

| Doc | What it holds |
|---|---|
| [DECISIONS.md](DECISIONS.md) | Decisions that are settled. Reverse one by adding a new entry, never by editing silently. |
| [ARCHITECTURE.md](ARCHITECTURE.md) | Components, trust model, data lifecycle, merge algorithm, failure modes |
| [CONTRACTS.md](CONTRACTS.md) | Operation format, signing, mesh messages, REST API, DB tables, comment templates |
| [CODEBASE_MAP.md](CODEBASE_MAP.md) | Where things live in Atlas and the SDK, plus gotchas found during the build |
| [DEMO_RUNBOOK.md](DEMO_RUNBOOK.md) | Hardware, reset, the demo script, recovery steps |
| [phases/](phases/) | One file per build phase, executed in order |

## The demo story

| # | What the audience sees | What it proves |
|---|---|---|
| 1 | Alice (Android) and Bob (iPhone) both open the work order *CH-2 quarterly inspection*, assigned to both, with Alice as primary. | Identity and permissions. Each device registers its SDK identity with Atlas and caches the work order's crew. |
| 2 | Both phones go to airplane mode with Bluetooth turned back on. An "Offline" banner appears. | The backend is cut off but the local link is still available. |
| 3 | Alice records a note ("Compressor bearing noise, needs follow-up"), ticks two checklist tasks, and requests a handoff. | Persistent local storage and signed operations |
| 4 | Bob's phone shows Alice under "Nearby crew", receives the update, and shows a handoff prompt. Bob taps **Accept responsibility**. | Service discovery, encrypted exchange, delivery receipts, human acknowledgement |
| 5 | Both phones show "Handoff accepted by Bob · 10:43", and Alice's items show Delivered. | Delivery receipts and human acknowledgement, kept separate |
| 6 | Bob force-quits the app and relaunches it while still offline. Everything is still there, and he stays logged in. | Persistence and recovery |
| 7 | Airplane mode goes off and both phones sync. The Atlas web UI shows one comment per update, Bob as primary, and the ticked tasks. The second phone's upload reports "already synced". | Backend reconciliation and deduplication |
| + | **Retries:** Bob turns Bluetooth off, Alice sends a note, and her phone shows "Retrying (2)". Bob turns Bluetooth back on and the note shows Delivered. | Retries |
| + | **Concurrent edits:** offline, Alice sets the status to On hold and Bob sets it to In progress. Both phones settle on the same winner and flag the loser. An admin changes the status on the web while the phones are offline, and after sync the server value wins with a conflict note. | Concurrent edits with a defined resolution policy |

## Scope

**In scope:**
- Notes, work-order status, task checklist values and notes, handoff request, handoff accept.
- iOS and Android talking over BLE.
- One seeded company.

**Out of scope:**
- Labor, parts, photos and attachments.
- Creating work orders offline.
- Editing or deleting comments offline.
- Wi-Fi Direct, LAN, internet relay, Nostr and Reticulum transports.
- SDK groups and the SDK's DataStore.
- EAS cloud builds and App Store distribution.
- Changes to the web UI.

## Phase status

Update this table when a phase's exit criteria are all checked.

| Phase | Name | Estimate | Status |
|---|---|---|---|
| 0 | [Environment, seed and BLE spike (go/no-go)](phases/phase-0-environment-and-spike.md) | 3 d | ☑ Done (GO) |
| 1 | [Offline-capable app shell](phases/phase-1-offline-shell.md) | 2 d | ☐ Not started |
| 2 | [Identity, crew and mesh runtime](phases/phase-2-identity-crew-mesh.md) | 3-4 d | ☐ Not started |
| 3 | [Signed operation log and merged view](phases/phase-3-op-log.md) | 3 d | ☐ Not started |
| 4 | [Handoff UX](phases/phase-4-handoff-ux.md) | 4-5 d | ☐ Not started |
| 5 | [Backend reconciliation](phases/phase-5-reconciliation.md) | 4 d | ☐ Not started |
| 6 | [Concurrency scene, hardening, rehearsal](phases/phase-6-hardening-and-demo.md) | 3 d | ☐ Not started |

Total is about 22-24 working days for one engineer.

## Top risks

| Risk | Retired in | Mitigation |
|---|---|---|
| iOS↔Android BLE in the SDK is claimed in its docs but untested by us | Phase 0 (go/no-go) | Measure it in a spike before building anything. The SDK is linked locally, so fixes can land the same day. |
| The SDK's native libraries fail to build or link into the Expo app (static frameworks, Gradle versions) | Phase 0 | The SDK's own example apps show the linking setup. Fall back to the npm 0.27.0 tarball's prebuilt binaries. |
| The SDK sends its delivery receipt before our handler has saved the message | Phase 3 | Persist inside the `message_received` handler. The remaining window is accepted for the demo (D19). |
| Opening the app offline logs the user out today | Phase 1 | Cached-login fallback (D16) |
| Seeding users without email | Phase 0 | Try the invite flow with `INVITATION_VIA_EMAIL=false`, and fall back to SQL |
| iOS stops BLE in the background | Phase 6 | Keep both apps in the foreground with screens awake during the demo (runbook) |

## Glossary

- **Crew:** users who may edit a work order, according to Atlas's `WorkOrder.canBeEditedBy`, and who have registered a device. Snapshotted while online.
- **Address:** the SDK's self-certifying peer id (`off1…`), derived from the device's Ed25519 identity key.
- **Op:** one immutable, signed change (a note, status, task update, handoff request or handoff accept). See CONTRACTS.md.
- **Envelope:** `{body, sig}`. The serialized op plus its signature.
- **Offline pack:** the cached work order, its tasks and its crew, captured while online. Ops are applied on top of it.
- **Merged view:** the offline pack with unsynced ops applied in total order. This is what the phone displays.
