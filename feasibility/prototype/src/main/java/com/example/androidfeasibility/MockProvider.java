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
    private volatile MockScenario scenario;

    public MockProvider() {
        this(MockScenario.NORMAL);
    }

    public MockProvider(MockScenario scenario) {
        setScenario(scenario);
    }

    public void setScenario(MockScenario scenario) {
        this.scenario = scenario == null ? MockScenario.NORMAL : scenario;
    }

    @Override
    public StreamHandle start(final ProviderRequest request, final Listener listener) {
        final AtomicBoolean cancelled = new AtomicBoolean(false);
        final List<ScheduledFuture<?>> futures = new ArrayList<>();
        final MockScenario selectedScenario = scenario;
        jobs.put(request.turnId, futures);
        long step = selectedScenario == MockScenario.SLOW ? 220L : 45L;
        schedule(request, listener, cancelled, futures, StreamEvent.started(request.turnId), 10L);

        if (selectedScenario == MockScenario.FAIL_BEFORE_CONTENT) {
            schedule(request, listener, cancelled, futures,
                    StreamEvent.failed(request.turnId, new ProviderError(
                            ProviderError.Category.PROVIDER,
                            "mock failure before first content", false)), 80L);
        } else if (selectedScenario == MockScenario.EMPTY) {
            schedule(request, listener, cancelled, futures, StreamEvent.completed(request.turnId), 80L);
        } else {
            String response = "Deterministic local reply for: " + request.prompt + ".";
            String[] chunks = response.split("(?<= )");
            int limit = selectedScenario == MockScenario.FAIL_AFTER_PARTIAL
                    ? Math.min(3, chunks.length) : chunks.length;
            for (int i = 0; i < limit; i++) {
                schedule(request, listener, cancelled, futures,
                        StreamEvent.delta(request.turnId, chunks[i]), 80L + (i * step));
            }
            long terminalAt = 80L + (limit * step) + 20L;
            if (selectedScenario == MockScenario.FAIL_AFTER_PARTIAL) {
                schedule(request, listener, cancelled, futures,
                        StreamEvent.failed(request.turnId, new ProviderError(
                                ProviderError.Category.PROVIDER,
                                "mock failure after partial content", false)), terminalAt);
            } else {
                schedule(request, listener, cancelled, futures,
                        StreamEvent.completed(request.turnId), terminalAt);
                if (selectedScenario == MockScenario.DUPLICATE_TERMINAL) {
                    schedule(request, listener, cancelled, futures,
                            StreamEvent.completed(request.turnId), terminalAt + 35L);
                }
                if (selectedScenario == MockScenario.DELTA_AFTER_TERMINAL) {
                    schedule(request, listener, cancelled, futures,
                            StreamEvent.delta(request.turnId, " late"), terminalAt + 35L);
                }
                if (selectedScenario == MockScenario.FAIL_AFTER_TERMINAL) {
                    schedule(request, listener, cancelled, futures,
                            StreamEvent.failed(request.turnId, new ProviderError(
                                    ProviderError.Category.PROVIDER,
                                    "late failure", false)), terminalAt + 35L);
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

    private void schedule(final ProviderRequest request, final Listener listener,
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
