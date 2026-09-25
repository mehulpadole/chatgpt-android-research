package com.example.androidfeasibility;

/** Lifecycle-safe scheduling shell; a platform worker may call runUntilTerminal(). */
public final class SyncWorker {
    public interface Sleeper {
        void sleep(long millis) throws InterruptedException;
    }

    public static final class BackoffPolicy {
        private final long baseMillis;
        private final long maxMillis;

        public BackoffPolicy(long baseMillis, long maxMillis) {
            this.baseMillis = Math.max(0L, baseMillis);
            this.maxMillis = Math.max(this.baseMillis, maxMillis);
        }

        public long delayForAttempt(int attempt) {
            if (attempt <= 0 || baseMillis == 0L) return 0L;
            long delay = baseMillis;
            for (int i = 1; i < attempt && delay < maxMillis; i++) {
                if (delay > Long.MAX_VALUE / 2L) return maxMillis;
                delay = Math.min(maxMillis, delay * 2L);
            }
            return Math.min(maxMillis, delay);
        }
    }

    private final SyncClient client;
    private final SyncOutbox outbox;
    private final SyncClient.Transport transport;
    private final BackoffPolicy backoff;
    private final Sleeper sleeper;
    private volatile boolean cancelled;

    public SyncWorker(SyncClient client, SyncOutbox outbox, SyncClient.Transport transport,
                      BackoffPolicy backoff, Sleeper sleeper) {
        if (client == null || outbox == null || transport == null) throw new IllegalArgumentException("sync dependencies missing");
        this.client = client;
        this.outbox = outbox;
        this.transport = transport;
        this.backoff = backoff == null ? new BackoffPolicy(1_000L, 30_000L) : backoff;
        this.sleeper = sleeper == null ? new Sleeper() {
            @Override public void sleep(long millis) throws InterruptedException { Thread.sleep(millis); }
        } : sleeper;
    }

    public SyncStatus runOnce() throws Exception {
        if (cancelled) return SyncStatus.CANCELLED;
        return client.sync(outbox, transport);
    }

    public SyncStatus runUntilTerminal(int maxAttempts) throws Exception {
        if (maxAttempts < 1) throw new IllegalArgumentException("maxAttempts must be positive");
        SyncStatus status = SyncStatus.IDLE;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            if (cancelled) return SyncStatus.CANCELLED;
            status = runOnce();
            if (status != SyncStatus.FAILED) return status;
            if (attempt < maxAttempts) sleeper.sleep(backoff.delayForAttempt(attempt));
        }
        return status;
    }

    public void cancel() { cancelled = true; }
}
