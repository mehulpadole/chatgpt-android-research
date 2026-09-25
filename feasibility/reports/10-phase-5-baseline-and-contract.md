# Phase 5 baseline and provider-boundary audit

## Audit date and scope

This is the read-only baseline for Phase 5 of the independent Android feasibility prototype. The audit was run on 2026-09-25 in the Phase 5 isolated worktree. It did not open or modify MoCHi, the official ChatGPT APK, production configuration, provider credentials, or production data.

## Verification environment

The repository’s existing baseline command is:

```powershell
feasibility/prototype/scripts/run-core-tests.ps1
```

The Windows PowerShell command could not run because PowerShell is not installed. After installing the available OpenJDK 17 headless package as test tooling, the equivalent pure-Java command was run:

```text
OUT=$(mktemp -d /tmp/phase5-baseline-XXXXXX)
find feasibility/prototype/src/main/java -name '*.java' \
  ! -name MainActivity.java \
  ! -name JsonConversationRepository.java \
  ! -name AndroidConversationCodec.java
javac --release 8 -d "$OUT" <pure-Java-main-sources> <test-sources>
java -cp "$OUT" com.example.androidfeasibility.PrototypeCoreTest
ALL CORE TESTS PASSED
```

The baseline passed: `ALL CORE TESTS PASSED`. A portable shell runner is still required so the same command is repeatable without manually assembling source lists.

## Existing dependency graph

```text
MainActivity
  -> ConversationCoordinator
       -> ProviderAdapter.Request
            -> MockScenario, provider name, model name, prompt
       -> ConversationRepository
            -> JsonConversationRepository / InMemoryConversationRepository
  -> MockProvider
       -> ScheduledExecutorService
       -> StreamEvent
```

The UI calls `ConversationCoordinator.startTurn(prompt, MockScenario)`. The coordinator creates a user message and a streaming assistant placeholder, persists that initial snapshot, and calls `ProviderAdapter.start()`.

## Boundary leaks found

The pre-refactor source scan was:

```bash
rg -n "MockScenario|ProviderAdapter\\.Request|startTurn\\(" feasibility/prototype
```

The relevant findings were:

- `ProviderAdapter.java:16-21` puts `MockScenario` in the provider request.
- `ConversationCoordinator.java:64` accepts `MockScenario` directly.
- `ConversationCoordinator.java:82-84` copies the scenario into the request.
- `MockProvider.java:22-46` consumes the scenario to select timing, content, and terminal behavior.
- `MainActivity.java:98` passes the selected scenario into the coordinator.
- Existing tests pass scenarios through the coordinator at lines 58, 77, 93, 99, 114, 119, 135, and 158 of `PrototypeCoreTest.java`.

This means mock timing and scenario selection are currently part of the coordinator-facing API. HTTP concepts are not present yet, but the request type is not provider-neutral because of the mock field.

## Existing state and persistence behavior

- `ConversationCoordinator` generates UUIDs for the conversation’s user and assistant messages and keeps those IDs stable in the in-memory model.
- `StreamEvent` identifies events by `turnId`, but only supports `STARTED`, `DELTA`, `COMPLETED`, and `ERROR`.
- A terminal flag prevents duplicate completion/error mutation after a turn reaches a terminal state.
- The coordinator persists after the initial message pair, after every stream event, and after cancellation.
- `Message` stores status/content/provider/model but not a structured failure category/message.
- `restore()` converts any persisted `STREAMING` message to `FAILED`, then saves the repaired conversation.
- The Android JSON codec has no new error fields and therefore supplies no backward-compatible extension point for structured failure details yet.
- The mock’s cancellation handle cancels scheduled jobs, while the coordinator ignores late events after cancellation.

## Phase 5 baseline decisions

1. Keep canonical conversation, turn, user-message, and assistant-message IDs in the client-owned request and message model.
2. Move `MockScenario` into mock/test configuration, not the coordinator request.
3. Replace string-only errors with a provider-neutral category and message while retaining persisted assistant content.
4. Add explicit `FAILED` and `CANCELLED` stream event types and preserve the one-terminal-state invariant.
5. Introduce a bounded streaming checkpoint policy so the contract does not require a database write for every network frame; terminal states always flush.
6. Use an original NDJSON test protocol for the local HTTP adapter. The coordinator will see only domain events.
