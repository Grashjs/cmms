# Upstream-Synchronisationsbericht

**Datum der Durchführung:** 03. September 2026  
**Ziel-Branch:** `sync/upstream-merge-2026-09-03` (vor Übernahme in `main`)  
**Autor:** Antigravity / Pair Programming mit Entwickler

---

## 1. Kontext & Betrachtete Zeitperiode

Bei der Weiterentwicklung des Forks `cmms4fm` (basierend auf `Grashjs/cmms`) entstand eine Divergenz zwischen dem Fork und dem ursprünglichen Upstream-Repository.

* **Gemeinsamer Ausgangspunkt (Merge-Base):**
  * Commit: `e1d24406fe41601773e4924ed68034068d340991`
  * Datum: **24. August 2026, 14:20:34 UTC**
  * Titel: *refactor: update custom field value schema to set access mode as READ_ONLY*

* **Upstream-Entwicklung (`upstream/main`):**
  * Betrachteter Zeitraum: **24. August 2026 bis 03. September 2026**
  * Umfang: **45 Commits**
  * Bis Commit: `12b996b1689cd5f8d557bb523c9cb5b16954209c` (03.09.2026, 14:40:52 UTC)
  * Hauptthemen Upstream:
    * Architektur-Refactoring: Verschiebung von Geschäftslogik aus Controllern in Service-Klassen
    * Sicherheitsmodell: Nutzung von `@CurrentUser User user` Parameter-Injection statt manueller `userService.whoami(req)`
    * Unit- und Integrationstests für Service- und Controller-Ebene
    * Bugfixes: Behebung von Nullable `estimatedDuration` bei Kalender-Events, Part-Version-Null Fix
    * Neuer Account-Löschungs-Workflow

* **Fork-Entwicklung (`origin/main` / `cmms4fm`):**
  * Umfang: **76 Commits**
  * Bis Commit: `06a305510488` (03.09.2026, 20:31:37 +02:00)
  * Eigene Features:
    * KI-Meldungstriage (`RequestCreatedEvent`, Suggestion von Anlagen/Prioritäten)
    * Gespeicherte Tabellenansichten (Saved Views)
    * Asset-Kategorien für Custom Fields
    * Verknüpfung von Bestellungen (Purchase Orders) mit Arbeitsaufträgen (Work Orders)
    * Erweiterte Reporting-Views
    * Vollständige deutsche Übersetzung (`messages_de_DE.properties`, `de.ts`) und UI-Branding

---

## 2. Analyse & Automatischer Merge

Von über 270 im Diff involvierten Dateien konnten nahezu alle Dateien durch Git konfliktfrei automatisch zusammengeführt werden (u. a. `PartService`, `UserService`, `WorkOrderService`, `application.yml`, Docker-Konfiguration und Frontend-Komponenten).

---

## 3. Aufgetretene Konflikte und deren Auflösung

Es traten genau 4 Dateikonflikte auf, die wie folgt gelöst wurden:

### 3.1 `api/src/main/resources/db/master.xml`
* **Ursache:** Beide Seiten haben am Ende der Datei neue Liquibase-Changelogs eingebunden.
* **Lösung:** Beide Einträge wurden sequentiell beibehalten:
  * `changelog/2026_08_29_00000000001_request_qualification.xml` (Fork - Triage Tabellen)
  * `changelog/2026_08_31_00000000001_fix_part_version_null.xml` (Upstream - Bugfix für Part Versionen)

### 3.2 `api/src/main/resources/messages.properties`
* **Ursache:** Beide Seiten ergänzten neue Lokalisierungs-Keys am Ende der Datei.
* **Lösung:** Beibehaltung der Fork-Keys (`Custom_ID`, `Asset_Custom_ID`, `Requested_By` etc.) sowie des neuen Upstream-Keys (`delete_account={0} Account Deletion`).

### 3.3 `api/src/main/resources/messages_de_DE.properties`
* **Ursache:** Analog zu den englischen Messages.
* **Lösung:** Zusammenführung aller deutschen Übersetzungs-Keys inklusive `delete_account={0}-Konto l\u00F6schen`.

### 3.4 `api/src/main/java/com/grash/controller/RequestController.java` & `RequestService.java`
* **Ursache:** 
  * Upstream hat den Controller radikal verschlankt und Methoden wie `create`, `createFromPortal`, `approve`, `cancel` und `delete` in den `RequestService` ausgelagert.
  * Der Fork hatte im alten `RequestController` in `createWorkflowsAndNotifications` das Event `RequestCreatedEvent` via `ApplicationEventPublisher` gefeuert (notwendig für die KI-Triage).
* **Architektur-Harmonisierung:**
  * Der schlanke, saubere `RequestController` aus Upstream wurde vollständig übernommen.
  * In `RequestService.java` wurde `ApplicationEventPublisher` als Dependency injiziert.
  * In `RequestService.onRequestCreation(...)` (wo Upstream nun Workflows und Benachrichtigungen verarbeitet) wurde das Triage-Event sauber integriert:
    ```java
    // Triage. Published rather than called: this method runs inside the request creation
    // transaction, and the listener has to see a committed request. See RequestTriageListener,
    // and docs/ki-meldungs-triage.md for why the workflow engine above cannot do this job.
    eventPublisher.publishEvent(new RequestCreatedEvent(createdRequest.getId(), company.getId()));
    ```

---

## 4. Leitfaden für künftige Upstream-Syncs

1. **Upstream abrufen:**
   ```bash
   git fetch upstream
   ```
2. **Divergenz prüfen:**
   ```bash
   git rev-list --left-right --count main...upstream/main
   git merge-tree --write-tree main upstream/main
   ```
3. **Synchronisations-Branch nutzen:**
   Stets auf einem temporären Branch (z. B. `sync/upstream-merge-YYYY-MM-DD`) mergen und erst nach Compiler-/Build-Checks in `main` überführen.
4. **Architektur-Regel:**
   Wenn Upstream Controller-Logik in Services verschiebt, eigene Anpassungen (wie Events oder Hooks) immer in die entsprechenden Service-Methoden nachziehen, anstatt den Controller wieder aufzublähen.
