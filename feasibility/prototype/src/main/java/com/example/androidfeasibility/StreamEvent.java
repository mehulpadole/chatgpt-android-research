package com.example.androidfeasibility;

public final class StreamEvent {
    public enum Type { STARTED, DELTA, COMPLETED, FAILED, CANCELLED }

    public final Type type;
    public final String turnId;
    public final String text;
    public final ProviderError error;
    public final ProviderMetadata metadata;

    private StreamEvent(Type type, String turnId, String text,
                        ProviderError error, ProviderMetadata metadata) {
        this.type = type;
        this.turnId = turnId;
        this.text = text;
        this.error = error;
        this.metadata = metadata == null ? ProviderMetadata.empty() : metadata;
    }

    public static StreamEvent started(String turnId) {
        return started(turnId, ProviderMetadata.empty());
    }

    public static StreamEvent started(String turnId, ProviderMetadata metadata) {
        return new StreamEvent(Type.STARTED, turnId, "", null, metadata);
    }

    public static StreamEvent delta(String turnId, String text) {
        return new StreamEvent(Type.DELTA, turnId, text == null ? "" : text,
                null, ProviderMetadata.empty());
    }

    public static StreamEvent completed(String turnId) {
        return new StreamEvent(Type.COMPLETED, turnId, "", null, ProviderMetadata.empty());
    }

    public static StreamEvent failed(String turnId, ProviderError error) {
        return new StreamEvent(Type.FAILED, turnId, "", error, ProviderMetadata.empty());
    }

    public static StreamEvent cancelled(String turnId) {
        return new StreamEvent(Type.CANCELLED, turnId, "", new ProviderError(
                ProviderError.Category.CANCELLED, "cancelled", false), ProviderMetadata.empty());
    }
}
