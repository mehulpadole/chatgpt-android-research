# Audio Lifecycle Tests

Confirmed pure-Java output:

```text
VOICE SESSION COORDINATOR TESTS PASSED
VOICE CONTRACT TESTS PASSED
```

The coordinator handles permission denial, cancellation, completion, TTS focus interruption, provider failure, repeated sessions, and resource release. The APK build compiles the Android microphone/TTS implementations. Device-level audio focus, Bluetooth, screen lock, background, and process-death behavior are Not tested without an emulator/device.
