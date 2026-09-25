package com.example.androidfeasibility;

public final class FilePart implements ContentPart {
    public final String attachmentId;
    public final String displayName;
    public final String mimeType;

    public FilePart(String attachmentId, String displayName, String mimeType) {
        if (attachmentId == null || attachmentId.isEmpty()) throw new IllegalArgumentException("attachmentId is empty");
        this.attachmentId = attachmentId;
        this.displayName = displayName == null ? "file" : displayName;
        this.mimeType = mimeType == null ? "application/octet-stream" : mimeType;
    }

    @Override public String type() { return "file"; }
    @Override public ContentPart copy() { return new FilePart(attachmentId, displayName, mimeType); }
}
