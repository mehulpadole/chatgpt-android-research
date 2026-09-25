package com.example.androidfeasibility;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** One semantic suite run against both provider implementations. */
public final class ProviderConformanceTest {
    private interface ProviderFactory {
        Harness create(String scenario) throws Exception;
    }

    private static final class Harness implements AutoCloseable {
        final ProviderAdapter provider;
        final MockProvider mock;
        final HttpStreamingProviderAdapter http;
        final LocalNdjsonTestServer server;

        Harness(ProviderAdapter provider, MockProvider mock,
                HttpStreamingProviderAdapter http, LocalNdjsonTestServer server) {
            this.provider = provider;
            this.mock = mock;
            this.http = http;
            this.server = server;
        }

        @Override public void close() {
            if (mock != null) mock.shutdown();
            if (http != null) http.shutdown();
            if (server != null) server.close();
        }
    }

    private static final class RecordingListener implements ConversationCoordinator.Listener {
        final List<Conversation> snapshots = new ArrayList<>();
        final CountDownLatch terminal = new CountDownLatch(1);

        @Override public synchronized void onChanged(Conversation conversation,
                                                      TurnState state, String error) {
            snapshots.add(conversation);
            if (state == TurnState.COMPLETED || state == TurnState.FAILED
                    || state == TurnState.CANCELLED) terminal.countDown();
        }

        synchronized Conversation latest() {
            return snapshots.get(snapshots.size() - 1);
        }
    }

    public static void main(String[] args) throws Exception {
        ProviderFactory mock = new ProviderFactory() {
            @Override public Harness create(String scenario) {
                MockScenario mockScenario = "CANCEL".equals(scenario)
                        ? MockScenario.SLOW : MockScenario.valueOf(scenario);
                MockProvider provider = new MockProvider(mockScenario);
                return new Harness(provider, provider, null, null);
            }
        };
        ProviderFactory http = new ProviderFactory() {
            @Override public Harness create(String scenario) throws Exception {
                LocalNdjsonTestServer server = new LocalNdjsonTestServer(scenario);
                HttpStreamingProviderAdapter provider = new HttpStreamingProviderAdapter(
                        new URL(server.baseUrl()), scenario);
                return new Harness(provider, null, provider, server);
            }
        };
        runSuite("mock", mock);
        runSuite("http", http);
        System.out.println("PROVIDER CONFORMANCE TESTS PASSED: mock + http");
    }

    private static void runSuite(String name, ProviderFactory factory) throws Exception {
        testSuccessfulStreaming(name, factory);
        testEmptyCompletion(name, factory);
        testCancellation(name, factory);
        testCancellationAfterCompletion(name, factory);
        testFailures(name, factory);
        testTerminalIdempotence(name, factory);
        testStableIdsPersistenceAndRestoration(name, factory);
        testInterruptedTurnPolicy(name, factory);
    }

    private static void testSuccessfulStreaming(String name, ProviderFactory factory) throws Exception {
        try (Harness harness = factory.create("NORMAL")) {
            RecordingListener listener = new RecordingListener();
            ConversationRepository repository = new InMemoryConversationRepository();
            ConversationCoordinator coordinator = coordinator(repository, harness.provider, listener);
            String turn = coordinator.startTurn("successful " + name);
            await(listener, "successful stream");
            Message assistant = coordinator.snapshot().messages.get(1);
            check(assistant.turnId.equals(turn), name + ": assistant must keep turn ID");
            check(assistant.status == MessageStatus.COMPLETED, name + ": stream must complete");
            check(!assistant.content.isEmpty(), name + ": stream must produce content");
            check(grew(listener), name + ": content must arrive incrementally");
        }
    }

    private static void testEmptyCompletion(String name, ProviderFactory factory) throws Exception {
        try (Harness harness = factory.create("EMPTY")) {
            RecordingListener listener = new RecordingListener();
            ConversationCoordinator coordinator = coordinator(new InMemoryConversationRepository(),
                    harness.provider, listener);
            coordinator.startTurn("empty " + name);
            await(listener, "empty stream");
            Message assistant = coordinator.snapshot().messages.get(1);
            check(assistant.status == MessageStatus.COMPLETED, name + ": empty stream must complete");
            check(assistant.content.isEmpty(), name + ": empty stream must remain empty");
        }
    }

    private static void testCancellation(String name, ProviderFactory factory) throws Exception {
        try (Harness harness = factory.create("CANCEL")) {
            RecordingListener listener = new RecordingListener();
            ConversationCoordinator coordinator = coordinator(new InMemoryConversationRepository(),
                    harness.provider, listener);
            String turn = coordinator.startTurn("cancel " + name);
            waitForActive(coordinator);
            coordinator.cancel(turn);
            coordinator.cancel(turn);
            String content = coordinator.snapshot().messages.get(1).content;
            Thread.sleep(180L);
            Message assistant = coordinator.snapshot().messages.get(1);
            check(assistant.status == MessageStatus.CANCELLED, name + ": cancel must be terminal");
            check(assistant.content.equals(content), name + ": late content after cancel must be ignored");
        }
    }

    private static void testCancellationAfterCompletion(String name, ProviderFactory factory)
            throws Exception {
        try (Harness harness = factory.create("NORMAL")) {
            RecordingListener listener = new RecordingListener();
            ConversationCoordinator coordinator = coordinator(new InMemoryConversationRepository(),
                    harness.provider, listener);
            String turn = coordinator.startTurn("cancel after complete " + name);
            await(listener, "completion before cancellation");
            coordinator.cancel(turn);
            check(coordinator.snapshot().messages.get(1).status == MessageStatus.COMPLETED,
                    name + ": cancellation after completion must be ignored");
        }
    }

    private static void testFailures(String name, ProviderFactory factory) throws Exception {
        try (Harness harness = factory.create("FAIL_BEFORE_CONTENT")) {
            RecordingListener listener = new RecordingListener();
            ConversationCoordinator coordinator = coordinator(new InMemoryConversationRepository(),
                    harness.provider, listener);
            coordinator.startTurn("failure before " + name);
            await(listener, "pre-content failure");
            Message assistant = coordinator.snapshot().messages.get(1);
            check(assistant.status == MessageStatus.FAILED, name + ": pre-content failure must persist");
            check(assistant.failureCategory.equals(ProviderError.Category.PROVIDER.name()),
                    name + ": failure category must persist");
        }
        try (Harness harness = factory.create("FAIL_AFTER_PARTIAL")) {
            RecordingListener listener = new RecordingListener();
            ConversationCoordinator coordinator = coordinator(new InMemoryConversationRepository(),
                    harness.provider, listener);
            coordinator.startTurn("failure after " + name);
            await(listener, "partial failure");
            Message assistant = coordinator.snapshot().messages.get(1);
            check(assistant.status == MessageStatus.FAILED, name + ": partial failure must persist");
            check(!assistant.content.isEmpty(), name + ": partial content must survive failure");
        }
    }

    private static void testTerminalIdempotence(String name, ProviderFactory factory) throws Exception {
        assertCompletedAndStable(name, factory, "DUPLICATE_TERMINAL");
        assertCompletedAndStable(name, factory, "DELTA_AFTER_TERMINAL");
        assertCompletedAndStable(name, factory, "FAIL_AFTER_TERMINAL");
    }

    private static void assertCompletedAndStable(String name, ProviderFactory factory, String scenario)
            throws Exception {
        try (Harness harness = factory.create(scenario)) {
            RecordingListener listener = new RecordingListener();
            ConversationCoordinator coordinator = coordinator(new InMemoryConversationRepository(),
                    harness.provider, listener);
            coordinator.startTurn(scenario + " " + name);
            await(listener, scenario);
            Thread.sleep(120L);
            Message assistant = coordinator.snapshot().messages.get(1);
            String content = assistant.content;
            check(assistant.status == MessageStatus.COMPLETED,
                    name + ": " + scenario + " must remain completed");
            Thread.sleep(120L);
            check(coordinator.snapshot().messages.get(1).content.equals(content),
                    name + ": " + scenario + " must ignore late events");
        }
    }

    private static void testStableIdsPersistenceAndRestoration(String name,
                                                                ProviderFactory factory) throws Exception {
        try (Harness harness = factory.create("NORMAL")) {
            RecordingListener listener = new RecordingListener();
            InMemoryConversationRepository repository = new InMemoryConversationRepository();
            ConversationCoordinator coordinator = coordinator(repository, harness.provider, listener);
            coordinator.startTurn("persist " + name);
            await(listener, "persistence stream");
            Conversation saved = repository.load();
            check(saved.messages.size() == 2, name + ": persistence must not duplicate messages");
            String userId = saved.messages.get(0).id;
            String assistantId = saved.messages.get(1).id;
            MockProvider restoredProvider = new MockProvider();
            ConversationCoordinator restored = coordinator(repository, restoredProvider, listener);
            Conversation reloaded = restored.restore();
            check(reloaded.messages.get(0).id.equals(userId), name + ": user ID must restore");
            check(reloaded.messages.get(1).id.equals(assistantId), name + ": assistant ID must restore");
            check(reloaded.messages.get(1).status == MessageStatus.COMPLETED,
                    name + ": completed turn must not restart on restore");
            restoredProvider.shutdown();
        }
    }

    private static void testInterruptedTurnPolicy(String name, ProviderFactory factory) throws Exception {
        try (Harness harness = factory.create("SLOW")) {
            RecordingListener listener = new RecordingListener();
            InMemoryConversationRepository repository = new InMemoryConversationRepository();
            ConversationCoordinator coordinator = coordinator(repository, harness.provider, listener);
            coordinator.startTurn("interrupt " + name);
            MockProvider restoredProvider = new MockProvider();
            ConversationCoordinator restored = coordinator(repository, restoredProvider, listener);
            Conversation snapshot = restored.restore();
            check(snapshot.messages.get(1).status == MessageStatus.FAILED,
                    name + ": interrupted stream must become failed");
            check(snapshot.messages.get(1).failureMessage.equals("interrupted by process termination"),
                    name + ": interrupted failure message must be explicit");
            restoredProvider.shutdown();
        }
    }

    private static ConversationCoordinator coordinator(ConversationRepository repository,
                                                       ProviderAdapter provider,
                                                       RecordingListener listener) {
        return new ConversationCoordinator(repository, provider, listener);
    }

    private static void await(RecordingListener listener, String behavior) throws Exception {
        check(listener.terminal.await(5, TimeUnit.SECONDS), behavior + " timed out");
    }

    private static void waitForActive(ConversationCoordinator coordinator) throws Exception {
        long deadline = System.currentTimeMillis() + 2_000L;
        while (coordinator.state() == TurnState.IDLE && System.currentTimeMillis() < deadline) {
            Thread.sleep(10L);
        }
        check(coordinator.state() == TurnState.STARTING || coordinator.state() == TurnState.STREAMING,
                "provider did not become active");
    }

    private static boolean grew(RecordingListener listener) {
        int previous = -1;
        int changes = 0;
        for (Conversation snapshot : listener.snapshots) {
            int length = snapshot.messages.size() < 2 ? 0 : snapshot.messages.get(1).content.length();
            if (length > previous && length > 0) changes++;
            previous = length;
        }
        return changes >= 2;
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
