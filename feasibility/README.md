# ChatGPT Android Feasibility Study — Phase 4

This workspace records a reversible modification experiment, a static architecture trace, and an independent Android comparison prototype for ChatGPT Android `com.openai.chatgpt` version `1.2026.230` / version code `2623031`.

The work continued from `C:\Users\Welcome\Downloads\ChatGPT-Android-Research`. It used the Windows Android emulator `ChatGPT-Research-API35` (`emulator-5554`) and did not require a physical phone. MoCHi, its database, Cloudflare, Vercel, production configuration, and production data were not opened or modified.

## Result at a glance

- The original four installed APK splits were preserved byte-for-byte in `01-baseline/original-installed-splits`.
- A resource-only patch was decodable, rebuildable, installable with a separate research signing key, launchable, and visibly executed. The changed text was the logged-out onboarding title, not an authenticated chat feature.
- The modified build was uninstalled and the original four splits were restored. The emulator currently has the original package installed and launchable.
- Static tracing confirms a coordinator-driven, incremental conversation pipeline, explicit message/content models, repository/cache/query boundaries, attachment upload state, and a Valdi voice API with several native transport paths.
- Chat rendering and the active composer implementation remain partly inside compiled Valdi modules or heavily obfuscated code. Authenticated chat, history, attachments, and voice were not runtime-tested.
- The independent prototype in `05-independent-prototype` demonstrates a provider-neutral state machine, deterministic streaming, cancellation/error handling, local JSON persistence, and emulator smoke coverage without real credentials or network access.
- Phase 5 adds a provider-neutral request/event contract, a local NDJSON HTTP adapter and backend, one conformance suite shared by mock and HTTP providers, provider selection in the standalone prototype, and portable build/test/audit scripts. The APK build is verified; Phase 5 emulator UI verification is pending because no emulator was connected in the execution environment.

## Directory map

- `01-baseline` — preserved original splits, inventory, package summary, baseline and restored screenshots.
- `02-modified-apk` — isolated decoded working copy, patch record, rebuilt/signed outputs, test key, and modification screenshots.
- `03-component-analysis` — component and feature-trace notes.
- `04-provider-boundary` — provider contract and direct-adaptation assessment.
- `05-independent-prototype` — original Java/Android comparison app, unit tests, and build script.
- `06-tests` — emulator screenshots and UI hierarchy evidence.
- `07-reports` — the Phase 4 report set.
- `scripts` — small evidence/inventory helpers where applicable.
- `05-independent-prototype/test-backend` — original deterministic NDJSON backend for Phase 5; it is not a commercial-provider-compatible service.

## Re-running the independent prototype

From PowerShell:

```powershell
cd C:\Users\Welcome\Downloads\ChatGPT-Android-Feasibility\05-independent-prototype
.\scripts\run-core-tests.ps1
.\scripts\build-prototype.ps1
```

The build is intentionally independent of MoCHi and real providers. It uses the locally installed Android SDK/JDK tools and signs with a local prototype key. Phase 5 also provides `scripts/build-prototype.sh` and `scripts/run-core-tests.sh` for Linux/macOS-style environments, while the existing PowerShell scripts remain available for Windows.

## Evidence policy

`Confirmed` means directly shown by code, bytecode/resource data, a successful build/install/launch, or captured emulator evidence. `Inferred` means a reasoned interpretation supported by multiple references but not directly observed end-to-end. `Unknown` means the available artifact does not expose enough evidence. `Not tested` means the behavior could be testable but was deliberately not exercised, usually because it requires account credentials, user data, microphone permission, or a real provider.

The logged-out welcome/onboarding shell is not treated as evidence for authenticated chat, history, streaming, attachments, or voice.

See `07-reports/00-executive-summary.md` for the conclusion and `07-reports/08-risks-and-unknowns.md` for the complete evidence matrix.
