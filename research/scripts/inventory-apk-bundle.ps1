param(
    [Parameter(Mandatory = $true)]
    [string]$ApkDirectory,

    [Parameter(Mandatory = $true)]
    [string]$OutputCsv,

    [Parameter(Mandatory = $true)]
    [string]$OutputText,

    [string]$Aapt = 'C:\Users\Welcome\AppData\Local\Android\Sdk\build-tools\36.0.0\aapt.exe',
    [string]$ApkSigner = 'C:\Users\Welcome\AppData\Local\Android\Sdk\build-tools\36.0.0\apksigner.bat'
)

$ErrorActionPreference = 'Stop'
$files = Get-ChildItem -LiteralPath $ApkDirectory -Filter '*.apk' -File | Sort-Object Name
$rows = foreach ($file in $files) {
    $md5 = (Get-FileHash -LiteralPath $file.FullName -Algorithm MD5).Hash
    $sha1 = (Get-FileHash -LiteralPath $file.FullName -Algorithm SHA1).Hash
    $sha256 = (Get-FileHash -LiteralPath $file.FullName -Algorithm SHA256).Hash

    $aaptText = (& $Aapt dump badging $file.FullName 2>&1 | Out-String)
    $packageLine = ($aaptText -split "`r?`n" | Where-Object { $_ -match '^package:' } | Select-Object -First 1)
    $splitLine = ($aaptText -split "`r?`n" | Where-Object { $_ -match '^split=' } | Select-Object -First 1)
    $certText = (& $ApkSigner verify --print-certs $file.FullName 2>&1 | Out-String)
    $certLines = $certText -split "`r?`n" | Where-Object {
        $_ -match 'Signer #1 certificate DN|Signer #1 certificate SHA-1 digest|Signer #1 certificate SHA-256 digest'
    }

    [pscustomobject]@{
        File = $file.Name
        Bytes = $file.Length
        MD5 = $md5
        SHA1 = $sha1
        SHA256 = $sha256
        PackageLine = if ($packageLine) { $packageLine.Trim() } else { '' }
        SplitLine = if ($splitLine) { $splitLine.Trim() } else { '' }
        Certificate = ($certLines -join ' | ').Trim()
    }
}

$rows | Export-Csv -LiteralPath $OutputCsv -NoTypeInformation -Encoding UTF8
$rows | Format-List | Out-File -LiteralPath $OutputText -Encoding UTF8
Write-Output "Inventoried $($rows.Count) APK files."
