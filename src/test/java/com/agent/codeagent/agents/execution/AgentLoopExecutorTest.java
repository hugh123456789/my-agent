package com.agent.codeagent.agents.execution;

import com.agent.codeagent.agents.approval.ApprovalManager;
import com.agent.codeagent.agents.conversation.SessionService;
import com.agent.codeagent.agents.conversation.JsonlConversationStore;
import com.agent.codeagent.agents.prompt.SystemPromptComposer;
import com.agent.codeagent.agents.tools.ToolCallProcessor;

import com.agent.codeagent.agents.hook.core.HookRegistry;
import com.agent.codeagent.agents.tool.ToolDispatcher;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.output.FinishReason;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AgentLoopExecutorTest {

    @Test
    void passesConversationSessionToNormalToolExecution() throws Exception {
        StreamingChatModel model = mock(StreamingChatModel.class);
        ToolDispatcher dispatcher = new ToolDispatcher();
        AtomicReference<String> observedSession = new AtomicReference<>();
        dispatcher.register(ToolSpecification.builder().name("echo").description("echo").build(),
                (sessionId, toolName, arguments) -> {
                    observedSession.set(sessionId);
                    return "ok";
                });
        HookRegistry hooks = new HookRegistry(List.of());
        SystemPromptComposer promptComposer = mock(SystemPromptComposer.class);
        when(promptComposer.compose(null))
                .thenReturn(new SystemPromptComposer.ComposedPrompt("system", null));
        AgentLoopExecutor executor = new AgentLoopExecutor(model, dispatcher, hooks, promptComposer,
                new SessionService(new JsonlConversationStore(Files.createTempDirectory("agent-sessions"))),
                new ToolCallProcessor(dispatcher, hooks), new ApprovalManager(60_000));
        AtomicInteger calls = new AtomicInteger();
        doAnswer(invocation -> {
            var handler = invocation.getArgument(1, dev.langchain4j.model.chat.response.StreamingChatResponseHandler.class);
            if (calls.getAndIncrement() == 0) {
                ToolExecutionRequest request = ToolExecutionRequest.builder()
                        .id("tool-1").name("echo").arguments("{}").build();
                handler.onCompleteResponse(ChatResponse.builder()
                        .aiMessage(AiMessage.from(List.of(request)))
                        .finishReason(FinishReason.TOOL_EXECUTION)
                        .build());
            } else {
                handler.onCompleteResponse(ChatResponse.builder()
                        .aiMessage(AiMessage.from("done"))
                        .finishReason(FinishReason.STOP)
                        .build());
            }
            return null;
        }).when(model).chat(any(ChatRequest.class), any());

        executor.execute("main-session", "use echo").collectList().block();

        assertEquals("main-session", observedSession.get());
    }

    @Test
    void constructorInitializesExecutorFromInjectedDependencies() throws Exception {
        StreamingChatModel model = mock(StreamingChatModel.class);
        ToolDispatcher toolDispatcher = mock(ToolDispatcher.class);
        HookRegistry hookRegistry = mock(HookRegistry.class);
        SystemPromptComposer promptComposer = mock(SystemPromptComposer.class);

        assertDoesNotThrow(() -> new AgentLoopExecutor(
                model,
                toolDispatcher,
                hookRegistry,
                promptComposer,
                new SessionService(new JsonlConversationStore(
                        Files.createTempDirectory("agent-sessions"))),
                new ToolCallProcessor(toolDispatcher, hookRegistry),
                new ApprovalManager(60_000)));
    }

    @Test
    void loopStateTracksAndLimitsRoundsAndToolCalls() {
        AgentLoopExecutor.LoopState state =
                new AgentLoopExecutor.LoopState(2, 1);

        assertDoesNotThrow(state::nextRound);
        assertDoesNotThrow(state::nextRound);
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalStateException.class, state::nextRound);

        assertDoesNotThrow(state::nextToolCall);
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalStateException.class, state::nextToolCall);
    }
}
