# Phase 10 Provider Registry Evidence

`ProviderDefinition`, `ProviderRegistry`, and `ProviderAdapterFactory` provide
the product-level provider boundary. Registry routing checks the requested
application capability before returning an adapter, and factory creation is
limited to registered provider IDs.

The test registry covers deterministic local, local HTTP, and OpenRouter-shaped
definitions. It verifies capability rejection and URL safety. The Android
shell uses the registry to compose the existing mock, staging HTTP, and
OpenRouter adapters; the test backend remains an explicit prototype control.

Evidence: `ProviderRegistryTest`, `ProviderRouterTest`, provider conformance
tests, and the signed APK build.
