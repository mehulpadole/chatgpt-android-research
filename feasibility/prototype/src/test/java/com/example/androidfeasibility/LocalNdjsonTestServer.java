package com.example.androidfeasibility;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/** Test-only HTTP server that emits the Phase 5 original NDJSON protocol. */
final class LocalNdjsonTestServer implements AutoCloseable {
    private final String scenario;
    private final int responseStatus;
    private final HttpServer server;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final AtomicInteger activeRequests = new AtomicInteger();
    private volatile String lastRequestBody = "";

    LocalNdjsonTestServer(String scenario) throws IOException {
        this(scenario, 200);
    }

    LocalNdjsonTestServer(String scenario, int responseStatus) throws IOException {
        this.scenario = scenario;
        this.responseStatus = responseStatus;
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 4);
        server.createContext("/v1/test-stream", new Handler());
        server.setExecutor(executor);
        server.start();
    }

    String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/";
    }

    String lastRequestBody() {
        return lastRequestBody;
    }

    boolean awaitNoActiveRequests(long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (activeRequests.get() != 0 && System.currentTimeMillis() < deadline) {
            Thread.sleep(10L);
        }
        return activeRequests.get() == 0;
    }

    @Override public void close() {
        server.stop(0);
        executor.shutdownNow();
    }

    private final class Handler implements HttpHandler {
        @Override public void handle(HttpExchange exchange) throws IOException {
            activeRequests.incrementAndGet();
            try {
                lastRequestBody = readRequest(exchange.getRequestBody());
                String turnId = extractString(lastRequestBody, "turn_id");
                if (turnId.isEmpty()) turnId = "turn-1";
                if (responseStatus != 200) {
                    byte[] body = "synthetic error".getBytes(StandardCharsets.UTF_8);
                    exchange.sendResponseHeaders(responseStatus, body.length);
                    try (OutputStream output = exchange.getResponseBody()) {
                        output.write(body);
                    }
                    return;
                }
                Headers headers = exchange.getResponseHeaders();
                headers.set("Content-Type", "application/x-ndjson; charset=utf-8");
                headers.set("Connection", "close");
                exchange.sendResponseHeaders(200, 0);
                try (OutputStream output = exchange.getResponseBody()) {
                    emit(output, "{\"type\":\"started\",\"turn_id\":\"" + turnId
                            + "\",\"request_id\":\"request-1\"}");
                    if ("FAIL_BEFORE_CONTENT".equals(scenario)) {
                        emit(output, failed(turnId, "provider", "synthetic pre-content failure", false));
                    } else if ("EMPTY".equals(scenario)) {
                        emit(output, completed(turnId));
                    } else if ("FAIL_AFTER_PARTIAL".equals(scenario)) {
                        emit(output, delta(turnId, "partial "));
                        emit(output, delta(turnId, "content"));
                        emit(output, failed(turnId, "provider", "synthetic partial failure", false));
                    } else if ("DISCONNECT".equals(scenario)) {
                        emit(output, delta(turnId, "partial"));
                        return;
                    } else if ("MALFORMED".equals(scenario)) {
                        emit(output, "{\"type\":\"delta\",\"turn_id\":\"" + turnId
                                + "\",\"text\":\"bad\"} trailing");
                    } else if ("CANCEL".equals(scenario)) {
                        for (int i = 0; i < 100; i++) {
                            emit(output, delta(turnId, "slow-" + i + " "));
                            sleep(50L);
                        }
                        emit(output, completed(turnId));
                    } else {
                        if ("SLOW".equals(scenario)) sleep(80L);
                        emit(output, delta(turnId, "first "));
                        if ("SLOW".equals(scenario)) sleep(80L);
                        emit(output, delta(turnId, "second"));
                        emit(output, completed(turnId));
                        if ("DUPLICATE_TERMINAL".equals(scenario)) emit(output, completed(turnId));
                        if ("DELTA_AFTER_TERMINAL".equals(scenario)) emit(output, delta(turnId, " late"));
                        if ("FAIL_AFTER_TERMINAL".equals(scenario)) {
                            emit(output, failed(turnId, "provider", "late failure", false));
                        }
                    }
                }
            } catch (IOException clientDisconnected) {
                if (!"CANCEL".equals(scenario)) throw clientDisconnected;
            } finally {
                activeRequests.decrementAndGet();
            }
        }

        private String readRequest(InputStream input) throws IOException {
            byte[] buffer = new byte[8192];
            StringBuilder result = new StringBuilder();
            int read;
            while ((read = input.read(buffer)) != -1) result.append(new String(buffer, 0, read, StandardCharsets.UTF_8));
            return result.toString();
        }

        private void emit(OutputStream output, String frame) throws IOException {
            output.write((frame + "\n").getBytes(StandardCharsets.UTF_8));
            output.flush();
        }

        private String delta(String turnId, String text) {
            return "{\"type\":\"delta\",\"turn_id\":\"" + turnId + "\",\"text\":\"" + text + "\"}";
        }

        private String completed(String turnId) {
            return "{\"type\":\"completed\",\"turn_id\":\"" + turnId + "\"}";
        }

        private String failed(String turnId, String category, String message, boolean retryable) {
            return "{\"type\":\"failed\",\"turn_id\":\"" + turnId + "\",\"category\":\""
                    + category + "\",\"message\":\"" + message + "\",\"retryable\":"
                    + retryable + "}";
        }

        private String extractString(String json, String key) {
            String marker = "\"" + key + "\":\"";
            int start = json.indexOf(marker);
            if (start < 0) return "";
            start += marker.length();
            int end = json.indexOf('"', start);
            return end < 0 ? "" : json.substring(start, end);
        }

        private void sleep(long millis) throws IOException {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new IOException("test server interrupted", interrupted);
            }
        }
    }
}
