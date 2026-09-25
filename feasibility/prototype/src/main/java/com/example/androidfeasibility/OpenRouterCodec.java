package com.example.androidfeasibility;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** OpenRouter transport codec. Its JSON types stay below the provider boundary. */
public final class OpenRouterCodec {
    private OpenRouterCodec() {}

    public static String encodeRequest(ProviderRequest request, boolean stream) {
        return encodeRequest(request, Collections.<PreparedAttachment>emptyList(), stream);
    }

    public static String encodeRequest(ProviderRequest request,
                                       List<PreparedAttachment> attachments, boolean stream) {
        if (request == null) throw new IllegalArgumentException("request is null");
        List<PreparedAttachment> prepared = attachments == null
                ? Collections.<PreparedAttachment>emptyList() : attachments;
        StringBuilder body = new StringBuilder();
        body.append("{\"model\":").append(quote(request.modelId));
        body.append(",\"messages\":[{\"role\":\"user\",\"content\":");
        if (prepared.isEmpty()) {
            body.append(quote(request.prompt));
        } else {
            body.append('[').append("{\"type\":\"text\",\"text\":")
                    .append(quote(request.prompt)).append('}');
            for (PreparedAttachment attachment : prepared) {
                body.append(",{\"type\":\"image_url\",\"image_url\":{\"url\":")
                        .append(quote(attachment.dataUrl)).append("}}");
            }
            body.append(']');
        }
        body.append("}]");
        body.append(",\"stream\":").append(stream ? "true" : "false");
        body.append(",\"metadata\":{");
        body.append("\"conversation_id\":").append(quote(request.conversationId));
        body.append(",\"turn_id\":").append(quote(request.turnId));
        body.append(",\"user_message_id\":").append(quote(request.userMessageId));
        body.append(",\"assistant_message_id\":").append(quote(request.assistantMessageId));
        for (Map.Entry<String, String> entry : request.metadata.entrySet()) {
            body.append(',').append(quote(entry.getKey())).append(':').append(quote(entry.getValue()));
        }
        body.append("}}");
        return body.toString();
    }

    public static Chunk parseSseData(String line) throws ProtocolException {
        if (line == null) throw new ProtocolException("null SSE line");
        String data = line.trim();
        if (data.startsWith("data:")) data = data.substring(5).trim();
        if ("[DONE]".equals(data)) return Chunk.done();
        if (data.isEmpty()) throw new ProtocolException("empty SSE data");
        Map<String, Object> root = object(new JsonReader(data).parse(), "SSE root");
        String requestId = string(root.get("id"));
        String model = string(root.get("model"));
        String delta = "";
        String finishReason = "";
        Object choicesValue = root.get("choices");
        if (choicesValue instanceof List && !((List<?>) choicesValue).isEmpty()) {
            Map<String, Object> choice = object(((List<?>) choicesValue).get(0), "choice");
            Map<String, Object> deltaObject = optionalObject(choice.get("delta"));
            if (deltaObject != null) delta = string(deltaObject.get("content"));
            finishReason = string(choice.get("finish_reason"));
        }
        Map<String, String> attributes = new HashMap<>();
        if (!model.isEmpty()) attributes.put("model", model);
        if (!finishReason.isEmpty()) attributes.put("finish_reason", finishReason);
        Map<String, Object> usage = optionalObject(root.get("usage"));
        if (usage != null) {
            copyNumber(usage, attributes, "prompt_tokens");
            copyNumber(usage, attributes, "completion_tokens");
            copyNumber(usage, attributes, "total_tokens");
        }
        return new Chunk(false, delta, requestId, model, finishReason,
                new ProviderMetadata(requestId, attributes));
    }

    public static ProviderModel parseModel(String json) throws ProtocolException {
        Map<String, Object> root = object(new JsonReader(json).parse(), "model root");
        Map<String, Object> data = optionalObject(root.get("data"));
        if (data != null) root = data;
        String id = string(root.get("id"));
        if (id.isEmpty()) throw new ProtocolException("model is missing id");
        String displayName = string(root.get("name"));
        Set<String> capabilities = new HashSet<>();
        Map<String, Object> architecture = optionalObject(root.get("architecture"));
        addModalities(capabilities, architecture == null ? null : architecture.get("input_modalities"));
        addModalities(capabilities, architecture == null ? null : architecture.get("output_modalities"));
        List<?> parameters = list(root.get("supported_parameters"));
        if (parameters != null) {
            if (contains(parameters, "tools")) capabilities.add(ProviderCapabilities.TOOLS);
            if (contains(parameters, "reasoning") || contains(parameters, "reasoning_effort")) {
                capabilities.add(ProviderCapabilities.REASONING);
            }
            if (contains(parameters, "stream")) capabilities.add(ProviderCapabilities.STREAMING);
        }
        if (!capabilities.contains(ProviderCapabilities.STREAMING)) {
            capabilities.add(ProviderCapabilities.STREAMING);
        }
        if (!capabilities.contains(ProviderCapabilities.TEXT)) capabilities.add(ProviderCapabilities.TEXT);
        return new ProviderModel("openrouter", id, displayName, capabilities,
                number(root.get("context_length")));
    }

    public static ProviderError mapHttpError(int status, String responseBody) {
        String message = errorMessage(responseBody);
        switch (status) {
            case 401:
            case 403:
                return new ProviderError(ProviderError.Category.AUTHENTICATION,
                        messageOr(message, "authentication required"), false);
            case 400:
            case 422:
                return new ProviderError(ProviderError.Category.INVALID_REQUEST,
                        messageOr(message, "invalid provider request"), false);
            case 404:
                return new ProviderError(ProviderError.Category.MODEL_UNAVAILABLE,
                        messageOr(message, "model unavailable"), false);
            case 408:
            case 504:
                return new ProviderError(ProviderError.Category.TIMEOUT,
                        messageOr(message, "provider request timed out"), true);
            case 429:
                return new ProviderError(ProviderError.Category.RATE_LIMIT,
                        messageOr(message, "rate limited"), true);
            default:
                if (status >= 500) {
                    return new ProviderError(ProviderError.Category.PROVIDER_UNAVAILABLE,
                            messageOr(message, "provider unavailable"), true);
                }
                return new ProviderError(ProviderError.Category.PROVIDER,
                        messageOr(message, "provider HTTP failure"), false);
        }
    }

    private static String errorMessage(String body) {
        if (body == null || body.isEmpty()) return "";
        try {
            Map<String, Object> root = object(new JsonReader(body).parse(), "error root");
            Map<String, Object> error = optionalObject(root.get("error"));
            String message = error == null ? string(root.get("message")) : string(error.get("message"));
            return sanitize(message);
        } catch (ProtocolException ignored) {
            return sanitize(body.trim());
        }
    }

    private static String sanitize(String value) {
        if (value == null) return "";
        String sanitized = value.replaceAll("(?i)bearer\\s+[^\\s,]+", "[REDACTED]");
        return sanitized.replaceAll("(?i)authorization\\s*[:=]\\s*[^,;\\s]+", "authorization=[REDACTED]");
    }

    private static String messageOr(String value, String fallback) {
        return value == null || value.isEmpty() ? fallback : value;
    }

    private static void addModalities(Set<String> capabilities, Object value) {
        List<?> modalities = list(value);
        if (modalities == null) return;
        for (Object item : modalities) {
            String modality = string(item);
            if ("text".equalsIgnoreCase(modality)) capabilities.add(ProviderCapabilities.TEXT);
            if ("image".equalsIgnoreCase(modality)) capabilities.add(ProviderCapabilities.VISION);
        }
    }

    private static boolean contains(List<?> values, String expected) {
        for (Object value : values) if (expected.equals(string(value))) return true;
        return false;
    }

    private static void copyNumber(Map<String, Object> source, Map<String, String> target, String key) {
        Object value = source.get(key);
        if (value != null) target.put(key, numberString(value));
    }

    private static String numberString(Object value) {
        if (value instanceof Number) {
            double numeric = ((Number) value).doubleValue();
            if (numeric == Math.rint(numeric)) return Long.toString((long) numeric);
        }
        return String.valueOf(value);
    }

    private static String quote(String value) {
        String input = value == null ? "" : value;
        StringBuilder result = new StringBuilder(input.length() + 8).append('"');
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            switch (c) {
                case '"': result.append("\\\""); break;
                case '\\': result.append("\\\\"); break;
                case '\n': result.append("\\n"); break;
                case '\r': result.append("\\r"); break;
                case '\t': result.append("\\t"); break;
                case '\b': result.append("\\b"); break;
                case '\f': result.append("\\f"); break;
                default:
                    if (c < 0x20) result.append(String.format("\\u%04x", (int) c));
                    else result.append(c);
            }
        }
        return result.append('"').toString();
    }

    private static String string(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static long number(Object value) {
        if (value instanceof Number) return ((Number) value).longValue();
        try { return value == null ? 0L : Long.parseLong(String.valueOf(value)); }
        catch (NumberFormatException ignored) { return 0L; }
    }

    private static List<?> list(Object value) {
        return value instanceof List ? (List<?>) value : null;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> optionalObject(Object value) {
        return value instanceof Map ? (Map<String, Object>) value : null;
    }

    private static Map<String, Object> object(Object value, String label) throws ProtocolException {
        Map<String, Object> result = optionalObject(value);
        if (result == null) throw new ProtocolException(label + " is not an object");
        return result;
    }

    public static final class Chunk {
        public final boolean done;
        public final String delta;
        public final String requestId;
        public final String model;
        public final String finishReason;
        public final ProviderMetadata metadata;

        private Chunk(boolean done, String delta, String requestId, String model,
                      String finishReason, ProviderMetadata metadata) {
            this.done = done;
            this.delta = delta == null ? "" : delta;
            this.requestId = requestId == null ? "" : requestId;
            this.model = model == null ? "" : model;
            this.finishReason = finishReason == null ? "" : finishReason;
            this.metadata = metadata == null ? ProviderMetadata.empty() : metadata;
        }

        static Chunk done() {
            return new Chunk(true, "", "", "", "", ProviderMetadata.empty());
        }
    }

    public static final class ProtocolException extends Exception {
        public ProtocolException(String message) { super(message); }
    }

    private static final class JsonReader {
        private final String input;
        private int position;

        JsonReader(String input) { this.input = input == null ? "" : input; }

        Object parse() throws ProtocolException {
            skipWhitespace();
            Object value = parseValue();
            skipWhitespace();
            if (position != input.length()) throw new ProtocolException("trailing JSON data");
            return value;
        }

        private Object parseValue() throws ProtocolException {
            skipWhitespace();
            if (position >= input.length()) throw new ProtocolException("missing JSON value");
            char c = input.charAt(position);
            if (c == '{') return parseObject();
            if (c == '[') return parseArray();
            if (c == '"') return parseString();
            if (input.startsWith("true", position)) { position += 4; return Boolean.TRUE; }
            if (input.startsWith("false", position)) { position += 5; return Boolean.FALSE; }
            if (input.startsWith("null", position)) { position += 4; return null; }
            return parseNumber();
        }

        private Map<String, Object> parseObject() throws ProtocolException {
            expect('{');
            Map<String, Object> result = new HashMap<>();
            skipWhitespace();
            if (consume('}')) return result;
            while (true) {
                skipWhitespace();
                String key = parseString();
                skipWhitespace();
                expect(':');
                result.put(key, parseValue());
                skipWhitespace();
                if (consume('}')) return result;
                expect(',');
            }
        }

        private List<Object> parseArray() throws ProtocolException {
            expect('[');
            List<Object> result = new ArrayList<>();
            skipWhitespace();
            if (consume(']')) return result;
            while (true) {
                result.add(parseValue());
                skipWhitespace();
                if (consume(']')) return result;
                expect(',');
            }
        }

        private String parseString() throws ProtocolException {
            expect('"');
            StringBuilder result = new StringBuilder();
            while (position < input.length()) {
                char c = input.charAt(position++);
                if (c == '"') return result.toString();
                if (c != '\\') {
                    result.append(c);
                    continue;
                }
                if (position >= input.length()) throw new ProtocolException("unterminated JSON escape");
                char escaped = input.charAt(position++);
                switch (escaped) {
                    case '"': result.append('"'); break;
                    case '\\': result.append('\\'); break;
                    case '/': result.append('/'); break;
                    case 'b': result.append('\b'); break;
                    case 'f': result.append('\f'); break;
                    case 'n': result.append('\n'); break;
                    case 'r': result.append('\r'); break;
                    case 't': result.append('\t'); break;
                    case 'u': result.append(parseUnicode()); break;
                    default: throw new ProtocolException("unsupported JSON escape");
                }
            }
            throw new ProtocolException("unterminated JSON string");
        }

        private char parseUnicode() throws ProtocolException {
            if (position + 4 > input.length()) throw new ProtocolException("short unicode escape");
            String hex = input.substring(position, position + 4);
            position += 4;
            try { return (char) Integer.parseInt(hex, 16); }
            catch (NumberFormatException error) { throw new ProtocolException("invalid unicode escape"); }
        }

        private Number parseNumber() throws ProtocolException {
            int start = position;
            while (position < input.length()) {
                char c = input.charAt(position);
                if ((c >= '0' && c <= '9') || c == '-' || c == '+' || c == '.' || c == 'e' || c == 'E') {
                    position++;
                } else break;
            }
            if (start == position) throw new ProtocolException("invalid JSON value");
            String value = input.substring(start, position);
            try {
                return value.indexOf('.') >= 0 || value.indexOf('e') >= 0 || value.indexOf('E') >= 0
                        ? Double.valueOf(value) : Long.valueOf(value);
            } catch (NumberFormatException error) {
                throw new ProtocolException("invalid JSON number");
            }
        }

        private void expect(char expected) throws ProtocolException {
            if (position >= input.length() || input.charAt(position) != expected) {
                throw new ProtocolException("expected '" + expected + "'");
            }
            position++;
        }

        private boolean consume(char value) {
            if (position < input.length() && input.charAt(position) == value) {
                position++;
                return true;
            }
            return false;
        }

        private void skipWhitespace() {
            while (position < input.length() && Character.isWhitespace(input.charAt(position))) position++;
        }
    }
}
