package com.example.androidfeasibility;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class MockProvider implements ProviderAdapter {
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);
    private final Map<String, List<ScheduledFuture<?>>> jobs = new ConcurrentHashMap<>();

    @Override
    public StreamHandle start(final Request request, final Listener listener) {
        final AtomicBoolean cancelled = new AtomicBoolean(false);
        final List<ScheduledFuture<?>> futures = new ArrayList<>();
        jobs.put(request.turnId, futures);
        long step = request.scenario == MockScenario.SLOW ? 220L : 45L;
        schedule(request, listener, cancelled, futures, StreamEvent.started(request.turnId), 10L);

        if (request.scenario == MockScenario.FAIL_BEFORE_CONTENT) {
            schedule(request, listener, cancelled, futures,
                    StreamEvent.error(request.turnId, "mock failure before first content"), 80L);
        } else if (request.scenario == MockScenario.EMPTY) {
            schedule(request, listener, cancelled, futures, StreamEvent.completed(request.turnId), 80L);
        } else {
            String response = "Deterministic local reply for: " + request.prompt + ".";
            String[] chunks = response.split("(?<= )");
            int limit = request.scenario == MockScenario.FAIL_AFTER_PARTIAL
                    ? Math.min(3, chunks.length) : chunks.length;
            for (int i = 0; i < limit; i++) {
                schedule(request, listener, cancelled, futures,
                        StreamEvent.delta(request.turnId, chunks[i]), 80L + (i * step));
            }
            long terminalAt = 80L + (limit * step) + 20L;
            if (request.scenario == MockScenario.FAIL_AFTER_PARTIAL) {
                schedule(request, listener, cancelled, futures,
                        StreamEvent.error(request.turnId, "mock failure after partial content"), terminalAt);
            } else {
                schedule(request, listener, cancelled, futures,
                        StreamEvent.completed(request.turnId), terminalAt);
                if (request.scenario == MockScenario.DUPLICATE_TERMINAL) {
                    schedule(request, listener, cancelled, futures,
                            StreamEvent.completed(request.turnId), terminalAt + 35L);
                }
            }
        }
        return new StreamHandle() {
            @Override
            public void cancel() {
                if (!cancelled.compareAndSet(false, true)) return;
                List<ScheduledFuture<?>> active = jobs.remove(request.turnId);
                if (active != null) {
                    for (ScheduledFuture<?> future : active) future.cancel(false);
                }
            }
        };
    }

    private void schedule(final Request request, final Listener listener,
                          final AtomicBoolean cancelled, final List<ScheduledFuture<?>> futures,
                          final StreamEvent event, long delayMs) {
        ScheduledFuture<?> future = executor.schedule(new Runnable() {
            @Override public void run() {
                if (!cancelled.get()) listener.onEvent(event);
            }
        }, delayMs, TimeUnit.MILLISECONDS);
        futures.add(future);
    }

    public void shutdown() {
        executor.shutdownNow();
        jobs.clear();
    }
}
