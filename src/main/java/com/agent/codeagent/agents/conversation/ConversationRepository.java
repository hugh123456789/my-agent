package com.agent.codeagent.agents.conversation;

import dev.langchain4j.data.message.ChatMessage;

import java.util.List;

/** Persistence port for durable conversation messages. */
public interface ConversationRepository {

    List<ChatMessage> load(String sessionId);

    void append(String sessionId, ChatMessage message);
}
