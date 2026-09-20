# Phase 4 experiment log

## Scope and guardrails

The experiment stayed in `C:\Users\Welcome\Downloads\ChatGPT-Android-Feasibility`. The original research workspace was read-only for reference. No MoCHi source, database, Cloudflare resource, Vercel resource, production configuration, real API key, provider credential, payment credential, or personal data was used.

## Baseline and artifact preservation

1. Confirmed the existing AVD `ChatGPT-Research-API35` on Android 15/API 35, x86_64 Google Play, with ADB serial `emulator-5554`.
2. Started the existing emulator and waited for ADB to report `device`.
3. Copied the four installed original splits into `01-baseline/original-installed-splits` and created `installed-split-inventory.csv`.
4. Copied the existing Apktool decode into `02-modified-apk/decoded-working-copy/apktool-decode`; the original decoded tree was not edited.
5. Captured the unmodified logged-out UI in `01-baseline/baseline-unmodified.png`.
6. The initial `am start` attempt used an invalid fully qualified activity name and returned `Error type 3`; the package was present. The correct activity is `com.openai.chatgpt/.MainActivity`.

## Resource modification experiment

### Candidate 1 — inactive composer label

- Changed the isolated `fc` resource (`0x7f140106`) from `Ask ChatGPT` to `Ask ChatGPT [Phase 4]`.
- Static reference: `smali_classes5/t8k.smali` loads `0x7f140106` before constructing the `ash("ask_chatgpt", ...)` action.
- Apktool rebuilt the base split. All four experimental splits were signed with `02-modified-apk/signing/phase4-test.keystore`.
- Installing over the original failed with the expected signature mismatch. After uninstalling the original package, `adb install-multiple` succeeded and the app launched.
- The changed label did not appear in the logged-out screenshot. This candidate was recorded as a negative result and reverted.

### Candidate 2 — active onboarding title

- Bytecode/resource cross-check identified `0x7f140986` in `defpackage/xek.java` and `smali_classes5/xek.smali` as the active onboarding branch.
- Changed only `res/values/strings.xml`:
  `APKTOOL_RENAMED_0x7f140986: Welcome to ChatGPT` → `Welcome to ChatGPT [Phase 4]`.
- Rebuilt `base-modified-unsigned-v3.apk`, signed the base and matching configuration splits, verified each with `apksigner`, and installed the complete set.
- Captured visible execution in `02-modified-apk/runtime-evidence/modified-v3-onboarding.png`.
- The changed title was visibly rendered, establishing that this resource path executes in the rebuilt app.

## Restoration

1. Uninstalled the test-signed package.
2. Reinstalled the preserved original `base.apk`, `split_config.en.apk`, `split_config.x86_64.apk`, and `split_config.xxhdpi.apk` with `adb install-multiple`.
3. Launched `com.openai.chatgpt/.MainActivity` and confirmed version `1.2026.230` / code `2623031`.
4. Captured `01-baseline/original-restored-onboarding.png`, showing the original `Welcome to ChatGPT` title.

## Independent prototype

- Implemented a new package, `com.example.androidfeasibility`, with no code copied from MoCHi or the official app.
- Built with local JDK/Android SDK tools because a usable Gradle dependency setup was not required for this experiment.
- Core tests passed: `ALL CORE TESTS PASSED`.
- Built, aligned, and signed `05-independent-prototype/build-output/local-stream-lab.apk`.
- Installed and smoke-tested on the same emulator using synthetic prompts and a deterministic mock provider.
- Exercised normal completion, slow incremental streaming, cancellation, failure before content, failure after partial content, duplicate terminal events, process restart/restoration, and background/foreground return.
- No real network, API key, account, file, microphone, or voice session was used.

## Tooling notes

The first prototype build exposed and then corrected three isolated script/tooling issues: D8 was given a directory instead of class files, AAPT2 needed the generated resource APK passed with `-R`, and the manifest needed an explicit package. These corrections are retained in the build script; they do not change the original application.
