package com.agent.codeagent.agents.hook.permission;

/**
 * 工具权限校验的三态结果。
 */
public sealed interface PermissionResult {

    /** 允许执行 */
    record Allowed() implements PermissionResult {}

    /** 硬拒绝 */
    record Denied(String reason) implements PermissionResult {}

    /** 需要用户审批 */
    record AskUser(String reason) implements PermissionResult {}
}
