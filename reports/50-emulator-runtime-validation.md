# Emulator Runtime Validation — Phases 5–10

Date: 2026-09-25

Final classification: **RUNTIME INCOMPLETE**

This recovery run created a reproducible Android target and exercised the existing
prototype. No production MoCHi systems were contacted, no PR was merged, and no
new product feature was added. One in-scope layout regression was fixed because
it made the requested emulator smoke controls unreachable.

## Environment and artifact

| Item | Result |
| ---- | ------ |
| Worktree | `/home/mehul/.codex/worktrees/phase-6-real-provider/chatgpt-android-research` |
| Branch | `phase-6-real-provider` |
| Starting commit | `af028cce300d49efb66936cf2375c6f5164fc8bd` |
| AVD | `MoCHi-Research-API35` |
| System image | `system-images;android-35;google_apis;x86_64`, revision 9 |
| Device | Pixel 2 profile, Android 15 / API 35, x86_64, Google APIs |
| Serial | `emulator-5554` |
| Memory/storage | 2 GB configured RAM, 2 GB data partition, 512 MB SD card |
| Display | 1080×1920, density 420 |
| Graphics/acceleration | `swiftshader_indirect`; KVM available and usable |
| APK | `feasibility/prototype/build-output/local-stream-lab.apk` |
| Original APK SHA-256 | `844ffaba16520b584d68c68a48dac95f04b754bb3fb83a23a3fb8b576f148991` |
| Validated APK SHA-256 | `0bd7c53bec57fc5f98182b9547d2f253b32ff63453d506b03999fb92e7fce20b` |
| Package/version | `com.example.androidfeasibility`, version code `1`, version name `0.11-debug` |
| Target SDK | 35 |
| Signing | v3 verified; certificate SHA-256 `7fc653152ee465795c5cb16f844c1c9601e554a533afb6b3297d439901136806` |

Boot evidence: `adb devices -l` reported `emulator-5554 device`; the emulator log
reported boot completion, `bootanim` was stopped, and package/device queries were
responsive. The APK installed successfully and its launcher activity remained
resumed without a fatal exception.

## Backend configuration

| Backend | Configuration | Result |
| ------ | ------------- | ------ |
| Phase 5 NDJSON | `test-backend/test_backend.py`, `0.0.0.0:8765`; emulator URL `http://10.0.2.2:8765/` | PASS |
| Staging sync | `test-backend/sync_backend.py`, `0.0.0.0:8787`; emulator URL `http://10.0.2.2:8787/` | PASS |
| Production services | Not used | PASS |

## Runtime matrix

| Area | Test | Result | Evidence and disposition |
| ---- | ---- | ------ | ------------------------ |
| Environment | Dedicated AVD creation, boot, install, and launch | PASS | API 35 x86_64 AVD; install returned `Success`; launcher activity resumed |
| Layout | Composer, send, and stop controls at default density | PASS | `run-android-layout-smoke.sh`; all control centers were inside visible frame bottom 1857 |
| Phase 5 mock | Normal completion with rendered user and assistant records | PASS | `phase5-normal`; deterministic assistant text and `COMPLETED` status observed |
| Phase 5 mock | Slow incremental cancellation | PASS | `phase5-slow-cancel-2`; `CANCELLED` status and partial assistant content observed |
| Phase 5 mock | Failure before content | PASS | `phase5-fail-before-2`; `FAILED · mock failure before first content` observed |
| Phase 5 mock | Failure after partial content | PASS | `phase5-fail-after-2`; failure plus partial assistant content observed |
| Phase 5 mock | Duplicate terminal event | PASS | One stable rendered assistant record remained `COMPLETED` |
| Phase 5 mock | Late delta and late failure after terminal | PASS | Terminal state remained `COMPLETED`; late events did not alter the record |
| Phase 5 mock | Message-ID stability visible through UI | NOT TESTED | IDs are not exposed by the prototype UI; coordinator contract coverage is automated only |
| Phase 5 mock | Restart/restoration/no duplicate record | PASS | `run-android-persistence-smoke.sh`; completed turn restored after force-stop/start and outer-body scroll |
| Phase 5 HTTP | Normal NDJSON stream with multiple deltas | PASS | `phase5-http-normal`; assistant rendered `first second` and `COMPLETED` |
| Phase 5 HTTP | Cancellation | PASS | `phase5-http-slow-cancel`; `CANCELLED` observed after stop |
| Phase 5 HTTP | Failure before content | PASS | `phase5-http-fail-before`; synthetic pre-content failure rendered |
| Phase 5 HTTP | Failure after partial content | PASS | `phase5-http-fail-after`; partial content and synthetic failure rendered |
| Phase 5 HTTP | Restart/restoration | PASS | HTTP turns remained present after force-stop/start; no duplicate target turn |
| Phase 5 HTTP | Abrupt disconnect | NOT TESTED | Backend supports it, but the Android scenario selector does not expose `DISCONNECT` |
| Phase 5 HTTP | Malformed frame | NOT TESTED | Backend supports it, but the Android scenario selector does not expose `MALFORMED` |
| Phase 6 UI | Provider selection and missing-credential connection behavior | PASS | OpenRouter selection worked; test returned `NOT_CONFIGURED · provider credential is missing` without a key |
| Phase 6 live BYOK | Credential save, real prompt, cancellation, replacement/removal | NOT TESTED | No real credential was entered, stored, transmitted, or requested from the user |
| Phase 6 live BYOK | Temporary unavailable endpoint | PASS | Injected unused local port produced a rendered network `FAILED` state |
| Phase 6 logs | Provider credential-pattern logcat audit | PASS | No `Authorization`, `Bearer`, `api_key`, `apikey`, or `sk-` pattern matched; contents were not printed |
| Phase 7 | Document picker opens and synthetic file selection/import | PASS | Picker opened; synthetic TXT appeared; status reported attachment ready |
| Phase 7 | Local attachment metadata/content-part persistence through a mock turn | PASS | Synthetic attachment name and a file content part were present in the app-managed conversation record |
| Phase 7 | Remove, unsupported/zero-byte/oversize fixtures, provider-failure retention | NOT TESTED | No integrated remove control or full fixture matrix was exposed in this UI run |
| Phase 7 | Supported multimodal send through provider path | BLOCKED | `MainActivity` passes a plain provider request through `ProviderRouter`; prepared attachments are not wired into the OpenRouter route |
| Phase 7 | Attachment path confinement | BLOCKED | `FileAttachmentRepository` trusts persisted attachment IDs/local references during load/delete; this remains a real static boundary gap |
| Phase 8 | Implemented TTS playback and completion | PASS | READ produced `Voice · SPEAKING`, then `Voice · ENDED`; emulator audio log showed normal track standby |
| Phase 8 | Repeated playback/explicit stop | NOT TESTED | Only one playback/completion cycle was exercised |
| Phase 8 | Dictation permission, recording, transcript, cancellation, cleanup | NOT TESTED | No dictation control is reachable in the implemented Android UI |
| Phase 8 | Realtime voice | NOT TESTED | No integrated realtime voice session is exposed |
| Phase 9 | Local-only sync behavior | PASS | UI reported `Sync · OFFLINE` and retained local content |
| Phase 9 | Staging sync connectivity | PASS | Injected staging backend returned `Sync · SYNCED` |
| Phase 9 | Conversation outbox, incoming application, tombstones, retry/quota/auth, second device | BLOCKED | The UI does not enqueue conversation changes or apply pulled entities; unit/simulation coverage is not runtime-device proof |
| Phase 10 | Cold launch, navigation, provider settings, chat, stop, error, attachment, TTS, restart | PASS | Implemented shell surfaces were exercised without a crash or stuck terminal turn |
| Phase 10 | Import/export through integrated UI | NOT TESTED | No import/export controls are exposed by the prototype shell |
| Endpoint safety | Invalid/unavailable connection error surfaced | PASS | Unavailable injected endpoint produced a user-visible `FAILED` state |
| Endpoint safety | User-configured scheme/host/redirect/credential-forwarding policy | BLOCKED | Provider settings accept remote HTTP and the OpenRouter adapter does not apply `EndpointValidator`/redirect-host enforcement after endpoint edits |
| Multimodal capability | Capability-gated UI/provider behavior | BLOCKED | Attachment capability metadata is not enforced by the integrated send path |
| Import/export | Runtime export, clean import, malformed ZIP/path traversal, duplicate prevention | NOT TESTED | No runtime entry point; pure-Java import/export security tests are reported below as automated coverage only |
| Lifecycle | Background during slow HTTP stream and return | PASS | `phase9-background-stream` completed after Home/return |
| Lifecycle | Process kill during streaming | PASS | `phase9-process-kill` restored as `FAILED · interrupted by process termination`; no impossible `STREAMING` state remained |
| Lifecycle | Process kill after completed content | PASS | Persistence smoke restored completed turns once |
| Lifecycle | Activity recreation | NOT TESTED | No separate configuration-change trigger was available in the shell |
| Lifecycle | Network disconnect/restore during an active stream | NOT TESTED | Connection-failure behavior was tested; a live disconnect/reconnect transition was not demonstrated |

## Fixes and regression evidence

The initial red layout smoke showed the default-density composer center at y=1865,
below the visible app frame bottom y=1857. The root cause was a single weighted
vertical layout that placed the composer after the settings/message content. The
minimal fix puts settings/messages in an outer scroll view and pins the composer
row at the root. The red test then passed on the API 35 emulator.

The first persistence assertion also produced a false negative because the new
outer body scroll was still at its top position. The conversation JSON and records
were intact; the smoke test was corrected to reveal the message viewport. No
production persistence change was needed.

Focused commits:

* `448dfaf` — `fix: keep emulator chat controls visible`
* `84d0ee6` — `test: add android persistence restart smoke`

## Post-runtime regression

| Check | Result | Evidence |
| ----- | ------ | -------- |
| Previous automated baseline | PASS | 27/27 entry points before runtime recovery |
| Current automated baseline | PASS | 27/27 entry points after runtime recovery/fixes |
| Provider boundary audit | PASS | `audit-provider-boundary.sh` |
| Release/security boundary audit | PASS | `audit-release-boundary.sh` |
| Backend compilation | PASS | `python3 -m py_compile` for both staging backends |
| APK build and signing | PASS | `build-prototype.sh`; v3 signature verified; validated hash recorded above |
| Repository credential scan | PASS | No secret-shaped literal match; build output excluded |
| Final emulator layout smoke | PASS | Layout smoke script against validated APK |
| Final emulator persistence smoke | PASS | Persistence smoke script against validated APK |

## Screenshots and sanitized evidence paths

* `/tmp/mochi-layout-fixed.png` — fixed default-density layout.
* `/tmp/mochi-phase5-cancelled-2.png` — mock cancellation with partial assistant record.
* `/tmp/mochi-phase5-restart.png` — post-restart shell capture.
* `/tmp/mochi-final-runtime.png` — validated APK with restored completed records and pinned composer layout.

These files contain synthetic prompts and no credentials. The runtime UI dumps and
logcat checks were inspected without copying secret values into this report.

## Security and remaining blockers

No credential was entered or transmitted during this run. The Android credential
field remained empty, the repository scan passed, and the post-run logcat pattern
audit reported no provider credential patterns. The following remain merge-blocking
scope gaps or security findings:

1. Live BYOK OpenRouter behavior was not tested because no user-entered key was available.
2. Endpoint validation is not enforced consistently after configurable endpoint edits; redirect and destination forwarding safety are incomplete.
3. Attachment path confinement is not enforced for persisted attachment metadata.
4. Multimodal attachment delivery/capability gating is not connected to the integrated provider route.
5. Dictation is not reachable from the Android UI.
6. Sync is staging-connected but conversation enqueue/pull application is not integrated in the shell.
7. Import/export exists in code/tests but has no integrated runtime entry point.
8. HTTP malformed/disconnect scenarios and a true network disconnect/reconnect transition remain untested.

## Git structure and PR disposition

Recommendation: **Option A — keep PR #2 as the integration PR**. The provider,
attachment, voice, sync, and shell work are already intertwined across the existing
history; splitting now would provide little value and would risk a history rewrite.
No history rewrite was performed.

PR #2 remains open and draft:

`https://github.com/mehulpadole/chatgpt-android-research/pull/2`

No merge or deployment was performed. Because required runtime areas and security
boundaries remain incomplete, PR #2 is classified **RUNTIME INCOMPLETE** rather than
READY FOR REVIEW.
