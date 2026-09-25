package org.json;

import java.util.LinkedHashMap;
import java.util.Map;

/** Minimal test-only stand-in for the Android JSONObject API used by the codec. */
public final class JSONObject {
    private final Map<String, Object> values = new LinkedHashMap<>();

    public JSONObject() {}

    public JSONObject put(String key, Object value) {
        values.put(key, value);
        return this;
    }

    public String getString(String key) {
        Object value = values.get(key);
        if (value == null) throw new IllegalArgumentException("missing " + key);
        return String.valueOf(value);
    }

    public String optString(String key, String fallback) {
        Object value = values.get(key);
        return value == null ? fallback : String.valueOf(value);
    }

    public long optLong(String key, long fallback) {
        Object value = values.get(key);
        if (value == null) return fallback;
        return value instanceof Number ? ((Number) value).longValue() : Long.parseLong(String.valueOf(value));
    }

    public JSONArray optJSONArray(String key) {
        Object value = values.get(key);
        return value instanceof JSONArray ? (JSONArray) value : null;
    }
}
