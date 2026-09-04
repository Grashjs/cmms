# Regel-Automatisierung: die Workflow-Engine erweitern

Status: Entwurf (überarbeitet nach Code-Review)
Datum: 2026-09-03
Bezogener Code: `api/src/main/java/com/grash/{model,service,controller}/Workflow*`,
`model/enums/workflow/`, `service/AssetService`, `service/WebhookDispatchService`,
`event/RequestTriageListener`

---

> **Ist-Stand und nächste Schritte stehen nicht hier.** Dieses Dokument hält die Entscheidungen
> fest und soll stabil bleiben. Was heute tatsächlich verdrahtet ist, wie ein weiterer Auslöser
> entsteht und welche Kandidaten es gibt: [`automation-engine.md`](automation-engine.md).

## 1. Ziel und Nicht-Ziele

### Ziele

1. **Regeln über Anlagen und ihre Merkmale** — nicht nur über Workorder/Meldung/Bestellung/Teil/
   Aufgabe. Der Leit-Use-Case (§3) hängt an der Kombination aus einem statischen Merkmal
   („Anlagenklasse") und einem dynamischen Zustand.
2. **Bedingungen als Daten, nicht als Code** — eine neue Bedingungsquelle darf keine Änderung an
   Engine-Code und keine Pflege von Enum-Spiegeln in Frontend, Home und Mobile erfordern.
3. **Mehrere, parametrisierbare Aktionen pro Regel** — inklusive der heute nur als `//TODO`
   existierenden (Workorder/Meldung/Bestellung anlegen, E-Mail/Notification senden).
4. **Laufzeitwerte in Aktionen** — „neue Workorder für *diese* Anlage" statt nur für eine bei
   Konfiguration fixierte Referenz. Das ist der Grund, warum die KI-Triage die Engine heute
   umgeht (siehe [`ki-meldungs-triage.md`](ki-meldungs-triage.md)).
5. **Robustheit und Nachvollziehbarkeit** — eine fehlschlagende Regel darf den auslösenden
   Use-Case nie mitreißen; jeder Lauf ist protokolliert, auch der, der *nicht* gefeuert hat.

### Nicht-Ziele (bewusst)

- **Kein BPMN-/Prozess-Orchestrator** (Camunda/Flowable, wartende Human-Tasks, mehrstufige
  Genehmigungsketten). Der Bedarf ist ereignisbasierte Regel-Automatisierung (Event–Condition–
  Action). Echte Approval-Workflows mit Wartezuständen sind ein separater Ausbau — §10 beschreibt
  die vier Stufen dorthin, was jede kostet, und warum keine davon verlangt, diese hier neu zu
  bauen.
- **Keine Endanwender-Scripting-Engine** (SpEL, Groovy, JS). Strukturierte Bedingungen
  (Subjekt/Operator/Wert) reichen und bleiben validierbar.
- **Kein Ersatz der KI-Triage.** Sie bleibt eigenständig; eine Anbindung ist optional und später.
- **Kein Ersatz der bestehenden Engine.** Das ist die zentrale Korrektur gegenüber dem ersten
  Entwurf und in §7/A1 begründet: die neue Engine entsteht **daneben**, nicht darüber. Die
  Alt-Engine, ihre UI und ihre TS-Spiegel bleiben unangetastet.

---

## 2. Ausgangslage

### 2.1 Was die Alt-Engine ist

Ein „**Trigger + AND-Bedingungen + genau eine Aktion**"-Regelwerk:

| Baustein | Heute | Ort |
|---|---|---|
| Regel | `Workflow` (Titel, Trigger-Enum, Bedingungen, 1 Aktion, `enabled`) | `model/Workflow` |
| Trigger | 10 Enum-Werte (`WFMainCondition`), **synchron inline** an 10 festen Aufrufstellen | `WorkOrderService` (3×), `RequestController` (3×), `PurchaseOrderController` (2×), `TaskController`, `PartController` |
| Bedingungen | Java-`switch` **in der JPA-Entity**, nur Identitäts- und Zeitprüfungen, nur AND | `model/WorkflowCondition.isMetFor*` |
| Aktionen | nur Feldzuweisungen + Bestellung genehmigen/ablehnen; E-Mail, Checkliste, WO/Meldung/Bestellung anlegen sind `//TODO` | `service/WorkflowService.run*` |
| Konfiguration | fest verdrahtete Selects; Enum-Listen **vierfach gepflegt** (Java, `frontend/src/models/owns/workflow.ts`, `home/src/models/owns/workflow.ts`, `mobile/models/workflow.ts`) | `frontend/src/content/own/Settings/Features/Workflows/index.tsx` (1081 Zeilen) |

### 2.2 Defizite

- **D1 — `enabled` ist nicht nur ungeprüft, es ist nicht setzbar.**
  `findByMainConditionAndCompany_Id` filtert nicht darauf, *und* weder `WorkflowPostDTO` noch
  `WorkflowPatchDTO` führen das Feld; das Frontend liest es nur, um nicht editierbare Regeln
  auszugrauen. Einziger Schreiber ist `PaddleService.disableWorkflows` (Lizenz-Downgrade).
  Ein Repository-Filter allein ändert also nichts — es braucht einen Toggle.
- **D2 — synchrone Inline-Ausführung.** Ein Fehler in einer Aktion reißt den ursprünglichen
  Use-Case in derselben Transaktion mit.
- **D3 — keine Anlagen-Ereignisse**, keine Merkmals-Bedingungen, kein Lesen des Anlagenstatus
  als Bedingung.
- **D4 — Aktionen ohne Laufzeitwerte.** Die Aktion trägt nur fixe, konfigurierte Referenzen.
- **D5 — keine Nachvollziehbarkeit.** Es gibt kein Ausführungslog; „warum hat die Regel nicht
  gefeuert?" ist unbeantwortbar.
- **D6 — Erweiterung = Code an 5 Stellen** (Java-Enum + `switch` + 3 TS-Spiegel).
- **D7 — `TITLE_CONTAINS` macht Regeln stumm tot.** Der Wert steht in `WorkOrderCondition` und
  `RequestCondition` und wird im UI angeboten, aber in keinem `switch` behandelt →
  `default: return false` → die **ganze Regel** feuert nie, ohne Fehler und ohne Hinweis. Das
  ist schlimmer als eine `//TODO`-Aktion, die nichts tut.
- **D8 — `WorkflowController.patch` löscht und legt neu an.** Folge: neue Id, `enabled` hart auf
  `true` (Bearbeiten reaktiviert also eine per Lizenz deaktivierte Regel), Lizenz-Zählung
  übersprungen, Audit-Historie weg. `WorkflowService.update` ist toter Code. Für ein Run-Log mit
  `rule_id` ist diese Id-Churn ein Blocker.
- **D9 — `SET_ASSET_STATUS` umgeht die Stillstandserfassung.** Der Zweig setzt
  `asset.setStatus(...)` und schreibt direkt über das Repository — also kein `AssetDowntime`, kein
  `ASSET_STATUS_CHANGE`-Webhook, keine Eltern-Propagation. Sobald eine Regel produktiv Status
  setzt, driften die Verfügbarkeits-KPIs.
- **D10 — `getAll()` ist nicht mandantengetrennt.** Für Rollen ≠ `ROLE_CLIENT` liefert
  `GET /workflows` die Regeln *aller* Companies. Betrifft nur Super-Admins, ist aber beim Bau
  eines neuen Endpunkts nicht zu kopieren.

### 2.3 Bestand, auf dem aufgebaut wird — und die Fallen darin

Vier Dinge existieren schon und sind wertvoller als ein Neubau:

**Das Async-Listener-Muster ist erprobt.** `event/RequestCreatedEvent` +
`event/RequestTriageListener`: `@Async` + `@TransactionalEventListener(AFTER_COMMIT)` + volle
Fehlerisolierung, mit der Begründung im Javadoc. Genau dieses Muster wird verallgemeinert.

**Es gibt bereits eine Ereignis-Oberfläche: den Webhook-Dispatch.**
`WebhookDispatchService.dispatchWebhook` ist `@Async` und wird an **22 Stellen** gerufen, mit 23
Ereignistypen — darunter `ASSET_STATUS_CHANGE`, `WORK_ORDER_STATUS_CHANGE`,
`PART_QUANTITY_CHANGED`, `NEW_ASSET`, `NEW_REQUEST`. Das ist fachlich schon ausgewähltes
Trigger-Vokabular an schon gewählten Stellen. Eine dritte Hook-Menge daneben zu stellen, würde
garantiert auseinanderdriften. Richtung: `EntityChangedEvent` wird die *eine* Quelle, der
Webhook-Dispatch wird ihr zweiter Konsument (siehe §4.2).

**Für Workorder und Meldung liegt der Feld-Diff schon in der Datenbank.** `WorkOrderBase` ist
`@Audited(withModifiedFlag = true)` (Envers) — die `_aud`-Tabellen tragen pro Feld ein
Modified-Flag. Für `Asset` gilt das *nicht*; dort muss der Diff im Code entstehen.

**Mail und In-App-Benachrichtigung sind async und entkoppelt vorhanden.**
`NotificationService.create/createMultiple` (inkl. Push) und der Mail-Versand über
`MailServiceFactory`. `Notification` erbt von `Audit`, nicht von `CompanyAudit` — für sie gilt
die Falle F1 unten also nicht.

Und vier Fallen, die ein Umbau ohne diese Notiz jeweils treffen würde:

- **F1 — im Async-Thread ist keine Company da.** `CompanyAudit.beforePersist()` holt die Company
  aus dem `SecurityContext`; `AsyncConfig` propagiert den Kontext nicht. Im AFTER_COMMIT-Thread
  ist er leer → `company` bleibt null → `@JoinColumn(nullable = false)` → Insert schlägt fehl.
  Betrifft jede Aktion, die etwas anlegt, **und die Run-Log-Zeilen selbst**, wenn sie von
  `CompanyAudit` erben. Der Bestand löst das schon explizit und kommentiert:
  `RequestQualificationService.qualify` setzt `qualification.setCompany(request.getCompany())`
  von Hand. Dasselbe gilt für `createdBy`: `AuditConfig`s `AuditorAware` liest ebenfalls den
  `SecurityContext` und liefert dann `Optional.empty()`.
- **F2 — beim Anlagen-Patch ist der alte Status schon überschrieben.** In `AssetService.patch`
  laufen `triggerDownTime`/`stopDownTime` **vor** `update(...)`, und `triggerDownTime` schreibt
  den neuen Status selbst. Das `previousStatus`, das `update(...)` danach berechnet, ist bereits
  der *neue* Wert; der `ASSET_STATUS_CHANGE`-Webhook feuert deshalb aus `triggerDownTime` und
  nicht aus `update`. Ein naiver Feld-Diff in `update(...)` sieht OPERATIONAL→DOWN **nicht** —
  UC-1 würde nie feuern. Der Ankerpunkt muss `patch` (dort ist der detachte Altzustand) oder die
  vorhandenen `dispatchAssetStatusChangeWebhook`-Stellen sein.
- **F3 — ein Anlagen-Ausfall ändert mehrere Anlagen.** `triggerDownTime` propagiert den Status
  rekursiv auf alle Elternanlagen. Ereignisse müssen für diese mit erzeugt werden, sonst greifen
  Regeln auf Anlagengruppen nicht.
- **F4 — der Async-Pool ist klein und geteilt.** `AsyncConfig`: `corePoolSize=3`,
  `maxPoolSize=3`, `queueCapacity=11`, geteilt mit Exports, Imports, Mail, Kommentaren,
  Demodaten und der Triage. Regelauswertung bei *jeder* Entity-Änderung dazu, hinter einem
  minutenlangen Export: Default-`AbortPolicy` → `TaskRejectedException`, Läufe fallen still aus.

---

## 3. Anforderungen

### UC-1 — Leit-Use-Case (anlagenklassengesteuerte Prozesse)

> Eine Anlage hat die Merkmale „Anlagenklasse" (statisch, Wichtigkeit) und einen Betriebszustand
> (dynamisch). Ändert sich der Zustand, sollen abhängig von der Kombination beider operative
> Prozesse anlaufen — Workorder anlegen, Team benachrichtigen, Anlage stillsetzen.

Abzubildende Regeln:

- „Anlagenstatus wird `DOWN` **und** Anlagenklasse = `A` → Workorder Kategorie „Störung",
  Priorität „Hoch", **für diese Anlage**, und Team „Schichtleitung" benachrichtigen."
- „Anlagenklasse = `B` **und** Status wird `DOWN` → Priorität „Mittel", Team „Instandhaltung"."
- „(A **oder** B) **und** C" — eine OR-Verknüpfung muss möglich sein.

### UC-2 — Bestands-Use-Cases

Automatische Zuweisung (Priorität/Team/Ort/Benutzer/Kategorie) bei Workorder- und
Meldungs-Ereignissen; Bestellung genehmigen/ablehnen; Anlagenstatus über eine Aufgabe setzen.
Diese laufen weiter über die Alt-Engine (Koexistenz) und sind **kein** Migrationsziel.

### UC-3 — spätere Erweiterungen

Zeitgesteuerte Regeln (Quartz ist vorhanden), Zählergrenzwerte als Trigger
(`WorkOrderMeterTrigger` als Spezialfall), Benachrichtigung an Rollen, KI-Triage als Aktion
„Anlagen-Vorschlag übernehmen".

### Nicht-funktionale Anforderungen

| # | Anforderung |
|---|---|
| N1 | Eine fehlschlagende Regel beeinträchtigt den auslösenden Use-Case **niemals** (Muster: `RequestTriageListener`). |
| N2 | Wirkungen laufen nach Commit und außerhalb des Anfrage-Threads; **Anreicherungen am auslösenden Objekt laufen in der Transaktion** (siehe §4.4 — sonst ist die API-Antwort falsch). |
| N3 | Endliche Ausführung: Kaskadentiefe und Dedup, transportiert **im Ereignis**, nicht im ThreadLocal. |
| N4 | Jeder Lauf ist protokolliert — auch SKIPPED, mit Grund. |
| N5 | Mandantentrennung: Regeln und Operanden pro Company; Company von Regel und Entity wird geprüft. `CustomField`/`CustomFieldValue` erben von `Audit`, **haben keine Company-Spalte** — der Check läuft über das Trägerobjekt bzw. `customField.companySettings.company`. |
| N6 | Additiv: kein Bestandscode wird ersetzt, keine Tabelle umbenannt, kein TS-Spiegel gelöscht. Abschalten = Flag aus. |
| N7 | Neue Bedingungsquelle = ein neuer `OperandResolver`, neuer Aktionstyp = ein neuer `ActionHandler`; der Metadaten-Endpunkt rendert beides automatisch. |
| N8 | Eigener Executor. Die Engine darf sich den 3er-Pool aus F4 nicht mit den Exports teilen. |

---

## 4. Zielarchitektur

### 4.1 Koexistenz statt Ersatz

Die neue Engine entsteht in einem eigenen Package (`com.grash.automation`), mit eigenen
Tabellen, eigenem Endpunkt (`/automation-rules`) und eigener Frontend-Route. `Workflow*`,
`WorkflowController`, die 1081-Zeilen-UI und die vier TS-Spiegel bleiben **wie sie sind**.

Der Grund ist nicht Vorsicht, sondern Rechnen: Upstream läuft mit ~11 Commits pro Tag und wird
monatlich gemergt (siehe `CLAUDE.md`, „Keeping in step with upstream"). Jede ersetzte
Upstream-Datei bedeutet, dass künftige Upstream-Workflow-Commits auf entfernten Code landen —
monatlich, dauerhaft. Ein additives Package erzeugt fast nur neue Dateien und mergt konfliktfrei.
`mobile/` ist ausdrücklich unverändert Upstream; ein gelöschter Spiegel dort wäre die teuerste
Zeile im ganzen Vorhaben.

Ein Feature-Flag (`automation.enabled`, Default `false`) bleibt damit Dauerzustand, nicht
Rollback-Phase.

### 4.2 Ereignis-Schicht

Ein generisches Ereignis statt eines pro Trigger:

```java
public record EntityChangedEvent(
        ChangeType type,           // CREATED, UPDATED, ARCHIVED, CLOSED, APPROVED, REJECTED
        EntityType entityType,     // WORK_ORDER, REQUEST, ASSET, PURCHASE_ORDER, PART, TASK
        Long entityId,
        Long companyId,
        Set<String> changedFields, // bei UPDATED: geänderte Felder; sonst leer
        Long actorUserId,          // wer die Änderung ausgelöst hat; null = System
        UUID correlationId,        // Wurzel-Ereignis einer Kaskade (Dedup)
        int depth) { }             // Kaskadentiefe
```

`actorUserId`, `correlationId` und `depth` **gehören ins Ereignis**, nicht in einen Kontext: ein
ThreadLocal überlebt den AFTER_COMMIT- und Async-Sprung nicht, und ohne sie sind Kaskadenschutz
und Dedup nicht implementierbar. `actorUserId` braucht es, weil im Async-Thread kein
`SecurityContext` liegt (F1) und Benachrichtigungstexte einen Absender brauchen.

Publiziert wird **in den Services**, neben dem `save` — und zwar an denselben Stellen, an denen
heute `dispatchWebhook` gerufen wird. Diese 22 Stellen sind bereits die fachlich richtigen. Für
den Walking Skeleton (§6) ist genau **eine** davon nötig: der Anlagen-Statuswechsel.

> **Richtungsentscheidung, nicht Phase-1-Arbeit:** mittelfristig wird `WebhookDispatchService`
> zum *Listener* von `EntityChangedEvent`. Dann gibt es eine Ereignisquelle mit zwei Konsumenten
> statt zwei parallelen Hook-Mengen, und die `//TODO`-Einträge in `WebhookEvent` fallen als
> Nebeneffekt mit. Bis dahin publizieren die Services beides nebeneinander.

**Feld-Diff.** Für `Asset` im Code, und zwar in `patch`, wo der detachte Altzustand liegt —
nicht in `update` (F2). Eltern-Anlagen aus `triggerDownTime` erhalten eigene Ereignisse (F3).
Für Workorder/Meldung sind die Envers-Modified-Flags die günstigere Quelle.

**Listener.** `@Async("automationExecutor")` + `@TransactionalEventListener(AFTER_COMMIT)` +
try/catch mit `log.warn` + Flag-Abfrage — 1:1 wie `RequestTriageListener`, aber auf einem
**eigenen** Executor (N8/F4), mit eigener Pool- und Queue-Größe und `CallerRunsPolicy` statt
`AbortPolicy`, damit Last bremst statt Läufe zu verlieren.

Regeln werden mit `findEnabledByTrigger(companyId, changeType, entityType)` geladen — `enabled`
wirkt hier also konstruktiv von Anfang an.

### 4.3 Regelmodell

```
automation_rule            trigger + condition_tree + actions (1..n) + run config
automation_condition_group self-referenzierend: operator (AND|OR), parent, Tiefe ≤ 2
automation_condition       subject | operator | value | value_type | custom_field_id (FK)
automation_action_step     action_type | parameters (JSON) | order_index | on_failure
automation_run             Ausführungs-/Audit-Log
```

**Regel:** `triggerChangeType` + `triggerEntityType`, optionaler `triggerChangedFields`-Filter
(Effizienz *und* Loop-Guard), `conditionRoot` (optional — ohne Bedingung feuert immer), geordnete
`actions`, `enabled`, `title`, `company`. Anders als bei D8 wird eine Regel **aktualisiert**, nicht
gelöscht und neu angelegt: die Id ist die Referenz des Run-Logs.

**Bedingung:** vollständig datengetrieben.

| Feld | Beispiel | Bedeutung |
|---|---|---|
| `subject` | `asset.status` · `asset.cf` · `self.priority` | Punktpfad: Wurzel-Operand + Attribut; `self` = auslösendes Entity |
| `customFieldId` | `42` | **echte FK-Spalte** auf `custom_field`, wenn `subject` auf ein Merkmal zeigt |
| `operator` | `IS`, `IS_NOT`, `IN`, `CONTAINS`, `GT`, `LT`, `BETWEEN`, `CHANGED_TO` | `CHANGED_TO` ist diff-basiert und für UC-1 zentral |
| `value` | `"DOWN"` · `"A"` | typisiert interpretiert über die Subject-Metadaten |

Die FK-Spalte ist der Unterschied zum ersten Entwurf, der die Merkmals-Id als String in den Pfad
schrieb (`asset.cf.42`). Ohne FK bricht eine Regel stumm, wenn das Merkmal gelöscht und neu
angelegt wird, und die Datenbank kann es nicht verhindern.

**Operanden-Auflösung** (Erweiterungspunkt N7):

```java
public interface OperandResolver {
    String prefix();                                // "asset.status", "asset.cf", "self"
    OperandDescriptor descriptor(String subject);   // Typ + Werteliste für die UI
    Object resolve(String subject, ExecutionContext ctx);
}
```

Erste Resolver: `AssetResolver` (status, category, location, primaryUser) und
`CustomFieldResolver`. Der Evaluator bleibt davon unberührt.

Zwei Eigenheiten, die der Editor sichtbar machen muss: Merkmale können **an Anlagenklassen
gebunden** sein (siehe [`custom-field-categories.md`](custom-field-categories.md)) — eine
Bedingung auf ein gebundenes Merkmal matcht für Anlagen anderer Klassen stumm nie. Und
`enable_lazy_load_no_trans: true` ist aktiv, d. h. Resolver-Zugriffe laden fröhlich einzeln
nach; der Operand-Cache im Kontext ist deshalb keine Optimierung, sondern Pflicht.

**Bedingungsbaum:** AND/OR-Gruppen, Tiefe ≤ 2 — deckt „(A oder B) und C" und bleibt in der UI
begreifbar.

**Aktionen:** Parameter als JSON, validiert gegen den Handler-Deskriptor beim Speichern.

```json
{ "field": "priority", "value": "HIGH" }
{ "template": { "category": 7, "priority": "HIGH",
                "asset": "${trigger.asset.id}",
                "title": "Störung ${trigger.asset.name}" } }
{ "asset": "${trigger.asset.id}", "status": "DOWN" }
{ "channel": "email|inapp", "recipients": ["team:12"], "templateKey": "asset_down" }
```

`${trigger.*}` ist ein **geschlossener** Platzhalter-Mechanismus mit Whitelist aus den
Operand-Metadaten, kein freier Ausdruck. Das ist die Laufzeitwert-Fähigkeit (D4).

### 4.4 Aktions-Schicht — zwei Klassen, nicht eine

Das ist die zweite wesentliche Korrektur. Heute mutiert `WorkflowService.runWorkOrder` das
Objekt *in place*, und genau dieses Objekt ist die API-Antwort **und** die Payload des
`NEW_WORK_ORDER`-Webhooks. Verlegt man das komplett hinter AFTER_COMMIT, liefert die
Create-Antwort die *unveränderten* Werte, und die Zuweisung erscheint erst beim Reload. Das wäre
eine Regression, kein Fortschritt. Also:

| Klasse | Was | Wann | Warum |
|---|---|---|---|
| **Anreicherung** | Feldzuweisung auf dem **auslösenden** Objekt (`ASSIGN_*` auf `self`) | synchron, vor dem Save, in der Ursprungstransaktion | Antwort und Webhook-Payload sind korrekt; keine zweite UPDATE-Runde, also kein Folgeereignis und keine Kaskade |
| **Wirkung** | alles nach außen: anlegen, benachrichtigen, Fremdobjekt ändern | `@Async`, AFTER_COMMIT | N1/N2; ein Fehler bleibt isoliert |

Nebeneffekt: der dreistufige Loop-Guard des ersten Entwurfs wird deutlich kleiner, weil die
häufigste Loop-Quelle („Regel auf UPDATED, Aktion schreibt dasselbe Objekt") per Konstruktion
verschwindet.

```java
public interface ActionHandler {
    ActionType getType();
    ExecutionPhase phase();          // IN_TRANSACTION | AFTER_COMMIT
    ActionDescriptor descriptor();   // Parameter-Schema für UI und Validierung
    void execute(ActionStep step, ExecutionContext ctx);
}
```

Registrierung über Spring (`List<ActionHandler>` → Map nach Typ). Handler und ihre Pflichten:

| Handler | Bemerkung |
|---|---|
| `AssignFieldHandler` | Anreicherung; Feld-Whitelist pro Entity-Typ |
| `CreateWorkOrderHandler` | über `WorkOrderService.create(wo, company)`; **muss Company explizit setzen (F1)** und `checkUsageBasedLimit`-Ausnahmen als `FAILED` protokollieren, nicht durchschlagen lassen |
| `CreateRequestHandler` | analog |
| `SetAssetStatusHandler` | **muss über `triggerDownTime`/`stopDownTime`** laufen, nicht über das Repository (D9); idempotent — setzt nur bei echter Änderung |
| `SetCustomFieldHandler` | schreibt einen Merkmalswert über `CustomFieldValueService`. **Nicht optional** (Entscheidung in §9.2): ohne ihn sind Merkmale nur lesbar, und die Engine kann keinen Zustand fortschreiben, den sie selbst auswertet. Achtung Klassenbindung: ein Wert für die falsche Anlagenklasse wird von `CustomFieldValueService` *verworfen*, nicht abgelehnt (siehe [`custom-field-categories.md`](custom-field-categories.md)) — der Handler muss das prüfen und als `FAILED` protokollieren, sonst schluckt er den Fehlschlag |
| `NotifyHandler` | `NotificationService.createMultiple` (In-App + Push) und/oder Mail; `Notification` erbt von `Audit`, F1 gilt hier nicht |

Der `ExecutionContext` liefert: frisch geladenes Trigger-Entity, Company, Diff, Regel-Id,
Akteur, `correlationId`, `depth`, Operand-Cache.

### 4.5 Loop- und Kaskadenschutz

1. **Diff-Trigger** — Ereignisse tragen `changedFields`; Regeln filtern darauf; Handler sind
   idempotent, Status X → X erzeugt kein Ereignis.
2. **Klassentrennung (§4.4)** — Anreicherungen erzeugen gar kein Folgeereignis.
3. **Kaskadentiefe** — `depth` im Ereignis, Limit Default 3.
4. **Dedup** — gleiche Regel + gleiches Entity + gleiche `correlationId` = Skip, mit Grund im
   Run-Log.

### 4.6 Audit

`automation_run`: `rule_id`, `entity_type`/`entity_id`, `triggered_at`, `status`
(`SUCCESS|SKIPPED|FAILED`), `matched_conditions` (Snapshot), `actions_executed`, `error`,
`correlation_id`, `depth`. **Company explizit setzen** (F1). SKIPPED-Einträge mit Grund
(Bedingung nicht erfüllt / Kaskadenlimit / Dedup) sind die eigentliche Antwort auf D5.

### 4.7 API und Frontend

**Metadaten-Endpunkt** `GET /automation-rules/meta`, company-scoped (und *nicht* nach dem
Muster von D10 gebaut). Er ist die einzige Stelle, an der das Vokabular der Engine definiert
ist — und er zählt es nicht auf, sondern fragt die registrierten Resolver und Handler
selbst (`AutomationMetaService`). Ein neuer Resolver erscheint dadurch als Bedingung im Editor,
ein neuer Handler als Aktion, ohne eine Zeile Frontend-Arbeit:

```json
{ "engineEnabled": true,
  "triggers": [{ "entityType": "ASSET", "changeType": "UPDATED", "live": true,
                 "changedFields": ["status"] },
               { "entityType": "WORK_ORDER", "changeType": "CREATED", "live": false,
                 "changedFields": [] }],
  "subjects": [{ "subject": "asset.status", "customFieldId": null,
                 "labelKey": "automation_subject_asset_status", "label": null,
                 "valueType": "ENUM", "operators": ["IS","IS_NOT","CHANGED_TO"],
                 "options": ["OPERATIONAL","DOWN"], "boundToCategories": [] },
               { "subject": "asset.cf", "customFieldId": 202, "labelKey": null,
                 "label": "Assetclass", "valueType": "CHOICE",
                 "operators": ["IS","IS_NOT","CONTAINS"],
                 "options": ["1-Critical","2-Operational Critical","3-Support"],
                 "boundToCategories": [] }],
  "actions":  [{ "type": "CREATE_WORK_ORDER", "labelKey": "automation_action_create_work_order",
                 "parameters": [
                   { "name": "title", "valueType": "TEXT", "required": true, "placeholders": true },
                   { "name": "priority", "valueType": "ENUM", "required": false,
                     "options": ["NONE","LOW","MEDIUM","HIGH"], "placeholders": false },
                   { "name": "asset", "valueType": "TRIGGER_REFERENCE", "required": false,
                     "options": ["${trigger.asset.id}"], "placeholders": true }] }],
  "placeholders": ["${trigger.asset.id}","${trigger.asset.name}","${trigger.asset.status}",
                   "${trigger.id}"] }
```

Vier Felder daran sind nicht Kosmetik, sondern jeweils die Antwort auf eine konkrete
Fehlbedienung:

- **`live`** — jede Trigger-Kombination wird gemeldet, aber nur die tatsächlich
  publizierten als `live`. Der Editor zeigt die übrigen als *noch nicht verfügbar* statt
  sie zu verstecken: eine Regel auf einem unpublizierten Trigger speichert sauber, sieht
  richtig aus und feuert nie — das ist von einem Fehler nicht zu unterscheiden. Die Liste
  (`LIVE_TRIGGERS`) ist die eine handgepflegte Stelle im Endpunkt; wer eine Publikationsstelle
  ergänzt, ergänzt sie dort mit.
- **`engineEnabled`** — `AUTOMATION_ENABLED` ist standardmäßig `false`. Ohne diese
  Angabe wäre die Seite eine Falle: Regeln speichern, laufen nie, und nichts auf dem
  Bildschirm sagt warum.
- **`options`** bei einem Auswahlmerkmal — der Editor bietet die echten Optionen als
  Dropdown an. Genau hier ist die erste reale Regel gescheitert: Sie verglich
  „Assetclass“ mit `A`, während die Optionen `1-Critical` / `2-Operational Critical` /
  `3-Support` heißen.
- **`boundToCategories`** — ein an Anlagenklassen gebundenes Merkmal hat für eine Anlage
  anderer Klasse keinen Wert; die Bedingung *kann* dann nicht zutreffen. Der Editor schreibt
  das unter das Feld.

Das Frontend rendert den Editor **vollständig metadatengetrieben**, als neue Route
`/app/settings/features/automation` neben der bestehenden Workflow-Seite. Es hält keine Liste
von Subjekten, Operatoren oder Aktionen — die einzige Abbildung ist `ValueInput.tsx`, das
aus `valueType` das Eingabefeld wählt und einen unbekannten Typ auf ein Textfeld
zurückfallen lässt. Eine serverseitig neue Fähigkeit ist damit am selben Tag bedienbar,
auch ohne passenden Picker. Für die neue Engine gibt es keinen TS-Spiegel, und die vier
bestehenden bleiben unberührt.

**Gegenprobe beim Speichern.** Dieselben Descriptors, aus denen der Editor sein Formular baut,
validiert `AutomationRuleService` beim Speichern (`assertParametersMatchDescriptor`): fehlender
Pflichtparameter, unbekannter Schlüssel, Wert außerhalb der Optionen, unbekannter
Platzhalter, Platzhalter in einem Parameter, der keinen tragen kann. Das ist kein doppelter
Check, sondern der eigentliche: Formular und Regel können so nicht auseinanderlaufen, und
eine per Swagger oder von einem älteren Client geschickte Regel wird am gleichen Maßstab
gemessen. Jeder dieser Fälle wäre sonst ein FAILED-Lauf Minuten später auf einem
Hintergrund-Thread.

### 4.8 Persistenz

Neue Tabellen per Liquibase-Changelog (`ddl-auto: validate`, `db/master.xml`). **Keine
Datenmigration, keine Umbenennung, kein Legacy-Archiv** — die Alt-Engine behält ihre Tabellen und
ihre Regeln. Wer eine Regel in die neue Engine holen will, legt sie dort neu an; bei rund 20
Regeln je Installation ist das billiger als ein Migrationsservice mit Golden-Dump-Test, und es
kann nicht semantisch driften.

---

## 5. UC-1 im Zielmodell

| Aspekt | Konfiguration |
|---|---|
| Trigger | `UPDATED` + `ASSET`, `changedFields`-Filter `status` |
| Bedingung (AND) | `asset.cf` (`customFieldId=202`, „Assetclass") `IS` `1-Critical` **und** `asset.status` `CHANGED_TO` `DOWN` |
| Aktion 1 | `CREATE_WORK_ORDER`, Wirkung — Kategorie „Störung", Priorität „Hoch", `asset = ${trigger.asset.id}`, Titel „Störung ${trigger.asset.name}" |
| Aktion 2 | `NOTIFY`, Wirkung — Team „Schichtleitung", In-App + Mail |
| Absicherung | Ereignis aus `AssetService.patch` (F2), Eltern-Ereignisse (F3), Company explizit (F1), eigener Executor (F4), Run-Log pro Lauf |

Kein Baustein daran ist UC-1-spezifisch; dieselben Mittel bilden UC-3 ab.

---

## 6. Umsetzung

### Phase 0 — Bestandsfixes, unabhängig vom Neubau (0,5–1 T)

Diese lohnen unabhängig davon, ob die neue Engine je kommt:

1. **D7 zuerst** — `TITLE_CONTAINS` implementieren oder aus den vier Modellen und dem UI
   entfernen. Eine Bedingung, die jede Regel stumm tötet, ist der teuerste der Defekte.
2. **D1** — `enabled` in die DTOs, ein Toggle im UI, Filter in der Trigger-Abfrage. Ohne den
   Toggle ist der Filter wirkungslos.
3. **D8** — `patch` aktualisieren statt löschen/neu anlegen; `enabled` nicht überschreiben.
4. Nicht implementierte Aktionen (`ADD_CHECKLIST`, `SEND_REMINDER_EMAIL`, `CREATE_*`) im UI
   ausblenden, solange sie `//TODO` sind.

### Phase S — Walking Skeleton: UC-1 einmal ganz durch (2–3 T)

Ziel ist nicht Vollständigkeit, sondern eine fachlich funktionierende Regel im Betrieb, bevor
Metadaten-Endpunkt und Editor gebaut werden. Umfang:

- `EntityChangedEvent` + `automationExecutor` + Listener (Muster `RequestTriageListener`).
- **Eine** Publikationsstelle: Anlagen-Statuswechsel in `AssetService.patch`, inkl. Eltern (F2/F3).
- Tabellen `automation_rule`, `automation_condition`, `automation_action_step`, `automation_run`.
- Evaluator mit AND-Gruppe und den Operatoren `IS` und `CHANGED_TO`.
- `AssetResolver` + `CustomFieldResolver`.
- `CreateWorkOrderHandler` + `NotifyHandler` + `SetCustomFieldHandler` (alle Wirkung, alle mit
  F1-Vertrag). Der dritte kostet etwa einen halben Tag mehr und ist der Preis der Entscheidung
  in §9.2 — er macht Merkmale schreibbar und nicht nur lesbar.
- Regel-CRUD als schlichter Endpunkt; **Konfiguration interim über Swagger**, keine UI.
- Tests: Evaluator als reine Funktion ohne Spring; ein `@SpringBootTest` mit `Awaitility` für
  Ereignis → Regel → Workorder, inkl. AFTER_COMMIT.

Danach wird neu entschieden. Trägt UC-1 fachlich, folgt:

### Phase 1F — Metadaten und Editor (erledigt)

`OperandResolver.describe` und `ActionHandler.descriptor` als zweite Hälfte der beiden
Erweiterungspunkte, `GET /automation-rules/meta`, Parametervalidierung gegen die Descriptors,
und im Frontend die Route `/app/settings/features/automation`: Regelliste mit Ein-/Aus-Schalter,
metadatengetriebener Editor und Ausführungsverlauf (gesamt und je Regel). Die alte
Workflow-Seite bleibt unberührt daneben stehen.

### Phase 1 — Breite (nach Bedarf)

Weitere Publikationsstellen (die verbleibenden Webhook-Stellen) — und mit jeder davon ein
Eintrag in `LIVE_TRIGGERS` — OR-Gruppen, restliche Operatoren, `AssignFieldHandler` als
Anreicherung, `SetAssetStatusHandler` über `triggerDownTime`. Backend 2—3 T.

### Phase 2 — Konsolidierung und Erweiterung (jeweils eigenständig)

`WebhookDispatchService` als Listener von `EntityChangedEvent` (§4.2) · Scheduler-Trigger über
Quartz · `WorkOrderMeterTrigger` als Spezialfall · KI-Triage als Aktion.

### Was bewusst *nicht* geplant ist

Migration der Bestandsregeln, Umbenennung der Alt-Tabellen, Ersatz der 1081-Zeilen-UI, Löschen
der TS-Spiegel, Übertragung des Lizenz-Gates. Das Lizenz-Gate der Alt-Engine bleibt, wo es ist;
die neue Engine ist auf dieser Instanz durch `SELF_HOSTED_UNLOCK_PREMIUM` ohnehin offen und
braucht keine eigene Zählbasis.

---

## 7. Entscheidungs-Log

| # | Entscheidung | Verworfene Alternative | Begründung |
|---|---|---|---|
| A1 | **Koexistenz: neue Engine additiv, Alt-Engine bleibt** | Ersetzen + migrieren + Legacy umbenennen | Upstream läuft mit ~11 Commits/Tag und wird monatlich gemergt. Jede ersetzte Upstream-Datei kostet bei *jedem* Sync erneut; `mobile/` ist ausdrücklich unverändert Upstream. Additiv heißt: fast nur neue Dateien, konfliktfreier Merge, keine Migration, kein Semantik-Drift-Risiko. |
| A2 | **Eigene schlanke ECA-Engine** auf Spring-Boardmitteln | Drools / Easy Rules; Camunda/Flowable | Der Bedarf ist Regel-Automatisierung, keine Orchestrierung mit Wartezuständen. Externe Engines bringen Lizenz-, Betriebs- und Lernkosten, die bei einem Ereignistyp + Bedingungsbaum + Aktionen nicht gerechtfertigt sind. |
| A3 | **Domain-Events statt Inline-Trigger**, publiziert an den vorhandenen Webhook-Stellen | neue Signatur an den 10 Alt-Aufrufstellen; dritte, eigene Hook-Menge | Entkoppelt (N1/N2), ein Listener-Ort statt zehn, Muster im Projekt erprobt. Die 22 Webhook-Stellen sind das schon ausgewählte Vokabular; eine dritte Menge daneben würde driften. |
| A4 | **Zwei Aktionsklassen: Anreicherung in der Transaktion, Wirkung nach Commit** | alles async | Alles async würde die Create-Antwort und die Webhook-Payload um genau die Zuweisung berauben, die die Regel gemacht hat — sichtbare Regression von UC-2. Nebeneffekt: der Loop-Guard wird kleiner. |
| A5 | **Aktionen als Strategie-Handler + JSON-Parameter + `${trigger.*}`** | typisierte Parameterspalten; freie Templates | Schließt D4, bleibt gegen Handler-Deskriptoren validierbar, verhindert Spalten-Explosion. |
| A6 | **Bedingungen als Daten, Merkmale über echte FK** | Enum-Werte + `switch`; Merkmals-Id als String im Pfad; SpEL/Groovy in der DB | Erweiterung ohne Engine-Änderung und ohne Spiegelpflege. Die FK verhindert, dass eine Regel beim Löschen eines Merkmals stumm bricht. Keine Skriptausführung durch Endanwender. |
| A7 | **Eigener Executor mit `CallerRunsPolicy`** | der bestehende 3er-Pool | Der Pool ist mit Exports, Imports, Mail und Triage geteilt (F4); `AbortPolicy` würde Läufe still verwerfen. |
| A8 | **Keine Datenmigration** | Java-Migrationsservice mit Golden-Dump-Test | Bei ~20 Regeln je Installation ist Neuanlegen billiger als ein Migrationspfad, der semantisch driften kann — und die Alt-Engine läuft ja weiter. |

---

## 8. Risiken

| Risiko | Gegenmaßnahme |
|---|---|
| Async-Aktion kann nichts anlegen (F1) | Vertrag: jeder Handler setzt Company explizit; Integrationstest, der eine Workorder aus dem Listener anlegt |
| Statuswechsel wird nicht erkannt (F2/F3) | Ereignis aus `patch`, nicht aus `update`; Eltern-Ereignisse; Test für OPERATIONAL→DOWN inkl. Anlagengruppe |
| Läufe fallen unter Last still aus (F4) | eigener Executor, `CallerRunsPolicy`, Queue-Größe bewusst gesetzt |
| Loop/Kaskade | §4.5: Diff-Trigger, Klassentrennung, `depth` im Ereignis, Dedup über `correlationId` |
| Neustart während eines Laufs = verlorener Lauf | akzeptiert. Optional später: `automation_run` als PENDING vor dem Lauf anlegen (Outbox), Worker verarbeitet |
| Zwei Engines nebeneinander verwirren den Anwender | getrennte Routen mit klarer Benennung; die neue Seite nennt die alte und umgekehrt; die Alt-Engine wird nicht erweitert |
| Regel bricht stumm, weil ein Merkmal gelöscht wurde | FK auf `custom_field`; Regel wird beim Laden als „unvollständig" markiert statt still falsch ausgewertet |
| Bedingung matcht nie, weil das Merkmal klassengebunden ist | Klassenbindung im Meta-Endpunkt und im Editor anzeigen |
| JSON-Parameter passen nicht zum Handler | Validierung gegen `ActionDescriptor` beim Speichern, keine unbekannten Felder |
| N Regeln × Resolver-Lookups | Regeln je `(company, changeType, entityType)` indiziert; Operand-Cache im Kontext (Pflicht wegen `enable_lazy_load_no_trans`); Merkmale in einem Sammel-Select |

---

## 9. Offene Punkte

### 9.1 Akteur der Engine — entschieden, bleibt offen

**Entscheidung (2026-09-04): `createdBy` bleibt leer.** Von der Engine angelegte Objekte haben
keinen Ersteller; die UI zeigt dort „System". Das ist konsistent mit dem Bestand — Meldungen aus
dem Meldeportal haben schon heute keinen Ersteller, weil dort ebenfalls niemand angemeldet ist.

Der Punkt bleibt trotzdem als offen notiert, aus zwei Gründen:

- **Die Alternative ist derzeit gar nicht baubar.** Ein technischer Benutzer je Company wäre die
  saubere Lösung, weil damit *alle* bestehenden Filter unverändert funktionieren. Auf dieser
  Instanz lassen sich aber momentan keine Benutzer anlegen: `UserService.checkUsageBasedLimit`
  wirft ab dem fünften bezahlten Benutzer (`Consts.usageBasedFreeLimits`, `UNLIMITED_USERS: 5`),
  und `SELF_HOSTED_UNLOCK_PREMIUM` ist per Default `false` und nicht in `docker-compose.yml`
  gesetzt. Solange das so ist, ist die Entscheidung erzwungen, nicht gewählt.
- **Ein leeres `createdBy` versteckt Datensätze.** Wer das Recht „andere sehen" für Aufträge
  nicht hat, bekommt nur, was er selbst erstellt hat oder was ihm bzw. seinem Team zugewiesen
  ist (`WorkOrderService.getSearchCriteria`). Eine Regel, die ein Team zuweist, ist gedeckt —
  UC-1 also. **Auslöser zum Umsteigen:** die erste Regel, die weder Benutzer noch Team zuweist.
  Vorher lohnt der technische Benutzer nicht, danach führt kein Weg daran vorbei.

Nebenbefund, der nicht hierher gehört, aber notiert werden will: die Grenzverletzung wirft eine
nackte `RuntimeException` und landet damit im Catch-all von
`GlobalExceptionHandlerController` — der Anwender bekommt einen HTTP 500 statt einer
Fachmeldung, obwohl der Text durchgereicht wird.

### 9.2 Merkmale müssen schreibbar sein — entschieden

**Entscheidung (2026-09-04): beides, und Merkmale sind erstklassig.** Die Aufgabenstellung ist
nicht ein bestimmter Prozess, sondern die flexible Erweiterbarkeit der Gesamtfunktionalität.
Damit ist die Frage „nativer Status *oder* Merkmal" falsch gestellt:

- Der native `AssetStatus` bleibt die führende Quelle für den **Betriebszustand**, weil an ihm
  Stillstandserfassung, Eltern-Propagation und Webhook hängen. Sieben feste Werte, und die Liste
  zu erweitern wäre eine Änderung an einem Upstream-Enum plus Datenmigration.
- **Merkmale sind gleichrangige Operanden — lesend *und* schreibend.** Ohne Schreibzugriff kann
  die Engine keinen Zustand fortschreiben, den sie selbst auswertet, und genau das ist die
  Erweiterbarkeit, um die es geht. Deshalb steht `SetCustomFieldHandler` ab Phase S in §4.4 und
  ist nicht als spätere Option geführt.

Preis: etwa ein halber Tag in Phase S, plus die Klassenbindung als Falle (ein Wert für die
falsche Anlagenklasse wird verworfen, nicht abgelehnt).
### 9.3 Noch zu klären

- **Benachrichtigungskanäle in Phase S:** In-App genügt (`NotificationService` vorhanden), oder
  Mail von Anfang an?
- **Massenänderungen** (Import, Klassen-Umzug): pro Entity ein Lauf. Einfach, kann viele
  Run-Zeilen erzeugen — Aufbewahrungsdauer für `automation_run` festlegen.

---

## 10. Von Regel-Automatisierung zum Workflow-System

Die Frage, ob daraus „ein echtes Workflow-System" wird, vermischt zwei Achsen, die getrennt
entschieden werden sollten:

- **Erweiterbarkeit** — wie viel neue Automatisierung ohne Codeänderung konfigurierbar ist.
- **Prozesstiefe** — ob ein Vorgang über Zeit und über mehrere Beteiligte einen Zustand behält.

Die erste ist das eigentliche Ziel dieses Konzepts und wird in Stufe 1 vollständig erreicht. Die
zweite ist ein separater Ausbau, und keine ihrer Stufen verlangt, Stufe 1 neu zu bauen — das ist
der Grund, sie nicht vorzuziehen.

### Stufe 1 — Regel-Automatisierung (dieses Konzept)

Ereignis → Bedingungen → Aktionen, ohne Gedächtnis: jede Auslösung ist unabhängig von der
vorigen. Was Erweiterbarkeit hier ausmacht, sind drei Eigenschaften, nicht die Prozesstiefe:
Bedingungen als Daten über beliebige Merkmale, parametrisierte Aktionen einschließlich
*Schreiben* von Merkmalen, und ein Metadaten-Endpunkt, aus dem die UI sich selbst baut. Damit ist
eine neue Automatisierung eine Konfiguration und keine Änderung an fünf Codestellen.

### Stufe 2 — Zeit als Auslöser

Der billigste große Gewinn, und Quartz ist bereits eingerichtet (`job/`, JDBC-JobStore). Zwei
neue Auslöserarten:

- **Terminbasiert** — „jeden Montag 6:00 alle Anlagen der Klasse A prüfen".
- **Fristbasiert** — „48 Stunden nach diesem Ereignis, falls Zustand X dann noch gilt, …".

Die zweite ist bereits Eskalation: Erinnerung, Fristüberschreitung, Wiederholfehler. Für FM ist
das erfahrungsgemäß der Bereich, in dem die meiste echte Automatisierung liegt — mehr als in
mehrstufigen Genehmigungen. Kosten: eine Tabelle geplanter Auslösungen plus ein Quartz-Job,
2–3 Tage. **Empfehlung: direkt nach Stufe 1.**

### Stufe 3 — Prozessinstanzen mit Wartezustand

Erst hier wird es ein Workflow-System im engeren Sinn. Eine Regel startet keinen Einzelschuss,
sondern einen *Fall*, der Zustand behält und auf etwas wartet — eine Freigabe, eine Rückmeldung,
eine Frist. Was dazukommt:

- eine Instanztabelle (welche Definition, welches Objekt, aktueller Schritt, Wartegrund) mit
  **Versionierung**: ein laufender Fall behält die Definition, mit der er gestartet ist
- Fortsetzungs-Auslöser: ein Ereignis oder ein Timer setzt einen wartenden Fall fort
- Verzweigung nach Ergebnis (freigegeben/abgelehnt), nicht nur AND/OR auf einem Ereignis
- Sichtbarkeit: „wo steht dieser Fall?" — das ist die Hälfte des Aufwands

Grob 8–12 Tage. Das ist der große Sprung, und er lohnt erst, wenn ein konkreter Prozess mit
echtem Wartezustand ansteht.

### Stufe 4 — Menschliche Aufgaben

Ein Eingangskorb, Zuweisung und Delegation, Fristen, Eskalation. Setzt Stufe 3 voraus. Hier ist
die Hauptgefahr nicht der Aufwand, sondern die Dopplung: die App hat schon zwei aufgabenartige
Konzepte — Workorder-Aufgaben (`Task`) und die Freigabe/Ablehnung von Meldungen. Ein drittes
danebenzustellen wäre der teuerste Fehler in dieser Reihe.

### Die Alternative, und warum sie hier nicht empfohlen wird

Für Stufe 3 und 4 gibt es fertige Engines; **Flowable** ist Apache-2.0 und in Spring Boot
einbettbar. Man bekäme Wartezustände, Timer, menschliche Aufgaben und einen Prozess-Designer
geschenkt. Bezahlt wird mit einem zweiten Datenmodell neben dem CMMS-Modell, einer BPMN-Lernkurve
und einer dauerhaft größeren Divergenzfläche gegenüber Upstream.

Für eine Instanz, deren Zweck es ist herauszufinden, *welche* FM-Funktionen der Markt braucht,
ist die Reihenfolge deshalb: **Stufe 1, dann Stufe 2, dann messen.** Wenn danach ein echter
mehrstufiger Prozess mit Wartezustand auftaucht, ist die Bewertung von Flowable gegen Stufe 3 die
richtige Frage — vorher ist sie eine Wette auf einen Bedarf, den niemand belegt hat.
