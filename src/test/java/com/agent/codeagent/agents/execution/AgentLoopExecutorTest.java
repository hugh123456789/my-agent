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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AgentLoopExecutorTest {

    @Test
    void executesAdjacentConcurrencySafeToolsInParallel() throws Exception {
        StreamingChatModel model = mock(StreamingChatModel.class);
        ToolDispatcher dispatcher = new ToolDispatcher();
        CountDownLatch started = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);
        dispatcher.register(ToolSpecification.builder().name("safe-a").description("safe").build(),
                (session, name, arguments) -> awaitTool(started, release), true);
        dispatcher.register(ToolSpecification.builder().name("safe-b").description("safe").build(),
                (session, name, arguments) -> awaitTool(started, release), true);
        HookRegistry hooks = new HookRegistry(List.of());
        SystemPromptComposer promptComposer = mock(SystemPromptComposer.class);
        when(promptComposer.compose(null, ""))
                .thenReturn(new SystemPromptComposer.ComposedPrompt("system", null));
        AgentLoopExecutor executor = new AgentLoopExecutor(model, dispatcher, hooks, promptComposer,
                new SessionService(new JsonlConversationStore(Files.createTempDirectory("agent-sessions"))),
                new ToolCallProcessor(dispatcher, hooks), new ApprovalManager(60_000));

        AtomicInteger calls = new AtomicInteger();
        doAnswer(invocation -> {
            var handler = invocation.getArgument(1, dev.langchain4j.model.chat.response.StreamingChatResponseHandler.class);
            if (calls.getAndIncrement() == 0) {
                handler.onCompleteResponse(ChatResponse.builder()
                        .aiMessage(AiMessage.from(List.of(
                                ToolExecutionRequest.builder().id("a").name("safe-a").arguments("{}").build(),
                                ToolExecutionRequest.builder().id("b").name("safe-b").arguments("{}").build())))
                        .finishReason(FinishReason.TOOL_EXECUTION).build());
            } else {
                handler.onCompleteResponse(ChatResponse.builder()
                        .aiMessage(AiMessage.from("done"))
                        .finishReason(FinishReason.STOP).build());
            }
            return null;
        }).when(model).chat(any(ChatRequest.class), any());

        CountDownLatch completed = new CountDownLatch(1);
        executor.execute("session", "run both")
                .collectList()
                .doFinally(signal -> completed.countDown())
                .subscribe();
        assertTrue(started.await(2, TimeUnit.SECONDS));
        release.countDown();
        assertTrue(completed.await(2, TimeUnit.SECONDS));
    }

    private static String awaitTool(CountDownLatch started, CountDownLatch release) {
        started.countDown();
        try {
            assertTrue(release.await(2, TimeUnit.SECONDS));
            return "ok";
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new AssertionError(error);
        }
    }

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
        when(promptComposer.compose(null, ""))
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
    void sendsActivatedDeferredSchemaOnTheNextApiCall() throws Exception {
        StreamingChatModel model = mock(StreamingChatModel.class);
        ToolDispatcher dispatcher = new ToolDispatcher();
        dispatcher.register(ToolSpecification.builder().name("rare_tool").description("rare operation")
                        .parameters(dev.langchain4j.model.chat.request.json.JsonObjectSchema.builder().build())
                        .build(), (session, name, arguments) -> "rare result");
        dispatcher.markDeferred(List.of("rare_tool"));
        dispatcher.registerToolSearch();
        HookRegistry hooks = new HookRegistry(List.of());
        SystemPromptComposer promptComposer = mock(SystemPromptComposer.class);
        when(promptComposer.compose(any(), eq("rare_tool")))
                .thenReturn(new SystemPromptComposer.ComposedPrompt("system", null));
        SessionService sessions = new SessionService(new JsonlConversationStore(
                Files.createTempDirectory("agent-sessions")));
        AgentLoopExecutor executor = new AgentLoopExecutor(model, dispatcher, hooks, promptComposer,
                sessions, new ToolCallProcessor(dispatcher, hooks), new ApprovalManager(60_000));
        AtomicInteger calls = new AtomicInteger();
        AtomicReference<List<ToolSpecification>> firstDefinitions = new AtomicReference<>();
        AtomicReference<List<ToolSpecification>> secondDefinitions = new AtomicReference<>();
        doAnswer(invocation -> {
            ChatRequest request = invocation.getArgument(0, ChatRequest.class);
            if (calls.getAndIncrement() == 0) {
                firstDefinitions.set(request.toolSpecifications());
                ToolExecutionRequest search = ToolExecutionRequest.builder()
                        .id("search-1").name("tool_search").arguments("{\"query\":\"rare\"}").build();
                invocation.getArgument(1, dev.langchain4j.model.chat.response.StreamingChatResponseHandler.class)
                        .onCompleteResponse(ChatResponse.builder()
                                .aiMessage(AiMessage.from(List.of(search)))
                                .finishReason(FinishReason.TOOL_EXECUTION).build());
            } else {
                secondDefinitions.set(request.toolSpecifications());
                invocation.getArgument(1, dev.langchain4j.model.chat.response.StreamingChatResponseHandler.class)
                        .onCompleteResponse(ChatResponse.builder()
                                .aiMessage(AiMessage.from("done"))
                                .finishReason(FinishReason.STOP).build());
            }
            return null;
        }).when(model).chat(any(ChatRequest.class), any());

        executor.execute("session-a", "find a rare tool").collectList().block();

        assertTrue(firstDefinitions.get().stream().anyMatch(spec -> spec.name().equals("tool_search")));
        assertFalse(firstDefinitions.get().stream().anyMatch(spec -> spec.name().equals("rare_tool")));
        assertTrue(secondDefinitions.get().stream().anyMatch(spec -> spec.name().equals("rare_tool")));
        assertTrue(secondDefinitions.get().stream().anyMatch(spec -> spec.parameters() != null));
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
