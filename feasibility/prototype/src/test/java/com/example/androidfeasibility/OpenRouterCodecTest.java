package com.example.androidfeasibility;

import java.util.HashMap;
import java.util.Map;

public final class OpenRouterCodecTest {
    public static void main(String[] args) throws Exception {
        testRequestEncodingEscapesAndStreams();
        testDeltaChunkAndMetadata();
        testUsageChunkAndDone();
        testModelCapabilityNormalization();
        testErrorMapping();
        testMalformedSseFails();
        System.out.println("OPENROUTER CODEC TESTS PASSED");
    }

    private static void testRequestEncodingEscapesAndStreams() {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("session_id", "session-1");
        ProviderRequest request = new ProviderRequest(
                "conversation-1", "turn-1", "user-1", "assistant-1",
                "say \"hi\"\nnext", "openrouter", "openai/gpt-4", metadata);
        String body = OpenRouterCodec.encodeRequest(request, true);
        check(body.contains("\"model\":\"openai/gpt-4\""), "model must be encoded");
        check(body.contains("\"stream\":true"), "stream must be enabled");
        check(body.contains("say \\\"hi\\\"\\nnext"), "prompt must be escaped");
        check(body.contains("\"role\":\"user\""), "request must use chat messages");
    }

    private static void testDeltaChunkAndMetadata() throws Exception {
        OpenRouterCodec.Chunk chunk = OpenRouterCodec.parseSseData(
                "data: {\"id\":\"gen-1\",\"model\":\"openai/gpt-4\","
                        + "\"choices\":[{\"delta\":{\"content\":\"Hello\"},"
                        + "\"finish_reason\":null}]} ");
        check(!chunk.done, "delta must not be terminal");
        check(chunk.delta.equals("Hello"), "delta content must be normalized");
        check(chunk.metadata.requestId.equals("gen-1"), "request ID must be optional metadata");
        check(chunk.metadata.attributes.get("model").equals("openai/gpt-4"),
                "model must be optional metadata");
    }

    private static void testUsageChunkAndDone() throws Exception {
        OpenRouterCodec.Chunk chunk = OpenRouterCodec.parseSseData(
                "data: {\"id\":\"gen-1\",\"model\":\"openai/gpt-4\","
                        + "\"choices\":[{\"delta\":{},\"finish_reason\":\"stop\"}],"
                        + "\"usage\":{\"prompt_tokens\":4,\"completion_tokens\":6,"
                        + "\"total_tokens\":10}}");
        check(chunk.finishReason.equals("stop"), "finish reason must be optional metadata");
        check(chunk.metadata.attributes.get("prompt_tokens").equals("4"),
                "prompt usage must be normalized");
        check(chunk.metadata.attributes.get("total_tokens").equals("10"),
                "total usage must be normalized");
        check(OpenRouterCodec.parseSseData("data: [DONE]").done,
                "DONE sentinel must be terminal");
    }

    private static void testModelCapabilityNormalization() throws Exception {
        ProviderModel model = OpenRouterCodec.parseModel(
                "{\"id\":\"openai/gpt-4o\",\"name\":\"GPT-4o\","
                        + "\"context_length\":128000,"
                        + "\"architecture\":{\"input_modalities\":[\"text\",\"image\"],"
                        + "\"output_modalities\":[\"text\"]}}");
        check(model.id.equals("openai/gpt-4o"), "model ID must be preserved");
        check(model.displayName.equals("GPT-4o"), "model display name must be preserved");
        check(model.contextLength == 128000L, "context length must be optional metadata");
        check(model.capabilities.contains(ProviderCapabilities.TEXT), "text capability expected");
        check(model.capabilities.contains(ProviderCapabilities.VISION), "vision capability expected");
        check(model.capabilities.contains(ProviderCapabilities.STREAMING),
                "streaming capability must be added for chat models");
    }

    private static void testErrorMapping() throws Exception {
        ProviderError auth = OpenRouterCodec.mapHttpError(401,
                "{\"error\":{\"message\":\"invalid key\",\"code\":401}}");
        check(auth.category == ProviderError.Category.AUTHENTICATION, "401 must be authentication");
        check(auth.message.equals("invalid key"), "provider error message must be preserved");
        check(!auth.message.contains("Bearer"), "error must not contain an authorization header");
        check(OpenRouterCodec.mapHttpError(400, "{\"error\":{\"message\":\"bad request\"}}")
                        .category == ProviderError.Category.INVALID_REQUEST,
                "400 must be invalid request");
        check(OpenRouterCodec.mapHttpError(404, "{\"error\":{\"message\":\"missing model\"}}")
                        .category == ProviderError.Category.MODEL_UNAVAILABLE,
                "404 must be model unavailable");
        check(OpenRouterCodec.mapHttpError(429, "{}").category == ProviderError.Category.RATE_LIMIT,
                "429 must be rate limit");
        check(OpenRouterCodec.mapHttpError(503, "{}").retryable,
                "server failures must be retryable");
    }

    private static void testMalformedSseFails() {
        boolean failed = false;
        try {
            OpenRouterCodec.parseSseData("data: {not-json}");
        } catch (OpenRouterCodec.ProtocolException expected) {
            failed = true;
        }
        check(failed, "malformed SSE must fail as a protocol error");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
