package com.agent.codeagent.agents.hook.core;

import com.agent.codeagent.agents.hook.permission.PermissionHook;
import com.agent.codeagent.agents.hook.permission.PermissionResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

class HookRegistryTest {

    @Test
    void callbacksRunInRegistrationOrderAndPassOutputForward() {
        HookRegistry registry = new HookRegistry(List.of());
        List<String> calls = new ArrayList<>();

        registry.registerHook(HookEvent.USER_PROMPT_SUBMIT, context -> {
            calls.add(context.block() + ":" + context.output());
            return "changed";
        });
        registry.registerHook(HookEvent.USER_PROMPT_SUBMIT, context -> {
            calls.add(context.block() + ":" + context.output());
            return null;
        });

        Object result = registry.triggerHooks(
                HookEvent.USER_PROMPT_SUBMIT, "query", "original");

        assertEquals(List.of("query:original", "query:changed"), calls);
        assertEquals("changed", result);
    }

    @Test
    void unregisteredEventAndNullReturnKeepCurrentOutput() {
        HookRegistry registry = new HookRegistry(List.of());
        registry.registerHook(HookEvent.POST_TOOL_USE, context -> null);

        assertEquals("original", registry.triggerHooks(
                HookEvent.POST_TOOL_USE, "block", "original"));
        assertEquals("original", registry.triggerHooks(
                HookEvent.PRE_TOOL_USE, "block", "original"));
        assertNull(registry.triggerHooks(HookEvent.AFTER_LOOP, "block", null));
    }

    @Test
    void callbackFailureDoesNotStopFollowingCallbacks() {
        HookRegistry registry = new HookRegistry(List.of());
        registry.registerHook(HookEvent.POST_TOOL_USE, context -> {
            throw new IllegalStateException("broken hook");
        });
        registry.registerHook(HookEvent.POST_TOOL_USE, context -> "handled");

        assertEquals("handled", registry.triggerHooks(
                HookEvent.POST_TOOL_USE, "block", "output"));
    }

    @Test
    void springRegistrarsAreRegisteredInConstructorOrder() {
        List<String> calls = new ArrayList<>();
        HookRegistrar first = registry -> registry.registerHook(
                HookEvent.AFTER_LOOP, context -> {
                    calls.add("first");
                    return null;
                });
        HookRegistrar second = registry -> registry.registerHook(
                HookEvent.AFTER_LOOP, context -> {
                    calls.add("second");
                    return null;
                });

        new HookRegistry(List.of(first, second))
                .triggerHooks(HookEvent.AFTER_LOOP, null, null);

        assertEquals(List.of("first", "second"), calls);
    }

    @Test
    void supportsExternalStringEventNames() {
        HookRegistry registry = new HookRegistry(List.of());
        registry.registerHook("PostToolUse", context -> "changed");

        assertEquals("changed", registry.triggerHooks("PostToolUse", "block", "output"));
    }

    @Test
    void registrarsAreDisabledWhenNotIncludedInEnabledList() {
        HookRegistry registry = new HookRegistry(
                List.<HookRegistrar>of(new PermissionHook()), Set.<String>of());

        Object result = registry.triggerHooks(
                HookEvent.PRE_TOOL_USE,
                dev.langchain4j.agent.tool.ToolExecutionRequest.builder()
                        .id("test").name("bash").arguments("rm -rf /").build(),
                new PermissionResult.Allowed());

        assertInstanceOf(PermissionResult.Allowed.class, result);
    }

    @Test
    void onlyNamedRegistrarsAreEnabled() {
        HookRegistry registry = new HookRegistry(
                List.<HookRegistrar>of(new PermissionHook()), Set.of("permission_hook"));

        Object result = registry.triggerHooks(
                HookEvent.PRE_TOOL_USE,
                dev.langchain4j.agent.tool.ToolExecutionRequest.builder()
                        .id("test").name("bash").arguments("rm -rf /").build(),
                new PermissionResult.Allowed());

        assertInstanceOf(PermissionResult.Denied.class, result);
    }

    @Test
    void nullEnabledListEnablesAllRegistrars() {
        HookRegistry registry = new HookRegistry(
                List.<HookRegistrar>of(new PermissionHook()),
                (java.util.Collection<String>) null);

        Object result = registry.triggerHooks(
                HookEvent.PRE_TOOL_USE,
                dev.langchain4j.agent.tool.ToolExecutionRequest.builder()
                        .id("test").name("bash").arguments("rm -rf /").build(),
                new PermissionResult.Allowed());

        assertInstanceOf(PermissionResult.Denied.class, result);
    }
}
