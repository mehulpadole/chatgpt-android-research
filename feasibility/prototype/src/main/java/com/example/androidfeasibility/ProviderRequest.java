package com.example.androidfeasibility;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** Client-owned identity and input for one provider stream. */
public final class ProviderRequest {
    public final String conversationId;
    public final String turnId;
    public final String userMessageId;
    public final String assistantMessageId;
    public final String prompt;
    public final String providerId;
    public final String modelId;
    public final Map<String, String> metadata;

    public ProviderRequest(String conversationId, String turnId,
                           String userMessageId, String assistantMessageId,
                           String prompt, String providerId, String modelId,
                           Map<String, String> metadata) {
        this.conversationId = require(conversationId, "conversationId");
        this.turnId = require(turnId, "turnId");
        this.userMessageId = require(userMessageId, "userMessageId");
        this.assistantMessageId = require(assistantMessageId, "assistantMessageId");
        this.prompt = prompt == null ? "" : prompt;
        this.providerId = require(providerId, "providerId");
        this.modelId = require(modelId, "modelId");
        this.metadata = immutableCopy(metadata);
    }

    private static String require(String value, String name) {
        if (value == null || value.isEmpty()) throw new IllegalArgumentException(name + " is empty");
        return value;
    }

    private static Map<String, String> immutableCopy(Map<String, String> source) {
        if (source == null || source.isEmpty()) return Collections.emptyMap();
        return Collections.unmodifiableMap(new HashMap<>(source));
    }
}
