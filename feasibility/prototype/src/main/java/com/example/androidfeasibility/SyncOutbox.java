package com.example.androidfeasibility;

import java.util.List;

public interface SyncOutbox {
    void enqueue(SyncOperation operation) throws Exception;
    List<SyncOperation> pending() throws Exception;
    void markSucceeded(String operationId) throws Exception;
    void markFailed(String operationId, boolean quota) throws Exception;
    SyncCursor cursor() throws Exception;
    void setCursor(SyncCursor cursor) throws Exception;
}
