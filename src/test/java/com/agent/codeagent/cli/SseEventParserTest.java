package com.agent.codeagent.cli;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SseEventParserTest {

    @Test
    void parsesJsonFromSseDataLine() throws Exception {
        JsonNode event = SseEventParser.parse("data: {\"type\":\"text\",\"text\":\"你好\"}",
                new ObjectMapper());

        assertEquals("text", event.path("type").asText());
        assertEquals("你好", event.path("text").asText());
    }

    @Test
    void parses裸JsonLineWhenServerOmitsSseDataPrefix() throws Exception {
        JsonNode event = SseEventParser.parse("{\"type\":\"text\",\"text\":\"hello\"}",
                new ObjectMapper());

        assertEquals("hello", event.path("text").asText());
    }
}
