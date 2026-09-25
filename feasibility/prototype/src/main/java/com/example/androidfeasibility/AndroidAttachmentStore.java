package com.example.androidfeasibility;

import android.content.Context;
import android.net.Uri;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;

/** Copies selected content URIs into app-managed files before domain persistence. */
public final class AndroidAttachmentStore {
    private final Context context;
    private final FileAttachmentRepository repository;

    public AndroidAttachmentStore(Context context) {
        if (context == null) throw new IllegalArgumentException("context is null");
        this.context = context.getApplicationContext();
        this.repository = new FileAttachmentRepository(new File(this.context.getFilesDir(), "attachments"));
    }

    public Attachment importUri(Uri uri, String conversationId, String messageId) throws Exception {
        if (uri == null) throw new IllegalArgumentException("uri is null");
        File staging = new File(context.getCacheDir(), "attachment-import-" + System.nanoTime());
        String displayName = uri.getLastPathSegment() == null ? "attachment" : uri.getLastPathSegment();
        try (InputStream input = context.getContentResolver().openInputStream(uri)) {
            if (input == null) throw new IllegalArgumentException("selected source is unavailable");
            try (FileOutputStream output = new FileOutputStream(staging)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) != -1) output.write(buffer, 0, read);
                output.flush();
            }
        }
        String mime = context.getContentResolver().getType(uri);
        try {
            return repository.importFile(staging, conversationId, messageId, mime, displayName);
        } finally {
            if (staging.exists()) staging.delete();
        }
    }

    public FileAttachmentRepository repository() { return repository; }
}
