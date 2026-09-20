# Phase 4 source references

All paths below are absolute so that a reviewer can open the exact artifact. The readable Java is primarily the JADX fallback output; Apktool smali is used to cross-check names, constants, and branches when JADX is incomplete.

## Artifact roots

- Original research: `C:\Users\Welcome\Downloads\ChatGPT-Android-Research`
- JADX fallback: `C:\Users\Welcome\Downloads\ChatGPT-Android-Research\03-jadx-output\chatgpt-1.2026.230-2623031-fallback\sources`
- Apktool decode: `C:\Users\Welcome\Downloads\ChatGPT-Android-Research\04-apktool-output\chatgpt-1.2026.230-2623031`
- Phase 4 isolated decode: `C:\Users\Welcome\Downloads\ChatGPT-Android-Feasibility\02-modified-apk\decoded-working-copy\apktool-decode`

## Bootstrap and cross-platform boundary

- `com\openai\chatgpt\MainActivity.java`: `n()` at line 35, `onCreate()` at line 150, `onNewIntent()` at line 167, and `onResume()` at line 182. The activity repeatedly routes lifecycle/intent handling through `n()`.
- `com\openai\chatgpt\app\MainApplication.java`: `onCreate()` at line 36 and lifecycle registration around line 66.
- `AndroidManifest.xml`: application `MainApplication` at line 132 and exported `MainActivity` at line 137.
- Compiled boundary evidence: `assets/chatgpt_conversation.valdimodule`, `assets/oai_file_picker.valdimodule`, and `assets/oai_voice_service.valdimodule`; Java DIL/Valdi APIs under `com\openai\valdi`.

## Chat rendering and message data

- `defpackage\t8k.java`: DIL/widget configuration and message-rendering setup; recovered code constructs `DILMessageMetadata` and action/rendering objects. The renderer implementation itself is in compiled Valdi assets, not readable Java.
- `defpackage\wqh.java`: DIL widget configuration schema, including `messageMetadata`, render metrics, render composition, action handling, and supported client actions.
- `defpackage\emu.java`: serialized message/content item model; role/status/content collections are represented in this model.
- `defpackage\bw1.java`: role vocabulary including User, Assistant, System, Developer, and Tool.
- `defpackage\sv1.java`, `defpackage\jw1.java`, and `defpackage\on20.java`: additional content/action representations referenced by the message mutation path.

## Streaming and turn coordination

- `defpackage\xgf.java`: class declaration at line 3; `p0()` at line 1405; request label `ConversationCoordinator/createRequest` at line 1562; `q0()` at line 1679; stream label `ConversationCoordinator/streamConversation` at line 1733; `H0()` at line 6173; `N0()` at line 6449; database-write label at line 6800; `g0()` at line 9876; `j0()` at line 12037; `P0()` at line 835.
- `defpackage\xef.java`: coroutine stream-tracking class at line 3; `invokeSuspend()` at line 75; `d890`/`u790` turn-state creation and tracking around lines 203–450; tracking label at line 450.
- `defpackage\vef.java`: event collector at line 3; event method `a(tlg, m4f)` at line 33; typed `tlg` handling and `emu` integration around lines 70–1075.
- `defpackage\u790.java`: per-turn state object at line 3; lists/maps/queues and `AtomicBoolean` fields at lines 28–69 and 201–227; accessors and equality/merge paths around lines 353–968.
- `com\openai\feature\conversations\impl\coordinator\ConversationStreamingService.java`: service declaration at line 3; `onDestroy()` at line 49; `onStartCommand()` at line 63; 1,200,000 ms failsafe at line 81.
- `defpackage\tlg.java`: typed stream-event hierarchy referenced by `vef`.
- `defpackage\rlg.java`: stream failure/terminal mapping referenced by coordinator branches.

## Composer and state mutation

- `defpackage\hlx.java`: message mutation/update object at line 3; static constructors/merge paths create or replace `emu` items.
- `defpackage\xgf.java`, `N0()` at line 6449: receives `hlx`, builds a new `f5f`, checks existing content IDs, and schedules the `ConversationCoordinator/writeMessagesToDb` path.
- `defpackage\xgf.java`, `P0()` at line 835 and `p0()` at line 1405: request/turn construction paths carrying model, mode, metadata, message lists, and attachment-related arguments.
- The exact Valdi composer callback, debounce/IME policy, send enablement, edit/stop/regenerate action mapping, and UI repaint callback were not recovered. The evidence supports the mutation boundary, not a complete composer implementation.

## Persistence

- `defpackage\f5f.java`: conversation aggregate model at line 4.
- `defpackage\s9g.java`: repository named `ConversationRepository` around line 32; title/delete/pin/update operations occur in the same class.
- `defpackage\hyh.java`: cache named `ConversationCache` around line 16.
- `defpackage\q4d.java`: complete-conversation query/serialization class; `system_hints` at lines 279 and 1259, `pending_attachment_placeholders` at lines 315 and 1307, and `pending_attachment_upload_ids` at lines 384 and 1300.
- `defpackage\xgf.java`, `N0()` at line 6449 and write label at line 6800: direct coordinator-to-repository write boundary.

## Attachments

- `defpackage\emu.java`: message/content model carrying attachment/content references.
- `defpackage\s6f.java`: upload registry; URI/entry methods around lines 28, 43, 91, 340, 460, and 504.
- `defpackage\qgf.java`: upload/result continuation; message field at line 38, success event at line 417, and failure event at line 582. The matching `emu` is replaced/routed back into the coordinator on success/failure.
- `defpackage\xgf.java`: upload status constants and `N0()` integration around lines 6817 and 8965.
- `com\openai\valdi\filepicker\FilePickerService.java` and `FilePickerSource.java`: Valdi picker boundary for Files, Photos, and Camera.

## Voice

- `com\openai\valdi\voice\VoiceService.java`: Valdi interface; `getCapabilities()` at line 6 and `start(VoiceSessionInput, VoiceSessionListener)` at line 11. The metadata schema names the same methods.
- `com\openai\valdi\voice\VoiceSessionInput.java`: messages plus `VoiceSessionMode` (`Dictation`, `Advanced`).
- `com\openai\valdi\voice\VoiceSessionListener.java`: assistant/user transcript callbacks, audio level, status, ended, and error callbacks are present in Kotlin metadata.
- `defpackage\cy0.java`: concrete service; `getCapabilities()` at line 21 and `start()` at line 42; branches for Advanced and Dictation begin around lines 49–68.
- `defpackage\mu0.java`: native realtime audio implementation; `AudioRecord`, `AudioTrack`, audio effects, cleanup, and `input_audio_buffer.clear` around line 167.
- `defpackage\co6.java`: HTTP request builder; `/backend-api/transcribe` at lines 391 and 1439.
- `com\openai\voice\webrtc\VoiceModeForegroundService.java`: foreground voice/screen-share lifecycle and `com.openai.voice.action.END` around line 674.
- `com\openai\voice\recording\VoiceAudioRecordingUploadWorker.java`: WorkManager upload worker; `doWork()` at line 191 and required `conversation_id`, `voice_session_id`, and `legacy_account_user_id` inputs at lines 233–241.

## Independent prototype

- `05-independent-prototype/src/main/java/com/example/androidfeasibility/ProviderAdapter.java`: provider boundary.
- `MockProvider.java`: deterministic normal/slow/empty/failure/duplicate-terminal scenarios.
- `ConversationCoordinator.java`: stable IDs, state transitions, cancellation, late-event guards, terminal-event idempotence, and persistence callbacks.
- `JsonConversationRepository.java`: local JSON persistence with temp-file replacement.
- `MainActivity.java`: standard Android Views, stable message-ID-to-TextView mapping, composer, send/stop controls, and restoration policy.
- `PrototypeCoreTest.java`: deterministic unit tests for streaming, cancellation, failure, persistence, and mock-provider completion.
