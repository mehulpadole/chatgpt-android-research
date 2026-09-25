package com.example.androidfeasibility;

public final class SyncEntity {
    public final String entityId;
    public final String entityType;
    public final String payload;
    public final long updatedAt;
    public final long deletedAt;
    public final String originDeviceId;
    public final long serverRevision;

    public SyncEntity(String entityId, String entityType, String payload, long updatedAt,
                      long deletedAt, String originDeviceId, long serverRevision) {
        if (entityId == null || entityId.isEmpty()) throw new IllegalArgumentException("entityId is empty");
        if (entityType == null || entityType.isEmpty()) throw new IllegalArgumentException("entityType is empty");
        this.entityId = entityId;
        this.entityType = entityType;
        this.payload = payload == null ? "" : payload;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
        this.originDeviceId = originDeviceId == null ? "" : originDeviceId;
        this.serverRevision = Math.max(0L, serverRevision);
    }

    public boolean deleted() { return deletedAt > 0L; }
}
