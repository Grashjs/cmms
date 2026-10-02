# Contracts

These formats are shared by the mobile app and the API. **Change this file first, then both sides, in the same
change.** Every format below is version 1.

## 1. Op body

Shared fields:

```ts
type OpBody = {
  v: 1;
  opId: string;            // 24 hex chars (utils/randomId.ts) — globally unique per op
  companyId: number;
  workOrderId: number;
  type: 'NOTE' | 'STATUS' | 'TASK_UPDATE' | 'HANDOFF_REQUEST' | 'HANDOFF_ACCEPT';
  authorUserId: number;    // Atlas user id
  authorAddress: string;   // SDK off1… address of the authoring device
  lamport: number;         // per-device Lamport counter (ARCHITECTURE §4)
  occurredAt: number;      // device wall clock, ms epoch — display only, never used for ordering
  payload: NotePayload | StatusPayload | TaskUpdatePayload | HandoffRequestPayload | HandoffAcceptPayload;
};
```

Payloads:

```ts
type NotePayload           = { text: string };                               // 1..2000 chars
type StatusPayload         = { base: WOStatus; to: WOStatus };               // WOStatus = 'OPEN'|'IN_PROGRESS'|'ON_HOLD'|'COMPLETE'
type TaskUpdatePayload     = { taskId: number; field: 'value' | 'notes'; base: string | null; to: string | null };
type HandoffRequestPayload = { note?: string };                              // open to any crew member except the author (D24)
type HandoffAcceptPayload  = { requestOpId: string; basePrimaryUserId: number | null };
```

Rules:
- `base` is the merged-view value when the user acted (ARCHITECTURE §5).
- A STATUS op with `to: 'COMPLETE'` carries no signature or feedback. The seeded work order has
  `requiredSignature: false`.
- `TaskUpdatePayload.value` uses whatever string `TasksScreen` sends today for that task type. Phase 4 records the
  exact values in CODEBASE_MAP.md.

Ordering key: `(lamport, authorAddress)`, compared as numbers, then strings.

## 2. Envelope and signing

```ts
type Envelope = { body: string; sig: string };   // body = JSON.stringify(OpBody); sig = base64(Ed25519 signature)
```

- **To sign:** `bytes = utf8(body)`, then `sig = base64(await protocol.signData([...bytes]))`.
- **To verify on a device:** `protocol.verifySignature(publicKeyBytes, [...utf8(body)], [...base64decode(sig)])`.
- **To verify on the server:** use the JDK 17 built-in Ed25519.
  - Wrap the raw 32-byte key in an X.509 `SubjectPublicKeyInfo` by prefixing the hex bytes
    `302a300506032b6570032100`.
  - Then call `KeyFactory.getInstance("Ed25519")`, followed by `Signature.getInstance("Ed25519").verify`.
- The signature covers the body **string** exactly as transmitted. Never re-serialize before verifying.
- Public keys travel as base64 of the 32 raw bytes from `getIdentityPublicKey()`.

## 3. Mesh message (the `content` of `sendMessage`)

```json
{ "t": "atlas.ops.v1", "ops": [ { "body": "...", "sig": "..." } ] }
```

- Send with `recipient` set to the crew member's `off1…` address and priority `High`.
- Keep each message under 64 KiB, well inside the SDK's 256 KiB cap. Split into several messages if needed (in
  practice there's one op per message).
- Receivers ignore any message whose `t` they don't recognize.

## 4. Service discovery

- Each device registers `registerService('atlas.handoff', '1', {})`. Capabilities stay empty, because discovery
  traffic is signed plaintext (D11).
- Discovery: while the handoff screen is open, call `discoverServices('atlas.handoff')` every 5 s.
- "Nearby crew" = `provider_peer_id` values from `service_discovered` events, intersected with the work order's
  crew addresses, with `neighbor_discovered`/`neighbor_lost` used as a hint.

## 5. REST API

All endpoints are under `/offline`. They require `ROLE_CLIENT` and a JWT, and are tenant-scoped through
`CompanyAudit`.

### `POST /offline/devices`

```json
// request
{ "address": "off1…", "publicKey": "<base64 32 bytes>" }
// 200
{ "id": 12, "address": "off1…" }
```

- Upsert keyed by `address`, bound to `whoami`.
- 409 if the address is already registered to a different user.
- 400 if the public key isn't 32 bytes.

### `GET /offline/work-orders/{id}/crew`

```json
// 200
[ { "userId": 7, "firstName": "Alice", "lastName": "Ng", "address": "off1…", "publicKey": "<base64>" } ]
```

- The caller needs view access to the work order.
- Returns every company user `u` with a registered device where `workOrder.canBeEditedBy(u)` holds.
- If a user has several devices, they get one entry per device.

### `POST /offline/ops`

```json
// request
{ "ops": [ { "body": "...", "sig": "..." } ] }          // max 200 per request
// 200 — one result per input op, same order as received
{ "results": [ { "opId": "…", "result": "APPLIED", "detail": null, "uploadedBy": 9, "firstSyncedAt": 1790000000000 } ] }
```

| `result` | Meaning | Device action |
|---|---|---|
| `APPLIED` | First time seen; effect applied | Mark synced and drop from overlay |
| `DUPLICATE` | Already processed earlier (by anyone). `detail` holds the original result. | Mark with the original result and drop from overlay |
| `CONFLICT` | Base value didn't match (server wins, D4); a conflict comment was written | Mark conflict and drop from overlay |
| `REJECTED` | `bad_envelope` / `unknown_device` / `bad_signature` / `forbidden`. Stored, so it is final. | Mark rejected and drop from overlay |

- The endpoint returns 200 even when some ops fail.
- Non-2xx means the whole request failed and the device retries later.

## 6. Database (one Liquibase changelog)

### `offline_device`

| Column | Type | Notes |
|---|---|---|
| id | bigint PK | Sequence `offline_device_seq`, increment 50 (CompanyAudit convention) |
| company_id | bigint FK company, on delete cascade | Not null |
| user_id | bigint FK own_user, on delete cascade | Not null |
| address | varchar(128) | Unique |
| public_key | varchar(64) | Base64 |
| created_at, updated_at, created_by_id, updated_by_id | | From the audit superclasses; columns must match the entity (`ddl-auto: validate`) |

### `offline_op`

| Column | Type | Notes |
|---|---|---|
| id | bigint PK | Sequence `offline_op_seq`, increment 50 |
| company_id | bigint FK company, on delete cascade | Not null |
| op_id | varchar(64) | **Unique.** This is the dedup key. |
| work_order_id | bigint FK work_order, on delete cascade | Nullable if the envelope couldn't be parsed |
| type | varchar(32) | |
| author_user_id | bigint FK own_user, on delete set null | Nullable |
| author_address | varchar(128) | |
| lamport | bigint | |
| occurred_at | timestamp | From the body; display only |
| body | text | Exact signed string |
| sig | varchar(128) | |
| result | varchar(16) | `APPLIED` / `CONFLICT` / `REJECTED` |
| detail | varchar(255) | Nullable |
| uploaded_by_id | bigint FK own_user, on delete set null | |
| created_at | timestamp | First sync time |

Both entities extend `CompanyAudit` (id, company, createdBy/updatedBy, createdAt/updatedAt). Copy column names and types
from an existing CompanyAudit changelog such as `2026_03_04_00000000001_add_request_portal.xml`. The user table is `own_user`.

## 7. Comment templates (what the web UI shows)

`{name}` is the author's full name. `{time}` is `occurredAt` formatted `yyyy-MM-dd HH:mm` in the company time zone.
Comments are created as the author (D14).

| Op | Comment content |
|---|---|
| NOTE | `[Offline update · {time}] {text}` |
| HANDOFF_REQUEST | `[Offline handoff · {time}] {name} requested a handoff.{ note ? " Note: " + note : ""}` |
| HANDOFF_ACCEPT applied | `[Offline handoff · {time}] {name} accepted responsibility (requested by {requesterName}). Primary assignee is now {name}.` |
| STATUS applied | No comment. The status change itself is the record. |
| TASK_UPDATE applied | No comment. The task value is the record. |
| Any CONFLICT | `[Offline conflict · {time}] {name}'s offline change was not applied: {field} → {to} (expected {base}, server has {current}).` |

Any op synced by someone other than its author gets ` · synced by {uploaderName}` appended.
