package com.example.androidfeasibility;

public interface SpeechToTextAdapter {
    interface Session { void cancel(); }

    interface Listener {
        void onReady();
        void onPartial(String text);
        void onFinal(String text);
        void onError(String error);
    }

    Session start(Listener listener);
}
