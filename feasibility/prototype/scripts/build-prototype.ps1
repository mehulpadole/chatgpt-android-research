$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$sdk = 'C:\Users\Welcome\AppData\Local\Android\Sdk'
$buildTools = Join-Path $sdk 'build-tools\36.0.0'
$androidJar = Join-Path $sdk 'platforms\android-37.0\android.jar'
$javaHome = 'C:\Program Files\Android\Android Studio\jbr'
$env:JAVA_HOME = $javaHome
$env:Path = "$javaHome\bin;$env:Path"
$java = Join-Path $javaHome 'bin\javac.exe'
$jar = Join-Path $javaHome 'bin\jar.exe'
$aapt2 = Join-Path $buildTools 'aapt2.exe'
$d8 = Join-Path $buildTools 'd8.bat'
$zipalign = Join-Path $buildTools 'zipalign.exe'
$apksigner = Join-Path $buildTools 'apksigner.bat'
$classes = Join-Path $root 'build\android-classes'
$classJar = Join-Path $root 'build\prototype-classes.jar'
$dex = Join-Path $root 'build\dex'
$flat = Join-Path $root 'build\flat'
$generated = Join-Path $root 'build\generated'
$out = Join-Path $root 'build-output'
foreach ($dir in @($classes, $dex, $flat, $generated, $out)) { New-Item -ItemType Directory -Force -Path $dir | Out-Null }
$sourceFiles = Get-ChildItem -LiteralPath (Join-Path $root 'src\main\java') -Recurse -Filter '*.java'
& $java --release 8 -classpath $androidJar -d $classes @($sourceFiles.FullName)
if ($LASTEXITCODE -ne 0) { throw "javac failed with exit code $LASTEXITCODE" }
& $jar cf $classJar -C $classes .
if ($LASTEXITCODE -ne 0) { throw "jar creation failed with exit code $LASTEXITCODE" }
& $d8 --lib $androidJar --output $dex $classJar
if ($LASTEXITCODE -ne 0) { throw "d8 failed with exit code $LASTEXITCODE" }
& $aapt2 compile --dir (Join-Path $root 'res') -o $flat
if ($LASTEXITCODE -ne 0) { throw "aapt2 compile failed with exit code $LASTEXITCODE" }
$unsigned = Join-Path $out 'local-stream-lab-unsigned.apk'
$manifest = Join-Path $root 'src\main\AndroidManifest.xml'
$flatFiles = Get-ChildItem -LiteralPath $flat -Filter '*.flat' | Select-Object -ExpandProperty FullName
$linkArgs = @('-o', $unsigned, '-I', $androidJar, '--manifest', $manifest,
    '--min-sdk-version', '32', '--target-sdk-version', '35', '--version-code', '1',
    '--version-name', '0.1-phase5', '--auto-add-overlay')
foreach ($flatFile in $flatFiles) { $linkArgs += @('-R', $flatFile) }
& $aapt2 link @linkArgs
if ($LASTEXITCODE -ne 0) { throw "aapt2 link failed with exit code $LASTEXITCODE" }
& $jar uf $unsigned -C $dex classes.dex
if ($LASTEXITCODE -ne 0) { throw "jar update failed with exit code $LASTEXITCODE" }
$aligned = Join-Path $out 'local-stream-lab-aligned.apk'
& $zipalign -f 4 $unsigned $aligned
if ($LASTEXITCODE -ne 0) { throw "zipalign failed with exit code $LASTEXITCODE" }
$keystore = Join-Path $root 'signing\prototype-test.keystore'
New-Item -ItemType Directory -Force -Path (Split-Path $keystore) | Out-Null
$testPassword = 'prototype-test-only'
if (-not (Test-Path $keystore)) {
    & (Join-Path $javaHome 'bin\keytool.exe') -genkeypair -keystore $keystore -storepass $testPassword -keypass $testPassword -alias prototype -keyalg RSA -keysize 2048 -validity 3650 -dname 'CN=Local Stream Lab, OU=Research, O=Local Prototype, C=US' | Out-Null
}
$signed = Join-Path $out 'local-stream-lab.apk'
& $apksigner sign --ks $keystore --ks-key-alias prototype --ks-pass "pass:$testPassword" --key-pass "pass:$testPassword" --out $signed $aligned
if ($LASTEXITCODE -ne 0) { throw "apksigner failed with exit code $LASTEXITCODE" }
& $apksigner verify --verbose --print-certs $signed
Get-FileHash -Algorithm SHA256 $signed
