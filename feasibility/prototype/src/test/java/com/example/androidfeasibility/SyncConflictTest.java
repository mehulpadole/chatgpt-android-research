package com.example.androidfeasibility;

public final class SyncConflictTest {
    public static void main(String[] args) {
        SyncEntity local = new SyncEntity("conversation-1", "conversation", "local", 20L,
                0L, "device-a", 4L);
        SyncEntity stale = new SyncEntity("conversation-1", "conversation", "stale", 30L,
                0L, "device-b", 3L);
        check(SyncConflictResolver.merge(local, stale) == local,
                "stale revision must not replace newer local entity");
        SyncEntity tombstone = new SyncEntity("conversation-1", "conversation", "", 20L,
                100L, "device-b", 4L);
        check(SyncConflictResolver.merge(local, tombstone) == tombstone,
                "delete must win equal revision");
        SyncEntity newer = new SyncEntity("conversation-1", "conversation", "remote", 21L,
                0L, "device-b", 4L);
        check(SyncConflictResolver.merge(local, newer) == newer,
                "newer timestamp must win equal revision");
        System.out.println("SYNC CONFLICT TESTS PASSED");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
