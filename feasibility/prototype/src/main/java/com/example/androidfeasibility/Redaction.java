package com.example.androidfeasibility;

/** Removes credentials and sensitive authorization values from diagnostics. */
public final class Redaction {
    private Redaction() {}

    public static String message(String value) {
        if (value == null || value.isEmpty()) return "";
        String result = value.replaceAll("(?i)bearer\\s+[^\\s,]+", "Bearer [REDACTED]");
        return result.replaceAll("(?i)authorization\\s*[:=]\\s*[^,;\\s]+",
                "Authorization: [REDACTED]");
    }
}
