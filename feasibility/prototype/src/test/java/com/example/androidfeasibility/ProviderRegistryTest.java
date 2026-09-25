package com.example.androidfeasibility;

import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class ProviderRegistryTest {
    public static void main(String[] args) throws Exception {
        testRegistryRoutesAndChecksCapabilities();
        testUnsupportedEndpointSchemesAreRejected();
        testFactoryCreatesRegisteredAdapterOnly();
        System.out.println("PROVIDER REGISTRY TESTS PASSED");
    }

    private static void testRegistryRoutesAndChecksCapabilities() throws Exception {
        ProviderRegistry registry = new ProviderRegistry();
        ProviderAdapter adapter = new NoOpAdapter();
        registry.register(definition("custom", "https://example.test/", ProviderCapabilities.TEXT), adapter);
        check(registry.adapterFor("custom", ProviderCapabilities.TEXT) == adapter,
                "registered provider must route its adapter");
        boolean rejected = false;
        try { registry.adapterFor("custom", ProviderCapabilities.VISION); }
        catch (IllegalArgumentException expected) { rejected = true; }
        check(rejected, "unsupported provider capability must be rejected before routing");
    }

    private static void testUnsupportedEndpointSchemesAreRejected() throws Exception {
        boolean rejected = false;
        try { new ProviderDefinition("bad", "Bad", new URL("ftp://example.test/"), capabilities()); }
        catch (IllegalArgumentException expected) { rejected = true; }
        check(rejected, "FTP provider endpoint must be rejected");
        rejected = false;
        try { new ProviderDefinition("remote-http", "Remote HTTP", new URL("http://example.test/"), capabilities()); }
        catch (IllegalArgumentException expected) { rejected = true; }
        check(rejected, "remote cleartext endpoint must be rejected");
        new ProviderDefinition("local-http", "Local HTTP", new URL("http://127.0.0.1:8787/"), capabilities());
    }

    private static void testFactoryCreatesRegisteredAdapterOnly() throws Exception {
        ProviderRegistry registry = new ProviderRegistry();
        registry.registerDefinition(definition("factory", "Factory", new URL("https://example.test/"), capabilities()));
        ProviderAdapterFactory factory = new ProviderAdapterFactory(registry);
        factory.register("factory", new ProviderAdapterFactory.Creator() {
            @Override public ProviderAdapter create(ProviderDefinition definition) { return new NoOpAdapter(); }
        });
        check(factory.create("factory") != null, "factory must create known provider adapter");
        boolean rejected = false;
        try { factory.create("missing"); }
        catch (IllegalArgumentException expected) { rejected = true; }
        check(rejected, "factory must reject unknown provider IDs");
    }

    private static ProviderDefinition definition(String id, String endpoint, String... capabilities) throws Exception {
        return new ProviderDefinition(id, id, new URL(endpoint), new HashSet<>(Arrays.asList(capabilities)));
    }

    private static ProviderDefinition definition(String id, String title, URL endpoint,
                                                 Set<String> capabilities) {
        return new ProviderDefinition(id, title, endpoint, capabilities);
    }

    private static Set<String> capabilities() {
        return new HashSet<>(Arrays.asList(ProviderCapabilities.TEXT, ProviderCapabilities.STREAMING));
    }

    private static final class NoOpAdapter implements ProviderAdapter {
        @Override public StreamHandle start(ProviderRequest request, Listener listener) {
            return new StreamHandle() { @Override public void cancel() { } };
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
