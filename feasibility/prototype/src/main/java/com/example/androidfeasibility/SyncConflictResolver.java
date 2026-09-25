package com.example.androidfeasibility;

/** Deterministic first-version conflict policy; deletes win ties and stale updates cannot resurrect. */
public final class SyncConflictResolver {
    private SyncConflictResolver() {}

    public static SyncEntity merge(SyncEntity local, SyncEntity remote) {
        if (local == null) return remote;
        if (remote == null) return local;
        if (remote.serverRevision > local.serverRevision) return remote;
        if (remote.serverRevision < local.serverRevision) return local;
        if (remote.deleted() != local.deleted()) return remote.deleted() ? remote : local;
        if (remote.updatedAt > local.updatedAt) return remote;
        if (remote.updatedAt < local.updatedAt) return local;
        if (remote.originDeviceId.compareTo(local.originDeviceId) >= 0) return remote;
        return local;
    }
}
