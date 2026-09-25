package com.example.androidfeasibility;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** Provider-neutral model metadata used by selection and capability checks. */
public final class ProviderModel {
    public final String providerId;
    public final String id;
    public final String displayName;
    public final Set<String> capabilities;
    public final long contextLength;

    public ProviderModel(String providerId, String id, String displayName,
                         Set<String> capabilities, long contextLength) {
        this.providerId = providerId == null ? "" : providerId;
        this.id = require(id, "id");
        this.displayName = displayName == null || displayName.isEmpty() ? id : displayName;
        this.capabilities = capabilities == null || capabilities.isEmpty()
                ? Collections.<String>emptySet()
                : Collections.unmodifiableSet(new HashSet<>(capabilities));
        this.contextLength = contextLength < 0 ? 0 : contextLength;
    }

    private static String require(String value, String name) {
        if (value == null || value.isEmpty()) throw new IllegalArgumentException(name + " is empty");
        return value;
    }
}
