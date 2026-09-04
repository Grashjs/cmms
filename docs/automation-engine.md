# Automatisierungs-Engine: Stand und Erweiterung

Arbeitsdokument. Es beschreibt, **was heute läuft**, **wie ein weiterer Auslöser entsteht** und
**welche Kandidaten es gibt** — geordnet nach Nutzen, mit den Fallen, die im Code stehen.

Die Begründungen und Entscheidungen stehen weiterhin in
[`workflow-engine-konzept.md`](workflow-engine-konzept.md); dieses Dokument wiederholt sie nicht.
Grobe Arbeitsteilung: das Konzept sagt *warum es so gebaut ist* und bleibt stabil, dieses hier
sagt *was gerade wahr ist* und ändert sich mit jedem neuen Auslöser.

Stand: 2026-09-04, Commit `222aad38`.

---

## 1. Ist-Stand

### 1.1 Was gebaut ist

| Schicht | Dateien | Zustand |
|---|---|---|
| Ereignis | `automation/event/EntityChangedEvent`, `AutomationListener`, `CurrentActor` | Läuft. `@TransactionalEventListener(AFTER_COMMIT)` + `@Async` auf eigenem Executor |
| Regelmodell | `automation/model/AutomationRule`, `AutomationCondition`, `AutomationActionStep`, `AutomationRun` | Läuft. Vier Tabellen, Liquibase, Enums als `VARCHAR` |
| Auswertung | `automation/eval/RuleEvaluator` + `AssetResolver`, `CustomFieldResolver` | Läuft, **nur für Anlagen** |
| Aktionen | `automation/action/CreateWorkOrderHandler`, `NotifyHandler`, `SetCustomFieldHandler` | Läuft |
| Ausführung | `AutomationEngine`, `AutomationRuleRunner`, `AutomationRunService` | Läuft. Je Regel eigene Transaktion, Lauf-Protokoll in noch einer |
| Metadaten | `AutomationMetaService`, `GET /automation-rules/meta` | Läuft |
| API | `AutomationRuleController` | CRUD, `enabled`, `runs`, `meta` — alles company-scoped |
| GUI | `frontend/.../Settings/Features/Automation/**` | Läuft, `/app/settings/features/automation` |
| Schalter | `AUTOMATION_ENABLED` (Standard `false`), `AUTOMATION_MAX_DEPTH` (3) | Auf dieser Instanz beide gesetzt |

44 Tests im Paket `com.grash.automation`, 1445 in der Suite. Ein echter Ende-zu-Ende-Test über
`AFTER_COMMIT` fehlt weiterhin (bräuchte Testcontainers, also Docker).

### 1.2 Die einzige lebende Publikationsstelle

```
AssetService.dispatchAssetStatusChangeWebhook   (AssetService.java:775)
  → EntityChangedEvent.root(UPDATED, ASSET, id, companyId, {"status"}, actor)
```

Das ist **eine von 36** möglichen Trigger-Kombinationen (6 Entitätsarten × 6 Änderungsarten).
Die anderen 35 sind im Editor sichtbar und als *noch nicht verfügbar* ausgegraut, weil eine
Regel darauf sauber speichern und nie feuern würde.

Drei Stellen wissen davon, und alle drei müssen mitwachsen:

1. `AutomationRuleRunner.loadTriggerEntity` — `switch` über `EntityType`, `default` wirft
   `501 NOT_IMPLEMENTED`. Bewusst laut: eine Regel auf einem unverdrahteten Trigger soll als
   FAILED im Protokoll stehen, nicht stumm nichts tun.
2. `AutomationMetaService.LIVE_TRIGGERS` — die eine handgepflegte Liste. Fehlt der Eintrag,
   bleibt der Trigger im Editor ausgegraut, obwohl er funktionieren würde.
3. Der Resolver für die Felder der neuen Entitätsart — ohne ihn hat die Regel keine Bedingungen
   außer denen, die es gar nicht gibt.

### 1.3 Die Anlagen-Schieflage in den Aktionen

Das ist der Teil, der beim ersten weiteren Trigger sofort wehtut, und er steht nicht im Konzept:
**die Aktionen sind anlagenzentriert.**

- `ActionParameters.PLACEHOLDERS` kennt genau vier Namen: `trigger.id`, `trigger.asset.id`,
  `trigger.asset.name`, `trigger.asset.status`. Bei einem Auftrags-Trigger löst
  `${trigger.asset.…}` eine Ausnahme aus („This rule was not triggered by an asset"), und
  `${trigger.id}` ist dann die Auftrags-ID — richtig, aber die einzige verfügbare Referenz.
- `SetCustomFieldHandler` schreibt **nur** auf Anlagen und sagt das auch (`SET_CUSTOM_FIELD only
  applies to assets`).
- `CustomFieldResolver` liest nur Anlagen-Merkmale (`CustomFieldEntityType.ASSET`), obwohl es
  Merkmale auch für Aufträge, Standorte, Teile, Zähler, Lieferanten und Kunden gibt.
  Für Meldungen übrigens nicht — `CustomFieldEntityType` hat kein `REQUEST`, nur
  `PURCHASE_REQUEST` (Bestellanforderung). Eine Meldungsregel kann also keine Merkmale lesen.

Ein Auftrags-Trigger ist also erst brauchbar, wenn mindestens die Platzhalter mitwachsen. Das
gehört in denselben Arbeitsgang, nicht in einen späteren — sonst entsteht genau die Sorte
halb verdrahteter Fähigkeit, die dieses Projekt gerade abgelöst hat.

### 1.4 Was offen bleibt

- **Akteur:** `createdBy` bleibt bei allem, was die Engine anlegt, leer — im Async-Thread gibt
  es keinen Security-Context, und ein technischer Benutzer ist derzeit nicht anlegbar
  (Konzept §9.1).
- **Alt-Engine:** läuft unverändert weiter und wird an denselben Stellen weiter aufgerufen
  (`RequestService` bei Genehmigung/Ablehnung, `WorkOrderService` beim Abschluss). Koexistenz
  ist beabsichtigt; ein neuer Trigger *ersetzt* keinen Alt-Aufruf, er tritt daneben.
- **Kein Zeit-Auslöser.** Alles hier ist reaktiv. „Nach 3 Tagen ohne Reaktion" ist Stufe 2 im
  Konzept (§10) und braucht Quartz, nicht einen weiteren Trigger.

---

## 2. Rezept: ein neuer Auslöser

Sieben Schritte. Die Reihenfolge ist nicht beliebig — nach Schritt 3 ist der Trigger im Editor
wählbar, und wer dort aufhört, hat eine Regel gebaut, die läuft und nichts lesen kann.

### Schritt 1 — Publikationsstelle

In den Dienst, der die Änderung durchführt, **innerhalb** der Transaktion:

```java
eventPublisher.publishEvent(EntityChangedEvent.root(
        ChangeType.UPDATED, EntityType.WORK_ORDER, workOrder.getId(),
        company.getId(), changedFieldNames, CurrentActor.userIdOrNull()));
```

Vier Dinge, die dabei schiefgehen:

- **Company darf nicht null sein.** Der Engine-Einstieg verwirft ein Ereignis ohne Company mit
  einer Log-Warnung, weil er sonst firmenübergreifend Regeln suchen würde.
- **Publizieren, nicht selbst asynchron werden.** Der Listener ist `AFTER_COMMIT`; publiziert
  wird mitten in der Transaktion. Wer selbst `@Async` davorsetzt, verliert die Ordnung.
- **Der Akteur wird jetzt gelesen**, nicht später — `CurrentActor.userIdOrNull()` greift auf den
  Security-Context zu, den es im Listener-Thread nicht mehr gibt.
- **Folgeereignisse sind Kinder.** Löst eine Aktion selbst eine Änderung aus (Genehmigung legt
  einen Auftrag an), dann `event.child(...)` statt `root(...)`: nur so bleiben `correlationId`
  und `depth` erhalten, und nur so können Kaskadenschutz und „lief in dieser Kaskade schon"
  überhaupt greifen.

Am besten direkt neben den bestehenden `webhookDispatchService.dispatchWebhook(...)`-Aufruf —
das ist genau die Stelle, an der der Code schon sagt „hier ist etwas Bemerkenswertes passiert",
und Company und Feld-Diff liegen dort bereits vor.

### Schritt 2 — Feldnamen festlegen

`changedFields` ist ein `Set<String>` und wird an zwei Stellen als Text verglichen: im
Feldfilter der Regel und in `CHANGED_TO`. Konvention: **der Feldname der Entität in camelCase**,
so wie das Java-Feld heißt (`status`, `dueDate`, `priority`, `assignedTo`). Wer aus einem
vorhandenen `WOField.DUE_DATE` publiziert, muss also abbilden, nicht `name()` nehmen — sonst
steht im Editor `DUE_DATE` und in der Anlagenregel daneben `status`.

Nur Felder aufnehmen, die der Diff wirklich erkennt. Ein angebotenes Feld, das der Diff nie
meldet, ist eine Bedingung, die nie zutrifft.

### Schritt 3 — `loadTriggerEntity` und `LIVE_TRIGGERS`

`AutomationRuleRunner.loadTriggerEntity`: ein `case` mehr, frisch per Id geladen (nicht aus dem
Ereignis — es trägt bewusst keine Entität, weil die nach dem Commit veraltet sein könnte).

`AutomationMetaService.LIVE_TRIGGERS`: ein Eintrag mit denselben `changedFields` wie in
Schritt 2. Ab hier ist der Trigger im Editor wählbar.

### Schritt 4 — Resolver für die neue Entitätsart

Eine Klasse, `OperandResolver` implementieren, `@Component`:

- `supports(subject)` — welche Pfade, Konvention `workOrder.status`, `workOrder.priority`, …
- `describe(company)` — was der Editor anbietet: `valueType`, Operatoren, bei Enums die Werte.
  `CHANGED_TO` **nur** für Felder, die der Diff aus Schritt 2 meldet.
- `resolve(condition, context)` — der Wert, `null` wenn keiner da ist. `context.cached(...)`
  benutzen; `enable_lazy_load_no_trans` ist an, jede unaufgelöste Assoziation kostet sonst eine
  eigene Abfrage pro Bedingung.

Dazu die i18n-Schlüssel. `OperandDescriptor.native_` leitet sie aus dem Pfad ab:
`workOrder.status` → `automation_subject_workOrder_status`. Nur `en.ts` und `de.ts`, wie in
[`i18n/translations/AGENTS.md`](../frontend/src/i18n/translations/AGENTS.md) beschrieben. Fehlt
ein Schlüssel, steht der rohe Schlüsselname im Editor — sichtbar, aber hässlich.

### Schritt 5 — Platzhalter

`ActionParameters.PLACEHOLDERS` um `trigger.workOrder.id` usw. erweitern. Die Liste ist bewusst
geschlossen: ein unbekannter Platzhalter ist ein Fehler, keine leere Zeichenkette. Sie wird
außerdem im Metadaten-Dokument mitgeliefert, erscheint also automatisch als Hinweis unter jedem
Textfeld, das Platzhalter tragen darf.

Der Zugriff muss den Fall „falsche Entitätsart" beantworten. Vorbild ist `ActionParameters.asset`:
es wirft mit klarer Meldung, statt `null` einzusetzen.

### Schritt 6 — Reichweite der Aktionen prüfen

Für jede der drei Aktionen entscheiden, ob sie mit dem neuen Trigger sinnvoll ist, und *im Code*
sicherstellen, dass die Antwort sichtbar ist:

- `CREATE_WORK_ORDER` — funktioniert überall, aber der Parameter `asset` bietet
  `${trigger.asset.id}` an. Bei einem Auftrags-Trigger gehört dort ein anderer Vorschlag hin
  (oder keiner).
- `NOTIFY` — funktioniert überall; `notificationType` hat für jede `EntityType` einen Zweig.
- `SET_CUSTOM_FIELD` — nur Anlagen. Entweder auf die neue Art erweitern oder die klare
  Fehlermeldung stehen lassen.

### Schritt 7 — Tests

- Resolver-Test wie `RuleEvaluatorTest`: keine Spring-Kontext, Stub-Resolver, Vergleichslogik
  als Funktion. Ein `@SpringBootTest` ist in Unit-Tests verboten
  ([`api/src/test/java/com/grash/agents.md`](../api/src/test/java/com/grash/agents.md)).
- `AutomationMetaServiceTest.distinguishWiredFromUnwired` zählt die lebenden Trigger. Die Zahl
  **muss** mitwachsen — das ist die beabsichtigte Kopplung, nicht eine Unbequemlichkeit.
- Der Dienst-Test des publizierenden Dienstes braucht `@Mock ApplicationEventPublisher`, sonst
  NPE (so schon bei `AssetServiceTest` und `RequestServiceTest` passiert).

### Checkliste

```
[ ] publishEvent an der Änderungsstelle, innerhalb der Transaktion, Company gesetzt
[ ] Folgeereignisse per event.child(...)
[ ] changedFields in camelCase, nur wirklich erkannte Felder
[ ] case in AutomationRuleRunner.loadTriggerEntity
[ ] Eintrag in AutomationMetaService.LIVE_TRIGGERS
[ ] Resolver mit supports/describe/resolve, CHANGED_TO nur für Diff-Felder
[ ] i18n-Schlüssel in en.ts und de.ts
[ ] Platzhalter in ActionParameters.PLACEHOLDERS
[ ] Reichweite der drei Handler geprüft
[ ] Tests: Resolver, LIVE_TRIGGERS-Zähler, @Mock ApplicationEventPublisher
[ ] CLAUDE.md-Divergenztabelle: geänderte Upstream-Datei ergänzt
```

---

## 3. Inventar der Kandidaten

Die natürliche Landkarte liegt schon vor: **jede `dispatchWebhook`-Stelle ist eine potenzielle
Publikationsstelle.** Dort ist die Änderung fertig, die Company bekannt und der Feld-Diff oft
schon berechnet. Dieselbe Liste ist in der App unter *Integrationen → Webhooks* sichtbar — ein
brauchbares Werkzeug, um Use Cases zu sammeln, bevor eine Zeile Code entsteht.

| # | Stelle | Trigger | Diff vorhanden? | Aufwand | Anmerkung |
|---|---|---|---|---|---|
| 1 | `AssetService:789` `ASSET_STATUS_CHANGE` | ASSET / UPDATED | `{status}` | **erledigt** | Die lebende Stelle |
| 2 | `AssetService:111` `NEW_ASSET` | ASSET / CREATED | — | XS | Resolver existiert schon. „Neue Anlage → Ersteinweisung anlegen" |
| 3 | `WorkOrderService:140` `NEW_WORK_ORDER` | WORK_ORDER / CREATED | — | S | Braucht `WorkOrderResolver` + Platzhalter |
| 4 | `WorkOrderService:454`+`458` (Statuswechsel) | WORK_ORDER / UPDATED bzw. CLOSED | `detectChangedFieldsFromEntity`, **inkl. STATUS** | S | Der wertvollste Auftrags-Trigger |
| 5 | `WorkOrderService:218` (Patch-Pfad) | WORK_ORDER / UPDATED | `detectPatchDTOChangedFields`, 11 Felder, **ohne** Status | S | Siehe Falle A |
| 6 | `RequestService:385` Genehmigung | REQUEST / APPROVED | — | M | Legt einen Auftrag an → Kindereignis, siehe Falle C |
| 7 | `RequestService:447` Ablehnung | REQUEST / REJECTED | — | S | |
| 8 | `RequestService:117`+`145` | REQUEST / CREATED | — | M | Zwei Pfade, siehe Falle B. Berührt die KI-Triage |
| 9 | `PartService:555` + `PurchaseOrderController:241` | PART / UPDATED | `{quantity}` | M | „Mindestbestand unterschritten" braucht einen Vergleichsoperator (`LT`), den es noch nicht gibt |
| 10 | `PartService:90` / `:124` | PART / CREATED bzw. UPDATED | `PartField` | S | Geringer Nutzen ohne #9 |
| 11 | `ReadingController:190` Zählertrigger | — | — | M | `EntityType` hat kein `METER`. Und der Pfad legt schon selbst einen Auftrag an |
| 12 | `CommentService:72` | WORK_ORDER / UPDATED | — | S | Sinnvoll nur mit einem Feld `workOrder.lastComment`, das es nicht gibt |
| 13 | `LocationService:78` / `VendorService:55` | — | — | S | Braucht neue `EntityType`-Werte. Kein erkennbarer Use Case |
| 14 | `WorkOrderService:283`, `PartService:227` (Löschungen) | — | — | — | **Nicht bauen.** `loadTriggerEntity` lädt frisch per Id; die Zeile ist weg |
| 15 | `PURCHASE_ORDER_*` | PURCHASE_ORDER / APPROVED … | — | L | Upstream `//TODO`, es gibt gar keinen Dispatch. Publikationsstelle komplett neu |
| 16 | `WORK_ORDER_OVERDUE` | — | — | L | Upstream `//TODO` **und** zeitbasiert → Konzept §10 Stufe 2, nicht hier |

`EntityType` und `ChangeType` liegen als `VARCHAR(32)` in der Datenbank
(`@Enumerated(EnumType.STRING)`), nicht als Ordinalzahl wie die Upstream-Enums. Ein neuer Wert
ist also billig und darf auch **zwischen** bestehende Werte — anders als bei allem, was
`2026_01_10_1768015926_enums_type.xml` auf `SMALLINT` umgestellt hat.

### Die vier Fallen im Detail

**Falle A — Aufträge haben zwei Änderungspfade.** `update()` (Patch-DTO, Zeile 218) und
`saveAndFlushWithWebhook()` (Entität, Zeile 454, aufgerufen aus dem Statuswechsel bei Zeile 904).
Wer nur einen publiziert, hat einen Trigger, der bei der Hälfte der Änderungen feuert — das
ist schlimmer als keiner, weil es sich wie ein sporadischer Fehler anfühlt.

Dass `detectPatchDTOChangedFields` kein `STATUS` meldet, ist **kein Defekt**: `WorkOrderPatchDTO`
trägt gar keinen Status, der Wechsel läuft ausschließlich über den eigenen Endpunkt. Der
Patch-Pfad erkennt allerdings auch `archived` nicht (es gibt kein `WOField` dafür), ein
ARCHIVED-Trigger braucht also seinen eigenen Vergleich.

**Falle B — Meldungen entstehen auf zwei Wegen.** `create(request, company)` über die API und
`create(request, company, requestPortal)` über das Meldeportal. Der Portalweg hat **keinen
angemeldeten Benutzer**: `CurrentActor.userIdOrNull()` liefert `null` (korrekt, aber der Akteur
im Protokoll bleibt leer), und die Company kommt aus dem Portal, nicht aus dem Benutzer.

**Falle C — Genehmigung ist eine Kaskade.** `RequestService.approve` legt einen Auftrag an. Wenn
REQUEST/APPROVED und WORK_ORDER/CREATED beide leben, kann eine Regel die andere auslösen. Genau
dafür gibt es `depth`, `correlationId` und `alreadyRanInThisCascade` — aber nur, wenn das
Auftrags-Ereignis mit `event.child(...)` publiziert wird. Mit `root(...)` beginnt eine neue
Kaskade mit Tiefe 0, und der Schutz ist blind.

**Falle D — die Alt-Engine läuft an denselben Stellen.** `RequestService.approve/reject` und der
Auftragsabschluss rufen weiterhin `workflowService.run…`. Beide Engines feuern dann für dasselbe
Geschehen. Das ist beabsichtigt (Konzept §4.1) und beim Testen leicht verwirrend: eine doppelt
angelegte Aufgabe kann aus der Alt-Regel kommen. Das Lauf-Protokoll der neuen Engine zeigt nur
ihre eigenen Läufe.

---

## 4. Empfohlene Reihenfolge

**1. Aufträge (#3, #4, #5) — ein Arbeitsgang.** Größter Nutzen pro Aufwand: der Feld-Diff
existiert, elf Felder plus Status, und Aufträge sind die Entität, um die sich der Betrieb dreht.
Enthält zwingend `WorkOrderResolver`, `trigger.workOrder.*`-Platzhalter und beide
Änderungspfade. Use-Case-Beispiele: „Auftrag auf Hoch → Schichtleitung benachrichtigen",
„Auftrag abgeschlossen an kritischer Anlage → Prüfauftrag anlegen".

**2. Meldungen (#6, #7, #8).** Deckt genau das ab, was die Alt-Engine heute noch tut, und
verbindet sich mit der KI-Triage: die Triage schlägt eine Anlage vor, eine Regel könnte den
Vorschlag verwerten. Hier zuerst entscheiden, ob die Triage ihr eigenes `RequestCreatedEvent`
behält oder auf `EntityChangedEvent` umzieht — zwei Ereignismechanismen für dasselbe Geschehen
sind eine Altlast in Zeitlupe.

**3. Vergleichsoperatoren, dann Teile (#9).** `LT` / `GT` / `LTE` / `GTE` im `RuleEvaluator`
plus `NUMBER`-Behandlung. Erst danach ist „Mindestbestand unterschritten" ausdrückbar; ohne die
Operatoren wäre der Trigger da und die Bedingung nicht formulierbar.

**4. Danach Konzept §10 Stufe 2 (Zeit als Auslöser).** Ab hier bringt ein weiterer reaktiver
Trigger weniger als die erste Zeitregel: „nichts passiert" ist der Auslöser, der in der Praxis
am häufigsten gebraucht wird, und kein Ereignis kann ihn liefern.

Bewusst **nicht** empfohlen: #11 bis #16. Zählertrigger und Bestellungen sind teuer oder
upstream unfertig, Standorte und Lieferanten haben keinen Use Case, Löschungen sind technisch
nicht sinnvoll.

---

## 5. Use Cases erheben, bevor gebaut wird

Der Reihenfolge oben liegt eine Annahme über den Nutzen zugrunde, und die gehört geprüft. Vier
Fragen je Kandidat, in dieser Reihenfolge:

1. **Was löst aus?** Muss eine der Stellen aus dem Inventar sein — oder es ist eine Zeitregel
   und damit Stufe 2.
2. **Woran erkennt man den Fall?** Nur aus Feldern, die ein Resolver lesen kann oder die als
   Merkmal existieren. Wenn die Antwort „das weiß nur der Meister" ist, fehlt ein Datenfeld,
   und *das* ist die eigentliche Aufgabe.
3. **Was soll passieren?** Auf die drei Aktionen abbilden. Was nicht abbildbar ist, ist ein
   Handler — und damit eine bewusste Erweiterung, kein Nebenprodukt.
4. **Wer merkt es, wenn es ausbleibt?** Ohne Antwort ist die Regel Dekoration. Mit Antwort steht
   damit auch fest, wer die Regel bekommt und wer ins Lauf-Protokoll schaut.

Zwei Werkzeuge, die dabei helfen und schon da sind:

- **Die Webhook-Liste in der App** (*Integrationen → Webhooks*) ist eine vollständige,
  gelesene Liste dessen, was das System überhaupt als Ereignis kennt.
- **Das Lauf-Protokoll** (*Automatisierungsregeln → Läufe*) beantwortet „ist mein Ereignis
  überhaupt angekommen?". Für einen neu verdrahteten Trigger: eine Regel ohne Bedingung mit
  `NOTIFY` anlegen, Änderung auslösen, Protokoll ansehen. Kein Eintrag heißt: die
  Publikationsstelle wurde nicht durchlaufen. Eintrag mit `SKIPPED` heißt: Ereignis da,
  Bedingung nicht erfüllt. Das trennt die beiden Fehlerbilder, die sich sonst gleich anfühlen.
