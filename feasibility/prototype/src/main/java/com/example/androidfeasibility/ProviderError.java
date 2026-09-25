package com.example.androidfeasibility;

/** Provider-independent failure information. */
public final class ProviderError {
    public enum Category {
        NETWORK,
        TIMEOUT,
        PROTOCOL,
        PROVIDER,
        AUTHENTICATION,
        RATE_LIMIT,
        INVALID_REQUEST,
        MODEL_UNAVAILABLE,
        PROVIDER_UNAVAILABLE,
        CANCELLED,
        UNKNOWN
    }

    public final Category category;
    public final String message;
    public final boolean retryable;

    public ProviderError(Category category, String message, boolean retryable) {
        this.category = category == null ? Category.UNKNOWN : category;
        this.message = message == null || message.isEmpty() ? this.category.name().toLowerCase() : message;
        this.retryable = retryable;
    }
}
