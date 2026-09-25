package org.json;

import java.util.ArrayList;
import java.util.List;

/** Minimal test-only stand-in for the Android JSONArray API used by the codec. */
public final class JSONArray {
    private final List<Object> values = new ArrayList<>();

    public JSONArray put(Object value) {
        values.add(value);
        return this;
    }

    public int length() {
        return values.size();
    }

    public JSONObject getJSONObject(int index) {
        return (JSONObject) values.get(index);
    }
}
