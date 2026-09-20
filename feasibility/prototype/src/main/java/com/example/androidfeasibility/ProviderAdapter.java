package com.example.androidfeasibility;

public interface ProviderAdapter {
    interface StreamHandle {
        void cancel();
    }

    interface Listener {
        void onEvent(StreamEvent event);
    }

    final class Request {
        public final String conversationId;
        public final String turnId;
        public final String prompt;
        public final MockScenario scenario;
        public final String provider;
        public final String model;

        public Request(String conversationId, String turnId, String prompt,
                       MockScenario scenario, String provider, String model) {
            this.conversationId = conversationId;
            this.turnId = turnId;
            this.prompt = prompt;
            this.scenario = scenario;
            this.provider = provider;
            this.model = model;
        }
    }

    StreamHandle start(Request request, Listener listener);
}
