package com.agent.codeagent.agents.hook.builtin;

import com.agent.codeagent.agents.hook.core.HookContext;
import com.agent.codeagent.agents.hook.core.HookEvent;
import com.agent.codeagent.agents.hook.core.HookRegistrar;
import com.agent.codeagent.agents.hook.core.HookRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** 记录关键 hook 事件，不改变事件 output。 */
@Component
public class LogHook implements HookRegistrar {

    private static final Logger log = LoggerFactory.getLogger(LogHook.class);

    @Override
    public String name() {
        return "log_hook";
    }

    @Override
    public void register(HookRegistry registry) {
        registry.registerHook(HookEvent.PRE_TOOL_USE, this::logEvent);
        registry.registerHook(HookEvent.POST_TOOL_USE, this::logEvent);
        registry.registerHook(HookEvent.AFTER_LOOP, this::logEvent);
    }

    private Object logEvent(HookContext context) {
        log.info("[HOOK] {} block={}", context.event().value(), context.block());
        return null;
    }
}
