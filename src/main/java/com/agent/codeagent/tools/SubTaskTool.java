package com.agent.codeagent.tools;

import com.agent.codeagent.agents.tool.ToolDispatcher;
import com.agent.codeagent.agents.tool.ToolRegistrar;
import com.agent.codeagent.agents.execution.SubAgentRunner;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonStringSchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.function.Function;

/** Main-agent tool that delegates a prompt to an isolated child Agent Loop. */
@Component
public final class SubTaskTool implements ToolRegistrar {

    private final Function<String, String> runner;
    private final ObjectMapper objectMapper;

    @Autowired
    public SubTaskTool(ObjectProvider<SubAgentRunner> runnerProvider, ObjectMapper objectMapper) {
        this(prompt -> runnerProvider.getObject().run(prompt), objectMapper);
    }

    public SubTaskTool(SubAgentRunner runner, ObjectMapper objectMapper) {
        this(runner::run, objectMapper);
    }

    public SubTaskTool(Function<String, String> runner) {
        this(runner, new ObjectMapper());
    }

    private SubTaskTool(Function<String, String> runner, ObjectMapper objectMapper) {
        this.runner = Objects.requireNonNull(runner);
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    public String execute(String toolName, String arguments) {
        try {
            JsonNode root = objectMapper.readTree(arguments);
            JsonNode prompt = root == null ? null : root.get("prompt");
            if (prompt == null || !prompt.isTextual() || prompt.asText().isBlank()) {
                return "Error: task requires a non-empty string prompt";
            }
            return runner.apply(prompt.asText());
        } catch (Exception error) {
            return "Error: invalid task arguments: " + error.getMessage();
        }
    }

    @Override
    public void register(ToolDispatcher dispatcher) {
        dispatcher.register(
                ToolSpecification.builder()
                        .name("task")
                        .description("同步运行一个独立消息上下文的子 Agent，返回子 Agent 的最终文本。")
                        .parameters(JsonObjectSchema.builder()
                                .addProperty("prompt", JsonStringSchema.builder()
                                        .description("要交给子 Agent 执行的任务")
                                        .build())
                                .required(java.util.List.of("prompt"))
                                .build())
                        .build(),
                (sessionId, toolName, arguments) -> execute(toolName, arguments));
    }

    @Override
    public boolean availableToSubAgent() {
        return false;
    }
}
