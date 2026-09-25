package com.example.androidfeasibility;

import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** User-visible provider identity and safe endpoint metadata. */
public final class ProviderDefinition {
    public final String id;
    public final String displayName;
    public final URL endpoint;
    public final Set<String> capabilities;

    public ProviderDefinition(String id, String displayName, URL endpoint, Set<String> capabilities) {
        this.id = require(id, "id");
        this.displayName = displayName == null || displayName.trim().isEmpty() ? id : displayName.trim();
        this.endpoint = validateEndpoint(endpoint);
        this.capabilities = capabilities == null || capabilities.isEmpty()
                ? Collections.<String>emptySet()
                : Collections.unmodifiableSet(new HashSet<>(capabilities));
    }

    public boolean supports(String capability) {
        return capability != null && capabilities.contains(capability);
    }

    private URL validateEndpoint(URL value) {
        return EndpointValidator.validate(value);
    }

    private static String require(String value, String name) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException(name + " is empty");
        return value.trim();
    }
}
