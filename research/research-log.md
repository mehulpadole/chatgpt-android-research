# Research Log

## 2026-09-07 — Initial environment inspection

Scope: official ChatGPT Android package researched in an Android emulator. No physical phone was used. MoCHi is out of scope and has not been opened or modified.

### Workspace

- Research root: `C:\Users\Welcome\Downloads\ChatGPT-Android-Research`
- Created directories for original APKs, metadata, JADX, Apktool, native libraries, analysis, observations, reports, scripts, and local tools.

### Tool findings

- `adb` was not on PATH, but a usable copy exists at `C:\Users\Welcome\AppData\Local\Android\Sdk\platform-tools\adb.exe`.
- ADB version: `1.0.41`, platform-tools `37.0.1-15733141`.
- Android Studio exists at `C:\Program Files\Android\Android Studio\studio64.exe`.
- Android Studio bundled Java exists at `C:\Program Files\Android\Android Studio\jbr\bin\java.exe`; version `OpenJDK 25.0.2`.
- Android SDK build-tools `36.0.0` provides `aapt.exe`.
- JADX was not initially installed. Official release `skylot/jadx` v1.5.6 was downloaded to `tools/jadx-1.5.6.zip` and extracted to `tools/jadx`.
- Apktool was not initially installed. Official release `iBotPeaches/Apktool` v3.0.3 was downloaded to `tools/apktool/apktool_3.0.3.jar` with a local wrapper at `tools/apktool/apktool.bat`.
- JADX CLI version check succeeded after setting `JAVA_HOME` to the Android Studio bundled JDK.
- Apktool version check: `3.0.3`.
- AAPT version: `Android Asset Packaging Tool, v0.2-13193326`.
- Python 3.13 and Git are available on PATH.

### Initial device check

Command run:

```powershell
& 'C:\Users\Welcome\AppData\Local\Android\Sdk\platform-tools\adb.exe' devices -l
```

Result: ADB daemon started successfully, but no devices were listed. This led to the emulator path below; no physical-phone setup was requested or used.

### Sources used for tool acquisition

- JADX official release repository: https://github.com/skylot/jadx/releases/tag/v1.5.6
- Apktool official release repository: https://github.com/iBotPeaches/Apktool/releases/tag/v3.0.3
- Android official Platform Tools documentation: https://developer.android.com/tools/releases/platform-tools

## 2026-09-08 — Emulator continuation and package preservation

### AVD and ADB

- Created `ChatGPT-Research-API35` from the Android 15/API 35 x86_64 `google_apis_playstore` revision 9 system image.
- Confirmed Play Store enabled, Google packages present, Android 15/API 35, ABI `x86_64,arm64-v8a`, and ADB serial `emulator-5554`.
- ADB reported the emulator online as `device`; the final online-state evidence is in `02-package-metadata/final-adb-devices.txt`.

### Play Store and fallback source

- The Play Store was launched, but it was unauthenticated. No Google account or credential was entered.
- The ChatGPT Play listing did not resolve to an installable package in that unauthenticated session, so the Play-delivered artifact could not be preserved.
- The APKMirror universal bundle for ChatGPT `1.2026.230` / version code `2623031` was downloaded through the browser and preserved unchanged. Its archive hash, 37-file split inventory, package identity, and signing certificate were recorded.

### Install and runtime

- Installed the base APK plus `config.en`, `config.x86_64`, and `config.xxhdpi` with `adb install-multiple`.
- Verified `com.openai.chatgpt`, version code `2623031`, target API 37, and all four installed paths. Pulled installed APKs match the corresponding archive files byte-for-byte.
- Launched `MainActivity`; after onboarding, the app reached the logged-out composer. Runtime evidence includes screenshots, UI hierarchy dumps, and filtered logcat.
- Did not enter credentials or test chat submission, uploads, microphone, screen sharing, billing, or authenticated synchronization.

### Static analysis

- Apktool manifest/resource decode completed with unresolved-resource warnings retained in the evidence.
- JADX produced a complete fallback decode; heavily obfuscated Kotlin/Compose code caused decompiler limitations in some paths. Readable classes and corroborating manifest/resource/class/native evidence were used.
- Phase 1, Phase 2, and Phase 3 reports were written under `08-reports/`.

### Final state

The AVD was restarted after static analysis and left running with ADB online. Static analysis can continue without runtime credentials. Confirmed findings and untested boundaries are separated in the reports.

## 2026-09-08 — Phase 3 feature traces and provenance review

- Reviewed the existing Phase 3 runtime report and explicitly separated the confirmed logged-out shell from unauthenticated/authenticated feature claims. The welcome screen is not treated as evidence for authenticated behavior.
- Traced chat rendering, stream handling, composer mutation, persistence, attachments, and voice through the readable JADX fallback, manifest/resources, event models, and compiled Valdi module boundaries.
- Confirmed the strongest static paths: xgf/xef/vef/u790 streaming state; xgf.N0/off/s9g/hyh/q4d persistence; FilePickerService/rcz/s6f/qgf attachment flow; and VoiceService/cy0/mu0/sq0/zp0/co6/VoiceModeForegroundService/VoiceAudioRecordingUploadWorker voice flow.
- Reviewed provenance: the APKMirror archive digest, installed-split byte matches, and apksigner results establish archive/install integrity and Android signature validity. No trusted official OpenAI/Google signing-fingerprint reference was found, so official-key attribution remains unknown.
- Added 08-reports/05-phase-3-feature-traces-and-evidence-matrix.md and linked it from the executive summary and topic matrix. No MoCHi files were changed.
