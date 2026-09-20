package com.example.androidfeasibility;

public final class InMemoryConversationRepository implements ConversationRepository {
    private Conversation stored;

    @Override public synchronized void save(Conversation conversation) {
        stored = conversation.copy();
    }

    @Override public synchronized Conversation load() {
        return stored == null ? null : stored.copy();
    }

    @Override public synchronized void clear() {
        stored = null;
    }
}
