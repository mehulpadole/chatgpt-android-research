package com.example.androidfeasibility;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public final class Conversation {
    public final String id;
    public final List<Message> messages = new ArrayList<>();
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
        return copy;
    }
}
