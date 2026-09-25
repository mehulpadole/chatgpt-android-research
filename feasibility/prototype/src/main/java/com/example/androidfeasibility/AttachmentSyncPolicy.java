package com.example.androidfeasibility;

/** Decides what attachment metadata may enter sync; binary transfer is opt-in and separate. */
public final class AttachmentSyncPolicy {
    public static final class Decision {
        public final boolean syncMetadata;
        public final boolean syncBinary;
        public final String reason;

        private Decision(boolean syncMetadata, boolean syncBinary, String reason) {
            this.syncMetadata = syncMetadata;
            this.syncBinary = syncBinary;
            this.reason = reason;
        }
    }

    private AttachmentSyncPolicy() { }

    public static Decision decide(Entitlement entitlement, Attachment attachment,
                                  boolean allowBinary, long remainingQuotaBytes) {
        if (entitlement != Entitlement.CLOUD_SYNC || attachment == null) {
            return new Decision(false, false, "local-only");
        }
        boolean binary = allowBinary && attachment.state == AttachmentState.READY
                && attachment.byteSize <= Math.max(0L, remainingQuotaBytes);
        return new Decision(true, binary, binary ? "binary-eligible" : "metadata-only");
    }

    public static SyncOperation metadataOperation(Attachment attachment, String deviceId, long createdAt) {
        if (attachment == null) throw new IllegalArgumentException("attachment is null");
        String payload = "attachmentId=" + escape(attachment.attachmentId)
                + ";conversationId=" + escape(attachment.conversationId)
                + ";messageId=" + escape(attachment.messageId)
                + ";displayName=" + escape(attachment.displayName)
                + ";detectedMimeType=" + escape(attachment.detectedMimeType)
                + ";declaredMimeType=" + escape(attachment.declaredMimeType)
                + ";byteSize=" + attachment.byteSize
                + ";state=" + attachment.state.name()
                + ";deviceId=" + escape(deviceId == null ? "" : deviceId);
        return new SyncOperation(attachment.attachmentId, "attachment",
                SyncOperation.Action.UPSERT, payload, createdAt);
    }

    private static String escape(String value) {
        return (value == null ? "" : value).replace("\\", "\\\\").replace(";", "\\;").replace("=", "\\=");
    }
}
