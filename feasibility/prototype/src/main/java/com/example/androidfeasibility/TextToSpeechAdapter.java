package com.example.androidfeasibility;

public interface TextToSpeechAdapter {
    interface Playback { void stop(); }

    interface Listener {
        void onStarted();
        void onCompleted();
        void onError(String error);
    }

    Playback speak(String text, Listener listener);
}
