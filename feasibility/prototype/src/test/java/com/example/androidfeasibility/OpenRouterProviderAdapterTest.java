package com.example.androidfeasibility;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class OpenRouterProviderAdapterTest {
    public static void main(String[] args) throws Exception {
        testIncrementalSseAndDone();
        testAuthenticationFailureIsNormalized();
        testMalformedSseIsProtocolFailure();
        testDisconnectIsNetworkFailure();
        testCancellationClosesTransport();
        testMissingCredentialFailsWithoutRequest();
        System.out.println("OPENROUTER ADAPTER TESTS PASSED");
    }

    private static void testIncrementalSseAndDone() throws Exception {
        try (LocalOpenRouterServer server = new LocalOpenRouterServer("NORMAL")) {
            InMemoryCredentialStore credentials = credentials();
            OpenRouterProviderAdapter adapter = new OpenRouterProviderAdapter(
                    new URL(server.baseUrl()), credentials);
            Events events = new Events();
            adapter.start(request(), events);
            check(events.terminal.await(2, TimeUnit.SECONDS), "SSE stream must terminate");
            check(events.events.size() == 4, "SSE stream must emit started, two deltas, completed");
            check(events.events.get(0).type == StreamEvent.Type.STARTED, "stream must start");
            check(events.events.get(1).text.equals("first "), "first delta must be preserved");
            check(events.events.get(2).text.equals("second"), "second delta must be preserved");
            check(events.events.get(3).type == StreamEvent.Type.COMPLETED, "DONE must complete");
            check(events.events.get(0).metadata.requestId.equals("gen-1"),
                    "request ID must be optional metadata");
            check(server.authorization().equals("Bearer sk-test-secret"),
                    "credential must be sent only as transport authorization");
            check(!server.requestBody().contains("sk-test-secret"),
                    "credential must not be sent in JSON body");
            adapter.shutdown();
        }
    }

    private static void testAuthenticationFailureIsNormalized() throws Exception {
        try (LocalOpenRouterServer server = new LocalOpenRouterServer("AUTH_FAILURE")) {
            OpenRouterProviderAdapter adapter = new OpenRouterProviderAdapter(
                    new URL(server.baseUrl()), credentials());
            Events events = new Events();
            adapter.start(request(), events);
            check(events.terminal.await(2, TimeUnit.SECONDS), "auth failure must terminate");
            check(events.last().type == StreamEvent.Type.FAILED, "auth failure must be failed");
            check(events.last().error.category == ProviderError.Category.AUTHENTICATION,
                    "401 must be authentication");
            check(!events.last().error.message.contains("sk-test-secret"),
                    "failure must not expose the key");
            adapter.shutdown();
        }
    }

    private static void testMalformedSseIsProtocolFailure() throws Exception {
        try (LocalOpenRouterServer server = new LocalOpenRouterServer("MALFORMED")) {
            OpenRouterProviderAdapter adapter = new OpenRouterProviderAdapter(
                    new URL(server.baseUrl()), credentials());
            Events events = new Events();
            adapter.start(request(), events);
            check(events.terminal.await(2, TimeUnit.SECONDS), "malformed stream must terminate");
            check(events.last().error.category == ProviderError.Category.PROTOCOL,
                    "malformed SSE must be protocol failure");
            adapter.shutdown();
        }
    }

    private static void testDisconnectIsNetworkFailure() throws Exception {
        try (LocalOpenRouterServer server = new LocalOpenRouterServer("DISCONNECT")) {
            OpenRouterProviderAdapter adapter = new OpenRouterProviderAdapter(
                    new URL(server.baseUrl()), credentials());
            Events events = new Events();
            adapter.start(request(), events);
            check(events.terminal.await(2, TimeUnit.SECONDS), "disconnect must terminate");
            check(events.last().error.category == ProviderError.Category.NETWORK,
                    "missing DONE must be network failure");
            adapter.shutdown();
        }
    }

    private static void testCancellationClosesTransport() throws Exception {
        try (LocalOpenRouterServer server = new LocalOpenRouterServer("SLOW")) {
            OpenRouterProviderAdapter adapter = new OpenRouterProviderAdapter(
                    new URL(server.baseUrl()), credentials());
            Events events = new Events();
            ProviderAdapter.StreamHandle handle = adapter.start(request(), events);
            check(events.started.await(2, TimeUnit.SECONDS), "slow stream must start");
            handle.cancel();
            handle.cancel();
            check(server.awaitNoActiveRequests(2_000L), "cancel must close server request");
            check(adapter.activeStreamCount() == 0, "cancel must stop active worker");
            check(events.terminal.getCount() == 1, "cancel must not invent a terminal event");
            adapter.shutdown();
        }
    }

    private static void testMissingCredentialFailsWithoutRequest() throws Exception {
        try (LocalOpenRouterServer server = new LocalOpenRouterServer("NORMAL")) {
            OpenRouterProviderAdapter adapter = new OpenRouterProviderAdapter(
                    new URL(server.baseUrl()), new InMemoryCredentialStore());
            Events events = new Events();
            adapter.start(request(), events);
            check(events.terminal.await(2, TimeUnit.SECONDS), "missing key must terminate");
            check(events.last().error.category == ProviderError.Category.AUTHENTICATION,
                    "missing key must be authentication failure");
            check(server.requestCount() == 0, "missing key must not make a network request");
            adapter.shutdown();
        }
    }

    private static InMemoryCredentialStore credentials() {
        InMemoryCredentialStore credentials = new InMemoryCredentialStore();
        credentials.put("openrouter", "sk-test-secret");
        return credentials;
    }

    private static ProviderRequest request() {
        return new ProviderRequest("conversation-1", "turn-1", "user-1", "assistant-1",
                "hello", "openrouter", "openai/gpt-4", null);
    }

    private static final class Events implements ProviderAdapter.Listener {
        final List<StreamEvent> events = new ArrayList<>();
        final CountDownLatch started = new CountDownLatch(1);
        final CountDownLatch terminal = new CountDownLatch(1);

        @Override public synchronized void onEvent(StreamEvent event) {
            events.add(event);
            if (event.type == StreamEvent.Type.STARTED) started.countDown();
            if (event.type == StreamEvent.Type.COMPLETED
                    || event.type == StreamEvent.Type.FAILED
                    || event.type == StreamEvent.Type.CANCELLED) terminal.countDown();
        }

        synchronized StreamEvent last() { return events.get(events.size() - 1); }
    }

    private static final class LocalOpenRouterServer implements AutoCloseable {
        private final String scenario;
        private final HttpServer server;
        private final ExecutorService executor = Executors.newCachedThreadPool();
        private final AtomicInteger active = new AtomicInteger();
        private final AtomicInteger requests = new AtomicInteger();
        private volatile String authorization = "";
        private volatile String requestBody = "";

        LocalOpenRouterServer(String scenario) throws IOException {
            this.scenario = scenario;
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 4);
            server.createContext("/api/v1/chat/completions", new Handler());
            server.setExecutor(executor);
            server.start();
        }

        String baseUrl() { return "http://127.0.0.1:" + server.getAddress().getPort() + "/"; }
        String authorization() { return authorization; }
        String requestBody() { return requestBody; }
        int requestCount() { return requests.get(); }

        boolean awaitNoActiveRequests(long timeoutMs) throws InterruptedException {
            long deadline = System.currentTimeMillis() + timeoutMs;
            while (active.get() != 0 && System.currentTimeMillis() < deadline) Thread.sleep(10L);
            return active.get() == 0;
        }

        @Override public void close() {
            server.stop(0);
            executor.shutdownNow();
        }

        private final class Handler implements HttpHandler {
            @Override public void handle(HttpExchange exchange) throws IOException {
                active.incrementAndGet();
                requests.incrementAndGet();
                authorization = exchange.getRequestHeaders().getFirst("Authorization");
                requestBody = read(exchange.getRequestBody());
                try {
                    if ("AUTH_FAILURE".equals(scenario)) {
                        byte[] body = "{\"error\":{\"message\":\"invalid key\"}}"
                                .getBytes(StandardCharsets.UTF_8);
                        exchange.sendResponseHeaders(401, body.length);
                        try (OutputStream output = exchange.getResponseBody()) { output.write(body); }
                        return;
                    }
                    exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
                    exchange.getResponseHeaders().set("Connection", "close");
                    exchange.sendResponseHeaders(200, 0);
                    try (OutputStream output = exchange.getResponseBody()) {
                        emit(output, data("gen-1", "first ", "null"));
                        if ("MALFORMED".equals(scenario)) {
                            emit(output, "data: {not-json}");
                            return;
                        }
                        if ("DISCONNECT".equals(scenario)) {
                            emit(output, data("gen-1", "partial", "null"));
                            return;
                        }
                        if ("SLOW".equals(scenario)) {
                            for (int i = 0; i < 100; i++) {
                                emit(output, data("gen-1", "slow-" + i + " ", "null"));
                                sleep(40L);
                            }
                        } else {
                            emit(output, data("gen-1", "second", "null"));
                            emit(output, data("gen-1", "", "stop"));
                            emit(output, "data: [DONE]");
                        }
                    }
                } catch (IOException disconnected) {
                    if (!"SLOW".equals(scenario)) throw disconnected;
                } finally {
                    active.decrementAndGet();
                }
            }

            private String read(InputStream input) throws IOException {
                byte[] buffer = new byte[4096];
                StringBuilder result = new StringBuilder();
                int read;
                while ((read = input.read(buffer)) != -1) {
                    result.append(new String(buffer, 0, read, StandardCharsets.UTF_8));
                }
                return result.toString();
            }

            private void emit(OutputStream output, String line) throws IOException {
                output.write((line + "\n\n").getBytes(StandardCharsets.UTF_8));
                output.flush();
            }

            private String data(String id, String content, String finishReason) {
                return "data: {\"id\":\"" + id + "\",\"model\":\"openai/gpt-4\","
                        + "\"choices\":[{\"delta\":{\"content\":\"" + content
                        + "\"},\"finish_reason\":"
                        + ("null".equals(finishReason) ? "null" : "\"" + finishReason + "\"") + "}]}";
            }

            private void sleep(long millis) throws IOException {
                try { Thread.sleep(millis); }
                catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new IOException("server interrupted", interrupted);
                }
            }
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
