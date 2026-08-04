# Derive every logo and favicon asset from the design sources, so that replacing artwork is one
# command instead of nine hand-exported files that drift apart.
#
#   pwsh frontend/scripts/build-logo-assets.ps1
#
# Windows only: it uses System.Drawing rather than adding an image dependency to the frontend
# build for something that runs by hand a few times a year.
#
# Two sources, because they play different roles across the product family:
#
#   docs/logo_v3.png      the shared mark. Identical in every application of the ecosystem; what
#                         distinguishes this one is the caption underneath it, which lives in
#                         src/components/LogoSign.
#   docs/fav_fm_v2..png   the per-application favicon — a gear and spanner for the maintenance
#                         tool. Deliberately a simplified mark: the full emblem turns into a
#                         coloured blur at 16px.
#
# Both sources are artwork on a white field, so each is cropped to its own content bounds first;
# neither is square, hence the pad-to-square step, which also adds a little breathing room.
#
# The logo keeps its white background on purpose. Both themes render the sidebar in #ffffff
# (theme/schemes/*.ts, layout.sidebar.background), so white is invisible there — while making it
# transparent would mean flood-filling a mark whose outline has gaps, letting the fill leak into
# the white areas inside the hexagon. The favicon does get a transparent background, because a
# browser tab strip can be dark, and there the fill is safe: every white area sits fully enclosed
# by the green ring, so it cannot be reached from the border.
Add-Type -AssemblyName System.Drawing

$repo = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$docs = Join-Path $repo 'docs'
$pub = Join-Path $repo 'frontend\public'
$logoDir = Join-Path $pub 'static\images\logo'

# --- helpers ---------------------------------------------------------------------------------

# Bounding box of everything that is not the white field. Any channel below 240 counts as
# content, which tolerates the compression noise in generated artwork (corners measured at 253).
function Get-ContentBounds([System.Drawing.Bitmap]$bmp) {
  $w = $bmp.Width; $h = $bmp.Height
  $rect = New-Object System.Drawing.Rectangle 0, 0, $w, $h
  $d = $bmp.LockBits($rect, [System.Drawing.Imaging.ImageLockMode]::ReadOnly, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
  $bytes = New-Object byte[] ($d.Stride * $h)
  [System.Runtime.InteropServices.Marshal]::Copy($d.Scan0, $bytes, 0, $bytes.Length)
  $bmp.UnlockBits($d)
  $x0 = $w; $y0 = $h; $x1 = -1; $y1 = -1
  for ($y = 0; $y -lt $h; $y++) {
    $off = $y * $d.Stride
    for ($x = 0; $x -lt $w; $x++) {
      $i = $off + $x * 4
      if ($bytes[$i] -lt 240 -or $bytes[$i + 1] -lt 240 -or $bytes[$i + 2] -lt 240) {
        if ($x -lt $x0) { $x0 = $x }
        if ($x -gt $x1) { $x1 = $x }
        if ($y -lt $y0) { $y0 = $y }
        if ($y -gt $y1) { $y1 = $y }
      }
    }
  }
  return @($x0, $y0, $x1, $y1)
}

# Crop to content, then centre on a square white canvas with `marginPct` padding.
function Get-SquareArtwork([string]$path, [double]$marginPct) {
  $src = [System.Drawing.Bitmap]::FromFile($path)
  $b = Get-ContentBounds $src
  $cw = $b[2] - $b[0] + 1; $ch = $b[3] - $b[1] + 1
  Write-Host ("{0}: {1}x{2}, content {3}..{4} x {5}..{6} ({7}x{8})" -f `
      [System.IO.Path]::GetFileName($path), $src.Width, $src.Height, $b[0], $b[2], $b[1], $b[3], $cw, $ch)
  $side = [int]([math]::Max($cw, $ch) * (1 + 2 * $marginPct))
  $out = New-Object System.Drawing.Bitmap $side, $side, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
  $g = [System.Drawing.Graphics]::FromImage($out)
  $g.Clear([System.Drawing.Color]::White)
  $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
  $g.DrawImage($src, (New-Object System.Drawing.Rectangle ([int](($side - $cw) / 2)), ([int](($side - $ch) / 2)), $cw, $ch),
               (New-Object System.Drawing.Rectangle $b[0], $b[1], $cw, $ch), [System.Drawing.GraphicsUnit]::Pixel)
  $g.Dispose(); $src.Dispose()
  return $out
}

# Clear the white field to transparency, starting from the border so enclosed white survives.
function Clear-Field([System.Drawing.Bitmap]$bmp) {
  $w = $bmp.Width; $h = $bmp.Height
  $rect = New-Object System.Drawing.Rectangle 0, 0, $w, $h
  $d = $bmp.LockBits($rect, [System.Drawing.Imaging.ImageLockMode]::ReadWrite, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
  $stride = $d.Stride
  $bytes = New-Object byte[] ($stride * $h)
  [System.Runtime.InteropServices.Marshal]::Copy($d.Scan0, $bytes, 0, $bytes.Length)
  $seen = New-Object bool[] ($w * $h)
  $stack = New-Object System.Collections.Generic.Stack[int]
  for ($x = 0; $x -lt $w; $x++) { $stack.Push($x); $stack.Push(($h - 1) * $w + $x) }
  for ($y = 0; $y -lt $h; $y++) { $stack.Push($y * $w); $stack.Push($y * $w + $w - 1) }
  $cleared = 0
  while ($stack.Count -gt 0) {
    $p = $stack.Pop()
    if ($seen[$p]) { continue }
    $seen[$p] = $true
    $y = [int][math]::Floor($p / $w); $x = $p - $y * $w
    $i = $y * $stride + $x * 4
    if ($bytes[$i] -lt 235 -or $bytes[$i + 1] -lt 235 -or $bytes[$i + 2] -lt 235) { continue }
    $bytes[$i + 3] = 0; $cleared++
    if ($x -gt 0) { $stack.Push($p - 1) }
    if ($x -lt $w - 1) { $stack.Push($p + 1) }
    if ($y -gt 0) { $stack.Push($p - $w) }
    if ($y -lt $h - 1) { $stack.Push($p + $w) }
  }
  [System.Runtime.InteropServices.Marshal]::Copy($bytes, 0, $d.Scan0, $bytes.Length)
  $bmp.UnlockBits($d)
  Write-Host ("  field cleared: {0} of {1} pixels" -f $cleared, ($w * $h))
}

function Save-Scaled([System.Drawing.Bitmap]$bmp, [int]$size, [string]$path) {
  $out = New-Object System.Drawing.Bitmap $size, $size, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
  $g = [System.Drawing.Graphics]::FromImage($out)
  $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
  $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
  $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
  $g.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality
  $g.DrawImage($bmp, (New-Object System.Drawing.Rectangle 0, 0, $size, $size))
  $g.Dispose()
  $out.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
  $out.Dispose()
  Write-Host ("  {0} -> {1}x{1}, {2} bytes" -f [System.IO.Path]::GetFileName($path), $size, (Get-Item $path).Length)
}

# --- the shared mark: sidebar logo and PWA icons ----------------------------------------------
# 256 for the two sidebar logos, not 512: they load on every page view and render at 64px, so 256
# is still oversampled, while staying above the 144px minimum of the Twitter summary card that
# reuses logo.png.
$logo = Get-SquareArtwork (Join-Path $docs 'logo_v3.png') 0.04
Save-Scaled $logo 256 (Join-Path $logoDir 'logo.png')
Save-Scaled $logo 256 (Join-Path $logoDir 'logo-white.png')
foreach ($s in 192, 256, 384, 512) { Save-Scaled $logo $s (Join-Path $logoDir "icon-$s.png") }
$logo.Dispose()

# --- the per-application favicon --------------------------------------------------------------
# Minimal margin here: every pixel counts at 16px.
$fav = Get-SquareArtwork (Join-Path $docs 'fav_fm_v2..png') 0.02
Clear-Field $fav
Save-Scaled $fav 32 (Join-Path $pub 'favicon-32x32.png')
Save-Scaled $fav 16 (Join-Path $pub 'favicon-16x16.png')

# favicon.ico with 16/32/48 entries carrying PNG payloads, which every current browser reads and
# which avoids hand-rolling BMP masks.
$icoSizes = 16, 32, 48
$blobs = @()
foreach ($s in $icoSizes) {
  $out = New-Object System.Drawing.Bitmap $s, $s, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
  $g = [System.Drawing.Graphics]::FromImage($out)
  $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
  $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
  $g.DrawImage($fav, (New-Object System.Drawing.Rectangle 0, 0, $s, $s))
  $g.Dispose()
  $ms = New-Object System.IO.MemoryStream
  $out.Save($ms, [System.Drawing.Imaging.ImageFormat]::Png)
  $blobs += , $ms.ToArray()
  $ms.Dispose(); $out.Dispose()
}
$ico = New-Object System.IO.MemoryStream
$bw = New-Object System.IO.BinaryWriter $ico
$bw.Write([uint16]0); $bw.Write([uint16]1); $bw.Write([uint16]$icoSizes.Count)
$offset = 6 + 16 * $icoSizes.Count
for ($i = 0; $i -lt $icoSizes.Count; $i++) {
  $s = $icoSizes[$i]
  $bw.Write([byte]$(if ($s -ge 256) { 0 } else { $s }))
  $bw.Write([byte]$(if ($s -ge 256) { 0 } else { $s }))
  $bw.Write([byte]0); $bw.Write([byte]0)
  $bw.Write([uint16]1); $bw.Write([uint16]32)
  $bw.Write([uint32]$blobs[$i].Length)
  $bw.Write([uint32]$offset)
  $offset += $blobs[$i].Length
}
foreach ($b in $blobs) { $bw.Write($b) }
$bw.Flush()
[System.IO.File]::WriteAllBytes((Join-Path $pub 'favicon.ico'), $ico.ToArray())
Write-Host ("  favicon.ico -> {0} px, {1} bytes" -f ($icoSizes -join '/'), (Get-Item (Join-Path $pub 'favicon.ico')).Length)
$bw.Dispose(); $ico.Dispose(); $fav.Dispose()
