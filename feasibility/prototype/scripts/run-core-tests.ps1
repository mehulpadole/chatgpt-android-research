$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$testBuild = Join-Path $root 'build\test-classes'
New-Item -ItemType Directory -Force -Path $testBuild | Out-Null
$sourceFiles = Get-ChildItem -LiteralPath (Join-Path $root 'src\main\java') -Recurse -Filter '*.java' |
    Where-Object { $_.Name -notin @('MainActivity.java', 'JsonConversationRepository.java', 'AndroidConversationCodec.java', 'AndroidCredentialStore.java', 'AndroidAttachmentPicker.java', 'AndroidAttachmentStore.java') }
$testFiles = Get-ChildItem -LiteralPath (Join-Path $root 'src\test\java') -Recurse -Filter '*.java'
$testFiles = $testFiles | Where-Object { $_.Name -notin @('AndroidCodecCompatibilityTest.java', 'AttachmentContractTest.java') }
$javac = 'C:\Program Files\Android\Android Studio\jbr\bin\javac.exe'
& $javac --release 8 -d $testBuild @($sourceFiles.FullName + $testFiles.FullName)
if ($LASTEXITCODE -ne 0) { throw "javac failed with exit code $LASTEXITCODE" }
$codecSource = Join-Path $root 'src\main\java\com\example\androidfeasibility\AndroidConversationCodec.java'
$codecStubs = Get-ChildItem -LiteralPath (Join-Path $root 'src\test\java\org\json') -Recurse -Filter '*.java'
$codecTest = Join-Path $root 'src\test\java\com\example\androidfeasibility\AndroidCodecCompatibilityTest.java'
$attachmentTest = Join-Path $root 'src\test\java\com\example\androidfeasibility\AttachmentContractTest.java'
& $javac --release 8 -classpath $testBuild -d $testBuild @($codecSource, $codecStubs.FullName, $codecTest, $attachmentTest)
if ($LASTEXITCODE -ne 0) { throw "Android codec compatibility test compilation failed" }
$java = 'C:\Program Files\Android\Android Studio\jbr\bin\java.exe'
$testClasses = @(
    'com.example.androidfeasibility.OpenRouterCodecTest',
    'com.example.androidfeasibility.OpenRouterProviderAdapterTest',
    'com.example.androidfeasibility.CredentialBoundaryTest',
    'com.example.androidfeasibility.ProviderSettingsControllerTest',
    'com.example.androidfeasibility.AttachmentValidatorTest',
    'com.example.androidfeasibility.AttachmentContractTest',
    'com.example.androidfeasibility.AttachmentPreparationTest',
    'com.example.androidfeasibility.OpenRouterAttachmentAdapterTest',
    'com.example.androidfeasibility.ProviderContractTest',
    'com.example.androidfeasibility.CoordinatorContractTest',
    'com.example.androidfeasibility.ProviderRouterTest',
    'com.example.androidfeasibility.HttpStreamingProviderAdapterTest',
    'com.example.androidfeasibility.ProviderConformanceTest',
    'com.example.androidfeasibility.AndroidCodecCompatibilityTest',
    'com.example.androidfeasibility.PrototypeCoreTest'
)
foreach ($testClass in $testClasses) {
    & $java -cp $testBuild $testClass
    if ($LASTEXITCODE -ne 0) { throw "test failed: $testClass" }
}
Write-Output "ALL PHASE 5 PURE-JAVA TESTS PASSED ($($testClasses.Count) test entry points)"
