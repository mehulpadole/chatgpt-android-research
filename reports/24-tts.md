# Text to Speech

`SystemTextToSpeechAdapter` wraps Android system TTS behind `TextToSpeechAdapter`, supports started/completed/error callbacks, explicit stop, and shutdown. The product shell exposes a `Read` action for the latest assistant response through `VoiceSessionCoordinator`.

Pure-Java fake-TTS tests confirm completion and audio-focus interruption cleanup. Android engine availability and audible output are Not tested without a runtime device.
