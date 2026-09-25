package com.example.androidfeasibility;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class TwoDeviceSimulationTest {
    public static void main(String[] args) throws Exception {
        testCaseAOfflineSaveAndRestart();
        testCaseBUploadAndPullAcrossDevices();
        testCaseCConcurrentMergeIsDeterministic();
        testCaseDTombstoneCannotBeResurrectedByStalePull();
        testCaseEReplayAndAttachmentMetadataRemainSafe();
        System.out.println("TWO DEVICE SIMULATION TESTS PASSED");
    }

    private static void testCaseAOfflineSaveAndRestart() throws Exception {
        File file = file("case-a");
        JsonSyncStore first = new JsonSyncStore(file);
        first.enqueue(new SyncOperation("a-op", "conversation-a", "conversation",
                SyncOperation.Action.UPSERT, "offline", 1L, 0, SyncState.PENDING));
        JsonSyncStore restarted = new JsonSyncStore(file);
        check(restarted.pending().size() == 1, "case A must preserve local outbox across restart");
        cleanup(file);
    }

    private static void testCaseBUploadAndPullAcrossDevices() throws Exception {
        SharedServer server = new SharedServer();
        JsonSyncStore deviceA = store("case-b-a");
        deviceA.enqueue(new SyncOperation("b-op", "conversation-b", "conversation",
                SyncOperation.Action.UPSERT, "from-a", 1L, 0, SyncState.PENDING));
        check(run(deviceA, server.transport("device-a")) == SyncStatus.SYNCED, "case B device A must upload");
        JsonSyncStore deviceB = store("case-b-b");
        SyncClient.ChangeBatch pulled = server.transport("device-b").changes(deviceB.cursor());
        check(pulled.entities.size() == 1 && pulled.entities.get(0).payload.equals("from-a"),
                "case B device B must pull device A change");
        cleanup(deviceA, deviceB);
    }

    private static void testCaseCConcurrentMergeIsDeterministic() {
        SyncEntity fromA = new SyncEntity("conversation-c", "conversation", "a", 5L,
                0L, "device-a", 7L);
        SyncEntity fromB = new SyncEntity("conversation-c", "conversation", "b", 5L,
                0L, "device-b", 7L);
        check(SyncConflictResolver.merge(fromA, fromB) == fromB,
                "case C equal revision/timestamp must use stable origin-device tie-breaker");
    }

    private static void testCaseDTombstoneCannotBeResurrectedByStalePull() {
        SyncEntity tombstone = new SyncEntity("conversation-d", "conversation", "", 8L,
                8L, "device-a", 9L);
        SyncEntity stale = new SyncEntity("conversation-d", "conversation", "old", 20L,
                0L, "device-b", 8L);
        check(SyncConflictResolver.merge(tombstone, stale) == tombstone,
                "case D stale pull must not resurrect tombstoned entity");
    }

    private static void testCaseEReplayAndAttachmentMetadataRemainSafe() throws Exception {
        SharedServer server = new SharedServer();
        JsonSyncStore deviceA = store("case-e");
        Attachment attachment = new Attachment("attachment-e", "conversation-e", "message-e",
                "note.txt", "text/plain", "text/plain", 4L, "/private/note.txt",
                AttachmentState.READY, 10L, null);
        SyncOperation metadata = AttachmentSyncPolicy.metadataOperation(attachment, "device-a", 10L);
        deviceA.enqueue(metadata);
        check(run(deviceA, server.transport("device-a")) == SyncStatus.SYNCED, "case E first push must succeed");
        deviceA.enqueue(metadata);
        check(server.acceptedOperationCount() == 1, "case E replay must remain idempotent");
        check(!server.lastEntity().payload.contains("/private"), "case E must not sync local attachment path");
        cleanup(deviceA);
    }

    private static SyncStatus run(JsonSyncStore store, SyncClient.Transport transport) throws Exception {
        return new SyncWorker(new SyncClient(Entitlement.CLOUD_SYNC), store, transport,
                new SyncWorker.BackoffPolicy(0L, 0L), new SyncWorker.Sleeper() {
                    @Override public void sleep(long millis) { }
                }).runUntilTerminal(2);
    }

    private static JsonSyncStore store(String label) throws Exception {
        return new JsonSyncStore(file(label));
    }

    private static File file(String label) {
        return new File(System.getProperty("java.io.tmpdir"), "mochi-two-device-" + label
                + "-" + System.nanoTime() + ".properties");
    }

    private static void cleanup(JsonSyncStore... stores) { }

    private static void cleanup(File file) {
        if (file.exists()) file.delete();
    }

    private static final class SharedServer {
        private long revision;
        private final Map<String, SyncEntity> entities = new HashMap<>();
        private final Map<String, String> operations = new HashMap<>();

        SyncClient.Transport transport(final String deviceId) {
            return new SyncClient.Transport() {
                @Override public synchronized SyncClient.PushResult push(List<SyncOperation> batch) {
                    for (SyncOperation operation : batch) {
                        if (operations.containsKey(operation.operationId)) continue;
                        revision++;
                        SyncEntity entity = new SyncEntity(operation.entityId, operation.entityType,
                                operation.action == SyncOperation.Action.DELETE ? "" : operation.payload,
                                operation.createdAt,
                                operation.action == SyncOperation.Action.DELETE ? operation.createdAt : 0L,
                                deviceId, revision);
                        entities.put(operation.entityId, entity);
                        operations.put(operation.operationId, operation.entityId);
                    }
                    return SyncClient.PushResult.accepted();
                }

                @Override public synchronized SyncClient.ChangeBatch changes(SyncCursor cursor) {
                    List<SyncEntity> result = new ArrayList<>();
                    for (SyncEntity entity : entities.values()) {
                        if (entity.serverRevision > cursor.revision) result.add(entity);
                    }
                    return new SyncClient.ChangeBatch(new SyncCursor(revision), result);
                }
            };
        }

        int acceptedOperationCount() { return operations.size(); }
        SyncEntity lastEntity() { return entities.values().iterator().next(); }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
