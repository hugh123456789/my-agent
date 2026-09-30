package com.agent.codeagent.agents.execution;

import com.agent.codeagent.agents.hook.core.HookRegistry;
import com.agent.codeagent.agents.tool.ToolDispatcher;
import com.agent.codeagent.agents.tools.ToolCallProcessor;
import com.agent.codeagent.agents.tools.ToolProcessResult;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolExecutionSchedulerTest {

    @Test
    void executesSafeBatchInParallelAndPreservesRequestOrder() throws Exception {
        ToolDispatcher dispatcher = new ToolDispatcher();
        CountDownLatch started = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);
        dispatcher.register(ToolSpecification.builder().name("a").description("a").build(),
                (session, name, arguments) -> await(started, release, "a"), true);
        dispatcher.register(ToolSpecification.builder().name("b").description("b").build(),
                (session, name, arguments) -> await(started, release, "b"), true);
        ToolExecutionScheduler scheduler = new ToolExecutionScheduler(
                dispatcher, new ToolCallProcessor(dispatcher, new HookRegistry(List.of())));
        List<ToolExecutionRequest> requests = List.of(
                request("a"), request("b"));

        var results = new java.util.concurrent.CompletableFuture<List<ToolProcessResult>>();
        Thread.startVirtualThread(() -> results.complete(scheduler.execute("session", requests)));

        assertTrue(started.await(2, TimeUnit.SECONDS));
        release.countDown();
        assertEquals(List.of("a", "b"), results.get(2, TimeUnit.SECONDS).stream()
                .map(ToolProcessResult::content).toList());
    }

    private static ToolExecutionRequest request(String name) {
        return ToolExecutionRequest.builder().id(name).name(name).arguments("{}").build();
    }

    private static String await(CountDownLatch started, CountDownLatch release, String result) {
        started.countDown();
        try {
            assertTrue(release.await(2, TimeUnit.SECONDS));
            return result;
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new AssertionError(error);
        }
    }
}
