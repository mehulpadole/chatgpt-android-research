# Phase 6 Provider Security

## Confirmed

- Provider credentials are represented by `ProviderCredentialStore`, not by `ProviderRequest`, `Message`, `Conversation`, `StreamEvent`, or persistence codecs.
- Pure-Java credential tests cover add, replacement, masking, removal, missing-key behavior, and transport-only lookup.
- The Android implementation uses an Android Keystore AES-GCM key and stores only versioned ciphertext plus IV in private preferences.
- Authorization headers are set only inside `OpenRouterProviderAdapter` and the settings connectivity probe.
- Adapter and codec diagnostics redact `Bearer` and `Authorization` values.
- Missing credentials fail before a network request; local mock and test-http providers remain available.

## Not tested

- Android Keystore behavior on a real device/emulator was not tested because no AVD or device was available.
- No live OpenRouter credential was available, so real authentication and provider-side key handling remain Not tested.

## Evidence command

```text
./feasibility/prototype/scripts/run-core-tests.sh
```

Result: 11 pure-Java entry points passed, including `CredentialBoundaryTest`, `OpenRouterProviderAdapterTest`, and `ProviderSettingsControllerTest`.
