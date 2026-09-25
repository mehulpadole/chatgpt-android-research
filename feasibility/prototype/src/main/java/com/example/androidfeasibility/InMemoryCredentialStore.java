package com.example.androidfeasibility;

import java.util.HashMap;
import java.util.Map;

/** Test-only credential store; the Android implementation owns encrypted persistence. */
public final class InMemoryCredentialStore implements ProviderCredentialStore {
    private final Map<String, String> values = new HashMap<>();

    @Override public synchronized void put(String providerId, String secret) {
        if (providerId == null || providerId.isEmpty()) throw new IllegalArgumentException("providerId is empty");
        if (secret == null || secret.isEmpty()) throw new IllegalArgumentException("secret is empty");
        values.put(providerId, secret);
    }

    @Override public synchronized String lookupForTransport(String providerId) {
        String value = values.get(providerId);
        return value == null ? "" : value;
    }

    @Override public synchronized boolean has(String providerId) {
        return !lookupForTransport(providerId).isEmpty();
    }

    @Override public synchronized String masked(String providerId) {
        String value = lookupForTransport(providerId);
        if (value.isEmpty()) return "";
        String suffix = value.length() <= 4 ? value : value.substring(value.length() - 4);
        return "••••••••" + suffix;
    }

    @Override public synchronized void remove(String providerId) {
        values.remove(providerId);
    }
}
