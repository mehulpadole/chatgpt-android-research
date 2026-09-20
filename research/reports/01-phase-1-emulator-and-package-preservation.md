# Phase 1 — Emulator and Package Preservation

## Emulator

Confirmed from the local AVD configuration, system-image metadata, `emulator -list-avds`, and ADB:

- AVD: `ChatGPT-Research-API35`
- Display name: `ChatGPT Research API 35 (Google Play)`
- System image: Android 15/API 35, x86_64, `google_apis_playstore`, revision 9
- Play Store enabled: yes
- Device model: `sdk_gphone64_x86_64`
- ABI list: `x86_64,arm64-v8a`
- Native bridge: `libndk_translation.so`
- Screen: 1080x2400, density 420
- ADB serial: `emulator-5554`

Preserved setup evidence:

- `02-package-metadata/ChatGPT-Research-API35.config.ini`
- `02-package-metadata/ChatGPT-Research-API35.ini`
- `02-package-metadata/system-image-source.properties`
- `02-package-metadata/emulator-getprop.txt`
- `02-package-metadata/emulator-settings-global.txt`

The AVD was started with no-snapshot, no-boot-animation, automatic GPU, no network delay, and full network speed. ADB reported the emulator as `device`, and package-manager commands succeeded. The Play Store package (`com.android.vending`) and Google services packages were present. `market://details?id=com.openai.chatgpt` did not resolve; launching the Play Store directly led to its unauthenticated activity. No account or credential was entered.

## Fallback source and integrity

Source: [APKMirror ChatGPT 1.2026.230 universal bundle](https://www.apkmirror.com/apk/openai/chatgpt/chatgpt-1-2026-230-release/chatgpt-1-2026-230-android-apk-download/). The browser download produced the preserved file:

`01-original-apks/com.openai.chatgpt_1.2026.230-2623031_4arch_7dpi_25lang_057f11437587689657ad6e6327b4a659_apkmirror.com.apkm`

Archive facts confirmed locally and against the source page:

- Size: 64,761,801 bytes
- MD5: `a9b5b104e49fabad28b4cae52a8c21da`
- SHA-1: `3fda035cae6a821707b569aca484a28a13a65b67`
- SHA-256: `097d7b60c51f14f35923c8e41d033e526ae9e6886ba7bbd276417eacb55b2252`
- Bundle contents: `base.apk` plus 36 splits
- ABIs: arm64-v8a, armeabi-v7a, x86, x86_64
- Densities: 120, 160, 213, 240, 320, 480, 640 dpi
- Languages: 25 configuration splits

The APKMirror download endpoint challenged direct command-line requests, but the browser session obtained the file without bypassing the challenge. An invalid-nonce HTML response from an earlier direct request is retained as `06-analysis/apkmirror-invalid-nonce-response.html` and is not treated as an APK.

## Package identity and signing

The archive base and all 36 split APKs pass `apksigner verify --print-certs` with the same certificate:

- DN: `CN=Android, OU=Android, O=Google Inc., L=Mountain View, ST=California, C=US`
- SHA-1: `51a2f260766c9c1a83b7dd5b4572040ac23e4aea`
- SHA-256: `b24f4bfbb3cf293f938703b9d87027c1102cc36dc4fa206910e08927db40473c`

The APKPure page independently lists the same package and SHA-1 certificate fingerprint, but its available variant was ARM64-only and was not used. See [APKPure’s version page](https://apkpure.net/chatgpt/com.openai.chatgpt/download/1.2026.230).

## Installed split preservation

The package was installed with:

- `base.apk`
- `split_config.en.apk`
- `split_config.x86_64.apk`
- `split_config.xxhdpi.apk`

The installed paths were recorded in `02-package-metadata/installed-package-paths.txt`, and the pulled files are preserved under `01-original-apks/installed-com.openai.chatgpt-2623031/`. Their SHA-256 values match the archive inventory exactly. Full archive and installed inventories are in `02-package-metadata/archive-split-inventory.csv` and `02-package-metadata/installed-split-inventory.csv`.
