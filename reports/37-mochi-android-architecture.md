# Phase 10 MoCHi Android Architecture

The Android prototype now presents a MoCHi identity and routes provider
execution through `ProviderRegistry` and `ProviderRouter`. Conversation,
attachment, voice, sync, project, and memory services remain separate
contracts. Provider definitions carry only user-visible identity, safe endpoint
metadata, and application capabilities; provider wire details stay in adapters.

Navigation is represented by an explicit `NavigationState` for Home, Chat,
Projects, Memory, and Settings. The shell is intentionally lightweight and
original; it is not a reconstruction of proprietary MoCHi implementation
details or an official ChatGPT client modification.
