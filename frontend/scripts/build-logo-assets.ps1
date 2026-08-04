# Derive every logo and favicon asset from the design source, so that replacing the logo is one
# command instead of eight hand-exported files that drift apart.
#
#   pwsh frontend/scripts/build-logo-assets.ps1
#
# Windows only: it uses System.Drawing rather than adding an image dependency to the frontend
# for something that runs by hand a few times a year.
#
# The source is a 2814x1536 canvas from an image generator: a white rounded-square plate holding
# the emblem, on a very light grey field (242,243,248), with a generator watermark in one corner.
# Hence crop to the plate, drop the field to transparency — a grey corner would show up on a dark
# browser tab — and only then scale down.
#
# Replacing the logo with a differently framed source means re-measuring $cx/$cy/$side. The plate
# bounds are the bounding box of near-white (>=252) columns and rows that contain more than a
# handful of such pixels; that test ignores the watermark, which is small.
Add-Type -AssemblyName System.Drawing

$repo = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$src = Join-Path $repo 'docs\AssetTrace_Logo_V2.png'
$pub = Join-Path $repo 'frontend\public'
$logoDir = Join-Path $pub 'static\images\logo'

# Plate measured at x 840..1974, y 202..1350 in the current source.
$cx = 1407; $cy = 776; $side = 1149
$cropX = [int]($cx - $side / 2); $cropY = [int]($cy - $side / 2)

$source = [System.Drawing.Bitmap]::FromFile($src)

# --- crop to the plate ---
$plate = New-Object System.Drawing.Bitmap $side, $side, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$g = [System.Drawing.Graphics]::FromImage($plate)
$g.DrawImage($source, (New-Object System.Drawing.Rectangle 0, 0, $side, $side),
             (New-Object System.Drawing.Rectangle $cropX, $cropY, $side, $side),
             [System.Drawing.GraphicsUnit]::Pixel)
$g.Dispose(); $source.Dispose()

# --- background field -> transparent ---
# Tolerance 8 around the field colour. The plate is 254-255 and the lightest emblem tone is
# (138,182,221), so nothing inside the mark is anywhere near this range.
$rect = New-Object System.Drawing.Rectangle 0, 0, $side, $side
$data = $plate.LockBits($rect, [System.Drawing.Imaging.ImageLockMode]::ReadWrite, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$stride = $data.Stride
$bytes = New-Object byte[] ($stride * $side)
[System.Runtime.InteropServices.Marshal]::Copy($data.Scan0, $bytes, 0, $bytes.Length)
$bg = @(248, 243, 242)  # B,G,R
$tol = 8
$cleared = 0
for ($y = 0; $y -lt $side; $y++) {
  $off = $y * $stride
  for ($x = 0; $x -lt $side; $x++) {
    $i = $off + $x * 4
    if ([math]::Abs($bytes[$i] - $bg[0]) -le $tol -and
        [math]::Abs($bytes[$i + 1] - $bg[1]) -le $tol -and
        [math]::Abs($bytes[$i + 2] - $bg[2]) -le $tol) {
      $bytes[$i + 3] = 0; $cleared++
    }
  }
}
[System.Runtime.InteropServices.Marshal]::Copy($bytes, 0, $data.Scan0, $bytes.Length)
$plate.UnlockBits($data)
"transparent pixels: $cleared of $($side*$side)"

function Save-Scaled([System.Drawing.Bitmap]$bmp, [int]$size, [string]$path) {
  $out = New-Object System.Drawing.Bitmap $size, $size, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
  $gg = [System.Drawing.Graphics]::FromImage($out)
  $gg.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
  $gg.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
  $gg.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
  $gg.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality
  $gg.DrawImage($bmp, (New-Object System.Drawing.Rectangle 0, 0, $size, $size))
  $gg.Dispose()
  $out.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
  $out.Dispose()
  # Write-Host, not the pipeline: a returned string would mix with the caller's output.
  Write-Host ("{0} -> {1}x{1}, {2} bytes" -f [System.IO.Path]::GetFileName($path), $size, (Get-Item $path).Length)
}

# --- the assets referenced by index.html, manifest.json and useBrand ---
# 256 for the two sidebar logos, not 512: they load on every page view and render at 60px, so
# 256 is still 4x oversampled while staying above the 144px minimum for a Twitter summary card,
# which reuses logo.png. 512 would cost ~270 KB extra per visit for nothing visible.
Save-Scaled $plate 256 (Join-Path $logoDir 'logo.png')
Save-Scaled $plate 256 (Join-Path $logoDir 'logo-white.png')
foreach ($s in 192, 256, 384, 512) {
  Save-Scaled $plate $s (Join-Path $logoDir "icon-$s.png")
}
Save-Scaled $plate 32 (Join-Path $pub 'favicon-32x32.png')
Save-Scaled $plate 16 (Join-Path $pub 'favicon-16x16.png')

# --- favicon.ico with 16/32/48 entries, each a PNG payload ---
# PNG-in-ICO is understood by every browser in use and avoids hand-rolling BMP masks.
$icoSizes = 16, 32, 48
$blobs = @()
foreach ($s in $icoSizes) {
  $out = New-Object System.Drawing.Bitmap $s, $s, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
  $gg = [System.Drawing.Graphics]::FromImage($out)
  $gg.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
  $gg.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
  $gg.DrawImage($plate, (New-Object System.Drawing.Rectangle 0, 0, $s, $s))
  $gg.Dispose()
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
"favicon.ico -> $($icoSizes -join '/') px, $((Get-Item (Join-Path $pub 'favicon.ico')).Length) bytes"
$bw.Dispose(); $ico.Dispose(); $plate.Dispose()
