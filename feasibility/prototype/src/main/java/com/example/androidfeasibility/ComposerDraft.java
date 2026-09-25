package com.example.androidfeasibility;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ComposerDraft {
    private String text = "";
    private final List<Attachment> attachments = new ArrayList<>();

    public synchronized void setText(String text) { this.text = text == null ? "" : text; }
    public synchronized String text() { return text; }

    public synchronized void addAttachment(Attachment attachment) {
        if (attachment == null) throw new IllegalArgumentException("attachment is null");
        attachments.add(attachment.copy());
    }

    public synchronized void removeAttachment(String attachmentId) {
        for (int i = attachments.size() - 1; i >= 0; i--) {
            if (attachments.get(i).attachmentId.equals(attachmentId)) attachments.remove(i);
        }
    }

    public synchronized List<Attachment> attachments() {
        List<Attachment> result = new ArrayList<>();
        for (Attachment attachment : attachments) result.add(attachment.copy());
        return Collections.unmodifiableList(result);
    }

    public synchronized void clear() {
        text = "";
        attachments.clear();
    }
}
