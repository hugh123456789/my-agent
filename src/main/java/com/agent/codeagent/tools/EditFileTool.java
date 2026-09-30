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

/** Replaces one unique string in a local UTF-8 text file. */
@Component
public class EditFileTool implements ToolRegistrar {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final FileVersionTracker versionTracker;
    private final FileWriteCoordinator writeCoordinator;

    public EditFileTool() {
        this(new FileVersionTracker(), new FileWriteCoordinator());
    }

    public EditFileTool(FileVersionTracker versionTracker) {
        this(versionTracker, new FileWriteCoordinator());
    }

    @Autowired
    public EditFileTool(FileVersionTracker versionTracker, FileWriteCoordinator writeCoordinator) {
        this.versionTracker = versionTracker;
        this.writeCoordinator = writeCoordinator;
    }

    public String execute(String argumentsJson) {
        return execute(null, argumentsJson);
    }

    public String execute(String sessionId, String argumentsJson) {
        try {
            JsonNode input = objectMapper.readTree(argumentsJson);
            String filePath = requiredText(input, "file_path");
            String oldString = requiredText(input, "old_string");
            String newString = requiredText(input, "new_string");
            return editFile(sessionId, filePath, oldString, newString);
        } catch (Exception error) {
            return "Error editing file: " + error.getMessage();
        }
    }

    public String editFile(String filePath, String oldString, String newString) throws Exception {
        return editFile(null, filePath, oldString, newString);
    }

    public String editFile(String sessionId, String filePath, String oldString, String newString) throws Exception {
        Path path = Path.of(filePath);
        return writeCoordinator.withExclusiveWrite(path, () -> {
            versionTracker.requireUnchanged(sessionId, path);
            String content = Files.readString(path, StandardCharsets.UTF_8);
            String actualString = findActualString(content, oldString);
            if (actualString == null) {
                return "Error: old_string not found in " + filePath;
            }

            int count = content.split(actualString, -1).length - 1;
            if (count > 1) {
                return "Error: old_string found " + count + " times in " + filePath + ". Must be unique.";
            }

            String updated = String.join(newString, content.split(actualString, -1));
            Files.writeString(path, updated, StandardCharsets.UTF_8);
            versionTracker.recordWritten(sessionId, path);
            return "Successfully edited " + filePath;
        });
    }

    private String findActualString(String fileContent, String searchString) {
        if (fileContent.contains(searchString)) return searchString;
        String normalizedSearch = normalizeQuotes(searchString);
        String normalizedFile = normalizeQuotes(fileContent);
        int index = normalizedFile.indexOf(normalizedSearch);
        return index == -1 ? null : fileContent.substring(index, index + searchString.length());
    }

    private String normalizeQuotes(String value) {
        return value
                .replaceAll("[\\u2018\\u2019\\u2032]", "'")
                .replaceAll("[\\u201C\\u201D\\u2033]", "\"");
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
                        .name("edit_file")
                        .description("在本地 UTF-8 文本文件中唯一地替换一段内容")
                        .parameters(JsonObjectSchema.builder()
                                .addProperty("file_path", JsonStringSchema.builder().description("文件路径").build())
                                .addProperty("old_string", JsonStringSchema.builder().description("要替换的原内容，必须唯一").build())
                                .addProperty("new_string", JsonStringSchema.builder().description("替换后的内容").build())
                                .required(List.of("file_path", "old_string", "new_string"))
                                .build())
                        .build(),
                (sessionId, toolName, arguments) -> execute(sessionId, arguments));
    }
}
