package com.example.androidfeasibility;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

/** OpenRouter chat-completions adapter; transport details stop at ProviderAdapter. */
public final class OpenRouterProviderAdapter implements ProviderAdapter {
    private final URL baseUrl;
    private final ProviderCredentialStore credentials;
    private final Set<OpenRouterStreamHandle> active = ConcurrentHashMap.newKeySet();
    private final int connectTimeoutMs;
    private final int readTimeoutMs;
    private volatile boolean shutdown;

    public OpenRouterProviderAdapter(URL baseUrl, ProviderCredentialStore credentials) {
        this(baseUrl, credentials, 5_000, 15_000);
    }

    public OpenRouterProviderAdapter(URL baseUrl, ProviderCredentialStore credentials,
                                     int connectTimeoutMs, int readTimeoutMs) {
        if (baseUrl == null) throw new IllegalArgumentException("baseUrl is null");
        if (credentials == null) throw new IllegalArgumentException("credentials is null");
        this.baseUrl = baseUrl;
        this.credentials = credentials;
        this.connectTimeoutMs = connectTimeoutMs;
        this.readTimeoutMs = readTimeoutMs;
    }

    @Override public StreamHandle start(final ProviderRequest request, final Listener listener) {
        if (shutdown) throw new IllegalStateException("adapter is shut down");
        if (request == null) throw new IllegalArgumentException("request is null");
        if (listener == null) throw new IllegalArgumentException("listener is null");
        final OpenRouterStreamHandle handle = new OpenRouterStreamHandle(request.turnId);
        active.add(handle);
        Thread worker = new Thread(new Runnable() {
            @Override public void run() { runStream(handle, request, listener); }
        }, "openrouter-stream-" + request.turnId);
        worker.setDaemon(true);
        handle.worker = worker;
        worker.start();
        return handle;
    }

    public int activeStreamCount() { return active.size(); }

    public void shutdown() {
        shutdown = true;
        for (OpenRouterStreamHandle handle : active) handle.cancel();
    }

    private void runStream(OpenRouterStreamHandle handle, ProviderRequest request, Listener listener) {
        HttpURLConnection connection = null;
        boolean terminal = false;
        boolean started = false;
        try {
            String secret = credentials.lookupForTransport(request.providerId);
            if (secret.isEmpty()) {
                emitFailure(handle, listener, new ProviderError(
                        ProviderError.Category.AUTHENTICATION, "provider credential is missing", false));
                return;
            }
            URL endpoint = new URL(baseUrl, "api/v1/chat/completions");
            connection = (HttpURLConnection) endpoint.openConnection();
            handle.connection = connection;
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setConnectTimeout(connectTimeoutMs);
            connection.setReadTimeout(readTimeoutMs);
            connection.setRequestProperty("Authorization", "Bearer " + secret);
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            connection.setRequestProperty("Accept", "text/event-stream");
            byte[] requestBytes = OpenRouterCodec.encodeRequest(request, true)
                    .getBytes(StandardCharsets.UTF_8);
            try (OutputStream output = connection.getOutputStream()) {
                output.write(requestBytes);
                output.flush();
            }
            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) {
                emitFailure(handle, listener, OpenRouterCodec.mapHttpError(status,
                        readAll(connection.getErrorStream())));
                return;
            }
            try (InputStream raw = connection.getInputStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(
                         raw, StandardCharsets.UTF_8))) {
                String line;
                while (!handle.cancelled.get() && !shutdown && (line = reader.readLine()) != null) {
                    String trimmed = line.trim();
                    if (trimmed.isEmpty() || trimmed.startsWith("event:") || trimmed.startsWith(":")) continue;
                    if (!trimmed.startsWith("data:")) continue;
                    OpenRouterCodec.Chunk chunk;
                    try {
                        chunk = OpenRouterCodec.parseSseData(trimmed);
                    } catch (OpenRouterCodec.ProtocolException error) {
                        emitFailure(handle, listener, new ProviderError(
                                ProviderError.Category.PROTOCOL, Redaction.message(error.getMessage()), false));
                        terminal = true;
                        break;
                    }
                    if (chunk.done) {
                        if (!started) listener.onEvent(StreamEvent.started(request.turnId,
                                chunk.metadata));
                        listener.onEvent(StreamEvent.completed(request.turnId));
                        terminal = true;
                        break;
                    }
                    if (!started) {
                        listener.onEvent(StreamEvent.started(request.turnId, chunk.metadata));
                        started = true;
                    }
                    if (!chunk.delta.isEmpty()) listener.onEvent(StreamEvent.delta(request.turnId, chunk.delta));
                }
            }
            if (!handle.cancelled.get() && !shutdown && !terminal) {
                emitFailure(handle, listener, new ProviderError(
                        ProviderError.Category.NETWORK,
                        "stream disconnected before terminal event", true));
            }
        } catch (SocketTimeoutException error) {
            if (!handle.cancelled.get() && !shutdown && !terminal) {
                emitFailure(handle, listener, new ProviderError(
                        ProviderError.Category.TIMEOUT, "provider stream timed out", true));
            }
        } catch (IOException error) {
            if (!handle.cancelled.get() && !shutdown && !terminal) {
                emitFailure(handle, listener, new ProviderError(
                        ProviderError.Category.NETWORK,
                        Redaction.message(error.getMessage()), true));
            }
        } finally {
            if (connection != null) connection.disconnect();
            active.remove(handle);
            handle.closed.countDown();
        }
    }

    private void emitFailure(OpenRouterStreamHandle handle, Listener listener, ProviderError error) {
        if (handle.cancelled.get() || shutdown) return;
        listener.onEvent(StreamEvent.failed(handle.turnId,
                new ProviderError(error.category, Redaction.message(error.message), error.retryable)));
    }

    private String readAll(InputStream input) throws IOException {
        if (input == null) return "";
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[2048];
        int read;
        try (InputStream stream = input) {
            while ((read = stream.read(buffer)) != -1) output.write(buffer, 0, read);
        }
        return new String(output.toByteArray(), StandardCharsets.UTF_8);
    }

    private final class OpenRouterStreamHandle implements StreamHandle {
        final String turnId;
        final AtomicBoolean cancelled = new AtomicBoolean();
        final CountDownLatch closed = new CountDownLatch(1);
        volatile HttpURLConnection connection;
        volatile Thread worker;

        OpenRouterStreamHandle(String turnId) { this.turnId = turnId; }

        @Override public void cancel() {
            if (!cancelled.compareAndSet(false, true)) return;
            HttpURLConnection current = connection;
            if (current != null) current.disconnect();
            Thread currentWorker = worker;
            if (currentWorker != null) currentWorker.interrupt();
        }
    }
}
