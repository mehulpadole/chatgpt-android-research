# Phase 9 Sync Security Boundary

Sync is intentionally separate from provider execution. Provider API keys,
authorization headers, bearer values, refresh tokens, and local attachment
paths are rejected or omitted before an operation can enter the outbox.

The Java HTTP transport accepts an ephemeral token provider but does not persist,
log, or expose the token. Authentication expiry is surfaced as a terminal
`AUTH_EXPIRED` run result while local operations remain durable for a later
reauthenticated run. Retryable transport failures use bounded exponential
backoff; quota responses block without deleting local work.

The local staging backend is in-memory and has no production data store. If a
future authorized staging service persists sync data, the documented choice is
server-readable encryption at rest managed by that service. This is not an
end-to-end-encryption claim: the server would be able to process sync records.
No production Cloudflare, R2, account, billing, or credential service was added.
