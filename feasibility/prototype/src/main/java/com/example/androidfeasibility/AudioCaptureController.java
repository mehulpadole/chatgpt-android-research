package com.example.androidfeasibility;

public interface AudioCaptureController {
    interface Listener {
        void onAudio(byte[] audio);
        void onError(String error);
    }

    void start(Listener listener);
    void stop();
    void release();
}
