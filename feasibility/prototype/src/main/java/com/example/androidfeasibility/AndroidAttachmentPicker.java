package com.example.androidfeasibility;

import android.content.Intent;

/** Scoped Android document intents; no broad filesystem permission is required. */
public final class AndroidAttachmentPicker {
    private AndroidAttachmentPicker() {}

    public static Intent imageIntent() {
        return baseIntent("image/*");
    }

    public static Intent documentIntent() {
        return baseIntent("*/*");
    }

    private static Intent baseIntent(String type) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(type);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        return intent;
    }
}
