package com.example.androidfeasibility;

public final class VoiceContractTest {
    public static void main(String[] args) {
        check(VoiceMode.DICTATION != VoiceMode.READ_ALOUD, "dictation and TTS must be distinct modes");
        check(VoiceMode.REALTIME != VoiceMode.DICTATION, "realtime must be distinct from dictation");
        check(VoiceState.ENDED != VoiceState.FAILED, "terminal states must be explicit");
        DeterministicSpeechToTextAdapter adapter = new DeterministicSpeechToTextAdapter();
        final String[] transcript = new String[1];
        adapter.start(new SpeechToTextAdapter.Listener() {
            @Override public void onReady() {}
            @Override public void onPartial(String text) {}
            @Override public void onFinal(String text) { transcript[0] = text; }
            @Override public void onError(String error) { throw new AssertionError(error); }
        });
        adapter.submitFinal("deterministic transcript");
        check("deterministic transcript".equals(transcript[0]),
                "deterministic STT must preserve transcript text");
        adapter.shutdown();
        System.out.println("VOICE CONTRACT TESTS PASSED");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
