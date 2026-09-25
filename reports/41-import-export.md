# Phase 11 Import/Export Evidence

`ExportService` produces versioned JSON, human-readable TXT, and ZIP archives
containing the conversation JSON. `ImportService` round-trips those formats,
preserves message text/content-part compatibility, rejects duplicate IDs, and
keeps attachment local paths empty on export/import.

ZIP imports enforce a bounded entry size, reject absolute and traversal paths,
allow only the documented attachment extensions, and require exactly one
`conversation.json`. Provider credentials are not part of the archive contract;
known secret-shaped message content is redacted as defense in depth.
