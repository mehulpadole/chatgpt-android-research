package com.example.androidfeasibility;

import java.io.File;
import java.net.URLConnection;
import java.util.Locale;

public final class AttachmentValidator {
    private AttachmentValidator() {}

    public static Result validate(File file, String declaredMimeType, long maxBytes) {
        if (file == null || !file.exists() || !file.isFile() || !file.canRead()) {
            return Result.invalid("source is missing or unreadable");
        }
        if (file.length() <= 0) return Result.invalid("file is empty");
        if (maxBytes <= 0 || file.length() > maxBytes) return Result.invalid("file is too large");
        String detected = declaredMimeType == null || declaredMimeType.trim().isEmpty()
                ? URLConnection.guessContentTypeFromName(file.getName()) : declaredMimeType.trim();
        if (detected == null || !supported(detected)) return Result.invalid("unsupported file type");
        return new Result(true, "", detected);
    }

    private static boolean supported(String mime) {
        String normalized = mime.toLowerCase(Locale.US);
        return normalized.startsWith("image/")
                || normalized.startsWith("audio/")
                || normalized.equals("application/pdf")
                || normalized.equals("application/json")
                || normalized.equals("text/plain")
                || normalized.equals("text/markdown");
    }

    public static final class Result {
        public final boolean valid;
        public final String error;
        public final String detectedMimeType;

        private Result(boolean valid, String error, String detectedMimeType) {
            this.valid = valid;
            this.error = error;
            this.detectedMimeType = detectedMimeType == null ? "" : detectedMimeType;
        }

        static Result invalid(String error) { return new Result(false, error, ""); }
    }
}
