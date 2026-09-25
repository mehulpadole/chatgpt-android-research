# Phase 6 Provider Tests

## Confirmed local contract tests

```text
OPENROUTER CODEC TESTS PASSED
OPENROUTER ADAPTER TESTS PASSED
CREDENTIAL BOUNDARY TESTS PASSED
PROVIDER SETTINGS CONTROLLER TESTS PASSED
PROVIDER CONTRACT TESTS PASSED
COORDINATOR CONTRACT TESTS PASSED
PROVIDER ROUTER TESTS PASSED
HTTP ADAPTER TESTS PASSED
PROVIDER CONFORMANCE TESTS PASSED: mock + http
ANDROID CODEC COMPATIBILITY TESTS PASSED
ALL CORE TESTS PASSED
ALL PHASE 5 PURE-JAVA TESTS PASSED (11 test entry points)
```

The deterministic conformance suite remains authoritative for duplicate terminal events, late deltas, partial failure, cancellation, stable IDs, persistence, restoration, and interrupted-turn policy. OpenRouter-specific tests cover the deterministic wire behaviors that can be reproduced without external credentials.

## Not tested

The real-provider path was not executed against OpenRouter because no API key was present and no key was requested or stored. No rate-limit abuse was attempted.
