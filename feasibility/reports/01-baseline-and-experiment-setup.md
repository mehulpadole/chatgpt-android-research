# Baseline, provenance, and experiment setup

## Artifact and emulator

- Package: `com.openai.chatgpt`
- Version: `1.2026.230`
- Version code: `2623031`
- AVD: `ChatGPT-Research-API35`
- Runtime: Android 15/API 35, x86_64 Google Play image
- ADB: `emulator-5554`
- Activity: `com.openai.chatgpt/.MainActivity`

The original split set was copied to `01-baseline/original-installed-splits` before modification. The full decoded tree was copied into `02-modified-apk/decoded-working-copy/apktool-decode`. The isolated copy was the only decode edited.

## Original archive and installed split hashes

Archive:

`C:\Users\Welcome\Downloads\ChatGPT-Android-Research\01-original-apks\com.openai.chatgpt_1.2026.230-2623031_4arch_7dpi_25lang_057f11437587689657ad6e6327b4a659_apkmirror.com.apkm`

| Artifact | SHA-256 |
|---|---|
| archive | `097D7B60C51F14F35923C8E41D033E526AE9E6886BA7BBD276417EACB55B2252` |
| installed `base.apk` | `F44373076ACABD320F1FB23E8ED1BFC17871B3D527506EAD613862DCF09B2763` |
| installed `split_config.en.apk` | `252C02A42FAF75B7F8DEF98204EA8507559042729423974C21BB710CA6106719` |
| installed `split_config.x86_64.apk` | `50446237758B7D7811370DC16E9534B2EA8EAE755253717AE545335A5E036E14` |
| installed `split_config.xxhdpi.apk` | `D7A40F66EEF4E96C24A9894C305E6CF72DB8DDA237B9CE7AF458C3EEB6FA0042` |

The archive’s MD5 is `A9B5B104E49FABAD28B4CAE52A8C21DA`; SHA-1 is `3FDA035CAE6A821707B569ACA484A28A13A65B67`. The archive page/hash match and the byte-match between preserved archive copies and pulled installed splits establish artifact identity and installation equivalence for those bytes. They do not prove that APKMirror is an official publisher or that the signing key is controlled by OpenAI.

## Signing certificate provenance

All 37 preserved APKs verified with `apksigner` and presented the same certificate:

- Subject: `CN=Android, OU=Android, O=Google Inc., L=Mountain View, ST=California, C=US`
- SHA-1: `51a2f260766c9c1a83b7dd5b4572040ac23e4aea`
- SHA-256: `b24f4bfbb3cf293f938703b9d87027c1102cc36dc4fa206910e08927db40473c`

This is a confirmed property of the preserved artifact. An independent comparison against a trusted official OpenAI/Google signing-certificate reference was not located, so official-key attribution remains `Unknown` rather than `Confirmed`.

## Baseline runtime evidence

`01-baseline/baseline-unmodified.png` and `01-baseline/original-restored-onboarding.png` show the logged-out shell/onboarding. This confirms startup and rendering of the shell only. It is not evidence for account-backed chat, history, streaming, attachments, voice, or persistence.

## Modification levels

| Level | Result |
|---|---|
| Decodable | Confirmed — existing Apktool decode was copied and read |
| Rebuildable | Confirmed — modified base rebuilt with Apktool |
| Signable | Confirmed — separate local Phase 4 key |
| Installable | Confirmed — complete test-signed split set installed after uninstall |
| Update-compatible with original | Not compatible by design — Android rejected the different signer |
| Launchable | Confirmed — `MainActivity` started |
| Changed behavior executes | Confirmed for the onboarding title resource |
| Authenticated behavior changed | Unknown / not tested |

## Restoration status

The test-signed package was removed and the preserved original four splits were reinstalled. The final emulator state was confirmed as original version `1.2026.230` / code `2623031`, with the original onboarding title visible.
