# ChatGPT Android Research — Executive Summary

Date: 2026-09-08  
Package studied: `com.openai.chatgpt`  
Artifact: ChatGPT 1.2026.230, version code 2623031

## Outcome

The physical phone was not used. A new Android Virtual Device named `ChatGPT-Research-API35` was created from the Android 15/API 35 x86_64 Google Play system image. ADB recognized `emulator-5554` as an online device. The Play Store is present, but the emulator has no authenticated Google account, so an in-Play installation could not be completed without user credentials.

The official-looking package was therefore obtained from the APKMirror universal bundle page as a fallback. The downloaded `.apkm` archive is preserved and its SHA-256 matches the archive page’s published digest. It contains the base APK and 36 configuration splits for four ABIs, seven densities, and 25 languages. The base and all splits verify with the same certificate fingerprint published by APKMirror.

The package was installed successfully on the x86_64 emulator using the base APK, English language split, x86_64 ABI split, and xxhdpi density split. The four APKs pulled back from `/data/app` match the corresponding archive files byte-for-byte. The app launched, showed the unauthenticated welcome screen, and reached the main composer after selecting `Skip`. No login, chat submission, file upload, voice recording, screen share, or authenticated network flow was performed.

## Confirmed findings

- `com.openai.chatgpt`, version `1.2026.230`, version code `2623031`, min API 32, target API 37.
- APK signing certificate DN: `CN=Android, OU=Android, O=Google Inc., L=Mountain View, ST=California, C=US`.
- Certificate SHA-1: `51a2f260766c9c1a83b7dd5b4572040ac23e4aea`.
- Certificate SHA-256: `b24f4bfbb3cf293f938703b9d87027c1102cc36dc4fa206910e08927db40473c`.
- The manifest declares 30 permissions, 8 features, 62 activities, 3 aliases, 24 services, 17 receivers, and 12 providers after Apktool decoding.
- The manifest names `MainApplication`, `MainActivity`, deep-link/auth components, conversation streaming and voice services, accessibility/screen-share components, file providers, WorkManager, Room invalidation support, Firebase, Sentry, Stripe, Plaid, and Persona integrations.
- Static URL constants include separate ChatGPT, authenticated backend, anonymous backend, public API, realtime, authentication, and telemetry base URLs. These are embedded constants; actual authenticated use was not tested.
- Native x86_64 libraries include `libopenai_xplat_export.so`, `liblkjingle_peerconnection_so.so`, Sentry libraries, image-processing JNI, and AndroidX/system libraries.

## Phase 3 follow-up

The feature-trace pass is documented in 08-reports/05-phase-3-feature-traces-and-evidence-matrix.md. It confirms static implementation boundaries for:

- typed conversation content and a Java/Valdi rendering split;
- a coordinator/per-turn streaming state machine with terminal, resume, and failsafe paths;
- guarded conversation persistence through ConversationRepository, ConversationCache, and CompleteConversationQueries;
- message-associated attachment upload state and success/failure transitions;
- pluggable voice, dictation, WebRTC/audio, foreground-service, and recording-upload paths.

These are code-backed findings, not authenticated runtime results. The emulator session did not cross login, prompt submission, uploads, voice, or history synchronization. The report also records that APKMirror/archive hash matching and Android signature validity do not independently prove that the observed certificate is OpenAI's official/current Play signing key.

## Main limitations

- The Play Store was unauthenticated, so the installed artifact is a reputable archive fallback rather than a Play-delivered copy.
- Runtime testing stopped at the logged-out main composer. Account-backed chat, streaming, history synchronization, uploads, voice, and settings remain untested.
- JADX’s readable decompiler hit control-flow/type-inference limits in heavily obfuscated Kotlin/Compose code. A complete JADX fallback decode and Apktool output are preserved; conclusions rely on corroborating manifest, resource, class-name, and runtime evidence.

Detailed reports: [Phase 1](C:\Users\Welcome\Downloads\ChatGPT-Android-Research\08-reports\01-phase-1-emulator-and-package-preservation.md), [Phase 2](C:\Users\Welcome\Downloads\ChatGPT-Android-Research\08-reports\02-phase-2-static-analysis.md), [Phase 3 runtime](C:\Users\Welcome\Downloads\ChatGPT-Android-Research\08-reports\03-phase-3-runtime-observations.md), and [Phase 3 feature traces/evidence matrix](C:\Users\Welcome\Downloads\ChatGPT-Android-Research\08-reports\05-phase-3-feature-traces-and-evidence-matrix.md).
