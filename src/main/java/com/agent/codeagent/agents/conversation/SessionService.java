package com.agent.codeagent.agents.conversation;

import org.springframework.stereotype.Service;

import java.util.UUID;

/** Opens durable conversation contexts and owns session ID resolution. */
@Service
public class SessionService {

    private final ConversationRepository store;

    public SessionService(ConversationRepository store) {
        this.store = store;
    }

    public ConversationContext open(String sessionId) {
        String resolved = sessionId == null || sessionId.isBlank()
                ? UUID.randomUUID().toString()
                : sessionId;
        return new ConversationContext(resolved, store);
    }
}
