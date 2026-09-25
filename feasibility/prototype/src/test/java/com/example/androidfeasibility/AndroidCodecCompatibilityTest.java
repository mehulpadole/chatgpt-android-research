package com.example.androidfeasibility;

import org.json.JSONArray;
import org.json.JSONObject;

public final class AndroidCodecCompatibilityTest {
    public static void main(String[] args) throws Exception {
        testPhase4RecordWithoutFailureFieldsStillDecodes();
        testPhase5FailureFieldsRoundTrip();
        System.out.println("ANDROID CODEC COMPATIBILITY TESTS PASSED");
    }

    private static void testPhase4RecordWithoutFailureFieldsStillDecodes() throws Exception {
        JSONObject root = oldRecord();
        Conversation decoded = AndroidConversationCodec.decode(root);
        Message assistant = decoded.messages.get(1);
        check(assistant.failureCategory.isEmpty(), "old record must default failure category");
        check(assistant.failureMessage.isEmpty(), "old record must default failure message");
    }

    private static void testPhase5FailureFieldsRoundTrip() throws Exception {
        Conversation conversation = Conversation.empty();
        conversation.add(new Message("user", conversation.id, "turn", Role.USER,
                "hello", MessageStatus.COMPLETED, "provider", "model", 1L));
        conversation.add(new Message("assistant", conversation.id, "turn", Role.ASSISTANT,
                "partial", MessageStatus.FAILED, "provider", "model", 2L,
                ProviderError.Category.NETWORK.name(), "offline"));
        JSONObject encoded = AndroidConversationCodec.encode(conversation);
        Conversation decoded = AndroidConversationCodec.decode(encoded);
        Message assistant = decoded.messages.get(1);
        check(assistant.failureCategory.equals(ProviderError.Category.NETWORK.name()),
                "failure category must round-trip");
        check(assistant.failureMessage.equals("offline"), "failure message must round-trip");
    }

    private static JSONObject oldRecord() throws Exception {
        JSONObject root = new JSONObject();
        root.put("id", "conversation");
        root.put("title", "Old");
        root.put("updatedAt", 1L);
        JSONArray messages = new JSONArray();
        messages.put(message("user", "USER", "hello", "COMPLETED"));
        messages.put(message("assistant", "ASSISTANT", "", "STREAMING"));
        root.put("messages", messages);
        return root;
    }

    private static JSONObject message(String id, String role, String content, String status)
            throws Exception {
        JSONObject message = new JSONObject();
        message.put("id", id);
        message.put("conversationId", "conversation");
        message.put("turnId", "turn");
        message.put("role", role);
        message.put("content", content);
        message.put("status", status);
        message.put("provider", "provider");
        message.put("model", "model");
        message.put("createdAt", 1L);
        return message;
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
