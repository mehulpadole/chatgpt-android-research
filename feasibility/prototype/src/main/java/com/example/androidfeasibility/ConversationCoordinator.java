package com.example.androidfeasibility;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ConversationCoordinator {
    /** Persist a streaming snapshot after this many new content characters. */
    public static final int DELTA_CHECKPOINT_CHAR_THRESHOLD = 16;

    public interface Listener {
        void onChanged(Conversation conversation, TurnState state, String error);
    }

    private static final class TurnRuntime {
        final String turnId;
        final String assistantMessageId;
        final ProviderRequest request;
        ProviderAdapter.StreamHandle handle;
        TurnState state = TurnState.STARTING;
        boolean terminal;
        int lastPersistedContentLength;

        TurnRuntime(String turnId, String assistantMessageId, ProviderRequest request) {
            this.turnId = turnId;
            this.assistantMessageId = assistantMessageId;
            this.request = request;
        }
    }

    private final ConversationRepository repository;
    private final ProviderAdapter provider;
    private final Listener listener;
    private ProviderConfiguration providerConfiguration;
    private final Map<String, TurnRuntime> turns = new HashMap<>();
    private Conversation conversation;
    private String activeTurnId;
    private String lastError = "";

    public ConversationCoordinator(ConversationRepository repository,
                                   ProviderAdapter provider, Listener listener) {
        this(repository, provider, listener,
                new ProviderConfiguration("local-mock", "deterministic"));
    }

    public ConversationCoordinator(ConversationRepository repository,
                                   ProviderAdapter provider, Listener listener,
                                   ProviderConfiguration providerConfiguration) {
        this.repository = repository;
        this.provider = provider;
        this.listener = listener;
        this.providerConfiguration = providerConfiguration;
        this.conversation = Conversation.empty();
    }

    public synchronized Conversation restore() throws Exception {
        Conversation loaded = repository.load();
        if (loaded != null) conversation = loaded;
        boolean repaired = false;
        for (Message message : conversation.messages) {
            if (message.status == MessageStatus.STREAMING) {
                message.status = MessageStatus.FAILED;
                message.failureCategory = ProviderError.Category.UNKNOWN.name();
                message.failureMessage = "interrupted by process termination";
                repaired = true;
            }
        }
        if (repaired || loaded == null) repository.save(conversation);
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

    public synchronized ProviderConfiguration providerConfiguration() {
        return providerConfiguration;
    }

    public synchronized void setProviderConfiguration(ProviderConfiguration providerConfiguration) {
        TurnState current = state();
        if (current == TurnState.STARTING || current == TurnState.STREAMING) {
            throw new IllegalStateException("cannot change provider during an active turn");
        }
        if (providerConfiguration == null) throw new IllegalArgumentException("provider configuration is null");
        this.providerConfiguration = providerConfiguration;
    }

    public synchronized String startTurn(String prompt) throws Exception {
        if (prompt == null || prompt.trim().isEmpty()) throw new IllegalArgumentException("prompt is empty");
        TurnState current = state();
        if (current == TurnState.STARTING || current == TurnState.STREAMING) {
            throw new IllegalStateException("a turn is already active");
        }
        final String turnId = UUID.randomUUID().toString();
        final String userId = UUID.randomUUID().toString();
        final String assistantId = UUID.randomUUID().toString();
        Message user = new Message(userId, conversation.id, turnId, Role.USER,
                prompt, MessageStatus.COMPLETED, providerConfiguration.providerId,
                providerConfiguration.modelId, System.currentTimeMillis());
        user.addContentPart(new TextPart(prompt));
        Message assistant = new Message(assistantId, conversation.id, turnId,
                Role.ASSISTANT, "", MessageStatus.STREAMING,
                providerConfiguration.providerId, providerConfiguration.modelId,
                System.currentTimeMillis());
        if (conversation.messages.isEmpty()) {
            conversation.title = prompt.length() > 32 ? prompt.substring(0, 32) : prompt;
        }
        conversation.add(user);
        conversation.add(assistant);
        final ProviderRequest request = new ProviderRequest(
                conversation.id, turnId, userId, assistantId, prompt,
                providerConfiguration.providerId, providerConfiguration.modelId,
                Collections.<String, String>emptyMap());
        final TurnRuntime runtime = new TurnRuntime(turnId, assistantId, request);
        turns.put(turnId, runtime);
        activeTurnId = turnId;
        lastError = "";
        repository.save(conversation);
        notifyChanged(TurnState.STARTING, "");
        try {
            runtime.handle = provider.start(request, new ProviderAdapter.Listener() {
                @Override public void onEvent(StreamEvent event) {
                    apply(event);
                }
            });
        } catch (Exception error) {
            apply(StreamEvent.failed(turnId, new ProviderError(
                    ProviderError.Category.PROVIDER, error.getMessage(), false)));
        }
        return turnId;
    }

    public synchronized void cancelActive() throws Exception {
        if (activeTurnId == null) return;
        cancel(activeTurnId);
    }

    public synchronized void cancel(String turnId) throws Exception {
        if (turnId == null) return;
        TurnRuntime runtime = turns.get(turnId);
        if (runtime == null || runtime.terminal) return;
        runtime.terminal = true;
        runtime.state = TurnState.CANCELLED;
        if (runtime.handle != null) runtime.handle.cancel();
        Message assistant = conversation.findMessage(runtime.assistantMessageId);
        if (assistant != null) {
            assistant.status = MessageStatus.CANCELLED;
            assistant.failureCategory = ProviderError.Category.CANCELLED.name();
            assistant.failureMessage = "cancelled";
        }
        if (turnId.equals(activeTurnId)) activeTurnId = null;
        persist(runtime);
        notifyChanged(TurnState.CANCELLED, "");
    }

    private void apply(StreamEvent event) {
        if (event == null || event.turnId == null) return;
        synchronized (this) {
            TurnRuntime runtime = turns.get(event.turnId);
            if (runtime == null || runtime.terminal) return;
            Message assistant = conversation.findMessage(runtime.assistantMessageId);
            if (assistant == null || !assistant.turnId.equals(event.turnId)) return;
            boolean checkpoint = false;
            switch (event.type) {
                case STARTED:
                    runtime.state = TurnState.STREAMING;
                    assistant.status = MessageStatus.STREAMING;
                    break;
                case DELTA:
                    if (runtime.state == TurnState.STARTING) runtime.state = TurnState.STREAMING;
                    if (runtime.state != TurnState.STREAMING) return;
                    String delta = event.text == null ? "" : event.text;
                    assistant.appendText(delta);
                    assistant.status = MessageStatus.STREAMING;
                    checkpoint = assistant.content.length() - runtime.lastPersistedContentLength
                            >= DELTA_CHECKPOINT_CHAR_THRESHOLD;
                    break;
                case COMPLETED:
                    runtime.terminal = true;
                    runtime.state = TurnState.COMPLETED;
                    assistant.status = MessageStatus.COMPLETED;
                    clearFailure(assistant);
                    if (event.turnId.equals(activeTurnId)) activeTurnId = null;
                    checkpoint = true;
                    break;
                case FAILED:
                    runtime.terminal = true;
                    runtime.state = TurnState.FAILED;
                    assistant.status = MessageStatus.FAILED;
                    ProviderError failure = event.error == null ? new ProviderError(
                            ProviderError.Category.UNKNOWN, "provider failure", false) : event.error;
                    assistant.failureCategory = failure.category.name();
                    assistant.failureMessage = failure.message;
                    lastError = failure.message;
                    if (event.turnId.equals(activeTurnId)) activeTurnId = null;
                    checkpoint = true;
                    break;
                case CANCELLED:
                    runtime.terminal = true;
                    runtime.state = TurnState.CANCELLED;
                    assistant.status = MessageStatus.CANCELLED;
                    assistant.failureCategory = ProviderError.Category.CANCELLED.name();
                    assistant.failureMessage = event.error == null ? "cancelled" : event.error.message;
                    if (event.turnId.equals(activeTurnId)) activeTurnId = null;
                    checkpoint = true;
                    break;
                default:
                    return;
            }
            if (checkpoint) persist(runtime);
            notifyChanged(runtime.state, lastError);
        }
    }

    private void persist(TurnRuntime runtime) {
        Message assistant = conversation.findMessage(runtime.assistantMessageId);
        if (assistant != null) runtime.lastPersistedContentLength = assistant.content.length();
        try {
            repository.save(conversation);
        } catch (Exception error) {
            lastError = "persistence error: " + error.getMessage();
        }
    }

    private void clearFailure(Message message) {
        message.failureCategory = "";
        message.failureMessage = "";
        lastError = "";
    }

    private void notifyChanged(TurnState state, String error) {
        if (listener != null) listener.onChanged(conversation.copy(), state, error == null ? "" : error);
    }
}
