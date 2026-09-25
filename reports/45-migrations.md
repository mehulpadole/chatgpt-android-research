# Phase 11 Migration Evidence

`SchemaVersion` identifies the Phase 5 schema and current archive schema.
`MigrationService` upgrades a Phase 5 conversation object that lacks a schema
field and rejects future unsupported versions. The migration fixture imports
successfully after upgrade, while JSON/TXT/ZIP import remains version-aware.

The Android conversation codec continues to accept records without newer
failure/content-part/attachment fields through its existing defaults. A full
production database migration is not claimed because the supplied repository
contains the independent prototype rather than MoCHi production persistence.
