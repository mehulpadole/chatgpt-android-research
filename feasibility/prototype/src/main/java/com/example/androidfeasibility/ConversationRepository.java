package com.example.androidfeasibility;

public interface ConversationRepository {
    void save(Conversation conversation) throws Exception;
    Conversation load() throws Exception;
    void clear() throws Exception;
}
