package com.example.androidfeasibility;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** Composition-layer routing; the coordinator still sees only ProviderAdapter. */
public final class ProviderRouter implements ProviderAdapter {
    private final Map<String, ProviderAdapter> adapters;

    public ProviderRouter(Map<String, ProviderAdapter> adapters) {
        if (adapters == null || adapters.isEmpty()) {
            this.adapters = Collections.emptyMap();
        } else {
            this.adapters = Collections.unmodifiableMap(new HashMap<>(adapters));
        }
    }

    public ProviderRouter(ProviderRegistry registry) {
        this(registry == null ? null : registry.adapterSnapshot());
    }

    @Override public StreamHandle start(ProviderRequest request, Listener listener) {
        ProviderAdapter adapter = adapters.get(request.providerId);
        if (adapter != null) return adapter.start(request, listener);
        if (listener != null) {
            listener.onEvent(StreamEvent.failed(request.turnId, new ProviderError(
                    ProviderError.Category.PROVIDER,
                    "unknown provider: " + request.providerId, false)));
        }
        return new StreamHandle() {
            @Override public void cancel() {}
        };
    }
}
