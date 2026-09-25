package com.example.androidfeasibility;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public final class Project {
    public final String projectId;
    public final String title;
    public final long createdAt;
    private long updatedAt;
    private final Set<String> conversationIds = new LinkedHashSet<>();

    public Project(String projectId, String title, long createdAt) {
        this.projectId = require(projectId, "projectId");
        this.title = require(title, "title");
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public synchronized void addConversation(String conversationId) {
        conversationIds.add(require(conversationId, "conversationId"));
        updatedAt = Math.max(updatedAt, System.currentTimeMillis());
    }

    public synchronized void removeConversation(String conversationId) {
        if (conversationId != null) conversationIds.remove(conversationId);
        updatedAt = Math.max(updatedAt, System.currentTimeMillis());
    }

    public synchronized boolean hasConversation(String conversationId) {
        return conversationIds.contains(conversationId);
    }

    public synchronized Set<String> conversationIds() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(conversationIds));
    }

    public synchronized long updatedAt() { return updatedAt; }

    public synchronized Project copy() {
        Project copy = new Project(projectId, title, createdAt);
        copy.updatedAt = updatedAt;
        copy.conversationIds.addAll(conversationIds);
        return copy;
    }

    private static String require(String value, String name) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException(name + " is empty");
        return value.trim();
    }
}
