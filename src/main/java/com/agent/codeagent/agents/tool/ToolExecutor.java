package com.agent.codeagent.agents.tool;

/**
 * 工具执行器 — Agent 通过此接口执行 LLM 请求的工具调用。
 */
@FunctionalInterface
public interface ToolExecutor {

    String execute(String sessionId, String toolName, String arguments) throws Exception;

    /**
     * @param toolName  工具名称
     * @param arguments 工具参数 JSON 字符串
     * @return 工具执行结果文本
     */
    default String execute(String toolName, String arguments) throws Exception {
        return execute(null, toolName, arguments);
    }
}
