package com.example.androidfeasibility;

import java.io.File;

public final class SyncOutboxTest {
    public static void main(String[] args) throws Exception {
        File file = new File(System.getProperty("java.io.tmpdir"), "mochi-sync-" + System.nanoTime() + ".properties");
        SyncOperation operation = new SyncOperation("conversation-1", "conversation",
                SyncOperation.Action.UPSERT, "{\"title\":\"offline\"}", 1L);
        JsonSyncStore first = new JsonSyncStore(file);
        first.enqueue(operation);
        first.setCursor(new SyncCursor(4L));
        JsonSyncStore second = new JsonSyncStore(file);
        check(second.pending().size() == 1, "outbox must survive store recreation");
        check(second.cursor().revision == 4L, "cursor must survive store recreation");
        second.enqueue(operation);
        check(second.pending().size() == 1, "duplicate operation must be idempotent");
        second.markSucceeded(operation.operationId);
        check(second.pending().isEmpty(), "succeeded operation must leave pending queue");
        if (file.exists()) file.delete();
        System.out.println("SYNC OUTBOX TESTS PASSED");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
