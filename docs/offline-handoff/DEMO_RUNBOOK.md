# Demo runbook

Started as a draft. Phase 0 fills in the credentials and ids, and Phase 6 finalizes the timings and the talk track.

## Hardware and setup
- Laptop running the full Atlas stack (`docker compose -f docker-compose.yml -f docker-compose.offline-demo.yml up -d`),
  with the web UI open at `http://<LAN-IP>:3000` and logged in as the admin.
- **Alice:** Android phone, dev build. **Bob:** iPhone, dev build. Both on the laptop's Wi-Fi, Bluetooth on,
  battery above 50 %, auto-lock off.
- **No other Offline Protocol SDK apps installed on either phone** (SDK demo apps etc.). They publish the same BLE
  GATT service, and peers can bind to the wrong app's identity.
- Optional **Carol:** a third phone, for the "not on the crew" permissions scene.

| Who | Login | Device |
|---|---|---|
| Admin | `admin@northwind.test` / `NorthwindDemo!2026` | Laptop, web UI |
| Alice | `alice@northwind.test` / `NorthwindDemo!2026` | Android |
| Bob | `bob@northwind.test` / `NorthwindDemo!2026` | iPhone |
| Carol | `carol@northwind.test` / `NorthwindDemo!2026` | Optional |

The seeded work order is *CH-2 quarterly inspection*, id **1** on a fresh database (the seed prints it). Alice is user
id 3, Bob 4, Carol 5. Tasks 1-3 are checklist subtasks and task 4 is the numeric "Discharge pressure, psi".

Seed from a fresh database: `API_URL=http://<LAN-IP>:3000/api node scripts/offline-demo/seed.mjs` (idempotent).
Set `DEMO_PASSWORD` to use a different password; it must be 12+ characters and not a common password.

## Pre-flight (10 min before)
1. OrbStack is signed in and has verified its licence recently (it stops the stack when it can't reach its licence
   server). `docker ps` lists all five containers.
2. Run `scripts/offline-demo/reset.sh`.
3. On both phones: Diagnostics → Reset offline data, then force-quit and relaunch.
4. Both phones online: log in, open the work order, then open Offline handoff and check that the crew lists the
   other technician.
5. Bring the phones within 1 m of each other and check that both show the other under Nearby crew.

## Script
| Step | Action | Say |
|---|---|---|
| 1 | Show the work order in the web UI and on both phones | "Two technicians, one work order. Their devices registered with Atlas, so each knows who its crew is." |
| 2 | Both phones: airplane mode on, then Bluetooth back on. The Offline banner appears. | "No backend, no internet. Only the radio between them." |
| 3 | Alice: add the note, tick 2 checklist items, Request handoff | "Every change is signed by Alice's device and stored locally." |
| 4 | Bob: Alice is under Nearby crew; the handoff modal appears; tap Accept responsibility | "Bob's phone found her, checked the signature and that Alice is on this crew, and received it encrypted." |
| 5 | Show both phones: Delivered vs "Accepted by Bob" | "Delivered is the machine receipt. Accepted is Bob taking responsibility, signed by Bob." |
| 6 | Bob: force-quit, relaunch while still offline | "Nothing lives only in memory. The session, the log and the delivery state survive." |
| + | Retry: Bob turns Bluetooth off; Alice adds a note (Retrying); Bob turns Bluetooth on (Delivered) | "Retries are automatic." |
| + | Concurrent edits: Alice sets On hold and Bob sets In progress, then let them sync over BLE. Meanwhile the admin edits the status on the web. | "Two people changed the same field. Both phones agree on a winner and flag the loser. The server's own edit wins at sync." |
| 7 | Bob turns airplane mode off, then Alice. Refresh the web UI. | "Both uploaded the same history, and Atlas recorded it once, under the right names. Bob is now primary." |

## Recovery
| Symptom | Fix |
|---|---|
| Peer not under Nearby crew after 20 s | Toggle Bluetooth on the phone that's missing from the other's list, then reopen the screen. Check Diagnostics for permissions. |
| Delivery stuck on Retrying | Bring the phones closer and keep both screens on. The SDK re-drives on its own. |
| App logged out after relaunch | The cached login is missing. Log in online before the demo (pre-flight step 3). |
| Sync shows Rejected unknown_device | The device registered under a different address. Reset offline data and redo pre-flight step 3. |
| Web UI / API down, `docker ps` can't connect | OrbStack stopped (licence check failed). `orb start`, then `docker compose -f docker-compose.yml -f docker-compose.offline-demo.yml up -d`. The data survives in volumes. |
| Something unrecoverable | Switch to the backup video *(Phase 6: location)* |
