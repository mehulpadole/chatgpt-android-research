package com.example.androidfeasibility;

public final class CredentialBoundaryTest {
    public static void main(String[] args) {
        InMemoryCredentialStore store = new InMemoryCredentialStore();
        check(!store.has("openrouter"), "new store must not contain a key");
        store.put("openrouter", "sk-first-secret");
        check(store.has("openrouter"), "stored key must be available to transport");
        check(store.masked("openrouter").equals("••••••••cret"),
                "masked display must reveal only a short suffix");
        check(!store.masked("openrouter").contains("sk-first-secret"),
                "masked display must not expose the full key");
        store.put("openrouter", "sk-second-secret");
        check(store.lookupForTransport("openrouter").equals("sk-second-secret"),
                "replacement must update transport value");
        store.remove("openrouter");
        check(!store.has("openrouter"), "remove must delete the credential");
        check(store.lookupForTransport("openrouter").isEmpty(),
                "removed credential must not be returned");
        System.out.println("CREDENTIAL BOUNDARY TESTS PASSED");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
