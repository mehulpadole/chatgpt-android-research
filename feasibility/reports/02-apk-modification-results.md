# APK modification results

## Patch scope

The patch was deliberately tiny and reversible. It changed one string in the isolated decode and did not alter networking, account logic, bytecode control flow, native libraries, or MoCHi.

The patch record is `02-modified-apk/patches/0001-visible-composer-label.patch`. Its filename is retained from the initial candidate; the successful change is the active onboarding title, not the composer label.

## Candidate results

### Candidate 1: `0x7f140106` / `fc`

`smali_classes5/t8k.smali` loads `0x7f140106` before creating the `ash("ask_chatgpt", ...)` action. Changing `Ask ChatGPT` to `Ask ChatGPT [Phase 4]` rebuilt and launched successfully, but the text did not appear in the logged-out screenshot. The candidate was reverted. This is useful negative evidence: a string reference can be real while the corresponding branch is not active in the observed state.

### Candidate 2: `0x7f140986`

The active onboarding branch was located by cross-checking `defpackage/xek.java` with `smali_classes5/xek.smali`. The isolated `res/values/strings.xml` entry was changed from:

```xml
<string name="APKTOOL_RENAMED_0x7f140986">Welcome to ChatGPT</string>
```

to:

```xml
<string name="APKTOOL_RENAMED_0x7f140986">Welcome to ChatGPT [Phase 4]</string>
```

The rebuilt base was `build-output/base-modified-unsigned-v3.apk`; the complete set was signed and installed. `02-modified-apk/runtime-evidence/modified-v3-onboarding.png` visibly shows the changed title, so this resource path is launchable and executed in the emulator.

## Test signing

The experiment key is `02-modified-apk/signing/phase4-test.keystore` and is not an official key.

- Subject: `CN=ChatGPT Phase 4 Research, OU=Research, O=Local Experiment, C=US`
- Certificate SHA-256: `1f567d0a5a9c475fd0eb3bd1b8e92098a167a2421c6f57358954677bd782b019`
- All four test-signed splits passed `apksigner verify --verbose --print-certs`.

Installing the test-signed set over the original correctly failed with `INSTALL_FAILED_UPDATE_INCOMPATIBLE`. After uninstalling the original package, `adb install-multiple` succeeded. This demonstrates packaging/install mechanics, not production update compatibility.

## What the experiment does not establish

- It does not prove that the official app can be re-signed for normal distribution.
- It does not prove that a modified app can authenticate or use the original backend.
- It does not establish that the active message renderer or composer can be changed using the same technique.
- It does not verify an official signing key against a trusted external reference.
- It does not show authenticated chat or any account-backed feature.

The original package was restored before the study continued.
