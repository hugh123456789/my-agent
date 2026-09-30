package com.agent.codeagent.agents.execution;

import com.agent.codeagent.agents.conversation.ConversationContext;
import com.agent.codeagent.agents.tools.ToolCallProcessor;
import com.agent.codeagent.agents.tools.ToolProcessResult;

import com.agent.codeagent.agents.hook.core.HookEvent;
import com.agent.codeagent.agents.hook.core.HookRegistry;
import com.agent.codeagent.agents.tool.ToolDispatcher;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.UUID;

/** Runs an isolated, synchronous child Agent Loop for the task tool. */
public final class SubAgentRunner {

    private static final int MAX_ROUNDS = 20;
    private static final int MAX_TOOL_CALLS = 50;

    private final ChatModel model;
    private final ToolDispatcher toolDispatcher;
    private final ToolCallProcessor toolCallProcessor;
    private final HookRegistry hookRegistry;
    private final int maxRounds;
    private final int maxToolCalls;
    private final Supplier<String> sessionIdSupplier;

    public SubAgentRunner(ChatModel model,
                           ToolDispatcher toolDispatcher,
                           ToolCallProcessor toolCallProcessor,
                           HookRegistry hookRegistry) {
        this(model, toolDispatcher, toolCallProcessor, hookRegistry, MAX_ROUNDS, MAX_TOOL_CALLS,
                () -> "sub-agent-" + UUID.randomUUID());
    }

    public SubAgentRunner(ChatModel model,
                          ToolDispatcher toolDispatcher,
                          ToolCallProcessor toolCallProcessor,
                          HookRegistry hookRegistry,
                          int maxRounds,
                          int maxToolCalls) {
        this(model, toolDispatcher, toolCallProcessor, hookRegistry, maxRounds, maxToolCalls,
                () -> "sub-agent-" + UUID.randomUUID());
    }

    SubAgentRunner(ChatModel model,
                   ToolDispatcher toolDispatcher,
                   ToolCallProcessor toolCallProcessor,
                   HookRegistry hookRegistry,
                   int maxRounds,
                   int maxToolCalls,
                   Supplier<String> sessionIdSupplier) {
        if (maxRounds <= 0 || maxToolCalls <= 0) {
            throw new IllegalArgumentException("Agent loop limits must be positive");
        }
        this.model = model;
        this.toolDispatcher = toolDispatcher;
        this.toolCallProcessor = toolCallProcessor;
        this.hookRegistry = hookRegistry;
        this.maxRounds = maxRounds;
        this.maxToolCalls = maxToolCalls;
        this.sessionIdSupplier = Objects.requireNonNull(sessionIdSupplier);
    }

    public String run(String prompt) {
        ConversationContext context = new ConversationContext();
        String sessionId = sessionIdSupplier.get();
        Object submittedPrompt = hookRegistry.triggerHooks(
                HookEvent.USER_PROMPT_SUBMIT, sessionId, prompt, prompt);
        context.add(new UserMessage(String.valueOf(submittedPrompt)));

        int toolCalls = 0;
        try {
            for (int round = 1; round <= maxRounds; round++) {
                String reminder = (String) hookRegistry.triggerHooks(
                        HookEvent.BEFORE_ROUND, sessionId, null, null);
                ChatRequest request = ChatRequest.builder()
                        .messages(context.snapshotForRequest(reminder))
                        .toolSpecifications(toolDispatcher.getActiveToolDefinitions(sessionId))
                        .build();
                ChatResponse response = model.chat(request);
                AiMessage aiMessage = response.aiMessage();
                context.add(aiMessage);

                if (!aiMessage.hasToolExecutionRequests()) {
                    return finish(sessionId, aiMessage.text());
                }

                for (ToolExecutionRequest toolRequest : aiMessage.toolExecutionRequests()) {
                    if (++toolCalls > maxToolCalls) {
                        return finish(sessionId, "Error: child Agent loop exceeded maximum tool calls: "
                                + maxToolCalls);
                    }
                    ToolProcessResult result = toolCallProcessor.process(sessionId, toolRequest);
                    if (result.status() == ToolProcessResult.Status.REQUIRES_APPROVAL) {
                        String error = "Error: child task requires approval and cannot pause: "
                                + result.reason();
                        context.add(ToolExecutionResultMessage.from(toolRequest, error));
                        return finish(sessionId, error);
                    }
                    String content = result.status() == ToolProcessResult.Status.ALLOWED
                            ? result.content()
                            : "Permission denied: " + result.reason();
                    context.add(ToolExecutionResultMessage.from(toolRequest, content));
                }
            }
            return finish(sessionId, "Error: child Agent loop exceeded maximum rounds: " + maxRounds);
        } catch (Exception error) {
            return finish(sessionId, "Error: child Agent failed: " + error.getMessage());
        }
    }

    private String finish(String sessionId, String output) {
        hookRegistry.triggerHooks(HookEvent.AFTER_LOOP, sessionId, null, null);
        return output == null ? "" : output;
    }
}
