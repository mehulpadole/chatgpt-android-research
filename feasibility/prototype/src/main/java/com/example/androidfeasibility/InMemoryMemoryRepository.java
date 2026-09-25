package com.example.androidfeasibility;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class InMemoryMemoryRepository implements MemoryRepository {
    private final Map<String, MemoryEntry> entries = new LinkedHashMap<>();

    @Override public synchronized void save(MemoryEntry entry) {
        if (entry == null) throw new IllegalArgumentException("memory entry is null");
        entries.put(entry.memoryId, entry.copy());
    }

    @Override public synchronized List<MemoryEntry> list(MemoryEntry.Scope scope, String projectId) {
        List<MemoryEntry> result = new ArrayList<>();
        if (scope == null) return result;
        for (MemoryEntry entry : entries.values()) {
            if (entry.scope != scope || !entry.enabled) continue;
            if (scope == MemoryEntry.Scope.GLOBAL
                    || entry.projectId.equals(projectId == null ? "" : projectId)) {
                result.add(entry.copy());
            }
        }
        return result;
    }

    @Override public synchronized void delete(String memoryId) { entries.remove(memoryId); }
}
