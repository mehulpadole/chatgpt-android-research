# Phase 3 follow-up — feature traces and evidence matrix

Date: 2026-09-08  
Package: com.openai.chatgpt  
Artifact: ChatGPT 1.2026.230 / version code 2623031  
Runtime: ChatGPT-Research-API35, emulator-5554

## Scope and evidence rule

This is a continuation of the completed setup, preservation, and decompilation work. It does not repeat those steps. The readable JADX fallback, Apktool output, preserved APK bundle, and earlier runtime captures remain the source material.

The Phase 3 runtime session crossed only startup, onboarding, and the logged-out shell. The welcome screen and its Skip path prove that the shell renders; they are not evidence that authenticated chat, history, uploads, voice, or other account-backed features work. Those features are assessed below only where decompiled code, bytecode/resource corroboration, or manifest evidence supports them.

Status terms used here:

- Confirmed — directly visible in runtime, directly present in code/bytecode/resources, or byte-for-byte/hash verified.
- Inferred — a bounded interpretation supported by several references, but not explicit enough to treat as a recovered product-level name or contract.
- Unknown — the available artifact does not reveal the answer reliably.
- Not tested — a runtime action was not performed, so code presence must not be read as execution evidence.

## Phase 3 audit: what is and is not supported

| Phase 3 finding | Evidence actually available | Classification |
|---|---|---|
| App starts and remains alive on the emulator | MainActivity focused; process alive; no filtered AndroidRuntime fatal exception; runtime screenshots/XML | Confirmed |
| Onboarding reaches an unauthenticated welcome screen | Welcome to ChatGPT, Continue with Google, Log in or sign up, Skip in 07-observations | Confirmed |
| Logged-out composer shell renders | ChatGPT, Ask ChatGPT, Attachment, Dictation, Send Message, and Menu in the post-Skip UI dump | Confirmed, shell only |
| Authenticated message submission succeeds | No account login or prompt submission; coordinator/request code exists statically | Not tested; static boundary Confirmed |
| Streaming responses render incrementally | tlg event hierarchy, vef, u790, and ConversationStreamingService exist statically | Not tested at runtime; static stream machinery Confirmed |
| Conversation history synchronizes | ConversationCoordinator, ConversationRepository, ConversationCache, and complete-conversation queries exist | Static plumbing Confirmed; account sync Not tested/Unknown |
| Attachments can be selected and uploaded | File-picker API, ChatFileProvider, upload registry, message attachment refs, and upload status transitions exist | Static path Confirmed; selection/upload Not tested |
| Voice can record, stream, transcribe, or upload | Voice APIs, audio implementation, foreground service, transcribe request branch, and WorkManager uploader exist | Static architecture Confirmed; all runtime behavior Not tested |
| The archive is the same artifact installed and pulled from the emulator | Archive SHA-256, per-split hashes, and four installed/preserved split byte comparisons | Confirmed |
| The certificate is independently verified as OpenAI's official/current signer | Local apksigner verification and same-cert result for all 37 APKs; no trusted official fingerprint match found | Signature validity Confirmed; official-key attribution Unknown |

## Cross-cutting flow

The following is the most defensible high-level flow recoverable from the Java layer. The Valdi module internals are compiled binary assets, so the UI-to-coordinator callbacks cannot be named more precisely without runtime instrumentation or additional symbols.

~~~mermaid
flowchart LR
    A[MainActivity / Valdi root] --> B[Composer or conversation UI]
    B --> C[ConversationCoordinator xgf]
    C --> D[Turn state u790 keyed by d890]
    C --> E[Stream source / event collector vef]
    E --> F[tlg events: rlg, umg, terminal/error]
    F --> C
    C --> G[Message/content model emu]
    G --> H[ConversationRepository s9g]
    H --> I[ConversationCache hyh / CompleteConversationQueries q4d]
    B --> J[FilePickerService / VoiceService]
    J --> K[Upload or audio transport boundary]
~~~

## Feature trace 1 — chat rendering

### Entry point and components

1. com\openai\chatgpt\MainActivity.java, onCreate, onNewIntent, onResume, and n() establish the Android activity lifecycle and launch the opaque root through fgs, t2k, and qqc0.N. This is a bootstrap boundary, not proof that a particular message composable is selected.
2. The decoded manifest and runtime hierarchy show a Compose/Valdi shell. The relevant conversation UI is primarily represented by the compiled asset 04-apktool-output\chatgpt-1.2026.230-2623031\assets\chatgpt_conversation.valdimodule; Java contains the bridge contracts and data models rather than a readable message-list renderer.
3. defpackage\emu.java is the serialized message/content item used by coordinator methods. Its fields include role/status/content collections and u1; defpackage\bw1.java enumerates User, Assistant, System, Developer, Tool, and other roles.
4. defpackage\on20.java enumerates content kinds including assistant/user/system content, image generation, browse/plugin/connector/tool content, reasoning, automation, file search, and computer-related kinds. This is stronger than a generic chat-bubble assumption: the artifact has a typed content-kind vocabulary.
5. defpackage\sv1.java carries content data and jw1 metadata. com\openai\valdi\dil\DILMessageMetadata.java explicitly exposes conversationId, messageId, content-reference indices, turn ordinal, widget type/name, and read-only/model-written flags. This identifies a message-to-rendered-widget metadata boundary.
6. defpackage\c89.java and its Valdi schemas define the reasoning component (ChatGPTReasoningComponent@chatgpt_reasoning/src/ChatGPTReasoningComponent) with active item, item list, status, summary title, tool icons, and text fields. The reasoning UI is therefore a separate typed Valdi surface, not merely plain markdown.

### State and data flow

- Stream events are collected by defpackage\vef.java, then routed into defpackage\xgf.java (h0, f0, g0, and j0). f0(tlg, emu, boolean, ...) calls B(...) for eligible message items and then kdf.S(...), which is the clearest recovered event-to-message integration point.
- xgf.g0(String, ...) handles a ConversationMessagesCompleteEvent, updates analytics, persists/merges the f5f conversation, and emits x79(W(...)) on d0.
- xgf.j0(String, ...) handles a ConversationStreamDone boundary, records success, persists/merges, and emits y79(W(...)).
- The exact mapping from each on20 kind or sv1 field to a concrete Valdi view is not present in readable Java. The compiled chatgpt_conversation.valdimodule is evidence of the component boundary, not readable renderer source.

### Network and persistence boundaries

The renderer receives coordinator/message-model state. Persistence is downstream through xgf to s9g/hyh/q4d; network transport is upstream of vef and is described in the streaming trace below. No runtime response was obtained, so no rendered assistant token, markdown block, tool card, reasoning card, or widget was observed.

### Unknowns

- Exact Valdi component names for ordinary user/assistant message rows.
- Whether a given on20 value is rendered natively, delegated to Valdi, or handled by a separate module.
- Markdown parsing, virtualization, scroll anchoring, and partial-token repaint behavior.
- Server payload fields and the precise sv1/jw1 field meanings beyond the uses shown by vef.

## Feature trace 2 — streaming response handling

### Entry point and components

- defpackage\xgf.java, static q0(...), launches xef under the named coroutine scope ConversationCoordinator/streamConversation.
- defpackage\xef.java, invokeSuspend, creates or reuses a per-conversation u790 in x790.l, keys the turn with d890(F0), sets the active conversation/turn fields, emits z79(F0), and launches ConversationCoordinator/trackStreaming.
- The same coroutine conditionally starts ConversationStreamingService via Application.startService(new Intent(app, ConversationStreamingService.class)). It catches IllegalStateException, so service start failure is an explicit branch.
- com\openai\feature\conversations\impl\coordinator\ConversationStreamingService.java is a non-bindable, START_STICKY service. onStartCommand schedules a 1,200,000 ms failsafe; onDestroy removes it; defpackage\lp.java calls stopSelf() on timeout.
- defpackage\tlg.java is the polymorphic stream-event interface. defpackage\vef.java is the collector/translator from events into u790 and coordinator state.

### Event/state transitions recovered from vef

| Event or branch | Directly recovered effect |
|---|---|
| rlg | Stores request ID in the turn map, saves metrics/counters/status, and updates turn state. |
| umg | Sets current status/content references and metadata; assistant-role content updates timing/JSON metadata, content references, source version, and progress state. |
| cmg | Stores a string field on u790. |
| k2g | Atomically marks terminal handling, updates status, dispatches completion through lp70.e and ffn.a. |
| jlg | Marks terminal completion and dispatches completion. |
| ylg / gmg | Marks terminal failure; gmg creates a ConversationStreamError with the decoded message. |
| log | Records success/resume/error analytics; terminal status can carry a throwable and optional HTTP code. |
| qng | Stores response metadata and extracts server_ttfvt_ms into turn metrics. |
| qag | Sets a turn flag. |
| xmg | Marks user_visible_token/last_token timing and trace sections. |
| resume errors | Explicit strings include tokenless_resume_unavailable and conduit_miss; a resume/refresh branch remains in the coordinator. |

u790 is a substantial in-memory turn state object: it includes status strings, role/model fields, counters, metrics, content references, queues, maps, atomic terminal state, and analytics collections. This is direct evidence of an incremental turn state machine, not proof of a particular wire protocol.

### Network boundary

xgf.p0(...) constructs yef under the named ConversationCoordinator/createRequest path before calling the stream path. xef receives a stream source through its L0 callback and wraps it with s8/vj/vef. The exact HTTP/SSE/WebSocket implementation and request endpoint are not recovered in readable Java. The separate URL constants collected in Phase 2 are configuration evidence only; they do not prove which endpoint this turn used.

### Completion and failure boundary

On normal stream completion, xgf.g0/j0 persist/merge the conversation and emit coordinator flow events. defpackage\pr0.java contains the stop-service finalizer around ConversationCoordinator/markMessagesFailedAfterStream. This supports a cleanup/failure reconciliation boundary, but the complete failure policy is not fully reconstructable from the obfuscated coroutine.

### Unknowns and runtime status

No authenticated request, response, token delta, resume, retry, or failure was observed. Consequently, incremental UI rendering, latency, backpressure, reconnection, and server compatibility are Not tested. The event classes, state storage, coordinator scopes, and failsafe lifecycle are Confirmed statically.

## Feature trace 3 — composer state

### Entry point and observable shell

The runtime post-Skip screen exposes Ask ChatGPT, Attachment, Dictation, and Send Message. This confirms affordances in the logged-out shell only. It does not establish that the send action is enabled for anonymous users or that the buttons reach their account-backed implementations.

MainActivity.o(Intent) is a separate input entry point. It accepts eligible PROCESS_TEXT, SEND, SEND_MULTIPLE, VIEW, and EDIT intents for content/file schemes, then passes them to ShareSheetContentProviderBindings via fm40.d(intent). This is a concrete way text or external content can enter the composer flow, independent of a user typing into the visible field.

### Message mutation path

- defpackage\hlx.java is a message mutation/update object containing an emu, lists, an ID, mode, and flags. Its static constructors create or merge emu items; hlx.f(String) creates an on20.c message item.
- defpackage\xgf.java, N0(...), receives an hlx update and current f5f conversation, builds a new f5f using f5f.b(...), and checks existing content IDs before writing.
- The coordinator then routes the updated state to P0(...)/request handling. A jdo value is carried through the path; defpackage\jdo.java contains snorlax and gpt values, but its product-level meaning is not proven.
- Coordinator state is guarded by xgf.H0(...) and blg: saved-state changes are checked against the expected state and streaming/managed-state conditions before xgf.Z is updated. This indicates guarded state transitions rather than an unconditionally mutable text field.

### Boundaries and unknowns

The exact Valdi composer state object, debounce/IME behavior, send-button enablement, edit/stop/regenerate semantics, and mapping from a text change callback into hlx are not recovered. The available code proves message mutation and coordinator integration, while the runtime proves only the logged-out visual affordances. These are Confirmed statically, Confirmed as shell UI, and Not tested for interaction.

## Feature trace 4 — conversation persistence

### Entry point and storage flow

~~~mermaid
sequenceDiagram
    participant C as xgf ConversationCoordinator
    participant W as off write coroutine
    participant R as s9g ConversationRepository
    participant H as hyh ConversationCache
    participant Q as q4d CompleteConversationQueries
    C->>C: build updated f5f from hlx/emu
    C->>W: ConversationCoordinator/writeMessagesToDb
    W->>R: s9g.y(updated conversation / mutation)
    R->>H: cache lookup/update/invalidate
    R->>Q: complete-conversation query/serialization
    Q-->>R: serialized conversation record
    R-->>C: repository result
~~~

- xgf.N0(...) constructs an updated f5f, iterates message/content IDs, and launches off under ConversationCoordinator/writeMessagesToDb.
- The off variant used there obtains s9g and xnf, creates storage/update wrappers, calls s9g.y(...), and normalizes success/failure. This is the direct repository write boundary recovered from the call chain.
- defpackage\s9g.java is named by its constructor as ConversationRepository and contains hyh plus storage/network backend fields. Its methods load/update/delete through those dependencies.
- defpackage\hyh.java is named ConversationCache; it maintains a state-backed map keyed by conversation ID and removes/updates entries through q4d-backed operations.
- defpackage\q4d.java is named CompleteConversationQueries. Its JSON deserialization explicitly handles system_hints and pending_attachment_placeholders in addition to emu data. This is direct evidence that attachment placeholders can be part of a persisted conversation record.
- xgf.g0 and xgf.j0 call the persistence/merge path after message-complete and stream-done events, so persistence is coupled to turn lifecycle boundaries rather than only to an explicit history-screen action.

### Unknowns and runtime status

The query/database table names, schema migrations, encryption/key handling, remote history endpoint, pagination and conflict resolution are not readable enough to assert. No account was used and no conversation was created or reloaded. Local persistence plumbing is Confirmed statically; actual writes, history sync, and cross-device behavior are Not tested.

## Feature trace 5 — attachments

### Selection and Android file boundary

- com\openai\valdi\filepicker\FilePickerService.java is a Valdi-marshalled interface with getCapabilities() and pickFiles(FilePickerOptions, FilePickerCompletion).
- FilePickerOptions has explicit acceptedTypes, maximumFileBytes, maximumFiles, maximumTotalBytes, and source fields. FilePickerSource has Files, Photos, and Camera values. FilePickerCapabilities reports camera/files/photos support.
- FilePickerCompletion.onFiles(List<rcz>) returns rcz objects carrying dataUrl, lastModified, mimeType, name, and size; onFailure carries a message. This identifies the Java-to-Valdi selection payload without guessing MIME or size policy values.
- com\openai\files\ChatFileProvider.java extends AndroidX FileProvider. The manifest gives it authority com.openai.chatgpt.files, exported=false, and URI grants. The manifest also declares camera permission. These are Android-side file/camera boundaries, not proof that a picker was opened.

### Message/upload path

- emu.p0 is used by xgf.N0(...) as a set of attachment/content references associated with a message. When non-empty, the coordinator dispatches through s6f.c(...) and emits CHATGPT_FILE_UPLOAD_STEP_STATUS_STARTED with the message ID and count.
- defpackage\s6f.java is an upload registry keyed around Uri and wu90 entries. It creates and removes entries, tracks statuses, and exposes cleanup/lookup operations. The low-level upload protocol is not legible enough to assign exact server fields.
- defpackage\qgf.java awaits an upload/result operation. On h790 success it emits CHATGPT_FILE_UPLOAD_STEP_STATUS_SUCCEEDED, replaces the matching emu in the working list by message ID, and routes the resulting state into xgf.P0(...). On b790 failure it emits ...FAILED and routes the failure result.
- q4d's pending_attachment_placeholders parsing and hlx/f5f persistence path show that attachment state can exist before/around final upload completion.

### Network/persistence boundary and unknowns

The exact upload endpoint, multipart/body format, authentication headers, server attachment IDs, retry policy, and relationship between dataUrl, URI registry entries, and emu.p0 are unknown. Selection, camera capture, upload, progress UI, and server acceptance were Not tested. The interfaces, message association, registry, success/failure telemetry, and persistence placeholder field are Confirmed statically.

## Feature trace 6 — voice architecture

### Cross-platform API boundary

com\openai\valdi\voice\VoiceService.java is a Valdi-marshalled service with getCapabilities() and start(VoiceSessionInput, VoiceSessionListener). The input exposes messages and a VoiceSessionMode; recovered modes include Advanced and Dictation. VoiceSessionListener exposes assistant transcript deltas/completion, user transcript/deltas, audio level, status changes, ended, and error callbacks. VoiceSessionStatus is Connecting, Listening, Thinking, and Speaking.

The compiled asset oai_voice_service.valdimodule, VoiceServiceNativeModuleFactoryImpl, ws0.createVoiceService(...), and q0w establish the native-module registration path. The API is a real Java/Valdi contract; it is not evidence that a voice session was started in this runtime.

### Transport selection and audio implementation

- defpackage\cy0.java implements VoiceService, stores VoiceServiceConfiguration, and selects a transport mode via o9b.G(config).
- getCapabilities() in this concrete implementation returns VoiceCapabilities(false, false). That is an implementation result for this artifact/configuration path, not a universal statement about all account/device feature flags.
- For Advanced plus transport code 2, cy0.start(...) returns an error session stating that Advanced voice requires an Android WebRTC transport. Other branches construct sq0, mu0, or zp0 depending on mode/transport.
- defpackage\mu0.java (ValdiRealtimeVoice) owns AudioRecord, AudioTrack, AcousticEchoCanceler, NoiseSuppressor, futures, a lock, and a kk10 transport/session. Cleanup releases audio resources, restores audio-manager state, cancels work, and ends with code 1000 / Voice session ended. setMuted(true) sends {"type":"input_audio_buffer.clear"} through that transport. This is direct low-level audio/protocol evidence.
- defpackage\sq0.java posts window.ValdiVoiceSession.setMuted(true/false) and cleanup to a Handler, proving an alternate JS/bridge session path.
- defpackage\zp0.java is a dictation path built around MediaRecorder and a temporary file. finish() rejects an absent completed recording, then submits processing to an executor; cleanup deletes the temporary recording.

### Network, background service, and recording persistence

- defpackage\co6.java builds POST requests from VoiceServiceConfiguration.baseUrl, appends an endpoint, applies request headers/content type, rejects responses over 1 MiB, and explicitly names /backend-api/transcribe in a transcription branch. This proves a voice HTTP boundary, but does not establish the realtime signaling endpoint.
- com\openai\voice\webrtc\VoiceModeForegroundService.java is declared with mediaProjection|microphone, handles voice/screen-share notification lifecycle, checks microphone/background constraints, and broadcasts com.openai.voice.action.END when the task is removed.
- com\openai\voice\recording\VoiceAudioRecordingUploadWorker.java is a WorkManager CoroutineWorker requiring conversation_id, voice_session_id, and legacy_account_user_id; it validates the current account, uploads through b8b0, and returns skipped, complete, or failed, with bounded retries for incomplete sessions.

### Unknowns and runtime status

The realtime transport's signaling/endpoint, codec/sample configuration, account gating, exact transcript request/response schema, and screen-share/voice session coupling remain unknown. No microphone permission, voice session, dictation, screen share, or recording upload was tested. Static API, transport branches, audio resource handling, transcription branch, foreground service, and worker are Confirmed; product behavior is Not tested.

## Authenticated emulator testing: what would add evidence

Static analysis is complete enough to continue independently. If runtime evidence is desired, the emulator can test the following without a physical phone:

- OpenAI login/OAuth and redirect handling.
- A harmless test prompt, visible token streaming, stop/failure behavior, and conversation creation.
- Reloading the conversation and history synchronization.
- A user-selected test image/document through the emulator picker, upload progress, and message attachment state.
- Microphone permission and voice/dictation callbacks; screen-share testing would additionally require enabling the requested accessibility/media-projection surfaces.

The user would need to enter their own credentials in the emulator's browser/custom-tab flow and, if prompted, complete any MFA or consent step. For attachment/voice tests, the user would need to choose non-sensitive test data and grant the relevant runtime permissions. No credentials, private file, audio, screen contents, or prompt were entered in the current run. A sign-in is optional; it is not required for the static conclusions in this report.

## APK provenance and certificate limits

The preserved fallback is the APKMirror universal archive:

01-original-apks\com.openai.chatgpt_1.2026.230-2623031_4arch_7dpi_25lang_057f11437587689657ad6e6327b4a659_apkmirror.com.apkm

Archive SHA-256: 097D7B60C51F14F35923C8E41D033E526AE9E6886BA7BBD276417EACB55B2252  
Archive MD5: A9B5B104E49FABAD28B4CAE52A8C21DA  
Archive SHA-1: 3FDA035CAE6A821707B569ACA484A28A13A65B67

The archive page publishes the same SHA-256, and its metadata identifies package com.openai.chatgpt, version 1.2026.230, and version code 2623031. Its 37 APKs contain base plus configuration splits. The four installed/pulled splits (base, config.en, config.x86_64, config.xxhdpi) match the corresponding archive entries byte-for-byte and have the recorded local SHA-256 values in 02-package-metadata/final-package-summary.txt.

apksigner verify --verbose --print-certs succeeds for all 37 preserved APKs and reports the same signing certificate:

- DN: CN=Android, OU=Android, O=Google Inc., L=Mountain View, ST=California, C=US
- Certificate SHA-1: 51a2f260766c9c1a83b7dd5b4572040ac23e4aea
- Certificate SHA-256: b24f4bfbb3cf293f938703b9d87027c1102cc36dc4fa206910e08927db40473c

What this establishes:

1. The local archive is intact relative to the APKMirror page's published digest.
2. The preserved archive contents and the installed/pulled emulator splits are the same bytes for the four installed splits.
3. Android signature verification succeeds, and all preserved splits are consistently signed by one certificate.

What it does not establish:

- It does not independently prove that the certificate is OpenAI's official/current Google Play signing key. The research did not locate a trusted official OpenAI or Google certificate fingerprint to compare against this digest.
- The APKMirror certificate listing is a corroborating archive record, not an independent trusted anchor. The official Google Play listing (https://play.google.com/store/apps/details/ChatGPT?hl=en_US&id=com.openai.chatgpt) and OpenAI installation guidance (https://help.openai.com/en/articles/8167604) support the package/publisher identity to users, but do not publish the signer digest used here.
- Android's APK signature verification documentation (https://developer.android.com/tools/apksigner) describes signature validity and certificate printing; Android's code-transparency guidance (https://developer.android.com/guide/app-bundle/code-transparency?hl=en) likewise requires comparing a fingerprint with a developer key communicated through a trusted channel for an authenticity claim.

The correct provenance label is therefore: reputable archive fallback, package/version/hash-consistent, Android-signature-valid, but not independently anchored to an official OpenAI/Play signing-key fingerprint.

## Evidence matrix

| Area / claim | Supporting evidence | Status |
|---|---|---|
| Android entry/root bootstrap | MainActivity.onCreate/onNewIntent/onResume/n; runtime MainActivity | Confirmed |
| Logged-out composer affordances | Post-Skip screenshot/XML | Confirmed |
| Composer's account-backed send state | hlx, xgf.N0, P0; no interaction run | Inferred statically; Not tested runtime |
| Typed message/content vocabulary | emu, bw1, on20, sv1, jw1 | Confirmed statically |
| Valdi conversation renderer boundary | chatgpt_conversation.valdimodule, DIL metadata, reasoning schemas | Confirmed boundary; exact renderer Unknown |
| Incremental streaming state machine | xgf.q0, xef, vef, u790, tlg subclasses | Confirmed statically; Not tested runtime |
| Stream service lifetime/failsafe | ConversationStreamingService, lp | Confirmed statically; service execution not tested |
| Stream wire protocol/endpoint | yef/stream callback path; obfuscated transport; URL constants | Unknown |
| Conversation write boundary | xgf.N0 -> off -> s9g.y | Confirmed statically; Not tested runtime |
| Cache/query/persisted attachment placeholders | hyh, s9g, q4d, system_hints, pending_attachment_placeholders | Confirmed statically; schema details Unknown |
| File selection contract | FilePickerService, FilePickerOptions, FilePickerCompletion, rcz | Confirmed statically; Not tested runtime |
| Upload/message association | emu.p0, s6f, qgf, started/succeeded/failed analytics | Confirmed statically; endpoint/result schema Unknown; Not tested runtime |
| Voice API and transcript callbacks | VoiceService, VoiceSessionInput, VoiceSessionListener, status enum | Confirmed statically; Not tested runtime |
| Realtime audio resources and mute event | mu0 AudioRecord/AudioTrack/effects and input_audio_buffer.clear | Confirmed statically; realtime session Not tested |
| Dictation recording path | zp0, MediaRecorder, temp file | Confirmed statically; Not tested runtime |
| Voice HTTP transcription branch | co6, /backend-api/transcribe | Confirmed statically; response behavior Not tested |
| Voice background/lifecycle support | VoiceModeForegroundService, manifest foreground types, upload worker | Confirmed statically; Not tested runtime |
| Runtime authenticated features | No credentials, prompt, upload, voice, or history test | Not tested |
| Archive integrity and installed split identity | Archive hash, per-split hashes, byte comparison | Confirmed |
| Certificate validity and consistency | apksigner on all 37 APKs | Confirmed |
| Certificate authenticity as official OpenAI/Play key | No trusted official fingerprint comparison | Unknown |

## Five concrete architectural findings

1. The app is a Java Android shell around compiled Valdi feature modules. MainActivity.n() bootstraps the root, while chatgpt_conversation.valdimodule, oai_file_picker.valdimodule, oai_voice_service.valdimodule, and DILMessageMetadata define cross-platform feature/data boundaries. The ordinary message renderer is not recoverable as Java source, which explains why runtime/UI evidence and Java model evidence do not line up one-to-one.

2. A conversation turn has a durable coordinator plus a separate per-turn state object. xgf.q0 launches xef; xef keys u790 by d890 and starts tracking; vef applies typed events and terminal branches; ConversationStreamingService supplies a sticky lifecycle/failsafe. The architecture explicitly accounts for resumability and failure reconciliation (tokenless_resume_unavailable, conduit_miss, and markMessagesFailedAfterStream).

3. Persistence is staged and guarded, not a direct UI write. xgf.N0 creates an updated f5f, schedules ConversationCoordinator/writeMessagesToDb through off, and passes it to s9g.y; hyh and q4d provide cache/query/serialization boundaries. xgf.H0/blg guard saved-state transitions, including streaming-active conditions.

4. Attachments are first-class message state with asynchronous replacement. emu.p0 carries attachment/content references; s6f maintains URI-keyed upload entries; qgf emits started/succeeded/failed stages and replaces the matching message item on success; q4d persists pending_attachment_placeholders. This is more specific than merely observing an attachment button in the shell.

5. Voice is a transport-pluggable cross-platform service, not one monolithic recorder. VoiceService chooses among advanced/WebRTC, realtime audio (mu0), alternate bridge (sq0), and dictation (zp0) paths; co6 provides an HTTP/transcription boundary; the foreground service and WorkManager uploader extend the lifecycle beyond the activity. The exact transport/signaling and feature gating remain unresolved.

## Five most important remaining questions

1. What Valdi component/state callbacks implement ordinary message rows and the composer, and how do partial emu/sv1 updates map to repaint and scroll behavior?
2. What exact authenticated request format and transport does xgf.p0/yef use for conversation turns, including endpoint, headers, resume token, and backpressure semantics?
3. What are the actual local database tables, migrations, encryption/key boundaries, and remote history conflict rules behind s9g/hyh/q4d?
4. What upload endpoint/body format turns picker rcz data and emu.p0 references into server attachment IDs, and how are retry/cancel/size/MIME failures surfaced?
5. Which trusted official OpenAI/Google reference publishes the current Play signing-certificate fingerprint, and how does it compare with b24f4bfbb3cf293f938703b9d87027c1102cc36dc4fa206910e08927db40473c?

## Decoder and test limitations

The fallback JADX output completed the class set but preserves register-oriented output in heavily obfuscated Kotlin/Compose methods. Valdi modules are compiled binary assets. Therefore, method names/log labels and API schemas are treated as strong evidence only where direct calls, fields, strings, or annotations support them; product-level names are not invented. The runtime evidence remains deliberately logged-out and emulator-only, with no physical-phone dependency and no MoCHi changes.

