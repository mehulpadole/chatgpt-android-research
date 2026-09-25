package com.example.androidfeasibility;

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
        return new Message(id, conversationId, turnId, role, content, status,
                provider, model, createdAt, failureCategory, failureMessage);
    }
}
