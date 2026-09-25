package com.example.androidfeasibility;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/** Small durable outbox/cursor store for the prototype; no provider credentials are serialized. */
public final class JsonSyncStore implements SyncOutbox {
    private final File file;
    private final List<SyncOperation> operations = new ArrayList<>();
    private long cursor;

    public JsonSyncStore(File file) throws Exception {
        if (file == null) throw new IllegalArgumentException("file is null");
        this.file = file;
        load();
    }

    @Override public synchronized void enqueue(SyncOperation operation) throws Exception {
        for (SyncOperation existing : operations) {
            if (existing.operationId.equals(operation.operationId)) return;
        }
        operations.add(operation);
        persist();
    }

    @Override public synchronized List<SyncOperation> pending() {
        List<SyncOperation> result = new ArrayList<>();
        for (SyncOperation operation : operations) {
            if (operation.state == SyncState.PENDING || operation.state == SyncState.FAILED
                    || operation.state == SyncState.BLOCKED_QUOTA) result.add(operation);
        }
        return result;
    }

    @Override public synchronized void markSucceeded(String operationId) throws Exception {
        for (SyncOperation operation : operations) {
            if (operation.operationId.equals(operationId)) operation.markSucceeded();
        }
        persist();
    }

    @Override public synchronized void markFailed(String operationId, boolean quota) throws Exception {
        for (SyncOperation operation : operations) {
            if (operation.operationId.equals(operationId)) operation.markAttemptFailed(quota);
        }
        persist();
    }

    @Override public synchronized SyncCursor cursor() { return new SyncCursor(cursor); }

    @Override public synchronized void setCursor(SyncCursor cursor) throws Exception {
        long candidate = cursor == null ? 0L : cursor.revision;
        this.cursor = Math.max(this.cursor, candidate);
        persist();
    }

    private void load() throws Exception {
        if (!file.exists()) return;
        Properties properties = new Properties();
        try (FileInputStream input = new FileInputStream(file)) { properties.load(input); }
        cursor = Long.parseLong(properties.getProperty("cursor", "0"));
        int count = Integer.parseInt(properties.getProperty("count", "0"));
        for (int i = 0; i < count; i++) {
            String prefix = "op." + i + ".";
            operations.add(new SyncOperation(properties.getProperty(prefix + "id"),
                    properties.getProperty(prefix + "entityId"), properties.getProperty(prefix + "entityType"),
                    SyncOperation.Action.valueOf(properties.getProperty(prefix + "action")),
                    properties.getProperty(prefix + "payload", ""),
                    Long.parseLong(properties.getProperty(prefix + "createdAt", "0")),
                    Integer.parseInt(properties.getProperty(prefix + "attempts", "0")),
                    SyncState.valueOf(properties.getProperty(prefix + "state", SyncState.PENDING.name()))));
        }
    }

    private void persist() throws Exception {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) throw new IOException("sync directory unavailable");
        Properties properties = new Properties();
        properties.setProperty("cursor", Long.toString(cursor));
        properties.setProperty("count", Integer.toString(operations.size()));
        for (int i = 0; i < operations.size(); i++) {
            SyncOperation operation = operations.get(i);
            String prefix = "op." + i + ".";
            properties.setProperty(prefix + "id", operation.operationId);
            properties.setProperty(prefix + "entityId", operation.entityId);
            properties.setProperty(prefix + "entityType", operation.entityType);
            properties.setProperty(prefix + "action", operation.action.name());
            properties.setProperty(prefix + "payload", operation.payload);
            properties.setProperty(prefix + "createdAt", Long.toString(operation.createdAt));
            properties.setProperty(prefix + "attempts", Integer.toString(operation.attempts));
            properties.setProperty(prefix + "state", operation.state.name());
        }
        File temp = new File(file.getPath() + ".tmp");
        try (FileOutputStream output = new FileOutputStream(temp)) { properties.store(output, "MoCHi local sync"); }
        if (!temp.renameTo(file)) throw new IOException("sync store replace failed");
    }
}
