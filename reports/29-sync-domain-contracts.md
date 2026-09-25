# Phase 8 Sync Domain Contracts

The prototype now has a provider-neutral local-first sync boundary. `SyncEntity`,
`SyncOperation`, `SyncOutbox`, `SyncCursor`, `SyncClient`, `Entitlement`, and
`SyncStatus` describe local state and synchronization intent without importing
provider credentials or provider wire types into the conversation domain.

The `SyncOperation` constructor rejects authorization headers, bearer values,
API-key fields, refresh tokens, and OpenRouter-style secret prefixes. This is a
defense-in-depth boundary: sync payloads are not a credential store.

Evidence: `SyncContractTest`, `SyncRetryTest`, and the provider-boundary audit.
The implementation is local/staging-only; no production account, database, R2,
or billing integration was added.
