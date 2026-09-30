package com.agent.codeagent.tools;

import com.agent.codeagent.agents.tool.ToolDispatcher;
import com.agent.codeagent.agents.tool.ToolRegistrar;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.model.chat.request.json.JsonArraySchema;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/** Todo tool facade: parses tool input and wires the Todo store/reminder hook. */
@Component
public class TodoTool implements ToolRegistrar {

    private final TodoStore store;
    private final ObjectMapper objectMapper;

    @Autowired
    public TodoTool(TodoStore store, ObjectMapper objectMapper) {
        this.store = store;
        this.objectMapper = objectMapper;
    }

    public String writeTask(String argumentsJson) {
        return writeTask(null, argumentsJson);
    }

    public String writeTask(String sessionId, String argumentsJson) {
        try {
            JsonNode root = objectMapper.readTree(argumentsJson);
            JsonNode tasks = root.get("tasks");
            if (tasks == null || !tasks.isArray()) {
                return "Error: 缺少 tasks 数组。格式: {\"tasks\":[{\"title\":\"...\",\"status\":\"pending|in_progress|completed\"}]}";
            }
            TodoStore.SessionState session = store.session(sessionId);
            for (JsonNode task : tasks) {
                try {
                    session.upsert(task.path("title").asText(null), task.path("status").asText(null));
                } catch (IllegalArgumentException error) {
                    return "Error: " + error.getMessage();
                }
            }
            return session.formatTaskList();
        } catch (Exception error) {
            return "Error: todo_write 参数解析失败 - " + error.getMessage();
        }
    }

    @Override
    public void register(ToolDispatcher dispatcher) {
        dispatcher.register(
                ToolSpecification.builder()
                        .name("todo_write")
                        .description("创建或更新任务清单中的一个或多个任务（title + status）。"
                                + "纯规划工具，只更新计划状态，不执行任何实际操作。"
                                + "推荐流程：先用一批 pending 任务列出所有步骤，再逐个标记 in_progress、completed。"
                                + "每次修改计划都必须调用本工具。")
                        .parameters(JsonObjectSchema.builder()
                                .addProperty("tasks", JsonArraySchema.builder()
                                        .description("要创建或更新的任务数组；title 相同视为更新")
                                        .items(JsonObjectSchema.builder()
                                                .addStringProperty("title", "任务标题/描述")
                                                .addEnumProperty("status",
                                                        List.of("pending", "in_progress", "completed"),
                                                        "任务状态：pending=待开始, in_progress=进行中, completed=已完成")
                                                .required("title", "status")
                                                .build())
                                        .build())
                                .required("tasks")
                                .build())
                        .build(),
                (sessionId, toolName, arguments) -> writeTask(sessionId, arguments));
    }

}
