package com.agent.codeagent.tools;

import com.agent.codeagent.agents.hook.core.HookContext;
import com.agent.codeagent.agents.hook.core.HookEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TodoReminderHookTest {

    @Test
    void keepsReminderCountersIsolatedBySession() {
        TodoStore store = new TodoStore();
        store.session("session-a").upsert("first goal", "pending");
        store.session("session-b").upsert("second goal", "pending");
        TodoReminderHook hook = new TodoReminderHook(store);

        hook.recordToolCall("session-a", false);
        hook.recordToolCall("session-a", false);
        hook.recordToolCall("session-a", false);

        assertNull(hook.beforeRound(new HookContext(HookEvent.BEFORE_ROUND, null, null, "session-b")));
        assertEquals(true, ((String) hook.beforeRound(
                new HookContext(HookEvent.BEFORE_ROUND, null, null, "session-a")))
                .contains("总目标：first goal"));
    }

    @Test
    void emitsReminderAfterThreeNonTodoCallsAndResetsAfterEmission() {
        TodoStore store = new TodoStore();
        store.upsert("goal", "pending");
        TodoReminderHook hook = new TodoReminderHook(store);

        hook.recordToolCall(false);
        hook.recordToolCall(false);
        hook.recordToolCall(false);

        String reminder = (String) hook.beforeRound(null);

        assertEquals(true, reminder.contains("总目标：goal"));
        assertNull(hook.beforeRound(null));
    }
}
