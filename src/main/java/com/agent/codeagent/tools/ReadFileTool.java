package com.agent.codeagent.tools;

import com.agent.codeagent.agents.tool.ToolDispatcher;
import com.agent.codeagent.agents.tool.ToolRegistrar;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonStringSchema;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Reads a local UTF-8 text file and prefixes each line with its line number. */
@Component
public class ReadFileTool implements ToolRegistrar {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final FileVersionTracker versionTracker;

    public ReadFileTool() {
        this(new FileVersionTracker());
    }

    @Autowired
    public ReadFileTool(FileVersionTracker versionTracker) {
        this.versionTracker = versionTracker;
    }

    @Override
    public boolean isConcurrencySafe() {
        return true;
    }

    public String execute(String argumentsJson) {
        return execute(null, argumentsJson);
    }

    public String execute(String sessionId, String argumentsJson) {
        try {
            JsonNode input = objectMapper.readTree(argumentsJson);
            JsonNode filePath = input == null ? null : input.get("file_path");
            if (filePath == null || !filePath.isTextual() || filePath.asText().isBlank()) {
                throw new IllegalArgumentException("缺少字符串参数 file_path");
            }
            return readFile(sessionId, filePath.asText());
        } catch (Exception error) {
            return "Error reading file: " + error.getMessage();
        }
    }

    public String readFile(String filePath) throws Exception {
        return readFile(null, filePath);
    }

    public String readFile(String sessionId, String filePath) throws Exception {
        String content = Files.readString(Path.of(filePath), StandardCharsets.UTF_8);
        versionTracker.recordRead(sessionId, Path.of(filePath));
        String[] lines = content.split("\\n", -1);
        StringBuilder numbered = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) numbered.append('\n');
            numbered.append(String.format("%4d | %s", i + 1, lines[i]));
        }
        return numbered.toString();
    }

    @Override
    public void register(ToolDispatcher dispatcher) {
        dispatcher.register(
                ToolSpecification.builder()
                        .name("read_file")
                        .description("读取本地 UTF-8 文本文件，并为每一行添加行号")
                        .parameters(JsonObjectSchema.builder()
                                .addProperty("file_path", JsonStringSchema.builder()
                                        .description("要读取的文件路径")
                                        .build())
                                .required(List.of("file_path"))
                                .build())
                        .build(),
                (sessionId, toolName, arguments) -> execute(sessionId, arguments));
    }
}
