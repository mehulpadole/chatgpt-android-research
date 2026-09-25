# Phase 5 Provider-Neutral Contract and Test-Backend Integration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Move the independent Android prototype from a mock-specific provider request to a tested provider-neutral streaming contract with interchangeable mock and local HTTP/NDJSON adapters.

**Architecture:** `ConversationCoordinator` will create canonical IDs, own turn state, and persist snapshots. A small `ProviderRequest`/`StreamEvent` contract will carry only domain data, while `MockProvider` and `HttpStreamingProviderAdapter` translate their own scenario or wire concerns into that contract. A provider router will compose adapters for the Android UI without exposing transport details to the coordinator.

**Tech Stack:** Java 8-compatible Android source, `HttpURLConnection`, newline-delimited JSON (NDJSON), standard-library Python test backend, pure-Java conformance tests, existing hand-built Android build scripts.

**Spec:** `/home/mehul/.codex/attachments/d24c932d-c81e-472a-a817-2bc2e8170172/Pasted text.txt`

## Global Constraints

- Do not modify MoCHi.
- Do not modify the official ChatGPT APK during this phase.
- Do not use real provider credentials or commercial providers.
- The coordinator must not depend on `MockScenario`, HTTP clients, SSE/NDJSON parsers, URLs, HTTP status codes, or provider-specific response objects.
- Provider IDs, model IDs, provider request IDs, finish reasons, usage, and rate-limit details remain optional metadata rather than core message identity.
- The test backend uses an original NDJSON protocol and is development-only.
- In-progress turns restored after process death become failed/interrupted turns; automatic network resume is out of scope.

## Review Focus

- A late event from an old turn must not mutate the current turn; covered by association and late-event conformance tests.
- A provider disconnect without a terminal frame must produce one persisted failure; covered by the HTTP disconnect test.
- Cancellation must stop the HTTP worker and not only change coordinator state; covered by adapter transport test and slow-stream conformance test.
- Partial content must remain visible after failure and survive reload; covered by failure/persistence tests.
- Old JSON records without new error fields must still restore; covered by codec compatibility test.

---

### Task 1: Capture the Phase 5 baseline and dependency audit

**Files:**
- Create: `feasibility/reports/10-phase-5-baseline-and-contract.md`
- Test: existing `feasibility/prototype/scripts/run-core-tests.ps1`

**Interfaces:**
- Consumes: existing Phase 4 source and test output.
- Produces: a checked-in baseline record naming the current coordinator leaks, persistence policy before refactoring, and baseline test result.

- [ ] **Step 1: Run the existing core suite before code changes**

Run the repository’s existing test command in the available environment, or the equivalent direct `javac`/`java` command if the Windows-only script cannot run. Record the exact command and result.

- [ ] **Step 2: Document the current dependency graph**

Record the current `MainActivity → ConversationCoordinator → ProviderAdapter → MockProvider` path, the `MockScenario` leak, the current per-event persistence behavior, and the current terminal/error representation.

- [ ] **Step 3: Verify the baseline report and source scan**

Run `rg -n "MockScenario|ProviderAdapter\.Request|startTurn\(" feasibility/prototype` and include the relevant findings in the report without editing production code.

- [ ] **Step 4: Commit the audit artifact**

```bash
git add feasibility/reports/10-phase-5-baseline-and-contract.md
git commit -m "docs: record phase 5 baseline and provider leaks"
```

### Task 2: Define the provider-neutral domain contract

**Files:**
- Create: `feasibility/prototype/src/main/java/com/example/androidfeasibility/ProviderRequest.java`
- Create: `feasibility/prototype/src/main/java/com/example/androidfeasibility/ProviderConfiguration.java`
- Create: `feasibility/prototype/src/main/java/com/example/androidfeasibility/ProviderError.java`
- Create: `feasibility/prototype/src/main/java/com/example/androidfeasibility/ProviderMetadata.java`
- Modify: `feasibility/prototype/src/main/java/com/example/androidfeasibility/ProviderAdapter.java`
- Modify: `feasibility/prototype/src/main/java/com/example/androidfeasibility/StreamEvent.java`
- Test: `feasibility/prototype/src/test/java/com/example/androidfeasibility/ProviderContractTest.java`

**Interfaces:**
- Consumes: the Phase 4 request and event types.
- Produces: `ProviderRequest(conversationId, turnId, userMessageId, assistantMessageId, prompt, providerId, modelId, metadata)`, `ProviderConfiguration`, `ProviderError`, `ProviderMetadata`, and `StreamEvent` types `STARTED`, `DELTA`, `COMPLETED`, `FAILED`, `CANCELLED`.

- [ ] **Step 1: Write failing contract tests**

Test that a request carries four stable client IDs and no `MockScenario`, that started metadata is optional, that failure categories are provider-neutral, and that cancelled/failed events are distinct terminal events.

- [ ] **Step 2: Run the contract tests and confirm the expected compilation failure**

Run the direct pure-Java test command for `ProviderContractTest`. Expected: compilation fails because the new domain types and event factories do not exist.

- [ ] **Step 3: Implement the minimal immutable contract**

Use defensive copies for metadata maps. Make `ProviderAdapter.StreamHandle.cancel()` idempotence an adapter responsibility and keep the interface free of transport types. Do not include scenario, URL, headers, sockets, or response objects in `ProviderRequest`.

- [ ] **Step 4: Run the contract tests and the Phase 4 suite**

Expected: the new contract tests pass; existing tests may require only test-side call-site updates because the request signature changed.

- [ ] **Step 5: Commit the contract**

```bash
git add feasibility/prototype/src/main/java/com/example/androidfeasibility/Provider*.java feasibility/prototype/src/main/java/com/example/androidfeasibility/StreamEvent.java feasibility/prototype/src/test/java/com/example/androidfeasibility/ProviderContractTest.java
git commit -m "feat: define provider-neutral streaming contract"
```

### Task 3: Refactor the coordinator and persistence around the contract

**Files:**
- Modify: `feasibility/prototype/src/main/java/com/example/androidfeasibility/ConversationCoordinator.java`
- Modify: `feasibility/prototype/src/main/java/com/example/androidfeasibility/Message.java`
- Modify: `feasibility/prototype/src/main/java/com/example/androidfeasibility/AndroidConversationCodec.java`
- Modify: `feasibility/prototype/src/main/java/com/example/androidfeasibility/MockProvider.java`
- Modify: `feasibility/prototype/src/main/java/com/example/androidfeasibility/MockScenario.java`
- Modify: `feasibility/prototype/src/test/java/com/example/androidfeasibility/PrototypeCoreTest.java`
- Test: `feasibility/prototype/src/test/java/com/example/androidfeasibility/CoordinatorContractTest.java`

**Interfaces:**
- Consumes: the Task 2 domain contract.
- Produces: `ConversationCoordinator.startTurn(String prompt)`, `cancelActive()`/`cancel(turnId)` semantics, canonical IDs in every request/message, one-terminal-state behavior, persisted failure category/message, and a documented bounded delta checkpoint policy.

- [ ] **Step 1: Write failing coordinator tests**

Cover no-scenario `startTurn`, stable user/assistant IDs copied into `ProviderRequest`, duplicate/late terminal events, failure categories, cancellation idempotence, checkpointed partial content, and restoration of an interrupted streaming message.

- [ ] **Step 2: Run the focused tests and observe failures caused by the old API**

Expected: failures or compilation errors identify the old `MockScenario` argument and missing terminal/error behavior.

- [ ] **Step 3: Refactor the coordinator minimally**

Move scenario selection into `MockProvider` configuration. Add a provider configuration object for provider/model IDs. Ignore all events after `TurnRuntime.terminal` or for an unknown turn. Persist initial messages immediately, flush meaningful accumulated deltas according to one documented policy, and always flush terminal state.

- [ ] **Step 4: Make persistence backward-compatible**

Add optional failure category/message fields to `Message` and codec output; decode them with `optString` defaults so Phase 4 JSON remains readable. Restore streaming messages as failed with an interruption error and persist that repair once.

- [ ] **Step 5: Update the mock without leaking into the coordinator**

Let `MockProvider` own its selected scenario and add scenarios for duplicate completion, late delta, and failure after completion. Keep all deterministic response text and timing inside the mock.

- [ ] **Step 6: Run all pure-Java tests**

Expected: coordinator and existing Phase 4 behavior pass, with no `MockScenario` reference in `ConversationCoordinator.java` or `ProviderAdapter.java`.

- [ ] **Step 7: Commit the refactor**

```bash
git add feasibility/prototype/src/main/java/com/example/androidfeasibility feasibility/prototype/src/test/java/com/example/androidfeasibility
git commit -m "refactor: isolate coordinator from mock provider details"
```

### Task 4: Implement the local NDJSON backend and HTTP adapter

**Files:**
- Create: `feasibility/prototype/src/main/java/com/example/androidfeasibility/HttpStreamingProviderAdapter.java`
- Create: `feasibility/prototype/src/main/java/com/example/androidfeasibility/NdjsonCodec.java`
- Create: `feasibility/prototype/test-backend/test_backend.py`
- Create: `feasibility/prototype/src/test/java/com/example/androidfeasibility/HttpStreamingProviderAdapterTest.java`
- Create: `feasibility/prototype/src/test/java/com/example/androidfeasibility/LocalNdjsonTestServer.java`

**Interfaces:**
- Consumes: `ProviderAdapter`, `ProviderRequest`, `StreamEvent`, `ProviderError`, and `ProviderMetadata`.
- Produces: `HttpStreamingProviderAdapter(URL baseUrl, String scenario)`, incremental `POST /v1/test-stream` NDJSON streaming, injectable development URL, cancellation that disconnects the worker, and provider-neutral event mapping.

- [ ] **Step 1: Write failing adapter tests**

Test request encoding, started/delta/completed parsing, failed-frame category mapping, malformed-frame protocol failure, EOF-without-terminal network failure, and cancellation closing the worker.

- [ ] **Step 2: Run the adapter tests and confirm the expected failure**

Expected: compilation fails because the HTTP adapter, codec, and local server do not exist.

- [ ] **Step 3: Implement the minimal NDJSON codec and adapter**

Use `HttpURLConnection` and a daemon worker thread so the Android source has no third-party dependency. Keep the selected NDJSON wire format entirely inside the adapter/codec. On non-2xx responses map 401/403 to authentication, 429 to rate limit, timeout statuses to timeout, and other responses to provider failure. Treat EOF without a terminal frame as network failure.

- [ ] **Step 4: Implement the standalone Python backend**

Bind to `127.0.0.1` by default, accept `--host` and `--port`, stream original JSON frames for normal, slow, pre-content failure, partial failure, disconnect, duplicate completion, late delta, late failure, cancellation, empty, and malformed scenarios, and close cleanly on shutdown.

- [ ] **Step 5: Run the adapter tests and verify cancellation/resource cleanup**

Expected: all adapter tests pass, including a server-side observation that the cancelled client no longer keeps the streaming request active.

- [ ] **Step 6: Commit the transport layer**

```bash
git add feasibility/prototype/src/main/java/com/example/androidfeasibility/HttpStreamingProviderAdapter.java feasibility/prototype/src/main/java/com/example/androidfeasibility/NdjsonCodec.java feasibility/prototype/test-backend feasibility/prototype/src/test/java/com/example/androidfeasibility/HttpStreamingProviderAdapterTest.java feasibility/prototype/src/test/java/com/example/androidfeasibility/LocalNdjsonTestServer.java
git commit -m "feat: add local NDJSON streaming provider adapter"
```

### Task 5: Build and run one conformance suite against both providers

**Files:**
- Create: `feasibility/prototype/src/test/java/com/example/androidfeasibility/ProviderConformanceTest.java`
- Modify: `feasibility/prototype/src/test/java/com/example/androidfeasibility/PrototypeCoreTest.java`
- Modify: `feasibility/prototype/scripts/run-core-tests.ps1`
- Create: `feasibility/prototype/scripts/run-core-tests.sh`

**Interfaces:**
- Consumes: the same coordinator and repository API with `MockProvider` and `HttpStreamingProviderAdapter` factories.
- Produces: a shared semantic matrix covering success, multiple deltas, empty completion, cancellation twice/after completion, pre/post-content failure, duplicate and late terminal events, stable IDs, persistence, restoration, and interrupted-turn policy.

- [ ] **Step 1: Write the shared conformance suite first**

Parameterize the suite by provider factory and run every semantic assertion once for the mock and once for HTTP. Keep protocol parser tests separate from conformance tests.

- [ ] **Step 2: Run the suite and verify it fails for missing conformance harness behavior**

Expected: compilation or assertion failures identify any remaining adapter/coordinator mismatch.

- [ ] **Step 3: Make the smallest fixes in the owning layer**

Fix provider mapping in the adapter, state transitions in the coordinator, or scenario scheduling in the mock; never add transport branches to the coordinator to satisfy a provider-specific assertion.

- [ ] **Step 4: Add a Linux/macOS shell test runner while preserving the Windows runner**

The shell runner will compile all pure-Java main/test sources except Android-only classes, run `ProviderContractTest`, `CoordinatorContractTest`, `HttpStreamingProviderAdapterTest`, `ProviderConformanceTest`, and `PrototypeCoreTest`, and print a countable pass summary. The PowerShell runner will use the same source exclusions and test entry points.

- [ ] **Step 5: Run the full shared suite**

Expected: all tests pass for both providers with no real network or credentials.

- [ ] **Step 6: Commit the conformance harness**

```bash
git add feasibility/prototype/src/test/java feasibility/prototype/scripts/run-core-tests.ps1 feasibility/prototype/scripts/run-core-tests.sh
git commit -m "test: run provider conformance suite against mock and HTTP"
```

### Task 6: Integrate provider selection into the Android prototype

**Files:**
- Create: `feasibility/prototype/src/main/java/com/example/androidfeasibility/ProviderRouter.java`
- Modify: `feasibility/prototype/src/main/java/com/example/androidfeasibility/MainActivity.java`
- Modify: `feasibility/prototype/src/main/AndroidManifest.xml`
- Modify: `feasibility/prototype/scripts/build-prototype.ps1`
- Create: `feasibility/prototype/config/development.properties.example`
- Test: `feasibility/prototype/src/test/java/com/example/androidfeasibility/ProviderRouterTest.java`

**Interfaces:**
- Consumes: provider-neutral coordinator and both concrete adapters.
- Produces: mock/HTTP provider selection in the debug prototype, injectable `http://10.0.2.2:<port>` development URL, INTERNET permission, and no production cleartext relaxation.

- [ ] **Step 1: Write failing router/configuration tests**

Verify provider IDs route to the correct adapter, unknown IDs fail as provider-neutral errors, and the coordinator can change provider configuration only while idle.

- [ ] **Step 2: Run the focused tests and observe the expected failure**

Expected: missing router/configuration behavior.

- [ ] **Step 3: Implement routing and UI selection**

Keep the coordinator unaware of concrete providers. Keep mock scenario controls in the synthetic test UI. Add a provider spinner or equivalent development control, keep the default mock path working, and make the HTTP adapter URL injectable from a development-only constant/config file.

- [ ] **Step 4: Scope Android network configuration**

Add only the INTERNET permission to the manifest. Document host binding, emulator address `10.0.2.2`, port, and debug cleartext requirements in the Phase 5 report; do not add a production network-security exception.

- [ ] **Step 5: Build and install the prototype if the Android SDK/emulator is available**

Run the existing build script with the local environment’s equivalent paths when available. If the captured Windows SDK/emulator is unavailable, record that UI verification is pending rather than claiming it passed.

- [ ] **Step 6: Commit Android integration**

```bash
git add feasibility/prototype/src/main feasibility/prototype/scripts/build-prototype.ps1 feasibility/prototype/config/development.properties.example
git commit -m "feat: expose interchangeable providers in prototype UI"
```

### Task 7: Document Phase 5 evidence, security preparation, and audit the boundary

**Files:**
- Create: `feasibility/reports/10-phase-5-provider-neutral-contract-and-http-backend.md`
- Modify: `feasibility/README.md`
- Modify: `README.md`
- Modify: `feasibility/reports/09-recommended-next-step.md`
- Create: `feasibility/prototype/scripts/audit-provider-boundary.sh`

**Interfaces:**
- Consumes: implementation, test results, emulator evidence if available, and the Phase 5 specification.
- Produces: a complete evidence matrix, explicit lifecycle/interrupted-turn policy, emulator connectivity instructions, future credential security requirements, and a repeatable import/dependency audit.

- [ ] **Step 1: Write the report from observed evidence**

Separate confirmed test results, inferred architecture, unavailable emulator evidence, and future work. Record the chosen NDJSON rationale, persistence checkpoint policy, process-death policy, and no-credential guardrail.

- [ ] **Step 2: Add the security preparation section**

Document Android Keystore-backed future secret storage, exclusion of keys from Room/conversation records, saved UI state and logs, authorization-header redaction, environment separation, and user-controlled credential deletion. Do not create a fake credential store.

- [ ] **Step 3: Add the boundary audit script**

The script must fail if `ConversationCoordinator.java` or `ProviderAdapter.java` imports or references `MockScenario`, `HttpURLConnection`, `HttpStreamingProviderAdapter`, `NdjsonCodec`, `URL`, HTTP status constants, or provider-specific response types.

- [ ] **Step 4: Run the audit and full verification commands**

Run the boundary audit, full pure-Java suite, and any available Android build/install/smoke test. Read the complete outputs and record limitations.

- [ ] **Step 5: Update project-level phase status**

Mark Phase 5 complete only for the evidence actually demonstrated; retain “pending” wording for unavailable emulator UI checks.

- [ ] **Step 6: Commit the evidence and documentation**

```bash
git add feasibility/reports/10-phase-5-provider-neutral-contract-and-http-backend.md feasibility/README.md README.md feasibility/reports/09-recommended-next-step.md feasibility/prototype/scripts/audit-provider-boundary.sh
git commit -m "docs: record phase 5 provider boundary evidence"
```

## Verification Commands

From `feasibility/prototype`:

```bash
./scripts/run-core-tests.sh
./scripts/audit-provider-boundary.sh
```

On Windows, use:

```powershell
.\scripts\run-core-tests.ps1
.\scripts\build-prototype.ps1
```

The final report must not claim Android emulator evidence unless the build/install/HTTP-provider UI flow was actually run and its output or screenshots were inspected.
