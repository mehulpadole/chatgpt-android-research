param(
    [Parameter(Mandatory = $true)]
    [string]$Root,

    [string]$Version = '1.2026.230-2623031'
)

$ErrorActionPreference = 'Stop'
$jadx = Join-Path $Root "03-jadx-output\chatgpt-$Version-fallback\sources"
$jadxReadable = Join-Path $Root "03-jadx-output\chatgpt-$Version-nodeobf\sources"
$decoded = Join-Path $Root "04-apktool-output\chatgpt-$Version"
$analysis = Join-Path $Root '06-analysis'
$native = Join-Path $Root "05-native-libraries\chatgpt-$Version\lib\x86_64"
New-Item -ItemType Directory -Path $analysis -Force | Out-Null

function Save-Rg {
    param([string]$Pattern, [string[]]$Paths, [string]$Destination, [int]$Max = 2000)
    $results = & rg.exe -n -i --hidden --glob '*.java' --glob '*.smali' --glob '*.xml' --glob '*.txt' $Pattern @Paths 2>$null | Select-Object -First $Max
    $results | Set-Content -LiteralPath $Destination -Encoding UTF8
}

rg.exe -n '<manifest|<uses-permission|<uses-feature|<application|<activity |<activity-alias|<service |<receiver |<provider |android:name=' (Join-Path $decoded 'AndroidManifest.xml') 2>$null |
    Set-Content -LiteralPath (Join-Path $analysis 'manifest-components.txt') -Encoding UTF8

Get-ChildItem -LiteralPath (Join-Path $jadx 'com\openai') -Recurse -File -Filter '*.java' -ErrorAction SilentlyContinue |
    ForEach-Object { $_.FullName.Substring($jadx.Length + 1) } |
    Sort-Object |
    Set-Content -LiteralPath (Join-Path $analysis 'openai-class-inventory.txt') -Encoding UTF8

Save-Rg 'https?://|wss?://|okhttp|retrofit|websocket|eventsource|server.?sent|stream|graphql|api\.openai|chatgpt\.com' @($jadx, $jadxReadable, (Join-Path $decoded 'res')) (Join-Path $analysis 'network-streaming-keywords.txt')
Save-Rg 'voice|microphone|audio|webrtc|camera|image|attachment|upload|file|screen.?share|accessibility|ocr|barcode' @($jadx, $jadxReadable, (Join-Path $decoded 'res')) (Join-Path $analysis 'media-voice-attachment-keywords.txt')
Save-Rg 'room|datastore|sqlite|sharedpreferences|workmanager|worker|database|conversation|history|message|cache|filesdir' @($jadx, $jadxReadable, (Join-Path $decoded 'res')) (Join-Path $analysis 'storage-history-keywords.txt')
Save-Rg 'compose|recyclerview|paging|glide|coil|markdown|latex|syntax|render|widget|notification|deep.?link|shortcut' @($jadx, $jadxReadable, (Join-Path $decoded 'res')) (Join-Path $analysis 'ui-navigation-keywords.txt')

Get-ChildItem -LiteralPath (Join-Path $decoded 'unknown') -Recurse -File -ErrorAction SilentlyContinue |
    ForEach-Object { $_.FullName.Substring($decoded.Length + 1) } |
    Set-Content -LiteralPath (Join-Path $analysis 'apktool-unknown-files.txt') -Encoding UTF8

Get-ChildItem -LiteralPath $native -File -ErrorAction SilentlyContinue |
    ForEach-Object {
        $hash = (Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash
        "{0}`t{1}`t{2}" -f $_.Name, $_.Length, $hash
    } | Set-Content -LiteralPath (Join-Path $Root "05-native-libraries\native-library-sha256.txt") -Encoding UTF8

Get-ChildItem -LiteralPath (Join-Path $Root '01-original-apks\apkmirror-2623031-universal-extracted\META-INF') -File -ErrorAction SilentlyContinue |
    Select-Object Name, Length | Format-Table -AutoSize | Out-String |
    Set-Content -LiteralPath (Join-Path $analysis 'bundle-signature-metadata.txt') -Encoding UTF8

Write-Output 'Static evidence files collected.'
