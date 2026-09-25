package com.example.androidfeasibility;

public interface RealtimeVoiceAdapter {
    interface Session { void cancel(); }

    interface Listener {
        void onConnected();
        void onTranscript(String text);
        void onAudio(byte[] audio);
        void onClosed();
        void onError(String error);
    }

    Session start(Listener listener);
}
