package com.example.androidfeasibility;

import org.json.JSONArray;
import org.json.JSONObject;

public final class AndroidConversationCodec {
    private AndroidConversationCodec() {}

    public static JSONObject encode(Conversation conversation) throws Exception {
        JSONObject root = new JSONObject();
        root.put("id", conversation.id);
        root.put("title", conversation.title);
        root.put("updatedAt", conversation.updatedAt);
        JSONArray messages = new JSONArray();
        for (Message message : conversation.snapshotMessages()) {
            JSONObject item = new JSONObject();
            item.put("id", message.id);
            item.put("conversationId", message.conversationId);
            item.put("turnId", message.turnId);
            item.put("role", message.role.name());
            item.put("content", message.content);
            item.put("status", message.status.name());
            item.put("provider", message.provider);
            item.put("model", message.model);
            item.put("createdAt", message.createdAt);
            messages.put(item);
        }
        root.put("messages", messages);
        return root;
    }

    public static Conversation decode(JSONObject root) throws Exception {
        Conversation conversation = new Conversation(
                root.getString("id"), root.optString("title", "Restored conversation"),
                root.optLong("updatedAt", System.currentTimeMillis()));
        JSONArray messages = root.optJSONArray("messages");
        if (messages != null) {
            for (int i = 0; i < messages.length(); i++) {
                JSONObject item = messages.getJSONObject(i);
                conversation.messages.add(new Message(
                        item.getString("id"), item.getString("conversationId"),
                        item.getString("turnId"), Role.valueOf(item.getString("role")),
                        item.optString("content", ""),
                        MessageStatus.valueOf(item.getString("status")),
                        item.optString("provider", ""), item.optString("model", ""),
                        item.optLong("createdAt", System.currentTimeMillis())));
            }
        }
        return conversation;
    }
}
