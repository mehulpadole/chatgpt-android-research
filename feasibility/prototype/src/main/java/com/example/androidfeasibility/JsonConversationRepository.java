package com.example.androidfeasibility;

import android.content.Context;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;

public final class JsonConversationRepository implements ConversationRepository {
    private final File target;

    public JsonConversationRepository(Context context) {
        target = new File(context.getFilesDir(), "prototype-conversation.json");
    }

    @Override public synchronized void save(Conversation conversation) throws Exception {
        File temp = new File(target.getParentFile(), target.getName() + ".tmp");
        byte[] bytes = AndroidConversationCodec.encode(conversation).toString().getBytes(StandardCharsets.UTF_8);
        FileOutputStream output = new FileOutputStream(temp);
        try {
            output.write(bytes);
            output.flush();
        } finally {
            output.close();
        }
        if (!temp.renameTo(target)) throw new IllegalStateException("atomic conversation replace failed");
    }

    @Override public synchronized Conversation load() throws Exception {
        if (!target.exists()) return null;
        FileInputStream input = new FileInputStream(target);
        try {
            byte[] bytes = new byte[(int) target.length()];
            int read = input.read(bytes);
            if (read <= 0) return null;
            return AndroidConversationCodec.decode(new JSONObject(new String(bytes, 0, read, StandardCharsets.UTF_8)));
        } finally {
            input.close();
        }
    }

    @Override public synchronized void clear() {
        if (target.exists() && !target.delete()) throw new IllegalStateException("conversation delete failed");
    }
}
