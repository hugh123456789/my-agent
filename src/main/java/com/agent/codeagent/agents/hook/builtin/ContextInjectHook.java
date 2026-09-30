package com.agent.codeagent.agents.hook.builtin;

import com.agent.codeagent.agents.hook.core.HookContext;
import com.agent.codeagent.agents.hook.core.HookEvent;
import com.agent.codeagent.agents.hook.core.HookRegistrar;
import com.agent.codeagent.agents.hook.core.HookRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** 在用户提交 prompt 时记录当前工作目录，预留上下文注入点。 */
@Component
public class ContextInjectHook implements HookRegistrar {

    private static final Logger log = LoggerFactory.getLogger(ContextInjectHook.class);
    private final String workdir;

    public ContextInjectHook() {
        this(System.getProperty("user.dir"));
    }

    ContextInjectHook(String workdir) {
        this.workdir = workdir;
    }

    @Override
    public String name() {
        return "context_inject_hook";
    }

    @Override
    public void register(HookRegistry registry) {
        registry.registerHook(HookEvent.USER_PROMPT_SUBMIT, this::injectContext);
    }

    private Object injectContext(HookContext context) {
        log.debug("[HOOK] UserPromptSubmit: working in {}", workdir);
        return null;
    }
}
