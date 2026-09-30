package com.agent.codeagent.agents.conversation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.ChatMessage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** Append-only JSONL storage for durable conversation messages. */
public final class JsonlConversationStore implements ConversationRepository {

    private final Path directory;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ConcurrentMap<String, Object> locks = new ConcurrentHashMap<>();

    public JsonlConversationStore(Path directory) {
        this.directory = directory.toAbsolutePath().normalize();
    }
    @Override
    public List<ChatMessage> load(String sessionId) {
        Path file = fileFor(sessionId);
        if (!Files.exists(file)) return new ArrayList<>();
        synchronized (locks.computeIfAbsent(sessionId, ignored -> new Object())) {
            List<ChatMessage> messages = new ArrayList<>();
            try {
                String content = Files.readString(file, StandardCharsets.UTF_8);
                String[] lines = content.split("\\R", -1);
                boolean completeFinalLine = content.endsWith("\n") || content.endsWith("\r");
                int lineCount = completeFinalLine ? lines.length - 1 : lines.length;
                for (int i = 0; i < lineCount; i++) {
                    String line = lines[i];
                    if (line.isBlank()) continue;
                    JsonNode node;
                    try {
                        node = objectMapper.readTree(line);
                    } catch (Exception e) {
                        if (i == lineCount - 1 && !completeFinalLine) break;
                        throw new IllegalStateException("Malformed conversation record", e);
                    }
                    if (node == null || !node.isObject()) {
                        throw new IllegalStateException("Malformed conversation record");
                    }
                    try {
                        messages.add(ConversationMessageCodec.decode(node));
                    } catch (RuntimeException e) {
                        throw new IllegalStateException("Malformed conversation record", e);
                    }
                }
            } catch (IOException e) {
                throw new IllegalStateException("Unable to load conversation: " + sessionId, e);
            }
            return messages;
        }
    }
    @Override
    public void append(String sessionId, ChatMessage message) {
        Path file = fileFor(sessionId);
        synchronized (locks.computeIfAbsent(sessionId, ignored -> new Object())) {
            try {
                Files.createDirectories(directory);
                String record = objectMapper.writeValueAsString(
                        ConversationMessageCodec.encode(objectMapper, message)) + System.lineSeparator();
                Files.writeString(file, record, StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND);
            } catch (IOException e) {
                throw new IllegalStateException("Unable to append conversation: " + sessionId, e);
            }
        }
    }

    private Path fileFor(String sessionId) {
        if (sessionId == null || sessionId.isBlank()
                || !sessionId.matches("[A-Za-z0-9][A-Za-z0-9_-]{0,127}")) {
            throw new IllegalArgumentException("Invalid sessionId");
        }
        return directory.resolve(sessionId + ".jsonl").normalize();
    }
}
