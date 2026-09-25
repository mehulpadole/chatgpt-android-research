package com.example.androidfeasibility;

public final class SyncCursor {
    public final long revision;

    public SyncCursor(long revision) { this.revision = Math.max(0L, revision); }
}
