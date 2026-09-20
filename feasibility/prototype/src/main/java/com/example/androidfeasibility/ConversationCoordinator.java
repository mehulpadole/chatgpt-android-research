package com.example.androidfeasibility;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ConversationCoordinator {
    public interface Listener {
        void onChanged(Conversation conversation, TurnState state, String error);
    }

    private static final class TurnRuntime {
        final String turnId;
        final String assistantMessageId;
        final ProviderAdapter.Request request;
        ProviderAdapter.StreamHandle handle;
        TurnState state = TurnState.STARTING;
        boolean terminal;

        TurnRuntime(String turnId, String assistantMessageId, ProviderAdapter.Request request) {
            this.turnId = turnId;
            this.assistantMessageId = assistantMessageId;
            this.request = request;
        }
    }

    private final ConversationRepository repository;
    private final ProviderAdapter provider;
    private final Listener listener;
    private final Map<String, TurnRuntime> turns = new HashMap<>();
    private Conversation conversation;
    private String activeTurnId;
    private String lastError = "";

    public ConversationCoordinator(ConversationRepository repository,
                                   ProviderAdapter provider, Listener listener) {
        this.repository = repository;
        this.provider = provider;
        this.listener = listener;
        this.conversation = Conversation.empty();
    }

    public synchronized Conversation restore() throws Exception {
        Conversation loaded = repository.load();
        if (loaded != null) conversation = loaded;
        for (Message message : conversation.messages) {
            if (message.status == MessageStatus.STREAMING) message.status = MessageStatus.FAILED;
        }
        repository.save(conversation);
        notifyChanged(TurnState.IDLE, "");
        return conversation.copy();
    }

    public synchronized Conversation snapshot() {
        return conversation.copy();
    }

    public synchronized TurnState state() {
        if (activeTurnId == null) return TurnState.IDLE;
        TurnRuntime runtime = turns.get(activeTurnId);
        return runtime == null ? TurnState.IDLE : runtime.state;
    }

    public synchronized String startTurn(String prompt, MockScenario scenario) throws Exception {
        if (prompt == null || prompt.trim().isEmpty()) throw new IllegalArgumentException("prompt is empty");
        TurnState current = state();
        if (current == TurnState.STARTING || current == TurnState.STREAMING) {
            throw new IllegalStateException("a turn is already active");
        }
        final String turnId = UUID.randomUUID().toString();
        final String userId = UUID.randomUUID().toString();
        final String assistantId = UUID.randomUUID().toString();
        Message user = new Message(userId, conversation.id, turnId, Role.USER,
                prompt, MessageStatus.COMPLETED, "local-mock", "deterministic", System.currentTimeMillis());
        Message assistant = new Message(assistantId, conversation.id, turnId, Role.ASSISTANT,
                "", MessageStatus.STREAMING, "local-mock", "deterministic", System.currentTimeMillis());
        if (conversation.messages.isEmpty()) {
            conversation.title = prompt.length() > 32 ? prompt.substring(0, 32) : prompt;
        }
        conversation.add(user);
        conversation.add(assistant);
        final ProviderAdapter.Request request = new ProviderAdapter.Request(
                conversation.id, turnId, prompt, scenario, "local-mock", "deterministic");
        final TurnRuntime runtime = new TurnRuntime(turnId, assistantId, request);
        turns.put(turnId, runtime);
        activeTurnId = turnId;
        lastError = "";
        repository.save(conversation);
        notifyChanged(TurnState.STARTING, "");
        runtime.handle = provider.start(request, new ProviderAdapter.Listener() {
            @Override public void onEvent(StreamEvent event) {
                apply(event);
            }
        });
        return turnId;
    }

    public synchronized void cancelActive() throws Exception {
        if (activeTurnId == null) return;
        TurnRuntime runtime = turns.get(activeTurnId);
        if (runtime == null || runtime.terminal) return;
        runtime.terminal = true;
        runtime.state = TurnState.CANCELLED;
        if (runtime.handle != null) runtime.handle.cancel();
        Message assistant = conversation.findMessage(runtime.assistantMessageId);
        if (assistant != null) assistant.status = MessageStatus.CANCELLED;
        activeTurnId = null;
        repository.save(conversation);
        notifyChanged(TurnState.CANCELLED, "");
    }

    private void apply(StreamEvent event) {
        synchronized (this) {
            TurnRuntime runtime = turns.get(event.turnId);
            if (runtime == null || runtime.terminal) return;
            Message assistant = conversation.findMessage(runtime.assistantMessageId);
            if (assistant == null) return;
            switch (event.type) {
                case STARTED:
                    runtime.state = TurnState.STREAMING;
                    assistant.status = MessageStatus.STREAMING;
                    break;
                case DELTA:
                    if (runtime.state == TurnState.STARTING) runtime.state = TurnState.STREAMING;
                    if (runtime.state != TurnState.STREAMING) return;
                    assistant.content = assistant.content + event.text;
                    assistant.status = MessageStatus.STREAMING;
                    break;
                case COMPLETED:
                    if (runtime.terminal) return;
                    runtime.terminal = true;
                    runtime.state = TurnState.COMPLETED;
                    assistant.status = MessageStatus.COMPLETED;
                    activeTurnId = null;
                    break;
                case ERROR:
                    if (runtime.terminal) return;
                    runtime.terminal = true;
                    runtime.state = TurnState.FAILED;
                    assistant.status = MessageStatus.FAILED;
                    lastError = event.error == null ? "mock provider failure" : event.error;
                    activeTurnId = null;
                    break;
            }
            try {
                repository.save(conversation);
            } catch (Exception error) {
                lastError = "persistence error: " + error.getMessage();
            }
            notifyChanged(runtime.state, lastError);
        }
    }

    private void notifyChanged(TurnState state, String error) {
        if (listener != null) listener.onChanged(conversation.copy(), state, error);
    }
}
