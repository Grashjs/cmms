# German terminology (partly migrated)

Status: navigation and the labels next to it are migrated (2026-08-04). The remaining ~155
translation keys are not, on purpose. This file exists so that finishing the job is a
half-day of careful work rather than a rediscovery.

## Why the terms changed

The upstream German translation is a machine translation of a US CMMS vocabulary. Several
terms are wrong for a German facility-management audience — "Arbeitsauftrag" is a literal
rendering of "work order" that nobody in FM says, "Anfrage" suggests a sales enquiry rather
than a fault report, and "Vermögenswert" (still present in the export headers) is accounting
language for what FM calls an Anlage.

## What is migrated

`frontend/src/i18n/translations/de.ts`, eight keys:

| Key | Was | Is |
|---|---|---|
| `work_orders` | Arbeitsaufträge | Aufträge |
| `work_order` | Arbeitsauftrag | Auftrag |
| `requests` | Anfragen | Meldungen |
| `request` | Anfrage | Meldung |
| `preventive_maintenance` | Präventive Wartung | Wartung |
| `parts_and_inventory` | Teile/Inventar | Teile & Inventar |
| `files` | Dateien | Dokumente |
| `file` | Datei | Dokument |
| `vendors_customers`, `Vendors_Customers` | Lieferanten & Auftragnehmer | Partner |
| `people_teams` | Personen & Teams | Personen |
| `parts_and_inventory`, `Parts_and_Inventory`, `parts_inventory` | Teile & Inventar / Teileinventar | Material |

These carry the whole navigation, every page title, the add-button labels and the chart series
names, because those all resolve the same keys. That is why the visible result looks more
complete than the key count suggests.

The last three replaced two-word labels with one word, so that no sidebar entry wraps to a
second line. Three things that came out of it and apply to any further rename:

- **Check for a second key.** Sidebar, page header and window title do not always read the same
  key: Partner needed `vendors_customers` *and* `Vendors_Customers`, Material needed three
  (`parts_inventory` labels the section in Settings → Features). Nothing enforces that they
  agree, so a rename that changes only one leaves the app contradicting itself.
- **The umbrella renames, the tabs do not.** Inside Partner the tabs stay `vendors:
  'Lieferanten'` and `customers: 'Auftragnehmer'`; inside Personen they stay Personen and Teams;
  inside Material they stay Teile and Teilesätze. Those are the distinctions the pages exist to
  make.
- **"Mitarbeiter" was rejected for `people_teams`.** The list also holds `REQUESTER` and
  `VIEW_ONLY` accounts, which the app's own copy describes as "typischerweise Lieferanten und
  Auftragnehmer" — external people who are precisely not employees, and who now live under
  Partner. "Personen" is the truthful superset.

Also renamed for consistency, being the same sections named in prose:
`no_access_people_team`, `no_access_vendors_customers`, `parts_inventory_settings_description`.

**English was deliberately left alone.** The target names were given in German, and the
obvious English counterparts are worse than what is there: "Aufträge" → "Orders" collides with
"Purchase Orders", and "Meldungen" → "Reports" collides with the reporting feature. Changing
`en.ts` needs its own decision about the English vocabulary, not a translation of the German
one.

## What is left

155 keys in `de.ts` still carry an old term: 85 with *Arbeitsauftrag*, 49 with *Anfrage*, 21
with *Datei*. They split into three groups, and only the first is a find-and-replace:

**64 keys — plain substitution.** The term stands alone or at a word boundary.
`request_delete_success: 'Die Anfrage wurde erfolgreich gelöscht'` → `Die Meldung wurde …`.
Watch the article: *die* Anfrage and *die* Meldung agree, but *der* Arbeitsauftrag becomes
*der* Auftrag (fine) and *die* Datei becomes *das* Dokument (**not** fine — every `die Datei`
needs `das Dokument`, and `eine Datei` needs `ein Dokument`).

**50 keys — compounds.** These are the ones a blanket replace mangles:

```
required_wo:           'Das Arbeitsauftragsfeld ist erforderlich.'   → Auftragsfeld
required_wo_title:     'Arbeitsauftrags-Titel ist erforderlich'      → Auftragstitel
wo_details:            'Details des Arbeitsauftrags'                 → des Auftrags
required_request_name: 'Anfragetitel ist erforderlich'               → Meldungstitel
request_details:       'Anfragedetails'                              → Meldungsdetails
no_file_in_location:   'Keine Dateien an diesem Standort angehängt'  → Keine Dokumente
```

Note the linking-s changes with the word: *Arbeitsauftrag**s**titel* stays
*Auftrag**s**titel*, but *Anfrage**titel*** becomes *Meldung**s**titel*. A regex will get this
wrong.

**41 keys — sentences and marketing copy**, e.g. `custom_dashboards`,
`include_cost_description`, the onboarding texts. Long enough that the sentence should be
reread as a whole rather than patched; several are awkward German independent of the
terminology.

## How to do it

1. Work in `de.ts` only. `frontend/src/i18n/translations/AGENTS.md` is the rule that applies:
   translate values, never touch keys, and do not copy the German decision into the other
   sixteen locale files.
2. Go key by key in the three groups above, in that order. The plain group can be done fast;
   budget the time for the compounds.
3. Do **not** rename keys to match. `wo_details` keeping its name while its value says
   "Auftrag" is correct — the key is an identifier, and renaming it touches every call site for
   no benefit.
4. Verify with `CI=true npm run build` in `frontend` (~3 min). It will not catch wording, but
   it catches a broken string literal, which is the realistic failure mode of a bulk edit.
5. Read the result in the running app, not in the diff. Truncated labels and wrong articles
   only show up rendered.

## Adjacent inconsistency, same theme

The CSV export headers come from a different place — `api/src/main/resources/messages_de_DE.properties`
— and still say `Asset_Name=Vermögenswertname` and `Parent_Asset=Übergeordneter Vermögenswert`.
That file uses `\uXXXX` escapes for umlauts by convention; keep to it. Roughly ten keys, and
they are all standalone column headers, so this part genuinely is a quick pass.
