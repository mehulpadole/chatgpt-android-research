package com.example.androidfeasibility;

public interface ProviderAttachmentAdapter {
    interface PreparationHandle {
        void cancel();
    }

    interface Listener {
        void onResult(Result result);
    }

    PreparationHandle prepare(Attachment attachment, ProviderModel model, Listener listener);

    final class Result {
        public final boolean success;
        public final boolean cancelled;
        public final PreparedAttachment prepared;
        public final String error;

        private Result(boolean success, boolean cancelled, PreparedAttachment prepared, String error) {
            this.success = success;
            this.cancelled = cancelled;
            this.prepared = prepared;
            this.error = error == null ? "" : error;
        }

        public static Result success(PreparedAttachment prepared) {
            return new Result(true, false, prepared, "");
        }

        public static Result failed(String error) {
            return new Result(false, false, null, error);
        }

        public static Result cancelled() {
            return new Result(false, true, null, "cancelled");
        }
    }
}
