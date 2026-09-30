package com.agent.codeagent.agents.conversation;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JsonlConversationStoreTest {

    @Test
    void appendsEachMessageAsOneRecordAndReloadsInOrder() throws Exception {
        Path directory = Files.createTempDirectory("sessions");
        JsonlConversationStore store = new JsonlConversationStore(directory);

        store.append("session-1", new UserMessage("hello"));
        long firstSize = Files.size(directory.resolve("session-1.jsonl"));
        store.append("session-1", new UserMessage("world"));

        assertEquals(2, Files.readAllLines(directory.resolve("session-1.jsonl")).size());
        assertEquals(2, store.load("session-1").size());
        assertEquals("hello", ((UserMessage) store.load("session-1").getFirst()).singleText());
        assertEquals("world", ((UserMessage) store.load("session-1").get(1)).singleText());
        assertEquals(true, Files.size(directory.resolve("session-1.jsonl")) > firstSize);
    }

    @Test
    void rejectsSessionIdsThatCanEscapeTheStorageDirectory() throws Exception {
        JsonlConversationStore store = new JsonlConversationStore(Files.createTempDirectory("sessions"));

        assertThrows(IllegalArgumentException.class,
                () -> store.load("../outside"));
    }

    @Test
    void ignoresAnIncompleteFinalRecord() throws Exception {
        Path directory = Files.createTempDirectory("sessions");
        JsonlConversationStore store = new JsonlConversationStore(directory);
        store.append("session-1", new UserMessage("hello"));
        Files.writeString(directory.resolve("session-1.jsonl"), "{\"type\":\"user\"", java.nio.file.StandardOpenOption.APPEND);

        assertEquals(1, store.load("session-1").size());
    }

    @Test
    void roundTripsAssistantAndToolResultMessages() throws Exception {
        JsonlConversationStore store = new JsonlConversationStore(Files.createTempDirectory("sessions"));
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .id("call-1").name("clock").arguments("{}").build();
        store.append("session-1", AiMessage.from("The time is"));
        store.append("session-1", ToolExecutionResultMessage.from(request, "12:00"));

        assertEquals("The time is", ((AiMessage) store.load("session-1").getFirst()).text());
        ToolExecutionResultMessage result = (ToolExecutionResultMessage) store.load("session-1").get(1);
        assertEquals("call-1", result.id());
        assertEquals("12:00", result.text());
    }

    @Test
    void roundTripsAssistantToolCalls() throws Exception {
        JsonlConversationStore store = new JsonlConversationStore(Files.createTempDirectory("sessions"));
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .id("call-1").name("clock").arguments("{}").build();
        store.append("session-1", AiMessage.from(java.util.List.of(request)));

        AiMessage restored = (AiMessage) store.load("session-1").getFirst();
        assertEquals("call-1", restored.toolExecutionRequests().getFirst().id());
        assertEquals("clock", restored.toolExecutionRequests().getFirst().name());
    }

    @Test
    void rejectsMalformedCompleteRecords() throws Exception {
        Path directory = Files.createTempDirectory("sessions");
        Files.writeString(directory.resolve("session-1.jsonl"), "{\"type\":\"unknown\"}\n");
        JsonlConversationStore store = new JsonlConversationStore(directory);

        assertThrows(IllegalStateException.class, () -> store.load("session-1"));
    }
}
