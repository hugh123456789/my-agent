package com.agent.codeagent.cli;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

final class SseEventParser {

    private SseEventParser() {}

    static JsonNode parse(String line, ObjectMapper objectMapper) throws IOException {
        if (line == null) return null;
        String data = line.startsWith("data:")
                ? line.substring("data:".length()).trim()
                : line.trim();
        if (!data.startsWith("{")) return null;
        return data.isEmpty() ? null : objectMapper.readTree(data);
    }
}
