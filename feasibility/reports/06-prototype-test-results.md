# Independent prototype test results

## Core tests

Command:

`C:\Users\Welcome\Downloads\ChatGPT-Android-Feasibility\05-independent-prototype\scripts\run-core-tests.ps1`

Result: `ALL CORE TESTS PASSED`.

Covered cases include normal incremental completion, stable IDs, cancellation with late events ignored, failure before content, failure after partial content, duplicate terminal handling, turn association, persistence/restoration, and real deterministic `MockProvider` completion.

## Emulator smoke tests

| Scenario | Evidence | Result |
|---|---|---|
| Initial launch and native controls | `06-tests/prototype-initial-v2.png`, `prototype-initial-window.xml` | Confirmed |
| Normal completion | `prototype-streaming.png`, `prototype-completed.png`, `prototype-completed-window.xml` | Confirmed |
| Slow incremental stream | `prototype-slow-streaming.png`, `prototype-slow-completed.png` | Confirmed; visible `STREAMING` partial assistant row |
| Cancellation | `prototype-cancelled.png`, hierarchy booleans `HasCancelled=True`, `HasLateFullReply=False` | Confirmed |
| Failure after partial content | `prototype-failed.png`, hierarchy booleans `HasFailed=True`, `HasPartial=True` | Confirmed |
| Process restart/restoration | `prototype-restored.png` and restoration hierarchy | Confirmed |
| Background/foreground return | `prototype-background-return.png` and hierarchy | Confirmed for persisted completed state |
| Real network/provider | None | Not tested by design |
| Attachments/files | None | Not tested |
| Microphone/voice | None | Not tested |

## Runtime interpretation

The screenshots and UI hierarchy are evidence for the prototype only. They demonstrate the independent state machine and UI, not behavior of the official ChatGPT app. The official package was left restored separately.

The first smoke attempt accidentally navigated back to ChatGPT when a keyboard/back action was issued at the wrong time; this was corrected by reopening the prototype and rerunning the smoke sequence without the extra Back action. The corrected artifacts are the ones listed above.

## Limitations

The prototype has no background service or provider resume protocol. On restoration, a previously streaming turn follows the documented local policy and becomes failed. This is intentionally explicit so a future real provider can replace it with a durable job/resume design after that contract is known.
