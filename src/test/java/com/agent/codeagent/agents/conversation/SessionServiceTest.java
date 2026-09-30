package com.agent.codeagent.agents.conversation;

import org.junit.jupiter.api.Test;
import com.agent.codeagent.common.AgentRuntimeConfig;

import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class SessionServiceTest {

    @Test
    void defaultRuntimeConfigurationProvidesJsonlConversationRepository() {
        ConversationRepository repository = new AgentRuntimeConfig().conversationRepository("./sessions");

        assertInstanceOf(JsonlConversationStore.class, repository);
    }

    @Test
    void preservesProvidedSessionIdAndGeneratesMissingOne() throws Exception {
        SessionService service = new SessionService(
                new JsonlConversationStore(Files.createTempDirectory("sessions")));

        ConversationContext provided = service.open("session-1");
        ConversationContext generated = service.open(" ");

        assertEquals("session-1", provided.sessionId());
        assertNotNull(generated.sessionId());
    }
}
