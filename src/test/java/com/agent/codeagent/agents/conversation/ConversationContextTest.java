package com.agent.codeagent.agents.conversation;

import dev.langchain4j.data.message.UserMessage;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConversationContextTest {

    @Test
    void reminderIsAddedOnlyToRequestSnapshot() {
        ConversationContext context = new ConversationContext();
        context.add(new UserMessage("hello"));

        List<?> requestMessages = context.snapshotForRequest("continue the todo");

        assertEquals(1, context.messages().size());
        assertEquals(2, requestMessages.size());
        assertNotSame(context.messages(), requestMessages);
    }

    @Test
    void reloadsDurableMessagesForTheSameSession() throws Exception {
        JsonlConversationStore store = new JsonlConversationStore(Files.createTempDirectory("sessions"));
        ConversationContext first = new ConversationContext("session-1", store);
        first.add(new UserMessage("hello"));

        ConversationContext second = new ConversationContext("session-1", store);

        assertEquals(1, second.messages().size());
        assertEquals("hello", ((UserMessage) second.messages().getFirst()).singleText());
    }

    @Test
    void doesNotExposeMutableMessageCollection() {
        ConversationContext context = new ConversationContext();
        context.add(new UserMessage("hello"));

        List<?> messages = context.messages();

        assertThrows(UnsupportedOperationException.class, () -> messages.clear());
        assertEquals(1, context.messages().size());
    }

    @Test
    void requestSnapshotKeepsAllMessagesWhenAtCompressionLimit() {
        ConversationContext context = contextWithMessages(40);

        List<?> requestMessages = context.snapshotForRequest(null);

        assertEquals(40, requestMessages.size());
        assertEquals("message-1", ((UserMessage) requestMessages.getFirst()).singleText());
        assertEquals("message-40", ((UserMessage) requestMessages.getLast()).singleText());
    }

    @Test
    void requestSnapshotRemovesOnlyMiddleMessagesWhenOverCompressionLimit() {
        ConversationContext context = contextWithMessages(42);

        List<?> requestMessages = context.snapshotForRequest(null);

        assertEquals(40, requestMessages.size());
        assertEquals("message-1", ((UserMessage) requestMessages.get(0)).singleText());
        assertEquals("message-2", ((UserMessage) requestMessages.get(1)).singleText());
        assertEquals("message-3", ((UserMessage) requestMessages.get(2)).singleText());
        assertEquals("message-6", ((UserMessage) requestMessages.get(3)).singleText());
        assertEquals("message-42", ((UserMessage) requestMessages.getLast()).singleText());
    }

    @Test
    void compressionDoesNotChangeDurableMessages() {
        ConversationContext context = contextWithMessages(42);

        context.snapshotForRequest(null);

        assertEquals(42, context.messages().size());
        assertEquals("message-4", ((UserMessage) context.messages().get(3)).singleText());
        assertEquals("message-5", ((UserMessage) context.messages().get(4)).singleText());
    }

    private static ConversationContext contextWithMessages(int count) {
        ConversationContext context = new ConversationContext();
        for (int i = 1; i <= count; i++) {
            context.add(new UserMessage("message-" + i));
        }
        return context;
    }
}
