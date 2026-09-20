$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$testBuild = Join-Path $root 'build\test-classes'
New-Item -ItemType Directory -Force -Path $testBuild | Out-Null
$sourceFiles = Get-ChildItem -LiteralPath (Join-Path $root 'src\main\java') -Recurse -Filter '*.java' |
    Where-Object { $_.Name -notin @('MainActivity.java', 'JsonConversationRepository.java', 'AndroidConversationCodec.java') }
$testFiles = Get-ChildItem -LiteralPath (Join-Path $root 'src\test\java') -Recurse -Filter '*.java'
$javac = 'C:\Program Files\Android\Android Studio\jbr\bin\javac.exe'
& $javac --release 8 -d $testBuild @($sourceFiles.FullName + $testFiles.FullName)
if ($LASTEXITCODE -ne 0) { throw "javac failed with exit code $LASTEXITCODE" }
$java = 'C:\Program Files\Android\Android Studio\jbr\bin\java.exe'
& $java -cp $testBuild com.example.androidfeasibility.PrototypeCoreTest
if ($LASTEXITCODE -ne 0) { throw "core tests failed with exit code $LASTEXITCODE" }
