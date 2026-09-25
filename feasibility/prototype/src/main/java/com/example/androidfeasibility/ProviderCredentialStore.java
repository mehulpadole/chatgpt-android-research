package com.example.androidfeasibility;

/** Credential boundary; only transport adapters may use lookupForTransport. */
public interface ProviderCredentialStore {
    void put(String providerId, String secret);

    String lookupForTransport(String providerId);

    boolean has(String providerId);

    String masked(String providerId);

    void remove(String providerId);
}
