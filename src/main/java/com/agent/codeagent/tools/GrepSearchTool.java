package com.agent.codeagent.tools;

import com.agent.codeagent.agents.tool.ToolDispatcher;
import com.agent.codeagent.agents.tool.ToolRegistrar;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonStringSchema;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.List;
import java.util.regex.Pattern;

/** Recursively searches UTF-8 text files with a regular expression. */
@Component
public class GrepSearchTool implements ToolRegistrar {

    private static final int MAX_RESULTS = 100;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String execute(String argumentsJson) {
        try {
            JsonNode input = objectMapper.readTree(argumentsJson);
            String patternText = requiredText(input, "pattern");
            String searchPath = optionalText(input, "path", ".");
            String include = optionalText(input, "include", null);
            return grepSearch(patternText, Path.of(searchPath), include);
        } catch (Exception error) {
            return "Error: " + error.getMessage();
        }
    }

    public String grepSearch(String patternText, Path searchPath, String include) throws IOException {
        Pattern pattern = Pattern.compile(patternText);
        PathMatcher includeMatcher = include == null || include.isBlank()
                ? null
                : FileSystems.getDefault().getPathMatcher("glob:" + include);
        StringBuilder result = new StringBuilder();
        int totalMatches = 0;

        try (var paths = Files.walk(searchPath)) {
            var iterator = paths.filter(Files::isRegularFile).iterator();
            while (iterator.hasNext()) {
                Path file = iterator.next();
                if (includeMatcher != null && !includeMatcher.matches(file.getFileName())) continue;
                List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
                for (int i = 0; i < lines.size(); i++) {
                    if (!pattern.matcher(lines.get(i)).find()) continue;
                    totalMatches++;
                    if (totalMatches <= MAX_RESULTS) {
                        if (result.length() > 0) result.append('\n');
                        result.append(file).append(':').append(i + 1).append(':').append(lines.get(i));
                    }
                }
            }
        }

        if (totalMatches == 0) return "No matches found.";
        if (totalMatches > MAX_RESULTS) {
            result.append("\n... and ").append(totalMatches - MAX_RESULTS).append(" more matches");
        }
        return result.toString();
    }

    private String requiredText(JsonNode input, String name) {
        String value = optionalText(input, name, null);
        if (value == null) throw new IllegalArgumentException("缺少字符串参数 " + name);
        return value;
    }

    private String optionalText(JsonNode input, String name, String defaultValue) {
        JsonNode value = input == null ? null : input.get(name);
        return value == null ? defaultValue : value.isTextual() ? value.asText() : defaultValue;
    }

    @Override
    public void register(ToolDispatcher dispatcher) {
        dispatcher.register(
                ToolSpecification.builder()
                        .name("grep_search")
                        .description("递归搜索本地文本文件，支持正则表达式、路径和文件名筛选")
                        .parameters(JsonObjectSchema.builder()
                                .addProperty("pattern", JsonStringSchema.builder().description("要搜索的正则表达式").build())
                                .addProperty("path", JsonStringSchema.builder().description("搜索的文件或目录，默认为当前目录").build())
                                .addProperty("include", JsonStringSchema.builder().description("可选的文件名 glob 筛选，例如 *.java").build())
                                .required(List.of("pattern"))
                                .build())
                        .build(),
                (sessionId, toolName, arguments) -> execute(arguments));
    }
}
