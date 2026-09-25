# SDD ledger — plan: docs/superpowers/plans/2026-09-25-mochi-phases-6-10.md

Base: d5555122459dd7aaf9ef9c218e91066731a677dd (Phase 5 provider-neutral boundary)

Spec read: /home/mehul/.codex/attachments/5277dde5-1c05-41f0-8657-c22863330978/Pasted text.txt

Pre-flight interface scan:
- Task 2 produces provider-neutral model metadata and an OpenRouter codec consumed by Task 3's adapter and Task 4's settings controller; names are aligned as ProviderModel, ProviderCapabilities, and OpenRouterCodec.
- Task 3 produces ProviderCredentialStore, Redaction, and OpenRouterProviderAdapter consumed by Task 4 and later multimodal tasks; credentials remain outside ProviderRequest and conversation persistence.
- Task 5 produces structured content parts and attachment persistence consumed by Task 6 and sync tasks; binary bytes remain outside conversation JSON.
- Task 7 consumes ConversationCoordinator and produces independent voice contracts consumed by Task 10 UI; voice adapters do not own conversation persistence.
- Task 8 produces SyncOutbox/SyncClient/cursor contracts consumed by Task 9 scheduling and by Task 10 product settings; provider credentials are explicitly excluded from sync payloads.
- Task 10 produces product registries/projects/memory consumed by Task 11 import/export and Task 12 final matrix.

Ruling: No installed emulator AVD was listed during baseline inspection — continue implementation with emulator/device coverage marked Not tested until an actual AVD or device is available; do not infer UI success from APK build output. Cost if wrong: runtime regressions may remain undiscovered.

Ruling: No OpenRouter credential is available in the repository/worktree — implement and test the adapter against local deterministic SSE fixtures, keep real-provider execution explicitly Not tested, and never create or request a production secret in source. Cost if wrong: the live provider contract may differ despite official-documentation alignment.

Ruling: The current repository is a minimal original Java prototype rather than the production MoCHi codebase — implement phases as isolated original prototype/product-shell components and do not invent production Cloudflare or billing integrations. Cost if wrong: additional integration work will be required when the real MoCHi product repository is supplied.

Task 1: complete (commits d555512..4aa4da4, tests: run-core-tests.sh + audit-provider-boundary.sh + build-prototype.sh; seven baseline tests pass, APK/signing pass, emulator Not tested)

Task 2: complete (commits 4aa4da4..2c47f25, tests: OpenRouterCodecTest RED→GREEN; run-core-tests.sh → 8 test entry points pass)

Task 3: complete (commits 2c47f25..6a10fec, tests: OpenRouterProviderAdapterTest and CredentialBoundaryTest RED→GREEN; run-core-tests.sh → 10 entry points pass; build-prototype.sh → APK/signature pass)

Task 4: complete (commits 6a10fec..1e46a3b, tests: ProviderSettingsControllerTest RED→GREEN; run-core-tests.sh → 11 entry points pass; build-prototype.sh → APK/signature pass; live credential/emulator explicitly Not tested)

Task 5: complete (commits 1e46a3b..d5d9a23, tests: AttachmentValidatorTest and AttachmentContractTest RED→GREEN; run-core-tests.sh → 13 entry points pass; build-prototype.sh → APK/signature pass)

Task 6: complete (commits d5d9a23..c985835, tests: AttachmentPreparationTest and OpenRouterAttachmentAdapterTest RED→GREEN; run-core-tests.sh → 15 entry points pass; build-prototype.sh → APK/signature pass; emulator/live multimodal explicitly Not tested)

Task 7: complete (commits c985835..b4d8e81, tests: VoiceSessionCoordinatorTest and VoiceContractTest RED→GREEN; run-core-tests.sh → 17 entry points pass; build-prototype.sh → APK/signature pass with Android TTS deprecation note; device audio/realtime explicitly Not tested)

Task 8: complete (commits b4d8e81..pending, tests: SyncContractTest, SyncOutboxTest, SyncConflictTest, and SyncRetryTest RED→GREEN; run-core-tests.sh → 21 entry points pass; Python staging backend syntax and protocol smoke pass for entitlement, cursor changes, idempotent replay, credential rejection, and HTTP 413 quota; Android emulator/device explicitly Not tested)

Task 9: complete (commits 4030af4..pending, focused integration tests RED→GREEN; `SyncWorker`, `AttachmentSyncPolicy`, `SyncHttpTransport`, monotonic cursor, auth-expiry/quota handling, two-device A–E simulation, and Android local-only/cloud-sync selector added; run-core-tests.sh → 23 entry points pass; build-prototype.sh → APK/signature pass; emulator lifecycle/network/background behavior explicitly Not tested)

Task 10: complete (commits 0436b60..pending, ProviderRegistryTest and ProjectMemoryTest RED→GREEN; MoCHi Android identity, explicit navigation state, provider registry/factory, safe endpoint validation, project and scoped-memory repositories added; run-core-tests.sh → 25 entry points pass; build-prototype.sh → APK/signature pass; cross-platform/emulator runtime parity explicitly Not tested)

Task 11: complete (commits e04ac7c..pending, ImportExportSecurityTest RED→GREEN; versioned JSON/TXT/ZIP export/import, Phase 5 migration, duplicate/path/file-type checks, privacy disclosure, endpoint/redirect validator, and debug/internal/release build gate added; run-core-tests.sh → 26 entry points pass; debug APK/signature pass; release build intentionally refused because no production release configuration exists)
