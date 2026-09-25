# Phase 10 Projects and Memory Evidence

Projects own local conversation membership through `ProjectRepository` and
preserve membership across repository copies. `MemoryEntry` has explicit
global and project scopes; `MemoryRepository` returns enabled entries only for
the requested scope, preventing project memory from leaking to another
project.

The prototype includes in-memory repositories and explicit navigation state.
Durable import/export and migration behavior are reserved for the next
hardening phase; no cloud memory or production account synchronization was
added.

Evidence: `ProjectMemoryTest` and the Phase 10 architecture report.
