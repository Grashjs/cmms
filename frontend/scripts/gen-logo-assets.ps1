<#
.SYNOPSIS
  Erzeugt alle Logo- und Icon-Assets aus den Design-Vorlagen.

.DESCRIPTION
  Zwei Quellen, zwei Rollen — das ist die tragende Entscheidung dieses Setups:

    docs/logo_v4.png    Die Bildmarke, als Rasterdatei. Sie ist in ALLEN
                        Anwendungen des Ökosystems dieselbe; unterschieden
                        werden die Anwendungen allein durch die Beschriftung
                        darunter. Git-ignoriert (mehrere MB).
    Lucide `badge`      Das Favicon, als Vektorpfad WEITER UNTEN IM SKRIPT.
                        DAS ist das Erkennungszeichen von AssetTrace im
                        Browser-Tab und auf dem Startbildschirm. Andere
                        Anwendungen des Ökosystems setzen hier ihr eigenes
                        Motiv ein.

  Wer beides zusammenlegt, verliert genau die Unterscheidung, für die das
  Favicon da ist.

  Das Favicon war früher eine PNG-Vorlage (docs/fav_at_v2.png) und ist jetzt
  ein Pfad im Code. Drei Gründe: Die Datei war git-ignoriert und fehlte damit
  auf jeder frischen Maschine; ihre Farben mussten aus Pixeln zurückgewonnen
  werden statt benannt zu sein; und sie ließ sich nicht ändern, ohne ein
  Grafikprogramm zu öffnen.

  Erzeugt wird:
    public/logo-mark.png   512², freigestellt — die geteilte Bildmarke
    public/logo-tile.png   512², weiße Rundkachel + Marke (og:image)
    src/app/icon.png       512², Badge  ┐
    src/app/apple-icon.png 180², Badge  ├─ anwendungsspezifisch
    src/app/favicon.ico    16/32/48/64  ┘

  KEINE Wortmarke: Die Beschriftung („AssetTrace") setzt components/Logo.tsx
  als Text. Sie muss je Anwendung austauschbar sein und sich an die Farbe der
  Fläche anpassen können — als Bild ginge beides nicht.

  Freigestellt wird per Flutfüllung von den Rändern nach innen, nicht über eine
  Helligkeitsschwelle: Die Marke hat große weiße Innenflächen, die eine
  Schwelle mit ausstanzen würde.

  Für das Favicon braucht der Lauf Chrome oder Edge: Der Vektorpfad wird im
  Browser gerastert, statt SVG-Bögen von Hand nach System.Drawing zu übersetzen.
  Fehlt beides, erzeugt der Lauf nur die Marke und lässt die Icons unberührt.

.PARAMETER LogoSource
  Bildmarke. Standard: docs/logo_v4.png
.PARAMETER BrowserPath
  Chrome oder Edge zum Rastern des Favicons. Standard: automatisch gesucht.
.PARAMETER Accent
  Füllfarbe des Favicons — der Name einer Beere aus der Marke (grau, orange,
  gruen, blau) oder ein eigener Hexwert. Das ist der EINZIGE Wert, der sich
  zwischen den Anwendungen des Ökosystems unterscheidet. Standard: grau
  (AssetTrace).

.EXAMPLE
  pwsh apps/web/scripts/gen-logo-assets.ps1
.EXAMPLE
  pwsh apps/web/scripts/gen-logo-assets.ps1 -Accent orange
#>
[CmdletBinding()]
param([string] $LogoSource, [string] $BrowserPath, [string] $Accent = 'grau')

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$webRoot  = Split-Path $PSScriptRoot -Parent
$repoRoot = Split-Path (Split-Path $webRoot -Parent) -Parent
if (-not $LogoSource) { $LogoSource = Join-Path $repoRoot 'docs\logo_v4.png' }
if (-not (Test-Path $LogoSource)) {
  throw "Bildmarke nicht gefunden: $LogoSource (liegt absichtlich nicht im Repo — Pfad via -LogoSource angeben)"
}

# Ohne Browser laeuft nur Teil 1+2. Die Icons im Repo bleiben, wie sie sind.
if (-not $BrowserPath) {
  $BrowserPath = @(
    "$env:ProgramFiles\Google\Chrome\Application\chrome.exe"
    "${env:ProgramFiles(x86)}\Google\Chrome\Application\chrome.exe"
    "$env:ProgramFiles\Microsoft\Edge\Application\msedge.exe"
    "${env:ProgramFiles(x86)}\Microsoft\Edge\Application\msedge.exe"
  ) | Where-Object { Test-Path $_ } | Select-Object -First 1
}
$hasBrowser = [bool]$BrowserPath

$pub = Join-Path $webRoot 'public'
$app = Join-Path $webRoot 'src\app'

# ─────────────────────────────────────────────────────────────────────────────
# Freistellen: Ink-Bounding-Box bestimmen, Weiß von außen fluten, weich
# auslaufende Kante, Ergebnis mittig auf eine quadratische Leinwand.
# `HeightRatio` legt fest, wie hoch die Marke auf dieser Leinwand steht — davon
# hängt ab, wie groß sie neben gleich großen Nachbarn wirkt.
# ─────────────────────────────────────────────────────────────────────────────
function New-CutoutSquare {
  param([string]$Path, [double]$HeightRatio)

  $src = [System.Drawing.Image]::FromFile($Path)
  $sbm = New-Object System.Drawing.Bitmap $src
  $W = $sbm.Width; $H = $sbm.Height
  $ld = $sbm.LockBits((New-Object System.Drawing.Rectangle 0, 0, $W, $H),
    [System.Drawing.Imaging.ImageLockMode]::ReadOnly, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
  $stride = $ld.Stride
  $px = New-Object byte[] ($stride * $H)
  [System.Runtime.InteropServices.Marshal]::Copy($ld.Scan0, $px, 0, $px.Length)
  $sbm.UnlockBits($ld)

  $ix = $W; $ax = -1; $iy = $H; $ay = -1
  for ($y = 0; $y -lt $H; $y++) {
    $row = $y * $stride
    for ($x = 0; $x -lt $W; $x++) {
      $o = $row + $x * 4
      $b = $px[$o]; $g = $px[$o + 1]; $r = $px[$o + 2]
      $mx = [Math]::Max($r, [Math]::Max($g, $b)); $mn = [Math]::Min($r, [Math]::Min($g, $b))
      $lum = (299 * $r + 587 * $g + 114 * $b) / 1000
      if (($mx - $mn) -gt 25 -or $lum -lt 200) {
        if ($x -lt $ix) { $ix = $x }; if ($x -gt $ax) { $ax = $x }
        if ($y -lt $iy) { $iy = $y }; if ($y -gt $ay) { $ay = $y }
      }
    }
  }
  $mw = $ax - $ix + 1; $mh = $ay - $iy + 1
  # Write-Host, nicht in die Pipeline: sonst gibt die Funktion [string, Bitmap]
  # zurueck und der Aufrufer bekommt ein Array statt der Bitmap.
  Write-Host "  Ink-bbox x $ix..$ax  y $iy..$ay   ($mw x $mh)"

  $PAD = 8
  $cw = $mw + 2 * $PAD; $ch = $mh + 2 * $PAD
  $lumArr = New-Object byte[] ($cw * $ch)
  $outside = New-Object bool[] ($cw * $ch)
  for ($y = 0; $y -lt $ch; $y++) {
    for ($x = 0; $x -lt $cw; $x++) {
      $sx = $ix - $PAD + $x; $sy = $iy - $PAD + $y
      if ($sx -lt 0 -or $sy -lt 0 -or $sx -ge $W -or $sy -ge $H) { $lumArr[$y * $cw + $x] = 255; continue }
      $o = $sy * $stride + $sx * 4
      $lumArr[$y * $cw + $x] = [byte](((299 * $px[$o + 2] + 587 * $px[$o + 1] + 114 * $px[$o]) / 1000))
    }
  }
  $WHITE = 238
  $stack = New-Object System.Collections.Generic.Stack[int]
  for ($x = 0; $x -lt $cw; $x++) { $stack.Push($x); $stack.Push(($ch - 1) * $cw + $x) }
  for ($y = 0; $y -lt $ch; $y++) { $stack.Push($y * $cw); $stack.Push($y * $cw + $cw - 1) }
  while ($stack.Count -gt 0) {
    $i = $stack.Pop()
    if ($outside[$i]) { continue }
    if ($lumArr[$i] -lt $WHITE) { continue }
    $outside[$i] = $true
    $x = $i % $cw; $y = [int](($i - $x) / $cw)
    if ($x -gt 0)       { $stack.Push($i - 1) }
    if ($x -lt $cw - 1) { $stack.Push($i + 1) }
    if ($y -gt 0)       { $stack.Push($i - $cw) }
    if ($y -lt $ch - 1) { $stack.Push($i + $cw) }
  }

  $cut = New-Object System.Drawing.Bitmap $cw, $ch, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
  $cd = $cut.LockBits((New-Object System.Drawing.Rectangle 0, 0, $cw, $ch),
    [System.Drawing.Imaging.ImageLockMode]::WriteOnly, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
  $cstride = $cd.Stride
  $outBytes = New-Object byte[] ($cstride * $ch)
  for ($y = 0; $y -lt $ch; $y++) {
    for ($x = 0; $x -lt $cw; $x++) {
      $i = $y * $cw + $x; $co = $y * $cstride + $x * 4
      if ($outside[$i]) { $outBytes[$co + 3] = 0; continue }
      $sx = $ix - $PAD + $x; $sy = $iy - $PAD + $y
      if ($sx -lt 0 -or $sy -lt 0 -or $sx -ge $W -or $sy -ge $H) { $outBytes[$co + 3] = 0; continue }
      $o = $sy * $stride + $sx * 4
      $outBytes[$co] = $px[$o]; $outBytes[$co + 1] = $px[$o + 1]; $outBytes[$co + 2] = $px[$o + 2]
      $alpha = 255
      $touches = ($x -gt 0 -and $outside[$i - 1]) -or ($x -lt $cw - 1 -and $outside[$i + 1]) -or
                 ($y -gt 0 -and $outside[$i - $cw]) -or ($y -lt $ch - 1 -and $outside[$i + $cw])
      if ($touches) {
        $l = $lumArr[$i]
        if ($l -gt 210) { $alpha = [int](255 * (($WHITE - $l) / [double]($WHITE - 210))) }
        if ($alpha -lt 0) { $alpha = 0 }; if ($alpha -gt 255) { $alpha = 255 }
      }
      $outBytes[$co + 3] = [byte]$alpha
    }
  }
  [System.Runtime.InteropServices.Marshal]::Copy($outBytes, 0, $cd.Scan0, $outBytes.Length)
  $cut.UnlockBits($cd)

  $side = [int]([Math]::Round($mh / $HeightRatio))
  if ($side -lt $mw + 16) { $side = $mw + 16 }
  $square = New-Object System.Drawing.Bitmap $side, $side, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
  $g = [System.Drawing.Graphics]::FromImage($square)
  $g.InterpolationMode = 'HighQualityBicubic'
  $g.DrawImage($cut, [int](($side - $cw) / 2), [int](($side - $ch) / 2), $cw, $ch)
  $g.Dispose(); $cut.Dispose(); $sbm.Dispose(); $src.Dispose()
  return $square
}

function Save-Scaled($bitmap, [int]$size, [string]$file) {
  $o = New-Object System.Drawing.Bitmap $size, $size, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
  $gg = [System.Drawing.Graphics]::FromImage($o)
  $gg.InterpolationMode = 'HighQualityBicubic'; $gg.SmoothingMode = 'HighQuality'; $gg.PixelOffsetMode = 'HighQuality'
  $gg.DrawImage($bitmap, 0, 0, $size, $size); $gg.Dispose()
  $o.Save($file, [System.Drawing.Imaging.ImageFormat]::Png); $o.Dispose()
  "{0,-24} {1}x{1}" -f (Split-Path $file -Leaf), $size
}

# ── 1) Geteilte Bildmarke ────────────────────────────────────────────────────
"Bildmarke ($(Split-Path $LogoSource -Leaf)):"
$markSquare = New-CutoutSquare -Path $LogoSource -HeightRatio 0.92
Save-Scaled $markSquare 512 (Join-Path $pub 'logo-mark.png')

# ── 2) og:image — Marke auf weißer Rundkachel ────────────────────────────────
$mark = [System.Drawing.Image]::FromFile((Join-Path $pub 'logo-mark.png'))
function New-Tile([System.Drawing.Image]$art, [int]$size, [double]$inset) {
  $b = New-Object System.Drawing.Bitmap $size, $size, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
  $gg = [System.Drawing.Graphics]::FromImage($b)
  $gg.SmoothingMode = 'AntiAlias'; $gg.InterpolationMode = 'HighQualityBicubic'; $gg.PixelOffsetMode = 'HighQuality'
  $r = [Math]::Max(2, [int]($size * 0.225))
  $p = New-Object System.Drawing.Drawing2D.GraphicsPath
  $p.AddArc(0, 0, 2 * $r, 2 * $r, 180, 90)
  $p.AddArc($size - 2 * $r, 0, 2 * $r, 2 * $r, 270, 90)
  $p.AddArc($size - 2 * $r, $size - 2 * $r, 2 * $r, 2 * $r, 0, 90)
  $p.AddArc(0, $size - 2 * $r, 2 * $r, 2 * $r, 90, 90)
  $p.CloseFigure()
  $brush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::White)
  $gg.FillPath($brush, $p); $gg.SetClip($p)
  $m = [int]($size * $inset); $off = [int](($size - $m) / 2)
  $gg.DrawImage($art, $off, $off, $m, $m)
  $gg.Dispose(); $p.Dispose(); $brush.Dispose()
  return $b
}
$t = New-Tile $mark 512 0.84
$t.Save((Join-Path $pub 'logo-tile.png'), [System.Drawing.Imaging.ImageFormat]::Png); $t.Dispose()
"{0,-24} 512x512" -f 'logo-tile.png'

# ── 3) Anwendungs-Icons: Lucide `badge`, grau gefüllt ────────────────────────
if (-not $hasBrowser) {
  Write-Host ''
  Write-Host 'Weder Chrome noch Edge gefunden — Icons bleiben unveraendert.' -ForegroundColor Yellow
  Write-Host 'Pfad notfalls mit -BrowserPath angeben.' -ForegroundColor DarkGray
  $mark.Dispose(); $markSquare.Dispose()
  return
}

# Lucide `badge` (MIT), unveraendert aus lucide-icons/lucide, icons/badge.svg.
# Acht Boegen mit Radius 4 im 24er-Raster.
$BADGE = 'M3.85 8.62a4 4 0 0 1 4.78-4.77 4 4 0 0 1 6.74 0 4 4 0 0 1 4.78 4.78 4 4 0 0 1 0 6.74 4 4 0 0 1-4.77 4.78 4 4 0 0 1-6.75 0 4 4 0 0 1-4.78-4.77 4 4 0 0 1 0-6.76Z'

# Die Beeren der Marke, aus logo_v4.png abgetastet — nicht geschaetzt, nicht
# aus einer Design-Datei abgeschrieben. Je Anwendung des Oekosystems eine:
# das Favicon ist das Einzige, was sie im Browser-Tab unterscheidet.
$BERRIES = @{
  grau   = '#A3A49F'   # AssetTrace
  orange = '#FD7C14'
  gruen  = '#51AA4C'
  blau   = '#1D477D'
}
$key = $Accent.Trim().ToLowerInvariant().Replace('ü', 'u').Replace('ä', 'a').Replace('ö', 'o').Replace('grun', 'gruen')
if ($Accent -match '^#[0-9a-fA-F]{6}$') { $FILL = $Accent.ToUpperInvariant() }
elseif ($BERRIES.ContainsKey($key)) { $FILL = $BERRIES[$key] }
else { throw "Unbekannter -Accent '$Accent'. Erlaubt: $($BERRIES.Keys -join ', ') oder ein Hexwert wie #FD7C14" }

# Die Kontur bleibt ueber alle Anwendungen gleich — sie gehoert zur Marke,
# nicht zur einzelnen Anwendung.
$STROKE = '#565455'; $SW = 1.2

# Jeder Bogen woelbt sich 1.85 Einheiten ueber seine Endpunkte hinaus. Die Form
# reicht also 2..22 und nicht 3.85..20.15 — mit der engeren Box schneidet man
# die Lappen ab und bekommt ein gerundetes Quadrat.
$inv = [System.Globalization.CultureInfo]::InvariantCulture
$vbMin = (2 - $SW / 2).ToString($inv); $vbSpan = (20 + $SW).ToString($inv)
# Einzige Abweichung vom AssetTrace-Original: der Hintergrund-<rect> steht hier
# auf fill="none" statt #ffffff, sodass icon.png transparente Ecken hat (in
# cmms4fm gewollt). Die vier in docs/OEKOSYSTEM-BRANDING.md dokumentierten
# Fallen — Bogenwölbung (viewBox oben), Konturbreite ($SW=1.2), Locale
# ($InvariantCulture) und der Browser-Aufruf (eigenes --user-data-dir, Array-
# Argumente, Width-ne-512-Prüfung weiter unten) — sind UNVERÄNDERT übernommen.
# Konsequenz: apple-icon.png ist auf iOS-Home-Screen keine geschlossene Kachel
# mehr; das ist hier bewusst in Kauf genommen.
$html = @"
<meta charset="utf-8"><style>html,body{margin:0;padding:0;background:transparent}</style>
<svg xmlns="http://www.w3.org/2000/svg" width="512" height="512" viewBox="0 0 512 512">
  <rect width="512" height="512" rx="115" fill="none"/>
  <svg x="30" y="30" width="452" height="452" viewBox="$vbMin $vbMin $vbSpan $vbSpan">
    <path d="$BADGE" fill="$FILL" stroke="$STROKE" stroke-width="$($SW.ToString($inv))" stroke-linejoin="round"/>
  </svg>
</svg>
"@
# Zahlen invariant formatieren: Unter deutscher Locale wuerde aus 1.4 die
# Zeichenkette "1,4", viewBox="1,4 ..." ist ungueltig und der Browser verwirft
# still das ganze Element — die Kachel kaeme leer heraus.

"Favicon (Lucide badge, Akzent '$Accent' = $FILL auf weisser Kachel):"
$tmp = Join-Path ([System.IO.Path]::GetTempPath()) ('at-badge-' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $tmp | Out-Null
try {
  $htmlFile = Join-Path $tmp 'badge.html'
  Set-Content -Path $htmlFile -Value $html -Encoding UTF8
  $shot = Join-Path $tmp 'badge.png'

  # Eigenes Profilverzeichnis ist Pflicht: Laeuft schon ein Browser mit dem
  # Standardprofil, reicht der neue Aufruf nur weiter und kehrt sofort zurueck,
  # ohne je einen Screenshot zu schreiben.
  # Argumente als Array, nicht mit Backtick-Fortsetzung: bricht eine davon,
  # laeuft der Browser kommentarlos ohne --window-size auf leerer Seite.
  $browserArgs = @(
    '--headless', '--disable-gpu', '--hide-scrollbars',
    "--user-data-dir=$tmp\profile", '--no-first-run', '--no-default-browser-check',
    '--force-device-scale-factor=1', '--default-background-color=00000000',
    "--screenshot=$shot", '--window-size=512,512',
    ('file:///' + ($htmlFile -replace '\\', '/'))
  )
  & $BrowserPath @browserArgs 2>&1 | Out-Null
  if (-not (Test-Path $shot)) { throw "Browser hat kein Bild geschrieben ($BrowserPath)" }

  $iconSquare = New-Object System.Drawing.Bitmap $shot
  if ($iconSquare.Width -ne 512) {
    throw "Browser lieferte $($iconSquare.Width)x$($iconSquare.Height) statt 512x512"
  }
}
finally { Remove-Item $tmp -Recurse -Force -ErrorAction SilentlyContinue }

foreach ($spec in @(@{S = 512; F = 'icon.png' }, @{S = 180; F = 'apple-icon.png' })) {
  Save-Scaled $iconSquare $spec.S (Join-Path $app $spec.F)
}

# favicon.ico — PNG-in-ICO (Vista+). Die Kachel steckt schon im gerasterten
# Bild, deshalb wird hier nur noch herunterskaliert.
$entries = foreach ($s in 16, 32, 48, 64) {
  $b = New-Object System.Drawing.Bitmap $s, $s, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
  $gg = [System.Drawing.Graphics]::FromImage($b)
  $gg.InterpolationMode = 'HighQualityBicubic'; $gg.SmoothingMode = 'HighQuality'; $gg.PixelOffsetMode = 'HighQuality'
  $gg.DrawImage($iconSquare, 0, 0, $s, $s); $gg.Dispose()
  $mem = New-Object System.IO.MemoryStream
  $b.Save($mem, [System.Drawing.Imaging.ImageFormat]::Png)
  , @($s, $mem.ToArray())
  $mem.Dispose(); $b.Dispose()
}
$ms = New-Object System.IO.MemoryStream
$bw = New-Object System.IO.BinaryWriter $ms
$bw.Write([uint16]0); $bw.Write([uint16]1); $bw.Write([uint16]$entries.Count)
$offset = 6 + 16 * $entries.Count
foreach ($e in $entries) {
  $bw.Write([byte]($e[0] -band 0xFF)); $bw.Write([byte]($e[0] -band 0xFF))
  $bw.Write([byte]0); $bw.Write([byte]0); $bw.Write([uint16]1); $bw.Write([uint16]32)
  $bw.Write([uint32]$e[1].Length); $bw.Write([uint32]$offset); $offset += $e[1].Length
}
foreach ($e in $entries) { $bw.Write($e[1]) }
$bw.Flush()
[System.IO.File]::WriteAllBytes((Join-Path $app 'favicon.ico'), $ms.ToArray())
"{0,-24} {1} Bytes, {2} Groessen" -f 'favicon.ico', $ms.Length, $entries.Count
$bw.Dispose(); $ms.Dispose()

$mark.Dispose(); $markSquare.Dispose(); $iconSquare.Dispose()

Write-Host ''
Write-Host 'Fertig. Die Landing-Page haelt eigene Kopien unter apps/landing/img/ —' -ForegroundColor DarkGray
Write-Host 'bei Aenderungen mit kopieren.' -ForegroundColor DarkGray
