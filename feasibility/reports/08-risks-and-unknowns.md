# Risks, unknowns, and evidence matrix

## Evidence categories

- **Confirmed:** direct code/bytecode/resource, build/install, or captured emulator evidence.
- **Inferred:** supported interpretation that still lacks an end-to-end observation.
- **Unknown:** the artifact does not expose enough evidence to answer.
- **Not tested:** a test could be designed, but it was not run in this study.

## Evidence matrix

| Area | Finding | Classification | Supporting evidence | Gap |
|---|---|---|---|---|
| Package identity | `com.openai.chatgpt`, version `1.2026.230`, code `2623031` | Confirmed | Package manager, manifest, preserved split inventory | None for this artifact |
| Split provenance | Archive and four installed splits byte-match | Confirmed | Archive/split SHA-256 inventory and preserved copies | Does not establish publisher authenticity |
| Archive signer | All preserved APKs share the reported certificate | Confirmed | `apksigner` output | Independent trusted official-key comparison not found |
| Official-key attribution | Certificate is officially controlled by OpenAI/Google | Unknown | Subject/fingerprint only | Requires trusted official reference |
| Emulator setup | AVD boots and ADB recognizes `emulator-5554` | Confirmed | ADB/device and launch evidence | None for setup |
| Original shell | MainActivity starts and logged-out onboarding renders | Confirmed | Baseline/restored screenshots | No authenticated inference allowed |
| Resource rebuild | Apktool can rebuild modified base split | Confirmed | `base-modified-unsigned-v3.apk` | Bytecode-level modifications may be harder |
| Test signing/install | Separate-signed complete split set installs after uninstall | Confirmed | `adb install-multiple`, `apksigner` | Not update-compatible with original signer |
| Changed execution | Active onboarding title resource is rendered | Confirmed | `xek.java`/`xek.smali`, modified screenshot | Only one resource path was exercised |
| Chat renderer | DIL/Valdi message rendering boundary exists | Confirmed | `t8k`, `wqh`, Valdi module asset | Actual row/markdown/tool renderer unknown |
| Partial token repaint | Renderer consumes incremental state and repaints correctly | Inferred | `vef`/`emu` event path; prototype proves independent analogue | Official runtime response not observed; Not tested |
| Stream coordinator | `xgf.q0` launches stream coroutine and `xef` tracks `u790` | Confirmed | `xgf`, `xef`, `u790` | Exact live transport unknown |
| Typed stream events | `vef` handles `tlg` events and terminal paths exist | Confirmed | `vef`, `xgf.g0`, `xgf.j0` | Live ordering/reconnect Not tested |
| Stream service lifetime | Sticky service and 1,200,000 ms failsafe exist | Confirmed | `ConversationStreamingService` | Service execution Not tested |
| Composer message mutation | `hlx` creates/merges `emu`; `xgf.N0` integrates it | Confirmed | `hlx`, `emu`, `xgf.N0` | Exact Valdi callback unknown |
| Composer UX | Debounce, IME, send/stop/edit/regenerate semantics | Unknown | No complete readable implementation | Not tested |
| Conversation persistence | `s9g` repository, `hyh` cache, `q4d` queries/serialization | Confirmed | Named classes and direct calls | Actual storage schema/encryption unknown; runtime Not tested |
| Attachment placeholders | `q4d` parses pending placeholders/upload IDs | Confirmed | `q4d` fields/keys | End-to-end semantics Not tested |
| Attachment upload association | `s6f` URI registry and `qgf` success/failure replacement | Confirmed | `s6f`, `qgf`, `emu`, telemetry enums | Endpoint/body/auth/retry unknown; Not tested |
| Voice API | Valdi `VoiceService` and callback contract | Confirmed | `VoiceService`, input/listener/status classes | Feature flags/runtime Not tested |
| Voice transport | `cy0` branches; `mu0` owns audio; `co6` transcribes | Confirmed | Decompiled classes and strings | Signaling/codec/account gating unknown; Not tested |
| Voice background lifecycle | Foreground service and WorkManager worker | Confirmed | Manifest/classes and worker inputs | Runtime Not tested |
| Provider-neutral design | Explicit adapter/state/persistence architecture works | Confirmed | Prototype source, unit tests, emulator screenshots | Real provider adapter not implemented |
| MoCHi safety | No MoCHi artifact was modified | Confirmed | Separate workspace and scope | None within this study |

## Main risks

1. **Provenance risk:** matching hashes establish that the preserved/install artifacts are the same bytes; they do not independently authenticate the publisher or signer.
2. **Framework risk:** Valdi assets hide the renderer/composer implementation and make direct reuse brittle.
3. **Protocol risk:** the official request/stream/attachment/voice wire contracts are not recoverable with enough confidence for a safe provider substitution.
4. **Account/security risk:** authenticated behavior, session handling, feature flags, and credential boundaries were not tested.
5. **Lifecycle risk:** the official client has service-level stream recovery mechanisms; an independent client must design its own durable background/resume policy.
6. **Scope risk:** a resource patch proves an execution path, not that the product feature of interest is adaptable.
