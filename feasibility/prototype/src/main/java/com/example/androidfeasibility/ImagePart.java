package com.example.androidfeasibility;

public final class ImagePart implements ContentPart {
    public final String attachmentId;
    public final String mimeType;

    public ImagePart(String attachmentId, String mimeType) {
        this.attachmentId = require(attachmentId, "attachmentId");
        this.mimeType = mimeType == null ? "image/*" : mimeType;
    }

    @Override public String type() { return "image"; }
    @Override public ContentPart copy() { return new ImagePart(attachmentId, mimeType); }

    private static String require(String value, String name) {
        if (value == null || value.isEmpty()) throw new IllegalArgumentException(name + " is empty");
        return value;
    }
}
