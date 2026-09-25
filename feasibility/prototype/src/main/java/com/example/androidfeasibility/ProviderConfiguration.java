package com.example.androidfeasibility;

/** Provider/model selection owned by the application composition layer. */
public final class ProviderConfiguration {
    public final String providerId;
    public final String modelId;

    public ProviderConfiguration(String providerId, String modelId) {
        if (providerId == null || providerId.isEmpty()) throw new IllegalArgumentException("providerId is empty");
        if (modelId == null || modelId.isEmpty()) throw new IllegalArgumentException("modelId is empty");
        this.providerId = providerId;
        this.modelId = modelId;
    }
}
