package com.agent.codeagent.agents.hook.builtin;

import com.agent.codeagent.agents.hook.core.HookEvent;
import com.agent.codeagent.agents.hook.core.HookRegistry;
import com.agent.codeagent.agents.hook.permission.PermissionHook;
import com.agent.codeagent.agents.hook.permission.PermissionResult;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class HookIntegrationTest {

    @Test
    void concreteRegistrarsHandlePromptPermissionOutputAndLoopEvents() {
        HookRegistry registry = new HookRegistry(List.of(
                new ContextInjectHook("D:/work"),
                new PermissionHook(),
                new LogHook()));

        assertEquals("query", registry.triggerHooks(
                HookEvent.USER_PROMPT_SUBMIT, "query", "query"));

        Object permission = registry.triggerHooks(
                HookEvent.PRE_TOOL_USE,
                ToolExecutionRequest.builder().id("1").name("bash").arguments("rm -rf /").build(),
                new PermissionResult.Allowed());
        assertInstanceOf(PermissionResult.Denied.class, permission);

        assertEquals(null, registry.triggerHooks(HookEvent.AFTER_LOOP, null, null));
    }
}
