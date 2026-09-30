package com.agent.codeagent.tools;

import com.agent.codeagent.agents.hook.core.HookContext;
import com.agent.codeagent.agents.hook.core.HookEvent;
import com.agent.codeagent.agents.hook.core.HookRegistrar;
import com.agent.codeagent.agents.hook.core.HookRegistry;
import com.agent.codeagent.agents.hook.permission.PermissionResult;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import org.springframework.stereotype.Component;

/** Registers Todo-specific permission and reminder lifecycle hooks. */
@Component
public class TodoHookRegistrar implements HookRegistrar {

    private final TodoReminderHook reminderHook;

    public TodoHookRegistrar(TodoStore store) {
        this.reminderHook = new TodoReminderHook(store);
    }

    @Override
    public String name() {
        return "todo_hook";
    }

    @Override
    public void register(HookRegistry registry) {
        registry.registerHook(HookEvent.PRE_TOOL_USE, this::permission);
        registry.registerHook(HookEvent.BEFORE_ROUND, reminderHook::beforeRound);
        registry.registerHook(HookEvent.AFTER_LOOP, reminderHook::resetAfterLoop);
    }

    private Object permission(HookContext context) {
        if (context.block() instanceof ToolExecutionRequest request) {
            reminderHook.recordToolCall(context.sessionId(), "todo_write".equals(request.name()));
        }
        if (context.output() instanceof PermissionResult result
                && !(result instanceof PermissionResult.Allowed)) {
            return result;
        }
        return new PermissionResult.Allowed();
    }
}
