# Dictation

The deterministic STT adapter and coordinator support permission-gated start, partial/final transcript delivery, cancellation, failure, repeated sessions, and insertion into the composer listener. `AndroidAudioCaptureController` uses `AudioRecord` only after `RECORD_AUDIO` permission is granted and releases the recorder on stop/error.

Confirmed in pure Java: permission denial, successful transcript, cancellation, STT failure, repeated sessions, and cleanup. Real microphone/STT emulator behavior is Not tested because no AVD/device was available.
