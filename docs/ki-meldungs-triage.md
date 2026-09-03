# KI-gestützte Meldungs-Triage

Eingehende Meldungen sollen sich selbst qualifizieren, damit der Mensch, der sie freigibt,
entscheidet statt recherchiert. Diese Datei beschreibt, was dafür gebaut ist, warum es so
geschnitten ist, und was die nächste Stufe wäre.

Stand: Stufe 1 (Anlagen-Vorschlag) ist umgesetzt. Stufen 2 bis 5 sind Absicht, kein Code.

## 1. Das Problem

Der heutige Weg einer Meldung, aus dem Code gelesen:

```
Portal-Nutzer legt Meldung an
   │  POST /requests/portal/{uuid}       (RequestController.createFromPortal)
   │  Request erbt von WorkOrderBase → title, description, priority,
   │  asset(?), location(?), category(?), image(?), files(?), audioDescription(?)
   ▼
Meldung liegt offen           (GET /requests/pending)
   │   ⇩ hier sitzt die Arbeit ⇩
   │   Ein Limited-Admin liest und entscheidet von Hand:
   │     - Um welche Anlage geht es überhaupt?
   │     - Wie dringend?    - Welche Kategorie?
   │     - Eigenes Team oder Fremdvergabe?    - Schon mal gemeldet?
   ▼
PATCH /requests/{id}/approve  →  createWorkOrderFromRequest(...)
   ▼
WorkOrder
```

Die erste dieser Fragen ist die teuerste. Melder schreiben, wo sie stehen, nicht wie die Anlage
heißt: „im 2. OG der Heizungsraum", nicht „HZ-2201". Die Zuordnung kostet den Admin jedes Mal
einen Blick in die Anlagenliste, und sie ist Voraussetzung für alles andere — Kategorie,
Priorität und die Frage nach dem Duplikat hängen daran, welche Anlage gemeint ist.

Deshalb ist der Anlagen-Vorschlag Stufe 1, und deshalb ist er nicht bloß der einfachste Anfang,
sondern auch die Messung, ob die weiteren Stufen überhaupt tragen. Siehe Abschnitt 6.

## 2. Zwei Annahmen aus der ursprünglichen Skizze, die nicht tragen

Die frühere Konzeptskizze (`docs/idee_ki_integration.md`, mit dieser Datei ersetzt) stützte sich
auf zwei Beobachtungen am Code. Beide klingen richtig und sind es nicht — sie sind hier
festgehalten, weil sie sonst beim nächsten Anlauf erneut gemacht werden.

**Die Workflow-Engine kann das Ergebnis nicht ausführen.** Die Skizze argumentierte, die KI müsse
nur entscheiden; ausführen könne der bestehende Engine, weil `RequestAction` bereits
`ASSIGN_ASSET` kennt. Ein Blick in [`WorkflowService.runRequest`](../api/src/main/java/com/grash/service/WorkflowService.java)
zeigt, warum das nicht geht:

```java
case ASSIGN_ASSET:
    request.setAsset(action.getAsset());
```

Gesetzt wird `action.getAsset()` — also die eine Anlage, die jemand beim *Anlegen der Regel*
fest ausgewählt hat. Der Engine führt eine vorkonfigurierte Entscheidung aus; er nimmt keinen
zur Laufzeit berechneten Wert entgegen, und es gibt keinen Weg, ihm einen zu übergeben, ohne
`WorkflowAction` selbst umzubauen. Ein eigener Übernehmen-Pfad war also unvermeidlich. Da er
ohnehin gebraucht wurde, liegt er dort, wo auch die Entscheidung protokolliert werden kann —
in `RequestQualificationService`.

**Der Event-Hook existiert, aber nicht an der Grenze, die zählt.** `REQUEST_CREATED` gibt es als
`WFMainCondition`, und `onRequestCreation` im Controller ist der richtige Ort. Aber der
Controller ist `@Transactional`: die Meldung ist dort noch nicht committed. Ein asynchroner Job,
der von dort aus startet, sucht in seinem eigenen Thread nach einer Zeile, die es noch nicht
gibt — mal erfolgreich, mal nicht, je nach Timing. Deshalb geht die Triage über ein Event mit
`@TransactionalEventListener(AFTER_COMMIT)` und nicht über einen direkten Aufruf.

## 3. Was gebaut ist

```
POST /requests  oder  /requests/portal/{uuid}
   │  RequestController.onRequestCreation
   │     └── eventPublisher.publishEvent(RequestCreatedEvent)
   ▼  (Transaktion committet)
RequestTriageListener            @TransactionalEventListener(AFTER_COMMIT) + @Async
   │  Fehler hier werden geloggt und verschluckt — die Meldung entsteht in jedem Fall
   ▼
RequestQualificationService.qualify(requestId)      @Transactional(REQUIRES_NEW)
   │  überspringt Meldungen, die schon eine Anlage tragen
   ▼
AssetMatcher.match(request, limit)     ← die austauschbare Stelle
   │  heute: LexicalAssetMatcher ("lexical-v1"), reine Wortähnlichkeit, kein Netz
   ▼
RequestQualification + RequestQualificationCandidate[]     Status PENDING
   ▼
Frontend: QualificationCard in der Meldungs-Detailansicht
   │  [Anlage übernehmen] je Kandidat   [Keine davon]   [Neu vorschlagen]
   ▼
PATCH /request-qualifications/{id}/apply?assetId=…
   │  schreibt request.asset, setzt Status APPLIED, merkt sich wer und welchen
   ▼
danach wie bisher: /approve → WorkOrder
```

### Der Vertrag, auf dem alles ruht

**Der Matcher schreibt nie in die Meldung.** Er liest, bewertet und liefert zurück. Geschrieben
wird nur, wenn ein Mensch auf „Übernehmen" klickt, und dann über das ganz normale Anlagen-Feld.

Das ist keine Vorsicht um ihrer selbst willen, sondern das, was die Fehlerkosten festlegt: ein
falscher Vorschlag kostet einen Blick, keine Korrektur. Solange das gilt, darf der Matcher auch
mittelmäßig sein, und man kann ihn im Betrieb messen, statt ihn vorher richtig raten zu müssen.

**Vorschläge werden abgelöst, nicht überschrieben.** Ein erneuter Lauf setzt die vorherige Zeile
auf `SUPERSEDED` und schreibt eine neue. Was vorgeschlagen wurde und was der Mensch damit gemacht
hat, bleibt beantwortbar — und genau das ist die Datengrundlage für die späteren Stufen.

**Kein Vorschlag ist eine gültige Antwort.** Findet der Matcher nichts über der Schwelle, wird
gar nichts geschrieben und die Karte erscheint nicht. Eine leere Vorschlagskarte ist schlechter
als keine: sie kostet einen Blick und lehrt den Leser, dass die Funktion nichts zu sagen hat.

### Dateien

| Zweck | Datei |
|---|---|
| Entscheidungs-Schnittstelle | [`service/triage/AssetMatcher.java`](../api/src/main/java/com/grash/service/triage/AssetMatcher.java) |
| Heutige Implementierung | [`service/triage/LexicalAssetMatcher.java`](../api/src/main/java/com/grash/service/triage/LexicalAssetMatcher.java) |
| Wortvergleich, Umlaute, Trigramme | [`service/triage/LexicalScorer.java`](../api/src/main/java/com/grash/service/triage/LexicalScorer.java) |
| Speichern und Anwenden | [`service/RequestQualificationService.java`](../api/src/main/java/com/grash/service/RequestQualificationService.java) |
| Event und Listener | [`event/RequestCreatedEvent.java`](../api/src/main/java/com/grash/event/RequestCreatedEvent.java), [`event/RequestTriageListener.java`](../api/src/main/java/com/grash/event/RequestTriageListener.java) |
| Entitäten | [`model/RequestQualification.java`](../api/src/main/java/com/grash/model/RequestQualification.java), [`model/RequestQualificationCandidate.java`](../api/src/main/java/com/grash/model/RequestQualificationCandidate.java) |
| Endpunkte | [`controller/RequestQualificationController.java`](../api/src/main/java/com/grash/controller/RequestQualificationController.java) |
| Schema | `db/changelog/2026_08_29_00000000001_request_qualification.xml` |
| Karte im Frontend | [`content/own/Requests/QualificationCard.tsx`](../frontend/src/content/own/Requests/QualificationCard.tsx) |
| Tests | [`service/triage/LexicalAssetMatcherTest.java`](../api/src/test/java/com/grash/service/triage/LexicalAssetMatcherTest.java) |

Fast alles davon sind neue Dateien. Das ist Absicht: eine Datei, die es nur in diesem Fork gibt,
kann beim Upstream-Abgleich nicht kollidieren. In `RequestController` steht genau eine
zusätzliche Zeile — das veröffentlichte Event.

## 4. Warum lexikalisch und nicht per Embedding

Die Skizze sah für Stufe 1 Embeddings vor. Dagegen sprachen drei Dinge, in dieser Reihenfolge:

1. **Es gibt keinen Untergrund dafür.** Die Datenbank ist `postgres:16-alpine` ohne pgvector; an
   Textsuche existiert `unaccent` plus `LIKE` in `WrapperSpecification`. Embeddings hätten
   entweder einen Image-Wechsel bedeutet oder Vektoren im Speicher — beides bevor überhaupt klar
   ist, ob der Ansatz trägt.
2. **Es hätte die eigentliche Frage verdeckt.** Wie gut ein Anlagen-Match überhaupt werden kann,
   entscheiden die Anlagenstammdaten, nicht das Verfahren. Ein lexikalischer Matcher misst das
   direkt: findet er die richtige Anlage meistens, sind die Daten gut und ein besseres Verfahren
   wird noch besser. Findet er sie nicht, sind Namen und Standorte zu dünn — und ein
   Embedding-Modell würde das hinter plausibel aussehenden falschen Antworten verstecken.
3. **Es kostet nichts und verlässt die Instanz nicht.** Kein Modell im Betrieb, kein Schlüssel,
   kein FM-Text, der nach außen geht.

`pg_trgm` wäre der naheliegende dritte Weg gewesen — Ähnlichkeit in SQL statt in Java. Dagegen
sprach, dass die Erklärung dann in der Datenbank entsteht: die Karte zeigt die Wörter, die den
Treffer erzeugt haben, und diese Wörter fallen bei der Bewertung in Java ohnehin ab. Für eine
Firmen-Anlagenliste dieser Größe ist der Unterschied in der Laufzeit ohne Bedeutung.

### Wie ein Score entsteht

```
score_raw = Σ über Meldungs-Wörter  MAX über Felder ( Feldgewicht · Ähnlichkeit · idf )
            + IDENTIFIER_BONUS je Bezeichner, der ganz im Text vorkommt
score     = score_raw / (score_raw + SATURATION)
```

Vier Entscheidungen darin sind nicht beliebig:

**Deutsche Komposita über Enthaltensein.** „Heizungsraum" enthält „Heizung". Ein reines
Trigramm-Maß bewertet das um 0,5 und verliert es unter jeder sinnvollen Schwelle; die Regel
„eines enthält das andere, beide mindestens vier Zeichen → 0,9" fängt genau den Fall, der in
deutschen Meldungen ständig auftritt. Ohne Stemmer und ohne Wörterbuch im Image.

**Umlaute werden aufgelöst, nicht entfernt.** „Tür" wird zu „tuer", nicht zu „tur" — so tippen
Leute es, und so steht es in importierten Stammdaten.

**idf macht die Rangfolge erst brauchbar.** In einem Gebäude mit fünfzig Leuchten identifiziert
„Beleuchtung" nichts; „HZ-2201" kommt einmal vor. Ohne inverse Dokumenthäufigkeit über die
eigenen Anlagen träfe jede Beleuchtungsmeldung fünfzig Anlagen gleich stark, und die Reihenfolge
wäre Zufall.

**Bezeichner werden ganz verglichen, nicht in Wörter zerlegt.** Seriennummer, Modell, Custom-ID
und Barcode werden auf Buchstaben und Ziffern reduziert („AB-1200" → `ab1200`) und als Ganzes im
ebenso reduzierten Meldungstext gesucht. Zerlegt man sie an den Trennzeichen, wird das Fragment
„ab" zum Treffer für „AB-1201", „AB-1300" und jede andere Maschine der Baureihe — das
verlässlichste Signal des Matchers würde zu seiner selbstbewusstesten Fehlerquelle. Dieser Fehler
war in der ersten Fassung drin und ist beim Durchrechnen der Testfälle aufgefallen.

**Nicht durch die Wortzahl geteilt.** Die naheliegende Normalisierung auf 0..1 wäre „geteilt
durch Anzahl der Wörter". Sie bestraft eine sorgfältige Beschreibung und belohnt eine knappe,
also genau verkehrt herum. Stattdessen die Sättigungsformel oben.

### Stellschrauben

Alles per Umgebungsvariable, Werte und Begründung in `application.yml`:

| Variable | Vorgabe | Wirkung |
|---|---|---|
| `TRIAGE_ENABLED` | `true` | Ausschalter, ohne Deploy |
| `TRIAGE_MIN_SCORE` | `0.25` | Ab hier wird ein Kandidat überhaupt angeboten |
| `TRIAGE_SATURATION` | `3.0` | Nur Darstellung — verschiebt die Prozentzahlen, nie die Reihenfolge |
| `TRIAGE_MAX_CANDIDATES` | `3` | Wie viele Alternativen die Karte zeigt |

Die Prozentzahl heißt in der Oberfläche bewusst **Übereinstimmung** und nicht Konfidenz. Sie
misst, wie viel des Meldungstextes auf diese Anlage entfällt, und das ist keine
Wahrscheinlichkeit dafür, dass die Anlage stimmt. Scores sind nur innerhalb desselben `engine`
vergleichbar — deshalb steht der Engine-Name an jeder gespeicherten Zeile und auf der Karte.

## 5. Was noch nicht stimmt

Ehrlich benannt, damit es niemand für fertig hält:

- **Bilder und Audio bleiben ungelesen.** `Request.image`, `files` und `audioDescription` gehen
  nicht in die Bewertung ein. Gerade das Typenschild-Foto wäre der stärkste Anhaltspunkt.
- **Keine Nachbewertung.** Wird eine Meldung nachträglich beschrieben oder ein Bild ergänzt,
  läuft die Triage nicht von selbst erneut; es gibt nur den Knopf „Neu vorschlagen".
- **Kein Backfill.** Meldungen, die vor dieser Funktion entstanden sind, haben keine
  Qualifikation. Für die Bestandsdaten reicht derselbe Knopf.
- **Keine Auswertung.** Wie oft der Top-Kandidat übernommen wird, ist die entscheidende Zahl für
  alles Weitere, und sie steht in der Datenbank (`status`, `chosen_asset_id`, `ordinal`) — aber es
  gibt weder eine Ansicht noch einen `rpt_*`-View dafür. Das ist der erste kleine Nachtrag, den
  diese Stufe schuldet; welche zwei Zahlen genau gebraucht werden, steht in Abschnitt 6 unter
  „Was zuerst geklärt sein muss".
- **Der ganze Firmen-Anlagenbestand wird je Meldung gelesen.** Bewusst so: eine Meldung entsteht
  im Menschentempo, und ein Cache bräuchte Invalidierung bei jeder Anlagenänderung. Sollte eine
  Instanz je so groß werden, dass das wehtut, ist die Antwort ein Cache pro Firma mit kurzer
  Lebensdauer — keine Schemaänderung.

## 6. Offene Punkte: die drei Hebel

Notiert 2026-09-03, nach dem ersten Test von Stufe 1 auf der Instanz.

**Der Anlagen-Vorschlag allein ist kein Game Changer, und das ist keine Enttäuschung, sondern
ein Befund.** Drei Gründe, die man kennen muss, bevor man auf ihn aufbaut:

- Er hilft am wenigsten dort, wo Hilfe gebraucht wird. Der Matcher ist genau so gut wie die
  Anlagenstammdaten. Wo Namen, Standorte und Seriennummern gepflegt sind, findet der Mensch die
  Anlage in fünf Sekunden — er kennt sein Gebäude. Wo die Daten dünn sind, findet der Matcher
  sie ebenso wenig. Diese Korrelation ist der Kern des Problems.
- Die Zeitersparnis trägt keine Rechnung. Dreißig Sekunden bei zwanzig Meldungen am Tag sind
  zehn Minuten.
- Es gibt einen deterministischen Weg, der jeden Matcher schlägt: ein QR- oder NFC-Aufkleber an
  der Anlage, gescannt beim Melden. `Asset.nfcId` und `Asset.barCode` liegen im Datenmodell
  bereits. Kein Verfahren, das Text rät, kommt gegen „der Melder stand davor und hat gescannt"
  an. **Das entscheidet die Reihenfolge unten mit**: wer die Identifikation am Objekt löst,
  braucht die späteren Stufen nicht schwächer, sondern kann sie auf sicherem Grund bauen.

**Was Stufe 1 dennoch rechtfertigt: sie ist ein Messinstrument, kein Feature.** Sie beantwortet
mit Zahlen die Frage, die vor allen anderen steht — sind die Anlagenstammdaten gut genug, um
überhaupt etwas darauf zu bauen? Alle drei Hebel unten brauchen zuerst eine sichere
Anlagenzuordnung. Fällt der Match schlecht aus, lautet die Antwort auf alles Weitere „erst
Datenqualität", und das zu wissen ist mehr wert als das Feature. Die zwei Zahlen, die zu erheben
sind, stehen unter „Was zuerst geklärt sein muss".

Die drei Hebel sind nach Aufwand *nicht* sortiert, sondern danach, wo Geld und Haftung liegen.
Keiner davon ist begonnen. Alle drei brauchen einen Entwurf, bevor Code entsteht — der Wert
steckt bei allen dreien in der fachlichen Modellierung, nicht in der Technik.

### 6.1 Disposition — eigenes Team oder Fremdvergabe

**Warum es zählt.** Hier bewegt sich echtes Geld: ein unnötig beauftragter Techniker kostet
dreistellig, ein nicht beauftragter kostet Stillstand. Und die Entscheidung ist
unternehmensspezifisch — ob „Leuchtstoffröhre defekt" Hausmeister oder Fremdvergabe ist, ist bei
Kunde A anders als bei Kunde B. Genau deshalb deckt Standardsoftware es nicht ab, und genau
deshalb ist es die Art Problem, für die jemand Beratung kauft.

**Was es braucht.** Beispiele aus der eigenen Historie: vergangene, freigegebene Aufträge mit
Vendor gegen solche ohne. Eine generische KI ohne diese Beispiele liefert hier Unsinn — das ist
der Grund, warum dieser Punkt nicht der erste sein kann, obwohl er der wertvollste ist.
Zielwerte: `IN_HOUSE` / `EXTERNAL`, dazu bei `EXTERNAL` ein Vendor-Vorschlag aus
`Asset.vendors` und den Vendor-Stammdaten.

**Woran es scheitern würde.** An zu wenig Historie und an einer Instanz, in der der Unterschied
gar nicht in den Daten steht — wenn Fremdvergaben nicht als solche erfasst sind, gibt es nichts
zu lernen. Das ist vor dem Entwurf zu prüfen, nicht danach.

### 6.2 Duplikate und Wiederholfehler

**Warum es zählt.** Ein zweiter Auftrag für dieselbe Störung kostet einen kompletten Einsatz.
Und die Wiederholung ist der eigentliche Ertrag: dieselbe Pumpe zum vierten Mal in sechs Monaten
ist kein Wartungsfall mehr, sondern ein Investitionsfall. Das ist der Übergang von
Ticketbearbeitung zu einer Lebenszyklus-Entscheidung — und damit von Betrieb zu Beratung.

**Was es braucht.** Räumliche und zeitliche Eingrenzung, sonst findet „Beleuchtung"
zweihundert Treffer: nur Meldungen und Aufträge derselben Anlage oder desselben Standorts,
letzte 90 Tage. Für den Wiederholfehler zusätzlich eine Zählung über einen längeren Zeitraum und
eine Schwelle, ab der aus „schon mal gemeldet" ein Hinweis auf Ersatz wird.

**Woran es scheitern würde.** An falschen Positiven. Und deshalb gilt hier dieselbe Regel wie in
Abschnitt 3: nie automatisch ablehnen, immer nur Hinweis mit Link auf die andere Meldung. Der
Mensch entscheidet.

### 6.3 Verbindlichkeit und Haftung

**Warum es zählt.** Das ist der Punkt, an dem FM-Kunden tatsächlich Schmerz spüren — GEFMA 310,
Verkehrssicherungspflichten, Nachweisführung. Eine Triage, die sagt „das ist eine prüfpflichtige
Anlage, hier hängt eine Frist dran", ist etwas anderes als eine, die sagt „das ist vermutlich die
Heizung". Sie verändert nicht die Bearbeitungsgeschwindigkeit, sondern das Risiko.

**Was es braucht.** Anders als 6.1 und 6.2 ist das **kein KI-Problem, sondern ein
Regelwerk-Problem**: welche Pflicht an welcher Anlagenklasse hängt, ist nachschlagbar und muss
deterministisch beantwortet werden, nicht geschätzt. Eine Einschätzung mit Konfidenz ist hier die
falsche Antwortform — bei einer Prüfpflicht will niemand 78 % hören. Die KI darf höchstens die
Anlagenklasse vorschlagen; die Pflicht selbst muss aus einer Tabelle kommen.

**Woran es scheitern würde.** Daran, es wie die anderen beiden zu bauen. Wenn ein Sprachmodell
Fristen erfindet, ist der Schaden größer als der gesamte Nutzen der Triage.

**Berührungspunkt.** Der Verbindlichkeitsfilter über die Klassifizierungsstufen ist Gegenstand
eines eigenen Vorhabens außerhalb dieses Repositorys. Vor einem Entwurf hier ist zu klären, ob
dieser Use Case dessen Regelwerk konsumiert statt ein zweites daneben zu stellen — zwei
Pflichtenmodelle, die sich widersprechen können, sind schlimmer als keines.

### Was zuerst geklärt sein muss

Bevor an 6.1 bis 6.3 gebaut wird, zwei Zahlen aus dem laufenden Stand 1. Sie stehen in der
Datenbank (`request_qualification.status`, `chosen_asset_id`, `request_qualification_candidate.ordinal`),
aber es gibt noch keine Ansicht dafür — das ist der erste kleine Nachtrag, den diese Stufe
schuldet:

1. **Wie oft hätte der Bearbeiter die Anlage ohne die Karte sofort gewusst?** Das ist der Anteil,
   an dem die Funktion nichts einspart. Nur von Hand erhebbar, über eine Stichprobe.
2. **Wie oft war der erste Vorschlag richtig?** Das ist der Anteil, auf dem 6.1 bis 6.3 aufbauen
   könnten. Aus `status = APPLIED` und `ordinal = 0` ablesbar.

### Kleinere Punkte, die aus der ursprünglichen Skizze übrig sind

Bewusst nachgeordnet, weil keiner davon Geld oder Haftung bewegt:

- **Kategorie und Priorität** per Sprachmodell mit strukturierter Ausgabe. Wirkt in der Liste der
  offenen Meldungen. Erst hier wird ein `LlmProvider`-Interface gebraucht; es jetzt zu bauen wäre
  eine Schnittstelle ohne zweite Implementierung. Mit ihm kommen die Fragen, die dieser Stand
  nicht hat: Schlüsselverwaltung, Kosten, und ob FM-Kundentexte die Instanz verlassen dürfen.
- **Bild und Audio.** OCR auf Typenschild-Fotos, Schadensbild, Spracherkennung für
  `audioDescription`. Eine Ausnahme ist hier wertvoll: OCR auf dem Typenschild speist den
  Bezeichner-Pfad aus Stufe 1, und der ist der stärkste Hebel im Anlagen-Match — die zweitbeste
  Variante dessen, was ein QR-Aufkleber sicher löst.
- **Automatische Übernahme** ist keine Stufe, sondern eine Entscheidung, die man erst treffen
  darf, wenn die Zahlen oben sie tragen — und dann nur für die Teilmenge, für die sie sie tragen.
  Der Vertrag aus Abschnitt 3 (der Matcher schreibt nie selbst) ist genau das, was diese
  Entscheidung offen hält, statt sie vorwegzunehmen.
