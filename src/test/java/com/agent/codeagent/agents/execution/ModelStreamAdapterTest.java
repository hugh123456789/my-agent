package com.agent.codeagent.agents.execution;

import com.agent.codeagent.agents.api.AgentEvent;

import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.FluxSink;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ModelStreamAdapterTest {

    @Test
    void emitsTextEventWhileSinkIsActive() {
        @SuppressWarnings("unchecked")
        FluxSink<AgentEvent> sink = mock(FluxSink.class);
        when(sink.isCancelled()).thenReturn(false);

        StreamingChatResponseHandler handler = ModelStreamAdapter.handler(
                sink, response -> {}, error -> {});

        handler.onPartialResponse("hello");

        verify(sink).next(new AgentEvent.TextDelta("hello"));
    }
}
