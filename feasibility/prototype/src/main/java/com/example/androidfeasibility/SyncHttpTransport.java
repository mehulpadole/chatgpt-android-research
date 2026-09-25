package com.example.androidfeasibility;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** HTTP transport for the local/staging sync contract; it never persists its auth token. */
public final class SyncHttpTransport implements SyncClient.Transport {
    public interface AuthTokenProvider {
        String token();
    }

    private final URL baseUrl;
    private final AuthTokenProvider tokenProvider;
    private final int connectTimeoutMillis;
    private final int readTimeoutMillis;

    public SyncHttpTransport(URL baseUrl, AuthTokenProvider tokenProvider) {
        this(baseUrl, tokenProvider, 5_000, 10_000);
    }

    public SyncHttpTransport(URL baseUrl, AuthTokenProvider tokenProvider,
                             int connectTimeoutMillis, int readTimeoutMillis) {
        if (baseUrl == null) throw new IllegalArgumentException("baseUrl is null");
        this.baseUrl = baseUrl;
        this.tokenProvider = tokenProvider;
        this.connectTimeoutMillis = Math.max(1, connectTimeoutMillis);
        this.readTimeoutMillis = Math.max(1, readTimeoutMillis);
    }

    @Override public SyncClient.PushResult push(List<SyncOperation> operations) throws Exception {
        HttpURLConnection connection = open("v1/sync/push", "POST");
        try {
            byte[] body = encodePush(operations).getBytes(StandardCharsets.UTF_8);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            connection.setRequestProperty("Accept", "application/json");
            try (OutputStream output = connection.getOutputStream()) {
                output.write(body);
            }
            int status = connection.getResponseCode();
            if (status >= 200 && status < 300) return SyncClient.PushResult.accepted();
            if (status == 401 || status == 403) return SyncClient.PushResult.authExpired("sync authentication expired");
            if (status == 413) return SyncClient.PushResult.quota("sync quota exceeded");
            return new SyncClient.PushResult(false, false, false, retryable(status),
                    "sync HTTP status " + status);
        } finally {
            connection.disconnect();
        }
    }

    @Override public SyncClient.ChangeBatch changes(SyncCursor cursor) throws Exception {
        String query = "v1/sync/changes?cursor=" + URLEncoder.encode(
                Long.toString(cursor == null ? 0L : cursor.revision), "UTF-8");
        HttpURLConnection connection = open(query, "GET");
        try {
            int status = connection.getResponseCode();
            if (status == 401 || status == 403) {
                throw new SyncTransportException("sync authentication expired", true, false);
            }
            if (status < 200 || status >= 300) {
                throw new SyncTransportException("sync HTTP status " + status, false, retryable(status));
            }
            return decodeChanges(read(connection.getInputStream()));
        } finally {
            connection.disconnect();
        }
    }

    private HttpURLConnection open(String path, String method) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(baseUrl, path).openConnection();
        connection.setRequestMethod(method);
        connection.setConnectTimeout(connectTimeoutMillis);
        connection.setReadTimeout(readTimeoutMillis);
        String token = tokenProvider == null ? "" : tokenProvider.token();
        if (token != null && !token.isEmpty()) connection.setRequestProperty("Authorization", "Bearer " + token);
        return connection;
    }

    private String encodePush(List<SyncOperation> operations) {
        StringBuilder json = new StringBuilder("{\"operations\":[");
        if (operations != null) {
            for (int i = 0; i < operations.size(); i++) {
                if (i > 0) json.append(',');
                SyncOperation operation = operations.get(i);
                json.append("{\"operation_id\":\"").append(escape(operation.operationId))
                        .append("\",\"entity_id\":\"").append(escape(operation.entityId))
                        .append("\",\"entity_type\":\"").append(escape(operation.entityType))
                        .append("\",\"action\":\"").append(operation.action.name())
                        .append("\",\"payload\":\"").append(escape(operation.payload))
                        .append("\",\"created_at\":").append(operation.createdAt)
                        .append("}");
            }
        }
        return json.append("]}").toString();
    }

    private SyncClient.ChangeBatch decodeChanges(String body) throws SyncTransportException {
        try {
            long cursor = number(body, "cursor", 0L);
            List<SyncEntity> entities = new ArrayList<>();
            int arrayStart = body.indexOf("\"changes\"");
            if (arrayStart >= 0) {
                arrayStart = body.indexOf('[', arrayStart);
                if (arrayStart >= 0) {
                    int index = arrayStart + 1;
                    while (index < body.length()) {
                        int start = nextObjectStart(body, index);
                        if (start < 0) break;
                        int end = matchingObjectEnd(body, start);
                        if (end < 0) throw new IllegalArgumentException("unterminated sync change");
                        String object = body.substring(start, end + 1);
                        entities.add(new SyncEntity(string(object, "entity_id"),
                                string(object, "entity_type"), payload(object),
                                number(object, "updated_at", 0L), number(object, "deleted_at", 0L),
                                string(object, "origin_device_id"), number(object, "server_revision", 0L)));
                        index = end + 1;
                    }
                }
            }
            return new SyncClient.ChangeBatch(new SyncCursor(cursor), entities);
        } catch (RuntimeException error) {
            throw new SyncTransportException("invalid sync response", false, false);
        }
    }

    private String payload(String object) {
        int valueStart = valueStart(object, "payload");
        if (valueStart < 0) return "";
        if (object.charAt(valueStart) == '"') return stringAt(object, valueStart);
        int end = valueStart;
        int depth = 0;
        boolean quoted = false;
        for (; end < object.length(); end++) {
            char c = object.charAt(end);
            if (c == '"' && (end == 0 || object.charAt(end - 1) != '\\')) quoted = !quoted;
            if (quoted) continue;
            if (c == '{' || c == '[') depth++;
            if (c == '}' || c == ']') depth--;
            if (depth == 0 && c == ',') break;
        }
        return object.substring(valueStart, end).trim();
    }

    private String string(String object, String key) {
        int start = valueStart(object, key);
        return start < 0 || object.charAt(start) != '"' ? "" : stringAt(object, start);
    }

    private String stringAt(String object, int start) {
        StringBuilder result = new StringBuilder();
        boolean escaped = false;
        for (int i = start + 1; i < object.length(); i++) {
            char c = object.charAt(i);
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

    private long number(String object, String key, long fallback) {
        int start = valueStart(object, key);
        if (start < 0) return fallback;
        int end = start;
        while (end < object.length() && (object.charAt(end) == '-' || Character.isDigit(object.charAt(end)))) end++;
        return Long.parseLong(object.substring(start, end));
    }

    private int valueStart(String object, String key) {
        int keyStart = object.indexOf("\"" + key + "\"");
        if (keyStart < 0) return -1;
        int colon = object.indexOf(':', keyStart);
        if (colon < 0) return -1;
        int start = colon + 1;
        while (start < object.length() && Character.isWhitespace(object.charAt(start))) start++;
        return start;
    }

    private int nextObjectStart(String body, int index) {
        for (int i = index; i < body.length(); i++) {
            if (body.charAt(i) == '{') return i;
            if (body.charAt(i) == ']') return -1;
        }
        return -1;
    }

    private int matchingObjectEnd(String body, int start) {
        int depth = 0;
        boolean quoted = false;
        boolean escaped = false;
        for (int i = start; i < body.length(); i++) {
            char c = body.charAt(i);
            if (quoted) {
                if (escaped) escaped = false;
                else if (c == '\\') escaped = true;
                else if (c == '"') quoted = false;
                continue;
            }
            if (c == '"') quoted = true;
            else if (c == '{') depth++;
            else if (c == '}' && --depth == 0) return i;
        }
        return -1;
    }

    private String read(InputStream input) throws IOException {
        StringBuilder body = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) body.append(line);
        }
        return body.toString();
    }

    private String escape(String value) {
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

    private boolean retryable(int status) {
        return status == 408 || status == 429 || status >= 500;
    }
}
