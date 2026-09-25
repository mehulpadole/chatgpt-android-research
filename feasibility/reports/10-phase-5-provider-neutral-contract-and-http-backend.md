# Phase 5 — Provider-neutral contract and test-backend integration

## Scope and result

Phase 5 moved the independent Android prototype from a mock-specific request API to a provider-neutral asynchronous streaming boundary. The work stayed in the independent prototype and did not modify MoCHi, the official ChatGPT APK, production configuration, provider credentials, or production data.

The contract, mock adapter, local HTTP adapter, conformance suite, standalone test backend, Android build, and boundary audit are verified. Emulator UI installation and HTTP-provider smoke testing remain pending because no Android emulator was connected in this environment.

## Architecture

```text
MainActivity
  -> ConversationCoordinator
       -> ProviderAdapter
            +-- ProviderRouter -> MockProvider
            +-- ProviderRouter -> HttpStreamingProviderAdapter
       -> ConversationRepository
            +-- JsonConversationRepository
            +-- InMemoryConversationRepository
```

The coordinator owns canonical conversation/turn/message identity, state transitions, terminal idempotence, cancellation state, and persistence. Concrete adapters own their timing, transport, wire protocol, and provider error translation.

## Frozen domain contract

`ProviderRequest` contains conversation ID, turn ID, user message ID, assistant message ID, prompt, provider ID, model ID, and optional string metadata.

`MockScenario`, URLs, headers, sockets, HTTP status codes, NDJSON details, and provider response objects are not part of that request.

`StreamEvent` supports `STARTED`, `DELTA`, `COMPLETED`, `FAILED`, and `CANCELLED`. Failure details use the provider-neutral categories `NETWORK`, `TIMEOUT`, `PROTOCOL`, `PROVIDER`, `AUTHENTICATION`, `RATE_LIMIT`, `CANCELLED`, and `UNKNOWN`. Optional request IDs remain `ProviderMetadata` rather than canonical message identity.

The coordinator enforces this invariant:

> A turn becomes terminal at most once. After completion, failure, or cancellation, later deltas and terminal events are ignored.

Cancellation is exposed as `ProviderAdapter.StreamHandle.cancel()`. The coordinator can cancel a turn without knowing whether the adapter uses an executor, HTTP, NDJSON, SSE, or another transport.

## Persistence and lifecycle policy

- User and assistant placeholder messages are persisted before provider work starts.
- Streaming content is checkpointed after at least 16 new content characters have accumulated since the previous checkpoint.
- Terminal completion, failure, and cancellation always flush the full conversation snapshot.
- Failure category and message are persisted on the assistant message while preserving partial content.
- Existing JSON records without the new failure fields decode with empty defaults.
- A persisted `STREAMING` message restored after process death becomes `FAILED` with category `UNKNOWN` and message `interrupted by process termination`.
- Automatic network resume is intentionally not implemented in Phase 5.

## Local HTTP protocol

The test adapter uses an original newline-delimited JSON protocol at:

```text
POST /v1/test-stream
Content-Type: application/json
Accept: application/x-ndjson
```

The adapter translates wire frames such as `started`, `delta`, `completed`, `failed`, and `cancelled` into `StreamEvent` values. The coordinator never sees a raw HTTP response or NDJSON parser.

The backend is `feasibility/prototype/test-backend/test_backend.py`. It is deliberately not OpenAI-compatible and accepts synthetic requests without authentication. It supports normal, slow, empty, pre-content failure, partial failure, disconnect, duplicate completion, late delta, late failure, cancellation, and malformed-frame scenarios.

The Android emulator reaches a host-bound backend through `10.0.2.2`. The default development URL is `http://10.0.2.2:8765/`; `MainActivity` accepts the `phase5_http_base_url` intent extra for development injection. The standalone prototype has a narrowly scoped cleartext network configuration for `10.0.2.2` and `127.0.0.1`; no production application configuration was changed.

Run the backend on the host with:

```bash
python3 feasibility/prototype/test-backend/test_backend.py --host 127.0.0.1 --port 8765
```

## Verification evidence

### Pure-Java behavior

`feasibility/prototype/scripts/run-core-tests.sh` passed all six entry points:

```text
PROVIDER CONTRACT TESTS PASSED
COORDINATOR CONTRACT TESTS PASSED
PROVIDER ROUTER TESTS PASSED
HTTP ADAPTER TESTS PASSED
PROVIDER CONFORMANCE TESTS PASSED: mock + http
ALL CORE TESTS PASSED
ALL PHASE 5 PURE-JAVA TESTS PASSED (6 test entry points)
```

The conformance suite runs the same semantic behaviors against both providers:

| Behavior | Mock | HTTP |
|---|---:|---:|
| Incremental successful stream | passed | passed |
| Multiple deltas | passed | passed |
| Empty completion | passed | passed |
| Cancel mid-stream | passed | passed |
| Cancel twice | passed | passed |
| Cancel after completion | passed | passed |
| Failure before content | passed | passed |
| Failure after partial content | passed | passed |
| Duplicate completion | passed | passed |
| Delta after completion | passed | passed |
| Failure after completion | passed | passed |
| Stable IDs and message association | passed | passed |
| Persistence and restoration | passed | passed |
| Interrupted streaming-turn policy | passed | passed |

The HTTP-specific tests additionally passed request encoding, provider error mapping, malformed-frame protocol failure, EOF-without-terminal network failure, HTTP 429 rate-limit mapping, and cancellation worker cleanup.

### Standalone backend

The Python backend was started on `127.0.0.1:18765` and exercised with `curl`. It emitted the expected `started`, two `delta`, and `completed` frames for a synthetic request.

### Android build

Both the portable Linux helper `scripts/build-prototype.sh` and the equivalent direct SDK command compiled the Android sources with API 37/build-tools 36, built the APK, aligned it, and verified a local test signature with `apksigner`. AAPT2 inspection confirmed the package `com.example.androidfeasibility`, version `0.1-phase5`, `android.permission.INTERNET`, and launchable activity `MainActivity`.

### Boundary audit

`feasibility/prototype/scripts/audit-provider-boundary.sh` passed. It checks that `ConversationCoordinator.java` and `ProviderAdapter.java` do not reference mock scenario types, HTTP clients, the HTTP adapter, the NDJSON codec, URLs, HTTP status constants, or provider-specific response types.

## Emulator evidence status

`adb devices -l` returned no connected device. Therefore the following are implemented but not claimed as runtime evidence in this phase:

- installing the Phase 5 APK on an emulator;
- sending a message through the Android HTTP-provider spinner selection;
- observing incremental HTTP text in the Android UI;
- background/foreground return during an HTTP stream;
- activity recreation and process termination with the UI;
- screenshots of the HTTP-provider flow.

The existing Phase 4 emulator screenshots remain valid for the earlier mock-driven prototype, not for this new HTTP UI path.

## Security preparation for a future real provider

No real provider or API key is integrated. Before a future BYOK adapter is added:

- store user credentials using Android Keystore-backed protection;
- never place credentials in conversation/Room records, saved UI state, APK resources, or logs;
- redact authorization headers and request bodies before logging;
- keep development backend URLs and production endpoints in separate configuration paths;
- map authentication and rate-limit failures without echoing secrets;
- provide user-controlled credential replacement and deletion;
- keep credentials out of the official APK and MoCHi.

## Remaining limitations and next decision

The Phase 5 transport is a deterministic test boundary, not a real provider integration. Authenticated backend behavior, attachments, voice, reconnection/resume, and production credential handling remain outside this phase.

The next decision is whether to perform optional authenticated emulator observations or to design a deliberately chosen real-provider/BYOK adapter after reviewing the Phase 5 contract and security requirements. OpenRouter and other commercial providers should remain outside the prototype until that decision is explicit.
