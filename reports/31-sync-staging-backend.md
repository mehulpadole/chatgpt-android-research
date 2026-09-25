# Phase 8 Local Sync Staging Backend

`feasibility/prototype/test-backend/sync_backend.py` provides a standard-library
HTTP server for local protocol tests only. It exposes entitlement, cursor-based
changes, and push endpoints with in-memory revisions, idempotent operation IDs,
tombstone-shaped delete changes, and an optional byte quota.

The manual protocol smoke run verified:

- entitlement returns `CLOUD_SYNC`;
- an upsert is accepted and appears in changes from cursor zero;
- replaying the same operation ID is idempotent;
- payloads containing authorization/bearer data are rejected;
- a configured small quota returns HTTP 413.

No authentication, production persistence, Cloudflare, R2, or billing behavior
is represented by this server.
