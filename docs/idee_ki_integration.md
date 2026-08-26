Ich fokussiere mich jetzt tief auf den Meldungs-Workflow. Zuerst verstehe ich, wie eine Meldung im heutigen System entsteht, welche Daten sie trägt, und wo sie in einen WorkOrder übergeht – das sind genau die Ankerpunkte für den KI-Eingriff.

Request erbt von `WorkOrderBase` (hat also Titel, Beschreibung, Priorität, Asset, Location, Bild, Kategorie, primaryUser). Jetzt der Request-Controller und wie aus einer Request eine WorkOrder wird.

Jetzt der entscheidende Punkt: der `/approve`-Endpoint (228) und wie der heutige Workflow aussieht, plus der `/pending`-View. Das ist genau die Stelle, wo die KI ansetzen würde.

Entscheidend: Beim `/approve` läuft bereits `workflowService.runRequest(...)` – es gibt also **bereits einen Automations-Hook** auf Request-Events. Das ist der natürliche Ankerpunkt. Ich prüfe noch den Workflow-Service und wie Dateien an einer Request hängen, dann habe ich alles für die Skizze.

Perfekt – ich habe jetzt das vollständige Bild. Der Workflow-Engine kann bereits Asset, Kategorie, Location, Priority, Team, User an einer Request automatisch setzen – die KI muss nur *entscheiden*, was gesetzt wird, das Ausführen kann der bestehende Engine übernehmen. Das ist die sauberste Integrationsnaht. Ich skizziere jetzt den Use Case.

## 1. Der heutige Workflow – und wo die Reibung sitzt

Aus dem Code ergibt sich der exakte Ist-Zustand einer Meldung:

```
Portal-Nutzer erstellt Request
   │  POST /requests/portal/{uuid}
   │  Request erbt von WorkOrderBase → hat: title, description, priority,
   │  asset(?), location(?), category(?), image(?), files(?), audioDescription(?)
   ▼
Request liegt "pending"  (GET /requests/pending)
   │   ⇩ HIER ist die Reibung ⇩
   │   Ein Limited-Admin muss manuell lesen + entscheiden:
   │     - Ist das überhaupt unser Asset? → asset zuordnen
   │     - Wie dringend?                   → priority setzen
   │     - Welche Kategorie?               → category setzen
   │     - Hausmeister-Sache oder Fremdvergabe?
   │     - Schon mal gemeldet?
   ▼
PATCH /requests/{id}/approve  →  createWorkOrderFromRequest(...)
   │  + runRequest(workflows, REQUEST_APPROVED)  ← existierender Hook
   ▼
WorkOrder
```

Die drei Erkenntnisse, die alles weitere tragen:

1. **Der KI-Eingriffszeitpunkt ist natürlich gegeben:** Das Event `REQUEST_CREATED` existiert bereits als `WFMainCondition`. Die KI feuert genau dann, wenn eine Meldung reinkommt – kein neuer Trigger-Mechanismus nötig.
2. **Der bestehende Workflow-Engine kann schon alles ausführen, was die KI entscheiden wird.** `runRequest()` kann `ASSIGN_ASSET / CATEGORY / LOCATION / PRIORITY / TEAM / USER`. Die KI muss *nicht* selbst mutieren – sie liefert Entscheidungen, der Engine setzt sie. Das hält die KI seiteneffektfrei und testbar.
3. **Ihre drei Beispiele ("Hausmeister", "Fremdvergabe", "bereits gemeldet") sind keine Automation, sondern Triage-Empfehlungen.** Genau richtig. Die KI qualifiziert, der Mensch entscheidet mit einem Klick. Das senkt die Fehlerkosten drastisch.

## 2. Was die KI konkret liefern soll (Triage-Ergebnis)

Eine qualification pro Request mit diesen Feldern – alles als *Vorschlag mit Begründung und Confidence*, niemals autonom:

| Feld | Wert | Datenbasis dafür |
|---|---|---|
| **asset** | Vorschlag + Alternativen | Match von Request-Text/Bild gegen `Asset.name/serialNumber/model/description/location` |
| **category** | Vorschlag | Klassifikation gegen `WorkOrderCategory` der Company, gelernt aus Historie |
| **priority** | Vorschlag (NONE/LOW/MEDIUM/HIGH/URGENT) | Schlüsselwörter + Bild-Analyse (Wasser, Rauch, Sicherheit → HIGH) |
| **disposition** | `IN_HOUSE` / `EXTERNAL` / `DUPLICATE` / `INFO_ONLY` | Die Unterscheidung, die Sie beschreiben |
| **duplicate_of** | Request/WorkOrder-ID + Score | Semantische Suche gegen offene Requests & kürzlich geschlossene WOs desselben Assets |
| **confidence** | 0..1 pro Feld | Damit der Mensch weiß, wo hinschauen |
| **rationale** | 1–2 Sätze | Vertrauen + Nachvollziehbarkeit (verweisend auf Asset/WO) |

Die Disposition ist der Kern Ihres Use Cases. Eine konsistente Triage-Sprache dafür:

- **`IN_HOUSE`** → "Hausmeister/ eigenes Team kann das" → Vorschlag: WO-Typ, Team, Standard-Checkliste
- **`EXTERNAL`** → "Muss beauftragt werden" → Vorschlag: Vendor (aus `Asset.vendors` / `Vendor`-Stammdaten), Purchase-Order-Anstoß
- **`DUPLICATE`** → "Bereits gemeldet" → Verweis auf die andere offene Request/WO, Vorschlag: zusammenlegen oder ablehnen
- **`INFO_ONLY`** → "Keine Wartung nötig" → Vorschlag: ablehnen mit Begründung

## 3. Datenfluss – Human-in-the-loop

```
                    ┌─────────────────────────────┐
   REQUEST_CREATED  │  1. Enrichment (synchron)   │   Das alles passiert asynchron,
   Event feuert ───▶│     Request + Files + Audio │   der Portal-User merkt nichts
                    │     sammeln                  │   davon; der Admin sieht es,
                    └─────────────┬───────────────┘   wenn er den Fall öffnet
                                  │
                    ┌─────────────▼───────────────┐
                    │  2. AI Qualification Service │
                    │     (Provider-Interface,     │
                    │      austauschbar)           │
                    │   • Asset-Match (Embedding)  │
                    │   • Category/Priority (LLM)  │
                    │   • Disposition (LLM+Rules)  │
                    │   • Duplicate-Suche          │
                    └─────────────┬───────────────┘
                                  │ schreibt
                    ┌─────────────▼───────────────┐
                    │  RequestQualification        │  neue Entity, 1:1 zu Request
                    │  (status: PENDING/Applied)   │  versioniert, nachvollziehbar
                    └─────────────┬───────────────┘
                                  │ Admin öffnet Request
                    ┌─────────────▼───────────────┐
                    │  3. Triage-UI im Frontend    │
                    │   "KI schlägt vor: …"        │
                    │   [Übernehmen] [Ändern] [Ablehnen] │
                    └─────────────┬───────────────┘
                                  │ Klick "Übernehmen"
                                  ▼
                    vorhandener Workflow-Engine
                    (runRequest setzt Asset/Category/Priority …)
                                  │
                                  ▼  wie heute:  /approve → WorkOrder
```

Bewusst so entkoppelt: Die KI *mutiert nie* die Request. Sie schreibt nur in eine eigene `RequestQualification`-Tabelle. Übernommen wird entweder per Klick durch den Menschen oder – später, wenn Vertrauen da ist – per Policy für High-Confidence-Fälle automatisch.

## 4. Technische Skizze

**Backend (Spring Boot, fügt sich in das bestehende Bild ein):**

- **Event-Anker:** Der Hook existiert schon. Die `onRequestCreation`-Logik im Controller (sichtbar ab Zeile 201) ist der Ort, an dem heute schon beim Anlegen etwas passiert – dort einen asynchronen Qualification-Job anstoßen, *nicht* synchron (der Portal-User soll nicht auf die KI warten).
- **Neue Entität `RequestQualification** (1:1 zu `Request`): `disposition`, `assetId`, `categoryId`, `priority`, `duplicateOf`, je mit `confidence` und `rationale`, `status` (PENDING / APPLIED / REJECTED / SUPERSEDED), `createdAt`. Bewusst versioniert – bei neuen Info (Bild nachgereicht) neu bewerten, alte nicht löschen (Nachvollziehbarkeit, Audit).
- **`AiQualificationService`** mit drei klar getrennten Stufen, weil sie unterschiedlich reifen und unterschiedlich teuer sind:
  1. **Asset-Match** – Embedding-Suche (lokal oder API) gegen die Asset-Stammdaten der *eigenen* Company. Kein LLM nötig. Deterministisch, billig, datenschutzfreundlich.
  2. **Duplicate-Detection** – Embedding-Ähnlichkeit über Title+Description der offenen/kürzlich geschlossenen Requests & WOs derselben Company und desselben Assets.
  3. **Disposition / Category / Priority** – LLM-Aufruf mit *strukturiertem Output* (JSON-Schema, `zod`/function-calling), gefüttert mit: Request-Text, transkribiertem Audio (falls vorhanden), Bild-Analyse (falls Bild angehängt), den Top-3 Asset-Kandidaten, der Kategorieliste der Company und wenigen wenigen Few-Shot-Beispielen aus der eigenen Historie.
- **Provider-Interface (kritisch für Nachhaltigkeit):** Ein `LlmProvider`-Interface, Implementierungen für OpenAI/Anthropic *und* einen lokalen Runner (Ollama/llama.cpp). Grund: Ihre Instanz ist single-tenant self-hosted, FM-Kundendaten sind sensitiv, und Vendor-Lock-In wäre fahrlässig. Konfiguration per Env-Vars (`AI_PROVIDER`, `AI_MODEL`, `AI_API_KEY`), analog zu den vielen bestehenden Env-Vars im Compose-File. Keine hardcodierte OpenAI-Verdrahtung – das ist der häufigste Fehler, den man dann ein Jahr lang bereut.
- **Async + fehlertolerant:** Quartz (bereits im Stack) für Retries; bei KI-Ausfall läuft die App normal weiter – der Admin triagiert dann eben manuell wie heute. Die KI ist *Enhancement*, nie Blocker.
- **Audit/Privacy:** Prompts und Antworten in einer `AiInteraction`-Tabelle loggen (Request-Id, Provider, Token-Cost, Latenz, rohe Antwort) – einerseits Debugging, andererseits Nachweis gegenüber Kunden ("die KI hat Daten an X gesendet"). Für FM-Kunden oft gefordert.

**Frontend (React/MUI):**
- In der Request-Detailansicht eine **"KI-Einschätzung"**-Karte (oberhalb der manuellen Felder), die `RequestQualification` zeigt: Disposition als Badge, Asset/Kategorie/Priorität als Vorschläge mit Confidence-Bar, Duplicate-Hinweis mit Link zur anderen Meldung, Begründung als Text. Drei Buttons: *Übernehmen* (schreibt die Vorschläge in die Felder), *Teilweise* (z.B. nur Asset), *Ablehnen* (mit optionalem Feedback → Trainingsdaten).
- In der `/pending`-Liste eine **Spalte "KI"** mit Disposition-Badge + Confidence, sodass der Admin die Liste schon visuell sortieren kann ("erst alle EXTERNAL, dann DUPLICATE prüfen"). Genau das ist der Geschwindigkeitsgewinn, den Sie beschreiben.
- Wichtig: die UI macht deutlich, dass es ein *Vorschlag* ist. Keine autonome Übernahme in MVP – erst wenn Sie anhand der Logs sehen, dass z.B. `IN_HOUSE`-Vorschläge bei confidence > 0,9 zu 98 % richtig sind, aktivieren Sie eine Auto-Apply-Policy *für genau diese Teilmenge*.

## 5. Die schwierigen Stellen – ehrlich benannt

Drei Punkte, die über Erfolg oder Misserfolg entscheiden und die man beim Bauen unterschätzt:

1. **Asset-Matching ist der Flaschenhals.** Die whole value hängt daran, dass die KI das richtige Asset identifiziert. Portal-Nutzer schreiben "im 2. OG der Heizungsraum" statt "Asset #A-4471". Solution: Embeddings über *alle* Asset-Texte (name + description + serialNumber + model + location-Kette) + Location-Hierarchie. Wenn confidence niedrig: 2–3 Alternativen zur Auswahl, nicht einer. Das ist der Ort, an dem Sie am meisten Zeit investieren sollten.
2. **Disposition `IN_HOUSE` vs. `EXTERNAL`** ist eine *unternehmensspezifische* Entscheidung, keine allgemeine. "Leuchtstoffröhre defekt" ist bei Kunde A Hausmeister-Job, bei Kunde B Fremdvergabe. Lösung: Diese Klassifikation *muss* aus der eigenen Historie gelernt werden (Few-Shot aus vergangenen bewilligten WOs mit Vendor bzw. ohne). Eine generische KI ohne diese Beispiele liefert hier Mist. Das ist gleichzeitig der Hebel, warum Datenqualität (Thema 1) und dieser Use Case sich gegenseitig brauchen.
3. **Duplicate-Erkennung** muss *räumlich und zeitlich* eingeschränkt sein, sonst gibt es falsche Positive ("Beleuchtung" findet 200 Treffer). Lösung: nur Requests/WOs *derselben Location/desselben Assets* der *letzten 90 Tage*. Und nie autonom ablehnen – immer nur Hinweis mit Link, der Mensch entscheidet.

## 6. Empfohlene MVP-Reihenfolge

Damit Sie nicht alles auf einmal bauen und dann merken, dass die Basis fehlt:

1. **Sprint 1 – Fundament & Asset-Match:** `RequestQualification`-Entity, Provider-Interface mit einer Implementierung, Event-Hook, und *nur* Asset-Matching per Embeddings. Frontend: Karte "KI schlägt Asset X vor (87 %)". → Schon das spürtbar viel Wert, und Sie haben die gesamte Infrastruktur.
2. **Sprint 2 – Category & Priority** via LLM mit strukturiertem Output. Wirkt sofort in der `/pending`-Liste.
3. **Sprint 3 – Duplicate-Detection** (braucht genug Historie, deshalb später).
4. **Sprint 4 – Disposition** `IN_HOUSE`/`EXTERNAL`/`DUPLICATE`/`INFO_ONLY` als das Herzstück – bewusst zuletzt, weil es die eigene Historie als Lernbasis braucht, die in den Sprints 1–3 aufgebaut/validiert wurde.
5. **Sprint 5 – Bild & Audio** (OCR von Typenschild-Fotos, Schadensbild-Klassifikation, Speech-to-Text für `audioDescription`). Optional, schöner Add-on, aber nicht kritisch für den Kernnutzen.

**Mein Rat zum Startpunkt:** Machen Sie Sprint 1 und messen Sie, wie gut das Asset-Matching allein schon ist. In der Praxis bringt der Asset-Match allein schon 60 % des Triage-Geschwindigkeitsgewinns, weil er die häufigste und zeitraubendste manuelle Entscheidung ist. Ist der gut, baut man beruhigt den Rest darauf auf. Ist er schlecht, weiß man, dass erst die Datenqualität der Asset-Stammdammen dran glauben muss – *bevor* man LLMs für Disposition investiert.

Wenn Sie möchten, kann ich als Nächstes die Entity- und Provider-Interface-Skizze konkreter ausarbeiten (Felder, Methoden-Signaturen, Compose-Einträge) oder den Prompt-Entwurf für die Disposition mit dem JSON-Schema formulieren.