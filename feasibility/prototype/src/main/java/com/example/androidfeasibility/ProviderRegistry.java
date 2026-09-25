package com.example.androidfeasibility;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Application-owned provider registry; providers remain behind ProviderAdapter. */
public final class ProviderRegistry {
    private final Map<String, ProviderDefinition> definitions = new HashMap<>();
    private final Map<String, ProviderAdapter> adapters = new HashMap<>();

    public synchronized void register(ProviderDefinition definition, ProviderAdapter adapter) {
        if (definition == null || adapter == null) throw new IllegalArgumentException("provider registration is incomplete");
        if (definitions.containsKey(definition.id)) throw new IllegalArgumentException("provider already registered");
        definitions.put(definition.id, definition);
        adapters.put(definition.id, adapter);
    }

    public synchronized void registerDefinition(ProviderDefinition definition) {
        if (definition == null) throw new IllegalArgumentException("provider definition is null");
        if (definitions.containsKey(definition.id)) throw new IllegalArgumentException("provider already registered");
        definitions.put(definition.id, definition);
    }

    public synchronized void registerAdapter(String providerId, ProviderAdapter adapter) {
        if (!definitions.containsKey(providerId)) throw new IllegalArgumentException("provider is not registered");
        if (adapter == null) throw new IllegalArgumentException("provider adapter is null");
        adapters.put(providerId, adapter);
    }

    public synchronized ProviderDefinition definition(String providerId) {
        ProviderDefinition definition = definitions.get(providerId);
        if (definition == null) throw new IllegalArgumentException("unknown provider: " + providerId);
        return definition;
    }

    public synchronized ProviderAdapter adapterFor(String providerId) {
        ProviderAdapter adapter = adapters.get(providerId);
        if (adapter == null) throw new IllegalArgumentException("provider adapter is not configured: " + providerId);
        return adapter;
    }

    public synchronized ProviderAdapter adapterFor(String providerId, String requiredCapability) {
        ProviderDefinition definition = definition(providerId);
        if (!definition.supports(requiredCapability)) {
            throw new IllegalArgumentException("provider does not support capability: " + requiredCapability);
        }
        return adapterFor(providerId);
    }

    public synchronized List<ProviderDefinition> definitions() {
        return Collections.unmodifiableList(new ArrayList<>(definitions.values()));
    }

    synchronized Map<String, ProviderAdapter> adapterSnapshot() {
        return new HashMap<>(adapters);
    }
}
