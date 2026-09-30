package com.agent.codeagent.agents.tools;

import com.agent.codeagent.agents.hook.core.HookEvent;
import com.agent.codeagent.agents.hook.core.HookRegistry;
import com.agent.codeagent.agents.hook.permission.PermissionResult;
import com.agent.codeagent.agents.tool.ToolDispatcher;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ToolCallProcessorTest {

    @Test
    void returnsDeniedResultWhenPermissionHookDenies() {
        HookRegistry hooks = new HookRegistry(List.of());
        hooks.registerHook(HookEvent.PRE_TOOL_USE,
                context -> new PermissionResult.Denied("blocked"));
        ToolCallProcessor processor = new ToolCallProcessor(new ToolDispatcher(), hooks);

        ToolProcessResult result = processor.process(request("bash"));

        assertEquals(ToolProcessResult.Status.DENIED, result.status());
        assertEquals("blocked", result.reason());
    }

    private ToolExecutionRequest request(String name) {
        return ToolExecutionRequest.builder()
                .id("test")
                .name(name)
                .arguments("{}")
                .build();
    }
}
