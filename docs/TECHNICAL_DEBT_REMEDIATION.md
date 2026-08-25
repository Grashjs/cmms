# CMMS4FM — Technische Schulden: Befund und Vorgehen

**Stand:** 2026-08-25 · **Status:** **Stufe 1 abgeschlossen** — 145 → 50 Befunde, A5/A6 bewusst zurückgestellt. Nächstes: Stufe 2 (CRA ablösen)

---

## 0. Was dieses Dokument ist — und was nicht

Ein Arbeitsplan für die Altlasten im Frontend-Stack dieses Atlas-CMMS-Forks, plus die
Sicherheitspunkte, die beim Durchsehen aufgefallen sind.

**Einordnung, die alles andere bestimmt:** Diese Instanz ist ein **Home-Lab** — sie dient
dazu, FM-Funktionen durchzudenken und in Beratung und Showcases zu zeigen. Es hängen keine
Kunden und keine produktiven Kundendaten daran. Daraus folgt für jede Empfehlung unten:
**Pragmatik vor Härtung, Funktionserkundung vor Produktionsreife.** Ein Punkt kommt nur auf
die Liste, wenn er entweder eine echte Angriffsfläche schließt oder die tägliche Arbeit am
Code spürbar erleichtert.

> **Zwei Korrekturrunden gegenüber dem ersten Entwurf** (alle am selben Tag). Beide Male
> lag der Schätzwert zu günstig, nie zu pessimistisch — das ist das Muster, nicht der
> Einzelfall.
>
> **Runde 1, gegen den Paketstand gemessen:**
> - **MUI ist vier Major-Versionen zurück, nicht eine** (5.8.2 gegen 9.3.1), Data Grid genauso.
> - **TypeScript 4.7.3 gegen 7.0.2** — ebenfalls deutlich mehr als „eine Major".
> - Die Backend-Versionstabelle war falsch abgelesen: „Hibernate 1.5.5.Final" ist
>   **MapStruct**, „Liquibase 2.0.1" ist **google-cloud-storage**.
> - „Tailwind fehlt" war keine Lücke — MUI *ist* das Design-System hier.
> - Euro-Beträge und Kalenderwochen: erfunden, gestrichen.
>
> **Runde 2, nach dem tatsächlichen `npm audit` (Abschnitt 3):**
> - **145 Befunde, davon 12 critical und 64 high.** Ich hatte keine Zahl und hätte auch
>   keine so hohe geraten.
> - **CRA stand als „nicht der Engpass" auf der Nicht-Liste. Das war der teuerste Fehler
>   im Dokument.** `react-scripts` ist mit 11 hohen Befunden die zweitgrößte Quelle — und
>   für keinen davon gibt es eine Fassung, weil CRA nicht mehr gepflegt wird. Der
>   CRA-Ausstieg ist damit von „nice to have" zu **Stufe 2** aufgerückt.

---

## 1. Bereits erledigt: der Default-Superadmin

**Nichts zu tun — der Punkt ist abgehakt, aber der Grund gehört festgehalten.**

Upstream legt beim ersten Start `superadmin@test.com` mit dem Passwort `pls_change_me` an.
Beides steht im öffentlichen Quelltext, und `/auth/signin` ist von der nginx-Sperre nicht
erfasst. Auf der laufenden Instanz ist dieses Konto **gesperrt** (`enabled = false`) — das
ist die richtige Maßnahme, sie hält über Neustarts, und `CustomUserDetail.isEnabled()` wird
bei der Anmeldung geprüft, ein gesperrtes Konto kommt also nicht durch.

**Das Konto darf nicht gelöscht werden.** Die Bedingung im `ApplicationInitializer` ist
nicht „gibt es diese E-Mail", sondern `userService.findByCompany(<Superadmin-Firma>).isEmpty()`.
Das Konto wird also mit Standardpasswort neu angelegt, sobald die Superadmin-Firma gar keine
Nutzer mehr hat. Zwei Folgen: Löschen holt es mit `pls_change_me` zurück — und falls dort
noch ein anderer Nutzer sitzt, kommt es *nicht* zurück und die Instanz steht ohne Superadmin da.

**Was daran offen bleibt:** Die Absicherung liegt in den Daten, nicht im Quelltext. Eine
frische Datenbank oder eine Rücksicherung von vor der Sperrung bringt die Lücke ab dem
ersten Start zurück, lautlos und ohne Logzeile. Deshalb steht in Abschnitt 5 mit **A1** ein
Code-Fix, der diesen Zustand unmöglich macht — und bis dahin gilt die Prüfung nach jeder
Rücksicherung:

```sql
SELECT id, email, enabled FROM users WHERE email = 'superadmin@test.com';
-- erwartet: enabled = false
```

Hintergrund steht auch in der [`CLAUDE.md`](../CLAUDE.md) („Known upstream issue: the
default super admin"); der Upstream-Leitfaden `dev-docs/SuperAdmin password update guide.md`
hat einen Hinweis bekommen, dass sein Weg auf dieser Instanz absichtlich nicht funktioniert.

---

## 2. Ausgangslage

484 Frontend-Dateien (`.ts`/`.tsx`), 836 Java-Dateien.

### Backend — unauffällig

| Baustein | Version | Bewertung |
|---|---|---|
| Spring Boot | 3.2.3 | aktuell genug, 3.4 wäre möglich, drängt nicht |
| Java | 17 | LTS |
| Liquibase | 4.22.0 | in Ordnung |
| MapStruct | 1.5.5.Final | in Ordnung |
| Lombok | 1.18.30 | in Ordnung |
| google-cloud-storage | 2.0.1 | **von 2021** — einzige echte Altlast im Backend, aber ungenutzt (hier läuft MinIO) |

Das Backend ist nicht das Problem.

### Frontend — der Rückstand

| Paket | im Projekt | aktuell | Abstand |
|---|---|---|---|
| `react` / `react-dom` | **17.0.2** | 19.2.8 | 2 Major |
| `@mui/material` | **5.8.2** | 9.3.1 | **4 Major** |
| `@mui/x-data-grid` | **5.17.3** | 9.12.0 | **4 Major** |
| `typescript` | **4.7.3** | 7.0.2 | ~3 Major |
| `firebase` | 9.8.2 | 12.18.0 | 3 Major |
| `swiper` | 8.2.2 | 14.1.0 | 6 Major |
| `react-router-dom` | 6.3.0 | 7.18.2 | 1 Major |
| `@reduxjs/toolkit` | 1.8.2 | 2.12.0 | 1 Major |
| `react-redux` | 8.0.2 | 9.3.0 | 1 Major |
| `date-fns` | 2.28.0 | 4.4.0 | 2 Major |
| `axios` | 0.27.2 | 1.19.0 | 1 Major |
| `react-scripts` (CRA) | 5.0.1 | 5.0.1 | „aktuell", weil eingefroren — CRA wird nicht mehr gepflegt |

### Wie tief sitzt das jeweils?

Zahl der Dateien in `src/`, die das Paket anfassen — das entscheidet über den Aufwand:

| Paket | Dateien | Bedeutung |
|---|---|---|
| `@mui/material` | **262** | das Design-System — jede MUI-Major fasst mehr als die Hälfte der Oberfläche an |
| `formik` | 26 | alle Formulare |
| `@mui/x-data-grid` | 15 | alle Tabellen |
| `@mui/lab` | 9 | vereinzelt |
| `axios` | 5 | zentral gekapselt, gut |
| `react-beautiful-dnd` | 5 | verwaist bei Atlassian, `@hello-pangea/dnd` ist der Fork |
| `swiper` | 4 | davon 3 nur eine CSS-Variable im Theme; echte Nutzung nur auf der Registrierungsseite |
| `firebase` | 4 | Push-Benachrichtigungen |
| `@mui/styles` | **2** | siehe unten |
| `xlsx` | **1** | `content/own/Imports/index.tsx` (`read`, `utils`) |
| `react-google-maps` | **1** | `content/own/components/Map/index.tsx` |

**Die interessanten Zeilen stehen unten.** `@mui/styles` ist die alte JSS-Schicht; sie ist
abgekündigt und **unterstützt React 18 nicht**. Sie ist der eigentliche Riegel vor dem
React-Upgrade — und sie steckt in genau **zwei** Dateien (`theme/ThemeProvider.tsx`,
`content/own/components/form/SelectTasks/DraggableTask.tsx`). Der Riegel ist also billig zu
entfernen. Im ersten Entwurf hatte ich ihn übersehen und wäre beim Upgrade hineingelaufen.

### Tote Abhängigkeiten

In `package.json` deklariert, in `src/` **null** Verwendungen:

| Paket | Anmerkung |
|---|---|
| `aws-amplify@4.3.24` | vier Jahre alter Abhängigkeitsbaum. Die einzigen „Amplify"-Treffer im Code sind ein Logo-Bild auf einer Template-Loginseite |
| `react-quill@2.0.0-beta.4` | Beta-Version, nirgends importiert |
| `react-simple-maps` + `@types/react-simple-maps` | nirgends importiert |

---

## 3. Der Schwachstellenbefund

`npm audit` im Ordner `frontend`, Stand 2026-08-25:

```
Ausgangsmessung:  145 Befunde: 12 critical · 64 high · 50 moderate · 19 low
nach A2 + A3:      50 Befunde:  2 critical · 25 high · 10 moderate · 13 low
```

Die folgende Aufschlüsselung beschreibt die **Ausgangslage** — sie erklärt, warum A2 und A3
so viel abgeräumt haben. Was danach übrig blieb, steht am Ende von Abschnitt 5.

Die Gesamtzahl ist wenig aussagekräftig — entscheidend ist, **welches direkte Paket** die
Befunde hereinzieht, denn danach richtet sich die Arbeit. Aufgelöst über die
Abhängigkeitsketten:

| direktes Paket | critical | high | in `src/` benutzt? | Weg |
|---|---:|---:|---|---|
| `aws-amplify` | 0 | **11** | **nein** | entfernen — kostenlos |
| `react-scripts` (CRA) | 0 | **11** | Build-Werkzeug | **keine Fassung verfügbar** — nur CRA-Ausstieg hilft |
| `react-google-maps` | 0 | **5** | 1 Datei | Paket seit ~2018 verwaist, ersetzen |
| `firebase` | 1 | 4 | 4 Dateien | Major 9 → 12 |
| `@grpc/grpc-js` | 1 | 2 | transitiv über firebase | mit firebase |
| `express` | 0 | 3 | Build-Kette | `npm audit fix` |
| `swiper` | **1** | 0 | 1 echte Datei | Major 8 → 14 |
| `xlsx` | 0 | 1 | 1 Datei | **keine Fassung verfügbar** — Entscheidung nötig |
| Rest (ca. 25 Pakete) | 9 | ~25 | transitiv | `npm audit fix`, ohne Breaking Changes |

Drei Dinge fallen daran auf:

**Sechzehn hohe Befunde hängen an Code, den niemand benutzt.** `aws-amplify` (11) ist tot,
`react-google-maps` (5) sitzt in einer einzigen Datei und ist ein seit Jahren aufgegebenes
Paket, das hier noch die alte HOC-Schnittstelle (`withGoogleMap`, `withScriptjs`) benutzt.
Das ist der billigste Teil des ganzen Vorhabens.

**Zehn der zwölf kritischen Befunde sind ohne Breaking Change zu erledigen** — sie hängen
an transitiven Paketen, für die `npm audit fix` eine Fassung kennt. Die verbleibenden zwei
brauchen einen Major-Sprung (`firebase`, `swiper`).

**Elf hohe Befunde sind gar nicht zu erledigen, solange CRA im Haus ist.** `@svgr/*`,
`svgo`, `css-select`, `nth-check`, `postcss`, `serialize-javascript`, `rollup-plugin-terser`,
`workbox-*` — alle stecken in `react-scripts@5.0.1`, und npm meldet als Fassung
`react-scripts@0.0.0`, was in dieser Ausgabe bedeutet: es gibt keine. CRA ist eingefroren.
Diese elf Befunde bleiben, bis das Build-Werkzeug gewechselt wird.

### Was noch nicht verifiziert ist

Der Zugriff auf die `users`-Tabelle der Instanz wird mir vom Auto-Mode-Classifier verweigert
(SSH selbst geht über Tailscale). Der `enabled`-Wert des Superadmins ist daher **aus
Quelltext und Projektnotizen abgeleitet, nicht gemessen**. Befehl in A0.

---

## 4. Vorgehen in Stufen

Vier Stufen, jede für sich abschließbar. Es gibt keinen Zwang, die nächste anzufangen, nur
weil die vorige fertig ist.

| Stufe | Inhalt | Aufwand (grob) | räumt auf | Empfehlung |
|---|---|---|---|---|
| **1 — Aufräumen** | tote Pakete raus, `npm audit fix`, Superadmin im Code, `@mui/styles`, Karte, `xlsx`-Entscheidung | 1–2 Tage | ~10 critical, ~40 high | **jetzt** |
| **2 — CRA ablösen (Vite)** | Build-Werkzeug wechseln, React bleibt vorerst 17 | 3–5 Tage | 11 high, sonst unerreichbar | **danach** |
| **3 — React 17 → 18/19** | nach Stufe 1 entriegelt; dazu TypeScript, firebase, swiper, die Ein-Major-Pakete | 1–2 Wochen | 2 critical, Rest high | wenn Ruhe ist |
| **4 — MUI 5 → 9** | vier Majors über 262 Dateien | mehrere Wochen | nichts sicherheitsrelevantes | **nicht** ohne konkreten Anlass |

Die Reihenfolge ist nicht beliebig. Stufe 1 macht die Abhängigkeitsliste erst überschaubar
— danach ist sichtbar, was wirklich übrig bleibt. Stufe 2 vor Stufe 3, weil ein
Werkzeugwechsel bei unverändertem React eine saubere Fehlersuche erlaubt: was danach kaputt
ist, liegt am Build und an nichts anderem. Beides gleichzeitig zu ändern, macht jeden Fehler
doppelt teuer.

**Stufe 4 ist ausdrücklich nicht empfohlen.** Vier MUI-Majors über 262 Dateien lösen kein
Sicherheitsproblem und sind für ein Home-Lab kein sinnvoller Einsatz. Der Grund, es doch zu
tun, wäre eine Funktion aus einer neueren MUI-Fassung, die für einen Showcase gebraucht wird
— dann aber als bewusste Entscheidung, nicht als Aufräumarbeit.

---

## 5. Stufe 1 im Detail

### A0 — Messen ✅ teilweise erledigt

`npm audit` ist gelaufen, Ergebnis in Abschnitt 3. Offen bleibt die Prüfung auf der Instanz
(mir vom Classifier verweigert, Container-Namen über `docker ps` auflösen):

```bash
ssh -i ~/.ssh/id_plgrnd.pem root@<host>
docker exec <postgres-container> psql -U cmms_admin -d atlas \
  -c "SELECT id, email, enabled FROM users WHERE email = 'superadmin@test.com';"
```

Erwartung: `enabled = false`. Falls `true` oder die Zeile fehlt, hat Stufe 1 eine andere
erste Aufgabe als hier geplant.

### A1 — Superadmin auch im Code entschärfen ✅ erledigt

`getSuperAdminSignupRequest` erzeugt jetzt ein zufälliges Passwort (`UUID.randomUUID()`) und
schreibt es **einmalig als WARN** ins Log. Eine frische Datenbank ist damit nicht mehr aus dem
öffentlichen Quelltext erreichbar. Die Zeile steht als eigener Eintrag in der
Upstream-Merge-Tabelle der `CLAUDE.md` — ohne den würde der nächste Abgleich sie kommentarlos
überschreiben.

Auf der bestehenden Instanz ändert sich **nichts**: das Konto existiert dort, also läuft der
Zweig gar nicht erst an.

#### ursprüngliche Beschreibung

Ziel ist nicht, die laufende Instanz zu ändern (die ist in Ordnung), sondern dass eine
frische Datenbank nicht wieder mit einem bekannten Passwort startet.

Datei: `api/src/main/java/com/grash/ApplicationInitializer.java`, Methode
`getSuperAdminSignupRequest`. Statt `pls_change_me` ein zufälliges Passwort erzeugen und
**einmalig als Warnung loggen**:

```java
String initialPassword = UUID.randomUUID().toString();
signupRequest.setPassword(initialPassword);
log.warn("Default super admin created with a generated password: {} " +
         "— sign in once, change it, then disable the account.", initialPassword);
```

Warum loggen statt still erzeugen: sonst steht man vor einer frischen Instanz ohne jeden
Zugang. Warum `warn`: es soll auffallen.

`ApplicationInitializer` steht auf der Upstream-Merge-Liste in der `CLAUDE.md`. Diese
Änderung gehört dort ergänzt, sonst wird sie beim nächsten Abgleich stillschweigend
überschrieben.

**Prüfen:** frische Datenbank hochziehen, Log auf die Warnung ansehen, mit dem geloggten
Passwort anmelden, danach sperren. Auf der bestehenden Instanz darf sich **nichts** ändern —
das Konto existiert dort, der Zweig läuft also gar nicht erst an.

### A2 — Tote Pakete entfernen ✅ erledigt

```bash
npm uninstall aws-amplify react-quill react-simple-maps @types/react-simple-maps
```

**Ergebnis: 145 → 103 Befunde** — ein kritischer, zehn hohe und achtundzwanzig mittlere
weniger, ohne dass eine Zeile Anwendungscode angefasst wurde. Der Rückgang bei den mittleren
war nicht eingeplant; die Schätzung lautete „elf hohe" und traf, der Rest kam obendrauf.

Die einzigen „Amplify"-Treffer im Code sind Logo-Bilder auf den Template-Seiten für Anmeldung
und Registrierung (`content/pages/Auth/…`). Die zeigen ein `amplify.svg` an, importieren aber
nichts aus dem Paket — der Verweis bleibt stehen.

### A2b — Phantom-Abhängigkeit `buffer` sichtbar geworden ✅ erledigt

**A2 hat den Build zerlegt, und das war nützlich.** `src/utils/jwt.ts` macht in Zeile 2
`import { Buffer } from 'buffer'` — und `buffer` stand **nie in `package.json`**. Es kam als
Beifang über `aws-amplify` herein. Solange das tote Paket dalag, funktionierte der Import;
mit dem Aufräumen war er weg:

```
Module not found: Error: Can't resolve 'buffer' in .../frontend/src/utils
```

Behoben durch Deklarieren, nicht durch Zurückrollen — der Code braucht das Paket wirklich,
er hat es nur nie gesagt:

```bash
npm install buffer      # ^6.0.3
```

Anschließend ein Scan über alle Importe in `src/` gegen `package.json`, um zu sehen, ob noch
weitere solcher Minen liegen. Es gibt drei, aber keine davon ist scharf:

| Import | kommt über | Risiko |
|---|---|---|
| `@emotion/cache` | `@mui/material` | erst beim MUI-Major (Stufe 4) |
| `@mui/types` | `@mui/material` | erst beim MUI-Major (Stufe 4) |
| `react-router` | `react-router-dom` | erst beim Router-Major (Stufe 3) |

Alle drei werden von Paketen geliefert, die deklariert sind und bleiben — sie brechen erst,
wenn genau diese Pakete ihren Abhängigkeitsbaum ändern. **Vor Stufe 3 und Stufe 4 gehören sie
deklariert**, sonst tritt derselbe Effekt mitten in einem Upgrade auf, wo er schwerer zu
deuten ist.

### A3 — `npm audit fix` ohne Breaking Changes ✅ erledigt

```bash
npm audit fix          # ohne --force
```

**Ergebnis: 103 → 50 Befunde.** Zusammen mit A2 also **145 → 50**:

| | vorher | nachher |
|---|---:|---:|
| critical | 12 | **2** |
| high | 64 | **25** |
| moderate | 50 | 10 |
| low | 19 | 13 |

Zehn der zwölf kritischen Befunde sind weg, wie vorhergesagt. **In `package.json` hat sich
dabei keine einzige direkte Abhängigkeit verschoben** — der Eingriff blieb vollständig im
Lockfile, bei den transitiven Paketen. Genau deshalb ohne `--force`: das hätte
`react-scripts`, `firebase`, `swiper` und `axios` gleichzeitig gesprungen, und bei einem
Fehler wäre nicht mehr zu sagen gewesen, welcher der vier schuld ist.

`CI=true npm run build` läuft grün durch.

> **Falle beim Prüfen des Builds.** `npm run build | tail` liefert den Exit-Code von `tail`,
> nicht den des Builds — ein fehlgeschlagener Build meldet so eine 0. Genau das ist hier
> einmal passiert und hätte den `buffer`-Fehler fast verdeckt. Entweder `set -o pipefail`
> setzen und `${PIPESTATUS[0]}` auswerten, oder die Ausgabe wirklich lesen. Das ist dieselbe
> Klasse Fehler wie `mvn compile` in Abschnitt 6.

### Rest nach A2/A3 — wer die verbliebenen 50 hereinzieht

| direktes Paket | critical | high | wo behandelt |
|---|---:|---:|---|
| `react-scripts` (CRA) | 0 | **11** | Stufe 2 — keine Fassung verfügbar |
| `react-google-maps` | 0 | **5** | **A5, offen** |
| `firebase` | 1 | 4 | Stufe 3 (Major 9 → 12) |
| `swiper` | **1** | 0 | Stufe 3 (Major 8 → 14) |
| `axios` | 0 | 1 | Stufe 3 (Major 0.27 → 1.x) |
| `jsonwebtoken` | 0 | 1 | Stufe 3 (Major 8 → 9) |
| `xlsx` | 0 | 1 | **A6, offen** — keine Fassung verfügbar |
| `d3-color` | 0 | 1 | über recharts; `audit fix` greift nicht, braucht recharts-Sprung |
| `ws` | 0 | 1 | unter `selenium-webdriver` — **nur Testwerkzeug, geht nie in den Build** |

Damit ist die Lage sortiert: von den 25 verbliebenen hohen Befunden hängen 11 an CRA
(Stufe 2), 5 an der Karte (A5), 8 an Majors (Stufe 3), und einer betrifft nur die Testkette.

### A4 — `@mui/styles` ablösen ✅ erledigt

**Der Riegel vor React 18 ist weg.** Zwei Dateien, wie erwartet:

- `src/theme/ThemeProvider.tsx` — `StylesProvider injectFirst` ersatzlos entfernt. Sein Zweck
  war die Reihenfolge der Stilinjektion für die JSS-Schicht aus MUI v4; unter v5 erledigen das
  die beiden Emotion-Caches, die ohnehin schon mit `prepend: true` angelegt werden.
- `src/content/own/components/form/SelectTasks/DraggableTask.tsx` — `makeStyles` durch die
  `sx`-Prop ersetzt. Der einzige Stil war ein Hintergrund während des Ziehens; er hängt jetzt
  direkt an der `ListItem`, die ohnehin schon ein `sx` trug.

Danach `npm uninstall @mui/styles`. **Optisch prüfen** (ein grüner Build beweist hier wenig):
Theme-Umschaltung hell/dunkel, RTL-Sprache, und Drag-and-Drop im Checklisten-Editor — der
graue Hintergrund muss beim Ziehen noch erscheinen.

#### ursprüngliche Beschreibung

Zwei Dateien, und danach ist der Riegel vor React 18 weg:

- `src/theme/ThemeProvider.tsx` — `StylesProvider` ersatzlos entfernen; MUI v5 braucht ihn
  nicht mehr, er ist Rest aus der v4-Zeit.
- `src/content/own/components/form/SelectTasks/DraggableTask.tsx` — `makeStyles` durch die
  `sx`-Prop oder `styled()` aus `@mui/material` ersetzen.

**Prüfen:** beide Oberflächen ansehen — Theme-Umschaltung hell/dunkel und die Aufgabenliste
mit Drag-and-Drop im Checklisten-Editor. Diese Änderung ist optisch, ein grüner Build
beweist hier wenig.

### A5 — Die Karte auf ein gepflegtes Paket stellen ⏸ bewusst zurückgestellt

**Entscheidung (Lars, 2026-08-25): bleibt wie sie ist.** Begründung: die Kartenfunktion ist
im laufenden Betrieb nicht bekannt und damit nicht beurteilbar. In den Stammdaten gibt es
Felder für Geodaten — möglicherweise hat Upstream hier etwas vorbereitet, das später gebraucht
wird. Etwas zu entfernen, dessen Zweck man nicht kennt, ist der teurere Fehler.

**Preis dieser Entscheidung: fünf hohe Befunde bleiben stehen.** Das ist vertretbar, weil
`GOOGLE_KEY` auf der Instanz leer ist — die Komponente lädt ohne Schlüssel gar keine
Google-Ressourcen, die Angriffsfläche ist also weitgehend theoretisch.

**Wenn die Karte je gebraucht wird**, ist der Weg das Ersetzen durch
`@vis.gl/react-google-maps` (das offiziell empfohlene Nachfolgepaket), nicht das Aktivieren
des alten. Dann fallen die fünf Befunde mit weg.

#### ursprüngliche Beschreibung

`react-google-maps` bringt **fünf hohe Befunde** und wird seit etwa 2018 nicht mehr gepflegt;
`content/own/components/Map/index.tsx` benutzt noch die alte HOC-Schnittstelle
(`withGoogleMap`, `withScriptjs`). Eine Datei.

Zwei Wege, und die Entscheidung gehört zuerst getroffen:

- **Ersetzen** durch `@vis.gl/react-google-maps` (das offiziell empfohlene Nachfolgepaket) —
  eine Datei umschreiben, Kartenfunktion bleibt.
- **Entfernen**, falls die Karte ohnehin nichts zeigt.

**Nachgesehen: `GOOGLE_KEY` ist auf der Instanz leer.** Die Karte kann dort also gar nichts
darstellen — `googleMapsConfig.apiKey` bleibt undefiniert. Damit spricht viel für Entfernen:
fünf hohe Befunde für den Preis einer Komponente, die nichts anzeigt.

Betroffen wären zwei Stellen, das ist der Umfang der Entscheidung:

- `content/own/Locations/index.tsx` — Kartenansicht der Standorte
- `content/own/components/form/SelectMapCoordinates.tsx` — Koordinatenauswahl im Formular

Die zweite ist der Haken: ohne sie lassen sich Koordinaten nur noch von Hand eintragen. Wer
die Karte irgendwann *doch* nutzen will, fährt mit dem Ersetzen besser. **Das ist eine
Produktentscheidung, keine technische** — sie gehört getroffen, bevor jemand Code anfasst.

### A6 — `xlsx`: Entscheidung, keine Fassung ⏸ bewusst zurückgestellt

**Entscheidung (Lars, 2026-08-25): bleibt wie es ist.** Damit gilt Weg (3) aus der Liste
unten, und die Begründung dafür gehört festgehalten, damit beim nächsten Audit niemand neu
darüber nachdenkt:

Die betroffene Stelle (`content/own/Imports/index.tsx`) verarbeitet ausschließlich Dateien,
die ein angemeldeter Betreiber selbst hochlädt — es gibt keinen Pfad, auf dem ein Fremder
eine Tabelle einschleust. Für ein Home-Lab ohne Kundendaten ist das vertretbar. **Sobald
diese Instanz je fremde Uploads entgegennimmt, ist die Entscheidung hinfällig** und Weg (1)
— `exceljs`, das im Bestand ohnehin schon läuft — wird fällig.

#### ursprüngliche Beschreibung

`xlsx` hat zwei bekannte Schwachstellen (Prototype Pollution, ReDoS) und **npm kennt keine
Fassung** — SheetJS hat npm verlassen und liefert nur noch über die eigene Adresse aus.
Benutzt wird es in genau einer Datei: `content/own/Imports/index.tsx` liest damit
hochgeladene Tabellen (`read`, `utils`).

Drei mögliche Antworten, eine davon muss bewusst gewählt und hier notiert werden:

1. **Auf `exceljs` wechseln.** Gepflegt, reines JS — und im Bestand bereits im Einsatz
   (prozess-hub benutzt es für den Excel-Export). Eine Datei, und ein Paket weniger, das nur
   dieses eine Projekt kennt.
2. **SheetJS von der Herstelleradresse beziehen** statt aus npm. Behebt die Befunde, führt
   aber eine Bezugsquelle außerhalb der Registry ein.
3. **Stehen lassen und begründen.** Für ein Home-Lab vertretbar: die Datei verarbeitet
   ausschließlich Dateien, die der Betreiber selbst hochlädt. Dann gehört genau dieser Satz
   hierher, damit beim nächsten Audit niemand neu darüber nachdenkt.

Empfehlung: (1), wegen der Konsistenz mit dem übrigen Bestand.

### A7 — LDAP-Fehler nicht als „falsches Passwort" ausgeben ✅ erledigt

**Nachgesehen: `LDAP_ENABLED=false` auf der Instanz.** Nach der ursprünglichen Regel unten
wäre der Punkt damit zu streichen gewesen. Trotzdem gemacht, weil der Fix vier Zeilen groß ist
und ein Muster spiegelt, das im selben Code schon steht — und weil genau dieser Fehler bei
`signin` laut `CLAUDE.md` schon einmal Stunden gekostet hat. Vorsorglich, nicht dringend.

`LdapService.signinLdap` fing bisher `AuthenticationException` in einem Block und antwortete
immer mit 403. `InternalAuthenticationServiceException` erbt davon, und Spring verpackt darin
**alles**, was beim Erreichen des Verzeichnisses schiefgeht — nicht erreichbarer Server,
abgelehnter Bind, DNS-Fehler. Als 403 gelesen heißt das „falsches Passwort". Jetzt wie bei
`signin`: Infrastruktur bekommt **503**, nur eine echte Ablehnung bekommt 403.

#### ursprüngliche Beschreibung

Kein Neufund, sondern der offene Punkt aus der `CLAUDE.md`: `signinLdap` verdichtet immer
noch jeden Fehlschlag zu einer Meldung — genau die Falle, die bei `signin` schon behoben
wurde und dort Stunden gekostet hat. Ein nicht erreichbarer LDAP-Server muss 503 mit eigener
Meldung ergeben, nicht 403 „falsche Zugangsdaten".

Nur sinnvoll, wenn LDAP hier überhaupt benutzt wird. Falls nicht: Punkt streichen, nicht
aufheben.

### A8 — Versionen festnageln (`overrides`) ✅ erledigt — Ergebnis: **keine Override**

Die Prüfung hat gezeigt, dass hier keine Override gehört, und das ist ein Ergebnis, kein
Ausweichen. Nach A2/A3 blieben zwei transitive Befunde übrig, die `npm audit` als „behebbar"
meldet:

**`ws`** liegt unter `selenium-webdriver` — reines Testwerkzeug, geht nie in einen Build.
Eine Override dafür wäre Lärm.

**`d3-color@1.4.1`** sah zunächst nach einem klaren Fall aus: behoben in 3.1.0, also
festnageln. Zwei Funde sprechen dagegen:

1. **Die Override wäre unsicher.** `d3-color@3.1.0` ist ESM-only (`"type": "module"`), `1.4.1`
   hatte noch einen CommonJS-Einstieg. Die Konsumenten hier erwarten CommonJS — die Override
   würde die Modulgrenze überschreiten und vermutlich zur Laufzeit brechen, nicht beim Build.
   Genau die Falle, vor der der ursprüngliche Text unten warnt.
2. **Eine der beiden Wurzeln war tot.** `d3-color` kam über `react-gauge-chart@0.4.0`, das ein
   uraltes `d3@5` mitschleppt — und `react-gauge-chart` wird in `src/` **nirgends** benutzt.
   Ebenso `react-sparklines`: der einzige Treffer ist eine Menü-Beschriftung im Seitenleisten-
   Template, kein Import.

```bash
npm uninstall react-gauge-chart @types/react-gauge-chart react-sparklines   # −31 Pakete
```

**Der Befund bleibt trotzdem stehen**, denn die zweite Wurzel ist `recharts@2.2.0`, und das
wird in 31 Dateien wirklich benutzt. Der Weg dahin ist ein Sprung auf eine neuere recharts-2.x
— ein *Minor*, aber eine direkte Abhängigkeit, und A3 hat bewusst keine einzige davon bewegt.
**Gehört nach Stufe 3**, zusammen mit den anderen Versionssprüngen.

Die 31 entfernten Pakete ändern die Befundzahl also nicht, verkleinern aber Baum und
Installationszeit — und sie nehmen eine Altlast mit, die beim nächsten Upgrade im Weg gestanden
hätte.

#### ursprüngliche Beschreibung

Zum Schluss, nicht zu Beginn: erst nach A2/A3 ist sichtbar, was *tatsächlich* als transitives
Problem übrig bleibt. Dann in `frontend/package.json` gezielt festnageln, analog zum übrigen
Bestand:

```json
"overrides": {
  "lodash": "4.17.21"
}
```

Eine Override auf Verdacht ist eine Falle: sie friert eine Version ein, die danach niemand
mehr prüft. Nach dem Setzen mit `npm ls <paket>` gegenprüfen, dass sie greift.

---

## 6. Prüfregeln für diese Codebasis

Hier sind schon Prüfungen ins Leere gelaufen; das steht hier, damit es nicht noch einmal
passiert.

- **Frontend niemals mit `tsc --noEmit` prüfen.** TypeScript 4.7.3 scheitert an den
  i18next-Typdefinitionen und bricht mit rund 88 Fehlern in `node_modules` ab, *bevor*
  `src/` überhaupt drankommt. Eine leere Fehlerliste für `src/` ist dann keine Entwarnung,
  sondern eine ausgefallene Prüfung. Maßgeblich ist `CI=true npm run build` im Ordner
  `frontend` (CRA behandelt dabei Warnungen wie Fehler, wie die Pipeline). Kostet rund drei
  Minuten.
- **`mvn compile` ist keine Prüfung.** Ohne `clean` lässt der inkrementelle Durchlauf
  geänderte Dateien unübersetzt und endet trotzdem mit 0. `mvn clean package -DskipTests`
  oder nichts.
- **Lokale Backend-Tests sind hier nur eingeschränkt brauchbar.** Auf dem Rechner liegt
  JDK 25; ByteBuddy/Mockito des Projekts kommt damit nicht zurecht und wirft hunderte
  Fehler, die nichts mit der Änderung zu tun haben. Maßgeblich ist CI auf JDK 17.
  `mvn test-compile` lohnt trotzdem lokal.
- **Zugriffe auf die `users`-Tabelle** blockt der Auto-Mode-Classifier, auch lesend. Solche
  Schritte gehören als Copy-Paste-Block formuliert, nicht umgangen.
- **Es arbeiten teils parallele Agents im selben Repo.** Vor jedem Commit `git status`
  ansehen und fremde Änderungen thematisch getrennt committen, nicht mit den eigenen mischen.

---

## 7. Fertig ist Stufe 1, wenn

- [~] `npm audit` meldet **keine** *critical*-Befunde mehr — *2 übrig (firebase, swiper), beide brauchen einen Major, verschoben nach Stufe 3*
- [ ] Von den 64 hohen Befunden sind nur noch die elf aus `react-scripts` übrig — die
      bleiben bis Stufe 2, und dass sie bleiben, ist hier vermerkt
      *(Stand: 25 übrig — 11 CRA, 5 Karte/A5, 8 Majors, 1 nur Testkette)*
- [x] `aws-amplify`, `react-quill`, `react-simple-maps` sind aus `package.json` verschwunden
- [x] `ApplicationInitializer` legt kein Konto mehr mit einem im Quelltext stehenden
      Passwort an; die Änderung steht in der Upstream-Merge-Tabelle der `CLAUDE.md`
- [x] `@mui/styles` kommt in `src/` nicht mehr vor — Paket ebenfalls deinstalliert
- [x] Für `react-google-maps` und `xlsx` ist entschieden **und hier notiert**: beide bleiben
      vorerst, Begründungen in A5 und A6
- [x] `CI=true npm run build` im Ordner `frontend` läuft durch *(nach dem Nachziehen von `buffer`, siehe A2b)*
- [ ] Die Maven-Suite ist in CI grün (nicht lokal — siehe Abschnitt 6) — *lokal läuft
      `clean package -DskipTests` durch; die Tests selbst kann nur CI auf JDK 17 beurteilen*
- [ ] **Von Hand angesehen** (steht noch aus, siehe A4): Anmeldung, Anlagenliste,
      Arbeitsauftrag anlegen und bearbeiten, Einstellungen, **Theme-Umschaltung hell/dunkel**,
      **RTL-Sprache**, **Checklisten-Drag-and-Drop**, Tabellen-Import, Kartenansicht
- [ ] Der Superadmin ist auf der Instanz weiterhin gesperrt (A0-Abfrage)

---

## 8. Zurückrollen

Stufe 1 fasst kein Datenbankschema an, ein Rückbau ist deshalb reines Zurücksetzen des Codes:

```bash
git revert <commit>
git push origin main
# CI baut ein neues Image, Coolify zieht es
```

Alternativ in Coolify `IMAGE_TAG` auf ein `sha-<commit>` von vorher setzen und neu ausrollen —
der schnellere Weg, wenn es eilt.

---

## 9. Bewusst nicht auf der Liste

Damit später niemand denkt, es sei übersehen worden:

- **Tailwind einführen.** MUI ist das Design-System dieses Forks. Ein zweites Styling-System
  danebenzustellen kostet mehr, als es bringt.
- **`google-cloud-storage` im Backend aktualisieren.** Alt, aber ungenutzt (hier läuft MinIO).
- **`formik` ablösen.** Nur ein Patch-Abstand (2.2.9 gegen 2.4.9), keine Befunde, 26 Dateien.
  Das Projekt lebt langsam, aber es lebt. Kein Anlass.
- **Der Rest der Upstream-Befunde** aus der `CLAUDE.md` — etwa dass
  `POST /work-orders/search` ohne Ansichtsrecht die ganze Firma zurückgibt. Das ist ein
  echter Fehler und gehört behoben, ist aber ein Fachthema und keine technische Schuld; er
  bleibt dort verzeichnet, wo er hingehört.

**Nicht mehr auf dieser Liste:** der CRA-Ausstieg. Er stand hier mit der Begründung, CRA
funktioniere und sei nicht der Engpass. Das erste stimmt, das zweite nicht — elf hohe
Befunde ohne verfügbare Fassung sind ein Engpass. Er ist jetzt Stufe 2.
