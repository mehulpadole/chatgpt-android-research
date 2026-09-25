# Phases 6–10 Final Evidence Matrix

| Area | Classification | Evidence / limitation |
|---|---|---|
| Real-provider-neutral OpenRouter codec/adapter | Confirmed locally | codec, adapter, credential boundary, and local SSE tests |
| Live OpenRouter account/model request | Not tested | no credential was available or requested |
| Attachment domain/picker/preparation | Confirmed locally; UI Not tested | contract/preparation tests; no device |
| Dictation/TTS/realtime contracts | Confirmed locally; device audio Not tested | voice state/contract tests |
| Local-first sync/outbox/cursor/conflicts | Confirmed locally | sync and two-device simulations |
| Java staging HTTP sync transport | Confirmed locally | in-process HTTP integration and Python staging smoke |
| Android sync/background lifecycle | Not tested | no connected emulator/device |
| MoCHi identity/navigation/provider registry | Confirmed in source/build | registry/project/memory tests and signed APK |
| Projects and memory | Confirmed in-memory | durable production repository not claimed |
| JSON/TXT/ZIP import/export | Confirmed locally | round-trip and adversarial archive tests |
| Phase 5 migration compatibility | Confirmed by fixture | migration and Android codec compatibility tests |
| Secret/PII export and sync boundary | Confirmed by scan/tests | no provider credentials in exports/outbox |
| Debug/internal build profiles | Confirmed | fresh APK/signing runs |
| Production release profile | Not implemented | intentionally gated |
| Accessibility source affordances | Confirmed statically | runtime TalkBack/layout checks Not tested |
| Performance | Confirmed as JVM microbenchmark | Android device performance Not tested |
| Production MoCHi infrastructure | Intentionally untouched | no Cloudflare/R2/auth/billing/deployment changes |
