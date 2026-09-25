# Phase 12 Release Build Evidence

Fresh verification completed:

- `run-core-tests.sh`: 27 pure-Java test entry points passed;
- provider boundary audit passed;
- release-boundary audit passed;
- debug APK build and signing passed;
- internal APK build and signing passed;
- signer certificate SHA-256: `7fc653152ee465795c5cb16f844c1c9601e554a533afb6b3297d439901136806`;
- `MOCHI_BUILD_VARIANT=release` is intentionally refused because no production
  release signing/resources/account configuration exists.

The output is a local prototype APK, not a store-ready MoCHi release or an
official ChatGPT APK.
