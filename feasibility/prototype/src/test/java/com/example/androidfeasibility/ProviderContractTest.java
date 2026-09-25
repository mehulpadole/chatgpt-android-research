package com.example.androidfeasibility;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public final class ProviderContractTest {
    public static void main(String[] args) {
        testRequestOwnsStableClientIdsAndNoMockScenario();
        testMetadataIsOptionalAndDefensive();
        testProviderNeutralErrorCategories();
        testTerminalEventVocabulary();
        System.out.println("PROVIDER CONTRACT TESTS PASSED");
    }

    private static void testRequestOwnsStableClientIdsAndNoMockScenario() {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("synthetic", "true");
        ProviderRequest request = new ProviderRequest(
                "conversation-1", "turn-1", "user-1", "assistant-1",
                "hello", "local-mock", "deterministic", metadata);
        check(request.conversationId.equals("conversation-1"), "conversation ID must be stable");
        check(request.turnId.equals("turn-1"), "turn ID must be stable");
        check(request.userMessageId.equals("user-1"), "user message ID must be stable");
        check(request.assistantMessageId.equals("assistant-1"), "assistant message ID must be stable");
        check(request.prompt.equals("hello"), "prompt must remain unchanged");
        check(request.providerId.equals("local-mock"), "provider ID must be explicit");
        check(request.modelId.equals("deterministic"), "model ID must be explicit");
        for (Field field : ProviderRequest.class.getDeclaredFields()) {
            check(field.getType() != MockScenario.class, "request must not expose MockScenario");
        }
    }

    private static void testMetadataIsOptionalAndDefensive() {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("trace", "synthetic");
        ProviderRequest request = new ProviderRequest(
                "conversation-1", "turn-1", "user-1", "assistant-1",
                "hello", "provider", "model", metadata);
        metadata.put("trace", "changed");
        check(request.metadata.get("trace").equals("synthetic"), "request metadata must be copied");
        check(ProviderMetadata.empty().requestId.isEmpty(), "metadata request ID is optional");
        ProviderMetadata startedMetadata = ProviderMetadata.withRequestId("provider-request-1");
        check(StreamEvent.started("turn-1", startedMetadata).metadata.requestId
                .equals("provider-request-1"), "started metadata must be preserved");
    }

    private static void testProviderNeutralErrorCategories() {
        ProviderError error = new ProviderError(
                ProviderError.Category.RATE_LIMIT, "synthetic limit", true);
        check(error.category == ProviderError.Category.RATE_LIMIT, "category must be provider-neutral");
        check(error.message.equals("synthetic limit"), "error message must be preserved");
        check(error.retryable, "retryability must be explicit");
    }

    private static void testTerminalEventVocabulary() {
        String turn = "turn-1";
        check(StreamEvent.completed(turn).type == StreamEvent.Type.COMPLETED,
                "completed event must be terminal");
        check(StreamEvent.failed(turn, new ProviderError(
                ProviderError.Category.NETWORK, "offline", true)).type == StreamEvent.Type.FAILED,
                "failed event must be distinct");
        check(StreamEvent.cancelled(turn).type == StreamEvent.Type.CANCELLED,
                "cancelled event must be distinct");
        check(StreamEvent.delta(turn, "x").type == StreamEvent.Type.DELTA,
                "delta event must remain non-terminal");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
