package com.agent.codeagent.agents.hook.core;

/** 单个事件回调。返回 null 表示保持当前 output。 */
@FunctionalInterface
public interface HookCallback {

    Object apply(HookContext context);
}
