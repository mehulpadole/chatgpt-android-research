package com.example.androidfeasibility;

/** Payload produced inside a provider attachment adapter, never persisted as domain state. */
public final class PreparedAttachment {
    public final String providerId;
    public final String attachmentId;
    public final String displayName;
    public final String mimeType;
    public final String dataUrl;

    public PreparedAttachment(String providerId, String attachmentId, String displayName,
                              String mimeType, String dataUrl) {
        this.providerId = providerId == null ? "" : providerId;
        this.attachmentId = attachmentId == null ? "" : attachmentId;
        this.displayName = displayName == null ? "file" : displayName;
        this.mimeType = mimeType == null ? "application/octet-stream" : mimeType;
        this.dataUrl = dataUrl == null ? "" : dataUrl;
    }
}
