# Topic Matrix and Limitations

The detailed continuation traces and evidence matrix are in 08-reports/05-phase-3-feature-traces-and-evidence-matrix.md. The rows below summarize the earlier pass; the follow-up report separates static confirmation from runtime testing and official-key provenance.

| Topic | Confirmed evidence | Status |
|---|---|---|
| Startup | `MainApplication`, `MainActivity`; emulator reaches logged-out home | Confirmed |
| Navigation/deep links | `ChatGptDeeplinkActivity`; HTTP(S) `/uc/` and `/app/uc/` normalization | Static confirmed; broader routes unknown |
| Composer | Runtime semantics expose `Ask ChatGPT`, attachment, dictation, send | Confirmed logged-out shell |
| Chat rendering | Compose-based runtime hierarchy; message/content classes and resources | Static/UI-shell confirmed; response rendering untested |
| Streaming | `ConversationStreamingService`; realtime/backend URL constants | Component boundary confirmed; transport unconfirmed |
| Auth | Auth Tab/Custom Tabs activity and redirect activity | Static confirmed; login untested |
| History/persistence | Room/WorkManager support, paging sources, history strings | Plumbing confirmed; schema/sync unknown |
| Attachments | SEND/SEND_MULTIPLE/PROCESS_TEXT/EDIT; file providers; camera permission | Static confirmed; selection/upload untested |
| Voice/audio | microphone permissions, WebRTC service, upload worker, native peer-connection library | Static confirmed; permission/recording/upload untested |
| Screen context | Accessibility service, screenshot API path, media-projection permission | Static confirmed; not enabled or tested |
| Settings/accessibility | Manifest and strings contain related surfaces | Not navigated in logged-out runtime |
| Performance/lifecycle | First launch eventually rendered; streaming service lifecycle logs; `onTrimMemory` code | Limited runtime sample |

## Important decoder limitation

The normal JADX pass produced a partial readable source tree and emitted type-inference, SSA, region, and stack-overflow errors in obfuscated Kotlin/Compose methods. The fallback pass completed all 40,537 classes but uses low-level register-oriented output for difficult methods. The reports therefore avoid treating inferred method names or decompiler reconstructions as authoritative where manifest/resource/runtime evidence is unavailable.

## Source limitations

- Google Play delivery could not be verified because the emulator was unauthenticated.
- APKMirror’s direct command-line endpoint returned Cloudflare/nonce responses; the browser download was used as the permitted inbound-transfer path.
- APKPure was used only as a corroborating archive source for package/certificate metadata, not as the installed artifact.
- No authenticated or user-specific data was introduced into the emulator, so server-side account behavior and local account data layout remain outside the tested evidence.
