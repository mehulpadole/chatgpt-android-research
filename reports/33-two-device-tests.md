# Phase 9 Two-Device and Sync Integration Tests

`SyncIntegrationTest` covers timeout retry, a batch accepted before a
connection drop, stale-cursor protection, authentication expiry, quota
blocking, attachment metadata-only transfer, and the Java HTTP transport.

`TwoDeviceSimulationTest` covers the requested A–E cases:

| Case | Result | Meaning |
|---|---|---|
| A | Pass | Offline local save survives store recreation. |
| B | Pass | Device A's conversation metadata is visible to device B through a shared change cursor. |
| C | Pass | Equal revision/timestamp edits resolve by stable origin-device tie-breaker. |
| D | Pass | A tombstone is not resurrected by a stale entity. |
| E | Pass | Replay is idempotent and attachment metadata excludes local paths. |

These are deterministic contract/simulation tests, not proof of production
multi-device availability or Android UI behavior.
