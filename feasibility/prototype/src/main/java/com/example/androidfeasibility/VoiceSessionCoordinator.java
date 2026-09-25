package com.example.androidfeasibility;

/** Coordinates voice lifecycle without coupling STT, TTS, realtime, or Android resources. */
public final class VoiceSessionCoordinator {
    public interface Listener {
        void onStateChanged(VoiceState state, String error);
        void onTranscript(String transcript);
    }

    private final AudioCaptureController capture;
    private final SpeechToTextAdapter speech;
    private final TextToSpeechAdapter tts;
    private final RealtimeVoiceAdapter realtime;
    private final Listener listener;
    private VoiceState state = VoiceState.IDLE;
    private SpeechToTextAdapter.Session speechSession;
    private TextToSpeechAdapter.Playback playback;
    private RealtimeVoiceAdapter.Session realtimeSession;
    private boolean captureStarted;
    private String lastError = "";

    public VoiceSessionCoordinator(AudioCaptureController capture, SpeechToTextAdapter speech,
                                   TextToSpeechAdapter tts, RealtimeVoiceAdapter realtime,
                                   Listener listener) {
        this.capture = capture;
        this.speech = speech;
        this.tts = tts;
        this.realtime = realtime;
        this.listener = listener;
    }

    public synchronized VoiceState state() { return state; }
    public synchronized String lastError() { return lastError; }

    public synchronized void startDictation(boolean permissionGranted) {
        endExisting();
        transition(VoiceState.REQUESTING_PERMISSION, "");
        if (!permissionGranted) {
            fail("microphone permission denied");
            return;
        }
        if (capture == null || speech == null) {
            fail("dictation is unavailable");
            return;
        }
        try {
            transition(VoiceState.CONNECTING, "");
            capture.start(new AudioCaptureController.Listener() {
                @Override public void onAudio(byte[] audio) {}
                @Override public void onError(String error) { fail(error); }
            });
            captureStarted = true;
            speechSession = speech.start(new SpeechToTextAdapter.Listener() {
                @Override public void onReady() { transition(VoiceState.LISTENING, ""); }
                @Override public void onPartial(String text) {
                    if (listener != null) listener.onTranscript(text == null ? "" : text);
                }
                @Override public void onFinal(String text) {
                    if (listener != null) listener.onTranscript(text == null ? "" : text);
                    finish();
                }
                @Override public void onError(String error) { fail(error); }
            });
        } catch (Exception error) {
            fail(error.getMessage());
        }
    }

    public synchronized void startReadAloud(String text) {
        endExisting();
        if (tts == null || text == null || text.isEmpty()) {
            fail("read aloud is unavailable");
            return;
        }
        try {
            transition(VoiceState.CONNECTING, "");
            playback = tts.speak(text, new TextToSpeechAdapter.Listener() {
                @Override public void onStarted() { transition(VoiceState.SPEAKING, ""); }
                @Override public void onCompleted() { finish(); }
                @Override public void onError(String error) { fail(error); }
            });
        } catch (Exception error) {
            fail(error.getMessage());
        }
    }

    public synchronized void startRealtime(boolean permissionGranted) {
        endExisting();
        if (!permissionGranted) { fail("microphone permission denied"); return; }
        if (realtime == null) { fail("realtime voice is unavailable"); return; }
        try {
            transition(VoiceState.CONNECTING, "");
            realtimeSession = realtime.start(new RealtimeVoiceAdapter.Listener() {
                @Override public void onConnected() { transition(VoiceState.LISTENING, ""); }
                @Override public void onTranscript(String text) {
                    if (listener != null) listener.onTranscript(text);
                }
                @Override public void onAudio(byte[] audio) {}
                @Override public void onClosed() { finish(); }
                @Override public void onError(String error) { fail(error); }
            });
        } catch (Exception error) {
            fail(error.getMessage());
        }
    }

    public synchronized void onAudioFocusLost() {
        if (state == VoiceState.SPEAKING) {
            if (playback != null) playback.stop();
            transition(VoiceState.INTERRUPTED, "audio focus lost");
            finish();
        }
    }

    public synchronized void cancel() {
        if (state == VoiceState.IDLE || state == VoiceState.ENDED
                || state == VoiceState.FAILED) return;
        transition(VoiceState.ENDING, "");
        closeResources();
        transition(VoiceState.ENDED, "");
    }

    private void endExisting() {
        if (state != VoiceState.IDLE && state != VoiceState.ENDED && state != VoiceState.FAILED) {
            closeResources();
        }
    }

    private void finish() {
        closeResources();
        transition(VoiceState.ENDED, "");
    }

    private void fail(String error) {
        closeResources();
        lastError = error == null || error.isEmpty() ? "voice failure" : error;
        transition(VoiceState.FAILED, lastError);
    }

    private void closeResources() {
        if (speechSession != null) { speechSession.cancel(); speechSession = null; }
        if (playback != null) { playback.stop(); playback = null; }
        if (realtimeSession != null) { realtimeSession.cancel(); realtimeSession = null; }
        if (captureStarted && capture != null) {
            capture.stop();
            capture.release();
            captureStarted = false;
        }
    }

    private void transition(VoiceState next, String error) {
        state = next;
        if (error != null && !error.isEmpty()) lastError = error;
        if (listener != null) listener.onStateChanged(next, error == null ? "" : error);
    }
}
