package com.agent.codeagent.common;

import com.agent.codeagent.agents.tool.ToolDispatcher;
import com.agent.codeagent.agents.tool.ToolRegistrar;
import com.agent.codeagent.tools.BashTool;
import com.agent.codeagent.agents.approval.ApprovalManager;
import com.agent.codeagent.agents.conversation.SessionService;
import com.agent.codeagent.agents.execution.AgentLoopExecutor;
import com.agent.codeagent.agents.tools.ToolCallProcessor;
import com.agent.codeagent.tools.DateTimeTool;
import com.agent.codeagent.tools.TodoTool;
import com.agent.codeagent.tools.SubTaskTool;
import com.agent.codeagent.tools.TodoStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.ToolSpecification;
import org.springframework.beans.factory.annotation.Qualifier;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolConfigTest {

    @Test
    void mainToolSetContainsAllMainTools() {
        ToolConfig config = new ToolConfig();
        ToolDispatcher dispatcher = config.mainToolDispatcher(toolRegistrars());

        assertEquals(Set.of("bash", "getCurrentDateTime", "getCurrentDate",
                "getCurrentTime", "todo_write", "task", "tool_search"), names(dispatcher));
    }

    @Test
    void subAgentToolSetContainsOnlySelectedTools() {
        ToolConfig config = new ToolConfig();
        ToolDispatcher dispatcher = config.subAgentToolDispatcher(toolRegistrars());

        assertEquals(Set.of("bash", "getCurrentDateTime", "getCurrentDate",
                "getCurrentTime", "todo_write", "tool_search"),
                names(dispatcher));
    }

    @Test
    void createsIndependentToolSets() {
        ToolConfig config = new ToolConfig();
        ToolDispatcher main = config.mainToolDispatcher(toolRegistrars());
        ToolDispatcher sub = config.subAgentToolDispatcher(toolRegistrars());

        assertNotSame(main, sub);
        assertNotEquals(names(main), names(sub));
    }

    @Test
    void hidesDeferredDateTimeToolsUntilSessionActivatesThem() {
        ToolDispatcher dispatcher = new ToolConfig().mainToolDispatcher(toolRegistrars());

        assertEquals(Set.of("bash", "todo_write", "task", "tool_search"),
                names(dispatcher, "session-a"));

        dispatcher.activateTools("session-a", List.of("getCurrentTime"));

        assertTrue(names(dispatcher, "session-a").contains("getCurrentTime"));
        assertTrue(!names(dispatcher, "session-b").contains("getCurrentTime"));
    }

    @Test
    void bashToolExecutesCommandOnTheHostShell() throws Exception {
        ToolConfig config = new ToolConfig();
        ToolDispatcher dispatcher = config.mainToolDispatcher(toolRegistrars());

        String result = dispatcher.execute("bash", "{\"command\":\"echo hello\"}");

        assertTrue(result.contains("hello"), () -> "Unexpected output: " + result);
    }

    private List<ToolRegistrar> toolRegistrars() {
        return List.of(
                new BashTool(),
                new DateTimeTool(),
                new TodoTool(new TodoStore(), new ObjectMapper()),
                new SubTaskTool(prompt -> prompt));
    }

    @Test
    void mainAgentDispatcherInjectionIsQualified() throws NoSuchMethodException {
        Constructor<?> constructor = AgentLoopExecutor.class.getConstructor(
                dev.langchain4j.model.chat.StreamingChatModel.class,
                ToolDispatcher.class,
                com.agent.codeagent.agents.hook.core.HookRegistry.class,
                com.agent.codeagent.common.SkillLoader.class,
                SessionService.class,
                ToolCallProcessor.class,
                ApprovalManager.class);

        Qualifier qualifier = Arrays.stream(constructor.getParameterAnnotations()[1])
                .map(Annotation.class::cast)
                .filter(Qualifier.class::isInstance)
                .map(Qualifier.class::cast)
                .findFirst()
                .orElseThrow();
        assertEquals("mainToolDispatcher", qualifier.value());
    }

    private Set<String> names(ToolDispatcher dispatcher) {
        return dispatcher.getSpecifications().stream()
                .map(ToolSpecification::name)
                .collect(Collectors.toSet());
    }

    private Set<String> names(ToolDispatcher dispatcher, String sessionId) {
        return dispatcher.getActiveToolDefinitions(sessionId).stream()
                .map(ToolSpecification::name)
                .collect(Collectors.toSet());
    }
}
