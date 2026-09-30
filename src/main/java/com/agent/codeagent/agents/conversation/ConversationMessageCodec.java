package com.agent.codeagent.agents.conversation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;

import java.util.ArrayList;
import java.util.List;

final class ConversationMessageCodec {

    private ConversationMessageCodec() {
    }

    static ObjectNode encode(ObjectMapper mapper, ChatMessage message) {
        ObjectNode node = mapper.createObjectNode();
        if (message instanceof UserMessage user) {
            node.put("type", "user");
            node.put("text", user.singleText());
        } else if (message instanceof AiMessage ai) {
            node.put("type", "assistant");
            if (ai.text() != null) node.put("text", ai.text());
            ArrayNode requests = node.putArray("toolCalls");
            for (ToolExecutionRequest request : ai.toolExecutionRequests()) {
                ObjectNode tool = requests.addObject();
                tool.put("id", request.id());
                tool.put("name", request.name());
                tool.put("arguments", request.arguments());
            }
        } else if (message instanceof ToolExecutionResultMessage result) {
            node.put("type", "tool_result");
            node.put("id", result.id());
            node.put("toolName", result.toolName());
            node.put("text", result.text());
        } else {
            throw new IllegalArgumentException("Unsupported chat message type: " + message.type());
        }
        return node;
    }

    static ChatMessage decode(JsonNode node) {
        String type = requiredText(node, "type");
        return switch (type) {
            case "user" -> new UserMessage(requiredText(node, "text"));
            case "assistant" -> decodeAssistant(node);
            case "tool_result" -> ToolExecutionResultMessage.from(
                    requiredText(node, "id"),
                    requiredText(node, "toolName"),
                    requiredText(node, "text"));
            default -> throw new IllegalArgumentException("Unsupported chat message record type: " + type);
        };
    }

    private static AiMessage decodeAssistant(JsonNode node) {
        String text = node.has("text") ? node.get("text").asText() : null;
        List<ToolExecutionRequest> requests = new ArrayList<>();
        for (JsonNode tool : node.path("toolCalls")) {
            requests.add(ToolExecutionRequest.builder()
                    .id(requiredText(tool, "id"))
                    .name(requiredText(tool, "name"))
                    .arguments(requiredText(tool, "arguments"))
                    .build());
        }
        if (!requests.isEmpty()) return AiMessage.from(requests);
        return AiMessage.from(text == null ? "" : text);
    }

    private static String requiredText(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull() || !value.isValueNode()) {
            throw new IllegalArgumentException("Missing message field: " + field);
        }
        return value.asText();
    }
}
