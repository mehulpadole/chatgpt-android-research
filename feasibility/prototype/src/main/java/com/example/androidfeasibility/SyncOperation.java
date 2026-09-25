package com.example.androidfeasibility;

import java.util.UUID;

public final class SyncOperation {
    public enum Action { UPSERT, DELETE }

    public final String operationId;
    public final String idempotencyKey;
    public final String entityId;
    public final String entityType;
    public final Action action;
    public final String payload;
    public final long createdAt;
    public int attempts;
    public SyncState state;

    public SyncOperation(String entityId, String entityType, Action action, String payload, long createdAt) {
        this(UUID.randomUUID().toString(), entityId, entityType, action, payload, createdAt, 0, SyncState.PENDING);
    }

    public SyncOperation(String operationId, String entityId, String entityType, Action action,
                         String payload, long createdAt, int attempts, SyncState state) {
        if (operationId == null || operationId.isEmpty()) throw new IllegalArgumentException("operationId is empty");
        if (entityId == null || entityId.isEmpty()) throw new IllegalArgumentException("entityId is empty");
        this.operationId = operationId;
        this.idempotencyKey = operationId;
        this.entityId = entityId;
        this.entityType = entityType == null ? "" : entityType;
        this.action = action == null ? Action.UPSERT : action;
        this.payload = requireSafePayload(payload);
        this.createdAt = createdAt;
        this.attempts = Math.max(0, attempts);
        this.state = state == null ? SyncState.PENDING : state;
    }

    public void markAttemptFailed(boolean quota) {
        attempts++;
        state = quota ? SyncState.BLOCKED_QUOTA : SyncState.FAILED;
    }

    public void markPending() { state = SyncState.PENDING; }
    public void markSucceeded() { state = action == Action.DELETE ? SyncState.DELETED : SyncState.SYNCED; }

    private static String requireSafePayload(String payload) {
        String value = payload == null ? "" : payload;
        String lower = value.toLowerCase();
        if (lower.contains("authorization") || lower.contains("bearer ")
                || lower.contains("api_key") || lower.contains("api-key")
                || lower.contains("refresh_token") || lower.matches(".*sk-[a-z0-9_-]{8,}.*")) {
            throw new IllegalArgumentException("provider credentials cannot enter sync payload");
        }
        return value;
    }
}
