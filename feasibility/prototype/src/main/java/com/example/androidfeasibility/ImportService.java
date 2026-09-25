package com.example.androidfeasibility;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class ImportService {
    private static final int MAX_ENTRY_BYTES = 10 * 1024 * 1024;

    private ImportService() { }

    public static Conversation importJson(String payload) {
        return importJson(payload, new HashSet<String>(), new HashSet<String>());
    }

    public static Conversation importJson(String payload, Set<String> existingConversationIds,
                                          Set<String> existingMessageIds) {
        try {
            String migrated = MigrationService.migrateJson(payload);
            Object parsed = new JsonParser(migrated).parse();
            if (!(parsed instanceof Map)) throw new IllegalArgumentException("JSON object required");
            Map<?, ?> root = (Map<?, ?>) parsed;
            int version = (int) number(root.get("schemaVersion"), SchemaVersion.CURRENT);
            if (version > SchemaVersion.CURRENT) throw new IllegalArgumentException("unsupported schema version");
            String conversationId = string(root.get("id"), "");
            if (conversationId.isEmpty()) throw new IllegalArgumentException("conversation ID is empty");
            if (existingConversationIds.contains(conversationId)) throw new IllegalArgumentException("duplicate conversation ID");
            Conversation conversation = new Conversation(conversationId,
                    string(root.get("title"), "Restored conversation"), number(root.get("updatedAt"), 0L));
            Object messagesValue = root.get("messages");
            if (messagesValue instanceof List) {
                for (Object value : (List<?>) messagesValue) {
                    if (!(value instanceof Map)) throw new IllegalArgumentException("message object required");
                    Map<?, ?> item = (Map<?, ?>) value;
                    String messageId = string(item.get("id"), "");
                    if (messageId.isEmpty() || existingMessageIds.contains(messageId)) {
                        throw new IllegalArgumentException("duplicate or empty message ID");
                    }
                    Message message = new Message(messageId,
                            string(item.get("conversationId"), conversation.id), string(item.get("turnId"), ""),
                            role(item.get("role")), string(item.get("content"), ""),
                            messageStatus(item.get("status")), string(item.get("provider"), ""),
                            string(item.get("model"), ""), number(item.get("createdAt"), 0L));
                    if (!message.content.isEmpty()) message.restoreContentPart(new TextPart(message.content));
                    conversation.messages.add(message);
                    existingMessageIds.add(messageId);
                }
            }
            Object attachmentsValue = root.get("attachments");
            Set<String> attachmentIds = new HashSet<>();
            if (attachmentsValue instanceof List) {
                for (Object value : (List<?>) attachmentsValue) {
                    if (!(value instanceof Map)) throw new IllegalArgumentException("attachment object required");
                    Map<?, ?> item = (Map<?, ?>) value;
                    String attachmentId = string(item.get("attachmentId"), "");
                    if (attachmentId.isEmpty() || !attachmentIds.add(attachmentId)) {
                        throw new IllegalArgumentException("duplicate or empty attachment ID");
                    }
                    conversation.attachments.add(new Attachment(
                            attachmentId, string(item.get("conversationId"), conversation.id),
                            string(item.get("messageId"), ""), string(item.get("displayName"), "file"),
                            string(item.get("detectedMimeType"), "application/octet-stream"),
                            string(item.get("declaredMimeType"), ""), number(item.get("byteSize"), 0L), "",
                            attachmentState(item.get("state")), number(item.get("createdAt"), 0L), null));
                }
            }
            existingConversationIds.add(conversationId);
            return conversation;
        } catch (IllegalArgumentException error) {
            throw error;
        } catch (Exception error) {
            throw new IllegalArgumentException("invalid export", error);
        }
    }

    public static Conversation importText(String payload) {
        if (payload == null) throw new IllegalArgumentException("payload is null");
        String[] lines = payload.split("\\r?\\n");
        String id = "";
        String title = "Restored conversation";
        List<String[]> messages = new ArrayList<>();
        for (String line : lines) {
            if (line.startsWith("id=")) id = unescape(line.substring(3));
            else if (line.startsWith("title=")) title = unescape(line.substring(6));
            else if (line.startsWith("message=")) {
                String[] fields = line.substring(8).split("\\t", 3);
                if (fields.length != 3) throw new IllegalArgumentException("invalid text message");
                messages.add(new String[]{unescape(fields[0]), fields[1], unescape(fields[2])});
            }
        }
        if (id.isEmpty()) throw new IllegalArgumentException("text export has no conversation ID");
        Conversation conversation = new Conversation(id, title, 0L);
        Set<String> seen = new HashSet<>();
        for (String[] item : messages) {
            if (!seen.add(item[0])) throw new IllegalArgumentException("duplicate message ID");
            Message message = new Message(item[0], id, "", role(item[1]), item[2],
                    MessageStatus.COMPLETED, "", "", 0L);
            if (!item[2].isEmpty()) message.restoreContentPart(new TextPart(item[2]));
            conversation.messages.add(message);
        }
        return conversation;
    }

    public static Conversation importZip(byte[] archive) throws Exception {
        if (archive == null || archive.length == 0) throw new IllegalArgumentException("archive is empty");
        String conversationJson = null;
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(archive))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                String name = safeEntryName(entry.getName());
                byte[] data = readLimited(zip);
                if ("conversation.json".equals(name)) {
                    if (conversationJson != null) throw new IllegalArgumentException("duplicate conversation.json");
                    conversationJson = new String(data, StandardCharsets.UTF_8);
                } else if (name.startsWith("attachments/")) {
                    validateAttachmentName(name);
                } else {
                    throw new IllegalArgumentException("unsupported ZIP entry");
                }
                zip.closeEntry();
            }
        }
        if (conversationJson == null) throw new IllegalArgumentException("conversation.json is missing");
        return importJson(conversationJson);
    }

    private static String safeEntryName(String value) {
        if (value == null || value.isEmpty() || value.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("invalid ZIP path");
        }
        String normalized = value.replace('\\', '/');
        if (normalized.startsWith("/") || normalized.startsWith("../") || normalized.contains("/../")
                || normalized.endsWith("/..")) throw new IllegalArgumentException("unsafe ZIP path");
        return normalized;
    }

    private static void validateAttachmentName(String name) {
        String file = name.substring("attachments/".length());
        if (file.isEmpty() || file.contains("/") || file.contains("..")) {
            throw new IllegalArgumentException("unsafe attachment path");
        }
        int dot = file.lastIndexOf('.');
        String extension = dot < 0 ? "" : file.substring(dot + 1).toLowerCase();
        if (!(extension.equals("png") || extension.equals("jpg") || extension.equals("jpeg")
                || extension.equals("gif") || extension.equals("pdf") || extension.equals("txt")
                || extension.equals("md") || extension.equals("json"))) {
            throw new IllegalArgumentException("unsupported attachment file type");
        }
    }

    private static byte[] readLimited(InputStream input) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int count;
        while ((count = input.read(buffer)) != -1) {
            if (bytes.size() + count > MAX_ENTRY_BYTES) throw new IllegalArgumentException("ZIP entry is too large");
            bytes.write(buffer, 0, count);
        }
        return bytes.toByteArray();
    }

    private static Role role(Object value) {
        try { return Role.valueOf(string(value, Role.USER.name())); }
        catch (IllegalArgumentException error) { throw new IllegalArgumentException("invalid message role"); }
    }

    private static MessageStatus messageStatus(Object value) {
        try { return MessageStatus.valueOf(string(value, MessageStatus.COMPLETED.name())); }
        catch (IllegalArgumentException error) { throw new IllegalArgumentException("invalid message status"); }
    }

    private static AttachmentState attachmentState(Object value) {
        try { return AttachmentState.valueOf(string(value, AttachmentState.READY.name())); }
        catch (IllegalArgumentException error) { throw new IllegalArgumentException("invalid attachment state"); }
    }

    private static long number(Object value, long fallback) {
        return value instanceof Number ? ((Number) value).longValue() : fallback;
    }

    private static String string(Object value, String fallback) {
        return value == null ? fallback : String.valueOf(value);
    }

    private static String unescape(String value) {
        return value.replace("\\n", "\n").replace("\\r", "\r").replace("\\t", "\t").replace("\\\\", "\\");
    }

    private static final class JsonParser {
        private final String input;
        private int index;

        JsonParser(String input) { this.input = input; }

        Object parse() {
            skipWhitespace();
            Object value = value();
            skipWhitespace();
            if (index != input.length()) throw new IllegalArgumentException("trailing JSON data");
            return value;
        }

        private Object value() {
            skipWhitespace();
            if (index >= input.length()) throw new IllegalArgumentException("JSON value missing");
            char c = input.charAt(index);
            if (c == '{') return object();
            if (c == '[') return array();
            if (c == '"') return quoted();
            if (input.startsWith("true", index)) { index += 4; return Boolean.TRUE; }
            if (input.startsWith("false", index)) { index += 5; return Boolean.FALSE; }
            if (input.startsWith("null", index)) { index += 4; return null; }
            return numberValue();
        }

        private Map<String, Object> object() {
            Map<String, Object> result = new HashMap<>();
            index++;
            skipWhitespace();
            if (consume('}')) return result;
            while (true) {
                String key = quoted();
                skipWhitespace();
                if (!consume(':')) throw new IllegalArgumentException("JSON object separator missing");
                result.put(key, value());
                skipWhitespace();
                if (consume('}')) return result;
                if (!consume(',')) throw new IllegalArgumentException("JSON object delimiter missing");
            }
        }

        private List<Object> array() {
            List<Object> result = new ArrayList<>();
            index++;
            skipWhitespace();
            if (consume(']')) return result;
            while (true) {
                result.add(value());
                skipWhitespace();
                if (consume(']')) return result;
                if (!consume(',')) throw new IllegalArgumentException("JSON array delimiter missing");
            }
        }

        private String quoted() {
            if (!consume('"')) throw new IllegalArgumentException("JSON string required");
            StringBuilder result = new StringBuilder();
            boolean escaped = false;
            while (index < input.length()) {
                char c = input.charAt(index++);
                if (escaped) {
                    if (c == 'n') result.append('\n');
                    else if (c == 'r') result.append('\r');
                    else if (c == 't') result.append('\t');
                    else result.append(c);
                    escaped = false;
                } else if (c == '\\') escaped = true;
                else if (c == '"') return result.toString();
                else result.append(c);
            }
            throw new IllegalArgumentException("unterminated JSON string");
        }

        private Number numberValue() {
            int start = index;
            while (index < input.length() && "-0123456789.eE+".indexOf(input.charAt(index)) >= 0) index++;
            if (start == index) throw new IllegalArgumentException("invalid JSON number");
            String value = input.substring(start, index);
            return value.indexOf('.') >= 0 || value.indexOf('e') >= 0 || value.indexOf('E') >= 0
                    ? Double.valueOf(value) : Long.valueOf(value);
        }

        private boolean consume(char expected) {
            if (index < input.length() && input.charAt(index) == expected) { index++; return true; }
            return false;
        }

        private void skipWhitespace() {
            while (index < input.length() && Character.isWhitespace(input.charAt(index))) index++;
        }
    }
}
