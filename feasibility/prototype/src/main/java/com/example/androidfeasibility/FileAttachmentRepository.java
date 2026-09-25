package com.example.androidfeasibility;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

/** Durable metadata plus application-managed binary files outside conversation JSON. */
public final class FileAttachmentRepository implements AttachmentRepository {
    private static final long MAX_BYTES = 25L * 1024L * 1024L;
    private final File root;

    public FileAttachmentRepository(File root) {
        if (root == null) throw new IllegalArgumentException("root is null");
        this.root = root;
        if (!root.exists() && !root.mkdirs()) throw new IllegalStateException("attachment root unavailable");
    }

    @Override public synchronized Attachment importFile(File source, String conversationId,
                                                         String messageId, String declaredMimeType,
                                                         String displayName) throws Exception {
        AttachmentValidator.Result result = AttachmentValidator.validate(source, declaredMimeType, MAX_BYTES);
        if (!result.valid) throw new IllegalArgumentException(result.error);
        String id = UUID.randomUUID().toString();
        File target = dataFile(id);
        copy(source, target);
        Attachment attachment = new Attachment(id, conversationId, messageId,
                displayName == null || displayName.isEmpty() ? source.getName() : displayName,
                result.detectedMimeType, declaredMimeType, source.length(), target.getAbsolutePath(),
                AttachmentState.READY, System.currentTimeMillis(), null);
        save(attachment);
        return attachment.copy();
    }

    @Override public synchronized void save(Attachment attachment) throws Exception {
        if (attachment == null) throw new IllegalArgumentException("attachment is null");
        if (!root.exists() && !root.mkdirs()) throw new IOException("attachment root unavailable");
        Properties properties = new Properties();
        properties.setProperty("attachmentId", attachment.attachmentId);
        properties.setProperty("conversationId", attachment.conversationId);
        properties.setProperty("messageId", attachment.messageId);
        properties.setProperty("displayName", attachment.displayName);
        properties.setProperty("detectedMimeType", attachment.detectedMimeType);
        properties.setProperty("declaredMimeType", attachment.declaredMimeType);
        properties.setProperty("byteSize", Long.toString(attachment.byteSize));
        properties.setProperty("localReference", attachment.localReference);
        properties.setProperty("state", attachment.state.name());
        properties.setProperty("createdAt", Long.toString(attachment.createdAt));
        File temp = new File(root, attachment.attachmentId + ".properties.tmp");
        try (FileOutputStream output = new FileOutputStream(temp)) {
            properties.store(output, "MoCHi attachment metadata");
        }
        move(temp, metaFile(attachment.attachmentId));
    }

    @Override public synchronized Attachment load(String attachmentId) throws Exception {
        if (attachmentId == null || attachmentId.isEmpty()) return null;
        File file = metaFile(attachmentId);
        if (!file.exists()) return null;
        Properties properties = new Properties();
        try (FileInputStream input = new FileInputStream(file)) { properties.load(input); }
        return fromProperties(properties);
    }

    @Override public synchronized List<Attachment> forMessage(String messageId) throws Exception {
        List<Attachment> result = new ArrayList<>();
        File[] files = root.listFiles();
        if (files == null) return result;
        for (File file : files) {
            if (!file.getName().endsWith(".properties")) continue;
            Attachment attachment = load(file.getName().substring(0, file.getName().length() - 11));
            if (attachment != null && attachment.messageId.equals(messageId)) result.add(attachment);
        }
        return result;
    }

    @Override public synchronized void delete(String attachmentId) throws Exception {
        Attachment target = load(attachmentId);
        if (target == null) return;
        if (!isReferencedByAnother(target.localReference, attachmentId)) {
            File local = new File(target.localReference);
            if (local.exists() && !local.delete()) throw new IOException("attachment file delete failed");
        }
        if (!metaFile(attachmentId).delete()) throw new IOException("attachment metadata delete failed");
    }

    private boolean isReferencedByAnother(String localReference, String attachmentId) throws Exception {
        File[] files = root.listFiles();
        if (files == null) return false;
        for (File file : files) {
            if (!file.getName().endsWith(".properties")) continue;
            String id = file.getName().substring(0, file.getName().length() - 11);
            if (id.equals(attachmentId)) continue;
            Attachment attachment = load(id);
            if (attachment != null && attachment.localReference.equals(localReference)) return true;
        }
        return false;
    }

    private Attachment fromProperties(Properties p) {
        return new Attachment(p.getProperty("attachmentId", ""), p.getProperty("conversationId", ""),
                p.getProperty("messageId", ""), p.getProperty("displayName", "file"),
                p.getProperty("detectedMimeType", "application/octet-stream"),
                p.getProperty("declaredMimeType", ""), Long.parseLong(p.getProperty("byteSize", "0")),
                p.getProperty("localReference", ""),
                AttachmentState.valueOf(p.getProperty("state", AttachmentState.READY.name())),
                Long.parseLong(p.getProperty("createdAt", "0")), null);
    }

    private File dataFile(String id) { return new File(root, id + ".bin"); }
    private File metaFile(String id) { return new File(root, id + ".properties"); }

    private static void copy(File source, File target) throws IOException {
        try (FileInputStream input = new FileInputStream(source);
             FileOutputStream output = new FileOutputStream(target)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) output.write(buffer, 0, read);
            output.flush();
        }
    }

    private static void move(File source, File target) throws IOException {
        try {
            Files.move(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (java.nio.file.AtomicMoveNotSupportedException unsupported) {
            Files.move(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
