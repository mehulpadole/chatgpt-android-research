package com.example.androidfeasibility;

public final class SyncTransportException extends Exception {
    public final boolean authExpired;
    public final boolean retryable;

    public SyncTransportException(String message, boolean authExpired, boolean retryable) {
        super(message == null ? "sync transport failure" : message);
        this.authExpired = authExpired;
        this.retryable = retryable;
    }
}
