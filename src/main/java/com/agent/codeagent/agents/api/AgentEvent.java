package com.agent.codeagent.agents.api;

import dev.langchain4j.model.chat.response.ChatResponse;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * LLM 流式事件 — 统一表达文本、思考、工具调用、完成、错误。
 * Jackson 序列化时 type 字段用于前端区分事件种类。
 */
public sealed interface AgentEvent {

    @JsonProperty("type")
    String type();

    /** Identifies the durable conversation used by the current loop. */
    record SessionStarted(String sessionId) implements AgentEvent {
        public String type() { return "session"; }
    }

    /** 文本增量 token */
    record TextDelta(String text) implements AgentEvent {
        public String type() { return "text"; }
    }

    /** 思考增量（推理模型如 DeepSeek-R1） */
    record ThinkingDelta(String text) implements AgentEvent {
        public String type() { return "thinking"; }
    }

    /** 工具调用增量（流式输出工具名和参数） */
    record ToolCallDelta(int index, String id, String name, String arguments) implements AgentEvent {
        public String type() { return "tool_call"; }
    }

    /** 流式完成 */
    record Complete(ChatResponse response) implements AgentEvent {
        public String type() { return "complete"; }
    }

    /** 工具执行结果 */
    record ToolResultDelta(String id, String toolName, String content) implements AgentEvent {
        public String type() { return "tool_result"; }
    }

    /** 工具调用被权限校验拒绝 */
    record PermissionDenied(String toolName, String reason) implements AgentEvent {
        public String type() { return "permission_denied"; }
    }

    /** 需要用户审批才能继续 — 携带 requestId 供 approve 端点使用 */
    record PermissionRequired(String requestId, String toolName, String arguments, String reason)
            implements AgentEvent {
        public String type() { return "permission_required"; }
    }

    /** 发生错误 */
    record Error(Throwable error) implements AgentEvent {
        public String type() { return "error"; }
    }

    /** 规划提醒 — LLM 连续多次工具调用未使用 todo_write 时注入 */
    record Reminder(String text) implements AgentEvent {
        public String type() { return "reminder"; }
    }
}
