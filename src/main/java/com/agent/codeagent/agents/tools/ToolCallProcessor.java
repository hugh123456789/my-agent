package com.agent.codeagent.agents.tools;

import com.agent.codeagent.agents.hook.core.HookEvent;
import com.agent.codeagent.agents.hook.core.HookRegistry;
import com.agent.codeagent.agents.hook.permission.PermissionResult;
import com.agent.codeagent.agents.tool.ToolDispatcher;
import dev.langchain4j.agent.tool.ToolExecutionRequest;

/** Owns permission checks, tool execution, and post-tool transformation. */
public final class ToolCallProcessor {

    private final ToolDispatcher toolDispatcher;
    private final HookRegistry hookRegistry;

    public ToolCallProcessor(ToolDispatcher toolDispatcher, HookRegistry hookRegistry) {
        this.toolDispatcher = toolDispatcher;
        this.hookRegistry = hookRegistry;
    }

    public ToolProcessResult process(ToolExecutionRequest request) {
        return process(null, request);
    }

    public ToolProcessResult process(String sessionId, ToolExecutionRequest request) {
        Object rawPermission = hookRegistry.triggerHooks(
                HookEvent.PRE_TOOL_USE,
                sessionId,
                request,
                new PermissionResult.Allowed());
        if (!(rawPermission instanceof PermissionResult permission)) {
            return ToolProcessResult.denied("Invalid permission result");
        }

        return switch (permission) {
            case PermissionResult.Allowed() -> execute(sessionId, request);
            case PermissionResult.Denied(var reason) -> ToolProcessResult.denied(reason);
            case PermissionResult.AskUser(var reason) ->
                    ToolProcessResult.requiresApproval(reason);
        };
    }

    public ToolProcessResult executeApproved(ToolExecutionRequest request) {
        return executeApproved(null, request);
    }

    public ToolProcessResult executeApproved(String sessionId, ToolExecutionRequest request) {
        return execute(sessionId, request);
    }

    private ToolProcessResult execute(String sessionId, ToolExecutionRequest request) {
        String result;
        try {
            result = toolDispatcher.execute(sessionId, request.name(), request.arguments());
        } catch (Exception error) {
            result = "Error: " + error.getMessage();
        }
        Object transformed = hookRegistry.triggerHooks(
                HookEvent.POST_TOOL_USE,
                sessionId,
                request,
                result);
        return ToolProcessResult.allowed(String.valueOf(transformed));
    }
}
