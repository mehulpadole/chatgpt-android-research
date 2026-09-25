package com.example.androidfeasibility;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** Transport-private codec for the intentionally small Phase 5 NDJSON protocol. */
public final class NdjsonCodec {
    private NdjsonCodec() {}

    public static String encodeRequest(ProviderRequest request, String scenario) {
        return "{"
                + field("conversation_id", request.conversationId) + ","
                + field("turn_id", request.turnId) + ","
                + field("user_message_id", request.userMessageId) + ","
                + field("assistant_message_id", request.assistantMessageId) + ","
                + field("prompt", request.prompt) + ","
                + field("provider_id", request.providerId) + ","
                + field("model_id", request.modelId) + ","
                + field("scenario", scenario == null ? "NORMAL" : scenario)
                + "}";
    }

    public static Frame parseFrame(String line) throws ProtocolException {
        if (line == null || line.trim().isEmpty()) throw new ProtocolException("empty NDJSON frame");
        Map<String, String> fields = new Parser(line).parseObject();
        String type = required(fields, "type");
        String turnId = required(fields, "turn_id");
        return new Frame(type, turnId, fields.get("text"), fields.get("request_id"),
                fields.get("category"), fields.get("message"),
                "true".equalsIgnoreCase(fields.get("retryable")));
    }

    private static String field(String name, String value) {
        return "\"" + escape(name) + "\":\"" + escape(value == null ? "" : value) + "\"";
    }

    private static String required(Map<String, String> fields, String key) throws ProtocolException {
        String value = fields.get(key);
        if (value == null || value.isEmpty()) throw new ProtocolException("missing " + key);
        return value;
    }

    private static String escape(String value) {
        StringBuilder result = new StringBuilder(value.length() + 8);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
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
        return result.toString();
    }

    public static final class Frame {
        public final String type;
        public final String turnId;
        public final String text;
        public final String requestId;
        public final String category;
        public final String message;
        public final boolean retryable;

        Frame(String type, String turnId, String text, String requestId,
              String category, String message, boolean retryable) {
            this.type = type;
            this.turnId = turnId;
            this.text = text == null ? "" : text;
            this.requestId = requestId == null ? "" : requestId;
            this.category = category == null ? "" : category;
            this.message = message == null ? "" : message;
            this.retryable = retryable;
        }
    }

    public static final class ProtocolException extends Exception {
        public ProtocolException(String message) {
            super(message);
        }
    }

    private static final class Parser {
        private final String input;
        private int position;

        Parser(String input) {
            this.input = input;
        }

        Map<String, String> parseObject() throws ProtocolException {
            skipWhitespace();
            expect('{');
            Map<String, String> result = new HashMap<>();
            skipWhitespace();
            if (consume('}')) return finish(result);
            while (true) {
                String key = parseString();
                skipWhitespace();
                expect(':');
                skipWhitespace();
                result.put(key, parseScalar());
                skipWhitespace();
                if (consume('}')) return finish(result);
                expect(',');
                skipWhitespace();
            }
        }

        private Map<String, String> finish(Map<String, String> result) throws ProtocolException {
            skipWhitespace();
            if (position != input.length()) throw new ProtocolException("trailing data");
            return Collections.unmodifiableMap(result);
        }

        private String parseScalar() throws ProtocolException {
            if (position >= input.length()) throw new ProtocolException("missing value");
            if (input.charAt(position) == '"') return parseString();
            int start = position;
            while (position < input.length()) {
                char c = input.charAt(position);
                if (c == ',' || c == '}' || Character.isWhitespace(c)) break;
                position++;
            }
            if (start == position) throw new ProtocolException("empty value");
            String value = input.substring(start, position);
            if ("null".equals(value)) return "";
            if ("true".equals(value) || "false".equals(value) || isNumber(value)) return value;
            throw new ProtocolException("unsupported JSON value");
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
                if (position >= input.length()) throw new ProtocolException("unterminated escape");
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
                    default: throw new ProtocolException("unsupported escape");
                }
            }
            throw new ProtocolException("unterminated string");
        }

        private char parseUnicode() throws ProtocolException {
            if (position + 4 > input.length()) throw new ProtocolException("short unicode escape");
            String hex = input.substring(position, position + 4);
            position += 4;
            try {
                return (char) Integer.parseInt(hex, 16);
            } catch (NumberFormatException error) {
                throw new ProtocolException("invalid unicode escape");
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

        private boolean isNumber(String value) {
            try {
                Double.parseDouble(value);
                return true;
            } catch (NumberFormatException error) {
                return false;
            }
        }
    }
}
