package com.example.androidfeasibility;

public final class VoiceSessionCoordinatorTest {
    public static void main(String[] args) throws Exception {
        testPermissionDenial();
        testDictationSuccessAndCancellation();
        testSpeechFailureCleansUp();
        testTtsCompletionAndFocusInterruption();
        testRepeatedSessions();
        System.out.println("VOICE SESSION COORDINATOR TESTS PASSED");
    }

    private static void testPermissionDenial() {
        FakeCapture capture = new FakeCapture();
        FakeSpeech speech = new FakeSpeech();
        VoiceSessionCoordinator coordinator = coordinator(capture, speech, new FakeTts());
        coordinator.startDictation(false);
        check(coordinator.state() == VoiceState.FAILED, "permission denial must fail explicitly");
        check(!capture.started, "capture must not start without permission");
        check(!speech.started, "STT must not start without permission");
    }

    private static void testDictationSuccessAndCancellation() {
        FakeCapture capture = new FakeCapture();
        FakeSpeech speech = new FakeSpeech();
        RecordingListener listener = new RecordingListener();
        VoiceSessionCoordinator coordinator = coordinator(capture, speech, new FakeTts(), listener);
        coordinator.startDictation(true);
        check(coordinator.state() == VoiceState.LISTENING, "granted dictation must listen");
        speech.finalText("hello from mic");
        check(listener.transcript.equals("hello from mic"), "final transcript must reach composer listener");
        check(coordinator.state() == VoiceState.ENDED, "final transcript must end dictation");
        check(capture.stopped && capture.released, "dictation must release capture");

        coordinator.startDictation(true);
        coordinator.cancel();
        check(coordinator.state() == VoiceState.ENDED, "cancelled dictation must end");
        check(speech.cancelled, "cancel must cancel STT");
    }

    private static void testSpeechFailureCleansUp() {
        FakeCapture capture = new FakeCapture();
        FakeSpeech speech = new FakeSpeech();
        VoiceSessionCoordinator coordinator = coordinator(capture, speech, new FakeTts());
        coordinator.startDictation(true);
        speech.fail("recognizer unavailable");
        check(coordinator.state() == VoiceState.FAILED, "STT failure must be visible");
        check(capture.stopped && capture.released, "STT failure must release capture");
    }

    private static void testTtsCompletionAndFocusInterruption() {
        FakeTts tts = new FakeTts();
        VoiceSessionCoordinator coordinator = coordinator(new FakeCapture(), new FakeSpeech(), tts);
        coordinator.startReadAloud("assistant text");
        check(coordinator.state() == VoiceState.SPEAKING, "read aloud must speak");
        tts.complete();
        check(coordinator.state() == VoiceState.ENDED, "TTS completion must end");
        coordinator.startReadAloud("second text");
        coordinator.onAudioFocusLost();
        check(coordinator.state() == VoiceState.ENDED, "audio focus loss must end speech");
        check(tts.stopped, "audio focus loss must stop TTS");
    }

    private static void testRepeatedSessions() {
        FakeSpeech speech = new FakeSpeech();
        VoiceSessionCoordinator coordinator = coordinator(new FakeCapture(), speech, new FakeTts());
        coordinator.startDictation(true);
        speech.finalText("one");
        coordinator.startDictation(true);
        speech.finalText("two");
        check(coordinator.state() == VoiceState.ENDED, "repeated sessions must have independent terminal states");
    }

    private static VoiceSessionCoordinator coordinator(FakeCapture capture, FakeSpeech speech, FakeTts tts) {
        return coordinator(capture, speech, tts, new RecordingListener());
    }

    private static VoiceSessionCoordinator coordinator(FakeCapture capture, FakeSpeech speech,
                                                        FakeTts tts, RecordingListener listener) {
        return new VoiceSessionCoordinator(capture, speech, tts, null, listener);
    }

    private static final class RecordingListener implements VoiceSessionCoordinator.Listener {
        String transcript = "";
        @Override public void onStateChanged(VoiceState state, String error) {}
        @Override public void onTranscript(String transcript) { this.transcript = transcript; }
    }

    private static final class FakeCapture implements AudioCaptureController {
        boolean started;
        boolean stopped;
        boolean released;
        @Override public void start(Listener listener) { started = true; }
        @Override public void stop() { stopped = true; }
        @Override public void release() { released = true; }
    }

    private static final class FakeSpeech implements SpeechToTextAdapter {
        Listener listener;
        boolean started;
        boolean cancelled;
        @Override public Session start(Listener listener) {
            this.listener = listener;
            started = true;
            listener.onReady();
            return new Session() { @Override public void cancel() { cancelled = true; } };
        }
        void finalText(String text) { listener.onFinal(text); }
        void fail(String error) { listener.onError(error); }
    }

    private static final class FakeTts implements TextToSpeechAdapter {
        Listener listener;
        boolean stopped;
        @Override public Playback speak(String text, Listener listener) {
            this.listener = listener;
            listener.onStarted();
            return new Playback() { @Override public void stop() { stopped = true; } };
        }
        void complete() { listener.onCompleted(); }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
