# Phase 10 Web/Android Compatibility Notes

The prototype keeps provider-neutral IDs, model capabilities, attachment
metadata, project membership, and memory scope independent of Android views.
The Android shell consumes those contracts and the same pure-Java tests run
outside Android. This supports later web parity at the contract level without
claiming an existing production web API or shared account backend.

Endpoint validation accepts HTTPS provider endpoints and restricts cleartext
HTTP to local development hosts (`localhost`, loopback, or Android emulator
loopback). Unsupported schemes, user-info URLs, and fragments are rejected.
The current UI/build is source and APK verified; cross-platform runtime parity
remains untested.
