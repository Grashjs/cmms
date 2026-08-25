# CMMS4FM — Technische Schulden: Befund und Vorgehen

**Stand:** 2026-08-25 · **Status:** abgestimmter Plan, Stufe 1 offen

---

## 0. Was dieses Dokument ist — und was nicht

Ein Arbeitsplan für die Altlasten im Frontend-Stack dieses Atlas-CMMS-Forks, plus die
Sicherheitspunkte, die beim Durchsehen aufgefallen sind.

**Einordnung, die alles andere bestimmt:** Diese Instanz ist ein **Home-Lab** — sie dient
dazu, FM-Funktionen durchzudenken und in Beratung und Showcases zu zeigen. Es hängen keine
Kunden und keine produktiven Kundendaten daran. Daraus folgt für jede Empfehlung unten:
**Pragmatik vor Härtung, Funktionserkundung vor Produktionsreife.** Ein Punkt kommt nur auf
die Liste, wenn er entweder eine echte Angriffsfläche schließt oder die tägliche Arbeit am
Code spürbar erleichtert. Alles andere ist notiert, aber nicht terminiert.

> **Korrekturen gegenüber dem ersten Entwurf** (gleicher Tag). Der erste Wurf enthielt
> geschätzte Zahlen, die die Prüfung nicht überstanden haben — und sie waren zu günstig,
> nicht zu pessimistisch:
>
> - **MUI ist vier Major-Versionen zurück, nicht eine** (5.8.2 gegen aktuell 9.3.1).
>   Gleiches beim Data Grid (5.17.3 gegen 9.12.0).
> - **TypeScript 4.7.3 gegen aktuell 7.0.2** — auch hier deutlich mehr als „eine Major".
> - Die Backend-Versionstabelle war falsch abgelesen: „Hibernate 1.5.5.Final" ist in
>   Wahrheit **MapStruct**, „Liquibase 2.0.1" ist **google-cloud-storage**. Echte Werte
>   in Abschnitt 2.
> - „Tailwind fehlt" stand als Lücke drin. Ist keine — das Projekt benutzt MUI als
>   Design-System, das ist eine Entscheidung, kein Versäumnis. Zeile gestrichen.
> - Der Default-Superadmin war als offene Aufgabe geführt. **Er ist längst gesperrt**,
>   siehe Abschnitt 1.
> - Kostenschätzungen in Euro und ein Zeitplan auf konkrete Kalenderwochen: beides
>   erfunden und für ein Home-Lab ohnehin sinnlos. Gestrichen.

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
ersten Start zurück, lautlos und ohne Logzeile. Deshalb steht in Abschnitt 4 mit **A1** ein
Code-Fix, der diesen Zustand unmöglich macht — und bis dahin gilt die Prüfung nach jeder
Rücksicherung:

```sql
SELECT id, email, enabled FROM users WHERE email = 'superadmin@test.com';
-- erwartet: enabled = false
```

Hintergrund steht jetzt auch in der [`CLAUDE.md`](../CLAUDE.md) („Known upstream issue: the
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
| google-cloud-storage | 2.0.1 | **von 2021** — einzige echte Altlast im Backend |

Das Backend ist nicht das Problem. `google-cloud-storage` fällt auf, greift aber nur, wenn
GCP als Storage konfiguriert ist — hier läuft MinIO. Kein Handlungsdruck.

### Frontend — der eigentliche Rückstand

| Paket | im Projekt | aktuell | Abstand |
|---|---|---|---|
| `react` / `react-dom` | **17.0.2** | 19.2.8 | 2 Major |
| `@mui/material` | **5.8.2** | 9.3.1 | **4 Major** |
| `@mui/x-data-grid` | **5.17.3** | 9.12.0 | **4 Major** |
| `typescript` | **4.7.3** | 7.0.2 | ~3 Major |
| `react-router-dom` | 6.3.0 | 7.18.2 | 1 Major |
| `@reduxjs/toolkit` | 1.8.2 | 2.12.0 | 1 Major |
| `react-redux` | 8.0.2 | 9.3.0 | 1 Major |
| `date-fns` | 2.28.0 | 4.4.0 | 2 Major |
| `axios` | 0.27.2 | 1.19.0 | 1 Major |
| `react-scripts` (CRA) | 5.0.1 | 5.0.1 | „aktuell", weil eingefroren — CRA wird nicht mehr gepflegt |

`formik@2.2.9` (aktuell 2.4.9) ist unkritisch: nur Patch-Abstand. Das Projekt lebt langsam,
aber es lebt.

### Wie tief sitzt das jeweils?

Gemessen an der Zahl der Dateien, die das Paket anfassen:

| Paket | Dateien | Bedeutung |
|---|---|---|
| `@mui/material` | **262** | das Design-System — jede MUI-Major fasst mehr als die Hälfte der Oberfläche an |
| `formik` | 26 | alle Formulare |
| `@mui/x-data-grid` | 15 | alle Tabellen |
| `@mui/lab` | 9 | vereinzelt |
| `axios` | 5 | zentral gekapselt, gut |
| `react-beautiful-dnd` | 5 | verwaist bei Atlassian, `@hello-pangea/dnd` ist der Fork |
| `firebase` | 4 | Push-Benachrichtigungen |
| `@mui/styles` | **2** | siehe unten |

**Die gute Nachricht steckt in der letzten Zeile.** `@mui/styles` ist die alte JSS-Schicht;
sie ist abgekündigt und **unterstützt React 18 nicht**. Sie ist der eigentliche Riegel vor
dem React-Upgrade — und sie steckt in genau **zwei** Dateien
(`theme/ThemeProvider.tsx`, `content/own/components/form/SelectTasks/DraggableTask.tsx`).
Der Riegel ist also billig zu entfernen. Im ersten Entwurf hatte ich ihn übersehen und wäre
beim Upgrade hineingelaufen.

### Tote Abhängigkeiten

In `package.json` deklariert, in `src/` **null** Verwendungen:

| Paket | Anmerkung |
|---|---|
| `aws-amplify@4.3.24` | großer, vier Jahre alter Abhängigkeitsbaum. Die einzigen „Amplify"-Treffer im Code sind ein Logo-Bild auf einer Template-Loginseite |
| `react-quill@2.0.0-beta.4` | Beta-Version, nirgends importiert |
| `react-simple-maps` + `@types/react-simple-maps` | nirgends importiert |

Rauswerfen kostet nichts und verkleinert Angriffsfläche und Build-Zeit spürbar. Der
billigste Punkt auf der ganzen Liste.

### Was ich nicht messen konnte

`npm audit` und der Zugriff auf die Produktionsdatenbank wurden vom Auto-Mode-Classifier
verweigert. Die konkrete Zahl der Schwachstellen und der tatsächliche `enabled`-Wert des
Superadmins sind daher **nicht verifiziert**, sondern aus Quelltext und Projektnotizen
abgeleitet. Beides steht als erster Schritt in Stufe 1, mit den Befehlen zum Selbstausführen.

---

## 3. Vorgehen in Stufen

Drei Stufen, jede für sich abschließbar und einzeln sinnvoll. Es gibt keinen Zwang,
Stufe 2 anzufangen, nur weil Stufe 1 fertig ist.

| Stufe | Inhalt | Aufwand (grob) | Empfehlung |
|---|---|---|---|
| **1 — Aufräumen und absichern** | tote Pakete raus, `npm audit`, Superadmin-Fix im Code, `@mui/styles` ablösen | 1–2 Tage | **jetzt** |
| **2 — React 17 → 18/19** | nach Stufe 1 technisch entriegelt; dazu TypeScript und die Ein-Major-Pakete | 1–2 Wochen | wenn Stufe 1 steht und Ruhe herrscht |
| **3 — MUI 5 → 9** | vier Majors über 262 Dateien | mehrere Wochen | **nicht** ohne konkreten Anlass |

**Stufe 3 ist ausdrücklich nicht empfohlen.** Vier MUI-Majors über 262 Dateien sind für ein
Home-Lab kein sinnvoller Einsatz. Der Grund, es doch zu tun, wäre eine Funktion aus einer
neueren MUI-Version, die für einen Showcase gebraucht wird — dann aber als bewusste
Entscheidung, nicht als Aufräumarbeit.

---

## 4. Stufe 1 im Detail

### A0 — Messen, bevor irgendetwas angefasst wird

Beides ist mir hier gesperrt worden, daher zum Selbstausführen:

```bash
# 1) Schwachstellen im Frontend
cd frontend && npm audit

# 2) Superadmin-Zustand auf der Instanz (Container-Namen immer via docker ps auflösen)
docker exec <postgres-container> psql -U <db-user> -d atlas \
  -c "SELECT id, email, enabled FROM users WHERE email = 'superadmin@test.com';"
```

Erwartung bei (2): `enabled = false`. Falls `true` oder die Zeile fehlt, hat Stufe 1 eine
andere erste Aufgabe als hier geplant.

**Ergebnis von `npm audit` bitte hier eintragen** — davon hängt ab, ob A2 fünf Minuten oder
einen halben Tag dauert:

```
Datum       | high | critical | Bemerkung
------------|------|----------|----------
2026-08-__  |      |          |
```

### A1 — Superadmin auch im Code entschärfen

Das Ziel ist nicht, die laufende Instanz zu ändern (die ist in Ordnung), sondern dass eine
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

Zu beachten: `UserService` und `ApplicationInitializer` stehen beide auf der
Upstream-Merge-Liste in der `CLAUDE.md`. Diese Änderung gehört dort ergänzt, sonst wird sie
beim nächsten Upstream-Abgleich stillschweigend überschrieben.

**Prüfen:** frische Datenbank hochziehen, Log auf die Warnung ansehen, mit dem geloggten
Passwort anmelden, danach sperren. Auf der bestehenden Instanz darf sich **nichts** ändern —
das Konto existiert dort, der Zweig läuft also gar nicht erst an.

### A2 — Tote Pakete entfernen und `npm audit` abarbeiten

```bash
cd frontend
npm uninstall aws-amplify react-quill react-simple-maps @types/react-simple-maps
npm audit fix          # ohne --force; Breaking Changes einzeln bewerten
```

`--force` bewusst nicht: es zieht Major-Sprünge herein, die bei React 17 mit hoher
Wahrscheinlichkeit etwas zerlegen. Was nach `npm audit fix` an *high*/*critical* übrig
bleibt, einzeln ansehen und begründet stehen lassen oder in Stufe 2 einplanen.

Erwarteter Nebeneffekt: der Wegfall von `aws-amplify` sollte einen guten Teil der Befunde
mitnehmen — vier Jahre alter Abhängigkeitsbaum.

### A3 — Versionen festnageln (`overrides`)

In `frontend/package.json`, analog zu den anderen Projekten im Bestand:

```json
"overrides": {
  "lodash": "4.17.21"
}
```

Nur aufnehmen, was `npm audit` in A2 tatsächlich als transitives Problem meldet. Eine
Override auf Verdacht ist eine Falle: sie friert eine Version ein, die niemand mehr prüft.
Nach dem Setzen mit `npm ls <paket>` gegenprüfen, dass sie auch greift.

### A4 — `@mui/styles` ablösen

Zwei Dateien, und danach ist der Riegel vor React 18 weg:

- `src/theme/ThemeProvider.tsx` — `StylesProvider` ersatzlos entfernen; MUI v5 braucht ihn
  nicht mehr, er ist Rest aus der v4-Zeit.
- `src/content/own/components/form/SelectTasks/DraggableTask.tsx` — `makeStyles` durch die
  `sx`-Prop oder `styled()` aus `@mui/material` ersetzen.

**Prüfen:** beide betroffenen Oberflächen ansehen (Theme-Umschaltung hell/dunkel und die
Aufgabenliste mit Drag-and-Drop im Checklisten-Editor). Diese Änderung ist optisch, ein
grüner Build beweist hier wenig.

### A5 — LDAP-Fehler nicht als „falsches Passwort" ausgeben

Kein Neufund, sondern der offene Punkt aus der `CLAUDE.md`: `signinLdap` verdichtet immer
noch jeden Fehlschlag zu einer Meldung — genau die Falle, die bei `signin` schon behoben
wurde und dort Stunden gekostet hat. Ein nicht erreichbarer LDAP-Server muss 503 mit eigener
Meldung ergeben, nicht 403 „falsche Zugangsdaten".

Nur sinnvoll, wenn LDAP hier überhaupt benutzt wird. Falls nicht: Punkt streichen, nicht
aufheben.

---

## 5. Prüfregeln für diese Codebasis

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
- **Datenbankschreibzugriffe** (DELETE, Passwortänderungen) blockt der Auto-Mode-Classifier.
  Solche Schritte gehören als Copy-Paste-Block formuliert, nicht umgangen.
- **Es arbeiten teils parallele Agents im selben Repo.** Vor jedem Commit `git status`
  ansehen und fremde Änderungen thematisch getrennt committen, nicht mit den eigenen mischen.

---

## 6. Fertig ist Stufe 1, wenn

- [ ] `npm audit` liefert keine *critical*-Befunde mehr; jeder verbleibende *high*-Befund
      hat eine notierte Begründung
- [ ] `aws-amplify`, `react-quill`, `react-simple-maps` sind aus `package.json` verschwunden
- [ ] `ApplicationInitializer` legt kein Konto mehr mit einem im Quelltext stehenden
      Passwort an; die Änderung steht in der Upstream-Merge-Tabelle der `CLAUDE.md`
- [ ] `@mui/styles` kommt in `src/` nicht mehr vor
- [ ] `CI=true npm run build` im Ordner `frontend` läuft durch
- [ ] Die Maven-Suite ist in CI grün (nicht lokal — siehe Abschnitt 5)
- [ ] Von Hand angesehen: Anmeldung, Anlagenliste, Arbeitsauftrag anlegen und bearbeiten,
      Einstellungen, Theme-Umschaltung, Checklisten-Drag-and-Drop
- [ ] Der Superadmin ist auf der Instanz weiterhin gesperrt (A0-Abfrage wiederholen)

---

## 7. Zurückrollen

Stufe 1 fasst kein Datenbankschema an, ein Rückbau ist deshalb reines Zurücksetzen des Codes:

```bash
git revert <commit>
git push origin main
# CI baut ein neues Image, Coolify zieht es
```

Alternativ in Coolify `IMAGE_TAG` auf ein `sha-<commit>` von vorher setzen und neu ausrollen —
der schnellere Weg, wenn es eilt.

---

## 8. Bewusst nicht auf der Liste

Damit später niemand denkt, es sei übersehen worden:

- **Tailwind einführen.** MUI ist das Design-System dieses Forks. Ein zweites Styling-System
  danebenzustellen kostet mehr, als es bringt.
- **Next.js oder Vite statt CRA.** Vite würde die Startzeiten deutlich verbessern, aber CRA
  funktioniert und ist nicht der Engpass. Erst interessant, wenn CRA konkret im Weg steht —
  etwa wenn ein Paket in Stufe 2 keinen Webpack-Pfad mehr anbietet.
- **`google-cloud-storage` im Backend aktualisieren.** Alt, aber ungenutzt (hier läuft MinIO).
- **Der Rest der Upstream-Befunde** aus der `CLAUDE.md` — etwa dass
  `POST /work-orders/search` ohne Ansichtsrecht die ganze Firma zurückgibt. Das ist ein
  echter Fehler und gehört behoben, ist aber ein Fachthema und keine technische Schuld; er
  bleibt dort verzeichnet, wo er hingehört.
