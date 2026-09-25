package com.example.androidfeasibility;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public final class AttachmentPreparationTest {
    public static void main(String[] args) throws Exception {
        File root = new File(System.getProperty("java.io.tmpdir"), "mochi-preparation-" + System.nanoTime());
        check(root.mkdirs(), "test root must be created");
        File image = new File(root, "image.png");
        try (FileOutputStream output = new FileOutputStream(image)) {
            output.write("synthetic-image".getBytes(StandardCharsets.UTF_8));
        }
        FileAttachmentRepository repository = new FileAttachmentRepository(new File(root, "store"));
        Attachment attachment = repository.importFile(image, "conversation-1", "message-1",
                "image/png", "image.png");
        ProviderModel visionModel = new ProviderModel("openrouter", "vision", "Vision", caps(), 8192);
        ProviderModel textModel = new ProviderModel("openrouter", "text", "Text", null, 8192);
        OpenRouterAttachmentAdapter adapter = new OpenRouterAttachmentAdapter();

        ResultCapture success = new ResultCapture();
        adapter.prepare(attachment, visionModel, success);
        check(success.await(), "image preparation must finish");
        check(success.result.success, "vision model must prepare image");
        check(success.result.prepared.dataUrl.startsWith("data:image/png;base64,"),
                "prepared image must be a data URL");

        ResultCapture unsupported = new ResultCapture();
        adapter.prepare(attachment, textModel, unsupported);
        check(unsupported.await(), "unsupported preparation must finish");
        check(!unsupported.result.success && unsupported.result.error.contains("does not support"),
                "unsupported capability must fail before reading content");

        Attachment missing = new Attachment("missing", "conversation-1", "message-1", "missing.png",
                "image/png", "image/png", 1L, new File(root, "missing.png").getAbsolutePath(),
                AttachmentState.READY, 1L, null);
        ResultCapture missingResult = new ResultCapture();
        adapter.prepare(missing, visionModel, missingResult);
        check(missingResult.await(), "missing source preparation must finish");
        check(!missingResult.result.success && missingResult.result.error.contains("unavailable"),
                "missing source must fail without deleting draft state");

        ResultCapture retry = new ResultCapture();
        adapter.prepare(attachment, visionModel, retry);
        check(retry.await() && retry.result.success, "same attachment must be retryable");

        OpenRouterAttachmentAdapter slow = new OpenRouterAttachmentAdapter(200L);
        ResultCapture cancelled = new ResultCapture();
        ProviderAttachmentAdapter.PreparationHandle handle = slow.prepare(attachment, visionModel, cancelled);
        handle.cancel();
        check(cancelled.await(), "cancelled preparation must notify listener");
        check(cancelled.result.cancelled, "cancelled preparation must be explicit");
        handle.cancel();
        slow.shutdown();
        adapter.shutdown();
        delete(root);
        System.out.println("ATTACHMENT PREPARATION TESTS PASSED");
    }

    private static Set<String> caps() {
        Set<String> result = new HashSet<>();
        result.add(ProviderCapabilities.TEXT);
        result.add(ProviderCapabilities.VISION);
        result.add(ProviderCapabilities.STREAMING);
        return result;
    }

    private static void delete(File file) {
        if (!file.exists()) return;
        File[] children = file.listFiles();
        if (children != null) for (File child : children) delete(child);
        file.delete();
    }

    private static final class ResultCapture implements ProviderAttachmentAdapter.Listener {
        final CountDownLatch latch = new CountDownLatch(1);
        volatile ProviderAttachmentAdapter.Result result;

        @Override public void onResult(ProviderAttachmentAdapter.Result result) {
            this.result = result;
            latch.countDown();
        }

        boolean await() throws InterruptedException {
            return latch.await(2, TimeUnit.SECONDS) && result != null;
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
