package com.agent.codeagent.agents.conversation;

import dev.langchain4j.data.message.ChatMessage;

import java.util.ArrayList;
import java.util.List;

/** Creates a bounded request view while leaving the durable conversation untouched. */
public final class MessageCompactor {

    public static final int DEFAULT_MAX_MESSAGES = 40;
    public static final int DEFAULT_KEEP_HEAD = 3;

    private MessageCompactor() {
    }

    public static List<ChatMessage> compact(List<ChatMessage> messages) {
        return compact(messages, DEFAULT_MAX_MESSAGES, DEFAULT_KEEP_HEAD);
    }

    static List<ChatMessage> compact(List<ChatMessage> messages, int maxMessages, int keepHead) {
        if (messages.size() <= maxMessages) {
            return new ArrayList<>(messages);
        }

        int keepTail = maxMessages - keepHead;
        if (keepHead < 0 || keepTail < 0) {
            throw new IllegalArgumentException("keepHead must be between 0 and maxMessages");
        }

        List<ChatMessage> compacted = new ArrayList<>(maxMessages);
        compacted.addAll(messages.subList(0, keepHead));
        compacted.addAll(messages.subList(messages.size() - keepTail, messages.size()));
        return compacted;
    }
}
