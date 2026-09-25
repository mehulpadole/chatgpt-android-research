package com.example.androidfeasibility;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public final class Conversation {
    public final String id;
    public final List<Message> messages = new ArrayList<>();
    public final List<Attachment> attachments = new ArrayList<>();
    public String title;
    public long updatedAt;

    public Conversation(String id, String title, long updatedAt) {
        this.id = id;
        this.title = title;
        this.updatedAt = updatedAt;
    }

    public static Conversation empty() {
        return new Conversation(UUID.randomUUID().toString(), "New conversation", System.currentTimeMillis());
    }

    public synchronized void add(Message message) {
        messages.add(message);
        updatedAt = System.currentTimeMillis();
    }

    public synchronized void addAttachment(Attachment attachment) {
        if (attachment == null) throw new IllegalArgumentException("attachment is null");
        attachments.add(attachment);
        updatedAt = System.currentTimeMillis();
    }

    public synchronized Attachment findAttachment(String attachmentId) {
        for (Attachment attachment : attachments) {
            if (attachment.attachmentId.equals(attachmentId)) return attachment;
        }
        return null;
    }

    public synchronized Message findMessage(String messageId) {
        for (Message message : messages) {
            if (message.id.equals(messageId)) return message;
        }
        return null;
    }

    public synchronized List<Message> snapshotMessages() {
        List<Message> copy = new ArrayList<>();
        for (Message message : messages) copy.add(message.copy());
        return Collections.unmodifiableList(copy);
    }

    public synchronized Conversation copy() {
        Conversation copy = new Conversation(id, title, updatedAt);
        for (Message message : messages) copy.messages.add(message.copy());
        for (Attachment attachment : attachments) copy.attachments.add(attachment.copy());
        return copy;
    }
}
