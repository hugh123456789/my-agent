package com.agent.codeagent.agents.hook.permission;

import com.agent.codeagent.agents.hook.core.HookEvent;
import com.agent.codeagent.agents.hook.core.HookRegistrar;
import com.agent.codeagent.agents.hook.core.HookRegistry;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PermissionHookTest {

    @Test
    void deniesDangerousBashCommand() {
        HookRegistry registry = registryWithPermissionHook();

        Object result = registry.triggerHooks(
                HookEvent.PRE_TOOL_USE,
                request("bash", "rm -rf /"),
                new PermissionResult.Allowed());

        assertInstanceOf(PermissionResult.Denied.class, result);
    }

    @Test
    void asksBeforeFileSystemMutation() {
        HookRegistry registry = registryWithPermissionHook();

        Object result = registry.triggerHooks(
                HookEvent.PRE_TOOL_USE,
                request("bash", "echo hi > output.txt"),
                new PermissionResult.Allowed());

        assertInstanceOf(PermissionResult.AskUser.class, result);
    }

    @Test
    void allowsReadOnlyCommand() {
        HookRegistry registry = registryWithPermissionHook();

        Object result = registry.triggerHooks(
                HookEvent.PRE_TOOL_USE,
                request("bash", "ls -la"),
                new PermissionResult.Allowed());

        assertInstanceOf(PermissionResult.Allowed.class, result);
    }

    @Test
    void doesNotOverrideEarlierDenial() {
        HookRegistry registry = new HookRegistry(List.of(
                registrar -> registrar.registerHook(
                        HookEvent.PRE_TOOL_USE,
                        context -> new PermissionResult.Denied("custom denial")),
                new PermissionHook()));

        Object result = registry.triggerHooks(
                HookEvent.PRE_TOOL_USE,
                request("bash", "ls -la"),
                new PermissionResult.Allowed());

        assertEquals(new PermissionResult.Denied("custom denial"), result);
    }

    private HookRegistry registryWithPermissionHook() {
        return new HookRegistry(List.of(new PermissionHook()));
    }

    private ToolExecutionRequest request(String name, String arguments) {
        return ToolExecutionRequest.builder()
                .id("test")
                .name(name)
                .arguments(arguments)
                .build();
    }
}
