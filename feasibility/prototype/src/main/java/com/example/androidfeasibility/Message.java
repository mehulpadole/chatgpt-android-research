package com.example.androidfeasibility;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Message {
    public final String id;
    public final String conversationId;
    public final String turnId;
    public final Role role;
    public final long createdAt;
    public String content;
    public MessageStatus status;
    public String provider;
    public String model;
    public String failureCategory;
    public String failureMessage;
    public final List<ContentPart> contentParts = new ArrayList<>();

    public Message(String id, String conversationId, String turnId, Role role,
                   String content, MessageStatus status, String provider,
                   String model, long createdAt) {
        this(id, conversationId, turnId, role, content, status, provider, model,
                createdAt, "", "");
    }

    public Message(String id, String conversationId, String turnId, Role role,
                   String content, MessageStatus status, String provider,
                   String model, long createdAt, String failureCategory,
                   String failureMessage) {
        this.id = id;
        this.conversationId = conversationId;
        this.turnId = turnId;
        this.role = role;
        this.content = content;
        this.status = status;
        this.provider = provider;
        this.model = model;
        this.createdAt = createdAt;
        this.failureCategory = failureCategory == null ? "" : failureCategory;
        this.failureMessage = failureMessage == null ? "" : failureMessage;
    }

    public Message copy() {
        Message copy = new Message(id, conversationId, turnId, role, content, status,
                provider, model, createdAt, failureCategory, failureMessage);
        copy.contentParts.clear();
        for (ContentPart part : contentParts) copy.contentParts.add(part.copy());
        return copy;
    }

    public synchronized void appendText(String delta) {
        String value = delta == null ? "" : delta;
        if (value.isEmpty()) return;
        content = content == null ? value : content + value;
        if (!contentParts.isEmpty() && contentParts.get(contentParts.size() - 1) instanceof TextPart) {
            TextPart previous = (TextPart) contentParts.remove(contentParts.size() - 1);
            contentParts.add(new TextPart(previous.text + value));
        } else {
            contentParts.add(new TextPart(value));
        }
    }

    public synchronized void addContentPart(ContentPart part) {
        if (part == null) throw new IllegalArgumentException("content part is null");
        ContentPart copy = part.copy();
        contentParts.add(copy);
        if (copy instanceof TextPart) content = (content == null ? "" : content) + ((TextPart) copy).text;
    }

    synchronized void restoreContentPart(ContentPart part) {
        if (part != null) contentParts.add(part.copy());
    }

    public synchronized List<ContentPart> snapshotContentParts() {
        List<ContentPart> copy = new ArrayList<>();
        for (ContentPart part : contentParts) copy.add(part.copy());
        return Collections.unmodifiableList(copy);
    }
}
