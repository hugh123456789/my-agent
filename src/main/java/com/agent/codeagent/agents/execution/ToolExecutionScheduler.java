package com.agent.codeagent.agents.execution;

import com.agent.codeagent.agents.tool.ToolDispatcher;
import com.agent.codeagent.agents.tools.ToolCallProcessor;
import com.agent.codeagent.agents.tools.ToolProcessResult;
import dev.langchain4j.agent.tool.ToolExecutionRequest;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Schedules one batch of tool requests while preserving request order. */
final class ToolExecutionScheduler {

    private final ToolDispatcher toolDispatcher;
    private final ToolCallProcessor toolCallProcessor;

    ToolExecutionScheduler(ToolDispatcher toolDispatcher, ToolCallProcessor toolCallProcessor) {
        this.toolDispatcher = toolDispatcher;
        this.toolCallProcessor = toolCallProcessor;
    }

    int nextBatchEnd(List<ToolExecutionRequest> requests, int start) {
        int end = start + 1;
        while (end < requests.size()
                && toolDispatcher.isConcurrencySafe(requests.get(end - 1).name())
                && toolDispatcher.isConcurrencySafe(requests.get(end).name())) {
            end++;
        }
        return end;
    }

    List<ToolProcessResult> execute(String sessionId, List<ToolExecutionRequest> requests) {
        if (requests.size() == 1) {
            return List.of(toolCallProcessor.process(sessionId, requests.getFirst()));
        }
        List<CompletableFuture<ToolProcessResult>> futures = requests.stream()
                .map(request -> CompletableFuture.supplyAsync(
                        () -> toolCallProcessor.process(sessionId, request)))
                .toList();
        return futures.stream().map(CompletableFuture::join).toList();
    }
}
