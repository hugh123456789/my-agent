package com.agent.codeagent.agents.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AgentEventSerializationTest {

    @Test
    void serializesTheEventDiscriminatorForSseConsumers() throws Exception {
        JsonNode json = new ObjectMapper().readTree(
                new ObjectMapper().writeValueAsString(new AgentEvent.TextDelta("hello")));

        assertEquals("text", json.get("type").asText());
    }
}
