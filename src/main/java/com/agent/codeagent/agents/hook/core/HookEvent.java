package com.agent.codeagent.agents.hook.core;

/** Agent 中可注册和触发的 hook 事件。 */
public enum HookEvent {
    USER_PROMPT_SUBMIT("UserPromptSubmit"),
    PRE_TOOL_USE("PreToolUse"),
    POST_TOOL_USE("PostToolUse"),
    BEFORE_ROUND("BeforeRound"),
    AFTER_LOOP("AfterLoop"),
    SUMMARY("Summary");

    private final String value;

    HookEvent(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static HookEvent fromValue(String value) {
        for (HookEvent event : values()) {
            if (event.value.equals(value)) return event;
        }
        throw new IllegalArgumentException("Unknown hook event: " + value);
    }
}
