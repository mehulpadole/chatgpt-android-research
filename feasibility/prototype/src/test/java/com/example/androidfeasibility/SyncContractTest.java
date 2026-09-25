package com.example.androidfeasibility;

public final class SyncContractTest {
    public static void main(String[] args) {
        SyncEntity entity = new SyncEntity("conversation-1", "conversation", "{\"title\":\"hello\"}",
                10L, 0L, "device-a", 3L);
        check(!entity.deleted(), "live entity must not be tombstone");
        SyncOperation operation = new SyncOperation(entity.entityId, entity.entityType,
                SyncOperation.Action.UPSERT, entity.payload, 10L);
        check(operation.idempotencyKey.equals(operation.operationId), "operation must be idempotent");
        boolean rejected = false;
        try { new SyncOperation("c", "conversation", SyncOperation.Action.UPSERT,
                "{\"api_key\":\"sk-long-secret-value\"}", 1L); }
        catch (IllegalArgumentException expected) { rejected = true; }
        check(rejected, "provider credentials must be rejected from sync payloads");
        check(new SyncCursor(-2L).revision == 0L, "cursor must not be negative");
        check(Entitlement.LOCAL_ONLY != Entitlement.CLOUD_SYNC, "entitlement must be explicit");
        System.out.println("SYNC CONTRACT TESTS PASSED");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
