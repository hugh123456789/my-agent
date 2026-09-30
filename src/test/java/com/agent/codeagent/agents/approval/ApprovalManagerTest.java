package com.agent.codeagent.agents.approval;

import com.agent.codeagent.agents.api.AgentEvent;
import com.agent.codeagent.agents.conversation.ConversationContext;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.ChatMessage;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.FluxSink;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

class ApprovalManagerTest {

    @Test
    void suspendedRequestCanBeTakenOnceAndRemovedForSink() {
        ApprovalManager manager = new ApprovalManager(60_000);
        @SuppressWarnings("unchecked")
        FluxSink<AgentEvent> sink = mock(FluxSink.class);
        ConversationContext context = new ConversationContext();
        List<ToolExecutionRequest> requests = List.of(request());

        String requestId = manager.suspend(context, requests, "state", sink);

        assertNotNull(manager.take(requestId));
        assertNull(manager.take(requestId));
    }

    private ToolExecutionRequest request() {
        return ToolExecutionRequest.builder()
                .id("test").name("bash").arguments("{}").build();
    }
}
