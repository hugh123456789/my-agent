package com.agent.codeagent.agents.api;

import com.agent.codeagent.agents.execution.AgentLoopExecutor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;

class AgentFacadeTest {

    @Test
    void constructorAcceptsTheLoopExecutorDependency() {
        assertDoesNotThrow(() -> new AgentFacade(mock(AgentLoopExecutor.class)));
    }
}
