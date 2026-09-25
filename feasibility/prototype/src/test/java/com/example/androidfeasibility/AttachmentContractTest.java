package com.example.androidfeasibility;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;

public final class AttachmentContractTest {
    public static void main(String[] args) throws Exception {
        testStableAttachmentAndMessageRoundTrip();
        testSharedFileCleanupOwnership();
        System.out.println("ATTACHMENT CONTRACT TESTS PASSED");
    }

    private static void testStableAttachmentAndMessageRoundTrip() throws Exception {
        Conversation conversation = Conversation.empty();
        Attachment attachment = new Attachment("attachment-1", conversation.id, "message-1",
                "photo.png", "image/png", "image/png", 12L, "/app/files/attachment-1.bin",
                AttachmentState.READY, 123L, null);
        conversation.addAttachment(attachment);
        Message message = new Message("message-1", conversation.id, "turn-1", Role.USER,
                "", MessageStatus.COMPLETED, "local-mock", "deterministic", 456L);
        message.addContentPart(new TextPart("look"));
        message.addContentPart(new ImagePart("attachment-1", "image/png"));
        conversation.add(message);

        Conversation restored = AndroidConversationCodec.decode(AndroidConversationCodec.encode(conversation));
        check(restored.attachments.size() == 1, "attachment must round-trip once");
        check(restored.attachments.get(0).attachmentId.equals("attachment-1"),
                "attachment ID must remain stable");
        check(restored.messages.get(0).contentParts.size() == 2,
                "structured message parts must round-trip");
        check(restored.messages.get(0).contentParts.get(1) instanceof ImagePart,
                "image part type must remain provider-neutral");
        check(restored.messages.get(0).content.equals("look"),
                "legacy text content must remain available");
    }

    private static void testSharedFileCleanupOwnership() throws Exception {
        File root = new File(System.getProperty("java.io.tmpdir"), "mochi-attachment-repo-" + System.nanoTime());
        File source = new File(root, "source.txt");
        check(root.mkdirs(), "repository root must be created");
        try (FileOutputStream output = new FileOutputStream(source)) {
            output.write("shared content".getBytes(StandardCharsets.UTF_8));
        }
        FileAttachmentRepository repository = new FileAttachmentRepository(root);
        Attachment first = repository.importFile(source, "conversation-1", "message-1",
                "text/plain", "text/plain");
        Attachment second = new Attachment("attachment-2", "conversation-1", "message-2",
                "shared.txt", "text/plain", "text/plain", first.byteSize, first.localReference,
                AttachmentState.READY, 2L, null);
        repository.save(second);
        check(repository.load(first.attachmentId) != null, "imported attachment must load");
        repository.delete(first.attachmentId);
        check(new File(first.localReference).exists(), "shared file must survive first delete");
        repository.delete(second.attachmentId);
        check(!new File(first.localReference).exists(), "unshared file must be cleaned up");
        delete(root, source);
    }

    private static void delete(File root, File source) {
        if (source.exists()) source.delete();
        if (root.exists()) {
            File[] children = root.listFiles();
            if (children != null) for (File child : children) child.delete();
            root.delete();
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
