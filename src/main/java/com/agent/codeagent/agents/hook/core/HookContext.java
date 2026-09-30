package com.agent.codeagent.agents.hook.core;

/** 一次 hook 调用的不可变上下文。 */
public record HookContext(HookEvent event, Object block, Object output, String sessionId) {

    public HookContext(HookEvent event, Object block, Object output) {
        this(event, block, output, null);
    }

    public HookContext withOutput(Object nextOutput) {
        return new HookContext(event, block, nextOutput, sessionId);
    }
}
