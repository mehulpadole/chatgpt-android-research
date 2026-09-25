package com.example.androidfeasibility;

public interface AudioPlaybackController {
    interface Listener { void onCompleted(); void onError(String error); }

    void play(byte[] audio, Listener listener);
    void stop();
    void release();
}
