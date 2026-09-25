package com.example.androidfeasibility;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

public final class SyncIntegrationTest {
    public static void main(String[] args) throws Exception {
        testTimeoutAndRetryWithoutDataLoss();
        testKilledBatchIsSafeToReplay();
        testStaleCursorDoesNotRegress();
        testAuthenticationExpiryStopsRetry();
        testQuotaBlocksWithoutDroppingOperation();
        testAttachmentMetadataOnlyPolicy();
        testStagingHttpTransport();
        System.out.println("SYNC INTEGRATION TESTS PASSED");
    }

    private static void testTimeoutAndRetryWithoutDataLoss() throws Exception {
        JsonSyncStore store = store("timeout");
        store.enqueue(operation("timeout-op"));
        ScriptedTransport transport = new ScriptedTransport();
        transport.timeoutOnce = true;
        SyncStatus status = worker(store, transport).runUntilTerminal(2);
        check(status == SyncStatus.SYNCED, "timeout must be retried to success");
        check(transport.pushCalls == 2, "timeout retry must make exactly one retry");
        check(store.pending().isEmpty(), "successful retry must drain only the completed operation");
    }

    private static void testKilledBatchIsSafeToReplay() throws Exception {
        JsonSyncStore store = store("killed");
        store.enqueue(operation("killed-op"));
        ScriptedTransport transport = new ScriptedTransport();
        transport.acceptThenDisconnect = true;
        SyncStatus status = worker(store, transport).runUntilTerminal(2);
        check(status == SyncStatus.SYNCED, "accepted-but-disconnected batch must replay safely");
        check(transport.acceptedIds.size() == 1, "idempotent replay must not duplicate server operation");
    }

    private static void testStaleCursorDoesNotRegress() throws Exception {
        JsonSyncStore store = store("cursor");
        store.setCursor(new SyncCursor(5L));
        ScriptedTransport transport = new ScriptedTransport();
        transport.returnedCursor = 3L;
        check(worker(store, transport).runOnce() == SyncStatus.SYNCED, "stale cursor response may be acknowledged");
        check(store.cursor().revision == 5L, "stale server cursor must not regress local cursor");
    }

    private static void testAuthenticationExpiryStopsRetry() throws Exception {
        JsonSyncStore store = store("auth-expiry");
        store.enqueue(operation("auth-op"));
        ScriptedTransport transport = new ScriptedTransport();
        transport.authExpired = true;
        SyncWorker worker = worker(store, transport);
        check(worker.runUntilTerminal(4) == SyncStatus.AUTH_EXPIRED,
                "expired authentication must be visible and terminal for this run");
        check(transport.pushCalls == 1, "authentication expiry must not spin retries");
        check(store.pending().size() == 1, "authentication expiry must preserve local work");
    }

    private static void testQuotaBlocksWithoutDroppingOperation() throws Exception {
        JsonSyncStore store = store("quota");
        store.enqueue(operation("quota-op"));
        ScriptedTransport transport = new ScriptedTransport();
        transport.quota = true;
        check(worker(store, transport).runUntilTerminal(3) == SyncStatus.BLOCKED_QUOTA,
                "quota response must block the run");
        check(store.pending().size() == 1 && store.pending().get(0).state == SyncState.BLOCKED_QUOTA,
                "quota block must preserve the operation for a later policy decision");
    }

    private static void testAttachmentMetadataOnlyPolicy() throws Exception {
        Attachment attachment = new Attachment("attachment-1", "conversation-1", "message-1",
                "photo.png", "image/png", "image/png", 12L, "/private/app/attachment-1.bin",
                AttachmentState.READY, 42L, null);
        AttachmentSyncPolicy.Decision cloud = AttachmentSyncPolicy.decide(
                Entitlement.CLOUD_SYNC, attachment, false, 0L);
        check(cloud.syncMetadata && !cloud.syncBinary, "default cloud sync must transfer metadata only");
        SyncOperation metadata = AttachmentSyncPolicy.metadataOperation(attachment, "device-a", 42L);
        check(metadata.payload.contains("attachment-1"), "metadata must identify the attachment");
        check(!metadata.payload.contains("/private/app"), "metadata must not transfer local paths");
        check(!metadata.payload.contains("providerReferences"), "metadata must not transfer provider references");
        AttachmentSyncPolicy.Decision local = AttachmentSyncPolicy.decide(
                Entitlement.LOCAL_ONLY, attachment, true, 100L);
        check(!local.syncMetadata && !local.syncBinary, "local-only mode must not schedule attachment sync");
    }

    private static void testStagingHttpTransport() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 2);
        server.createContext("/v1/sync/push", SyncIntegrationTest::respondPush);
        server.createContext("/v1/sync/changes", SyncIntegrationTest::respondChanges);
        server.start();
        try {
            SyncHttpTransport transport = new SyncHttpTransport(
                    new URL("http://127.0.0.1:" + server.getAddress().getPort() + "/"),
                    new SyncHttpTransport.AuthTokenProvider() {
                        @Override public String token() { return "staging-token"; }
                    }, 2_000, 2_000);
            check(transport.push(java.util.Collections.singletonList(operation("http-op"))).accepted,
                    "staging HTTP push must accept the batch");
            SyncClient.ChangeBatch batch = transport.changes(new SyncCursor(0L));
            check(batch.cursor.revision == 2L && batch.entities.size() == 1,
                    "staging HTTP changes must decode cursor and entity");
            check(batch.entities.get(0).payload.equals("remote"),
                    "staging HTTP entity payload must remain provider-neutral");
        } finally {
            server.stop(0);
        }
    }

    private static void respondPush(HttpExchange exchange) throws IOException {
        read(exchange);
        respond(exchange, "{\"accepted\":[\"http-op\"],\"cursor\":1}");
    }

    private static void respondChanges(HttpExchange exchange) throws IOException {
        respond(exchange, "{\"cursor\":2,\"changes\":[{\"entity_id\":\"remote-1\",\"entity_type\":\"conversation\",\"payload\":\"remote\",\"updated_at\":2,\"deleted_at\":0,\"origin_device_id\":\"device-b\",\"server_revision\":2}]}");
    }

    private static String read(HttpExchange exchange) throws IOException {
        byte[] buffer = new byte[4096];
        StringBuilder body = new StringBuilder();
        int count;
        while ((count = exchange.getRequestBody().read(buffer)) != -1) {
            body.append(new String(buffer, 0, count, java.nio.charset.StandardCharsets.UTF_8));
        }
        return body.toString();
    }

    private static void respond(HttpExchange exchange, String body) throws IOException {
        byte[] bytes = body.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) { output.write(bytes); }
    }

    private static SyncWorker worker(JsonSyncStore store, ScriptedTransport transport) {
        return new SyncWorker(new SyncClient(Entitlement.CLOUD_SYNC), store, transport,
                new SyncWorker.BackoffPolicy(0L, 0L), new SyncWorker.Sleeper() {
                    @Override public void sleep(long millis) { }
                });
    }

    private static JsonSyncStore store(String label) throws Exception {
        return new JsonSyncStore(new File(System.getProperty("java.io.tmpdir"),
                "mochi-sync-integration-" + label + "-" + System.nanoTime() + ".properties"));
    }

    private static SyncOperation operation(String id) {
        return new SyncOperation(id, "conversation-1", "conversation",
                SyncOperation.Action.UPSERT, "{\"title\":\"local\"}", 1L, 0, SyncState.PENDING);
    }

    private static final class ScriptedTransport implements SyncClient.Transport {
        int pushCalls;
        boolean timeoutOnce;
        boolean acceptThenDisconnect;
        boolean authExpired;
        boolean quota;
        long returnedCursor;
        final List<String> acceptedIds = new ArrayList<>();

        @Override public SyncClient.PushResult push(List<SyncOperation> operations) throws Exception {
            pushCalls++;
            if (authExpired) return SyncClient.PushResult.authExpired("authentication expired");
            if (quota) return SyncClient.PushResult.quota("quota");
            for (SyncOperation operation : operations) {
                if (!acceptedIds.contains(operation.operationId)) acceptedIds.add(operation.operationId);
            }
            if (timeoutOnce) {
                timeoutOnce = false;
                throw new IOException("timeout");
            }
            if (acceptThenDisconnect) {
                acceptThenDisconnect = false;
                throw new IOException("connection lost after server commit");
            }
            return SyncClient.PushResult.accepted();
        }

        @Override public SyncClient.ChangeBatch changes(SyncCursor cursor) {
            long next = returnedCursor == 0L ? cursor.revision + 1L : returnedCursor;
            return new SyncClient.ChangeBatch(new SyncCursor(next), null);
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
