package com.example.androidfeasibility;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** Provider-specific remote identity kept separate from local Attachment identity. */
public final class ProviderAttachmentReference {
    public final String providerId;
    public final String remoteId;
    public final String remoteUrl;
    public final Map<String, String> metadata;

    public ProviderAttachmentReference(String providerId, String remoteId, String remoteUrl,
                                       Map<String, String> metadata) {
        this.providerId = providerId == null ? "" : providerId;
        this.remoteId = remoteId == null ? "" : remoteId;
        this.remoteUrl = remoteUrl == null ? "" : remoteUrl;
        this.metadata = metadata == null || metadata.isEmpty()
                ? Collections.<String, String>emptyMap()
                : Collections.unmodifiableMap(new HashMap<>(metadata));
    }
}
