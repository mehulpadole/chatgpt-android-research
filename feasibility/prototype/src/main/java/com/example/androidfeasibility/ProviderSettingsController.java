package com.example.androidfeasibility;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/** Pure settings state for provider credentials, endpoint, and model selection. */
public final class ProviderSettingsController {
    public enum State { NOT_CONFIGURED, READY, TESTING, CONNECTED, FAILED }

    public interface ConnectionProbe {
        ProviderModel test(URL endpoint, String modelId, String secret) throws Exception;
    }

    private final ProviderCredentialStore credentials;
    private URL endpoint;
    private String modelId = "openai/gpt-4";
    private State state;
    private String lastError = "";

    public ProviderSettingsController(ProviderCredentialStore credentials) {
        if (credentials == null) throw new IllegalArgumentException("credentials is null");
        this.credentials = credentials;
        try { endpoint = new URL("https://openrouter.ai/"); }
        catch (Exception impossible) { throw new IllegalStateException(impossible); }
        state = credentials.has("openrouter") ? State.READY : State.NOT_CONFIGURED;
    }

    public synchronized void setEndpoint(String value) throws Exception {
        URL candidate = new URL(value);
        if (!"http".equalsIgnoreCase(candidate.getProtocol())
                && !"https".equalsIgnoreCase(candidate.getProtocol())) {
            throw new IllegalArgumentException("provider endpoint must use HTTP or HTTPS");
        }
        endpoint = candidate;
    }

    public synchronized void setModel(String value) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException("model is empty");
        modelId = value.trim();
    }

    public synchronized void saveCredential(String secret) {
        credentials.put("openrouter", secret);
        state = State.READY;
        lastError = "";
    }

    public synchronized void removeCredential() {
        credentials.remove("openrouter");
        state = State.NOT_CONFIGURED;
        lastError = "";
    }

    public synchronized String maskedCredential() { return credentials.masked("openrouter"); }

    public synchronized URL endpoint() { return endpoint; }

    public synchronized String modelId() { return modelId; }

    public synchronized ProviderConfiguration configuration() {
        return new ProviderConfiguration("openrouter", modelId);
    }

    public synchronized State state() { return state; }

    public synchronized String lastError() { return lastError; }

    public synchronized State testConnection(ConnectionProbe probe) {
        if (!credentials.has("openrouter")) {
            state = State.NOT_CONFIGURED;
            lastError = "provider credential is missing";
            return state;
        }
        if (probe == null) throw new IllegalArgumentException("probe is null");
        state = State.TESTING;
        lastError = "";
        try {
            probe.test(endpoint, modelId, credentials.lookupForTransport("openrouter"));
            state = State.CONNECTED;
        } catch (Exception error) {
            state = State.FAILED;
            lastError = Redaction.message(error.getMessage());
        }
        return state;
    }

    /** Minimal connectivity probe; response bodies are deliberately discarded. */
    public synchronized State testConnection() {
        return testConnection(new ConnectionProbe() {
            @Override public ProviderModel test(URL base, String model, String secret) throws Exception {
                URL endpointUrl = new URL(base, "api/v1/models");
                HttpURLConnection connection = (HttpURLConnection) endpointUrl.openConnection();
                try {
                    connection.setRequestMethod("GET");
                    connection.setConnectTimeout(5_000);
                    connection.setReadTimeout(10_000);
                    connection.setRequestProperty("Authorization", "Bearer " + secret);
                    int status = connection.getResponseCode();
                    if (status < 200 || status >= 300) {
                        throw new IOException("provider connection returned HTTP " + status);
                    }
                    return new ProviderModel("openrouter", model, model, null, 0);
                } finally {
                    connection.disconnect();
                }
            }
        });
    }
}
