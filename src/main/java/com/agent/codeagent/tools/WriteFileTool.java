package com.agent.codeagent.tools;

import com.agent.codeagent.agents.tool.ToolDispatcher;
import com.agent.codeagent.agents.tool.ToolRegistrar;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonStringSchema;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Writes complete UTF-8 text files and creates missing parent directories. */
@Component
public class WriteFileTool implements ToolRegistrar {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String execute(String argumentsJson) {
        try {
            JsonNode input = objectMapper.readTree(argumentsJson);
            String filePath = requiredText(input, "file_path");
            String content = requiredText(input, "content");
            Path path = Path.of(filePath);
            Path parent = path.getParent();
            if (parent != null) Files.createDirectories(parent);
            Files.writeString(path, content, StandardCharsets.UTF_8);
            return "Successfully wrote to " + filePath;
        } catch (Exception error) {
            return "Error writing file: " + error.getMessage();
        }
    }

    private String requiredText(JsonNode input, String name) {
        JsonNode value = input == null ? null : input.get(name);
        if (value == null || !value.isTextual()) {
            throw new IllegalArgumentException("缺少字符串参数 " + name);
        }
        return value.asText();
    }

    @Override
    public void register(ToolDispatcher dispatcher) {
        dispatcher.register(
                ToolSpecification.builder()
                        .name("write_file")
                        .description("创建或覆盖本地 UTF-8 文本文件，必要时自动创建父目录")
                        .parameters(JsonObjectSchema.builder()
                                .addProperty("file_path", JsonStringSchema.builder().description("文件路径").build())
                                .addProperty("content", JsonStringSchema.builder().description("要写入的完整内容").build())
                                .required(List.of("file_path", "content"))
                                .build())
                        .build(),
                (sessionId, toolName, arguments) -> execute(arguments));
    }
}
