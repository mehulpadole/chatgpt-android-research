package com.example.androidfeasibility;

import java.util.HashMap;
import java.util.Map;

public final class ProviderRouterTest {
    private static final class RecordingProvider implements ProviderAdapter {
        int starts;

        @Override public StreamHandle start(ProviderRequest request, Listener listener) {
            starts++;
            return new StreamHandle() {
                @Override public void cancel() {}
            };
        }
    }

    public static void main(String[] args) throws Exception {
        testProviderIdsRouteWithoutCoordinatorBranches();
        testUnknownProviderProducesProviderNeutralFailure();
        testCoordinatorConfigurationChangesOnlyWhenIdle();
        System.out.println("PROVIDER ROUTER TESTS PASSED");
    }

    private static void testProviderIdsRouteWithoutCoordinatorBranches() {
        RecordingProvider mock = new RecordingProvider();
        RecordingProvider http = new RecordingProvider();
        Map<String, ProviderAdapter> adapters = new HashMap<>();
        adapters.put("local-mock", mock);
        adapters.put("test-http", http);
        ProviderRouter router = new ProviderRouter(adapters);
        ProviderRequest request = request("test-http");
        router.start(request, new NoopListener());
        check(http.starts == 1, "test-http ID must route to HTTP adapter");
        check(mock.starts == 0, "local-mock adapter must not receive HTTP request");
    }

    private static void testUnknownProviderProducesProviderNeutralFailure() {
        ProviderRouter router = new ProviderRouter(new HashMap<String, ProviderAdapter>());
        final StreamEvent[] received = new StreamEvent[1];
        router.start(request("missing"), new ProviderAdapter.Listener() {
            @Override public void onEvent(StreamEvent event) { received[0] = event; }
        });
        check(received[0] != null, "unknown provider must produce an event");
        check(received[0].type == StreamEvent.Type.FAILED, "unknown provider must fail");
        check(received[0].error.category == ProviderError.Category.PROVIDER,
                "unknown provider failure must remain provider-neutral");
    }

    private static void testCoordinatorConfigurationChangesOnlyWhenIdle() throws Exception {
        RecordingProvider provider = new RecordingProvider();
        ConversationCoordinator coordinator = new ConversationCoordinator(
                new InMemoryConversationRepository(), provider,
                new ConversationCoordinator.Listener() {
                    @Override public void onChanged(Conversation conversation, TurnState state, String error) {}
                });
        coordinator.startTurn("active");
        boolean rejected = false;
        try {
            coordinator.setProviderConfiguration(new ProviderConfiguration("test-http", "ndjson"));
        } catch (IllegalStateException expected) {
            rejected = true;
        }
        check(rejected, "configuration changes must be rejected during an active turn");
        coordinator.cancelActive();
        coordinator.setProviderConfiguration(new ProviderConfiguration("test-http", "ndjson"));
        check(coordinator.providerConfiguration().providerId.equals("test-http"),
                "idle coordinator must accept provider changes");
    }

    private static ProviderRequest request(String providerId) {
        return new ProviderRequest("conversation", "turn", "user", "assistant",
                "prompt", providerId, "model", null);
    }

    private static final class NoopListener implements ProviderAdapter.Listener {
        @Override public void onEvent(StreamEvent event) {}
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
