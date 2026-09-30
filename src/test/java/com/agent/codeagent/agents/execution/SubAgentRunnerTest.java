package com.agent.codeagent.agents.execution;

import com.agent.codeagent.agents.conversation.ConversationContext;
import com.agent.codeagent.agents.tools.ToolCallProcessor;
import com.agent.codeagent.agents.tools.ToolProcessResult;

import com.agent.codeagent.agents.hook.core.HookRegistry;
import com.agent.codeagent.agents.hook.core.HookEvent;
import com.agent.codeagent.agents.hook.permission.PermissionResult;
import com.agent.codeagent.agents.tool.ToolDispatcher;
import com.agent.codeagent.tools.TodoHookRegistrar;
import com.agent.codeagent.tools.TodoStore;
import com.agent.codeagent.tools.TodoTool;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.FinishReason;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SubAgentRunnerTest {

    @Test
    void returnsFinalTextFromFreshChildMessages() {
        ChatModel model = mock(ChatModel.class);
        List<ChatRequest> requests = new ArrayList<>();
        when(model.chat(any(ChatRequest.class))).thenAnswer(invocation -> {
            ChatRequest request = invocation.getArgument(0);
            requests.add(request);
            return response(AiMessage.from("child answer"), FinishReason.STOP);
        });

        SubAgentRunner runner = new SubAgentRunner(
                model, new ToolDispatcher(), new ToolCallProcessor(
                new ToolDispatcher(), new HookRegistry(List.of())), new HookRegistry(List.of()));

        assertEquals("child answer", runner.run("child prompt"));
        assertEquals(1, requests.size());
        assertEquals(1, requests.getFirst().messages().size());
        assertEquals("child prompt", ((UserMessage) requests.getFirst().messages().getFirst()).singleText());
        assertFalse(requests.getFirst().messages().stream()
                .anyMatch(message -> message.toString().contains("parent")));
    }

    @Test
    void executesChildToolBeforeReturningFinalText() {
        ChatModel model = mock(ChatModel.class);
        ToolExecutionRequest toolRequest = ToolExecutionRequest.builder()
                .id("call-1").name("echo").arguments("{}").build();
        when(model.chat(any(ChatRequest.class))).thenReturn(
                response(AiMessage.from(List.of(toolRequest)), FinishReason.TOOL_EXECUTION),
                response(AiMessage.from("after tool"), FinishReason.STOP));

        ToolDispatcher tools = new ToolDispatcher();
        tools.register(ToolSpecification.builder().name("echo").description("echo").build(),
                (sessionId, name, arguments) -> "tool output");
        HookRegistry hooks = new HookRegistry(List.of());
        SubAgentRunner runner = new SubAgentRunner(
                model, tools, new ToolCallProcessor(tools, hooks), hooks);

        assertEquals("after tool", runner.run("use a tool"));
    }

    @Test
    void appliesSharedHooksToChildToolExecution() {
        ChatModel model = mock(ChatModel.class);
        ToolExecutionRequest toolRequest = ToolExecutionRequest.builder()
                .id("call-1").name("echo").arguments("{}").build();
        when(model.chat(any(ChatRequest.class))).thenReturn(
                response(AiMessage.from(List.of(toolRequest)), FinishReason.TOOL_EXECUTION),
                response(AiMessage.from("hooked answer"), FinishReason.STOP));

        ToolDispatcher tools = new ToolDispatcher();
        tools.register(ToolSpecification.builder().name("echo").description("echo").build(),
                (sessionId, name, arguments) -> "raw output");
        HookRegistry hooks = new HookRegistry(List.of());
        hooks.registerHook(HookEvent.PRE_TOOL_USE, context -> context.output());
        hooks.registerHook(HookEvent.POST_TOOL_USE, context -> "post-hook output");
        SubAgentRunner runner = new SubAgentRunner(
                model, tools, new ToolCallProcessor(tools, hooks), hooks);

        assertEquals("hooked answer", runner.run("run with hooks"));
    }

    @Test
    void turnsChildApprovalRequestIntoAnErrorResult() {
        ChatModel model = mock(ChatModel.class);
        ToolExecutionRequest toolRequest = ToolExecutionRequest.builder()
                .id("call-1").name("protected").arguments("{}").build();
        when(model.chat(any(ChatRequest.class))).thenReturn(
                response(AiMessage.from(List.of(toolRequest)), FinishReason.TOOL_EXECUTION));

        ToolDispatcher tools = new ToolDispatcher();
        tools.register(ToolSpecification.builder().name("protected").description("protected").build(),
                (sessionId, name, arguments) -> "must not execute");
        HookRegistry hooks = new HookRegistry(List.of());
        hooks.registerHook(HookEvent.PRE_TOOL_USE,
                context -> new PermissionResult.AskUser("confirmation required"));
        SubAgentRunner runner = new SubAgentRunner(
                model, tools, new ToolCallProcessor(tools, hooks), hooks);

        assertTrue(runner.run("protected action").contains("requires approval"));
    }

    @Test
    void clearsEphemeralTodoStateWhenChildCompletes() {
        ChatModel model = mock(ChatModel.class);
        ToolExecutionRequest todoWrite = ToolExecutionRequest.builder()
                .id("call-1").name("todo_write")
                .arguments("{\"tasks\":[{\"title\":\"child task\",\"status\":\"pending\"}]}")
                .build();
        when(model.chat(any(ChatRequest.class))).thenReturn(
                response(AiMessage.from(List.of(todoWrite)), FinishReason.TOOL_EXECUTION),
                response(AiMessage.from("child answer"), FinishReason.STOP));

        TodoStore store = new TodoStore();
        ToolDispatcher tools = new ToolDispatcher();
        new TodoTool(store, new ObjectMapper()).register(tools);
        HookRegistry hooks = new HookRegistry(List.of(new TodoHookRegistrar(store)));
        SubAgentRunner runner = new SubAgentRunner(model, tools,
                new ToolCallProcessor(tools, hooks), hooks, 20, 50,
                () -> "sub-agent-test");

        assertEquals("child answer", runner.run("make a plan"));
        assertEquals("任务清单为空。", store.session("sub-agent-test").formatTaskList());
    }

    private ChatResponse response(AiMessage message, FinishReason finishReason) {
        return ChatResponse.builder().aiMessage(message).finishReason(finishReason).build();
    }
}
