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
            item.put("failureCategory", message.failureCategory);
            item.put("failureMessage", message.failureMessage);
            item.put("createdAt", message.createdAt);
            JSONArray parts = new JSONArray();
            if (message.contentParts.isEmpty() && message.content != null && !message.content.isEmpty()) {
                parts.put(new JSONObject().put("type", "text").put("text", message.content));
            } else {
                for (ContentPart part : message.snapshotContentParts()) parts.put(encodePart(part));
            }
            item.put("contentParts", parts);
            messages.put(item);
        }
        root.put("messages", messages);
        JSONArray attachments = new JSONArray();
        for (Attachment attachment : conversation.attachments) {
            JSONObject item = new JSONObject();
            item.put("attachmentId", attachment.attachmentId);
            item.put("conversationId", attachment.conversationId);
            item.put("messageId", attachment.messageId);
            item.put("displayName", attachment.displayName);
            item.put("detectedMimeType", attachment.detectedMimeType);
            item.put("declaredMimeType", attachment.declaredMimeType);
            item.put("byteSize", attachment.byteSize);
            item.put("localReference", attachment.localReference);
            item.put("state", attachment.state.name());
            item.put("createdAt", attachment.createdAt);
            attachments.put(item);
        }
        root.put("attachments", attachments);
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
                Message message = new Message(
                        item.getString("id"), item.getString("conversationId"),
                        item.getString("turnId"), Role.valueOf(item.getString("role")),
                        item.optString("content", ""),
                        MessageStatus.valueOf(item.getString("status")),
                        item.optString("provider", ""), item.optString("model", ""),
                        item.optLong("createdAt", System.currentTimeMillis()),
                        item.optString("failureCategory", ""),
                        item.optString("failureMessage", ""));
                JSONArray parts = item.optJSONArray("contentParts");
                if (parts == null) {
                    if (message.content != null && !message.content.isEmpty()) {
                        message.restoreContentPart(new TextPart(message.content));
                    }
                } else {
                    for (int partIndex = 0; partIndex < parts.length(); partIndex++) {
                        ContentPart part = decodePart(parts.getJSONObject(partIndex));
                        if (part != null) message.restoreContentPart(part);
                    }
                }
                conversation.messages.add(message);
            }
        }
        JSONArray attachments = root.optJSONArray("attachments");
        if (attachments != null) {
            for (int i = 0; i < attachments.length(); i++) {
                JSONObject item = attachments.getJSONObject(i);
                conversation.attachments.add(new Attachment(
                        item.getString("attachmentId"), item.optString("conversationId", conversation.id),
                        item.optString("messageId", ""), item.optString("displayName", "file"),
                        item.optString("detectedMimeType", "application/octet-stream"),
                        item.optString("declaredMimeType", ""), item.optLong("byteSize", 0L),
                        item.optString("localReference", ""),
                        AttachmentState.valueOf(item.optString("state", AttachmentState.READY.name())),
                        item.optLong("createdAt", System.currentTimeMillis()), null));
            }
        }
        return conversation;
    }

    private static JSONObject encodePart(ContentPart part) throws Exception {
        JSONObject item = new JSONObject().put("type", part.type());
        if (part instanceof TextPart) {
            item.put("text", ((TextPart) part).text);
        } else if (part instanceof ImagePart) {
            item.put("attachmentId", ((ImagePart) part).attachmentId);
            item.put("mimeType", ((ImagePart) part).mimeType);
        } else if (part instanceof FilePart) {
            FilePart file = (FilePart) part;
            item.put("attachmentId", file.attachmentId);
            item.put("displayName", file.displayName);
            item.put("mimeType", file.mimeType);
        }
        return item;
    }

    private static ContentPart decodePart(JSONObject item) throws Exception {
        String type = item.optString("type", "text");
        if ("text".equals(type)) return new TextPart(item.optString("text", ""));
        if ("image".equals(type)) return new ImagePart(item.getString("attachmentId"),
                item.optString("mimeType", "image/*"));
        if ("file".equals(type)) return new FilePart(item.getString("attachmentId"),
                item.optString("displayName", "file"), item.optString("mimeType", "application/octet-stream"));
        return null;
    }
}
