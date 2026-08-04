# Reporting and BI

Status: stages 0 and 1 implemented (2026-08-04). Stages 2 and 3 not started, and stage 3
deliberately so.

This document exists because reporting is the area of this application most likely to need a
much larger build-out later. Everything below is written so that a later, bigger effort starts
from a known position instead of rediscovering it.

## 1. What was wrong

The application ships analytics — the "Statistics" section — as **42 endpoints** across five
controllers in [`controller/analytics/`](../api/src/main/java/com/grash/controller/analytics).
They produce fixed charts. Three properties made them unusable as a reporting foundation, in
descending order of how much they actually hurt:

1. **No filter axis exists.** Every one of the 42 endpoints takes a `DateRange` and an
   optional `companyId`. Nothing else. There is no way to ask "…but only for building B", "…only
   for the HVAC category", "…only for this team". This is not a frontend gap: the API has no
   parameter to carry the question. Every individualisation wish is therefore a backend change,
   in 42 signatures.
2. **Aggregation lives in controllers, over fully-loaded collections.** A typical endpoint reads
   `Collection<WorkOrder>` for the whole company and date range, then computes with Java
   streams — see
   [`WOAnalyticsController.getCompleteStats`](../api/src/main/java/com/grash/controller/analytics/WOAnalyticsController.java).
   Some paths add an N+1 on top (one labour query per work order). At this instance's data
   volumes this is a code smell, not a live problem, and the 44 `@Cacheable` annotations on
   those endpoints blunt it further. **It is not a reason to rewrite them.** Point 1 is.
3. **Nothing is tabular.** The output is pre-summed numbers for chart widgets. "Give me the
   list behind that number, as a table, with the columns I choose" has no endpoint at all.

Two things that were *not* wrong, and were initially assumed to be:

- **The filter DSL is capable enough.** `SearchCriteria` (package
  [`advancedsearch`](../api/src/main/java/com/grash/advancedsearch)) ANDs its `FilterField`s,
  and each field may carry `alternatives`, which `WrapperSpecification.wrapAlternatives` ORs
  in. So AND-of-ORs works today. What it cannot do is `GROUP BY` or column projection: it
  filters rows and returns whole entities. Good enough for tabular reporting, not for
  aggregates.
- **The frontend already had most of a report table.** The work-order and asset lists run on
  TanStack Table ([`CustomDatagrid2`](../frontend/src/content/own/components/CustomDatagrid2))
  with column visibility, ordering, resizing, pinning and server-side paging and sorting, plus
  a filter drawer over `SearchCriteria`. Two things were missing: saved views (there was one
  unnamed `localStorage` slot per table, per browser) and any way to export the filtered result
  — `/export/*` dumped the whole company table and ignored every filter.

## 2. The staged plan, and where the line was drawn

| Stage | Scope | Status |
|---|---|---|
| 0 | `rpt_*` reporting views in Postgres | **done** |
| 1 | Saved list views + filtered, column-selectable export | **done** |
| 2 | Generic aggregation endpoint (`groupBy` / measures) | not started, see §6 |
| 3 | Full self-service report builder in the app | **rejected**, see §6 |

An external BI tool (Metabase) reading the stage-0 views was evaluated and **deferred** — see
§7, which records what that evaluation found so it does not have to be redone.

## 3. Stage 0 — the `rpt_*` views

[`2026_08_04_00000000001_reporting_views.xml`](../api/src/main/resources/db/changelog/2026_08_04_00000000001_reporting_views.xml)

Five views. They exist because the report-shaped work — labelling ordinal enums, resolving
foreign keys to names, pivoting EAV custom fields, recomputing per-work-order cost — is
identical for every consumer, and doing it once in SQL is what makes the numbers agree between
a CSV export, a future endpoint and a BI tool.

| View | Grain | Use |
|---|---|---|
| `rpt_work_order_cost` | work order | labour / part / additional cost and logged time |
| `rpt_work_order` | work order | the report-ready work order: labels, names, KPIs, costs, custom fields |
| `rpt_asset` | asset | asset with work-order and downtime rollups |
| `rpt_cost_line` | cost line | tidy long format: one row per labour / part / additional cost |
| `rpt_custom_field_value` | custom field value | EAV in long form with type-guarded numeric and date columns |

No entity maps them, so `ddl-auto: validate` ignores them entirely.

### Rules any new `rpt_` view must follow

1. **Translate enum ordinals with an inline `CASE`, next to the column.**
   `2026_01_10_1768015926_enums_type.xml` converted the enum columns to `SMALLINT` holding Java
   ordinals. A wrong number is a silent data bug, so it has to be visible in the diff rather
   than hidden in a lookup table. The authority is the declaration order in
   `model/enums/*.java`, and those enums may only be **appended** to:

   | Enum | Ordinals |
   |---|---|
   | `Status` | 0 OPEN, 1 IN_PROGRESS, 2 ON_HOLD, 3 COMPLETE |
   | `Priority` | 0 NONE, 1 LOW, 2 MEDIUM, 3 HIGH |
   | `AssetStatus` | 0 OPERATIONAL, 1 DOWN, 2 MODERNIZATION, 3 STANDBY, 4 INSPECTION_SCHEDULED, 5 COMMISSIONING, 6 EMERGENCY_SHUTDOWN |

2. **Mirror the application's arithmetic, including where it is wrong.** The cost columns
   reproduce `WorkOrderRepository.findTotalWOCosts` exactly. A report that disagrees with the
   number on the work-order screen is worse than a slightly wrong one that agrees. Two known
   deviations from ideal, both intentional:
   - labour cost is `hourly_rate * duration / 3600` in **integer** arithmetic, truncating per
     row, because `Labor.getCost()` does the same in Java;
   - `labor.include_to_total_time` and `additional_cost.include_to_total_cost` are ignored,
     because the application's own aggregate ignores them.

   If the Java formula changes, change the view in the same commit.

3. **"Now" is `now() AT TIME ZONE 'UTC'`.** The timestamp columns are
   `timestamp without time zone` holding UTC. Plain `now()` is the server timezone and would
   skew every age and overdue flag.

4. **Derived columns follow the Java semantics, not intuition.** In `rpt_work_order`:
   `real_created_at` is the originating request's creation date when there is one
   (`WorkOrder.getRealCreatedAt`), so lead time counts from when the requester asked;
   `is_compliant` treats "no due date" as compliant (`WorkOrder.isCompliant`); `is_reactive`
   means "not generated by a maintenance plan"; `cycle_time_days` truncates to whole days like
   `TimeUnit.DAYS`.

### Custom fields

Custom fields are EAV: rows in `custom_field`, values in `custom_field_value.value` as `TEXT`,
with one foreign-key column per owning entity type. A static column pivot is therefore
impossible — the column list differs per company and changes at runtime. Both workable shapes
are provided:

- `rpt_custom_field_value` — long form, one row per value, with `value_number` and `value_date`
  guarded by both the declared `field_type` **and** a regex on the value. `field_type` alone is
  not enough: the column is plain `TEXT` and nothing stops an older row from holding free text
  after a field's type was changed.
- `custom_fields jsonb` on `rpt_work_order` and `rpt_asset` — label → value, for the common
  "show me one field next to the work order" case: `custom_fields->>'Kostenstelle'`.

For a real column pivot per company, generate it — the field list is data, so the SQL has to be
too:

```sql
SELECT 'SELECT entity_id, ' || string_agg(
         format('MAX(value_text) FILTER (WHERE label = %L) AS %I', label, label), ', ')
       || ' FROM rpt_custom_field_value WHERE entity_type = ''WORK_ORDER''
            AND company_id = 1 GROUP BY entity_id'
FROM (SELECT DISTINCT label FROM rpt_custom_field_value
      WHERE entity_type = 'WORK_ORDER' AND company_id = 1) labels;
```

### Changing a view

`CREATE OR REPLACE VIEW` cannot reorder or retype existing columns, only append. A changed
column list needs a `DROP` first, which is why each view sits in its own changeSet with a
`DROP` rollback. Views that depend on each other (`rpt_asset` and `rpt_cost_line` both read
`rpt_work_order`) must be dropped in reverse dependency order.

Two Postgres details that have already cost a broken changeset:

- **`VALUES` is a fully reserved word** and cannot be a column label even after `AS`. The
  custom-field aggregates are called `field_values` for exactly this reason.
- **Liquibase runs before Hibernate at startup**, so a view that fails to create stops the API
  from booting. That is intentional — a silently missing reporting view is worse than a visibly
  failed deploy, and `IMAGE_TAG` gives a rollback. But it means changed view SQL is worth trying
  against a restored backup first:

  ```bash
  # on the server, against a throwaway copy — never the live database
  createdb -U cmms_admin rpt_check && psql -U cmms_admin -d rpt_check -f /root/atlas_backup_<stamp>.sql
  # then paste the CDATA block of the changeset in question
  psql -U cmms_admin -d rpt_check
  ```

## 4. Stage 1 — saved views

Backend: [`SavedView`](../api/src/main/java/com/grash/model/SavedView.java),
[`SavedViewController`](../api/src/main/java/com/grash/controller/SavedViewController.java),
table in
[`2026_08_04_00000000002_saved_view.xml`](../api/src/main/resources/db/changelog/2026_08_04_00000000002_saved_view.xml).
Frontend: [`SavedViews`](../frontend/src/content/own/components/SavedViews/index.tsx),
[`slices/savedView.ts`](../frontend/src/slices/savedView.ts), wired into the work-order and
asset lists.

A saved view is a name plus two opaque JSON payloads: the `SearchCriteria` and the table
layout. `REST /saved-views?entityType=WORK_ORDER` returns the user's own views plus everything
shared in the company.

Decisions worth keeping:

- **The payloads stay opaque to the backend.** Modelling filter fields and columns as rows
  would turn every new filter widget into a schema migration, and buys nothing: the only
  consumer of a view is the list page that produced it. `TEXT` holding JSON, not `jsonb`,
  because nothing queries inside it; migrating later is one `ALTER ... USING`.
- **Stored criteria are not a security boundary.** They are re-scoped through the owning
  service's `getSearchCriteria(user, ...)` on every use, exactly like criteria arriving from a
  client, so a hand-edited view cannot widen access.
- **`owner_id` is a real foreign key, not the inherited `created_by`.** `created_by` comes from
  Spring Data auditing, is not populated on every path, and is an audit trail rather than an
  authorisation subject. Ownership decides who may edit, so it gets its own column. Company
  owners may also edit, so a shared view does not become unmaintainable when its author leaves.
- **`editable` is resolved server-side** and returned in the DTO. The rule lives in
  `SavedView.canBeEditedBy`; duplicating it in TypeScript is how the two drift apart.
- **`PATCH` means partial here**, unlike the other patch mappers in the project: renaming is the
  common case and the client has no reason to resend criteria and layout for it. That is what
  `NullValuePropertyMappingStrategy.IGNORE` on `SavedViewMapper.updateSavedView` is for.
- **A view can go stale.** Rename a filterable field and views referencing the old name will
  error out of the search endpoint rather than return silently wrong rows. That is the failure
  mode we want; there is no migration for it.

`pageIndex` is deliberately not restored — the rows are about to change, and page 4 of the old
result set is likely empty.

## 5. Stage 1 — filtered export

`POST /export/work-orders?uuid=…` and `POST /export/assets?uuid=…` with an
[`ExportRequestDTO`](../api/src/main/java/com/grash/dto/ExportRequestDTO.java) body:
`{criteria, columns}`. Both fields optional; an empty body reproduces the old behaviour, which
is why the `GET` variants still exist and still serve the "export everything" menu entries.

The websocket handshake is unchanged: the client generates a uuid, subscribes to
`/exports/<uuid>`, calls the endpoint, and receives a signed URL when the file lands in MinIO.

### The column registry

[`CsvColumnRegistries`](../api/src/main/java/com/grash/utils/csv/CsvColumnRegistries.java)
holds, per entity, an ordered map of `key → (translated header, extractor)`. Its **keys are
API**: they are stored inside saved views and sent by the client, so renaming one breaks every
view that references it.

- The first block of each registry is byte-for-byte the column list the unfiltered export
  always produced. `writeWorkOrdersToCsv` and `writeAssetsToCsv` now delegate to it, so the two
  export paths cannot drift apart.
- **`add` vs `addOptional` is the load-bearing distinction.** `add` puts a column in the default
  set — the file a caller gets when it names no columns. `addOptional` makes it selectable by
  name only. Without that split the refactor silently widened the unfiltered work-order export
  from 20 columns to 29, which
  [`CsvColumnRegistriesTest`](../api/src/test/java/com/grash/utils/csv/CsvColumnRegistriesTest.java)
  now guards against for both entities. That test is deliberately mock-free, so it also runs on
  JDKs the project's Byte Buddy cannot instrument — which is every JDK above 22, including the
  one on the development machine.
- An unknown key is **rejected**, not skipped. A file that looks complete and quietly lacks a
  column is the worst outcome for a report someone forwards.
- The frontend derives its selection from the visible columns in their arranged order, filtering
  only `NON_EXPORTABLE_COLUMNS` (controls and images). Every remaining column id has a
  counterpart in the registry — `locationAddress`, `daysSinceCreated`, `files` and
  `requestedBy` exist there purely because the work-order list shows them.

### Paging, and why the sort carries a tiebreaker

`findForExport` appends `id ASC` to the requested sort. Paging over a non-unique sort column —
and every column a user sorts by is non-unique — lets Postgres reorder equal keys between
queries, so an export can repeat one row and lose another. Interactive lists get away with it
because nobody notices a row moving between page 3 and page 4.

### Known limitation, inherited

A filter using the many-to-many `inm` operator returns one row per matching association, so an
entity matching several selected values appears several times — in the list's `totalElements`
today, and in the exported file now. The join is documented in `WrapperSpecification`;
`query.distinct(true)` is **not** the fix (it breaks every page that sorts by a joined column).
The safe fix is an IN-subquery on the root id and it changes shared search behaviour, so it
wants its own change.

### Access control, and a finding about `/search`

The `POST` export endpoints check the plain **view** permission and then rely on
`getSearchCriteria` to narrow to the user's own records when they lack view-other. The `GET`
variants keep requiring view-other because they ignore filters and dump the company.

Two reasons the explicit check in the controller is load-bearing and must not be removed:

- `getSearchCriteria` only ever *narrows*. A user with **no** work-order view permission gets no
  narrowing from it at all. This is also why **`POST /work-orders/search` is more permissive
  than it looks** — it applies the company filter and then, lacking the view permission, adds
  nothing else, returning the whole company's work orders. That is pre-existing upstream
  behaviour, not something the export introduced; it is recorded here because it is the kind of
  thing that gets found twice. Assets do gate this (`AssetService.getSearchCriteria` throws).
- The export runs on an `@Async` thread with no `SecurityContext`, so
  `CompanyAudit.afterLoad` returns early and provides no second line of defence.

`AssetService.getSearchCriteria` was extracted out of `AssetController.search` for this work, so
the list and the export enforce one rule rather than two copies of it.

## 6. Stage 2 and 3

**Stage 2 — a generic aggregation endpoint.** `POST /reports/aggregate` taking
`{entity, filter: SearchCriteria, groupBy[], measures[]}`, implemented over the Criteria API
with `multiselect` returning `Tuple`. This is what would make the charts filterable and let the
42 endpoints be retired incrementally. Realistically 2–3 weeks including the part that matters:
a field whitelist, because `resolveFieldPath` walks arbitrary property paths and an
un-whitelisted `groupBy` is an information-disclosure primitive. Start it when someone asks the
same aggregated question a third time and stage 0 plus a spreadsheet is not enough.

**Stage 3 — a self-service report builder in the app.** Rejected. It is a Mini-Metabase: weeks
to months, and afterwards there are two BI systems to operate. Only revisit if reporting becomes
a product feature that is sold, rather than a tool that is used.

## 7. The Metabase option (evaluated, deferred)

Not adopted for now. Recorded so the evaluation is not repeated:

- **Embedding does not deliver self-service.** Metabase OSS offers only *static embedding*:
  signed JWT, locked parameters, read-only dashboard in an iframe. No query builder, no ad-hoc
  questions. *Interactive embedding* — self-service inside our frontend with SSO and data
  sandboxing — is a paid tier. The pragmatic shape is therefore: self-service happens **in**
  Metabase with its own login, and the app links to, or statically embeds, a handful of curated
  dashboards.
- **Point it at the `rpt_*` views, not the tables**, with its own read-only role. That keeps
  ordinal enums, EAV and Envers noise out of the way by construction. Envers, incidentally,
  audits only `WorkOrder` (`WorkOrderAud` + `RevInfo`) — there is no history to report on for
  any other entity.
- **Operational caveats:** Postgres publishes no host port (`expose` only, Coolify proxies), so
  Metabase must join the same Docker network — across Coolify resources that needs an explicitly
  shared network, not just a compose entry. And a careless full scan hits the same Postgres
  instance as everything else on that host.

Grant for that role, when the time comes:

```sql
CREATE ROLE bi_readonly LOGIN PASSWORD '…';
GRANT CONNECT ON DATABASE atlas TO bi_readonly;
GRANT USAGE ON SCHEMA public TO bi_readonly;
GRANT SELECT ON rpt_work_order, rpt_work_order_cost, rpt_asset,
                rpt_cost_line, rpt_custom_field_value TO bi_readonly;
```

## 8. Open items

- Only work orders and assets have a column registry. The other five exports (locations, parts,
  meters, preventive maintenances, part transactions) still use the hand-written writers in
  `CsvFileGenerator` and ignore filters. Adding one is a registry, not a redesign.
- `daysSinceCreated` reproduces the list column's off-by-one (`dayDiff` subtracts 1, so a work
  order created today reads −1). Worth fixing, but in both places at once.
- The asset downtime column is one query per row, inherited. It belongs with a wider cost
  refactor.
- No XLSX or PDF export exists anywhere; CSV only.
- Saved views have no per-page default: a user's most-used view still has to be picked after a
  reload.
