# Voice Contracts

Voice is split into `SpeechToTextAdapter`, `TextToSpeechAdapter`, `RealtimeVoiceAdapter`, `AudioCaptureController`, `AudioPlaybackController`, and `VoiceSessionCoordinator`. `VoiceMode` distinguishes dictation, read aloud, and realtime voice. Provider and platform implementations do not become conversation-history stores.

`VoiceSessionCoordinator` owns explicit states and terminal cleanup. It does not start recording automatically and does not combine TTS with STT into one provider contract.
