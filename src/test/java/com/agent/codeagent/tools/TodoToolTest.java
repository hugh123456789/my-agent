package com.agent.codeagent.tools;

import com.agent.codeagent.agents.hook.core.HookEvent;
import com.agent.codeagent.agents.hook.core.HookRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TodoToolTest {

    @Test
    void isolatesTasksBetweenSessions() {
        TodoTool todoTool = newTodoTool();

        String firstSession = todoTool.writeTask("session-a",
                "{\"tasks\":[{\"title\":\"same title\",\"status\":\"completed\"}]}");
        String secondSession = todoTool.writeTask("session-b",
                "{\"tasks\":[{\"title\":\"same title\",\"status\":\"pending\"}]}");

        assertEquals("当前任务清单（共 1 项）：\n  [x] same title (completed)", firstSession);
        assertEquals("当前任务清单（共 1 项）：\n  [ ] same title (pending)", secondSession);
    }

    @Test
    void remindsAgentAfterThreeNonTodoToolCallsWithCurrentPlan() {
        TodoTool todoTool = newTodoTool();
        HookRegistry registry = new HookRegistry(List.of(new TodoHookRegistrar(
                (TodoStore) org.springframework.test.util.ReflectionTestUtils.getField(todoTool, "store"))));

        todoTool.writeTask("""
                {"tasks":[
                  {"title":"实现 SSO 登录","status":"pending"},
                  {"title":"实现后端登录逻辑","status":"in_progress"},
                  {"title":"已读取 X-AIDP-User","status":"completed"},
                  {"title":"补单元测试","status":"pending"},
                  {"title":"运行测试","status":"pending"}
                ]}
                """);

        invokeTool(registry, "bash");
        invokeTool(registry, "getCurrentDate");
        invokeTool(registry, "bash");

        assertEquals("""
                总目标：实现 SSO 登录

                当前 Todo：实现后端登录逻辑

                已完成：

                - 已读取 X-AIDP-User

                还没完成：

                - 实现 SSO 登录
                - 补单元测试
                - 运行测试

                要求：
                继续当前 Todo。
                """.strip(), registry.triggerHooks(HookEvent.BEFORE_ROUND, null, null));
    }

    @Test
    void todoWriteResetsNonTodoCallCounter() {
        TodoTool todoTool = newTodoTool();
        HookRegistry registry = new HookRegistry(List.of(new TodoHookRegistrar(
                (TodoStore) org.springframework.test.util.ReflectionTestUtils.getField(todoTool, "store"))));
        todoTool.writeTask("{\"tasks\":[{\"title\":\"实现 SSO 登录\",\"status\":\"pending\"}]}");

        invokeTool(registry, "bash");
        invokeTool(registry, "bash");
        invokeTool(registry, "todo_write");
        invokeTool(registry, "bash");

        assertNull(registry.triggerHooks(HookEvent.BEFORE_ROUND, null, null));
    }

    @Test
    void exposesOnlyDependencyInjectedConstructionAndToolExecutionApi() throws Exception {
        assertThrows(NoSuchMethodException.class,
                () -> TodoTool.class.getConstructor());
        assertThrows(NoSuchMethodException.class,
                () -> TodoTool.class.getMethod("formatTaskList"));
    }

    private TodoTool newTodoTool() {
        return new TodoTool(new TodoStore(), new ObjectMapper());
    }

    private void invokeTool(HookRegistry registry, String toolName) {
        registry.triggerHooks(
                HookEvent.PRE_TOOL_USE,
                ToolExecutionRequest.builder().id(toolName).name(toolName).arguments("{}").build(),
                null);
    }
}
