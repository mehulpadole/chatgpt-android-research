package com.example.androidfeasibility;

import java.io.BufferedReader;
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
import java.util.concurrent.atomic.AtomicBoolean;

/** Provider adapter for the local Phase 5 NDJSON streaming test backend. */
public final class HttpStreamingProviderAdapter implements ProviderAdapter {
    private final URL baseUrl;
    private final String scenario;
    private final Set<HttpStreamHandle> active = ConcurrentHashMap.newKeySet();
    private volatile boolean shutdown;

    public HttpStreamingProviderAdapter(URL baseUrl, String scenario) {
        if (baseUrl == null) throw new IllegalArgumentException("baseUrl is null");
        this.baseUrl = baseUrl;
        this.scenario = scenario == null || scenario.isEmpty() ? "NORMAL" : scenario;
    }

    @Override public StreamHandle start(final ProviderRequest request, final Listener listener) {
        if (shutdown) throw new IllegalStateException("adapter is shut down");
        if (request == null) throw new IllegalArgumentException("request is null");
        if (listener == null) throw new IllegalArgumentException("listener is null");
        final HttpStreamHandle handle = new HttpStreamHandle();
        handle.turnId = request.turnId;
        active.add(handle);
        Thread worker = new Thread(new Runnable() {
            @Override public void run() {
                runStream(handle, request, listener);
            }
        }, "phase5-http-stream-" + request.turnId);
        worker.setDaemon(true);
        handle.worker = worker;
        worker.start();
        return handle;
    }

    public int activeStreamCount() {
        return active.size();
    }

    public void shutdown() {
        shutdown = true;
        for (HttpStreamHandle handle : active) handle.cancel();
    }

    private void runStream(HttpStreamHandle handle, ProviderRequest request, Listener listener) {
        HttpURLConnection connection = null;
        boolean terminalSeen = false;
        try {
            URL endpoint = new URL(baseUrl, "v1/test-stream");
            connection = (HttpURLConnection) endpoint.openConnection();
            handle.connection = connection;
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setConnectTimeout(5_000);
            connection.setReadTimeout(10_000);
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            connection.setRequestProperty("Accept", "application/x-ndjson");
            byte[] requestBytes = NdjsonCodec.encodeRequest(request, scenario)
                    .getBytes(StandardCharsets.UTF_8);
            try (OutputStream output = connection.getOutputStream()) {
                output.write(requestBytes);
                output.flush();
            }
            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) {
                emitFailure(handle, listener, mapStatus(status), "HTTP status " + status);
                return;
            }
            try (InputStream raw = connection.getInputStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(raw, StandardCharsets.UTF_8))) {
                String line;
                while (!handle.cancelled.get() && !shutdown && (line = reader.readLine()) != null) {
                    final NdjsonCodec.Frame frame;
                    try {
                        frame = NdjsonCodec.parseFrame(line);
                    } catch (NdjsonCodec.ProtocolException error) {
                        emitFailure(handle, listener, new ProviderError(
                                ProviderError.Category.PROTOCOL, error.getMessage(), false),
                                error.getMessage());
                        terminalSeen = true;
                        break;
                    }
                    if (!request.turnId.equals(frame.turnId)) {
                        emitFailure(handle, listener, new ProviderError(
                                ProviderError.Category.PROTOCOL, "event turn ID mismatch", false),
                                "event turn ID mismatch");
                        terminalSeen = true;
                        break;
                    }
                    StreamEvent event = toEvent(frame);
                    if (event == null) {
                        emitFailure(handle, listener, new ProviderError(
                                ProviderError.Category.PROTOCOL, "unknown event type", false),
                                "unknown event type");
                        terminalSeen = true;
                        break;
                    }
                    listener.onEvent(event);
                    if (event.type == StreamEvent.Type.COMPLETED
                            || event.type == StreamEvent.Type.FAILED
                            || event.type == StreamEvent.Type.CANCELLED) {
                        terminalSeen = true;
                    }
                }
            }
            if (!handle.cancelled.get() && !shutdown && !terminalSeen) {
                emitFailure(handle, listener, new ProviderError(
                        ProviderError.Category.NETWORK,
                        "stream disconnected before terminal event", true),
                        "stream disconnected before terminal event");
            }
        } catch (SocketTimeoutException error) {
            if (!handle.cancelled.get() && !shutdown && !terminalSeen) {
                emitFailure(handle, listener, new ProviderError(
                        ProviderError.Category.TIMEOUT, "stream timed out", true), error.getMessage());
            }
        } catch (IOException error) {
            if (!handle.cancelled.get() && !shutdown && !terminalSeen) {
                emitFailure(handle, listener, new ProviderError(
                        ProviderError.Category.NETWORK,
                        error.getMessage() == null ? "network failure" : error.getMessage(), true),
                        error.getMessage());
            }
        } finally {
            if (connection != null) connection.disconnect();
            active.remove(handle);
            handle.closed.countDown();
        }
    }

    private StreamEvent toEvent(NdjsonCodec.Frame frame) {
        if ("started".equals(frame.type)) {
            return StreamEvent.started(frame.turnId, ProviderMetadata.withRequestId(frame.requestId));
        }
        if ("delta".equals(frame.type)) return StreamEvent.delta(frame.turnId, frame.text);
        if ("completed".equals(frame.type)) return StreamEvent.completed(frame.turnId);
        if ("cancelled".equals(frame.type)) return StreamEvent.cancelled(frame.turnId);
        if ("failed".equals(frame.type)) {
            return StreamEvent.failed(frame.turnId, new ProviderError(
                    parseCategory(frame.category), frame.message, frame.retryable));
        }
        return null;
    }

    private ProviderError.Category parseCategory(String value) {
        if (value == null) return ProviderError.Category.UNKNOWN;
        try {
            return ProviderError.Category.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException error) {
            return ProviderError.Category.PROVIDER;
        }
    }

    private ProviderError mapStatus(int status) {
        if (status == 401 || status == 403) {
            return new ProviderError(ProviderError.Category.AUTHENTICATION,
                    "authentication required", false);
        }
        if (status == 429) {
            return new ProviderError(ProviderError.Category.RATE_LIMIT,
                    "rate limited", true);
        }
        if (status == 408 || status == 504) {
            return new ProviderError(ProviderError.Category.TIMEOUT,
                    "provider request timed out", true);
        }
        return new ProviderError(ProviderError.Category.PROVIDER,
                "provider HTTP failure", status >= 500);
    }

    private void emitFailure(HttpStreamHandle handle, Listener listener,
                             ProviderError error, String fallback) {
        if (handle.cancelled.get() || shutdown) return;
        String message = error.message == null || error.message.isEmpty() ? fallback : error.message;
        listener.onEvent(StreamEvent.failed(handle.turnId == null ? "" : handle.turnId,
                new ProviderError(error.category, message, error.retryable)));
    }

    private final class HttpStreamHandle implements StreamHandle {
        final AtomicBoolean cancelled = new AtomicBoolean();
        final java.util.concurrent.CountDownLatch closed = new java.util.concurrent.CountDownLatch(1);
        volatile HttpURLConnection connection;
        volatile Thread worker;
        volatile String turnId;

        @Override public void cancel() {
            if (!cancelled.compareAndSet(false, true)) return;
            HttpURLConnection current = connection;
            if (current != null) current.disconnect();
            Thread currentWorker = worker;
            if (currentWorker != null) currentWorker.interrupt();
        }
    }
}
