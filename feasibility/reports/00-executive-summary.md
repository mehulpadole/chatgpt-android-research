# Phase 4 executive summary

## Conclusion

The official ChatGPT Android artifact is technically modifiable at the resource/build level, but the evidence does not support using a modified official client as the foundation for MoCHi. The rebuilt APK can execute a reversible resource change, while the high-value chat behavior is split between a readable-but-obfuscated Android coordinator and compiled Valdi modules whose renderer/composer implementation is not available as portable source. The coordinator also sits beside account, backend, upload, voice, and lifecycle dependencies that are not independently replaceable from the recovered artifact.

The practical path is an independent Android client with an explicit provider adapter. The comparison prototype validates the highest-risk client behaviors—incremental rendering, stable turn/message identity, cancellation, terminal-event guards, failure handling, persistence, and lifecycle restoration—without touching MoCHi or using a real provider.

## What was actually established

| Question | Result | Evidence |
|---|---|---|
| Can the original artifact be preserved and restored? | Confirmed | Four split copies and byte/hash inventory in `01-baseline`; original restored on emulator |
| Can the decoded app be rebuilt? | Confirmed | Apktool rebuilt `base-modified-unsigned-v3.apk` |
| Can a modified complete split set be installed? | Confirmed, with a new key after uninstall | `adb install-multiple` succeeded; update-over-original correctly failed on signature mismatch |
| Does a changed code/resource path execute? | Confirmed for the onboarding resource | `modified-v3-onboarding.png` visibly contains `Welcome to ChatGPT [Phase 4]`; `xek.java`/`xek.smali` reference `0x7f140986` |
| Does authenticated chat work? | Not tested | Runtime stayed logged out; welcome screen is not evidence of authenticated features |
| Is the official signing certificate independently verified? | Unknown | Matching archive/install hashes and consistent certificate were verified; no trusted official OpenAI/Google fingerprint reference was located |
| Is a provider-neutral client feasible? | Confirmed for the tested core | Independent prototype unit tests and emulator smoke tests passed |

## Feature-trace status

- Chat rendering: Valdi/DIL boundary is confirmed; ordinary message renderer internals are unknown because the implementation is in compiled modules.
- Streaming: `xgf.q0` → `xef` → `vef`/`u790` and terminal handlers `g0`/`j0` are confirmed statically; a real response was not observed.
- Composer: `hlx`/`emu` mutation and coordinator persistence/request paths are confirmed; the active Valdi callback and interaction semantics are unknown and not tested.
- Persistence: `s9g`/`hyh`/`q4d` repository, cache, query, and attachment-placeholder boundaries are confirmed; exact database schema/encryption/conflict policy is unknown.
- Attachments: URI registry, message association, upload continuation, success/failure telemetry, and persisted placeholders are confirmed; endpoint/body/auth/retry behavior is unknown and not tested.
- Voice: Valdi API, realtime audio branch, transcription branch, foreground service, and upload worker are confirmed statically; no voice or microphone runtime behavior was tested.

## Five most interesting concrete architectural findings

1. **Streaming is a typed, multi-stage turn pipeline rather than a single response string.** `xgf.q0()` launches the `ConversationCoordinator/streamConversation` coroutine; `xef.invokeSuspend()` creates/tracks `u790`; `vef.a(tlg, ...)` applies typed events; `xgf.g0()` and `xgf.j0()` handle separate completion/done boundaries. References: `source-references.md`, streaming section.
2. **A sticky service provides a lifecycle safety net for active streams.** `ConversationStreamingService.onStartCommand()` schedules a 1,200,000 ms failsafe and the service is designed for sticky restart. This indicates active turns are not assumed to live only inside the foreground activity. Reference: `ConversationStreamingService.java:49–81`.
3. **Conversation writes are guarded and staged through a repository boundary.** `xgf.N0()` creates a new `f5f`, checks content IDs, and schedules `ConversationCoordinator/writeMessagesToDb` before reaching `s9g.y`; `hyh` and `q4d` form cache/query/serialization layers. Reference: `xgf.java:6449–6800`, `s9g.java`, `hyh.java`, `q4d.java`.
4. **Attachments are first-class message state with asynchronous replacement.** `emu` carries content/attachment references; `s6f` maintains URI-keyed upload entries; `qgf` emits started/succeeded/failed stages and routes a replaced `emu` back to the coordinator; `q4d` persists pending placeholders and upload IDs. References: `xgf.java:6817`, `qgf.java:38,417,582`, `q4d.java:315,384`.
5. **Voice is a transport-pluggable cross-platform service.** `VoiceService` exposes Valdi-marshalled `getCapabilities()` and `start()`; `cy0` selects mode/transport; `mu0` owns native audio resources and realtime events; `co6` contains an HTTP transcription branch; foreground service and WorkManager extend lifecycle. References: `VoiceService.java`, `cy0.java`, `mu0.java:167`, `co6.java:391,1439`, worker/service files.

## Five most important remaining questions

1. What Valdi component owns ordinary message rows, markdown/tool/widget rendering, composer callbacks, and scroll behavior?
2. What exact authenticated request/stream protocol and model-selection payload does `p0/q0` produce for a real turn?
3. What local database tables, migrations, encryption/key boundaries, and remote-history conflict rules back `s9g/hyh/q4d`?
4. What picker-to-upload contract converts URI entries and `emu.p0` references into server attachment IDs, including retry/cancel/MIME/size behavior?
5. Which account/device/feature flags select the realtime voice transport, transcription path, and screen-share coupling?

## Recommendation

Keep the official APK only as a reference artifact. If more evidence is desired, perform narrow authenticated emulator tests using the user’s own account and non-sensitive synthetic files/audio, with explicit consent at the emulator UI. Do not make the official APK or MoCHi the integration surface. Continue with the independent prototype and add a real provider only behind the already-defined `ProviderAdapter` after the provider contract and security model are reviewed.
