package com.example.androidfeasibility;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class ExportService {
    private ExportService() { }

    public static String toJson(Conversation conversation) {
        if (conversation == null) throw new IllegalArgumentException("conversation is null");
        StringBuilder json = new StringBuilder("{\"schemaVersion\":").append(SchemaVersion.CURRENT)
                .append(",\"id\":\"").append(escape(conversation.id))
                .append("\",\"title\":\"").append(escape(conversation.title))
                .append("\",\"updatedAt\":").append(conversation.updatedAt)
                .append(",\"messages\":[");
        for (int i = 0; i < conversation.messages.size(); i++) {
            if (i > 0) json.append(',');
            Message message = conversation.messages.get(i);
            json.append("{\"id\":\"").append(escape(message.id))
                    .append("\",\"conversationId\":\"").append(escape(message.conversationId))
                    .append("\",\"turnId\":\"").append(escape(message.turnId))
                    .append("\",\"role\":\"").append(message.role.name())
                    .append("\",\"content\":\"").append(escape(redact(message.content)))
                    .append("\",\"status\":\"").append(message.status.name())
                    .append("\",\"provider\":\"").append(escape(message.provider))
                    .append("\",\"model\":\"").append(escape(message.model))
                    .append("\",\"createdAt\":").append(message.createdAt).append('}');
        }
        json.append("],\"attachments\":[");
        for (int i = 0; i < conversation.attachments.size(); i++) {
            if (i > 0) json.append(',');
            Attachment attachment = conversation.attachments.get(i);
            json.append("{\"attachmentId\":\"").append(escape(attachment.attachmentId))
                    .append("\",\"conversationId\":\"").append(escape(attachment.conversationId))
                    .append("\",\"messageId\":\"").append(escape(attachment.messageId))
                    .append("\",\"displayName\":\"").append(escape(attachment.displayName))
                    .append("\",\"detectedMimeType\":\"").append(escape(attachment.detectedMimeType))
                    .append("\",\"declaredMimeType\":\"").append(escape(attachment.declaredMimeType))
                    .append("\",\"byteSize\":").append(attachment.byteSize)
                    .append(",\"localReference\":\"\",\"state\":\"").append(attachment.state.name())
                    .append("\",\"createdAt\":").append(attachment.createdAt).append('}');
        }
        return json.append("]}").toString();
    }

    public static String toJson(Conversation conversation, ProviderCredentialStore ignoredCredentials) {
        return toJson(conversation);
    }

    public static String toText(Conversation conversation) {
        StringBuilder text = new StringBuilder("MoCHi Android Export\n")
                .append("schemaVersion=").append(SchemaVersion.CURRENT).append('\n')
                .append("id=").append(line(conversation.id)).append('\n')
                .append("title=").append(line(conversation.title)).append('\n');
        for (Message message : conversation.messages) {
            text.append("message=").append(line(message.id)).append('\t')
                    .append(message.role.name()).append('\t').append(line(redact(message.content))).append('\n');
        }
        return text.toString();
    }

    public static byte[] toZip(Conversation conversation) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ZipOutputStream zip = new ZipOutputStream(bytes);
        zip.putNextEntry(new ZipEntry("conversation.json"));
        zip.write(toJson(conversation).getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
        zip.finish();
        zip.close();
        return bytes.toByteArray();
    }

    private static String line(String value) {
        return escape(value).replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    private static String redact(String value) {
        String result = Redaction.message(value == null ? "" : value);
        return result.replaceAll("(?i)sk-[a-z0-9_-]{8,}", "[REDACTED]");
    }

    private static String escape(String value) {
        String source = value == null ? "" : value;
        StringBuilder result = new StringBuilder(source.length() + 8);
        for (int i = 0; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == '\\' || c == '"') result.append('\\').append(c);
            else if (c == '\n') result.append("\\n");
            else if (c == '\r') result.append("\\r");
            else if (c == '\t') result.append("\\t");
            else result.append(c);
        }
        return result.toString();
    }
}
