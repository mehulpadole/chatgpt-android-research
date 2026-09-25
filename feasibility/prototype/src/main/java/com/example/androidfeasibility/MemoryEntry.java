package com.example.androidfeasibility;

public final class MemoryEntry {
    public enum Scope { GLOBAL, PROJECT }

    public final String memoryId;
    public final Scope scope;
    public final String projectId;
    public final String content;
    public final boolean enabled;
    public final long createdAt;

    public MemoryEntry(String memoryId, Scope scope, String projectId, String content,
                       boolean enabled, long createdAt) {
        this.memoryId = require(memoryId, "memoryId");
        this.scope = scope == null ? Scope.GLOBAL : scope;
        this.projectId = projectId == null ? "" : projectId;
        this.content = require(content, "content");
        this.enabled = enabled;
        this.createdAt = createdAt;
        if (this.scope == Scope.PROJECT && this.projectId.isEmpty()) {
            throw new IllegalArgumentException("project memory requires projectId");
        }
        if (this.scope == Scope.GLOBAL && !this.projectId.isEmpty()) {
            throw new IllegalArgumentException("global memory must not have projectId");
        }
    }

    public MemoryEntry copy() {
        return new MemoryEntry(memoryId, scope, projectId, content, enabled, createdAt);
    }

    private static String require(String value, String name) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException(name + " is empty");
        return value.trim();
    }
}
