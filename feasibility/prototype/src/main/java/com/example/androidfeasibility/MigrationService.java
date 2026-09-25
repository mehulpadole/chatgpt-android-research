package com.example.androidfeasibility;

public final class MigrationService {
    private MigrationService() { }

    public static String migrateJson(String payload) {
        if (payload == null) throw new IllegalArgumentException("payload is null");
        String value = payload.trim();
        if (!value.startsWith("{") || !value.endsWith("}")) throw new IllegalArgumentException("JSON object required");
        int marker = value.indexOf("\"schemaVersion\"");
        if (marker < 0) return "{\"schemaVersion\":" + SchemaVersion.CURRENT + "," + value.substring(1);
        int colon = value.indexOf(':', marker);
        if (colon < 0) throw new IllegalArgumentException("schemaVersion is malformed");
        int end = colon + 1;
        while (end < value.length() && Character.isWhitespace(value.charAt(end))) end++;
        int numberEnd = end;
        while (numberEnd < value.length() && Character.isDigit(value.charAt(numberEnd))) numberEnd++;
        if (numberEnd == end) throw new IllegalArgumentException("schemaVersion is malformed");
        int version = Integer.parseInt(value.substring(end, numberEnd));
        if (version > SchemaVersion.CURRENT) throw new IllegalArgumentException("unsupported schema version");
        return value.substring(0, end) + SchemaVersion.CURRENT + value.substring(numberEnd);
    }
}
