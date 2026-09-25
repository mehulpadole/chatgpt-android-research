package com.example.androidfeasibility;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public final class PrototypeCoreTest {
    private static final class ManualProvider implements ProviderAdapter {
        ProviderRequest request;
        Listener listener;
        boolean cancelled;

        @Override public StreamHandle start(ProviderRequest request, Listener listener) {
            this.request = request;
            this.listener = listener;
            return new StreamHandle() {
                @Override public void cancel() { cancelled = true; }
            };
        }

        void emit(StreamEvent event) { listener.onEvent(event); }
    }

    private static class RecordingListener implements ConversationCoordinator.Listener {
        final List<TurnState> states = new ArrayList<>();
        Conversation latest;
        String error = "";

        @Override public synchronized void onChanged(Conversation conversation, TurnState state, String error) {
            latest = conversation;
            states.add(state);
            this.error = error == null ? "" : error;
        }
    }

    public static void main(String[] args) throws Exception {
        testNormalIncrementalAndStableIds();
        testCancellationIgnoresLateEvents();
        testFailuresPreservePartialContent();
        testDuplicateTerminalAndAssociation();
        testPersistenceAndRestoration();
        testRealDeterministicMockProvider();
        System.out.println("ALL CORE TESTS PASSED");
    }

    private static ConversationCoordinator coordinator(InMemoryConversationRepository repo,
                                                       ProviderAdapter provider,
                                                       RecordingListener listener) {
        return new ConversationCoordinator(repo, provider, listener);
    }

    private static void testNormalIncrementalAndStableIds() throws Exception {
        InMemoryConversationRepository repo = new InMemoryConversationRepository();
        ManualProvider provider = new ManualProvider();
        RecordingListener listener = new RecordingListener();
        ConversationCoordinator coordinator = coordinator(repo, provider, listener);
        String turn = coordinator.startTurn("hello");
        provider.emit(StreamEvent.started(turn));
        provider.emit(StreamEvent.delta(turn, "first "));
        provider.emit(StreamEvent.delta(turn, "second"));
        Conversation beforeComplete = coordinator.snapshot();
        String assistantId = beforeComplete.messages.get(1).id;
        provider.emit(StreamEvent.completed(turn));
        Conversation result = coordinator.snapshot();
        check(result.messages.size() == 2, "normal turn must have exactly two messages");
        check(result.messages.get(1).id.equals(assistantId), "assistant ID must remain stable");
        check(result.messages.get(1).content.equals("first second"), "deltas must append in order");
        check(result.messages.get(1).status == MessageStatus.COMPLETED, "turn must complete");
    }

    private static void testCancellationIgnoresLateEvents() throws Exception {
        InMemoryConversationRepository repo = new InMemoryConversationRepository();
        ManualProvider provider = new ManualProvider();
        RecordingListener listener = new RecordingListener();
        ConversationCoordinator coordinator = coordinator(repo, provider, listener);
        String turn = coordinator.startTurn("cancel me");
        provider.emit(StreamEvent.started(turn));
        coordinator.cancelActive();
        provider.emit(StreamEvent.delta(turn, "late content"));
        provider.emit(StreamEvent.completed(turn));
        Message assistant = coordinator.snapshot().messages.get(1);
        check(provider.cancelled, "provider handle must be cancelled");
        check(assistant.status == MessageStatus.CANCELLED, "cancelled state must persist");
        check(assistant.content.isEmpty(), "late deltas must not mutate a cancelled turn");
    }

    private static void testFailuresPreservePartialContent() throws Exception {
        InMemoryConversationRepository repo = new InMemoryConversationRepository();
        ManualProvider provider = new ManualProvider();
        RecordingListener listener = new RecordingListener();
        ConversationCoordinator coordinator = coordinator(repo, provider, listener);
        String before = coordinator.startTurn("before");
        provider.emit(StreamEvent.started(before));
        provider.emit(StreamEvent.failed(before, new ProviderError(
                ProviderError.Category.PROVIDER, "before failure", false)));
        check(coordinator.snapshot().messages.get(1).status == MessageStatus.FAILED, "pre-content failure must be visible");
        check(coordinator.snapshot().messages.get(1).content.isEmpty(), "pre-content failure has no fabricated text");

        String after = coordinator.startTurn("after");
        provider.emit(StreamEvent.started(after));
        provider.emit(StreamEvent.delta(after, "partial"));
        provider.emit(StreamEvent.failed(after, new ProviderError(
                ProviderError.Category.PROVIDER, "after failure", false)));
        Message assistant = coordinator.snapshot().messages.get(3);
        check(assistant.status == MessageStatus.FAILED, "partial failure must be visible");
        check(assistant.content.equals("partial"), "partial content must be preserved");
        check(listener.error.equals("after failure"), "error text must reach the listener");
    }

    private static void testDuplicateTerminalAndAssociation() throws Exception {
        InMemoryConversationRepository repo = new InMemoryConversationRepository();
        ManualProvider provider = new ManualProvider();
        RecordingListener listener = new RecordingListener();
        ConversationCoordinator coordinator = coordinator(repo, provider, listener);
        String first = coordinator.startTurn("one");
        provider.emit(StreamEvent.started(first));
        provider.emit(StreamEvent.delta(first, "reply one"));
        provider.emit(StreamEvent.completed(first));
        provider.emit(StreamEvent.completed(first));
        String second = coordinator.startTurn("two");
        provider.emit(StreamEvent.started(second));
        provider.emit(StreamEvent.delta(second, "reply two"));
        provider.emit(StreamEvent.completed(second));
        Conversation result = coordinator.snapshot();
        check(result.messages.size() == 4, "two turns must create four messages without duplicates");
        check(!first.equals(second), "turn IDs must be unique");
        check(result.messages.get(1).turnId.equals(first), "first assistant must keep first turn ID");
        check(result.messages.get(3).turnId.equals(second), "second assistant must keep second turn ID");
    }

    private static void testPersistenceAndRestoration() throws Exception {
        InMemoryConversationRepository repo = new InMemoryConversationRepository();
        ManualProvider provider = new ManualProvider();
        RecordingListener listener = new RecordingListener();
        ConversationCoordinator coordinator = coordinator(repo, provider, listener);
        String turn = coordinator.startTurn("persist");
        provider.emit(StreamEvent.started(turn));
        provider.emit(StreamEvent.delta(turn, "saved reply"));
        provider.emit(StreamEvent.completed(turn));
        String id = coordinator.snapshot().messages.get(0).id;
        ConversationCoordinator restored = new ConversationCoordinator(repo, new ManualProvider(), listener);
        Conversation reloaded = restored.restore();
        check(reloaded.messages.size() == 2, "restored conversation must retain messages");
        check(reloaded.messages.get(0).id.equals(id), "restored IDs must be stable");
        check(reloaded.messages.get(1).status == MessageStatus.COMPLETED, "completed status must survive reload");
    }

    private static void testRealDeterministicMockProvider() throws Exception {
        InMemoryConversationRepository repo = new InMemoryConversationRepository();
        MockProvider provider = new MockProvider(MockScenario.NORMAL);
        final CountDownLatch done = new CountDownLatch(1);
        RecordingListener listener = new RecordingListener() {
            @Override public synchronized void onChanged(Conversation conversation, TurnState state, String error) {
                super.onChanged(conversation, state, error);
                if (state == TurnState.COMPLETED) done.countDown();
            }
        };
        ConversationCoordinator coordinator = new ConversationCoordinator(repo, provider, listener);
        coordinator.startTurn("mock");
        check(done.await(5, TimeUnit.SECONDS), "deterministic mock must complete");
        check(coordinator.snapshot().messages.get(1).content.contains("mock"), "mock must emit content");
        provider.shutdown();
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
