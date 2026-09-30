package com.agent.codeagent.tools;

import com.agent.codeagent.agents.hook.core.HookContext;

/** Owns the reminder counter and lifecycle, separate from Todo persistence. */
public final class TodoReminderHook {

    static final int REMIND_AFTER_TOOL_CALLS = 3;
    private final TodoStore store;

    public TodoReminderHook(TodoStore store) {
        this.store = store;
    }

    public void recordToolCall(boolean todoWrite) {
        recordToolCall(null, todoWrite);
    }

    public void recordToolCall(String sessionId, boolean todoWrite) {
        store.session(sessionId).recordToolCall(todoWrite);
    }

    public Object beforeRound(HookContext context) {
        TodoStore.SessionState session = store.session(context == null ? null : context.sessionId());
        if (session.consumeReminderIfDue(REMIND_AFTER_TOOL_CALLS)) {
            return session.formatReminder();
        }
        return null;
    }

    public Object resetAfterLoop(HookContext context) {
        String sessionId = context == null ? null : context.sessionId();
        if (sessionId != null && sessionId.startsWith("sub-agent-")) {
            store.removeSession(sessionId);
        } else {
            store.session(sessionId).resetReminder();
        }
        return null;
    }
}
