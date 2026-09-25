package com.example.androidfeasibility;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public final class HttpStreamingProviderAdapterTest {
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

        synchronized StreamEvent last() {
            return events.get(events.size() - 1);
        }
    }

    public static void main(String[] args) throws Exception {
        testRequestEncodingAndNormalStream();
        testFailureFrameMapping();
        testMalformedFrameBecomesProtocolFailure();
        testDisconnectBecomesNetworkFailure();
        testCancellationClosesWorker();
        testHttpStatusMapping();
        System.out.println("HTTP ADAPTER TESTS PASSED");
    }

    private static void testRequestEncodingAndNormalStream() throws Exception {
        try (LocalNdjsonTestServer server = new LocalNdjsonTestServer("NORMAL")) {
            HttpStreamingProviderAdapter adapter = new HttpStreamingProviderAdapter(
                    new URL(server.baseUrl()), "NORMAL");
            Events events = new Events();
            adapter.start(request(), events);
            check(events.terminal.await(2, TimeUnit.SECONDS), "normal stream must terminate");
            check(events.events.size() == 4, "normal stream must have start, two deltas, complete");
            check(events.events.get(1).text.equals("first "), "first delta must be incremental");
            check(events.events.get(2).text.equals("second"), "second delta must be incremental");
            check(events.last().type == StreamEvent.Type.COMPLETED, "normal stream must complete");
            check(server.lastRequestBody().contains("\"conversation_id\":\"conversation-1\""),
                    "wire request must contain canonical conversation ID");
            check(server.lastRequestBody().contains("\"assistant_message_id\":\"assistant-1\""),
                    "wire request must contain canonical assistant ID");
            adapter.shutdown();
        }
    }

    private static void testFailureFrameMapping() throws Exception {
        try (LocalNdjsonTestServer server = new LocalNdjsonTestServer("FAIL_AFTER_PARTIAL")) {
            HttpStreamingProviderAdapter adapter = new HttpStreamingProviderAdapter(
                    new URL(server.baseUrl()), "FAIL_AFTER_PARTIAL");
            Events events = new Events();
            adapter.start(request(), events);
            check(events.terminal.await(2, TimeUnit.SECONDS), "failure stream must terminate");
            StreamEvent failure = events.last();
            check(failure.type == StreamEvent.Type.FAILED, "failure frame must become FAILED");
            check(failure.error.category == ProviderError.Category.PROVIDER,
                    "failure category must remain provider-neutral");
            check(failure.error.message.equals("synthetic partial failure"),
                    "failure message must be preserved");
            adapter.shutdown();
        }
    }

    private static void testMalformedFrameBecomesProtocolFailure() throws Exception {
        try (LocalNdjsonTestServer server = new LocalNdjsonTestServer("MALFORMED")) {
            HttpStreamingProviderAdapter adapter = new HttpStreamingProviderAdapter(
                    new URL(server.baseUrl()), "MALFORMED");
            Events events = new Events();
            adapter.start(request(), events);
            check(events.terminal.await(2, TimeUnit.SECONDS), "malformed stream must terminate");
            check(events.last().type == StreamEvent.Type.FAILED, "malformed frame must fail");
            check(events.last().error.category == ProviderError.Category.PROTOCOL,
                    "malformed frame must be a protocol failure");
            adapter.shutdown();
        }
    }

    private static void testDisconnectBecomesNetworkFailure() throws Exception {
        try (LocalNdjsonTestServer server = new LocalNdjsonTestServer("DISCONNECT")) {
            HttpStreamingProviderAdapter adapter = new HttpStreamingProviderAdapter(
                    new URL(server.baseUrl()), "DISCONNECT");
            Events events = new Events();
            adapter.start(request(), events);
            check(events.terminal.await(2, TimeUnit.SECONDS), "disconnect must terminate");
            check(events.last().type == StreamEvent.Type.FAILED, "disconnect must fail");
            check(events.last().error.category == ProviderError.Category.NETWORK,
                    "unexpected EOF must be a network failure");
            adapter.shutdown();
        }
    }

    private static void testCancellationClosesWorker() throws Exception {
        try (LocalNdjsonTestServer server = new LocalNdjsonTestServer("CANCEL")) {
            HttpStreamingProviderAdapter adapter = new HttpStreamingProviderAdapter(
                    new URL(server.baseUrl()), "CANCEL");
            Events events = new Events();
            ProviderAdapter.StreamHandle handle = adapter.start(request(), events);
            check(events.started.await(2, TimeUnit.SECONDS), "cancel scenario must start");
            handle.cancel();
            handle.cancel();
            check(server.awaitNoActiveRequests(2_000L), "cancellation must close the server request");
            check(adapter.activeStreamCount() == 0, "cancellation must stop the HTTP worker");
            check(events.terminal.getCount() == 1, "cancelled transport must not invent a terminal event");
            adapter.shutdown();
        }
    }

    private static void testHttpStatusMapping() throws Exception {
        try (LocalNdjsonTestServer server = new LocalNdjsonTestServer("NORMAL", 429)) {
            HttpStreamingProviderAdapter adapter = new HttpStreamingProviderAdapter(
                    new URL(server.baseUrl()), "NORMAL");
            Events events = new Events();
            adapter.start(request(), events);
            check(events.terminal.await(2, TimeUnit.SECONDS), "HTTP error must terminate");
            check(events.last().error.category == ProviderError.Category.RATE_LIMIT,
                    "HTTP 429 must map to rate limit");
            adapter.shutdown();
        }
    }

    private static ProviderRequest request() {
        return new ProviderRequest("conversation-1", "turn-1", "user-1", "assistant-1",
                "hello synthetic", "test-http", "ndjson-test", null);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
