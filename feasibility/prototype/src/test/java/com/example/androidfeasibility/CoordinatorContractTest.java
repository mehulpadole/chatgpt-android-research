package com.example.androidfeasibility;

import java.util.ArrayList;
import java.util.List;

public final class CoordinatorContractTest {
    private static final class ManualProvider implements ProviderAdapter {
        ProviderRequest request;
        Listener listener;
        int cancelCount;

        @Override public StreamHandle start(ProviderRequest request, Listener listener) {
            this.request = request;
            this.listener = listener;
            return new StreamHandle() {
                private boolean cancelled;

                @Override public void cancel() {
                    if (!cancelled) {
                        cancelled = true;
                        cancelCount++;
                    }
                }
            };
        }

        void emit(StreamEvent event) {
            listener.onEvent(event);
        }
    }

    private static final class CountingRepository implements ConversationRepository {
        final List<Conversation> writes = new ArrayList<>();

        @Override public synchronized void save(Conversation conversation) {
            writes.add(conversation.copy());
        }

        @Override public synchronized Conversation load() {
            return writes.isEmpty() ? null : writes.get(writes.size() - 1).copy();
        }

        @Override public synchronized void clear() {
            writes.clear();
        }
    }

    private static final ConversationCoordinator.Listener NOOP =
            new ConversationCoordinator.Listener() {
                @Override public void onChanged(Conversation conversation, TurnState state, String error) {}
            };

    public static void main(String[] args) throws Exception {
        testStartTurnBuildsCanonicalProviderRequest();
        testTerminalStateIgnoresLateEvents();
        testCancellationIsIdempotentAndScopedToTurn();
        testMeaningfulDeltaCheckpointAndTerminalFlush();
        testRestoreConvertsStreamingToInterruptedFailure();
        System.out.println("COORDINATOR CONTRACT TESTS PASSED");
    }

    private static void testStartTurnBuildsCanonicalProviderRequest() throws Exception {
        InMemoryConversationRepository repository = new InMemoryConversationRepository();
        ManualProvider provider = new ManualProvider();
        ConversationCoordinator coordinator = new ConversationCoordinator(repository, provider, NOOP,
                new ProviderConfiguration("test-provider", "test-model"));
        coordinator.startTurn("hello");
        Conversation snapshot = coordinator.snapshot();
        check(provider.request.conversationId.equals(snapshot.id), "request conversation ID must be canonical");
        check(provider.request.turnId.equals(snapshot.messages.get(0).turnId), "request turn ID must be canonical");
        check(provider.request.userMessageId.equals(snapshot.messages.get(0).id), "request user ID must be canonical");
        check(provider.request.assistantMessageId.equals(snapshot.messages.get(1).id), "request assistant ID must be canonical");
        check(provider.request.providerId.equals("test-provider"), "provider configuration must be copied");
        check(provider.request.modelId.equals("test-model"), "model configuration must be copied");
    }

    private static void testTerminalStateIgnoresLateEvents() throws Exception {
        InMemoryConversationRepository repository = new InMemoryConversationRepository();
        ManualProvider provider = new ManualProvider();
        ConversationCoordinator coordinator = new ConversationCoordinator(repository, provider, NOOP);
        String turn = coordinator.startTurn("partial");
        provider.emit(StreamEvent.started(turn));
        provider.emit(StreamEvent.delta(turn, "partial content"));
        provider.emit(StreamEvent.failed(turn, new ProviderError(
                ProviderError.Category.PROVIDER, "synthetic failure", false)));
        provider.emit(StreamEvent.delta(turn, " late"));
        provider.emit(StreamEvent.completed(turn));
        provider.emit(StreamEvent.failed(turn, new ProviderError(
                ProviderError.Category.NETWORK, "late failure", true)));
        Message assistant = coordinator.snapshot().messages.get(1);
        check(assistant.status == MessageStatus.FAILED, "failed turn must remain failed");
        check(assistant.content.equals("partial content"), "late delta must not mutate content");
        check(assistant.failureCategory.equals(ProviderError.Category.PROVIDER.name()),
                "terminal failure category must persist in memory");
        check(assistant.failureMessage.equals("synthetic failure"),
                "terminal failure message must persist in memory");
    }

    private static void testCancellationIsIdempotentAndScopedToTurn() throws Exception {
        InMemoryConversationRepository repository = new InMemoryConversationRepository();
        ManualProvider provider = new ManualProvider();
        ConversationCoordinator coordinator = new ConversationCoordinator(repository, provider, NOOP);
        String turn = coordinator.startTurn("cancel");
        coordinator.cancel(turn);
        coordinator.cancel(turn);
        Message assistant = coordinator.snapshot().messages.get(1);
        check(provider.cancelCount == 1, "cancellation must call the provider once");
        check(assistant.status == MessageStatus.CANCELLED, "cancellation must persist cancelled state");
        provider.emit(StreamEvent.delta(turn, "late"));
        check(assistant.content.isEmpty(), "late content after cancellation must be ignored");
    }

    private static void testMeaningfulDeltaCheckpointAndTerminalFlush() throws Exception {
        CountingRepository repository = new CountingRepository();
        ManualProvider provider = new ManualProvider();
        ConversationCoordinator coordinator = new ConversationCoordinator(repository, provider, NOOP);
        String turn = coordinator.startTurn("checkpoint");
        int initialWrites = repository.writes.size();
        provider.emit(StreamEvent.started(turn));
        provider.emit(StreamEvent.delta(turn, "0123456789abcdef"));
        check(repository.writes.size() > initialWrites, "meaningful delta must create a checkpoint");
        check(repository.load().messages.get(1).content.equals("0123456789abcdef"),
                "checkpoint must retain partial content");
        provider.emit(StreamEvent.delta(turn, "tail"));
        provider.emit(StreamEvent.completed(turn));
        check(repository.load().messages.get(1).status == MessageStatus.COMPLETED,
                "terminal completion must flush the final snapshot");
        check(repository.load().messages.get(1).content.equals("0123456789abcdef" + "tail"),
                "terminal flush must retain all content");
    }

    private static void testRestoreConvertsStreamingToInterruptedFailure() throws Exception {
        InMemoryConversationRepository repository = new InMemoryConversationRepository();
        ManualProvider provider = new ManualProvider();
        ConversationCoordinator coordinator = new ConversationCoordinator(repository, provider, NOOP);
        coordinator.startTurn("process death");
        ConversationCoordinator restored = new ConversationCoordinator(repository,
                new ManualProvider(), NOOP);
        Conversation snapshot = restored.restore();
        Message assistant = snapshot.messages.get(1);
        check(assistant.status == MessageStatus.FAILED, "streaming message must restore as failed");
        check(assistant.failureMessage.equals("interrupted by process termination"),
                "interrupted policy must be explicit");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
