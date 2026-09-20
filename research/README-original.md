# ChatGPT Android Research Workspace

This workspace contains a read-only research copy and analysis of the `com.openai.chatgpt` Android package. It is intentionally outside the MoCHi project; MoCHi was not opened or modified.

## Status

- Workspace created: 2026-09-07
- Phase 1: complete using the `ChatGPT-Research-API35` Android 15/API 35 Google Play AVD; package preservation complete.
- Phase 2: complete/partially constrained by JADX decompiler limitations; Apktool, JADX fallback output, native-library inventory, and corroborating evidence are preserved.
- Phase 3: logged-out runtime shell and composer confirmed; authenticated and privacy-sensitive flows were not tested.

## Current artifact and runtime status

- Package: `com.openai.chatgpt`, ChatGPT `1.2026.230`, version code `2623031`.
- Emulator: `ChatGPT-Research-API35`, Android 15/API 35, x86_64 Google Play image, ADB serial `emulator-5554`.
- The Play Store and Google services are present, but no Google account was entered. The Play-delivered installation therefore could not be completed in this session.
- Fallback source: APKMirror’s universal bundle, preserved under `01-original-apks/`; its base APK plus the x86_64, English, and xxhdpi splits were installed and pulled back from `/data/app`.
- The emulator was restarted after analysis and left running with ADB online for continued work.

The detailed continuation is in 08-reports/05-phase-3-feature-traces-and-evidence-matrix.md, including feature traces, provenance limits, and the evidence matrix.

See `08-reports/00-executive-summary.md` and the Phase 1–3 reports for the evidence, hashes, signing certificate, runtime boundaries, and limitations.

## Layout

- `01-original-apks/` — untouched APK copies and hashes.
- `02-package-metadata/` — package-manager output, device metadata, and inventories.
- `03-jadx-output/` — JADX decompilation output.
- `04-apktool-output/` — Apktool decoded resources and manifests.
- `05-native-libraries/` — copied native-library inventory and inspection notes.
- `06-analysis/` — search results, scripts' generated summaries, and architecture evidence.
- `07-observations/` — normal on-device observations and screenshots/log references.
- `08-reports/` — human-readable reports.
- `scripts/` — reproducible helper scripts.
- `tools/` — locally scoped analysis tools.
- `scripts/inventory-apk-bundle.ps1` — reproducible archive/installed-split inventory and certificate check.
- `scripts/collect-static-evidence.ps1` — reproducible static evidence collection.

## Tools

- Android SDK Platform Tools / `adb`: `C:\Users\Welcome\AppData\Local\Android\Sdk\platform-tools\adb.exe`, version 37.0.1.
- Android Studio: `C:\Program Files\Android\Android Studio\studio64.exe`.
- Bundled Java: `C:\Program Files\Android\Android Studio\jbr\bin\java.exe`, OpenJDK 25.0.2.
- JADX 1.5.6: `tools/jadx/bin/jadx.bat` and `tools/jadx/bin/jadx-gui.bat`.
- Apktool 3.0.3: `tools/apktool/apktool.bat`.
- Android build tools `aapt`: SDK build-tools 36.0.0.

JADX requires `JAVA_HOME` to point to the Android Studio bundled JDK when invoked from a fresh shell.

## Scope and handling

The original APK copies are preserved and must not be modified, resigned, rebuilt, or installed. Findings use `Confirmed`, `Inferred`, and `Unknown` confidence labels. Sensitive values such as tokens, cookies, credentials, and personal data must not be copied into reports. The APKMirror archive is retained as the source artifact; the installed four-split set is separately preserved and byte-compared to the archive copies.
