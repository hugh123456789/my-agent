package com.agent.codeagent.agents.tool;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonStringSchema;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolDispatcherTest {

    @Test
    void returnsOnlyNonDeferredToolsUntilSessionActivatesOne() {
        ToolDispatcher dispatcher = new ToolDispatcher();
        ToolSpecification active = ToolSpecification.builder().name("active").description("always available").build();
        ToolSpecification deferred = ToolSpecification.builder().name("deferred").description("rare tool")
                .parameters(JsonObjectSchema.builder()
                        .addProperty("value", JsonStringSchema.builder().build()).build())
                .build();
        dispatcher.register(active, (session, name, arguments) -> "active");
        dispatcher.register(deferred, (session, name, arguments) -> "deferred");
        dispatcher.markDeferred(List.of(deferred.name()));

        assertEquals(List.of(active), dispatcher.getActiveToolDefinitions("session-a"));
        assertEquals(List.of("deferred"), dispatcher.getDeferredToolNames());

        dispatcher.activateTools("session-a", List.of("deferred"));

        assertEquals(List.of(active, deferred), dispatcher.getActiveToolDefinitions("session-a"));
        assertEquals(List.of(active), dispatcher.getActiveToolDefinitions("session-b"));
    }

    @Test
    void toolSearchActivatesMatchingDeferredToolForCurrentSessionOnly() throws Exception {
        ToolDispatcher dispatcher = new ToolDispatcher();
        ToolSpecification deferred = ToolSpecification.builder().name("rare_tool").description("searchable rare operation").build();
        dispatcher.register(deferred, (session, name, arguments) -> "ok");
        dispatcher.markDeferred(List.of(deferred.name()));

        String result = dispatcher.searchAndActivate("session-a", "rare");

        assertTrue(result.contains("rare_tool"));
        assertEquals(List.of(deferred), dispatcher.getActiveToolDefinitions("session-a"));
        assertEquals(List.of(), dispatcher.getActiveToolDefinitions("session-b"));
    }
}
