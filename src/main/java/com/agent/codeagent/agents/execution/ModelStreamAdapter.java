package com.agent.codeagent.agents.execution;

import com.agent.codeagent.agents.api.AgentEvent;

import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.PartialThinking;
import dev.langchain4j.model.chat.response.PartialToolCall;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import reactor.core.publisher.FluxSink;

import java.util.Objects;
import java.util.function.Consumer;

/** Converts LangChain4j callbacks into the application's streaming events. */
public final class ModelStreamAdapter {

    private ModelStreamAdapter() {
    }

    public static StreamingChatResponseHandler handler(
            FluxSink<AgentEvent> sink,
            Consumer<ChatResponse> onComplete,
            Consumer<Throwable> onError) {
        Objects.requireNonNull(sink, "sink");
        Objects.requireNonNull(onComplete, "onComplete");
        Objects.requireNonNull(onError, "onError");

        return new StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(String partialResponse) {
                emit(sink, new AgentEvent.TextDelta(partialResponse));
            }

            @Override
            public void onPartialThinking(PartialThinking partialThinking) {
                emit(sink, new AgentEvent.ThinkingDelta(partialThinking.text()));
            }

            @Override
            public void onPartialToolCall(PartialToolCall partialToolCall) {
                emit(sink, new AgentEvent.ToolCallDelta(
                        partialToolCall.index(),
                        partialToolCall.id(),
                        partialToolCall.name(),
                        partialToolCall.partialArguments()));
            }

            @Override
            public void onCompleteResponse(ChatResponse response) {
                if (!sink.isCancelled()) onComplete.accept(response);
            }

            @Override
            public void onError(Throwable error) {
                if (!sink.isCancelled()) {
                    sink.next(new AgentEvent.Error(error));
                    onError.accept(error);
                }
            }
        };
    }

    private static void emit(FluxSink<AgentEvent> sink, AgentEvent event) {
        if (!sink.isCancelled()) sink.next(event);
    }
}
