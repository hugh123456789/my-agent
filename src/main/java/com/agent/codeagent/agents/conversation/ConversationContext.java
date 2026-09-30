package com.agent.codeagent.agents.conversation;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;

import java.util.ArrayList;
import java.util.List;

/** Owns durable conversation messages and creates request-local views. */
public final class ConversationContext {

    private final List<ChatMessage> messages = new ArrayList<>();
    private final String sessionId;
    private final ConversationRepository store;

    public ConversationContext() {
        this.sessionId = null;
        this.store = null;
    }

    public ConversationContext(String sessionId, ConversationRepository store) {
        this.sessionId = sessionId;
        this.store = store;
        messages.addAll(store.load(sessionId));
    }

    public void add(ChatMessage message) {
        messages.add(message);
        if (store != null) store.append(sessionId, message);
    }

    public String sessionId() {
        return sessionId;
    }

    public List<ChatMessage> messages() {
        return List.copyOf(messages);
    }

    public List<ChatMessage> snapshotForRequest(String temporarySystemPrompt) {
        List<ChatMessage> snapshot = MessageCompactor.compact(messages);
        if (temporarySystemPrompt == null || temporarySystemPrompt.isBlank()) {
            return snapshot;
        }
        snapshot.add(0, new SystemMessage(temporarySystemPrompt));
        return snapshot;
    }
}
