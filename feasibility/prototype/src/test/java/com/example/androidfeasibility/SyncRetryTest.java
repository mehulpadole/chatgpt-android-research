package com.example.androidfeasibility;

import java.io.File;
import java.util.List;

public final class SyncRetryTest {
    public static void main(String[] args) throws Exception {
        File file = new File(System.getProperty("java.io.tmpdir"), "mochi-sync-retry-" + System.nanoTime() + ".properties");
        JsonSyncStore store = new JsonSyncStore(file);
        SyncOperation operation = new SyncOperation("conversation-1", "conversation",
                SyncOperation.Action.UPSERT, "{\"title\":\"retry\"}", 1L);
        store.enqueue(operation);
        FlakyTransport transport = new FlakyTransport();
        SyncClient client = new SyncClient(Entitlement.CLOUD_SYNC);
        check(client.sync(store, transport) == SyncStatus.FAILED, "transport failure must be visible");
        check(store.pending().size() == 1 && store.pending().get(0).attempts == 1,
                "failed operation must remain retryable");
        check(client.sync(store, transport) == SyncStatus.SYNCED, "retry must succeed");
        check(store.pending().isEmpty(), "successful retry must drain outbox");

        SyncOperation quotaOperation = new SyncOperation("conversation-2", "conversation",
                SyncOperation.Action.UPSERT, "quota", 2L);
        store.enqueue(quotaOperation);
        transport.quota = true;
        check(client.sync(store, transport) == SyncStatus.BLOCKED_QUOTA,
                "quota must block sync without deleting local operation history");

        JsonSyncStore localStore = new JsonSyncStore(new File(file.getPath() + ".local"));
        localStore.enqueue(new SyncOperation("conversation-3", "conversation",
                SyncOperation.Action.UPSERT, "local", 3L));
        check(new SyncClient(Entitlement.LOCAL_ONLY).sync(localStore, transport) == SyncStatus.OFFLINE,
                "local-only mode must not require cloud");
        check(localStore.pending().size() == 1, "local-only mode must preserve local outbox state");
        if (file.exists()) file.delete();
        File localFile = new File(file.getPath() + ".local");
        if (localFile.exists()) localFile.delete();
        System.out.println("SYNC RETRY TESTS PASSED");
    }

    private static final class FlakyTransport implements SyncClient.Transport {
        int attempts;
        boolean quota;

        @Override public SyncClient.PushResult push(List<SyncOperation> operations) {
            attempts++;
            if (quota) return new SyncClient.PushResult(false, true, "quota");
            if (attempts == 1) return new SyncClient.PushResult(false, false, "timeout");
            return new SyncClient.PushResult(true, false, "");
        }

        @Override public SyncClient.ChangeBatch changes(SyncCursor cursor) {
            return new SyncClient.ChangeBatch(new SyncCursor(cursor.revision + 1L), null);
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
