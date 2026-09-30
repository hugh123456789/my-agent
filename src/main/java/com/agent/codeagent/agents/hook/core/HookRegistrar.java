package com.agent.codeagent.agents.hook.core;

/** Spring hook 组件向注册表声明自身回调的入口。 */
@FunctionalInterface
public interface HookRegistrar {

    default String name() {
        return getClass().getSimpleName();
    }

    void register(HookRegistry registry);
}
