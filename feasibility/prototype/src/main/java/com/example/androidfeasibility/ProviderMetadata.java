package com.example.androidfeasibility;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** Optional provider metadata kept outside canonical message identity. */
public final class ProviderMetadata {
    public final String requestId;
    public final Map<String, String> attributes;

    public ProviderMetadata(String requestId, Map<String, String> attributes) {
        this.requestId = requestId == null ? "" : requestId;
        if (attributes == null || attributes.isEmpty()) {
            this.attributes = Collections.emptyMap();
        } else {
            this.attributes = Collections.unmodifiableMap(new HashMap<>(attributes));
        }
    }

    public static ProviderMetadata empty() {
        return new ProviderMetadata("", Collections.<String, String>emptyMap());
    }

    public static ProviderMetadata withRequestId(String requestId) {
        return new ProviderMetadata(requestId, Collections.<String, String>emptyMap());
    }
}
