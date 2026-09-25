package com.example.androidfeasibility;

import java.util.List;

public interface MemoryRepository {
    void save(MemoryEntry entry) throws Exception;
    List<MemoryEntry> list(MemoryEntry.Scope scope, String projectId) throws Exception;
    void delete(String memoryId) throws Exception;
}
