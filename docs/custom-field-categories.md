# Merkmale an Anlagenklassen binden — und was das mobil kostet

**Stand:** 2026-08-25 · **Status:** Web fertig, Backend entschärft (Stufe 0), mobile Weggabelung offen

---

## 0. Was dieses Dokument ist

Die Begründung hinter der Kategoriebindung der Anlagen-Merkmale, und die Analyse der Frage,
die sie aufgeworfen hat: **die Android-App des Upstream-Entwicklers kennt die Bindung nicht.**

Abschnitt 3 beschreibt eine bereits umgesetzte Verhaltensänderung im Backend, die man ohne
Kontext für einen Rückschritt halten würde — sie steht hier, damit sie niemand „repariert".
Die Abschnitte 4 bis 6 sind Entscheidungsvorbereitung: was es kostet, die App selbst zu
übernehmen, welche Alternativen es gibt, und was daran nicht offensichtlich ist.

**Einordnung wie in [TECHNICAL_DEBT_REMEDIATION.md](TECHNICAL_DEBT_REMEDIATION.md):** Diese
Instanz ist ein Home-Lab ohne Kundendaten. Jede Empfehlung unten gewichtet laufende
Wartungslast höher als Funktionsumfang — ein Zweig, den niemand pflegt, ist teurer als eine
Funktion, die fehlt.

---

## 1. Das Feature

Custom Fields waren auf Company und Entitätstyp begrenzt. Jede Anlage teilte also einen
flachen Satz Zusatzfelder: „Volumenstrom" erschien am Aufzug, „Anzahl Haltestellen" am
Lüftungsgerät. Brauchbar für Hersteller oder Baujahr, unbrauchbar für typspezifische
Eigenschaften.

Seit `cf7d6f0c` trägt ein Feld eine **n:m-Bindung an Anlagenklassen**. n:m und nicht eine
einzelne Klasse, weil Eigenschaften über Klassen hinweg wiederkehren: „Leistung" in kW gehört
sowohl zum Lüftungsgerät als auch zum Kessel. **Ein Feld ohne Klassen gilt weiterhin für jede
Anlage** — das ist das Verhalten aller Felder vor dem Feature, deshalb brauchte es keine
Datenmigration.

Damit ist aus Klasse + Merkmal ein Klassifikations- und Merkmalsystem geworden: die
Anlagenklasse bestimmt, welche Merkmale erfasst werden.

### Der Vertrag

**Ein gespeicherter Merkmalswert gilt immer für die Klasse seiner Anlage.** Das ist die
Zusage, auf die sich Auswertungen verlassen dürfen, und sie wird serverseitig gehalten, nicht
im Formular — siehe
[`CustomFieldValueService`](../api/src/main/java/com/grash/service/CustomFieldValueService.java).

Zwei Feinheiten, die beim Ändern leicht verloren gehen:

- **Beim Update zählt die Klasse aus dem Patch, nicht die der gespeicherten Anlage.** Werte
  werden geschrieben, bevor der Patch gemappt ist. Gegen die alte Klasse zu prüfen würde genau
  die Felder verwerfen, die der Nutzer beim Klassenwechsel gerade ausgefüllt hat.
- **„Keine Klasse übergeben" ist nicht „Anlage hat keine Klasse".** Die kurze Signatur von
  `setCustomFields` (Standorte, Zähler, Ersatzteile) filtert **gar nicht**; die lange filtert
  auch dann, wenn die Klasse `null` ist. Das spiegelt `undefined` vs. `null` in
  [`customFieldAppliesToCategory`](../frontend/src/content/own/type.ts) im Frontend. Würde der
  Anlagenpfad je auf die kurze Signatur zurückfallen, gingen sonst alle Merkmalswerte still
  verloren, statt nur ungefiltert gespeichert zu werden.

---

## 2. Warum die Android-App das nicht kann

Die Filterung ist **reine Client-Logik**, und zwar an einer Stelle, an die kein Server
heranreicht.

[`mobile/models/form.ts`](../mobile/models/form.ts) baut in `getCustomFieldsIFields()` die
Feldliste aus allem, was `entityType === ASSET` hat, sortiert nach `order`. Eine Klasse kommt
darin nicht vor. `getCustomFieldsRequiredShape()` macht daneben **jedes** Pflichtmerkmal aller
Klassen zur Pflicht.

Die Daten fehlen der App nicht: `CustomFieldShowDTO` liefert `assetCategories` in jeder
Antwort mit. Die App ignoriert sie — ihr TypeScript-Interface in
[`mobile/models/customField.ts`](../mobile/models/customField.ts) kennt das Feld nicht einmal.

**Serverseitig ist das nicht zu heilen.** `GET /custom-fields` ist ein globaler Katalog, den
die App einmal beim Login lädt. Zu diesem Zeitpunkt weiß der Server nicht, welche Anlage der
Nutzer später bearbeitet. Ein Query-Parameter `?assetCategory=` wäre technisch möglich und
nützt nichts, solange kein Client ihn schickt. Wer die Filterung mobil will, muss die App
ändern — es gibt keinen dritten Weg.

---

## 3. Stufe 0 (umgesetzt): verwerfen statt ablehnen

### Das eigentliche Problem war nicht die fehlende Filterung

Die ursprüngliche Durchsetzung warf `406 NOT_ACCEPTABLE`, sobald ein Wert zu einem Feld
gehörte, das nicht zur Klasse passt. Für die Web-App folgenlos — sie sendet ohnehin nur die
IDs der sichtbaren Felder, siehe `formatCustomFields` in
[`frontend/src/utils/formatters.ts`](../frontend/src/utils/formatters.ts).

Für die Android-App war es ein **Totalausfall**, und zwar in einer Schere, aus der der Nutzer
nicht herauskam:

1. Die App zeigt alle Merkmale aller Klassen und **erzwingt** die Pflichtfelder aller Klassen.
2. Der Server lehnt genau diese Felder ab.

Ergebnis: Anlagen ließen sich per App nicht mehr anlegen. Das war kein Komfortmangel, sondern
ein Datenerfassungsstopp — und er entstand als stiller Nebeneffekt einer Web-Verbesserung.

### Die Änderung

Ein Wert zu einem klassenfremden Feld wird jetzt **verworfen statt abgelehnt**. Die restliche
Speicherung läuft durch. Eine unbekannte Feld-ID bleibt ein `404`; das ist ein echter
Client-Fehler und kein Klassenthema.

Der Vertrag aus Abschnitt 1 bleibt unangetastet: der ungültige Wert erreicht die Datenbank
nach wie vor nicht. Was verloren geht, ist allein die laute Rückmeldung. Deshalb protokolliert
der Verwurf auf `WARN` mit Feldlabel und Klasse — ein Client, der dauerhaft Unsinn schickt,
soll im Log sichtbar sein und nicht nur in der Statistik fehlender Werte.

### Was dagegen sprach, und warum es trotzdem so ist

| Alternative | Verworfen, weil |
|---|---|
| Beim Ablehnen bleiben, App später reparieren | Lässt die Erfassung mobil bis zur Fork-Entscheidung stehen. Koppelt eine Betriebsstörung an eine strategische Entscheidung, die Wochen dauern darf. |
| Nur für Nicht-Web-Clients tolerant sein | Der Server kann „alter Client" nicht von „fehlerhafter Client" unterscheiden. Bräuchte eine Client-Kennung — neue API-Oberfläche für einen Sonderfall. |
| Schaltbar per Property | Konfigurationsfläche für eine Entscheidung, die niemand pro Umgebung anders trifft. Nachholbar, falls die strenge Variante je gebraucht wird. |

**Wenn diese Instanz je Mandanten oder fremde API-Clients bekommt, gehört die strenge
Variante zurück** — dann ist ein lautes 406 richtiger als ein stiller Verwurf. Für ein
Home-Lab mit einem Web-Client und einer fremden App ist es andersherum.

Abgesichert in
[`CustomFieldValueServiceTest`](../api/src/test/java/com/grash/service/CustomFieldValueServiceTest.java),
insbesondere `mixedSubmission_keepsApplicableFields` — der Fall, den die App tatsächlich
auslöst: gültige und ungültige Felder in einem Submit, die gültigen müssen überleben.

---

## 4. Die Weggabelung

Stufe 0 stellt die Erfassung wieder her. Sie stellt **nicht** her, dass die App nur passende
Merkmale zeigt. Dafür gibt es drei Wege, und sie unterscheiden sich fast nur in der laufenden
Last:

| | Aufwand einmalig | Laufend | Ergebnis |
|---|---|---|---|
| **A — PWA** | ~1 Tag | keiner | Merkmalsfilter mobil, kein NFC/Barcode/Push/Offline |
| **B — eigener APK-Zweig** | 1–2 Tage | ~½ Tag/Sync, 1–2 Tage/SDK-Upgrade | volle native App unter eigener Kontrolle |
| **C — Upstream-PR** | ~½ Tag | keiner | offen, ob und wann übernommen |

**A und C schließen sich nicht aus, und keiner von beiden schließt B aus.** C ist der
günstigste Versuch überhaupt: das Feature ist generisch nützlich, das Backend liegt fertig
vor, und wenn Grashjs es übernimmt, kommt die Filterung ohne eigenen Zweig in die Store-App.
Ein PR ist zudem AGPL-konform ohnehin die erwartete Richtung.

### Zu A: die PWA ist halb fertig

[`frontend/public/manifest.json`](../frontend/public/manifest.json) existiert, ist auf
`standalone` gestellt und trägt bereits das eigene Branding. Was fehlt: der Service Worker
wird in [`frontend/src/index.tsx`](../frontend/src/index.tsx) bewusst per `unregister()`
abgemeldet, und ohne registrierten Service Worker bietet Chrome auf Android keine Installation
an. Das ist eine Zeile plus die eigentliche Arbeit — eine Cache-Strategie, die nicht bei jedem
Deploy eine veraltete Bundle-Version festhält. CRAs Standard ist Cache-First-Precache, also
genau die Falle.

Wofür A **nicht** reicht: NFC-Tag lesen, Barcode scannen, Push-Benachrichtigungen, Offline-
Erfassung. Wenn Techniker im Feld damit arbeiten sollen, führt kein Weg an B vorbei. Wenn es
darum geht, Merkmale unterwegs zu sehen und zu pflegen, ist A das Richtige.

---

## 5. Falls B — der Playbook

Der Quellcode der App liegt in [`mobile/`](../mobile). Ein eigener Build ist damit möglich,
ohne den Upstream-Entwickler und ohne Expo-Cloud: `mobile/android/` ist **vollständig
eingecheckt** (48 Dateien, Gradle 8.10.2), ein `./gradlew assembleRelease` genügt.

Der Code-Anteil ist der billigste Teil. Erst die Buildkette härten — **in dieser Reihenfolge**,
weil Punkt 1 jedes Testergebnis davor entwertet.

### 5.1 OTA-Kaperung abschalten (zuerst, immer)

[`mobile/android/app/src/main/AndroidManifest.xml`](../mobile/android/app/src/main/AndroidManifest.xml):

```
expo.modules.updates.ENABLED                      = true
expo.modules.updates.EXPO_UPDATES_CHECK_ON_LAUNCH = ALWAYS
expo.modules.updates.EXPO_UPDATE_URL              = https://u.expo.dev/803b5007-…
```

Die Projekt-ID gehört dem Upstream-Entwickler. Eine selbstgebaute APK fragt damit **bei jedem
Start** dessen Expo-Kanal nach einem JS-Bundle und ersetzt bei passender `runtimeVersion`
(`1.0.44`) den eigenen Code zur Laufzeit. Der Fehler ist maximal unangenehm: keine Meldung,
keine Neuinstallation, nicht reproduzierbar — die eigene Änderung ist einfach weg.
Abschalten oder auf einen eigenen Kanal umstellen, bevor irgendetwas anderes passiert.

### 5.2 Eigenes Firebase-Projekt

[`mobile/android/app/build.gradle`](../mobile/android/app/build.gradle) wendet
`com.google.gms.google-services` an, `@react-native-firebase/app` ist Pflicht-Plugin, aber
`android/app/google-services.json` liegt nicht im Repo. Ohne die Datei bricht der Build ab,
ohne eigenes Projekt gibt es keine Push-Benachrichtigungen. Kostenlos, aber ein eigener
Google-Cloud-Kontext, der mitgepflegt werden will.

### 5.3 Eigener Paketname und eigener Keystore

`applicationId = com.atlas.cmms`. Bleibt er, kollidiert die eigene APK mit der Store-App:
nicht parallel installierbar, und wegen abweichender Signatur scheitert jede Installation über
die andere Variante hinweg. Ein eigener Name (z. B. `de.corefm.cmms`) ist sauber, kostet aber
den vollständigen Neuaufbau jeder bestehenden Installation.

**Der Release-Keystore ist der Punkt ohne Rückfahrkarte.** Geht er verloren, lässt sich keine
bestehende Installation mehr aktualisieren — auch beim Sideload nicht, nicht nur im Play
Store. Sichern wie ein Datenbank-Backup, nicht wie eine Build-Datei. Und: **nicht ins Repo**,
das hier ist öffentlich.

### 5.4 Erst dann der Code

Vier Dateien, spiegelbildlich zur Web-Umsetzung. Die kritische Frage — ob das Formular auf
einen Klassenwechsel überhaupt reagieren kann — ist beantwortet, **ohne Eingriff in die
Form-Komponente**: [`mobile/components/form/index.tsx`](../mobile/components/form/index.tsx)
meldet jede Änderung als `props.onChange({ field, e })` nach außen, rendert `props.fields` bei
jedem Durchlauf neu, und `initialValues` steht ohne `enableReinitialize` still. Ein Neuaufbau
der Feldliste von außen verliert also keine Eingaben. Die Klasse kommt als
`{ label, value: id }` an — dieselbe Form wie im Web.

| Web-Vorlage | mobile |
|---|---|
| `customFieldAppliesToCategory` in `content/own/type.ts` | neu in `models/form.ts` |
| `getCustomFieldsIFields`, `getCustomFieldsRequiredShape` | dieselben in `models/form.ts` um `assetCategoryId` erweitern |
| `formatCustomFields(values, applicableIds?)` | zweiter Parameter in `utils/formatters.ts` |
| `selectedCategoryId` in `Assets/index.tsx` | lokaler State in `CreateAssetScreen` und `EditAssetScreen`, gespeist aus `onChange` |
| — | `assetCategories` und `unit` in `models/customField.ts` ergänzen |

`EditAssetScreen` reicht heute **kein** `onChange` durch — dort muss der Hook erst gelegt
werden, mit `asset?.category?.id` als Startwert.

Alle Parameter optional halten. Das ist kein Stilpunkt, sondern die Merge-Strategie aus
Abschnitt 6: additive Signaturen erzeugen bei Upstream-Syncs kaum Konfliktfläche.

---

## 6. Was B laufend kostet

| Kennzahl | Wert (Stand 2026-08-25) |
|---|---|
| Commits in `mobile/`, letzte 3 Monate | 61 |
| letzte 12 Monate | 221 |
| Stack | Expo 53, React Native 0.79.6, React 19 |
| `upstream`-Remote im Repo konfiguriert | **nein** — nur `origin` |

Schätzung: **1–2 Tage Einrichtung**, danach **~½ Tag je Upstream-Sync** und **1–2 Tage je
Expo-SDK-Upgrade** (etwa jährlich). Für den Play Store käme Googles jährliche
`targetSdk`-Pflicht dazu; beim Sideload entfällt sie.

Wer B geht, richtet zusätzlich ein:

- `upstream`-Remote auf `Grashjs/atlas-cmms` und einen festen Sync-Rhythmus,
- einen Eintrag für `mobile/` in der Divergenzliste in [`CLAUDE.md`](../CLAUDE.md),
- den Gradle-Release-Build in CI, damit „baut noch" nicht von einer Arbeitsstation abhängt.

Lizenzrechtlich ist der Weg frei: AGPLv3 deckt Fork und interne Nutzung. Bei Weitergabe der
APK an Dritte greift die Quelltextpflicht — das Repo ist ohnehin öffentlich.

---

## 7. Offene Entscheidung

Die einzige Frage, die alles Weitere bestimmt: **brauchen die Nutzer die App wegen ihrer
nativen Fähigkeiten** (NFC, Barcode, Kamera, Push, Offline) — oder reicht mobiler
Browserzugriff?

- reicht Browserzugriff → **A**, und B entfällt vollständig
- native Fähigkeiten nötig → **B**, nach dem Playbook in Abschnitt 5

**C ist in beiden Fällen sinnvoll** und sollte unabhängig davon versucht werden — der Aufwand
ist gering und ein übernommener PR macht B nachträglich überflüssig.

Bis dahin gilt der Zustand nach Stufe 0: die App erfasst wieder, zeigt aber weiterhin alle
Merkmale. Die Web-App bleibt der Ort für saubere klassenbezogene Erfassung.
