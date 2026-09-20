package com.example.androidfeasibility;

public final class StreamEvent {
    public enum Type { STARTED, DELTA, COMPLETED, ERROR }

    public final Type type;
    public final String turnId;
    public final String text;
    public final String error;

    private StreamEvent(Type type, String turnId, String text, String error) {
        this.type = type;
        this.turnId = turnId;
        this.text = text;
        this.error = error;
    }

    public static StreamEvent started(String turnId) {
        return new StreamEvent(Type.STARTED, turnId, "", "");
    }

    public static StreamEvent delta(String turnId, String text) {
        return new StreamEvent(Type.DELTA, turnId, text, "");
    }

    public static StreamEvent completed(String turnId) {
        return new StreamEvent(Type.COMPLETED, turnId, "", "");
    }

    public static StreamEvent error(String turnId, String error) {
        return new StreamEvent(Type.ERROR, turnId, "", error);
    }
}
