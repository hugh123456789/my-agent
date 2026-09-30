package com.agent.codeagent.agents.tools;

/** Structured outcome of one tool request. */
public record ToolProcessResult(
        Status status,
        String content,
        String reason) {

    public enum Status {
        ALLOWED,
        DENIED,
        REQUIRES_APPROVAL
    }

    public static ToolProcessResult allowed(String content) {
        return new ToolProcessResult(Status.ALLOWED, content, null);
    }

    public static ToolProcessResult denied(String reason) {
        return new ToolProcessResult(Status.DENIED, null, reason);
    }

    public static ToolProcessResult requiresApproval(String reason) {
        return new ToolProcessResult(Status.REQUIRES_APPROVAL, null, reason);
    }
}
