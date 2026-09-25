package com.example.androidfeasibility;

import java.util.Arrays;

public final class OpenRouterAttachmentAdapterTest {
    public static void main(String[] args) {
        ProviderRequest request = new ProviderRequest("conversation-1", "turn-1", "user-1",
                "assistant-1", "describe this", "openrouter", "vision-model", null);
        PreparedAttachment image = new PreparedAttachment("openrouter", "attachment-1",
                "photo.png", "image/png", "data:image/png;base64,ZmFrZQ==");
        String body = OpenRouterCodec.encodeRequest(request, Arrays.asList(image), true);
        check(body.contains("\"type\":\"image_url\""), "image content part must be encoded");
        check(body.contains("data:image/png;base64,ZmFrZQ=="), "data URL must be preserved");
        check(body.contains("\"type\":\"text\""), "text prompt must remain a content part");
        System.out.println("OPENROUTER ATTACHMENT ADAPTER TESTS PASSED");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
