package com.example.androidfeasibility;

import java.io.File;
import java.util.Collections;

public final class PerformanceLifecycleTest {
    public static void main(String[] args) throws Exception {
        measureConversationCopies();
        measureLongStreamAndAttachments();
        testProcessRestoreRepairsStreamingState();
        testOfflineStartAndCancelledSyncWorker();
        System.out.println("PERFORMANCE LIFECYCLE TESTS PASSED");
    }

    private static void measureConversationCopies() {
        int[] sizes = new int[]{0, 10, 100, 1000};
        for (int size : sizes) {
            Conversation conversation = conversation(size);
            long start = System.nanoTime();
            for (int i = 0; i < 5; i++) conversation.copy();
            long elapsed = System.nanoTime() - start;
            double milliseconds = elapsed / 1_000_000.0;
            System.out.println("BENCH copy_messages=" + size + " copies=5 ms=" + milliseconds);
            check(milliseconds < 5_000.0, "representative conversation copy exceeded 5 seconds");
        }
    }

    private static void measureLongStreamAndAttachments() {
        Message message = new Message("long", "conversation", "turn", Role.ASSISTANT,
                "", MessageStatus.STREAMING, "local", "model", 1L);
        long start = System.nanoTime();
        for (int i = 0; i < 10_000; i++) message.appendText("chunk-");
        double milliseconds = (System.nanoTime() - start) / 1_000_000.0;
        System.out.println("BENCH long_stream_chunks=10000 ms=" + milliseconds);
        check(message.content.length() == 60_000, "long stream must retain every chunk");
        check(milliseconds < 5_000.0, "long stream append exceeded 5 seconds");
        Conversation conversation = new Conversation("attachments", "Attachments", 1L);
        for (int i = 0; i < 100; i++) {
            conversation.addAttachment(new Attachment("attachment-" + i, conversation.id, "message",
                    "file-" + i + ".txt", "text/plain", "text/plain", i + 1L,
                    "local-ref-" + i, AttachmentState.READY, i, null));
        }
        check(conversation.copy().attachments.size() == 100, "attachment dataset must copy completely");
    }

    private static void testProcessRestoreRepairsStreamingState() throws Exception {
        InMemoryConversationRepository repository = new InMemoryConversationRepository();
        Conversation conversation = new Conversation("restore", "Restore", 1L);
        conversation.add(new Message("streaming", conversation.id, "turn", Role.ASSISTANT,
                "partial", MessageStatus.STREAMING, "local", "model", 1L));
        repository.save(conversation);
        ConversationCoordinator coordinator = new ConversationCoordinator(repository,
                new ProviderAdapter() {
                    @Override public StreamHandle start(ProviderRequest request, Listener listener) {
                        return new StreamHandle() { @Override public void cancel() { } };
                    }
                }, null);
        check(coordinator.restore().messages.get(0).status == MessageStatus.FAILED,
                "restore must repair interrupted streaming state");
    }

    private static void testOfflineStartAndCancelledSyncWorker() throws Exception {
        File file = new File(System.getProperty("java.io.tmpdir"), "mochi-performance-" + System.nanoTime());
        JsonSyncStore store = new JsonSyncStore(file);
        SyncClient.Transport transport = new SyncClient.Transport() {
            @Override public SyncClient.PushResult push(java.util.List<SyncOperation> operations) {
                throw new AssertionError("local-only sync must not call transport");
            }
            @Override public SyncClient.ChangeBatch changes(SyncCursor cursor) {
                throw new AssertionError("local-only sync must not call transport");
            }
        };
        SyncWorker offline = new SyncWorker(new SyncClient(Entitlement.LOCAL_ONLY), store, transport,
                new SyncWorker.BackoffPolicy(0L, 0L), null);
        check(offline.runOnce() == SyncStatus.OFFLINE, "offline start must be explicit");
        SyncWorker cancelled = new SyncWorker(new SyncClient(Entitlement.CLOUD_SYNC), store, transport,
                new SyncWorker.BackoffPolicy(0L, 0L), null);
        cancelled.cancel();
        check(cancelled.runUntilTerminal(2) == SyncStatus.CANCELLED,
                "cancelled lifecycle worker must not start transport work");
        if (file.exists()) file.delete();
    }

    private static Conversation conversation(int messages) {
        Conversation conversation = new Conversation("bench-" + messages, "Bench", 1L);
        for (int i = 0; i < messages; i++) {
            conversation.add(new Message("message-" + i, conversation.id, "turn-" + i,
                    i % 2 == 0 ? Role.USER : Role.ASSISTANT, "message content " + i,
                    MessageStatus.COMPLETED, "local", "model", i));
        }
        return conversation;
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
