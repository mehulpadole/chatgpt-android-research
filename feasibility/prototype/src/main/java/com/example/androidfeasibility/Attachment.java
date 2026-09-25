package com.example.androidfeasibility;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class Attachment {
    public final String attachmentId;
    public final String conversationId;
    public final String messageId;
    public final String displayName;
    public final String detectedMimeType;
    public final String declaredMimeType;
    public final long byteSize;
    public final String localReference;
    public AttachmentState state;
    public final long createdAt;
    public final Map<String, String> metadata;
    public final Map<String, ProviderAttachmentReference> providerReferences;

    public Attachment(String attachmentId, String conversationId, String messageId,
                      String displayName, String detectedMimeType, String declaredMimeType,
                      long byteSize, String localReference, AttachmentState state,
                      long createdAt, Map<String, String> metadata) {
        this.attachmentId = require(attachmentId, "attachmentId");
        this.conversationId = conversationId == null ? "" : conversationId;
        this.messageId = messageId == null ? "" : messageId;
        this.displayName = displayName == null ? "file" : displayName;
        this.detectedMimeType = detectedMimeType == null ? "application/octet-stream" : detectedMimeType;
        this.declaredMimeType = declaredMimeType == null ? "" : declaredMimeType;
        this.byteSize = Math.max(0L, byteSize);
        this.localReference = localReference == null ? "" : localReference;
        this.state = state == null ? AttachmentState.SELECTED : state;
        this.createdAt = createdAt;
        this.metadata = metadata == null || metadata.isEmpty()
                ? Collections.<String, String>emptyMap()
                : Collections.unmodifiableMap(new HashMap<>(metadata));
        this.providerReferences = new HashMap<>();
    }

    public Attachment copy() {
        Attachment copy = new Attachment(attachmentId, conversationId, messageId, displayName,
                detectedMimeType, declaredMimeType, byteSize, localReference, state, createdAt, metadata);
        copy.providerReferences.putAll(providerReferences);
        return copy;
    }

    public void addProviderReference(ProviderAttachmentReference reference) {
        if (reference == null || reference.providerId.isEmpty()) throw new IllegalArgumentException("reference is invalid");
        providerReferences.put(reference.providerId, reference);
    }

    private static String require(String value, String name) {
        if (value == null || value.isEmpty()) throw new IllegalArgumentException(name + " is empty");
        return value;
    }
}
