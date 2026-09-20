# ChatGPT Android client research

Public research archive for the Android client investigation and the separate Phase 4 feasibility study. This is a research record and an independent prototype; it is **not** the official ChatGPT Android source code and it does not modify MoCHi.

## Current completion level

| Area | Status | What is actually established |
|---|---|---|
| Phase 1 — emulator, package identity, APK preservation | Complete for the captured build | The package, installed split inventory, version, signing metadata, and hashes were recorded in `research/reports/01-phase-1-emulator-and-package-preservation.md`. |
| Phase 2 — static analysis | Substantially complete for available decompiled material | Streaming, persistence, attachments, navigation, and voice-related traces are documented with concrete class/method/file references. |
| Phase 3 — runtime observations | Partial | Emulator and logged-out flows were tested. Authenticated conversations, attachments, and voice were not tested. The logged-out welcome screen is not evidence for authenticated features. |
| Phase 4 — modification feasibility | Complete for the scoped experiment | A reversible resource patch was rebuilt, installed with a separate research key, launched, and visibly executed. This demonstrates the packaging/modification workflow, not a detached ChatGPT backend or OpenRouter integration. |
| Independent provider-neutral prototype | Complete for the scoped prototype | The prototype exercises streaming, cancellation, failure, persistence, restoration, and background return with a deterministic mock provider. |

Overall: **research complete for the documented scope; product implementation not started**. The next major unknowns are the authenticated backend contract, the compiled Valdi renderer/composer boundary, and production attachment/voice protocols.

## Repository contents

- `research/` — Phase 1–3 reports, evidence inventories, and collection scripts.
- `feasibility/` — Phase 4 reports, experiment log, source references, prototype source, and emulator test evidence.
- `feasibility/prototype/` — the independent Java/Android prototype source and build/test scripts.

## Intentionally omitted

The original workspaces were several gigabytes and contained APKs, split APKs, SDK/tool archives, generated decompilation output, native libraries, build products, and signing material. Those are intentionally excluded from this public repository because GitHub size limits, redistribution/licensing concerns, and generated-file noise make them unsuitable for a public source archive. The reports preserve the relevant hashes, provenance, paths, and limitations.

No official ChatGPT APK, decompiled source tree, signing key, credential, or MoCHi source is included here.

## Reproducing the independent prototype locally

The scripts assume a Windows Android Studio installation and currently contain explicit local SDK/JDK paths from the original experiment. Review and adjust those paths before running them:

```powershell
Set-Location feasibility/prototype
.\scripts\build-prototype.ps1
.\scripts\run-core-tests.ps1
```

The original emulator smoke-test screenshots and UI XML are in `feasibility/tests/`. The prototype uses a deterministic mock provider; it does not log in to ChatGPT or call OpenAI/OpenRouter.

## Evidence rules

The reports distinguish `confirmed`, `inferred`, `unknown`, and `not tested`. In particular, static references are not presented as proof of runtime behavior, and an unauthenticated welcome screen is not used as evidence for authenticated features.

## Attribution and scope

ChatGPT, OpenAI, and related marks belong to their respective owners. This repository contains original research notes and an independently written feasibility prototype. No license is granted to any upstream proprietary material that may be described in the reports.
