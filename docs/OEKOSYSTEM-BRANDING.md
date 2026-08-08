# Ökosystem-Branding: Marke teilen, Favicon unterscheiden

Alle Anwendungen des Ökosystems tragen **dieselbe Bildmarke**. Unterschieden werden sie
durch **die Beschriftung** (Text in `Logo.tsx`) und **die Favicon-Farbe**. Sonst nichts.

Dieses Dokument ist die Übergabe an die nächste Anwendung, damit dort niemand
dieselben Sackgassen noch einmal durchläuft.

---

## 1. Die Farben — abgetastet, nicht geschätzt

Aus `logo_v4.png` gemessen (dominante Fläche je Beere). **Nicht neu herleiten.**

| Rolle | Hex | Anwendung |
|---|---|---|
| Grau | `#A3A49F` | AssetTrace |
| Orange | `#FD7C14` | frei |
| Grün | `#51AA4C` | frei |
| Navy/Blau | `#1D477D` | frei |
| **Kontur** | `#565455` | **konstant über alle Anwendungen** |

Die Kontur gehört zur Marke, nicht zur einzelnen Anwendung — sie wechselt nie mit.

---

## 2. Der Ablauf je Anwendung

```powershell
# 1. Bildmarke ablegen (git-ignoriert, mehrere MB)
#    docs/logo_v4.png

# 2. Skript aus AssetTrace kopieren
#    apps/web/scripts/gen-logo-assets.ps1

# 3. Einmal laufen lassen — EINE Zeile, der Rest ist identisch
pwsh apps/web/scripts/gen-logo-assets.ps1 -Accent orange
```

Erzeugt `public/logo-mark.png`, `public/logo-tile.png`, `src/app/icon.png`,
`src/app/apple-icon.png`, `src/app/favicon.ico`.

`-Accent` nimmt `grau|orange|gruen|blau` oder direkt einen Hexwert (`#FD7C14`).

**Nicht vergessen:** Hat die Anwendung eine Landing-Seite mit eigenen Kopien, ziehen die
nicht automatisch mit. Und deren `<link rel="icon" type="image/png">` muss auf das
**Badge** zeigen, nicht auf die Marke — Browser bevorzugen das PNG gegenüber der `.ico`,
sonst steht im Tab weiter die Marke.

---

## 3. Die Fallen — jede hat einmal ein falsches Bild erzeugt

Diese vier Punkte sind der eigentliche Wert dieses Dokuments.

**Die Bogenwölbung.** Der Lucide-`badge`-Pfad besteht aus acht Bögen mit Radius 4. Jeder
wölbt sich **1,85 Einheiten über seine Endpunkte hinaus**. Die Form reicht deshalb
`2…22`, nicht `3.85…20.15` (das sind nur die Endpunkte). Mit der engeren viewBox
schneidet man die Lappen ab und bekommt ein gerundetes Quadrat mit Kerben.

**Die Konturbreite.** Lucide zeichnet mit `stroke-width="2"` im 24er-Raster. Auf 512 px
hochskaliert sind das ~45 px, und weil die Linie mittig auf dem Pfad liegt, füllt ihre
äußere Hälfte die Kerben zwischen den Bögen zu. Bewährt: **1.2**.

**Die Locale.** Unter deutschem Windows formatiert PowerShell `1.4` als `"1,4"`. Ein
`viewBox="1,4 …"` ist ungültig, der Browser **verwirft das Element still** und die Kachel
kommt leer heraus. Alle Zahlen im SVG mit `InvariantCulture` formatieren.

**Der Browser-Aufruf.** Zum Rastern läuft Chrome/Edge headless. Zwei Details sind Pflicht:
- **eigenes `--user-data-dir`** — läuft schon ein Browser mit dem Standardprofil, reicht
  der neue Aufruf nur dorthin weiter und kehrt sofort zurück, **ohne je zu schreiben**.
  Die alte Datei bleibt liegen und man debuggt stundenlang das falsche Bild.
- **Argumente als Array**, keine Backtick-Fortsetzung. Bricht eine Zeile, läuft der
  Browser kommentarlos ohne `--window-size` auf einer leeren Seite.

Nach dem Rastern **immer die Bildgröße prüfen** (`if ($bmp.Width -ne 512) { throw }`) —
alle drei Fehler oben äußern sich als stillschweigend falsches Bild, nicht als Absturz.

---

## 4. Warum der Vektorpfad und keine PNG-Vorlage

Das Favicon war früher eine git-ignorierte PNG-Datei. Das hieß: fehlt auf jeder frischen
Maschine, Farben nur aus Pixeln rückgewinnbar, Änderung nur mit Grafikprogramm. Als
Pfad im Skript ist es reproduzierbar, die Farben sind benannt, und eine neue Anwendung
kostet einen Parameter.
