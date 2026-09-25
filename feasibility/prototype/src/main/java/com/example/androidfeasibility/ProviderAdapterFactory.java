package com.example.androidfeasibility;

import java.util.HashMap;
import java.util.Map;

/** Lazily creates provider adapters from registered, provider-neutral definitions. */
public final class ProviderAdapterFactory {
    public interface Creator {
        ProviderAdapter create(ProviderDefinition definition) throws Exception;
    }

    private final ProviderRegistry registry;
    private final Map<String, Creator> creators = new HashMap<>();

    public ProviderAdapterFactory(ProviderRegistry registry) {
        if (registry == null) throw new IllegalArgumentException("provider registry is null");
        this.registry = registry;
    }

    public synchronized void register(String providerId, Creator creator) {
        if (providerId == null || providerId.isEmpty() || creator == null) {
            throw new IllegalArgumentException("provider creator is incomplete");
        }
        registry.definition(providerId);
        creators.put(providerId, creator);
    }

    public synchronized ProviderAdapter create(String providerId) throws Exception {
        ProviderDefinition definition = registry.definition(providerId);
        Creator creator = creators.get(providerId);
        if (creator == null) throw new IllegalArgumentException("provider creator is not configured: " + providerId);
        ProviderAdapter adapter = creator.create(definition);
        if (adapter == null) throw new IllegalStateException("provider creator returned null");
        registry.registerAdapter(providerId, adapter);
        return adapter;
    }
}
