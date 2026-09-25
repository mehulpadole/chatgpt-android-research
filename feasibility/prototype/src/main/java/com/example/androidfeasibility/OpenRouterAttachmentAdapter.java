package com.example.androidfeasibility;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

/** Local preparation for OpenRouter image content; upload/wire encoding stays in the adapter. */
public final class OpenRouterAttachmentAdapter implements ProviderAttachmentAdapter {
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final Set<Future<?>> active = ConcurrentHashMap.newKeySet();
    private final long preparationDelayMs;

    public OpenRouterAttachmentAdapter() { this(0L); }

    OpenRouterAttachmentAdapter(long preparationDelayMs) {
        this.preparationDelayMs = Math.max(0L, preparationDelayMs);
    }

    @Override public PreparationHandle prepare(final Attachment attachment, final ProviderModel model,
                                               final Listener listener) {
        if (attachment == null) throw new IllegalArgumentException("attachment is null");
        if (model == null) throw new IllegalArgumentException("model is null");
        if (listener == null) throw new IllegalArgumentException("listener is null");
        final AtomicBoolean cancelled = new AtomicBoolean();
        final AtomicBoolean delivered = new AtomicBoolean();
        final Future<?>[] future = new Future<?>[1];
        future[0] = executor.submit(new Runnable() {
            @Override public void run() {
                try {
                    if (preparationDelayMs > 0L) Thread.sleep(preparationDelayMs);
                    if (cancelled.get()) { deliver(listener, delivered, Result.cancelled()); return; }
                    if (!model.capabilities.contains(ProviderCapabilities.VISION)
                            || !attachment.detectedMimeType.startsWith("image/")) {
                        deliver(listener, delivered, Result.failed("selected model does not support this image"));
                        return;
                    }
                    File file = new File(attachment.localReference);
                    if (!file.exists() || !file.isFile() || !file.canRead()) {
                        deliver(listener, delivered, Result.failed("attachment source is unavailable"));
                        return;
                    }
                    byte[] bytes = read(file, cancelled);
                    if (cancelled.get()) { deliver(listener, delivered, Result.cancelled()); return; }
                    deliver(listener, delivered, Result.success(new PreparedAttachment("openrouter",
                            attachment.attachmentId, attachment.displayName, attachment.detectedMimeType,
                            "data:" + attachment.detectedMimeType + ";base64," + Base64.getEncoder().encodeToString(bytes))));
                } catch (IOException error) {
                    if (!cancelled.get()) deliver(listener, delivered, Result.failed(error.getMessage()));
                } catch (InterruptedException interrupted) {
                    if (!cancelled.get()) deliver(listener, delivered, Result.failed("preparation interrupted"));
                } finally {
                    if (future[0] != null) active.remove(future[0]);
                }
            }
        });
        active.add(future[0]);
        return new PreparationHandle() {
            @Override public void cancel() {
                if (cancelled.compareAndSet(false, true)) {
                    deliver(listener, delivered, Result.cancelled());
                    if (future[0] != null) future[0].cancel(true);
                }
            }
        };
    }

    public int activePreparationCount() { return active.size(); }

    public void shutdown() {
        for (Future<?> future : active) future.cancel(true);
        active.clear();
        executor.shutdownNow();
    }

    private byte[] read(File file, AtomicBoolean cancelled) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (FileInputStream input = new FileInputStream(file)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) {
                if (cancelled.get()) return new byte[0];
                output.write(buffer, 0, read);
            }
        }
        return output.toByteArray();
    }

    private static void deliver(Listener listener, AtomicBoolean delivered, Result result) {
        if (delivered.compareAndSet(false, true)) listener.onResult(result);
    }
}
