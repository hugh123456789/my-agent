package com.agent.codeagent.agents.conversation;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SessionLifecycleTest {

    @Test
    void missingSessionIdCreatesANewId() throws Exception {
        SessionService service = new SessionService(
                new JsonlConversationStore(Files.createTempDirectory("sessions")));
        String sessionId = service.open(null).sessionId();

        assertNotNull(sessionId);
        assertNotEquals("", sessionId);
        assertEquals(sessionId, service.open(sessionId).sessionId());
    }
}
