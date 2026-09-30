package com.agent.codeagent.agents.execution;

import com.agent.codeagent.agents.api.AgentEvent;
import com.agent.codeagent.agents.approval.ApprovalManager;
import com.agent.codeagent.agents.conversation.ConversationContext;
import com.agent.codeagent.agents.conversation.SessionService;
import com.agent.codeagent.agents.prompt.SystemPromptComposer;
import com.agent.codeagent.agents.tools.ToolCallProcessor;
import com.agent.codeagent.agents.tools.ToolProcessResult;

import com.agent.codeagent.agents.hook.core.HookEvent;
import com.agent.codeagent.agents.hook.core.HookRegistry;
import com.agent.codeagent.agents.tool.ToolDispatcher;
import com.agent.codeagent.common.SkillLoader;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.output.FinishReason;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.util.ArrayList;
import java.util.List;

/** Owns the ReAct loop and delegates tools and approvals to focused services. */
@Service
public class AgentLoopExecutor {

    private static final int MAX_ROUNDS = 20;
    private static final int MAX_TOOL_CALLS = 50;

    private final StreamingChatModel mainStreamingChatModel;
    private final ToolDispatcher toolDispatcher;
    private final HookRegistry hookRegistry;
    private final SystemPromptComposer promptComposer;
    private final SessionService sessionService;
    private final ApprovalManager approvalManager;
    private final ToolCallProcessor toolCallProcessor;

    @Autowired
    public AgentLoopExecutor(
            @Lazy @Qualifier("mainStreamingChatModel") StreamingChatModel mainStreamingChatModel,
            @Qualifier("mainToolDispatcher") ToolDispatcher toolDispatcher,
            HookRegistry hookRegistry,
            SystemPromptComposer promptComposer,
            SessionService sessionService,
            ToolCallProcessor toolCallProcessor,
            ApprovalManager approvalManager) {
        this.mainStreamingChatModel = mainStreamingChatModel;
        this.toolDispatcher = toolDispatcher;
        this.hookRegistry = hookRegistry;
        this.promptComposer = promptComposer;
        this.sessionService = sessionService;
        this.toolCallProcessor = toolCallProcessor;
        this.approvalManager = approvalManager;
    }

    /** Compatibility constructor for callers that still assemble the executor directly. */
    public AgentLoopExecutor(
            StreamingChatModel mainStreamingChatModel,
            @Qualifier("mainToolDispatcher") ToolDispatcher toolDispatcher,
            HookRegistry hookRegistry,
            SkillLoader skillLoader,
            SessionService sessionService,
            ToolCallProcessor toolCallProcessor,
            ApprovalManager approvalManager) {
        this.mainStreamingChatModel = mainStreamingChatModel;
        this.toolDispatcher = toolDispatcher;
        this.hookRegistry = hookRegistry;
        this.promptComposer = new SystemPromptComposer(skillLoader);
        this.sessionService = sessionService;
        this.toolCallProcessor = toolCallProcessor;
        this.approvalManager = approvalManager;
    }


    public Flux<AgentEvent> execute(String sessionId, String userMessage) {
        return Flux.create(rawSink -> {
            ConversationContext context = sessionService.open(sessionId);
            rawSink.next(new AgentEvent.SessionStarted(context.sessionId()));
            Object prompt = hookRegistry.triggerHooks(
                    HookEvent.USER_PROMPT_SUBMIT,
                    context.sessionId(),
                    userMessage,
                    userMessage);
            context.add(new UserMessage(String.valueOf(prompt)));
            rawSink.onCancel(() -> approvalManager.removeForSink(rawSink));
            runLoop(context, rawSink, new LoopState(MAX_ROUNDS, MAX_TOOL_CALLS));
        });
    }

    public Flux<AgentEvent> approve(String requestId, boolean approved) {
        return Flux.create(rawSink -> {
            ApprovalManager.PendingApproval state = approvalManager.take(requestId);
            if (state == null) {
                finishWithError(null, rawSink, new IllegalArgumentException(
                        "Unknown or expired requestId: " + requestId));
                return;
            }
            state.sink().complete();
            processPending(state, approved, rawSink);
        });
    }

    private void runLoop(ConversationContext context,
                         FluxSink<AgentEvent> sink,
                         LoopState state) {
        if (sink.isCancelled()) return;
        try {
            state.nextRound();
        } catch (IllegalStateException error) {
            finishWithError(context, sink, error);
            return;
        }

        String reminder = (String) hookRegistry.triggerHooks(
                HookEvent.BEFORE_ROUND, context.sessionId(), null, null);
        SystemPromptComposer.ComposedPrompt prompt = promptComposer.compose(
                reminder, String.join(", ", toolDispatcher.getDeferredToolNames()));
        List<ChatMessage> requestMessages = context.snapshotForRequest(prompt.systemPrompt());
        if (prompt.reminder() != null && !sink.isCancelled()) {
            sink.next(new AgentEvent.Reminder(prompt.reminder()));
        }

        ChatRequest request = ChatRequest.builder()
                .messages(requestMessages)
                .toolSpecifications(toolDispatcher.getActiveToolDefinitions(context.sessionId()))
                .build();

        mainStreamingChatModel.chat(request, ModelStreamAdapter.handler(
                sink,
                response -> onComplete(context, sink, state, response),
                error -> {
                    hookRegistry.triggerHooks(HookEvent.AFTER_LOOP, context.sessionId(), null, null);
                    sink.complete();
                }));
    }

    private void onComplete(ConversationContext context,
                            FluxSink<AgentEvent> sink,
                            LoopState state,
                            ChatResponse response) {
        if (sink.isCancelled()) return;
        if (response.finishReason() == FinishReason.TOOL_EXECUTION) {
            context.add(response.aiMessage());
            processToolRequests(context, response.aiMessage().toolExecutionRequests(), sink, state);
            return;
        }

        context.add(response.aiMessage());
        hookRegistry.triggerHooks(HookEvent.AFTER_LOOP, context.sessionId(), null, null);
        if (response.finishReason() == FinishReason.LENGTH) {
            sink.next(new AgentEvent.Error(
                    new RuntimeException("Response truncated (max tokens)")));
        } else {
            sink.next(new AgentEvent.Complete(response));
        }
        sink.complete();
    }

    private void processToolRequests(ConversationContext context,
                                     List<ToolExecutionRequest> requests,
                                     FluxSink<AgentEvent> sink,
                                     LoopState state) {
        for (int i = 0; i < requests.size(); i++) {
            if (sink.isCancelled()) return;
            try {
                state.nextToolCall();
            } catch (IllegalStateException error) {
                finishWithError(context, sink, error);
                return;
            }

            ToolExecutionRequest request = requests.get(i);
            ToolProcessResult result = toolCallProcessor.process(context.sessionId(), request);
            switch (result.status()) {
                case ALLOWED -> {
                    context.add(ToolExecutionResultMessage.from(request, result.content()));
                    sink.next(new AgentEvent.ToolResultDelta(
                            request.id(), request.name(), result.content()));
                }
                case DENIED -> {
                    context.add(ToolExecutionResultMessage.from(
                            request, "Permission denied: " + result.reason()));
                    sink.next(new AgentEvent.PermissionDenied(
                            request.name(), result.reason()));
                }
                case REQUIRES_APPROVAL -> {
                    List<ToolExecutionRequest> remaining = new ArrayList<>(
                            requests.subList(i, requests.size()));
                    String requestId = approvalManager.suspend(
                            context, remaining, state, sink);
                    sink.next(new AgentEvent.PermissionRequired(
                            requestId, request.name(), request.arguments(), result.reason()));
                    return;
                }
            }
        }
        runLoop(context, sink, state);
    }

    private void processPending(ApprovalManager.PendingApproval approval,
                                boolean approved,
                                FluxSink<AgentEvent> sink) {
        List<ToolExecutionRequest> remaining = approval.remaining();
        ToolExecutionRequest request = remaining.getFirst();
        if (approved) {
            ToolProcessResult result = toolCallProcessor.executeApproved(approval.context().sessionId(), request);
            approval.context().add(ToolExecutionResultMessage.from(request, result.content()));
            sink.next(new AgentEvent.ToolResultDelta(
                    request.id(), request.name(), result.content()));
        } else {
            approval.context().add(ToolExecutionResultMessage.from(
                    request, "User denied the tool call"));
            sink.next(new AgentEvent.PermissionDenied(request.name(), "User denied"));
        }

        List<ToolExecutionRequest> rest = remaining.subList(1, remaining.size());
        LoopState state = (LoopState) approval.continuationState();
        if (rest.isEmpty()) {
            runLoop(approval.context(), sink, state);
        } else {
            processToolRequests(approval.context(), rest, sink, state);
        }
    }

    private void finishWithError(ConversationContext context, FluxSink<AgentEvent> sink, Throwable error) {
        hookRegistry.triggerHooks(HookEvent.AFTER_LOOP,
                context == null ? null : context.sessionId(), null, null);
        if (!sink.isCancelled()) {
            sink.next(new AgentEvent.Error(error));
            sink.complete();
        }
    }

    public void expireApproval(ApprovalManager.PendingApproval approval) {
        finishWithError(approval.context(), approval.sink(), new IllegalStateException(
                "Tool approval request expired"));
    }

    static final class LoopState {
        private final int maxRounds;
        private final int maxToolCalls;
        private int rounds;
        private int toolCalls;

        LoopState(int maxRounds, int maxToolCalls) {
            if (maxRounds <= 0 || maxToolCalls <= 0) {
                throw new IllegalArgumentException("Agent loop limits must be positive");
            }
            this.maxRounds = maxRounds;
            this.maxToolCalls = maxToolCalls;
        }

        void nextRound() {
            if (++rounds > maxRounds) {
                throw new IllegalStateException(
                        "Agent loop exceeded maximum rounds: " + maxRounds);
            }
        }

        void nextToolCall() {
            if (++toolCalls > maxToolCalls) {
                throw new IllegalStateException(
                        "Agent loop exceeded maximum tool calls: " + maxToolCalls);
            }
        }
    }
}
