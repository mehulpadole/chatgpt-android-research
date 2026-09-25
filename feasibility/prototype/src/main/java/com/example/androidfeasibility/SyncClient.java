package com.example.androidfeasibility;

import java.util.Collections;
import java.util.List;

/** Transport-neutral sync engine; cloud identity/entitlement stays outside provider execution. */
public final class SyncClient {
    public interface Transport {
        PushResult push(List<SyncOperation> operations) throws Exception;
        ChangeBatch changes(SyncCursor cursor) throws Exception;
    }

    public static final class PushResult {
        public final boolean accepted;
        public final boolean quotaBlocked;
        public final boolean authExpired;
        public final boolean retryable;
        public final String error;

        public PushResult(boolean accepted, boolean quotaBlocked, String error) {
            this(accepted, quotaBlocked, false, !accepted && !quotaBlocked, error);
        }

        public PushResult(boolean accepted, boolean quotaBlocked, boolean authExpired,
                          boolean retryable, String error) {
            this.accepted = accepted;
            this.quotaBlocked = quotaBlocked;
            this.authExpired = authExpired;
            this.retryable = retryable;
            this.error = error == null ? "" : error;
        }

        public static PushResult accepted() {
            return new PushResult(true, false, false, false, "");
        }

        public static PushResult quota(String error) {
            return new PushResult(false, true, false, false, error);
        }

        public static PushResult authExpired(String error) {
            return new PushResult(false, false, true, false, error);
        }
    }

    public static final class ChangeBatch {
        public final SyncCursor cursor;
        public final List<SyncEntity> entities;

        public ChangeBatch(SyncCursor cursor, List<SyncEntity> entities) {
            this.cursor = cursor == null ? new SyncCursor(0L) : cursor;
            this.entities = entities == null ? Collections.<SyncEntity>emptyList() : entities;
        }
    }

    private final Entitlement entitlement;

    public SyncClient(Entitlement entitlement) {
        this.entitlement = entitlement == null ? Entitlement.LOCAL_ONLY : entitlement;
    }

    public SyncStatus sync(SyncOutbox outbox, Transport transport) throws Exception {
        if (entitlement == Entitlement.LOCAL_ONLY) return SyncStatus.OFFLINE;
        if (outbox == null || transport == null) return SyncStatus.OFFLINE;
        List<SyncOperation> pending = outbox.pending();
        if (!pending.isEmpty()) {
            PushResult pushed;
            try { pushed = transport.push(pending); }
            catch (Exception error) {
                for (SyncOperation operation : pending) outbox.markFailed(operation.operationId, false);
                return SyncStatus.FAILED;
            }
            if (!pushed.accepted) {
                for (SyncOperation operation : pending) outbox.markFailed(operation.operationId, pushed.quotaBlocked);
                if (pushed.authExpired) return SyncStatus.AUTH_EXPIRED;
                return pushed.quotaBlocked ? SyncStatus.BLOCKED_QUOTA : SyncStatus.FAILED;
            }
            for (SyncOperation operation : pending) outbox.markSucceeded(operation.operationId);
        }
        ChangeBatch changes;
        try {
            changes = transport.changes(outbox.cursor());
        } catch (SyncTransportException error) {
            return error.authExpired ? SyncStatus.AUTH_EXPIRED : SyncStatus.FAILED;
        } catch (Exception error) {
            return SyncStatus.FAILED;
        }
        if (changes != null) outbox.setCursor(changes.cursor);
        return SyncStatus.SYNCED;
    }
}
