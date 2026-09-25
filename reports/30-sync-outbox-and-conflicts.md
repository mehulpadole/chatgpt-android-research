# Phase 8 Outbox, Retry, and Conflict Evidence

`JsonSyncStore` persists the outbox and cursor through an atomic replacement
file. Enqueue is idempotent by operation ID, pending and failed operations are
replayable, and a successful delete reaches the `DELETED` terminal state.

`SyncConflictResolver` is deterministic: higher server revision wins; for an
equal revision, deletion wins, then the newer update timestamp, then the
lexicographically stable origin-device ID. This makes stale-cursor recovery and
tombstone handling testable without relying on wall-clock ordering.

Evidence: `SyncOutboxTest`, `SyncConflictTest`, and `SyncRetryTest`. The
current prototype does not claim multi-device production conflict guarantees;
the local contract is the verified scope.
