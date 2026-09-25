package com.example.androidfeasibility;

public interface ProviderAdapter {
    interface StreamHandle {
        void cancel();
    }

    interface Listener {
        void onEvent(StreamEvent event);
    }

    StreamHandle start(ProviderRequest request, Listener listener);
}
