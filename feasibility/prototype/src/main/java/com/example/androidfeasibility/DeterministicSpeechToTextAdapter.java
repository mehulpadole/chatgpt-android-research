package com.example.androidfeasibility;

/** Test/local STT adapter; it never accesses a microphone or sends audio. */
public final class DeterministicSpeechToTextAdapter implements SpeechToTextAdapter {
    private Listener listener;
    private boolean active;

    @Override public synchronized Session start(Listener listener) {
        if (listener == null) throw new IllegalArgumentException("listener is null");
        this.listener = listener;
        active = true;
        listener.onReady();
        return new Session() {
            @Override public void cancel() {
                synchronized (DeterministicSpeechToTextAdapter.this) { active = false; }
            }
        };
    }

    public synchronized void submitPartial(String text) {
        if (active && listener != null) listener.onPartial(text);
    }

    public synchronized void submitFinal(String text) {
        if (active && listener != null) {
            active = false;
            listener.onFinal(text);
        }
    }

    public synchronized void fail(String error) {
        if (active && listener != null) {
            active = false;
            listener.onError(error);
        }
    }

    public synchronized void shutdown() {
        active = false;
        listener = null;
    }
}
